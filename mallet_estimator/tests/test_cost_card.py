# The plugin's cost card. ERP is the only authority for money, and every
# number it hands over has to say so — that is the whole contract.
import frappe

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase

from mallet_estimator import api, estimator, inventory


class TestCostCard(MalletTestCase):

    def test_all_seventeen_operations_in_process_order(self):
        # Amit, 2026-08-22: "keep all 17 operations as is . no drop." The
        # plugin quotes the same process the floor works, or it is not a gauge
        # of anything.
        card = api.cost_card()
        ops = card["operations"]
        self.assertEqual(len(ops), len(estimator.OPERATION_STANDARDS))
        self.assertEqual(len(ops), 17)
        self.assertEqual(ops[0]["name"], "Sheet Lamination")   # pasting
        self.assertEqual(ops[-2]["name"], "Installation")      # …to installation
        self.assertEqual([o["seq"] for o in ops], list(range(1, len(ops) + 1)))

    def test_every_operation_carries_a_workstation_and_a_rate_source(self):
        for o in api.cost_card()["operations"]:
            self.assertTrue(o["workstation"], f"{o['name']} has no workstation")
            self.assertIn(o["rate_source"], ("erp:Workstation", "unset"))
            self.assertIn(o["min_source"], ("erp:Operation", "code default"))

    def test_the_envelope_names_erp_as_the_authority(self):
        # The plugin stores its own material rates and must override them with
        # these. A payload that did not say where it came from would leave a
        # person unable to tell one from the other, which is exactly what Amit
        # asked to be able to see.
        card = api.cost_card()
        self.assertEqual(card["authority"], "erp")
        self.assertEqual(card["price_list"], inventory.ESTIMATION_PRICE_LIST)
        self.assertIn("post-tax", card["rates_are"])
        self.assertEqual(card["productive_min_per_day"],
                         estimator.PRODUCTIVE_MIN_PER_DAY)

    def test_the_assembly_rule_is_declared_not_hardcoded_on_the_plugin(self):
        rule = api.cost_card()["assembly_rule"]
        self.assertEqual(rule["operation"], "Assembly")
        self.assertEqual(rule["component_prefix"], "ASMBL")
        self.assertTrue(rule["editable"])
        self.assertIn("Assembly", estimator.OPERATION_STANDARDS)

    def test_a_material_erp_has_never_heard_of_is_named_not_hidden(self):
        # The failure that matters: the plugin has its own rate for this and
        # would happily quote it. Coming back "not in erp" and NOT quotable is
        # what stops a client being shown a number from the wrong place.
        card = api.cost_card(codes="SG_PLY_V9_NOSUCHTHING_99mm")
        row = card["materials"][0]
        self.assertEqual(row["source"], "not in erp")
        self.assertFalse(row["quotable"])
        self.assertEqual(row["landed_rate"], 0.0)

    def test_a_known_material_comes_back_post_tax_with_its_source(self):
        item = frappe.db.get_value("Item", {"item_group": ["like", "%"]}, "name")
        if not item:
            self.skipTest("no items on this bench")
        row = api.cost_card(codes=item)["materials"][0]
        self.assertEqual(row["item_code"], item)
        self.assertTrue(row["source"].startswith("erp:"))
        # landed = base grossed up by the item's GST; never quietly equal when
        # a GST percentage applies.
        if row["base_rate"] and row["gst_pct"]:
            self.assertGreater(row["landed_rate"], row["base_rate"])

    def test_create_missing_mints_the_item_but_never_a_rate(self):
        # Amit, 2026-08-22: "ADD material TO ERP." ERP is the master, so a
        # material the model uses and ERP has not seen is a gap in ERP. What
        # gets created is the Item — a fact about the model. The RATE is a
        # decision somebody has to make, and stays a human act on the
        # Estimation (Assumed) price list.
        code = "SG_PLY_V1_zzt_zzt_18mm"
        frappe.db.delete("Item", {"item_code": ["like", "%zzt_zzt%"]})
        before = api.cost_card(codes=code)["materials"][0]
        self.assertEqual(before["source"], "not in erp")

        after = api.cost_card(codes=code, create_missing=1)
        row = after["materials"][0]
        self.assertTrue(row["item_code"], "no Item was created")
        self.assertIn(row["item_code"], after["created_items"])
        # Created, and deliberately NOT quotable — it has no rate yet, and
        # saying so is the whole point.
        self.assertFalse(row["quotable"])
        self.assertEqual(row["landed_rate"], 0.0)

        # …and it is idempotent: a second call finds it rather than minting
        # a second one.
        again = api.cost_card(codes=code, create_missing=1)
        self.assertEqual(again["created_items"], [])
        self.assertEqual(again["materials"][0]["item_code"], row["item_code"])

    def test_create_missing_will_not_mint_an_item_from_a_typo(self):
        # The OpenCutList grammar is the gate. A component somebody named
        # "Group#3" must never become an Item.
        junk = "Group#3 copy"
        out = api.cost_card(codes=junk, create_missing=1)
        self.assertEqual(out["created_items"], [])
        self.assertEqual(out["materials"][0]["source"], "not in erp")

    def test_the_card_states_the_wastage_rule_both_sides_must_use(self):
        # "wastage treatment - charge full board" — whole boards consumed, not
        # the area the parts occupy. Stated in the payload so the plugin and
        # the bench cannot drift into quoting two different numbers.
        self.assertIn("full board", api.cost_card()["wastage"])

    def test_codes_accept_a_list_a_csv_string_or_nothing(self):
        self.assertEqual(api.cost_card()["materials"], [])
        self.assertEqual(len(api.cost_card(codes="A_ONE,A_TWO")["materials"]), 2)
        self.assertEqual(len(api.cost_card(codes='["A_ONE","A_TWO"]')["materials"]), 2)
        self.assertEqual(len(api.cost_card(codes=["A_ONE"])["materials"]), 1)
