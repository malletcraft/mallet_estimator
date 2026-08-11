import unittest

from mallet_estimator import consolidate


def _inputs(**skus):
    return skus


class TestConsolidate(unittest.TestCase):
    def test_combined_never_more_than_standalone(self):
        # two SKUs, each with half-sheet parts of the SAME ply: alone they'd
        # each round up to a full sheet; together they share sheets
        half = [(2400, 590)]
        r = consolidate.consolidate(_inputs(
            WAR={"ply": {"SG_PLY_V1_a_b@18": half}, "lam": {}, "edges": {}},
            BED={"ply": {"SG_PLY_V1_a_b@18": half}, "lam": {}, "edges": {}},
        ))
        m = r["materials"]["SG_PLY_V1_a_b@18"]
        self.assertEqual(m["standalone"], 2)
        self.assertEqual(m["combined"], 1)
        # equal parts -> equal halves of the one combined sheet
        self.assertAlmostEqual(m["alloc"]["WAR"], 0.5, places=3)
        self.assertAlmostEqual(m["alloc"]["BED"], 0.5, places=3)

    def test_allocation_is_part_area_share(self):
        big = [(2000, 600)]           # 1.2 m² of parts
        small = [(1000, 500)]         # 0.5 m² of parts
        r = consolidate.consolidate(_inputs(
            A={"ply": {"P@18": big}, "lam": {}, "edges": {}},
            B={"ply": {"P@18": small}, "lam": {}, "edges": {}},
        ))
        m = r["materials"]["P@18"]
        self.assertEqual(m["combined"], 1)
        share_a = m["alloc"]["A"] / (m["alloc"]["A"] + m["alloc"]["B"])
        self.assertAlmostEqual(share_a, 1.2 / 1.7, places=2)

    def test_edge_rolls_combine(self):
        # 30 m + 30 m: alone 1 roll each (2 total), together 2 rolls of 50 m?
        # no — 60 m -> 2 rolls; but 20 m + 20 m -> alone 2 rolls, together 1
        r = consolidate.consolidate(_inputs(
            A={"ply": {"P@18": [(100, 100)]}, "lam": {}, "edges": {"EB_X": 20.0}},
            B={"ply": {"P@18": [(100, 100)]}, "lam": {}, "edges": {"EB_X": 20.0}},
        ))
        m = r["materials"]["EB_X"]
        self.assertEqual(m["standalone"], 2)
        self.assertEqual(m["combined"], 1)
        self.assertAlmostEqual(m["alloc"]["A"], 0.5, places=3)

    def test_sheet_ratio_drives_ops(self):
        half = [(2400, 590)]
        r = consolidate.consolidate(_inputs(
            WAR={"ply": {"P@18": half}, "lam": {}, "edges": {}},
            BED={"ply": {"P@18": half}, "lam": {}, "edges": {}},
        ))
        # 1 standalone sheet each -> 0.5 allocated each -> ratio 0.5
        self.assertAlmostEqual(r["sheet_ratio"]["WAR"], 0.5, places=3)

    def test_single_sku_is_neutral(self):
        r = consolidate.consolidate(_inputs(
            WAR={"ply": {"P@18": [(2000, 1000)]}, "lam": {}, "edges": {"EB_X": 10}},
        ))
        m = r["materials"]["P@18"]
        self.assertEqual(m["combined"], m["standalone"])
        self.assertAlmostEqual(r["sheet_ratio"]["WAR"], 1.0, places=3)

    def test_batch_factor(self):
        tiers = [(0, 1.0), (10, 0.85), (30, 0.7)]
        self.assertEqual(consolidate.batch_factor(tiers, 5), 1.0)
        self.assertEqual(consolidate.batch_factor(tiers, 10), 0.85)
        self.assertEqual(consolidate.batch_factor(tiers, 50), 0.7)
        self.assertEqual(consolidate.batch_factor([], 50), 1.0)
        self.assertEqual(consolidate.batch_factor(None, 5), 1.0)



class TestModeGuard(unittest.TestCase):
    def test_split_and_mixed(self):
        modes = {"A": "CSV-Nest", "B": "OCL PDF (standard)", "C": "CSV-Nest"}
        csv_nest, pdf = consolidate.split_by_mode(modes)
        self.assertEqual(csv_nest, ["A", "C"])
        self.assertEqual(pdf, ["B"])
        self.assertTrue(consolidate.is_mixed(modes))

    def test_homogeneous_is_not_mixed(self):
        self.assertFalse(consolidate.is_mixed({"A": "CSV-Nest", "B": "CSV-Nest"}))
        self.assertFalse(consolidate.is_mixed({"A": "OCL PDF (standard)"}))
        self.assertFalse(consolidate.is_mixed({}))

    def test_blank_mode_counts_as_pdf(self):
        # legacy SKUs predate the field: they are PDF-mode by definition
        self.assertFalse(consolidate.is_mixed({"A": None, "B": ""}))
        self.assertTrue(consolidate.is_mixed({"A": None, "B": "CSV-Nest"}))


class TestIntakeRowMode(unittest.TestCase):
    def test_files_decide_the_mode(self):
        self.assertEqual(consolidate.intake_row_mode(True, False), "CSV-Nest")
        self.assertEqual(consolidate.intake_row_mode(False, True), "OCL PDF (standard)")
        self.assertIsNone(consolidate.intake_row_mode(False, False))

    def test_both_files_is_ambiguous(self):
        with self.assertRaises(ValueError):
            consolidate.intake_row_mode(True, True)


class TestStoredModeField(unittest.TestCase):
    """Estimate.estimation_mode is a stored Select whose options are typed out
    in the DocType JSON, while the value written into it comes from
    consolidate. If either constant is renamed and the JSON is not, every save
    would silently drop the mode (a Select rejects unknown values) and the list
    view would go blank — so pin them together."""

    def _options(self):
        import json
        import os
        here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path = os.path.join(here, "mallet_estimator", "doctype", "estimate", "estimate.json")
        with open(path) as fh:
            meta = json.load(fh)
        field = next(f for f in meta["fields"] if f["fieldname"] == "estimation_mode")
        return [o for o in field["options"].split("\n") if o]

    def test_json_options_match_the_engine_constants(self):
        self.assertEqual(sorted(self._options()),
                         sorted([consolidate.CSV_MODE, consolidate.PDF_MODE]))

    def test_blank_is_selectable_for_an_empty_estimate(self):
        # an estimate with no SKUs is committed to NEITHER mode, so the Select
        # must accept "" — a leading empty option is how Frappe spells that
        import json
        import os
        here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path = os.path.join(here, "mallet_estimator", "doctype", "estimate", "estimate.json")
        with open(path) as fh:
            meta = json.load(fh)
        field = next(f for f in meta["fields"] if f["fieldname"] == "estimation_mode")
        self.assertTrue(field["options"].startswith("\n"))


if __name__ == "__main__":
    unittest.main()


class TestSlotLettersAreNotShared(unittest.TestCase):
    """Slot letters are PER SKU. `b` on the wardrobe and `b` on the bed are two
    independent names that usually mean two different laminates, and nesting
    keyed on the raw OpenCutList code pooled them anyway — packing physically
    different sheets as one. The damage points the wrong way: FEWER sheets than
    will be bought, and a shared-material saving that does not exist."""

    def _two_skus(self, key_a, key_b, parts):
        return {"WAR": {"ply": {}, "lam": {key_a: parts}, "edges": {}},
                "BED": {"ply": {}, "lam": {key_b: parts}, "edges": {}}}

    def test_the_same_letter_meaning_two_laminates_is_not_one_material(self):
        parts = [(900.0, 450.0)] * 2
        pooled = consolidate.consolidate(self._two_skus("X_b", "X_b", parts))["materials"]
        apart = consolidate.consolidate(
            self._two_skus("X_MER1834", "X_VM6534", parts))["materials"]
        self.assertEqual(len(pooled), 1, "the generic letter collapses them to one bucket")
        self.assertEqual(len(apart), 2, "resolved décors stay two materials")
        # and the pooled answer under-buys: one sheet fewer than the job needs
        self.assertLess(sum(v["combined"] for v in pooled.values()),
                        sum(v["combined"] for v in apart.values()))

    def test_the_same_real_laminate_in_two_skus_still_pools(self):
        # the saving is real when the letters point at the SAME material —
        # that is the case cross-SKU nesting exists for, and it must survive
        parts = [(900.0, 450.0)] * 2
        same = consolidate.consolidate(
            self._two_skus("X_MER1834", "X_MER1834", parts))["materials"]
        self.assertEqual(len(same), 1)
        self.assertEqual(set(same["X_MER1834"]["alloc"]), {"WAR", "BED"})


class TestOffcutCredit(unittest.TestCase):
    """Retained offcuts come off the job. Not all of their value, though: a
    kept piece is worth something only once it is used, and crediting a whole
    board back would discount the client for a piece that may sit on the rack."""

    def _panel(self, key):
        p = [(1200.0, 600.0)] * 3
        return {"A": {"ply": {key: p}, "lam": {}, "edges": {}},
                "B": {"ply": {key: p}, "lam": {}, "edges": {}}}

    def test_no_policy_means_no_credit(self):
        m = consolidate.consolidate(self._panel("PANEL_V0_16mm_GE_GE"),
                                    recovery_pct=0)["materials"]["PANEL_V0_16mm_GE_GE"]
        self.assertEqual(m["credit"], 0.0)
        self.assertEqual(m["billable"], float(m["combined"]))

    def test_a_policy_credits_part_of_a_board_never_all_of_it(self):
        m = consolidate.consolidate(self._panel("PANEL_V0_16mm_GE_GE"),
                                    recovery_pct=60)["materials"]["PANEL_V0_16mm_GE_GE"]
        self.assertGreater(m["credit"], 0)
        self.assertLess(m["billable"], m["combined"])
        self.assertGreaterEqual(m["billable"], 1.0, "a board can never be un-bought")

    def test_an_external_panel_is_never_retained(self):
        # this client's laminate; worth nothing on the next job
        only_v0 = lambda k: k.startswith("PANEL_V0_")
        m = consolidate.consolidate(self._panel("PANEL_V1_16mm_GE_ME"), recovery_pct=60,
                                    retainable=only_v0)["materials"]["PANEL_V1_16mm_GE_ME"]
        self.assertEqual(m["credit"], 0.0)

    def test_the_credit_reaches_every_sku_by_part_area_share(self):
        m = consolidate.consolidate(self._panel("PANEL_V0_16mm_GE_GE"),
                                    recovery_pct=60)["materials"]["PANEL_V0_16mm_GE_GE"]
        self.assertEqual(set(m["alloc"]), {"A", "B"})
        self.assertAlmostEqual(sum(m["alloc"].values()), m["billable"], places=2)


class TestLaminateFollowsThePanelAcrossSKUs(unittest.TestCase):
    """Laminate is pressed onto whole boards, so pooling two SKUs' boards pools
    their laminate too — and the offcut credit runs through the sandwich, since
    a retained offcut is a LAMINATED board the client did not consume."""

    PANEL = "PANEL_V0_16mm_1834_1834"
    LAM = "SG_LAM_V0_16mm_a_a"

    def _inputs(self, sku, parts):
        area = sum(l * w for (l, w) in parts)
        return {sku: {
            "ply": {self.PANEL: parts},
            "faces": {self.PANEL: {"Frontside": {self.LAM: area},
                                   "Backside": {self.LAM: area}}},
            "edges": {},
        }}

    def test_laminate_is_two_per_pooled_board(self):
        # each SKU's parts alone need a board; together they share one
        half = [(2400.0, 580.0)]   # two of these share one board's width
        inputs = {}
        inputs.update(self._inputs("A", half))
        inputs.update(self._inputs("B", half))
        out = consolidate.consolidate(inputs)
        boards = out["materials"][self.PANEL]["combined"]
        self.assertEqual(boards, 1)
        # one board, laminated both faces -> 2 sheets, not 2 per SKU
        self.assertEqual(out["materials"][self.LAM]["combined"], 2 * boards)

    def test_laminate_allocation_follows_the_boards(self):
        # A brings three times B's area, so it pays three quarters of the laminate
        out = consolidate.consolidate({
            **self._inputs("A", [(1200.0, 600.0)] * 3),
            **self._inputs("B", [(1200.0, 600.0)]),
        })
        alloc = out["materials"][self.LAM]["alloc"]
        self.assertAlmostEqual(alloc["A"] / (alloc["A"] + alloc["B"]), 0.75, places=3)

    def test_laminate_is_never_nested_on_its_own(self):
        # 2 boards laminated both sides is 4 sheets. Nesting the laminate to
        # part size would fit those parts on fewer, and buy laminate the press
        # never presses.
        parts = [(2400.0, 1180.0), (2400.0, 1180.0)]   # one full board each
        out = consolidate.consolidate(self._inputs("A", parts))
        self.assertEqual(out["materials"][self.PANEL]["combined"], 2)
        self.assertEqual(out["materials"][self.LAM]["combined"], 4)
        self.assertIsNone(out["materials"][self.LAM]["util"])

    def test_offcut_credit_carries_through_the_sandwich(self):
        # a recovery policy that discounts the board must discount its laminate
        # by the same fraction — the retained offcut is already pasted
        parts = [(1200.0, 600.0)] * 5
        plain = consolidate.consolidate(self._inputs("A", parts))
        credited = consolidate.consolidate(self._inputs("A", parts), recovery_pct=60)
        self.assertLess(credited["materials"][self.PANEL]["billable"],
                        plain["materials"][self.PANEL]["billable"])
        self.assertLess(credited["materials"][self.LAM]["billable"],
                        plain["materials"][self.LAM]["billable"])

    def test_drivers_stashed_before_faces_still_cost(self):
        # an SKU saved before this rule existed has no `faces` blob: fall back
        # to nesting its laminate rather than dropping the line altogether
        out = consolidate.consolidate({"A": {
            "ply": {self.PANEL: [(1200.0, 600.0)]},
            "lam": {self.LAM: [(1200.0, 600.0), (1200.0, 600.0)]},
            "edges": {},
        }})
        self.assertEqual(out["materials"][self.LAM]["kind"], "laminate")
        self.assertGreaterEqual(out["materials"][self.LAM]["combined"], 1)


class TestPressList(unittest.TestCase):
    """The lamination station's work order: which laminate goes on which face
    of which board stack, off the same panel nest the pricing uses."""

    PANEL_V0 = "PANEL_V0_16mm_GE1834_GE1834"
    PANEL_V1 = "PANEL_V1_16mm_GE1834_VM1834"

    def _inputs(self):
        v0_parts = [(2400.0, 1180.0)] * 2          # two full boards
        v1_parts = [(2400.0, 1180.0)]              # one full board
        a = 2400.0 * 1180.0
        return {"WAR": {
            "ply": {self.PANEL_V0: v0_parts, self.PANEL_V1: v1_parts},
            "faces": {
                self.PANEL_V0: {"Frontside": {"SG_LAM_GE1834": a * 2},
                                "Backside": {"SG_LAM_GE1834": a * 2}},
                self.PANEL_V1: {"Frontside": {"SG_LAM_VM1834": a},
                                "Backside": {"SG_LAM_GE1834": a}},
            },
            "edges": {},
        }}

    def test_one_sheet_per_board_per_face(self):
        inputs = self._inputs()
        result = consolidate.consolidate(inputs)
        rows = {r["panel"]: r for r in consolidate.press_list(inputs, result["materials"])}
        v0 = rows[self.PANEL_V0]
        self.assertEqual(v0["boards"], 2)
        # both faces internal: 2 sheets front + 2 back, same code
        self.assertEqual(sorted((f["face"], f["code"], f["sheets"]) for f in v0["faces"]),
                         [("Backside", "SG_LAM_GE1834", 2.0),
                          ("Frontside", "SG_LAM_GE1834", 2.0)])
        v1 = rows[self.PANEL_V1]
        self.assertEqual([(f["face"], f["code"], f["sheets"]) for f in sorted(
            v1["faces"], key=lambda f: f["face"])],
            [("Backside", "SG_LAM_GE1834", 1.0), ("Frontside", "SG_LAM_VM1834", 1.0)])

    def test_press_uses_combined_not_billable(self):
        # a recovery credit reduces the BILL, never the pressing — the board
        # exists and gets pasted either way
        inputs = self._inputs()
        credited = consolidate.consolidate(inputs, recovery_pct=60)
        rows = {r["panel"]: r for r in consolidate.press_list(inputs, credited["materials"])}
        self.assertEqual(rows[self.PANEL_V0]["boards"],
                         credited["materials"][self.PANEL_V0]["combined"])

    def test_press_reconciles_to_the_bom_laminate(self):
        # sum of press sheets per code == the laminate the BOM buys (before
        # credit), because both come off the same panel nest
        inputs = self._inputs()
        result = consolidate.consolidate(inputs)
        totals = {}
        for r in consolidate.press_list(inputs, result["materials"]):
            for f in r["faces"]:
                totals[f["code"]] = totals.get(f["code"], 0.0) + f["sheets"]
        self.assertEqual(totals["SG_LAM_GE1834"], 5.0)   # 2+2 on V0, 1 on V1 back
        self.assertEqual(totals["SG_LAM_VM1834"], 1.0)
        self.assertEqual(result["materials"]["SG_LAM_GE1834"]["combined"], 5)
        self.assertEqual(result["materials"]["SG_LAM_VM1834"]["combined"], 1)

    def test_a_mixed_face_splits_by_part_area(self):
        a = 2400.0 * 1180.0
        inputs = {"X": {
            "ply": {"PANEL_V1_16mm_GE1834_MIX": [(2400.0, 1180.0)] * 4},
            "faces": {"PANEL_V1_16mm_GE1834_MIX": {
                "Frontside": {"SG_LAM_VM1834": a * 3, "SG_LAM_VM6534": a}}},
            "edges": {},
        }}
        result = consolidate.consolidate(inputs)
        row = consolidate.press_list(inputs, result["materials"])[0]
        got = {f["code"]: f["sheets"] for f in row["faces"]}
        self.assertEqual(got, {"SG_LAM_VM1834": 3.0, "SG_LAM_VM6534": 1.0})
