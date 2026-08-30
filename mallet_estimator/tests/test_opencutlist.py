# Pure unit tests for the OpenCutList CSV parser + aggregator — no database.
#   python -m unittest mallet_estimator.tests.test_opencutlist
import unittest

from mallet_estimator import decor as D
from mallet_estimator import opencutlist as OCL

# A tiny semicolon-delimited OpenCutList "parts" export: 2 sheet parts (one edged
# on all four sides), plus a hardware row — enough to exercise every aggregator path.
CSV = """No.;Material name;Material type;Length;Width;Thickness;Area - final;Edge Length 1;Edge Length 2;Edge Width 1;Edge Width 2;Frontside;Backside;Tag
1;SG_PLY_V0_a_a;Sheet Goods;600;400;16;0.24;EB_PVC_IN_a (1 mm x 22 mm);;EB_PVC_IN_a (1 mm x 22 mm);;;;shelf
2;SG_PLY_V0_a_a;Sheet Goods;800;500;16;0.40;;;;;SG_LAM_V0_a_a;;door
3;HWD_Hinge;Hardware;;;;;;;;;;;
"""


class TestParse(unittest.TestCase):
    def test_rows_parsed(self):
        rows = OCL.parse_opencutlist_csv(CSV)
        self.assertEqual(len(rows), 3)
        self.assertEqual(rows[0]["Material name"], "SG_PLY_V0_a_a")

    def test_material_from_strips_spec(self):
        self.assertEqual(OCL._material_from("EB_PVC_IN_a (1 mm x 22 mm)"), "EB_PVC_IN_a")
        self.assertIsNone(OCL._material_from(""))


class TestPartsList(unittest.TestCase):
    def test_only_sheet_goods_with_part_no(self):
        parts = OCL.parts_list(OCL.parse_opencutlist_csv(CSV))
        self.assertEqual(len(parts), 2)
        self.assertEqual(parts[0]["part_no"], "1")
        self.assertEqual(parts[0]["thickness"], 16)

    def test_station_flags(self):
        # Part 1 is edge-banded (not laminated); part 2 is laminated (not edged);
        # every sheet part is cut. These drive the Job Card part list per station.
        parts = OCL.parts_list(OCL.parse_opencutlist_csv(CSV))
        self.assertEqual((parts[0]["cut"], parts[0]["edge_banded"], parts[0]["laminated"]), (1, 1, 0))
        self.assertEqual((parts[1]["cut"], parts[1]["edge_banded"], parts[1]["laminated"]), (1, 0, 1))


# Hardware rows: Material name is the coarse category; Designation is the real
# SKU; OpenCutList suffixes duplicate instances with #N. Two hinges (one #1) roll
# up to one SKU qty 2; two handle designations under one category stay distinct.
HW_CSV = """No.;Material name;Material type;Designation;Length;Width;Thickness
1;HWD_Hinge;Hardware;HWD_AH_SC_0;80 mm;65 mm;42 mm
2;HWD_Hinge;Hardware;HWD_AH_SC_0#1;80 mm;65 mm;42 mm
3;HWD_Handle;Hardware;HWD_Handle_150mm;86 mm;32 mm;22 mm
4;HWD_Handle;Hardware;HWD_HandleDrawer_150mm;152 mm;32 mm;22 mm
5;SG_PLY_V0_a_a;Sheet Goods;panel;600;400;16
"""


class TestHardwareList(unittest.TestCase):
    def test_canonical_strips_instance_suffix(self):
        self.assertEqual(OCL.canonical_hw_code("HWD_AH_SC_0#3"), "HWD_AH_SC_0")
        self.assertEqual(OCL.canonical_hw_code("HWD_AH_SC_0"), "HWD_AH_SC_0")

    def test_hardware_by_designation_with_dims(self):
        hw = OCL.hardware_list(OCL.parse_opencutlist_csv(HW_CSV))
        by = {h["code"]: h for h in hw}
        self.assertEqual(set(by), {"HWD_AH_SC_0", "HWD_Handle_150mm", "HWD_HandleDrawer_150mm"})
        self.assertEqual(by["HWD_AH_SC_0"]["qty"], 2)          # #1 instance rolled up
        self.assertEqual(by["HWD_AH_SC_0"]["category"], "HWD_Hinge")
        self.assertEqual(
            (by["HWD_AH_SC_0"]["length"], by["HWD_AH_SC_0"]["width"], by["HWD_AH_SC_0"]["thickness"]),
            (80.0, 65.0, 42.0),
        )
        # one Material name (HWD_Handle) resolves to two distinct SKUs
        self.assertEqual(by["HWD_Handle_150mm"]["qty"], 1)
        self.assertEqual(by["HWD_HandleDrawer_150mm"]["qty"], 1)
        # sheet-goods rows are ignored here
        self.assertNotIn("panel", by)


class TestAggregate(unittest.TestCase):
    def setUp(self):
        self.agg = OCL.aggregate(OCL.parse_opencutlist_csv(CSV))

    def test_sheet_line_present(self):
        sheets = [l for l in self.agg["lines"] if l["kind"] == "sheet"]
        self.assertEqual(len(sheets), 1)
        self.assertEqual(sheets[0]["material"], "SG_PLY_V0_a_a")

    def test_edge_measured_in_metres(self):
        edge = [l for l in self.agg["lines"] if l["kind"] == "edge"]
        self.assertTrue(edge, "expected an edge-banding line")
        self.assertEqual(edge[0]["uom"], "Meter")
        self.assertGreater(edge[0]["qty"], 0)

    def test_hardware_counted(self):
        self.assertEqual(self.agg["drivers"]["hinges"], 1)

    def test_panels_and_edge_parts(self):
        self.assertEqual(self.agg["drivers"]["panels"], 2)
        self.assertEqual(self.agg["drivers"]["edge_parts"], 1)  # only part 1 is edged


class TestClassifyHardware(unittest.TestCase):
    def test_buckets(self):
        self.assertEqual(OCL.classify_hardware("HWD_Hinge"), "hinges")
        self.assertEqual(OCL.classify_hardware("HWD_MiniFix"), "minifix")
        self.assertEqual(OCL.classify_hardware("HWD_Screw"), "screws")



class TestHardwareDesignationColumns(unittest.TestCase):
    """Which column carries a part's own name depends on how the OpenCutList
    export is configured. Reading only 'Designation' made every hardware line
    fall back to its CATEGORY (HWD_Hinge instead of HWD_AH_SC_0), which hides
    several SKUs at different rates behind one code."""

    def _rows(self, column):
        return [
            {"Material type": "Hardware", "Material name": "HWD_Hinge", column: "HWD_AH_SC_0#1"},
            {"Material type": "Hardware", "Material name": "HWD_Hinge", column: "HWD_AH_SC_0#2"},
        ]

    def test_every_designation_column_is_tried(self):
        for column in OCL.DESIGNATION_COLUMNS:
            hw = OCL.hardware_list(self._rows(column))
            self.assertEqual([h["code"] for h in hw], ["HWD_AH_SC_0"], column)
            self.assertEqual(hw[0]["qty"], 2, column)
            self.assertEqual(hw[0]["category"], "HWD_Hinge", column)
            self.assertTrue(hw[0]["named"], column)

    def test_a_csv_with_no_designation_is_flagged_not_faked(self):
        hw = OCL.hardware_list([{"Material type": "Hardware", "Material name": "HWD_Handle"}])
        self.assertEqual(hw[0]["code"], "HWD_Handle")
        self.assertFalse(hw[0]["named"])   # caller warns instead of pretending

    def test_a_designation_echoing_the_category_is_not_a_name(self):
        hw = OCL.hardware_list([{"Material type": "Hardware", "Material name": "HWD_Hinge",
                                 "Designation": "HWD_Hinge#1"}])
        self.assertFalse(hw[0]["named"])


if __name__ == "__main__":
    unittest.main()


class TestCsvHardwareDesignations(unittest.TestCase):
    """CSV-Nest must aggregate hardware by DESIGNATION (HWD_AH_SC_0), never by
    the coarse Material name (HWD_Hinge) — several distinct SKUs at different
    rates can hide inside one category."""

    CSV = (
        "Material type;Material name;Designation;Length;Width;Thickness\n"
        "Hardware;HWD_Hinge;HWD_AH_SC_0;0;0;0\n"
        "Hardware;HWD_Hinge;HWD_AH_SC_0#2;0;0;0\n"
        "Hardware;HWD_Hinge;HWD_AH_SC_165;0;0;0\n"
        "Hardware;HWD_Handle;HWD_HDL_128;0;0;0\n"
    )

    def test_designations_not_categories(self):
        rows = OCL.parse_opencutlist_csv(self.CSV)
        hw = OCL.hardware_list(rows)
        by_code = {h["code"]: h for h in hw}
        self.assertIn("HWD_AH_SC_0", by_code)
        self.assertIn("HWD_AH_SC_165", by_code)
        self.assertNotIn("HWD_Hinge", by_code)      # the category is never a line
        self.assertEqual(by_code["HWD_AH_SC_0"]["qty"], 2)   # '#2' rolls up
        self.assertEqual(by_code["HWD_AH_SC_0"]["category"], "HWD_Hinge")

    def test_category_still_classifies_the_driver(self):
        # the operation driver must still see these as hinges/handles
        self.assertEqual(OCL.classify_hardware("HWD_AH_SC_0 · HWD_Hinge"), "hinges")
        self.assertEqual(OCL.classify_hardware("HWD_HDL_128 · HWD_Handle"), "handles")
        self.assertEqual(OCL.classify_hardware("HWD_AH_SC_0"), "other")


# OpenCutList GROUPS identical parts onto one row and puts the count in
# `Quantity` — "Group181#2 ( CARCASS_SHELF x3 )" is three shelves, one row. On
# the shop's real YS_MB_WAR export, reading the row and not the count nested 21
# parts instead of 34 (7 sheets against OpenCutList's own 9) and bought 10
# pieces of hardware where the model has 99 — 24 MiniFix on one row counted once.
GROUPED_CSV = (
    "No.;Material name;Material type;Designation;Quantity;Length;Width;Thickness\n"
    "1;SG_PLY_V0_a_a;Sheet Goods;CARCASS_SHELF x3;3;600;400;16\n"
    "2;SG_PLY_V0_a_a;Sheet Goods;CARCASS_SIDE;1;2060;580;16\n"
    "3;HWD_MiniFix;Hardware;HWD_MiniFix;24;0;0;0\n"
)


class TestGroupedRows(unittest.TestCase):
    def test_part_qty_reads_the_count(self):
        rows = OCL.parse_opencutlist_csv(GROUPED_CSV)
        self.assertEqual([OCL.part_qty(r) for r in rows], [3, 1, 24])

    def test_missing_quantity_is_one_part(self):
        # the older export shape has no Quantity column at all
        self.assertEqual(OCL.part_qty({}), 1)
        self.assertEqual(OCL.part_qty({"Quantity": ""}), 1)
        self.assertEqual(OCL.part_qty({"Quantity": "0"}), 1)

    def test_parts_list_carries_the_count(self):
        # the ROW stays whole so the part number still matches the QR label on
        # the floor; the operator cuts `qty` of it
        parts = OCL.parts_list(OCL.parse_opencutlist_csv(GROUPED_CSV))
        self.assertEqual([(p["part_no"], p["qty"]) for p in parts], [("1", 3), ("2", 1)])

    def test_a_grouped_hardware_row_is_flagged_not_silently_counted(self):
        """The rule changed on 2026-08-30 and this is the test that carries it.

        Hardware quantity is now OpenCutList's Qty — the ROW COUNT, what you
        buy: two drawers is two rail sets, not four runners (Amit). On a
        GROUPED export that count understates the purchase, and this fixture
        is the exact shape that once ordered 10 pieces for a 99-piece model:
        24 MiniFix on one row.

        So the row count is reported AND the grouping is flagged with the
        real piece count beside it. Callers refuse (the saved import) or
        shout (the preview). What must never happen again is 1 quietly
        standing in for 24.
        """
        hw = OCL.hardware_list(OCL.parse_opencutlist_csv(GROUPED_CSV))
        self.assertEqual([(h["code"], h["qty"], h["pieces"]) for h in hw],
                         [("HWD_MiniFix", 1, 24)])
        self.assertEqual([h["code"] for h in OCL.grouped_hardware(hw)],
                         ["HWD_MiniFix"])

    def test_an_ungrouped_export_is_not_flagged(self):
        # One row per piece: row count and piece count agree, nothing to say.
        csv = ("No.;Material name;Material type;Designation;Quantity;Length;Width;Thickness\n"
               "1;HWD_Hinge;Hardware;HWD_AH_SC_0;1;0;0;0\n"
               "2;HWD_Hinge;Hardware;HWD_AH_SC_0;1;0;0;0\n"
               "3;HWD_Drw_Rail;Hardware;HWD_DR_SC_550mm;1;0;0;0\n")
        hw = OCL.hardware_list(OCL.parse_opencutlist_csv(csv))
        self.assertEqual([(h["code"], h["qty"], h["pieces"]) for h in hw],
                         [("HWD_AH_SC_0", 2, 2), ("HWD_DR_SC_550mm", 1, 1)])
        self.assertEqual(OCL.grouped_hardware(hw), [])

    def test_two_rail_rows_are_two_sets_not_four_runners(self):
        # Amit, 2026-08-30, choosing the unit: "A set — 2 drawers, 2 rail
        # sets." The estimate counts what a purchase order counts.
        csv = ("No.;Material name;Material type;Designation;Quantity;Length;Width;Thickness\n"
               "1;HWD_Drw_Rail;Hardware;HWD_DR_SC_550mm;1;0;0;0\n"
               "2;HWD_Drw_Rail;Hardware;HWD_DR_SC_550mm;1;0;0;0\n")
        hw = OCL.hardware_list(OCL.parse_opencutlist_csv(csv))
        self.assertEqual(hw[0]["qty"], 2)


class TestOneItemCodeRule(unittest.TestCase):
    """A board is a PURCHASING identity, and only one function decides it.

    Found 2026-08-29, by running one CSV down both paths and diffing, after
    Amit asked what else the plugin was missing. opencutlist had its own
    item_code_for that appended the thickness and stopped, so the plugin's ply
    kept its décor slot letters — SG_PLY_V0_a_a_16mm — while every Item the
    bench actually mints obeys inventory.item_code_for and drops them.

    Two functions of the same name in two modules, one right and one naive, is
    why nobody noticed. The naive one is deleted; these tests exist so it
    cannot come back by another route.
    """

    def test_a_board_loses_its_decor_letters(self):
        self.assertEqual(D.purchasing_code("SG_PLY_V0_a_a", 16, "sheet"),
                         "SG_PLY_V0_16mm")
        self.assertEqual(D.purchasing_code("SG_PLY_V1_a_b", 16, "sheet"),
                         "SG_PLY_V1_16mm")
        # Two décors, one board to buy. That is the whole point of the rule.
        self.assertEqual(D.purchasing_code("SG_PLY_V0_a_a", 16, "sheet"),
                         D.purchasing_code("SG_PLY_V0_b_c", 16, "sheet"))

    def test_an_already_collapsed_code_is_left_alone(self):
        # Idempotence matters: the preview re-prices the same lines every
        # refresh, and a rule that kept eating tokens would walk a code down
        # to nothing.
        self.assertEqual(D.purchasing_code("SG_PLY_V0_16mm", 0, "sheet"),
                         "SG_PLY_V0_16mm")
        self.assertEqual(D.purchasing_code("SG_PLY_V0_16mm", 16, "sheet"),
                         "SG_PLY_V0_16mm")

    def test_thickness_still_makes_a_different_board(self):
        # The letters go; the millimetres never do. Collapsing 12 and 16 onto
        # one Item would be a worse bug than the one being fixed.
        self.assertNotEqual(D.purchasing_code("SG_PLY_V0_a_a", 12, "sheet"),
                            D.purchasing_code("SG_PLY_V0_a_a", 16, "sheet"))

    def test_laminate_keeps_what_identifies_it(self):
        # A laminate IS its décor — stripping it would merge every laminate in
        # the site into one meaningless code.
        self.assertEqual(D.purchasing_code("SG_LAM_V1_16mm_VM6534", 0, "laminate"),
                         "SG_LAM_V1_16mm_VM6534")
        # And an UNMAPPED placeholder keeps its slot letters too: nothing has
        # said what it is yet, so it is not a purchasing identity at all.
        self.assertEqual(D.purchasing_code("SG_LAM_V0_a_a", 0, "laminate"),
                         "SG_LAM_V0_a_a")

    def test_the_naive_helper_is_gone_and_stays_gone(self):
        # The bug was not the arithmetic, it was that a second function of the
        # same name existed to be imported by mistake.
        self.assertFalse(hasattr(OCL, "item_code_for"),
                         "opencutlist.item_code_for is back — the preview will "
                         "price ply against a code the bench never mints")


class TestPlyIsNotAPlaceholder(unittest.TestCase):
    """A board's slot letters are not an unresolved décor.

    Ply loses its slot letters on the way to its Item — two décors on one
    board is still one board to buy — while laminate and edge band keep
    theirs, because for those the décor IS the identity. That asymmetry is
    what these tests pin, and it is load-bearing in two directions.

    It first mattered on 2026-08-30, when I made the preview REFUSE to price
    any code still carrying slot letters and applied the test to the raw
    OpenCutList name: SG_PLY_V0_a_a matched, so every sheet line went to zero
    reporting "décor not set". A bench test caught that, twenty minutes at a
    time; these run in milliseconds.

    The refusal itself is gone — Amit, the same day: "this is set in erp. use
    from assumed price list." A slot-coded laminate is the estimating
    identity before a décor is chosen, and the site keeps a real rate for
    each one. But the CODE distinction still decides which line can resolve
    through a décor map and which is already final, so it is still worth
    holding still.
    """

    def test_a_board_stops_being_a_placeholder_once_it_is_a_code(self):
        self.assertTrue(D.trailing_slots("SG_PLY_V0_a_a"))
        self.assertFalse(D.trailing_slots(D.purchasing_code("SG_PLY_V0_a_a", 16, "sheet")))
        self.assertFalse(D.trailing_slots(D.purchasing_code("SG_PLY_V1_a_b", 16, "sheet")))

    def test_an_unresolved_laminate_stays_a_placeholder(self):
        # The set the refusal is FOR: laminate and edge keep their letters
        # through purchasing_code, because the décor IS their identity.
        for name, kind in (("SG_LAM_V0_16mm_a_a", "laminate"),
                           ("SG_LAM_V1_16mm_b_a", "laminate"),
                           ("EB_PVC_IN_a", "edge"),
                           ("EB_PVC_EX_b", "edge")):
            self.assertTrue(D.trailing_slots(D.purchasing_code(name, 0, kind)),
                            "%s must still read as unresolved" % name)

    def test_a_resolved_decor_is_quotable_again(self):
        for name, kind in (("SG_LAM_GE1000", "laminate"),
                           ("SG_LAM_V1_16mm_VM6534", "laminate"),
                           ("EB_PVC_IN_RE1000", "edge")):
            self.assertFalse(D.trailing_slots(D.purchasing_code(name, 0, kind)),
                             "%s is a real décor and must price" % name)
