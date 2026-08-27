# Pure unit tests for the cost engine — no database, no frappe. Run locally with
#   python -m unittest mallet_estimator.tests.test_estimator
# and in CI under `bench run-tests --app mallet_estimator`.
import types
import unittest

from mallet_estimator import estimator as E


def _settings(**over):
    base = dict(
        monthly_rent=60000, working_days_per_month=26, working_hours_per_day=8,
        carpenter_rate=157, helper_rate=107, design_rate=500, design_flat=0,
        markup_material=15, markup_labor=20, markup_overhead=20, markup_design=20,
    )
    base.update(over)
    return types.SimpleNamespace(**base)


class TestWorkstationRates(unittest.TestCase):
    def test_modular_components(self):
        # OPS3: modular per-role components in canonical order; zero-value
        # components are skipped (cost seed values are ### — never in the repo,
        # so code-side seeds are zeros and only rent + wages appear here).
        rates = {w["name"]: w for w in E.workstation_rates(_settings())}
        comps = [c for c, _ in rates["Panel Saw"]["components"]]
        self.assertIn("Rent", comps)
        self.assertIn("Carpenter Wage", comps)
        self.assertIn("Helper Wage", comps)
        # order always respects WS_COMPONENTS
        idx = [E.WS_COMPONENTS.index(c) for c in comps]
        self.assertEqual(idx, sorted(idx))
        # a synthetic capital produces a Depreciation component of its own
        orig = E.WORKSTATIONS[0]["capital"]
        try:
            E.WORKSTATIONS[0]["capital"] = 120000
            r2 = {w["name"]: w for w in E.workstation_rates(_settings())}
            self.assertIn("Depreciation", [c for c, _ in r2["Panel Saw"]["components"]])
        finally:
            E.WORKSTATIONS[0]["capital"] = orig
        # D1: the Design Desk is crewed by the designer only.
        dcomps = [c for c, _ in rates["Design Desk"]["components"]]
        self.assertIn("Designer Wage", dcomps)
        self.assertNotIn("Carpenter Wage", dcomps)

    def test_salary_calendar_rates(self):
        # L1: SYNTHETIC salaries (real figures are sensitive, never in the repo).
        # salary + bonus month over (26 − 2 paid − 10/12 natl days) x 7 hrs.
        s = _settings(carpenter_salary=13000, helper_salary=6500, bonus_months=1,
                      paid_holidays_per_month=2, national_holidays_per_year=10,
                      lunch_hours_per_day=1)
        self.assertAlmostEqual(E.working_days_per_month(s), 26 - 2 - 10 / 12.0, places=4)
        self.assertAlmostEqual(E.working_hours_per_month(s), (26 - 2 - 10 / 12.0) * 7, places=4)
        r = E.staff_rates(s)
        self.assertAlmostEqual(r["carpenter"], 13000 * 13 / 12.0 / ((26 - 2 - 10 / 12.0) * 7), places=4)
        self.assertAlmostEqual(r["helper"], 6500 * 13 / 12.0 / ((26 - 2 - 10 / 12.0) * 7), places=4)

    def test_legacy_hourly_fallback(self):
        # No salaries keyed -> the old hourly fields still price (back-compat).
        r = E.staff_rates(_settings())
        self.assertEqual(r["carpenter"], 157)
        self.assertEqual(r["helper"], 107)

    def test_wages_is_two_person_crew(self):
        r = {w["name"]: w for w in E.workstation_rates(_settings())}["Assembly Station"]
        self.assertAlmostEqual(r["wages_hr"], 157 + 107, places=2)

    def test_net_is_sum_of_components(self):
        for r in E.workstation_rates(_settings()):
            self.assertAlmostEqual(r["net_hr"], sum(v for _, v in r["components"]), places=6)

    def test_onsite_has_no_rent(self):
        r = {w["name"]: w for w in E.workstation_rates(_settings())}["On-Site"]
        self.assertEqual(r["rent_hr"], 0)


class TestCalcSku(unittest.TestCase):
    def _row(self, ws, qty, mins):
        return types.SimpleNamespace(
            phase="Sheet Cutting", workstation=ws, qty=qty, carp_min=mins,
            is_misc=0, carp_total=0, helper_total=0, op_cost=0,
        )

    def test_total_min_and_phase_cost(self):
        s = _settings()
        rate = {"rent_hr": 24.48, "wages_hr": 250, "machine_hr": 0, "elec_hr": 10,
                "consumable_hr": 40, "net_hr": 324.48, "labour_hr": 250, "dep_hr": 0, "total_hr": 324.48}
        row = self._row("Pasting Station", 9, 15)
        sku = types.SimpleNamespace(labor=[row], materials=[], design_hours=0, design_flat=0, include_misc=0)
        E.calc_sku(sku, s, ws_rates={"Pasting Station": rate})
        self.assertEqual(row.carp_total, 135)                      # qty x min
        self.assertAlmostEqual(row.op_cost, 324.48 * 135 / 60, 2)  # net rate x hours

    def test_breakdown_balances_to_op_cost(self):
        s = _settings()
        rate = {"rent_hr": 111, "wages_hr": 264, "machine_hr": 0, "elec_hr": 50,
                "consumable_hr": 60, "net_hr": 485, "labour_hr": 264, "dep_hr": 0, "total_hr": 485}
        row = self._row("Panel Saw", 3, 20)
        sku = types.SimpleNamespace(labor=[row], materials=[], design_hours=0, design_flat=0, include_misc=0)
        out = E.calc_sku(sku, s, ws_rates={"Panel Saw": rate})
        self.assertAlmostEqual(out["labor_cost"] + out["overhead_cost"], row.op_cost, 2)

    def test_material_cost_and_markup(self):
        s = _settings()
        mat = types.SimpleNamespace(line_cost=1000)
        sku = types.SimpleNamespace(labor=[], materials=[mat], design_hours=0, design_flat=0, include_misc=0)
        out = E.calc_sku(sku, s, ws_rates={})
        self.assertEqual(out["material_cost"], 1000)
        self.assertAlmostEqual(out["client_material"], 1000 * 1.15, 2)  # 15% markup


    def test_a_settings_record_without_design_rate_does_not_explode(self):
        # Amit, 2026-08-26, pressing Recompute on a subcontract SKU: the whole
        # form came back as a raw AttributeError traceback.
        #
        # There IS no design_rate field on Estimate Settings — only
        # designer_salary and markup_design — so `settings.design_rate` raised
        # on any Frappe document reaching this branch. It hid for months
        # because the branch only runs when a SKU has NO design rows, and every
        # article SKU has them. The first SKU without any was a subcontract
        # one.
        #
        # A settings object missing the field is the NORMAL case, so the test
        # uses one, and asserts the honest answer: unset design rate means zero
        # design cost, not a crash.
        s = types.SimpleNamespace(
            markup_material=15, markup_labor=0, markup_overhead=0,
            markup_design=0, misc_pct=0)
        sku = types.SimpleNamespace(labor=[], materials=[], design_hours=8,
                                    design_flat=0, include_misc=0)
        out = E.calc_sku(sku, s, ws_rates={})
        self.assertEqual(out["design_cost"], 0)

    def test_a_flat_design_fee_is_still_charged_without_a_rate(self):
        # The other half: design_flat is a number somebody typed, and it must
        # survive the missing rate rather than being zeroed with it.
        s = types.SimpleNamespace(
            markup_material=15, markup_labor=0, markup_overhead=0,
            markup_design=0, misc_pct=0)
        sku = types.SimpleNamespace(labor=[], materials=[], design_hours=8,
                                    design_flat=5000, include_misc=0)
        out = E.calc_sku(sku, s, ws_rates={})
        self.assertEqual(out["design_cost"], 5000)


class TestOpPhase(unittest.TestCase):
    def test_prefers_operation_link_over_legacy_phase(self):
        row = types.SimpleNamespace(operation="Drilling", phase="Sheet Cutting", is_misc=0)
        self.assertEqual(E.op_phase(row), "Drilling")

    def test_falls_back_to_legacy_phase(self):
        row = types.SimpleNamespace(operation=None, phase="Grooving", is_misc=0)
        self.assertEqual(E.op_phase(row), "Grooving")

    def test_misc_row_uses_sanitized_operation_name(self):
        row = types.SimpleNamespace(operation=None, phase=None, is_misc=1)
        self.assertEqual(E.op_phase(row), "Miscellaneous - extra")
        self.assertEqual(E.op_phase(row), E.MISC_OPERATION)


class TestCodes(unittest.TestCase):
    def test_customer_initials(self):
        self.assertEqual(E.customer_initials("Yogesh Sahasrabudhe"), "YS")

    def test_sku_code(self):
        self.assertEqual(E.sku_code("Yogesh Sahasrabudhe", "Master Bedroom", "Wardrobe"), "YS_MB_WAR")



class TestDecor(unittest.TestCase):
    def test_bcn_standard(self):
        from mallet_estimator import decor
        v = decor.parse_slot_value("Merino 1834 Moonlit Gray")
        self.assertEqual((v["brand"], v["catalogue"], v["name"]), ("Merino", "1834", "Moonlit Gray"))
        v = decor.parse_slot_value("RT 6575")  # alias + name optional
        self.assertEqual((v["brand"], v["catalogue"], v["name"]), ("Royal Touch", "6575", ""))
        v = decor.parse_slot_value("Royal Touch 6575 Black Marmor")
        self.assertEqual(v["brand"], "Royal Touch")
        # multi-word maker + initials alias, straight from the maker list
        v = decor.parse_slot_value("Virgo Mica 1834 Grey")
        self.assertEqual((v["brand"], v["catalogue"], v["name"]), ("Virgo Mica", "1834", "Grey"))
        v = decor.parse_slot_value("VM 1834")
        self.assertEqual(v["brand"], "Virgo Mica")
        # a NEW maker supplied via the live list is recognised without code changes
        v = decor.parse_slot_value("Greenlam 204 Teak", brands=["Greenlam", "Merino"])
        self.assertEqual(v["brand"], "Greenlam")

    def test_legacy_freeform(self):
        from mallet_estimator import decor
        v = decor.parse_slot_value("YS_6534_MOONLIT_BED_Laminate")
        self.assertIsNone(v["brand"])
        self.assertEqual(v["raw"], "YS_6534_MOONLIT_BED_Laminate")

    def test_material_slots(self):
        from mallet_estimator import decor
        self.assertEqual(decor.material_slots("SG_PLY_V2_b_c"), ["b", "c"])
        self.assertEqual(decor.material_slots("SG_LAM_V1_16mm_b_a"), ["b"])
        self.assertEqual(decor.material_slots("EB_PVC_EX_c"), ["c"])
        self.assertEqual(decor.material_slots("SG_PLY_V0_a_a"), [])

    def test_panel_slots(self):
        # A panel names a laminate per FACE — slot_key keeps only the deciding
        # first letter (right for SG_LAM), panel_slots keeps both (for SG_PLY,
        # where the second face's laminate must exist in the décor map even
        # before any SG_LAM line arrives to claim it).
        from mallet_estimator import decor
        self.assertEqual(decor.panel_slots("SG_PLY_V1_a_b"), ["a", "b"])
        self.assertEqual(decor.panel_slots("SG_PLY_V0_a_a"), ["a"])
        self.assertEqual(decor.panel_slots("SG_PLY_V2_b_c1"), ["b1", "c1"])
        self.assertEqual(decor.panel_slots("SG_PLY_V0_12mm"), [])
        self.assertEqual(decor.slot_key("SG_PLY_V1_a_b"), "a")

    def test_multi_slot_description(self):
        from mallet_estimator import decor
        out = decor.parse_description("b=Merino 6534; c=RT 6575 Black Marmor", "SG_PLY_V2_b_c")
        self.assertEqual(out["b"]["brand"], "Merino")
        self.assertEqual(out["c"]["catalogue"], "6575")

    def test_suffixed_slots(self):
        # paste-rename décor instances: b1 is a DIFFERENT décor than b, in the
        # description parser and the slot extractors alike
        from mallet_estimator import decor
        out = decor.parse_description("b1=Merino 6534; c1=RT 6575", "SG_PLY_V2_b1_c1")
        self.assertEqual(out["b1"]["brand"], "Merino")
        self.assertEqual(out["c1"]["catalogue"], "6575")
        self.assertEqual(decor.material_slots("SG_PLY_V2_b1_c1"), ["b1", "c1"])
        self.assertEqual(decor.material_slots("EB_PVC_EX_c1"), ["c1"])
        # single letters untouched (backward compatible)
        self.assertEqual(decor.material_slots("SG_PLY_V2_b_c"), ["b", "c"])
        v = decor.parse_slot_value("b1 = Merino 1834 Moonlit Gray")
        self.assertEqual((v["brand"], v["catalogue"]), ("Merino", "1834"))

    def test_real_code_substitution(self):
        # S9v2 — the user's exact convention: trailing letters replaced by the
        # FIRST letter's décor short code; one PO code per laminate.
        from mallet_estimator import decor
        ss = {"b": "VM6534", "a": "GE1834"}
        # The board's grade and thickness drop out: they describe what the sheet
        # gets pressed ONTO, and one décor is one stock item however many boards
        # it lands on.
        self.assertEqual(decor.substitute_real_code("SG_LAM_V0_a_a", ss)[0], "SG_LAM_GE1834")
        self.assertEqual(decor.substitute_real_code("SG_LAM_V1_16mm_a_b", ss)[0], "SG_LAM_GE1834")
        self.assertEqual(decor.substitute_real_code("SG_LAM_V0_12mm_a_a", ss)[0], "SG_LAM_GE1834")
        self.assertEqual(decor.substitute_real_code("SG_LAM_V1_16mm_b_a", ss)[0], "SG_LAM_VM6534")
        # edge bands carry no board attributes, so they pass through untouched
        self.assertEqual(decor.substitute_real_code("EB_PVC_EX_b", ss)[0], "EB_PVC_EX_VM6534")
        # no description -> the placeholder itself stays the item
        self.assertEqual(decor.substitute_real_code("SG_LAM_V0_12mm_a_a", {}), ("SG_LAM_V0_12mm_a_a", None))
        # ply codes are untouched by design (no substitution is ever called on them)

    def test_short_codes(self):
        from mallet_estimator import decor
        self.assertEqual(decor.short_code({"brand": "Virgo Mica", "catalogue": "6534"}), "VM6534")
        self.assertEqual(decor.short_code({"brand": "Merino", "catalogue": "1834"}), "ME1834")
        self.assertEqual(decor.short_code({"short": "GE", "brand": "Greenlam", "catalogue": "1834"}), "GE")
        self.assertTrue(decor.short_code({"raw": "YS_6534_MOONLIT"}))

    def test_extract_from_pdf_text(self):
        from mallet_estimator import decor
        text = ("SG_LAM_V1_16mm_b_a / 1 mm\n"
                "b=Merino 1834 Moonlit Gray\n"
                "3 4.16 m²8.92 m² - -12 Rs34 Rs\n"
                "EB_PVC_EX_b / 1 mm x 22 mm\n"
                "b=RT 6575\n")
        out = decor.extract_slot_map(text)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0]["placeholder"], "SG_LAM_V1_16mm_b_a")
        self.assertEqual(out[0]["brand"], "Merino")
        self.assertEqual(out[1]["brand"], "Royal Touch")


class TestDecorBlocks(unittest.TestCase):
    BLOCK = ("b = External Laminate\n"
             "Brand = Virgo Mica\n"
             "Code = 1834\n"
             "Name = Moonlight\n"
             "Year = 2025-26\n"
             "a = Internal Laminate\n"
             "Brand = Serplex\n"
             "Code = 1834\n"
             "Name = Fabric\n")

    def test_labelled_block(self):
        from mallet_estimator import decor
        out = decor.parse_description(self.BLOCK, "SG_LAM_V1_16mm_b_a",
                                      brands=["Virgo Mica", "Serplex"])
        self.assertEqual(out["b"]["brand"], "Virgo Mica")
        self.assertEqual(out["b"]["year"], "2025-26")
        self.assertEqual(out["b"]["title"], "External Laminate")
        self.assertEqual(out["a"]["brand"], "Serplex")
        self.assertEqual(out["a"]["name"], "Fabric")

    def test_unknown_maker_accepted_when_labelled(self):
        from mallet_estimator import decor
        out = decor.parse_description("b = Ext\nBrand = Greenlam\nCode = 204\n",
                                      "SG_LAM_V1_16mm_b_a")
        self.assertEqual(out["b"]["brand"], "Greenlam")  # explicit label = intent

    def test_initials_canonicalize_in_block(self):
        from mallet_estimator import decor
        out = decor.parse_description("b = Ext\nBrand = VM\nCode = 1834\n",
                                      "SG_LAM_V1_16mm_b_a", brands=["Virgo Mica"])
        self.assertEqual(out["b"]["brand"], "Virgo Mica")

    def test_extract_block_from_pdf_text(self):
        from mallet_estimator import decor
        text = ("SG_LAM_V1_16mm_b_a / 1 mm\n" + self.BLOCK +
                "3 4.16 m²8.92 m² - -12 Rs34 Rs\n")
        out = decor.extract_slot_map(text, brands=["Virgo Mica", "Serplex"])
        slots = {(e["placeholder"], e["slot"]) for e in out}
        self.assertIn(("SG_LAM_V1_16mm_b_a", "b"), slots)
        self.assertIn(("SG_LAM_V1_16mm_b_a", "a"), slots)


class TestLineDiscountTax(unittest.TestCase):
    """Per-line discount + tax arithmetic (the pure part of price_material_lines):
    stock prices are PRE-tax, discount applies to the line only, tax follows the
    line's applied rate or the policy rate."""

    @staticmethod
    def _price(qty, rate, disc_pct, policy_pct, applied_pct=None, client_supplied=False):
        net_rate = rate * (1 - disc_pct / 100.0)
        discount = qty * rate * disc_pct / 100.0
        line_cost = qty * net_rate
        pct = policy_pct if applied_pct is None else applied_pct
        tax = line_cost * pct / 100.0
        if client_supplied:
            line_cost = discount = tax = 0
        return {"net_rate": net_rate, "discount": discount, "line_cost": line_cost, "tax": tax}

    def test_discount_applies_to_line_only(self):
        r = self._price(10, 100, 10, 18)
        self.assertAlmostEqual(r["net_rate"], 90)
        self.assertAlmostEqual(r["discount"], 100)
        self.assertAlmostEqual(r["line_cost"], 900)

    def test_tax_follows_policy_then_override(self):
        self.assertAlmostEqual(self._price(10, 100, 0, 18)["tax"], 180)
        self.assertAlmostEqual(self._price(10, 100, 0, 18, applied_pct=12)["tax"], 120)
        # tax is charged on the DISCOUNTED value, not the list value
        self.assertAlmostEqual(self._price(10, 100, 10, 18)["tax"], 162)

    def test_client_supplied_costs_us_nothing(self):
        r = self._price(10, 100, 10, 18, client_supplied=True)
        self.assertEqual((r["line_cost"], r["discount"], r["tax"]), (0, 0, 0))


class TestTaxDiscount(unittest.TestCase):
    """The full line model: MRP -> discount -> taxable -> std tax vs applied
    tax -> tax discount -> landed."""

    @staticmethod
    def _line(qty, mrp, disc_pct, std_pct, applied_pct=None):
        net_rate = mrp * (1 - disc_pct / 100.0)
        taxable = qty * net_rate
        applied = std_pct if applied_pct is None else applied_pct
        tax_disc_pct = std_pct - applied
        return {
            "net_rate": net_rate,
            "discount": qty * mrp * disc_pct / 100.0,
            "taxable": taxable,
            "tax_discount_pct": tax_disc_pct,
            "tax_saved": taxable * tax_disc_pct / 100.0,
            "tax": taxable * applied / 100.0,
            "landed": taxable + taxable * applied / 100.0,
        }

    def test_full_line_chain(self):
        r = self._line(10, 100, 10, 18, 12)
        self.assertAlmostEqual(r["net_rate"], 90)
        self.assertAlmostEqual(r["discount"], 100)
        self.assertAlmostEqual(r["taxable"], 900)
        self.assertAlmostEqual(r["tax_discount_pct"], 6)
        self.assertAlmostEqual(r["tax_saved"], 54)     # 900 x 6%
        self.assertAlmostEqual(r["tax"], 108)          # 900 x 12%
        self.assertAlmostEqual(r["landed"], 1008)
        # policy tax would have been 162 — saved is exactly the difference
        self.assertAlmostEqual(900 * 0.18 - r["tax"], r["tax_saved"])

    def test_no_override_means_no_tax_discount(self):
        r = self._line(10, 100, 0, 18)
        self.assertAlmostEqual(r["tax_discount_pct"], 0)
        self.assertAlmostEqual(r["tax_saved"], 0)
        self.assertAlmostEqual(r["landed"], 1180)

    def test_above_policy_rate_is_negative_saving(self):
        r = self._line(1, 100, 0, 12, 18)
        self.assertAlmostEqual(r["tax_discount_pct"], -6)
        self.assertAlmostEqual(r["tax_saved"], -6)


class TestSingleSkuGrid(unittest.TestCase):
    """The estimate screen collapsed three tables (intake grid, SKUs grid,
    files panel) into ONE grid, so the SKUs child table now has to carry the
    intake columns as well. These live in JSON, which nothing else type-checks
    — a stray edit would quietly take the intake away and leave no way to
    attach a Part List CSV without opening the SKU."""

    def _child(self):
        import json
        import os
        here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path = os.path.join(here, "mallet_estimator", "doctype",
                            "execution_estimate_sku", "execution_estimate_sku.json")
        with open(path) as fh:
            return json.load(fh)

    def test_grid_carries_the_intake_columns(self):
        fields = {f["fieldname"]: f for f in self._child()["fields"]}
        for fn in ("parts_csv", "estimate_pdf", "views_pdf"):
            self.assertEqual(fields[fn]["fieldtype"], "Attach", fn)
            self.assertTrue(fields[fn].get("in_list_view"), f"{fn} must be a grid column")

    def test_derived_columns_are_read_only(self):
        # mode / sheets are a VIEW of the SKU; typing over them would invent a
        # second truth the next save silently overwrites
        fields = {f["fieldname"]: f for f in self._child()["fields"]}
        for fn in ("estimation_mode", "sheets", "client_total", "est_days"):
            self.assertTrue(fields[fn].get("in_list_view"), f"{fn} must be a grid column")
        for fn in ("estimation_mode", "sheets"):
            self.assertTrue(fields[fn].get("read_only"), f"{fn} must be read-only")



class TestUnkeyedTaxPolicy(unittest.TestCase):
    """A GST business must never quietly drop GST off a line.

    Item.mallet_gst_pct reads back as 0 when nobody has keyed it, which is
    indistinguishable from "this item is zero-rated" — and reading 0 as a real
    policy took the tax off every material line on the site. Unkeyed falls
    back to the house rate; genuine exemption is expressed by overriding the
    APPLIED rate, which stays visible next to the policy.
    """

    def _policy(self, item_value, house=18.0):
        # mirrors the one expression under test in price_material_lines
        return float(item_value) if item_value else house

    def test_unkeyed_item_uses_the_house_rate(self):
        for unkeyed in (None, "", 0, 0.0):
            self.assertEqual(self._policy(unkeyed), 18.0, f"{unkeyed!r} must fall back")

    def test_a_keyed_rate_wins(self):
        self.assertEqual(self._policy(12), 12.0)
        self.assertEqual(self._policy("5"), 5.0)


class TestClientSuppliedIsPricedButNotCosted(unittest.TestCase):
    """Client-supplied material is not money we spend, but an estimate that
    hides its value cannot be read as the whole job. It keeps full pricing on
    the line and is excluded from cost."""

    def _sku(self):
        lines = [
            types.SimpleNamespace(line_cost=1000, customer_supplied=0),
            types.SimpleNamespace(line_cost=4000, customer_supplied=1),
        ]
        return types.SimpleNamespace(
            materials=lines, labor=[], design_labor=[], joinery_items=[],
            include_misc=0, use_custom_margins=0, design_hours=0, design_flat=0,
            trips_tempo=0, trips_ext_lam=0, trips_client_hw=0, trips_outward=0,
        )

    def test_cost_counts_only_what_we_buy(self):
        r = E.calc_sku(self._sku(), _settings(), ws_rates={})
        self.assertEqual(r["material_cost"], 1000)

    def test_the_client_line_keeps_its_own_amount(self):
        # the line object is untouched — the estimate can still show it
        sku = self._sku()
        E.calc_sku(sku, _settings(), ws_rates={})
        self.assertEqual(sku.materials[1].line_cost, 4000)



class TestTaxDiscount(unittest.TestCase):
    """The scheme sets the standard rate; a user knocks percentage points off
    it. Deriving the applied rate from the discount (rather than the other way
    round) is what makes "how much have I taken off?" readable on the line
    instead of a subtraction done in the reader's head."""

    def applied(self, standard, discount):
        # mirrors price_material_lines: clamp, then subtract
        d = min(max(discount, 0.0), standard)
        return standard - d, d

    def test_a_discount_comes_off_the_standard_rate(self):
        self.assertEqual(self.applied(18.0, 6.0), (12.0, 6.0))

    def test_no_discount_charges_the_standard_rate(self):
        self.assertEqual(self.applied(18.0, 0.0), (18.0, 0.0))

    def test_a_discount_cannot_take_the_rate_below_zero(self):
        # 25 points off an 18% rate is 0%, not -7%
        self.assertEqual(self.applied(18.0, 25.0), (0.0, 18.0))

    def test_a_negative_discount_cannot_overcharge(self):
        # otherwise a stray minus sign charges more tax than the scheme allows
        self.assertEqual(self.applied(18.0, -5.0), (18.0, 0.0))


class TestNameSplitting(unittest.TestCase):
    """A name is written for a person to read, so people separate words
    however reads best. Space, underscore and hyphen all mean the same thing
    to the code generator — the grammar must not depend on which key someone
    happened to press."""

    def test_every_word_keeps_three_letters_and_stays_a_separate_word(self):
        # "BC" is not a name, it is a puzzle. Three letters per word, rejoined
        # with underscores, so the abbreviation still reads as what it came from
        self.assertEqual(E.abbr("Base Cabinet"), "BAS_CAB")
        self.assertEqual(E.abbr("Study Table"), "STU_TAB")
        self.assertEqual(E.abbr("Wardrobe-Left"), "WAR_LEF")
        self.assertEqual(E.abbr("Wardrobe-Right"), "WAR_RIG")

    def test_a_word_with_fewer_than_three_letters_gives_what_it_has(self):
        # nothing can invent a third character out of "TV"
        self.assertEqual(E.abbr("TV"), "TV")
        self.assertEqual(E.abbr("TV Unit"), "TV_UNI")
        self.assertEqual(E.abbr("Wardrobe Option A"), "WAR_OPT_A")

    def test_all_three_separators_split_the_same(self):
        for name in ("Wardrobe Option A", "Wardrobe_Option_A", "Wardrobe-Option-A"):
            self.assertEqual(E.abbr(name), "WAR_OPT_A", name)
            self.assertEqual(E.initials(name), "WOA", name)

    def test_a_single_word_takes_its_first_three_letters(self):
        self.assertEqual(E.abbr("Wardrobe"), "WAR")
        self.assertEqual(E.abbr("Loft"), "LOF")

    def test_an_underscore_name_no_longer_swallows_the_separator(self):
        # MB_WAR_CSV read as ONE word gave "MB_" — the separator ended up IN
        # the code and the article itself never appeared. Read as three words
        # each part survives, and no part is a bare separator.
        self.assertEqual(E.abbr("MB_WAR_CSV"), "MB_WAR_CSV")
        self.assertNotIn("__", E.abbr("MB_WAR_CSV"))

    def test_a_one_word_room_takes_three_letters_too(self):
        # "K" is a letter, not a room. Multi-word rooms already read fine as
        # initials, so those are left alone.
        self.assertEqual(E.room_abbr("Kitchen"), "KIT")
        self.assertEqual(E.room_abbr("Study"), "STU")
        self.assertEqual(E.room_abbr("Balcony"), "BAL")
        self.assertEqual(E.room_abbr("Master Bedroom"), "MB")
        self.assertEqual(E.room_abbr("Living Room"), "LR")
        self.assertEqual(E.room_abbr("All Rooms"), "AR")
        self.assertEqual(E.room_abbr(""), "")

    def test_the_whole_code_reads_customer_room_article(self):
        self.assertEqual(E.sku_code("Yogesh Sahasrabudhe", "Master Bedroom", "Wardrobe"),
                         "YS_MB_WAR")
        self.assertEqual(E.sku_code("Yogesh Sahasrabudhe", "Kitchen", "Base Cabinet"),
                         "YS_KIT_BAS_CAB")

    def test_empty_and_separator_only_names_are_survivable(self):
        for junk in ("", "   ", "___", "-", None):
            self.assertEqual(E.abbr(junk), "")
            self.assertEqual(E.initials(junk), "")


if __name__ == "__main__":
    unittest.main()


class TestPerUnitLanded(unittest.TestCase):
    """A line total answers "what does this material cost". Per unit answers
    "is that the right price for a sheet", which is the question someone
    holding a supplier's quote is actually asking."""

    def landed_per_unit(self, net_rate, standard, discount):
        d = min(max(discount, 0.0), standard)
        return round(net_rate * (1 + (standard - d) / 100.0), 2)

    def test_the_standard_rate_lands_a_sheet_at_the_full_amount(self):
        # 2208 pre-tax at 18% -> 2605.44
        self.assertEqual(self.landed_per_unit(2208.0, 18.0, 0.0), 2605.44)

    def test_a_concession_lands_it_lower(self):
        # 9 points off 18 is 9% applied -> 2406.72
        self.assertEqual(self.landed_per_unit(2208.0, 18.0, 9.0), 2406.72)

    def test_what_the_concession_saved_is_the_difference(self):
        full = self.landed_per_unit(2208.0, 18.0, 0.0)
        cut = self.landed_per_unit(2208.0, 18.0, 9.0)
        self.assertAlmostEqual(full - cut, 2208.0 * 9 / 100.0, places=2)


class TestPanelIdentity(unittest.TestCase):
    """A panel saw cuts the sandwich, so what shares a sheet is the pasted
    assembly, not the ply code. `a` is always the internal face; `b` onwards
    are always external (Amit, 2026-08-09)."""

    def faces(self, code):
        from mallet_estimator import decor
        return decor.panel_faces(code)

    def key(self, code, th, shorts):
        from mallet_estimator import decor
        return decor.panel_key(code, th, shorts)

    SHORTS = {"a": "GE1834", "b": "ME1834", "b1": "VM6534", "c": "RT6575"}

    def test_a_is_internal_and_b_onwards_external(self):
        self.assertEqual(self.faces("SG_PLY_V1_a_b"), ("a", "b"))
        self.assertEqual(self.faces("SG_PLY_V1_b_a"), ("a", "b"))
        self.assertEqual(self.faces("SG_PLY_V1_a_b1"), ("a", "b1"))
        self.assertEqual(self.faces("SG_PLY_V1_a_c"), ("a", "c"))

    def test_an_internal_board_is_laminated_both_sides_with_a(self):
        self.assertEqual(self.faces("SG_PLY_V0_a_a"), ("a", "a"))

    def test_internal_boards_pool_across_the_whole_project(self):
        # `a` is one décor for a project, so every article's V0 panels match
        w = self.key("SG_PLY_V0_a_a", 16, self.SHORTS)
        b = self.key("SG_PLY_V0_a_a", 16, self.SHORTS)
        self.assertEqual(w, b)
        self.assertIn("V0", w)

    def test_two_externals_never_pool_however_alike_the_ply_code_looks(self):
        # the wardrobe in Merino and the bed in Virgo Mica are the same string
        # in OpenCutList and two different pasted panels in the workshop
        wardrobe = self.key("SG_PLY_V1_a_b", 16, {"a": "GE1834", "b": "ME1834"})
        bed = self.key("SG_PLY_V1_a_b", 16, {"a": "GE1834", "b": "VM6534"})
        self.assertNotEqual(wardrobe, bed)

    def test_thickness_separates_panels(self):
        self.assertNotEqual(self.key("SG_PLY_V0_a_a", 16, self.SHORTS),
                            self.key("SG_PLY_V0_a_a", 18, self.SHORTS))

    def test_an_unmapped_external_has_no_panel_identity(self):
        # nothing has said what it is; guessing it into someone else's sheet
        # is the error the key exists to prevent
        self.assertIsNone(self.key("SG_PLY_V1_a_d", 16, self.SHORTS))


def _ply_item(name, thickness):
    """inventory.item_code_for's ply branch, without the frappe import so this
    runs in CI's no-DB unit job. Kept deliberately literal — if it drifts from
    inventory.py the DB-backed test in test_naming.py catches it."""
    import re
    from mallet_estimator import decor
    mm = re.compile(r"\d+(?:\.\d+)?mm", re.I)
    base = "_".join(t for t in str(name).split("_") if not mm.fullmatch(t))
    slots = decor.trailing_slots(base)
    if slots:
        base = "_".join(base.split("_")[: -len(slots)])
    if thickness:
        return f"{base}_{thickness:g}mm"
    own = next((t for t in str(name).split("_") if mm.fullmatch(t)), "")
    return f"{base}_{own}" if own else base


class TestPurchasingIdentity(unittest.TestCase):
    """The OpenCutList material name and the stock Item code answer different
    questions — "which board do these parts come off" vs "what do I buy". The
    décor letters are load-bearing in the first and false in the second."""

    def test_one_board_however_many_decors(self):
        from mallet_estimator import decor
        # SG_PLY_V1_a_b and SG_PLY_V1_a_c are two OpenCutList materials — that
        # is what makes it lay them out on separate boards — and ONE Item.
        self.assertEqual(_ply_item("SG_PLY_V1_a_b", 16), "SG_PLY_V1_16mm")
        self.assertEqual(_ply_item("SG_PLY_V1_a_c", 16), "SG_PLY_V1_16mm")
        self.assertEqual(_ply_item("SG_PLY_V1_a_b1", 16), "SG_PLY_V1_16mm")
        self.assertIsNotNone(decor.trailing_slots("SG_PLY_V1_a_b"))

    def test_thickness_still_separates_boards(self):
        self.assertEqual(_ply_item("SG_PLY_V0_a_a", 12), "SG_PLY_V0_12mm")
        self.assertEqual(_ply_item("SG_PLY_V0_a_a", 16), "SG_PLY_V0_16mm")

    def test_grade_still_separates_boards(self):
        self.assertNotEqual(_ply_item("SG_PLY_V0_a_a", 16), _ply_item("SG_PLY_V1_a_b", 16))

    def test_collapsing_is_idempotent(self):
        # a code that has already collapsed must collapse to itself, or a
        # second migrate would mint yet another Item
        self.assertEqual(_ply_item("SG_PLY_V1_16mm", 16), "SG_PLY_V1_16mm")

    def test_a_code_carrying_its_own_thickness_keeps_it(self):
        # the seeder calls this with no thickness argument; dropping the mm
        # token would put a 12mm and a 16mm board on the same Item
        self.assertEqual(_ply_item("SG_PLY_V0_a_a_12mm", 0), "SG_PLY_V0_12mm")
        self.assertEqual(_ply_item("SG_PLY_V0_a_a_16mm", 0), "SG_PLY_V0_16mm")
        self.assertNotEqual(_ply_item("SG_PLY_V0_a_a_12mm", 0),
                            _ply_item("SG_PLY_V0_a_a_16mm", 0))

    def test_one_laminate_however_many_boards(self):
        from mallet_estimator import decor
        ss = {"a": "GE1834"}
        codes = ["SG_LAM_V0_12mm_a_a", "SG_LAM_V0_16mm_a_a", "SG_LAM_V1_16mm_a_b"]
        self.assertEqual({decor.substitute_real_code(c, ss)[0] for c in codes},
                         {"SG_LAM_GE1834"})

    def test_stock_base_leaves_everything_else_alone(self):
        from mallet_estimator import decor
        self.assertEqual(decor.stock_base("EB_PVC_EX"), "EB_PVC_EX")
        self.assertEqual(decor.stock_base("HWD_Hinge"), "HWD_Hinge")
        self.assertEqual(decor.stock_base("SG_LAM"), "SG_LAM")


class WorkstationsCarryWork(unittest.TestCase):
    """THE RULE, recorded at Amit's instruction on 2026-08-24: "keep it as per
    current erp setup. record it as rule as well."

    A costed workstation with no operation assigned is not free. Rent is spread
    across billable footprint, so a station in WORKSTATIONS takes its share
    whether or not anything is billed to it — and if nothing is, that share is
    computed and then charged to nothing at all. It simply disappears.

    That is exactly what the Project Room was doing: 14x15 ft of a floor rented
    whole, a fifth of the total footprint, and not one of the seventeen steps
    pointed at it. The plugin therefore never showed it, ERP did, and the
    difference looked like a bug in one of them when it was a hole in the
    costing.
    """

    def test_every_costed_workstation_carries_work(self):
        used = set(E.OPERATION_WORKSTATION.values())
        for w in E.WORKSTATIONS:
            self.assertIn(
                w["name"], used,
                "%s is in WORKSTATIONS, so it takes a share of the rent, but "
                "no operation runs there — that share is charged to nothing. "
                "Either give it work or take it out of the footprint."
                % w["name"])

    def test_every_operation_runs_at_a_station_that_exists(self):
        # The other direction, and it is the one that fails silently: an
        # operation pointing at a workstation nobody created prices at zero
        # rather than refusing.
        known = {w["name"] for w in E.WORKSTATIONS}
        for phase, ws in E.OPERATION_WORKSTATION.items():
            self.assertIn(ws, known,
                          "%s runs at %r, which is not a workstation" % (phase, ws))

    def test_the_staging_steps_are_in_the_project_room(self):
        # Named explicitly rather than left to the two rules above, because
        # this one is a PRICE and it was got wrong twice in one day.
        for phase in ("Disassembly", "Packing", "Loading"):
            self.assertEqual(E.OPERATION_WORKSTATION[phase], "Project Room", phase)


class HardwareSplit(unittest.TestCase):
    """Install Hardware is a parent line with one child per fitting type.

    Amit, 2026-08-24: "always divide the hardware by its type ... that way
    quantity will not be editable but its time will be editable depending on
    type of hardware."
    """

    def test_the_two_double_counted_buckets_are_absent(self):
        # minifix is step 5 and screws ARE step 6's quantity. A child for
        # either would charge the same fitting twice, and the bug would show
        # up as a total that is merely a bit high — the hardest kind to spot.
        kinds = {k for k, _ in E.HARDWARE_INSTALL_TYPES}
        self.assertNotIn("minifix", kinds)
        self.assertNotIn("screws", kinds)

    def test_every_type_has_a_standard_and_an_operation(self):
        for kind, label in E.HARDWARE_INSTALL_TYPES:
            self.assertIn(kind, E.HARDWARE_STANDARDS, kind)
            self.assertEqual(E.hardware_operation(kind), "Install %s" % label)

    def test_no_standard_is_left_at_zero(self):
        # A child seeded at zero prices its fittings at nothing and looks like
        # a line that is simply free. Miscellaneous is allowed to be zero
        # because it exists for work nobody has named; a hinge is not.
        for kind, _ in E.HARDWARE_INSTALL_TYPES:
            self.assertGreater(E.HARDWARE_STANDARDS[kind], 0, kind)

    def test_the_children_run_where_the_parent_runs(self):
        # Derived, not restated — the workstation is a price, and the day the
        # parent moves station these have to move with it.
        parent_ws = E.OPERATION_WORKSTATION[E.HARDWARE_PARENT]
        parent_zone = E.OPERATION_ZONE[E.HARDWARE_PARENT]
        for kind, _ in E.HARDWARE_INSTALL_TYPES:
            op = E.hardware_operation(kind)
            self.assertEqual(E.OPERATION_WORKSTATION[op], parent_ws, op)
            self.assertEqual(E.OPERATION_ZONE[op], parent_zone, op)

    def test_the_classifier_and_the_type_list_agree(self):
        # Every bucket classify_hardware can return must either be a child or
        # be excluded ON PURPOSE. A bucket that is neither would be counted in
        # the parent and shown in no child, so the rows would not add up.
        from mallet_estimator import opencutlist
        buckets = set()
        for name in ("HWD_Minifix", "HWD_Hinge", "HWD_Handle", "HWD_Rail",
                     "HWD_Shelf Support", "HWD_Tower Bolt", "HWD_Screw",
                     "HWD_Something Nobody Named"):
            buckets.add(opencutlist.classify_hardware(name))
        kinds = {k for k, _ in E.HARDWARE_INSTALL_TYPES}
        unaccounted = buckets - kinds - {"minifix", "screws"}
        self.assertEqual(unaccounted, set(),
                         "bucket(s) counted in the parent but shown in no child")


class ParentAndChildRows(unittest.TestCase):
    """A split step is charged ONCE — by its children, never also by itself.

    Assembly splits into three sizes and Install Hardware into fitting types.
    Both parents keep a quantity and a standard time of their own, so a costing
    loop that priced every row would bill the same work twice and the total
    would simply come out high, with every line looking reasonable.
    """

    RATE = {"rent_hr": 0, "wages_hr": 600, "machine_hr": 0, "elec_hr": 0,
            "consumable_hr": 0, "net_hr": 600, "labour_hr": 600, "dep_hr": 0,
            "total_hr": 600}

    def _row(self, op, qty, mins, parent="", split=""):
        return types.SimpleNamespace(
            operation=op, phase=op, workstation="Assembly Station",
            qty=qty, carp_min=mins, is_misc=0, carp_total=0, helper_total=0,
            op_cost=0, parent_step=parent, split_key=split)

    def _calc(self, rows):
        sku = types.SimpleNamespace(labor=rows, materials=[], design_hours=0,
                                    design_flat=0, include_misc=0)
        return E.calc_sku(sku, _settings(),
                          ws_rates={"Assembly Station": self.RATE})

    def test_a_parent_with_children_adds_nothing_of_its_own(self):
        parent = self._row("Assembly", 3, 60)                       # would be 180 min
        kids = [self._row("Assembly", 1, 60, "Assembly", "large"),
                self._row("Assembly", 1, 30, "Assembly", "medium"),
                self._row("Assembly", 1, 15, "Assembly", "small")]  # 105 min
        out = self._calc([parent] + kids)
        # 105 minutes of work, not 285.
        self.assertAlmostEqual(out["carp_min_total"], 105, 2)

    def test_the_parent_reads_as_the_sum_of_its_children(self):
        # A parent showing its own numbers while its children show theirs is a
        # row that contradicts the rows underneath it, on a screen a client may
        # be looking at.
        parent = self._row("Assembly", 3, 60)
        kids = [self._row("Assembly", 1, 60, "Assembly", "large"),
                self._row("Assembly", 2, 30, "Assembly", "medium")]
        self._calc([parent] + kids)
        self.assertAlmostEqual(parent.carp_total, 120, 2)      # 60 + 2x30
        self.assertAlmostEqual(parent.op_cost,
                               sum(k.op_cost for k in kids), 2)

    def test_a_step_with_no_children_is_unaffected(self):
        # The overwhelmingly common case still has to price the ordinary way.
        row = self._row("Sheet Cutting", 9, 20)
        out = self._calc([row])
        self.assertAlmostEqual(out["carp_min_total"], 180, 2)
        self.assertAlmostEqual(row.carp_total, 180, 2)

    def test_hardware_children_are_charged_and_their_parent_is_not(self):
        parent = self._row("Install Hardware", 63, 30)          # would be 1890
        kids = [self._row("Install Hinges", 24, 4, "Install Hardware", "hinges"),
                self._row("Install Drawer Rails", 12, 6, "Install Hardware", "rails")]
        out = self._calc([parent] + kids)
        self.assertAlmostEqual(out["carp_min_total"], 24 * 4 + 12 * 6, 2)
        self.assertAlmostEqual(parent.carp_total, 168, 2)


class TestCalcSubcontract(unittest.TestCase):
    """Work somebody else does, priced from that somebody's rate.

    Amit, 2026-08-25: subcontracted work becomes a full Estimate SKU with the
    vendor rate standing in for shop cost. Every number in these tests is
    invented — round figures chosen to make the arithmetic checkable at a
    glance. Real vendor rates live only in the site DB and never in this repo.
    """

    def _settings(self, **kw):
        # markup 0 is the shipped default and is deliberate: a margin is the
        # studio's own number. The tests that care pass one explicitly.
        return types.SimpleNamespace(markup_subcontract=kw.get("markup", 0))

    def test_each_line_is_its_own_unit_and_they_still_add_up(self):
        # Sqft of POP, points of wiring, and a lumpsum in the same SKU. The
        # unit belongs to the ARTICLE, which is the whole reason electrical is
        # three articles instead of one with three columns.
        out = E.calc_subcontract([
            {"article": "POP", "qty": 100, "uom": "Sqft", "rate": 10},
            {"article": "ELP", "qty": 20, "uom": "Point", "rate": 100},
            {"article": "SUB", "qty": 1, "uom": "Lumpsum", "rate": 5000},
        ], self._settings())
        self.assertEqual([l["uom"] for l in out["lines"]],
                         ["Sqft", "Point", "Lumpsum"])
        self.assertAlmostEqual(out["cost"], 1000 + 2000 + 5000, 2)

    def test_an_unpriced_vendor_is_named_not_silently_zero(self):
        # THE FAILURE THIS GUARDS. A subcontract quote missing one vendor's
        # rate looks exactly like a complete one — same layout, same lines,
        # a total that is merely too low. Nothing on the page says so unless
        # something counts it, which is the same rule unpriced materials
        # already follow.
        out = E.calc_subcontract([
            {"article": "POP", "qty": 400, "uom": "Sqft", "rate": 0},
            {"article": "TIL", "qty": 200, "uom": "Sqft", "rate": 50},
        ], self._settings())
        self.assertEqual(out["unpriced"], ["POP"])
        self.assertAlmostEqual(out["cost"], 10000, 2)
        self.assertEqual(out["lines"][0]["rate_source"], "unset")
        self.assertEqual(out["lines"][1]["rate_source"], "")

    def test_the_markup_is_policy_and_zero_by_default(self):
        rows = [{"article": "TIL", "qty": 100, "uom": "Sqft", "rate": 50}]
        bare = E.calc_subcontract(rows, self._settings())
        self.assertAlmostEqual(bare["client_total"], bare["cost"], 2)
        self.assertAlmostEqual(bare["margin"], 0, 2)

        # A percentage passed in beats the settings, the way every other
        # calc in this file lets a per-SKU override win.
        over = E.calc_subcontract(rows, self._settings(markup=10), markup_pct=25)
        self.assertAlmostEqual(over["markup_pct"], 25, 2)
        self.assertAlmostEqual(over["client_total"], 5000 * 1.25, 2)
        self.assertAlmostEqual(over["margin"], 1250, 2)

    def test_it_reads_documents_and_dicts_alike(self):
        # The desk passes child-table documents, the tests pass dicts, and
        # neither should have to care which.
        doc = types.SimpleNamespace(article="POP", vendor="A", qty="120",
                                    uom="Sqft", rate="25", rate_source="vendor")
        out = E.calc_subcontract([doc], self._settings())
        self.assertAlmostEqual(out["cost"], 3000, 2)
        self.assertEqual(out["lines"][0]["article"], "POP")
        self.assertEqual(out["lines"][0]["rate_source"], "vendor")

    def test_nothing_at_all_costs_nothing_and_says_nothing_is_missing(self):
        out = E.calc_subcontract([], self._settings())
        self.assertEqual(out["cost"], 0)
        self.assertEqual(out["unpriced"], [])
        self.assertEqual(out["lines"], [])

    def test_subcontract_is_off_the_floor_but_is_not_site_labour(self):
        # It shares "no parts, no nesting" with repair and supply-and-install,
        # and shares NOTHING else: those two are MCFT's own crew on site and
        # have a labour model. Subcontract has no crew at all, so folding it
        # into SITE_WORK would have it costed with carpenter minutes.
        self.assertIn(E.SUBCONTRACT, E.OFF_FLOOR)
        self.assertNotIn(E.SUBCONTRACT, E.SITE_WORK)

class TestAssemblyIdentification(unittest.TestCase):
    """WHICH components are assemblies, and what size they are.

    Amit, 2026-08-27, with the SketchUp Outliner beside the labour table:
    "Large / Medium / Small assemblies are not captured or identified
    correctly. Qualifier is TOp level component MCFT_ASMBL_L_ MCFT_ASMBL_M_
    MCFT_ASMBL_S_".
    """

    def _rows(self, *names):
        return [{"designation": n} for n in names]

    # The model from the screenshot, named exactly as the Outliner showed it.
    SCREENSHOT = ("MCFT_ASMBL_M_BOOKCAB", "MCFT_ASMBL_M_STUDYTABLE",
                  "ASMBL_Door_Loft_Left", "ASMBL_Door_Loft_Right",
                  "ASMBL_LOFT.WAR.SLID", "ASMBL_CARCASS_SHELF",
                  "ASMBL_DRW_Box.1_520", "ASMBL_DRW_Facia",
                  "ASMBL_STUDYTABLE")

    def test_the_reported_model_is_two_mediums_not_ten_larges(self):
        # The bug in one assertion. The matcher anchored on ASMBL at the START
        # of the name, so MCFT_ASMBL_M_BOOKCAB did not match at all while
        # every part INSIDE it did — unsized, therefore large. A model holding
        # two medium assemblies was priced as ten large ones.
        out = E._asmbl_counts(self._rows(*self.SCREENSHOT))
        self.assertEqual(out["medium"], 2)
        self.assertEqual(out["large"], 0)
        self.assertEqual(out["small"], 0)
        self.assertTrue(out["top_level"])

    def test_parts_inside_an_assembly_are_not_assemblies(self):
        # This is the half that matters most for money: a drawer box and a
        # door are things the assembly is MADE of, and counting them beside
        # their parent charges assembly time for the same object twice.
        out = E._asmbl_counts(self._rows(
            "MCFT_ASMBL_L_WAR", "ASMBL_DRW_Box", "ASMBL_Door", "ASMBL_Shelf"))
        self.assertEqual(sum(out[k] for k in E.ASSEMBLY_SIZES), 1)
        self.assertEqual(out["large"], 1)

    def test_a_model_drawn_before_the_convention_still_counts(self):
        # The fallback, and why it is not laziness. Every model drawn before
        # MCFT_ existed says plain ASMBL_WAR at top level. Demanding the
        # prefix outright would price those at ZERO assemblies — a silent
        # halving of the estimate, which is worse than the bug being fixed.
        out = E._asmbl_counts(self._rows("ASMBL_WAR", "ASMBL_LOFT"))
        self.assertEqual(out["large"], 2)
        self.assertEqual(out["unsized"], 2)
        self.assertFalse(out["top_level"])

    def test_an_unsized_top_level_assembly_is_large_and_says_it_was_unsized(self):
        # "Nobody said" and "somebody said large" cost the same and mean
        # different things: the first tells you a model predates the
        # convention, which is worth being able to see.
        out = E._asmbl_counts(self._rows("MCFT_ASMBL_BOOKCAB"))
        self.assertEqual(out["large"], 1)
        self.assertEqual(out["unsized"], 1)

    def test_every_size_token_is_read(self):
        out = E._asmbl_counts(self._rows(
            "MCFT_ASMBL_L_WAR", "MCFT_ASMBL_M_DRW", "MCFT_ASMBL_S_SHELF"))
        self.assertEqual((out["large"], out["medium"], out["small"]), (1, 1, 1))
        self.assertEqual(out["unsized"], 0)

    def test_a_component_that_is_not_an_assembly_at_all_is_ignored(self):
        out = E._asmbl_counts(self._rows(
            "bukcab_ref", "studytbl_ref", "Group", "< studytbl_skirt>"))
        self.assertEqual(sum(out[k] for k in E.ASSEMBLY_SIZES), 0)

class TestHardwareStandardMinutes(unittest.TestCase):
    """Amit, 2026-08-27: "every install hardware operation is 15 minutes per
    unit." """

    def test_every_hardware_type_starts_at_the_same_fifteen(self):
        # The per-type figures this replaces were guesses made when the split
        # was built, not measurements. One number he chose beats six I
        # invented, and the split still earns its keep because the COUNTS
        # differ per type and the minutes stay editable per type.
        for kind, _label in E.HARDWARE_INSTALL_TYPES:
            self.assertEqual(E.HARDWARE_STANDARDS[kind], 15, kind)

    def test_the_parent_carries_the_same_figure(self):
        # Amit, 2026-08-27, when the six children had moved and the parent had
        # not: "Set it to 15 as well." Unused while children exist — the
        # parent's time is their sum — so it is the FALLBACK for a hardware
        # line whose type nothing matched, and it is what a person scanning
        # the Operation list reads. Two figures for one idea is how a stale
        # number outlives its reason.
        self.assertEqual(
            E.OPERATION_STANDARDS[E.HARDWARE_PARENT]["min_per_unit"], 15)

    def test_every_type_has_a_standard_at_all(self):
        # A type present in the model but missing from the table would price
        # its fittings at zero minutes and look like a complete estimate.
        self.assertEqual(set(E.HARDWARE_STANDARDS),
                         {k for k, _l in E.HARDWARE_INSTALL_TYPES})
