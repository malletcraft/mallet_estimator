# Pure unit tests for the work-stage and article masters — no database.
#   python -m unittest mallet_estimator.tests.test_worksite
#
# What is worth asserting here is the ORDER. A wrong sequence still saves,
# still renders, and still looks like a list of stages; it is only wrong on
# site, months later, when somebody photographs 'Putty & primer' before the
# moulding is up and nobody can say when the rule was lost.
import unittest

from mallet_estimator import worksite_data as W


def jobs(row_job_types):
    return [j.strip() for j in row_job_types.split(",")]


class TestWorkStageOrder(unittest.TestCase):

    def test_sequences_strictly_increase(self):
        seqs = [r[0] for r in W.WORK_STAGES]
        self.assertEqual(seqs, sorted(seqs))
        self.assertEqual(len(seqs), len(set(seqs)), "duplicate sequence number")

    def test_sequences_leave_room_to_insert(self):
        # Steps of 10 are what let 'Anti-termite treatment' be added between
        # two stages without renumbering thirty-nine rows.
        gaps = [b - a for a, b in zip([r[0] for r in W.WORK_STAGES],
                                      [r[0] for r in W.WORK_STAGES][1:])]
        self.assertTrue(all(g >= 10 for g in gaps), f"tight gaps: {gaps}")

    def test_stage_names_are_unique(self):
        names = [r[2] for r in W.WORK_STAGES]
        self.assertEqual(len(names), len(set(names)))

    def test_phases_appear_in_trade_order_and_never_twice(self):
        seen = []
        for _seq, phase, _stage, _jobs, _note in W.WORK_STAGES:
            if not seen or seen[-1] != phase:
                self.assertNotIn(phase, seen,
                                 f"{phase} is split across the sequence")
                seen.append(phase)
        self.assertEqual(tuple(seen), W.PHASES)

    def test_every_phase_declared_is_actually_used(self):
        used = {r[1] for r in W.WORK_STAGES}
        self.assertEqual(used, set(W.PHASES))

    def test_services_close_before_the_ceiling_does(self):
        # First fix is concealed work. Once the ceiling boards up or the wall
        # is plastered, changing it is the most expensive mistake on the job.
        self.assertLess(self._seq("Electrical first fix — conduit & wiring"),
                        self._seq("False ceiling framing (GI grid)"))
        self.assertLess(self._seq("Plumbing first fix — concealed lines"),
                        self._seq("Wall tiling / dado"))

    def test_the_ceiling_finishes_before_paint(self):
        # POP is the dirtiest trade in the flat.
        self.assertLess(self._seq("POP / gypsum board & finish"),
                        self._seq("Paint, wallpaper or texture"))

    def test_moulding_goes_up_before_primer(self):
        # You fix it, fill the pin-holes, sand, and then paint over the lot.
        self.assertLess(self._seq("Wall moulding & trims"),
                        self._seq("Putty & primer"))
        self.assertLess(self._seq("Wall panelling"),
                        self._seq("Putty & primer"))

    def test_handover_is_last(self):
        self.assertEqual(W.WORK_STAGES[-1][2], "Handover")

    def _seq(self, stage):
        for seq, _phase, name, _jobs, _note in W.WORK_STAGES:
            if name == stage:
                return seq
        self.fail(f"no such stage: {stage}")


class TestJobTypes(unittest.TestCase):

    def test_every_stage_names_only_real_job_types(self):
        for _seq, _phase, stage, j, _note in W.WORK_STAGES:
            for name in jobs(j):
                self.assertIn(name, W.JOB_TYPES, f"{stage}: {name}")

    def test_every_job_type_can_reach_some_stage(self):
        # An empty picker on a phone is the worst place to find this out.
        for job in W.JOB_TYPES:
            reachable = [r for r in W.WORK_STAGES if job in jobs(r[3])]
            self.assertTrue(reachable, f"{job} reaches nothing")

    def test_every_job_type_can_start_and_finish(self):
        for job in W.JOB_TYPES:
            phases = {r[1] for r in W.WORK_STAGES if job in jobs(r[3])}
            self.assertIn("Survey", phases, f"{job} cannot record what it found")
            self.assertIn("Closing", phases, f"{job} cannot be handed over")

    def test_repair_and_install_are_a_slice_of_the_same_sequence(self):
        # The whole point: they share stages with new work rather than owning
        # a private list. If either stopped sharing, this is where it shows.
        shared_rp = [r[2] for r in W.WORK_STAGES
                     if W.REPAIR in jobs(r[3]) and W.NEW in jobs(r[3])]
        shared_si = [r[2] for r in W.WORK_STAGES
                     if W.INSTALL in jobs(r[3]) and W.NEW in jobs(r[3])]
        self.assertGreater(len(shared_rp), 5, shared_rp)
        self.assertGreater(len(shared_si), 5, shared_si)

    def test_the_job_specific_stages_are_exactly_the_ones_intended(self):
        only = {}
        for _seq, _phase, stage, j, _note in W.WORK_STAGES:
            names = jobs(j)
            if len(names) == 1 and names[0] != W.NEW:
                only.setdefault(names[0], []).append(stage)
        self.assertEqual(sorted(only.get(W.REPAIR, [])),
                         ["Defect recorded", "Rectified & verified"])
        self.assertEqual(sorted(only.get(W.INSTALL, [])), ["Opening measured"])


class TestArticles(unittest.TestCase):

    def test_codes_are_unique_and_shaped_like_sku_tokens(self):
        codes = [a[0] for a in W.ARTICLES]
        self.assertEqual(len(codes), len(set(codes)))
        for c in codes:
            self.assertRegex(c, r"^[A-Z0-9]{2,6}$")

    def test_every_article_names_only_real_job_types(self):
        for code, _name, j, _k, _b in W.ARTICLES:
            for name in jobs(j):
                self.assertIn(name, W.JOB_TYPES, code)

    def test_every_job_type_has_articles_to_pick_from(self):
        for job in W.JOB_TYPES:
            self.assertTrue([a[0] for a in W.ARTICLES if job in jobs(a[2])], job)

    def test_the_codes_the_estimator_already_uses_survive(self):
        # YS_MB_WAR and friends are live SKU codes; dropping WAR would orphan
        # every wardrobe ever priced.
        codes = {a[0] for a in W.ARTICLES}
        for c in ("WAR", "BED", "LOF", "STU", "TVU"):
            self.assertIn(c, codes)


class TestArticleCodeOwnsTheSkuToken(unittest.TestCase):
    """The master's code is the SKU token, and that is the point of it.

    abbr() reads 'PVC bathroom door' as three words and answers PVC_BAT_DOO —
    correct by its own rule, useless as a token. Before the master existed the
    article half of every SKU code was whatever prose happened to abbreviate
    to. These tests pin the handover: master wins, prose is the fallback, and
    a code that silently stopped being used would show up here."""

    def test_the_master_code_wins_over_the_derived_one(self):
        from mallet_estimator.estimator import article_token
        self.assertEqual(article_token("PVC bathroom door", "PVC"), "PVC")
        self.assertEqual(article_token("Study table", "STU"), "STU")

    def test_an_article_with_no_master_row_still_gets_a_token(self):
        from mallet_estimator.estimator import abbr, article_token
        self.assertEqual(article_token("Some New Thing"), abbr("Some New Thing"))
        self.assertEqual(article_token("Some New Thing", ""), abbr("Some New Thing"))

    def test_a_lowercase_master_code_is_still_a_token(self):
        from mallet_estimator.estimator import article_token
        self.assertEqual(article_token("Wardrobe", "war"), "WAR")

    def test_the_seeded_codes_produce_the_codes_the_shop_already_writes(self):
        from mallet_estimator.estimator import sku_code
        by_name = {a[1]: a[0] for a in W.ARTICLES}
        cases = [
            ("Yogesh Sahasrabudhe", "Master Bedroom", "Wardrobe",  "YS_MB_WAR"),
            ("Yogesh Sahasrabudhe", "Master Bedroom", "Bed",       "YS_MB_BED"),
            ("Yogesh Sahasrabudhe", "Kitchen",        "Kitchen base", "YS_KIT_KIT"),
            ("Yogesh Sahasrabudhe", "Toilet",         "PVC bathroom door", "YS_TOI_PVC"),
            ("Sameer Kulkarni",     "Living Room",    "TV unit",   "SK_LR_TVU"),
        ]
        for customer, room, article, want in cases:
            self.assertEqual(
                sku_code(customer, room, article, by_name[article]), want,
                f"{customer}/{room}/{article}")

    def test_no_two_articles_share_a_token(self):
        # Two articles with one token would collide in the SKU code the moment
        # they landed in the same room, and _unique_code would quietly suffix
        # one of them — a code nobody can read back.
        codes = [a[0] for a in W.ARTICLES]
        self.assertEqual(len(codes), len(set(codes)))


class TestStageRenames(unittest.TestCase):

    def test_each_old_word_maps_onto_a_real_phase(self):
        for old, new in W.STAGE_RENAMES.items():
            self.assertIn(new, W.PHASES, f"{old} -> {new}")

    def test_all_six_legacy_values_are_covered(self):
        self.assertEqual(sorted(W.STAGE_RENAMES),
                         ["Baseline", "Carpentry", "Civil", "Finishing",
                          "Handover", "Wiring"])

    def test_an_unknown_word_passes_through_untouched(self):
        # The map is applied with .get(x, x) on every incoming stage, so a
        # value that is already a new phase name must not be mangled.
        for phase in W.PHASES:
            self.assertEqual(W.STAGE_RENAMES.get(phase, phase), phase)

    def test_the_map_does_not_collapse_two_words_into_one_phase(self):
        # A collapse would make the rename lossy, and the legacy field the
        # only record of which of the two a photo used to be.
        targets = list(W.STAGE_RENAMES.values())
        self.assertEqual(len(targets), len(set(targets)))


class TestSiteKey(unittest.TestCase):

    def test_spacing_case_and_underscores_do_not_make_a_second_site(self):
        for a, b in [("Kothrud Flat", "kothrud_flat"),
                     ("Kothrud  Flat", " Kothrud Flat "),
                     ("Lonavala_Bungalow", "lonavala bungalow")]:
            self.assertEqual(W.site_key(a), W.site_key(b), (a, b))

    def test_different_places_stay_different(self):
        self.assertNotEqual(W.site_key("Kothrud Flat"), W.site_key("Baner Flat"))

    def test_empty_input_is_survivable(self):
        self.assertEqual(W.site_key(None), "")
        self.assertEqual(W.site_key("   "), "")


if __name__ == "__main__":
    unittest.main()


class TestArticleKindAndBasis(unittest.TestCase):
    """Who does the work, and what unit it is quoted in.

    Amit, 2026-08-21: "pop work is esimated by sqft, electrical work by
    number of points, running feet of wire installations, number of fan and
    light installations, tile work is done by sqft ... depending on the volume
    of the work it can be estimated lumpsum as well depending on vendor."
    """

    def test_every_article_declares_a_kind_and_a_basis(self):
        for a in W.ARTICLES:
            self.assertEqual(len(a), 5, f"{a[0]} is not (code, name, jobs, kind, basis)")
            self.assertIn(a[3], W.KINDS, a[0])
            self.assertIn(a[4], W.BASES, a[0])

    def test_electrical_is_three_articles_not_one_with_three_numbers(self):
        """That is how the vendor quotes it — so many points, so many feet of
        wire, so many fittings hung. Splitting it keeps ONE number per line,
        which keeps the site UI a single field instead of a spreadsheet in a
        dusty flat."""
        by_code = {a[0]: a for a in W.ARTICLES}
        self.assertEqual(by_code["ELP"][4], W.POINT, "points are counted")
        self.assertEqual(by_code["ELW"][4], W.RFT, "wire runs in feet")
        self.assertEqual(by_code["ELF"][4], W.NOS, "fittings are counted")

    def test_the_trades_that_are_given_away_are_subcontract(self):
        """POP, tiling, plaster, wiring and paint are not made in the shop.
        The kind is not a label: it decides whether an SKU needs a cut list
        and a Work Order at all."""
        by_code = {a[0]: a for a in W.ARTICLES}
        for code in ("POP", "TIL", "PNT", "PLS", "ELP", "ELW", "PLM", "DEM"):
            self.assertEqual(by_code[code][3], W.SUBCONTRACT, code)

    def test_the_shops_own_work_is_build(self):
        by_code = {a[0]: a for a in W.ARTICLES}
        for code in ("WAR", "BED", "KIT", "LOF", "TVU"):
            self.assertEqual(by_code[code][3], W.BUILD, code)

    def test_a_lumpsum_escape_exists(self):
        """A vendor who prices a whole flat's wiring as one figure is not a
        measurement error — it is how the deal was struck."""
        by_code = {a[0]: a for a in W.ARTICLES}
        self.assertEqual(by_code["SUB"][4], W.LUMPSUM)
        self.assertEqual(by_code["SUB"][3], W.SUBCONTRACT)

    def test_area_work_is_quoted_by_area(self):
        by_code = {a[0]: a for a in W.ARTICLES}
        for code in ("POP", "TIL", "PNT", "WIN", "GRL", "WLP"):
            self.assertEqual(by_code[code][4], W.SQFT, code)

    def test_every_kind_and_every_basis_is_actually_used(self):
        """A vocabulary with an unused word in it is a vocabulary somebody
        will misuse."""
        kinds = {a[3] for a in W.ARTICLES}
        bases = {a[4] for a in W.ARTICLES}
        self.assertEqual(kinds, set(W.KINDS))
        self.assertEqual(bases, set(W.BASES))

    def test_a_subcontract_article_still_reaches_the_stages_it_belongs_to(self):
        """Filing is the point: a wall that needs POP must have somewhere to
        put it, on a job type that actually reaches POP's stage."""
        by_code = {a[0]: a for a in W.ARTICLES}
        jobs = [j.strip() for j in by_code["POP"][2].split(",")]
        self.assertIn(W.NEW, jobs)
        reach = [s for s in W.WORK_STAGES if "POP" in s[2] and W.NEW in s[3]]
        self.assertTrue(reach, "no New-work stage covers POP")
