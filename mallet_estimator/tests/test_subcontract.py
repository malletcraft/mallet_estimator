# Subcontract SKUs on a real site — the vendor rate reaching an estimate.
# Run under `bench run-tests`. The pure arithmetic lives in test_estimator.
import frappe

from mallet_estimator import estimator as E, install, inventory, worksite

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


class TestSubcontractMasters(MalletTestCase):

    def test_every_subcontract_article_has_somewhere_to_hold_a_rate(self):
        # The quiet failure this prevents: a subcontract line whose article
        # has no Item resolves to rate 0, and a quote missing a trade's cost
        # looks exactly like a complete one.
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        out = worksite.ensure_subcontract_service_items()
        missing = [c for c, _n, _j, k, _b in worksite.ARTICLES
                   if k == worksite.SUBCONTRACT
                   and not frappe.db.exists(
                       "Item", worksite.subcontract_item_code(c))]
        # The errors come back with the result rather than only reaching the
        # Error Log. A seeder that fails quietly is how fifteen items were
        # missing for a whole CI round with nothing on screen but "0 of 15".
        self.assertEqual(missing, [], "no Item for %s; errors: %s"
                         % (missing, out.get("errors")))

    def test_a_service_item_is_bought_never_stocked(self):
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        code = worksite.subcontract_item_code("POP")
        it = frappe.db.get_value(
            "Item", code, ["is_stock_item", "is_purchase_item", "stock_uom"],
            as_dict=True)
        self.assertEqual(it.is_stock_item, 0,
                         "nothing is received, stored or valued")
        self.assertEqual(it.is_purchase_item, 1)
        # The UNIT is the article's basis. POP is quoted by area, and an Item
        # in the wrong unit turns 420 sqft into 420 of something else.
        self.assertEqual(it.stock_uom, "Sqft")

    def test_seeding_twice_changes_nothing(self):
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        before = frappe.db.count("Item", {"item_code": ["like", "SVC\\_%"]})
        out = worksite.ensure_subcontract_service_items()
        after = frappe.db.count("Item", {"item_code": ["like", "SVC\\_%"]})
        self.assertEqual(before, after)
        self.assertEqual(out["made"], 0)

    def test_no_service_item_carries_a_rate_out_of_the_box(self):
        # THE RULE, asserted rather than trusted: cost data never enters this
        # repo. The seeder builds the shelf; keying the rate is a human act on
        # the site. A shipped rate would be a real vendor price in a public
        # repository, permanently.
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        for c, _n, _j, k, _b in worksite.ARTICLES:
            if k != worksite.SUBCONTRACT:
                continue
            code = worksite.subcontract_item_code(c)
            for field in ("valuation_rate", "last_purchase_rate", "standard_rate"):
                self.assertFalse(frappe.db.get_value("Item", code, field),
                                 "%s ships with a %s" % (code, field))


class TestVendorRate(MalletTestCase):
    """Whose rate a line is priced at, and whether it says so."""

    def _item(self):
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        return worksite.subcontract_item_code("TIL")

    def _supplier(self, name):
        if not frappe.db.exists("Supplier", name):
            frappe.get_doc({"doctype": "Supplier", "supplier_name": name}
                           ).insert(ignore_permissions=True)
        return name

    def test_an_unpriced_trade_reads_unset_not_free(self):
        rate, source = inventory.vendor_rate(self._item(), None)
        if source != "unset":
            self.skipTest("this site already prices tiling")
        self.assertEqual(rate, 0.0)

    def test_the_named_vendors_own_price_wins(self):
        item = self._item()
        a, b = self._supplier("ZZ Tiling A"), self._supplier("ZZ Tiling B")
        # Invented figures. B is dearer, which is the whole point of the test.
        inventory.set_vendor_price(item, a, 40)
        inventory.set_vendor_price(item, b, 90)
        rate, source = inventory.vendor_rate(item, a)
        self.assertEqual(source, "vendor")
        self.assertAlmostEqual(float(rate), 40, 2)
        # Quoting A at B's rate because B is dearer is not conservative — it
        # is a number no invoice will ever match, and it pads the job.
        self.assertNotAlmostEqual(float(rate), 90, 2)

    def test_a_stand_in_rate_admits_that_it_is_one(self):
        item = self._item()
        a = self._supplier("ZZ Tiling A")
        inventory.set_vendor_price(item, a, 40)
        unknown = self._supplier("ZZ Tiling Never Priced")
        rate, source = inventory.vendor_rate(item, unknown)
        self.assertTrue(rate, "a fallback should still produce a number")
        self.assertIn(source, ("assumed ceiling", "another vendor"))
        self.assertNotEqual(source, "vendor",
                            "a substitute must never pass as this vendor's own")


class TestSubcontractSku(MalletTestCase):

    def _project(self):
        """Estimate SKU requires a project — every SKU belongs to a job."""
        name = frappe.db.get_value("Project", {"project_name": "ZZ Subcontract Job"})
        if name:
            return name
        return frappe.get_doc({
            "doctype": "Project", "project_name": "ZZ Subcontract Job",
        }).insert(ignore_permissions=True).name

    def _sku(self, lines):
        worksite.ensure_articles()
        worksite.ensure_subcontract_service_items()
        doc = frappe.get_doc({
            "doctype": "Estimate SKU",
            "article_name": "ZZ Subcontract Probe",
            "project": self._project(),
            "work_type": E.SUBCONTRACT,
            "auto_name": 0,
            "sku_code": "ZZ_SUB_PROBE",
            "create_item": 0,
            "subcontract_lines": lines,
        })
        doc.insert(ignore_permissions=True)
        return doc

    def test_the_unit_comes_from_the_article_not_from_typing(self):
        doc = self._sku([{"article": "POP", "qty": 100},
                         {"article": "ELP", "qty": 20}])
        self.assertEqual(doc.subcontract_lines[0].uom, "Sqft")
        self.assertEqual(doc.subcontract_lines[1].uom, "Point")
        self.assertEqual(doc.subcontract_lines[0].service_item, "SVC_POP")

    def test_a_build_article_is_refused_on_a_subcontract_line(self):
        # WAR is a wardrobe — something the shop builds. Left unchecked it
        # would price silently against whatever that article's Item costs.
        with self.assertRaises(frappe.ValidationError):
            self._sku([{"article": "WAR", "qty": 1}])

    def test_an_unpriced_trade_is_named_on_the_document(self):
        doc = self._sku([{"article": "POP", "qty": 400}])
        if doc.subcontract_lines[0].rate:
            self.skipTest("this site already prices POP")
        self.assertIn("POP", doc.subcontract_unpriced or "")
        self.assertFalse(doc.subcontract_cost)

    def test_the_shop_floor_is_not_involved(self):
        # The claim that makes this a different work type at all: no parts, no
        # operations, no décor map. A subcontract SKU that quietly grew the
        # seventeen steps would be costing agency work in carpenter minutes.
        doc = self._sku([{"article": "TIL", "qty": 50}])
        self.assertFalse(doc.get("labor"), "no shop labour on agency work")
        self.assertFalse(doc.get("parts"))
        self.assertFalse(doc.get("sku_decors"))
