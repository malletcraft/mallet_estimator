"""The cost engine's arithmetic, end to end, with numbers that exist.

Everything else in this suite can only check SHAPE — that a row appears, that
a source string is right, that a quantity is what the model said. The one
thing it could never check is whether the money is correct, because every rate
in this repo is 0 by design and 0 x anything agrees with any theory you like.

With synthetic_rates installed the chain is closed: Estimate Settings -> a
salary -> an hourly rate -> operation minutes -> a line amount -> a total.
Each number below is checkable by hand, which is why they were chosen.
"""

import frappe

from mallet_estimator import api, estimator
from mallet_estimator.tests import synthetic_rates as R

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except ImportError:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


# One large assembly, two sheets' worth of parts, 24 minifix housings — the
# shape of a real wardrobe push, small enough to price in your head.
CSV = (
    "No.;Designation;Quantity;Length;Width;Thickness;Material type;"
    "Material name;Edge Length 1;Edge Length 2;Edge Width 1;Edge Width 2;"
    "Frontside;Backside;Tags\n"
    "1;ASMBL_L_WAR;2;2100;600;16;Sheet Goods;SG_PLY_V0_a_a;;;;;;;carcass_vert\n"
    "2;HWD_MiniFix;24;;;;Hardware;HWD_MiniFix;;;;;;;\n"
)


class TestTheMoneyIsRight(MalletTestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # Kept so tearDownClass can put the site back exactly as it was. The
        # suite shares one site; a fixture that does not clean up is a fixture
        # every later test silently inherits.
        cls._before = R.install()

    @classmethod
    def tearDownClass(cls):
        R.clear_prices()
        R.restore(getattr(cls, "_before", None))
        super().tearDownClass()

    def test_a_salary_becomes_an_hourly_rate(self):
        """The first link, and the one every other number rests on.

        20,000 a month over 200 productive hours is 100 an hour. If this is
        wrong nothing downstream can be right, and nothing downstream would
        say so — it would just quote a different number.
        """
        settings = frappe.get_single("Estimate Settings")
        self.assertEqual(estimator.working_hours_per_month(settings),
                         R.PRODUCTIVE_HOURS_PER_MONTH)

        roles = estimator.staff_rates(settings)
        self.assertAlmostEqual(roles["carpenter"], R.CARPENTER_HR, places=6)
        self.assertAlmostEqual(roles["helper"], R.HELPER_HR, places=6)
        self.assertAlmostEqual(roles["designer"], R.DESIGNER_HR, places=6)

    def test_a_workstation_bills_its_crew(self):
        card = api.cost_card()
        by_name = {s["name"]: s for s in card["workstations"]}

        # A carpenter and a helper: 100 + 50.
        self.assertAlmostEqual(by_name["Assembly Station"]["hour_rate"],
                               R.CREW_HR, places=2)
        self.assertAlmostEqual(by_name["Panel Saw"]["hour_rate"],
                               R.CREW_HR, places=2)
        # The designer's desk is crewed by a designer alone.
        self.assertAlmostEqual(by_name["Design Desk"]["hour_rate"],
                               R.DESIGNER_HR, places=2)

    def test_a_rate_is_the_sum_of_its_parts(self):
        """Asserted by composition, not by a magic number.

        This is what covers rent and depreciation, which the fixture zeroes to
        keep the other numbers clean: whatever the components are, the rate
        has to be their total. A component that stopped being counted would
        show up here without anybody having to guess a figure.
        """
        for s in api.cost_card()["workstations"]:
            self.assertAlmostEqual(
                s["hour_rate"], round(sum(v for _c, v in s["components"]), 2),
                places=2, msg="%s does not equal its own components" % s["name"])

    def test_minifix_boring_costs_what_the_standard_says(self):
        """The line that was wrong by double until this morning.

        24 housings x 15 min = 6 hours. At a 150/hr crew that is 900. Both
        halves matter: the standard time comes from the Operation master, the
        rate from a salary, and this is the only test that would notice if
        either silently changed.
        """
        out = api.estimate_preview(CSV)
        row = [r for r in out["labour"] if r["name"] == "Minifix Boring"][0]

        self.assertEqual(row["qty"], 24.0)
        self.assertEqual(row["hours"], 6.0)
        self.assertAlmostEqual(row["hour_rate"], R.CREW_HR, places=2)
        self.assertAlmostEqual(row["amount"], 6.0 * R.CREW_HR, places=2)

    def test_the_labour_total_is_the_sum_of_its_lines(self):
        out = api.estimate_preview(CSV)
        self.assertAlmostEqual(
            out["labour_total"],
            round(sum(r["amount"] for r in out["labour"]), 2), places=2)
        # And it is not zero, which is the assertion this whole file exists to
        # make possible.
        self.assertGreater(out["labour_total"], 0)

    def test_typed_minutes_move_the_money_by_exactly_what_they_should(self):
        base = api.estimate_preview(CSV)
        base_row = [r for r in base["labour"] if r["name"] == "Installation"][0]

        # Double the minutes on one line; its amount must double, and the
        # total must rise by precisely that line's increase and nothing else.
        edited = api.estimate_preview(
            CSV, overrides={"Installation": {"min": base_row["min_per_unit"] * 2}})
        row = [r for r in edited["labour"] if r["name"] == "Installation"][0]

        self.assertAlmostEqual(row["hours"], base_row["hours"] * 2, places=1)
        self.assertAlmostEqual(
            edited["labour_total"] - base["labour_total"],
            row["amount"] - base_row["amount"], places=2)

    def test_a_material_costs_its_assumed_rate(self):
        """The other half of the quote, through the real resolution order.

        material_rate() reads the Estimation (Assumed) price list FIRST, ahead
        of valuation and last-purchase, so a planning rate is a deliberate
        number rather than whatever stock happened to cost. Pricing the item
        that way here exercises that order rather than stepping around it.
        """
        out = api.estimate_preview(CSV, create_missing=1)
        ply = [m for m in out["materials"] if m["code"].startswith("SG_PLY")]
        self.assertTrue(ply, "the fixture CSV produced no sheet-goods line")
        code = ply[0]["code"]

        # An Item Price needs the ERP ITEM code, and estimate_preview reports
        # the OpenCutList one — SG_PLY_V0_a_a is not a docname and pricing it
        # throws. cost_card is the endpoint that carries both, so the mapping
        # comes from the app rather than from a guess about the naming rule.
        card = api.cost_card(codes=code, create_missing=1)
        item = card["materials"][0]["item_code"]
        self.assertTrue(item, "no Item behind %s to price" % code)

        R.price(item, 1000.0)
        priced = api.estimate_preview(CSV)
        line = [m for m in priced["materials"] if m["code"] == code][0]

        self.assertEqual(line["rate"], 1000.0)
        # "erp:assumed", not "assumed": material_rate() returns the bare word
        # and api.py prefixes every source with erp: on the way out. Checked
        # against a real payload rather than guessed a third time.
        self.assertEqual(line["source"], "erp:assumed")
        self.assertTrue(line["quotable"])
        # Deliberately not asserting amount == qty x rate: wastage and landed
        # cost sit between them, and inventing an expected total here would be
        # asserting my reading of the code rather than its behaviour.
        self.assertGreater(line["amount"], 0)
        self.assertGreater(priced["material_total"], 0)

    def test_the_material_total_is_the_sum_of_its_lines(self):
        out = api.estimate_preview(CSV)
        self.assertAlmostEqual(
            out["material_total"],
            round(sum(m["amount"] for m in out["materials"]), 2), places=2)

    def test_the_fixture_refuses_to_run_outside_a_test(self):
        """The guard that makes this file safe to have in the repo at all.

        Estimate Settings on a real site holds the only copy of the real cost
        data and has no undo. install() must be unable to run anywhere else,
        and this asserts the refusal rather than trusting the comment above it.
        """
        frappe.flags.in_test = False
        try:
            with self.assertRaises(RuntimeError):
                R.install()
            with self.assertRaises(RuntimeError):
                R.price("anything", 1.0)
        finally:
            frappe.flags.in_test = True
