# Pure unit tests for the scope matrix — no database, no frappe.
#   python -m unittest mallet_estimator.tests.test_scope
import unittest

from mallet_estimator import scope as S


def _rooms(*pairs):
    return [{"room": r, "area_sqft": a} for r, a in pairs]


def _svcs(*pairs):
    return [{"article": a, "rate": r} for a, r in pairs]


class TestCell(unittest.TestCase):
    def test_area_times_rate(self):
        self.assertEqual(S.cell_amount(140, 45), 6300.0)

    def test_a_missing_rate_prices_at_nothing_not_at_a_guess(self):
        # Zero is the honest answer; suspect_cells is what stops zero being
        # mistaken for "included and cheap".
        self.assertEqual(S.cell_amount(140, 0), 0.0)
        self.assertEqual(S.cell_amount(140, None), 0.0)

    def test_a_missing_area_prices_at_nothing(self):
        self.assertEqual(S.cell_amount(0, 45), 0.0)

    def test_junk_does_not_raise(self):
        self.assertEqual(S.cell_amount("x", 45), 0.0)


class TestMatrix(unittest.TestCase):
    def test_every_room_crossed_with_every_service(self):
        m = S.build_matrix(_rooms(("MB", 140), ("Kitchen", 80)),
                           _svcs(("SVC_POP", 45), ("SVC_TILE", 60)))
        self.assertEqual(len(m), 4)
        self.assertEqual({(c["room"], c["article"]) for c in m},
                         {("MB", "SVC_POP"), ("MB", "SVC_TILE"),
                          ("Kitchen", "SVC_POP"), ("Kitchen", "SVC_TILE")})

    def test_a_rebuild_does_not_re_tick_what_was_cleared(self):
        # THE POINT OF kept. The matrix is regenerated on every save so that
        # adding a room adds its column — and if that also resurrected the
        # services a room does not need, the document would fight the person
        # editing it.
        kept = {("Kitchen", "SVC_POP"): 0}
        m = S.build_matrix(_rooms(("MB", 140), ("Kitchen", 80)),
                           _svcs(("SVC_POP", 45)), kept)
        cell = next(c for c in m if c["room"] == "Kitchen")
        self.assertEqual(cell["include"], 0)
        self.assertEqual(cell["amount"], 0.0)

    def test_a_newly_added_room_starts_ticked(self):
        kept = {("MB", "SVC_POP"): 1}
        m = S.build_matrix(_rooms(("MB", 140), ("Study", 60)),
                           _svcs(("SVC_POP", 45)), kept)
        self.assertEqual(next(c for c in m if c["room"] == "Study")["include"], 1)

    def test_blank_names_are_skipped_not_crossed(self):
        m = S.build_matrix(_rooms(("", 140), ("MB", 140)), _svcs(("", 45), ("SVC_POP", 45)))
        self.assertEqual(len(m), 1)


class TestTotals(unittest.TestCase):
    def test_sqft_counts_each_room_once(self):
        # THE TRAP: a room appears once per service, so summing the LINES
        # multiplies the floor area by the number of services — a number that
        # looks plausible and is several times too big.
        rooms = _rooms(("MB", 140), ("Kitchen", 80))
        m = S.build_matrix(rooms, _svcs(("A", 10), ("B", 10), ("C", 10)))
        sqft, _money = S.totals(m, rooms)
        self.assertEqual(sqft, 220.0)

    def test_unticked_cells_do_not_count(self):
        rooms = _rooms(("MB", 100))
        m = S.build_matrix(rooms, _svcs(("A", 10), ("B", 10)),
                           {("MB", "B"): 0})
        _sqft, money = S.totals(m, rooms)
        self.assertEqual(money, 1000.0)

    def test_an_empty_scope_totals_zero_rather_than_failing(self):
        self.assertEqual(S.totals([], []), (0.0, 0.0))


class TestSuspectCells(unittest.TestCase):
    def test_a_ticked_service_with_no_rate_is_named(self):
        rooms = _rooms(("MB", 140))
        m = S.build_matrix(rooms, _svcs(("SVC_POP", 45), ("SVC_TILE", 0)))
        bad = S.suspect_cells(m)
        self.assertEqual(len(bad), 1)
        self.assertEqual(bad[0][1], "SVC_TILE")
        self.assertEqual(bad[0][2], "no rate")

    def test_a_ticked_room_with_no_area_is_named(self):
        rooms = _rooms(("MB", 0))
        m = S.build_matrix(rooms, _svcs(("SVC_POP", 45)))
        self.assertEqual(S.suspect_cells(m)[0][2], "no area")

    def test_an_unticked_hollow_cell_is_not_a_complaint(self):
        # Deliberately excluded is not the same as accidentally worthless.
        rooms = _rooms(("MB", 140))
        m = S.build_matrix(rooms, _svcs(("SVC_TILE", 0)), {("MB", "SVC_TILE"): 0})
        self.assertEqual(S.suspect_cells(m), [])

    def test_a_complete_scope_complains_about_nothing(self):
        rooms = _rooms(("MB", 140), ("Kitchen", 80))
        m = S.build_matrix(rooms, _svcs(("SVC_POP", 45), ("SVC_TILE", 60)))
        self.assertEqual(S.suspect_cells(m), [])


class TestNoRatesInTheRepo(unittest.TestCase):
    def test_the_scope_module_carries_no_figures(self):
        """mallet_estimator is PUBLIC. A rate committed here would be
        permanent and world-readable, and this is the file where one would
        most plausibly get written 'just as a default'.

        Parsed with ast rather than grepped: a regex over the text also finds
        the dates in the docstrings, and a check that cries wolf gets deleted
        by the next person in a hurry.
        """
        import ast
        import pathlib
        import mallet_estimator
        src = (pathlib.Path(mallet_estimator.__file__).parent / "scope.py").read_text()
        tree = ast.parse(src)
        # Drop every docstring before looking at the literals.
        for node in ast.walk(tree):
            if isinstance(node, (ast.Module, ast.FunctionDef, ast.ClassDef)):
                body = getattr(node, "body", [])
                if (body and isinstance(body[0], ast.Expr)
                        and isinstance(body[0].value, ast.Constant)
                        and isinstance(body[0].value.value, str)):
                    node.body = body[1:]
        for node in ast.walk(tree):
            if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
                self.assertLessEqual(
                    abs(node.value), 5,
                    f"{node.value} in scope.py looks like a rate or an area; "
                    f"cost data never enters this repo")
