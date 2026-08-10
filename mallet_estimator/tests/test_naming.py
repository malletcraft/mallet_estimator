# Naming, verified against a real database rather than as pure functions.
#
# abbr() returning BAS_CAB does not prove that saving a SKU produces an Item
# called BAS_CAB. The code is computed on validate, made unique against rows
# that already exist, and carried onto an ERPNext Item that has to FOLLOW a
# rename. Each of those steps needs a database to be wrong in, so each is
# exercised here against one.
import frappe

from mallet_estimator import estimator

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


# Distinctive enough that no other test's SKUs share the prefix — _unique_code
# looks at every Estimate SKU whose code starts the same way, so a shared
# prefix would make these assertions depend on test ordering.
CUSTOMER_NAME = "Naming Verification Customer"


def _room(name):
    if not frappe.db.exists("Estimate Room", name):
        frappe.get_doc({"doctype": "Estimate Room", "room_name": name}).insert(
            ignore_permissions=True)
    return name


def _customer():
    existing = frappe.db.get_value("Customer", {"customer_name": CUSTOMER_NAME}, "name")
    if existing:
        return existing
    return frappe.get_doc({
        "doctype": "Customer", "customer_name": CUSTOMER_NAME,
        "customer_type": "Individual",
    }).insert(ignore_permissions=True).name


def _company():
    name = frappe.db.get_value("Company", {}, "name")
    if name:
        return name
    return frappe.get_doc({
        "doctype": "Company", "company_name": "Mallet Test Co", "abbr": "MTC",
        "default_currency": "INR", "country": "India",
    }).insert(ignore_permissions=True).name


def _project(customer):
    existing = frappe.db.get_value("Project", {"project_name": "Naming Verification"}, "name")
    if existing:
        return existing
    return frappe.get_doc({
        "doctype": "Project", "project_name": "Naming Verification", "customer": customer,
        "company": _company(),
    }).insert(ignore_permissions=True).name


def _why_the_rename_did_not_happen(old_code, new_code):
    """sync_item swallows a refused rename into the Error Log and carries on,
    which is right for a user mid-save and useless for a failing test. Pull the
    reason back out so the failure says WHY instead of only that it happened."""
    facts = [
        f"old Item {old_code} exists: {bool(frappe.db.exists('Item', old_code))}",
        f"new Item {new_code} exists: {bool(frappe.db.exists('Item', new_code))}",
    ]
    log = frappe.get_all("Error Log", filters={"method": ["like", "%rename item%"]},
                         fields=["error"], order_by="creation desc", limit=1)
    if log:
        facts.append("Error Log: " + (log[0].error or "")[-1200:])
    return "\n".join(facts)


class TestSkuNamingInTheDatabase(MalletTestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.customer = _customer()
        cls.project = _project(cls.customer)
        _room("Kitchen")
        _room("Master Bedroom")
        # The customer prefix is whatever the customer is called; only the room
        # and article halves are asserted literally, since those are the rule.
        cls.ci = estimator.customer_initials(
            frappe.db.get_value("Customer", cls.customer, "customer_name"))

    def _sku(self, article, room):
        doc = frappe.new_doc("Estimate SKU")
        doc.project = self.project
        doc.customer = self.customer
        doc.room = room
        doc.article_name = article
        return doc.insert(ignore_permissions=True)

    def test_a_saved_sku_carries_the_code_and_an_item_named_for_it(self):
        sku = self._sku("Base Cabinet", "Kitchen")
        self.assertEqual(sku.sku_code, f"{self.ci}_KIT_BAS_CAB")
        # The Item is the point of the exercise — it is what the stock ledger,
        # the BOM and the person searching actually see.
        self.assertEqual(sku.item, sku.sku_code)
        self.assertTrue(frappe.db.exists("Item", sku.sku_code),
                        f"no ERPNext Item called {sku.sku_code}")

    def test_a_multi_word_room_keeps_its_initials(self):
        sku = self._sku("Wardrobe", "Master Bedroom")
        self.assertEqual(sku.sku_code, f"{self.ci}_MB_WAR")
        self.assertTrue(frappe.db.exists("Item", sku.sku_code))

    def test_a_second_identical_article_gets_its_own_code_and_its_own_item(self):
        # Two wardrobes for one customer in one room used to compute the SAME
        # code, and the second silently attached itself to the FIRST one's Item
        # — both wrote to one Item and its price became whichever saved last.
        first = self._sku("Study Table", "Kitchen")
        second = self._sku("Study Table", "Kitchen")
        self.assertEqual(first.sku_code, f"{self.ci}_KIT_STU_TAB")
        self.assertEqual(second.sku_code, f"{self.ci}_KIT_STU_TAB_2")
        self.assertNotEqual(first.item, second.item)
        self.assertTrue(frappe.db.exists("Item", second.sku_code))

    def test_renaming_the_article_renames_the_erpnext_item(self):
        sku = self._sku("Tall Unit", "Kitchen")
        old_code = sku.sku_code
        self.assertEqual(old_code, f"{self.ci}_KIT_TAL_UNI")
        self.assertTrue(frappe.db.exists("Item", old_code))

        sku.article_name = "Crockery Unit"
        sku.save(ignore_permissions=True)

        new_code = f"{self.ci}_KIT_CRO_UNI"
        self.assertEqual(sku.sku_code, new_code)
        self.assertEqual(sku.item, new_code, _why_the_rename_did_not_happen(old_code, new_code))
        self.assertTrue(frappe.db.exists("Item", new_code),
                        "the Item did not follow the rename")
        # Renamed, not duplicated: the old code must be gone, or a search for
        # the article turns up two Items and neither is obviously the live one.
        self.assertFalse(frappe.db.exists("Item", old_code),
                         f"{old_code} still exists — the Item was copied, not renamed")

    def test_a_one_word_room_reads_as_a_room_not_a_letter(self):
        # "K" in the middle of a code is a letter, not a room. This is the whole
        # reason single-word rooms take three characters.
        sku = self._sku("Chimney Hood", "Kitchen")
        self.assertIn("_KIT_", sku.sku_code)
        self.assertNotIn("_K_", sku.sku_code)


class TestAnArticleWithNoParts(MalletTestCase):
    """An SKU with no material lines is not a cheap article — it is one nobody
    has told us the parts of. It still accrues labour, overhead and days, so it
    prices like a finished quote and reads like one. Silent and plausible is
    the worst way for this to fail, so both halves are pinned."""

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.customer = _customer()
        cls.project = _project(cls.customer)
        _room("Master Bedroom")

    def _bare_sku(self, article):
        doc = frappe.new_doc("Estimate SKU")
        doc.project = self.project
        doc.customer = self.customer
        doc.room = "Master Bedroom"
        doc.article_name = article
        return doc.insert(ignore_permissions=True)

    def test_it_says_it_has_no_material_lines(self):
        sku = self._bare_sku("Partless Wardrobe")
        self.assertIn("NO MATERIAL LINES", sku.unpriced_materials or "",
                      "an SKU with no parts must say so where the screen shows it in red")

    def test_a_labour_quantity_cannot_conjure_glue(self):
        # 7 typed into Sheet Lamination used to produce 21 packets of Fevicol
        # and 77 m of tape — a third of the internal cost — with nothing bought.
        sku = self._bare_sku("Glueless Wardrobe")
        for row in sku.labor or []:
            if (row.operation or row.phase) == "Sheet Lamination":
                row.qty = 7
        sku.save(ignore_permissions=True)
        self.assertEqual(len(sku.joinery_items or []), 0,
                         "joinery must follow laminate MATERIAL, not a labour row")
        self.assertFalse(sku.joinery_cost)

    def test_an_estimate_cannot_be_approved_with_a_partless_sku(self):
        sku = self._bare_sku("Unapprovable Wardrobe")
        est = frappe.new_doc("Estimate")
        est.project = self.project
        est.work_type = "New Work"
        est.append("skus", {"estimate_sku": sku.name})
        est.insert(ignore_permissions=True)
        with self.assertRaises(frappe.ValidationError):
            est.submit()


class TestFilesReachTheSku(MalletTestCase):
    """A file is useless wherever it is not read. Two ways it went missing:
    dropped on the Attachments sidebar instead of the field, and blanked off
    the SKU by an empty grid row on the estimate."""

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.customer = _customer()
        cls.project = _project(cls.customer)
        _room("Master Bedroom")

    def _sku(self, article):
        doc = frappe.new_doc("Estimate SKU")
        doc.project = self.project
        doc.customer = self.customer
        doc.room = "Master Bedroom"
        doc.article_name = article
        return doc.insert(ignore_permissions=True)

    def test_an_empty_grid_row_never_wipes_the_skus_file(self):
        # Picking an existing SKU makes a row with no file in it. That row used
        # to count as a change and push a blank back, taking the SKU's part
        # list — and every material line imported from it — with it.
        sku = self._sku("Filed Wardrobe")
        frappe.db.set_value("Estimate SKU", sku.name, "parts_csv",
                            "/files/keep_me.csv", update_modified=False)
        est = frappe.new_doc("Estimate")
        est.project = self.project
        est.work_type = "New Work"
        est.append("skus", {"estimate_sku": sku.name})
        est.insert(ignore_permissions=True)
        self.assertEqual(
            frappe.db.get_value("Estimate SKU", sku.name, "parts_csv"),
            "/files/keep_me.csv", "the empty row wiped the SKU's part list")

    def test_the_grid_row_shows_the_file_the_sku_already_has(self):
        # ...and it is pulled INTO the row, so the same file is not asked for
        # a second time on a screen that already has it.
        sku = self._sku("Pulled Wardrobe")
        frappe.db.set_value("Estimate SKU", sku.name, "parts_csv",
                            "/files/pull_me.csv", update_modified=False)
        est = frappe.new_doc("Estimate")
        est.project = self.project
        est.work_type = "New Work"
        est.append("skus", {"estimate_sku": sku.name})
        est.insert(ignore_permissions=True)
        est.reload()
        self.assertEqual(est.skus[0].parts_csv, "/files/pull_me.csv")

    def test_a_file_still_claiming_a_cleared_field_is_adopted_back(self):
        # The empty-grid-row bug cleared parts_csv but left the File row saying
        # attached_to_field="parts_csv". Treating that as "already claimed"
        # left the CSV visible in the sidebar, pointing at a field that no
        # longer pointed back, and adopted by nothing.
        sku = self._sku("Orphaned File Wardrobe")
        f = frappe.get_doc({
            "doctype": "File", "file_name": "orphan.csv",
            "content": "Material name,Length,Width\n", "is_private": 1,
            "attached_to_doctype": "Estimate SKU", "attached_to_name": sku.name,
        }).insert(ignore_permissions=True)
        frappe.db.set_value("File", f.name, "attached_to_field", "parts_csv",
                            update_modified=False)
        frappe.db.set_value("Estimate SKU", sku.name, "parts_csv", None,
                            update_modified=False)
        sku.reload()
        sku.adopt_sidebar_attachments()
        self.assertTrue(sku.parts_csv, "the stranded file should be adopted back")


class TestBoardItemsArePurchasingIdentities(MalletTestCase):
    """A ply board is one Item however many décors get pasted on it, and the
    Item that ends up in the database is what proves it — item_code_for can be
    right in isolation and still be bypassed by ensure_material_item."""

    def test_two_decors_one_board_item(self):
        from mallet_estimator import inventory
        a, _r, _s = inventory.ensure_material_item("SG_PLY_V1_a_b", kind="sheet", thickness=16)
        b, _r, _s = inventory.ensure_material_item("SG_PLY_V1_a_c", kind="sheet", thickness=16)
        self.assertEqual(a, b)
        self.assertEqual(a, "SG_PLY_V1_16mm")
        self.assertTrue(frappe.db.exists("Item", "SG_PLY_V1_16mm"))

    def test_the_board_item_carries_no_decor_letters(self):
        # a slot letter means a different laminate on the next project, so
        # stamping one on the stock Item would be false
        from mallet_estimator import inventory
        code, _r, _s = inventory.ensure_material_item("SG_PLY_V1_a_b", kind="sheet", thickness=18)
        row = frappe.db.get_value(
            "Item", code, ["mallet_visible_sides", "mallet_lam_internal", "mallet_lam_external"],
            as_dict=True) or {}
        self.assertEqual(row.get("mallet_visible_sides"), 1)   # grade is real, keep it
        self.assertFalse(row.get("mallet_lam_internal"))
        self.assertFalse(row.get("mallet_lam_external"))

    def test_thickness_still_makes_a_different_board(self):
        from mallet_estimator import inventory
        twelve, _r, _s = inventory.ensure_material_item("SG_PLY_V0_a_a", kind="sheet", thickness=12)
        sixteen, _r, _s = inventory.ensure_material_item("SG_PLY_V0_a_a", kind="sheet", thickness=16)
        self.assertNotEqual(twelve, sixteen)

    def test_the_patch_collapses_and_then_leaves_alone(self):
        from mallet_estimator.patches import collapse_board_item_codes as P
        self.assertEqual(P.collapsed_code("SG_PLY_V1_a_b_16mm"), "SG_PLY_V1_16mm")
        self.assertEqual(P.collapsed_code("SG_LAM_V1_16mm_VM6534"), "SG_LAM_VM6534")
        # already collapsed, and things that are neither: nothing to do
        self.assertIsNone(P.collapsed_code("SG_PLY_V1_16mm"))
        self.assertIsNone(P.collapsed_code("SG_LAM_VM6534"))
        self.assertIsNone(P.collapsed_code("EB_PVC_EX_RH1834"))
        self.assertIsNone(P.collapsed_code("HWD_MiniFix"))

    def test_an_unmapped_laminate_placeholder_is_left_alone(self):
        # stripping the board tokens off a placeholder would merge every
        # unmapped laminate on the site into one meaningless SG_LAM_a_a
        from mallet_estimator.patches import collapse_board_item_codes as P
        self.assertIsNone(P.collapsed_code("SG_LAM_V0_12mm_a_a"))
        self.assertIsNone(P.collapsed_code("SG_LAM_V1_16mm_b_a"))
