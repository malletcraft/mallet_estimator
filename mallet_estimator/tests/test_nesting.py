import unittest

from mallet_estimator import nesting


class TestNesting(unittest.TestCase):
    def test_single_part_single_sheet(self):
        r = nesting.pack_sheets([(2000, 1000)])
        self.assertEqual(r["sheets"], 1)

    def test_full_sheets_do_not_merge(self):
        # four half-sheets fit two per sheet
        r = nesting.pack_sheets([(2400, 590)] * 4, kerf=4, trim=10)
        self.assertEqual(r["sheets"], 2)

    def test_grain_lock_blocks_rotation(self):
        # part fits only rotated: allowed when rotation on, rejected when locked
        ok = nesting.pack_sheets([(1000, 2000)], allow_rotate=True)
        self.assertEqual(ok["sheets"], 1)
        locked = nesting.pack_sheets([(1000, 2000)], allow_rotate=False)
        self.assertEqual(locked["sheets"], 0)
        self.assertEqual(len(locked["too_big"]), 1)

    def test_monotonic_more_parts_never_fewer_sheets(self):
        base = [(800, 600)] * 6
        more = base + [(800, 600)] * 6
        self.assertLessEqual(nesting.pack_sheets(base)["sheets"],
                             nesting.pack_sheets(more)["sheets"])

    def test_edge_rolls(self):
        self.assertEqual(nesting.edge_rolls(0), 0)
        self.assertEqual(nesting.edge_rolls(49.9), 1)
        self.assertEqual(nesting.edge_rolls(50.1), 2)

    def test_envelope_check(self):
        ply = {("SG_PLY_V1_a_b", 18.0): [(2100, 580), (2400, 600)]}
        # wardrobe 2133 x 580 x 2133: the 2400 part cannot build it
        bad = nesting.envelope_check((2133, 580, 2133), ply)
        self.assertEqual(len(bad), 1)
        self.assertEqual(bad[0][1], 2400)
        # within tolerance passes; missing dims -> no check
        self.assertEqual(nesting.envelope_check((2133, 580, 2133),
                                                {("X", 18.0): [(2150, 580)]}), [])
        self.assertEqual(nesting.envelope_check((0, 0, 0), ply), [])


class TestOffcutsWorthKeeping(unittest.TestCase):
    """A board that leaves a shelf-sized piece behind did not cost the job a
    whole board. The free rectangles were always computed and always thrown
    away; reporting them is what lets that be priced instead of assumed."""

    def test_packing_reports_what_is_left_over(self):
        r = nesting.pack_sheets([(1200.0, 600.0)] * 3)
        self.assertIn("offcuts", r)
        self.assertTrue(r["offcuts"], "a part-filled sheet always leaves something")
        # biggest first — the useful ones are the ones you look at
        areas = [l * w for (l, w) in r["offcuts"]]
        self.assertEqual(areas, sorted(areas, reverse=True))

    def test_only_pieces_you_can_build_from_are_kept(self):
        # 400 x 600 is the floor: a shelf or a small box. Below it, dust with
        # a shape. Orientation does not matter to a rack, so both ways round.
        keep = nesting.reusable([(1000.0, 500.0), (500.0, 1000.0),
                                 (2000.0, 300.0), (350.0, 900.0), (100.0, 100.0)])
        self.assertEqual(keep, [(1000.0, 500.0), (1000.0, 500.0)])

    def test_a_sliver_the_full_length_of_the_sheet_is_still_a_sliver(self):
        self.assertEqual(nesting.reusable([(2440.0, 120.0)]), [])


class TestLaminateFollowsThePanel(unittest.TestCase):
    """The shop presses a full laminate sheet onto a full ply sheet and only
    then puts the sandwich on the panel saw, so laminate is bought per ply
    sheet per laminated face — never nested to part size on its own."""

    def test_both_faces_same_decor_doubles_the_boards(self):
        # 3 boards of V0, internal laminate on both faces -> 6 laminate sheets
        panels = {("SG_PLY_V0_a_a", 12): 3}
        faces = {("SG_PLY_V0_a_a", 12): {
            "Frontside": {"SG_LAM_V0_12mm_a_a": 4.78e6},
            "Backside": {"SG_LAM_V0_12mm_a_a": 4.78e6},
        }}
        self.assertEqual(nesting.laminate_from_panels(panels, faces),
                         {"SG_LAM_V0_12mm_a_a": 6})

    def test_each_face_of_a_v1_board_buys_its_own_laminate(self):
        panels = {("SG_PLY_V1_a_b", 16): 2}
        faces = {("SG_PLY_V1_a_b", 16): {
            "Frontside": {"SG_LAM_V1_16mm_b_a": 4.16e6},   # external
            "Backside": {"SG_LAM_V1_16mm_a_b": 4.16e6},    # internal
        }}
        self.assertEqual(nesting.laminate_from_panels(panels, faces),
                         {"SG_LAM_V1_16mm_b_a": 2, "SG_LAM_V1_16mm_a_b": 2})

    def test_a_face_carrying_two_decors_splits_by_part_area(self):
        # half the boards' front area is one laminate, half the other: 4 boards
        # -> 2 sheets each, and neither is rounded up to a whole board twice
        panels = {("SG_PLY_V1_a_b", 16): 4}
        faces = {("SG_PLY_V1_a_b", 16): {
            "Frontside": {"SG_LAM_V1_16mm_b_a": 1.0e6, "SG_LAM_V1_16mm_c_a": 1.0e6},
        }}
        self.assertEqual(nesting.laminate_from_panels(panels, faces),
                         {"SG_LAM_V1_16mm_b_a": 2, "SG_LAM_V1_16mm_c_a": 2})

    def test_shares_are_summed_before_rounding(self):
        # two boards each contributing half a sheet is ONE sheet, not two:
        # rounding each contribution first buys laminate the press never eats
        panels = {("A", 16): 1, ("B", 16): 1}
        faces = {
            ("A", 16): {"Frontside": {"LAM": 1.0e6, "OTHER": 1.0e6}},
            ("B", 16): {"Frontside": {"LAM": 1.0e6, "OTHER": 1.0e6}},
        }
        self.assertEqual(nesting.laminate_from_panels(panels, faces)["LAM"], 1)

    def test_an_unlaminated_board_buys_nothing(self):
        self.assertEqual(nesting.laminate_from_panels({("SG_PLY_V0_a_a", 16): 4}, {}), {})
