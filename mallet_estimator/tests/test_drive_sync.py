# Pure unit tests for the Drive sync decisions — no database, no frappe, no
# network.  python -m unittest mallet_estimator.tests.test_drive_sync
#
# The contract: never upload the same face twice, and never attach a photo to
# a capture we cannot prove it belongs to.
import datetime
import unittest

from mallet_estimator import drive_sync as D

FACES = {f: f"/private/files/MEST-PH-2026-00001_{f}.jpg"
         for f in ("front", "right", "back", "left", "up", "down")}
PHOTO = "MEST-PH-2026-00001"


class TestPlanUploads(unittest.TestCase):
    def test_a_fresh_capture_hands_over_all_six(self):
        plan = D.plan_uploads(PHOTO, FACES)
        self.assertEqual(len(plan), 6)
        self.assertEqual({p["face"] for p in plan}, set(FACES))
        self.assertIn(f"{PHOTO}_top.jpg", [p["filename"] for p in plan])

    def test_rerunning_a_handover_uploads_nothing(self):
        # Otherwise the same wall appears twice in ImageMeter with no way to
        # tell which one is current.
        first = D.plan_uploads(PHOTO, FACES)
        again = D.plan_uploads(PHOTO, FACES,
                               existing_filenames=[p["filename"] for p in first])
        self.assertEqual(again, [])

    def test_only_the_missing_face_is_re_sent(self):
        done = [f"{PHOTO}_front.jpg", f"{PHOTO}_right.jpg", f"{PHOTO}_back.jpg",
                f"{PHOTO}_left.jpg", f"{PHOTO}_top.jpg"]
        plan = D.plan_uploads(PHOTO, FACES, existing_filenames=done)
        self.assertEqual([p["face"] for p in plan], ["down"])

    def test_existing_names_match_regardless_of_case(self):
        plan = D.plan_uploads(PHOTO, FACES,
                              existing_filenames=[f"{PHOTO}_FRONT.JPG"])
        self.assertNotIn("front", [p["face"] for p in plan])

    def test_a_capture_that_never_split_hands_over_nothing(self):
        self.assertEqual(D.plan_uploads(PHOTO, {f: "" for f in FACES}), [])


class TestClassifyReturn(unittest.TestCase):
    def test_a_file_we_already_took_is_skipped(self):
        action, payload = D.classify_return(
            {"id": "abc", "title": f"{PHOTO}_front.jpg"}, imported_file_ids=["abc"])
        self.assertEqual(action, D.SKIP)
        self.assertIn("already", payload["reason"])

    def test_a_file_naming_its_capture_attaches_itself(self):
        action, payload = D.classify_return(
            {"id": "x1", "title": f"{PHOTO}_top.jpg"}, known_photos=[PHOTO])
        self.assertEqual(action, D.ATTACH)
        self.assertEqual(payload["photo"], PHOTO)
        self.assertEqual(payload["face"], "up")     # Top is the up face

    def test_imagemeters_own_naming_goes_to_a_person(self):
        # This is the ordinary case: ImageMeter renames exports after the date.
        action, payload = D.classify_return(
            {"id": "x2", "title": "image_from_15._Aug_2026-2.jpg",
             "parents_path": ["yogesh_sar", "master_Bad"]}, known_photos=[PHOTO])
        self.assertEqual(action, D.REVIEW)
        self.assertEqual(payload["folder"], ["yogesh_sar", "master_Bad"])

    def test_an_unknown_capture_id_is_never_invented(self):
        # Looks like ours, names a capture this site does not have.
        action, payload = D.classify_return(
            {"id": "x3", "title": "MEST-PH-2099-00042_front.jpg"},
            known_photos=[PHOTO])
        self.assertEqual(action, D.REVIEW)
        self.assertIn("no such capture", payload["reason"])

    def test_a_known_capture_with_no_face_still_asks(self):
        action, payload = D.classify_return(
            {"id": "x4", "title": f"{PHOTO}.jpg"}, known_photos=[PHOTO])
        self.assertEqual(action, D.REVIEW)
        self.assertEqual(payload["photo"], PHOTO)

    def test_summary_counts_every_outcome(self):
        decisions = [
            D.classify_return({"id": "a", "title": f"{PHOTO}_front.jpg"}, known_photos=[PHOTO]),
            D.classify_return({"id": "b", "title": "image_from_1._Jan_2026.jpg"}, known_photos=[PHOTO]),
            D.classify_return({"id": "c", "title": "x.jpg"}, imported_file_ids=["c"]),
        ]
        self.assertEqual(D.summarise(decisions),
                         {D.ATTACH: 1, D.REVIEW: 1, D.SKIP: 1})


DEV = "MCAP-a1b2c3d4e5f6"


class TestDeviceBornCaptures(unittest.TestCase):
    """A capture made on a phone with no signal reaches ImageMeter under the
    id the DEVICE minted, because the server had not named it yet. The face
    that comes back therefore says MCAP-… where every existing file says
    MEST-PH-…, and nothing else in it identifies the wall."""

    def test_a_synced_device_capture_attaches_to_its_real_docname(self):
        action, payload = D.classify_return(
            {"id": "d1", "title": f"{DEV}_top.jpg"},
            known_photos=[PHOTO], device_ids={DEV: PHOTO})
        self.assertEqual(action, D.ATTACH)
        self.assertEqual(payload["photo"], PHOTO, "must resolve to the docname")
        self.assertEqual(payload["face"], "up", "top is the up face")

    def test_an_unsynced_device_capture_is_asked_about_never_guessed(self):
        # Annotated before the phone ever reached the server. The face is
        # ours, but the capture is not here yet — inventing a link would file
        # somebody's wall against whichever room happened to look close.
        action, payload = D.classify_return(
            {"id": "d2", "title": f"{DEV}_front.jpg"},
            known_photos=[PHOTO], device_ids={})
        self.assertEqual(action, D.REVIEW)
        self.assertIn("not synced yet", payload["reason"])
        self.assertIn(DEV, payload["reason"])

    def test_a_server_born_id_is_unaffected_by_the_new_shape(self):
        action, payload = D.classify_return(
            {"id": "d3", "title": f"{PHOTO}_front.jpg"},
            known_photos=[PHOTO], device_ids={DEV: PHOTO})
        self.assertEqual(action, D.ATTACH)
        self.assertEqual(payload["photo"], PHOTO)

    def test_the_device_pattern_cannot_match_an_ordinary_word(self):
        from mallet_estimator import handover
        for junk in ("MCAP-notahexstrng", "MCAP-a1b2c3", "mcap-a1b2c3d4e5f6",
                     "MCAP-a1b2c3d4e5f6a", "recap-image.jpg", ""):
            self.assertFalse(handover.is_device_id(junk), junk)
        self.assertTrue(handover.is_device_id(DEV))


class TestNonImagesAreNotQuestions(unittest.TestCase):
    """ImageMeter keeps its own working files beside the photos. None can be a
    face, but each queued a review row nobody would ever resolve, so they came
    back every run. A queue is only useful while everything in it is a real
    question."""

    def test_imagemeters_own_exports_are_skipped(self):
        for name in ("Kids_Bedroom.xlsx", "Kids_Bedroom-copy.xlsx",
                     "measurements.pdf", "project.imm", "notes.txt"):
            action, payload = D.classify_return({"id": "z", "title": name},
                                                known_photos=[PHOTO])
            self.assertEqual(action, D.SKIP, name)
            self.assertEqual(payload["reason"], "not an image")

    def test_photos_are_still_looked_at(self):
        for name in (f"{PHOTO}_front.jpg", "image_from_1._Jan_2026.JPG",
                     "shot.jpeg", "scan.PNG", "phone.heic"):
            action, _ = D.classify_return({"id": "z", "title": name},
                                          known_photos=[PHOTO])
            self.assertNotEqual(action, D.SKIP, name)

    def test_an_already_imported_file_is_still_skipped_as_imported(self):
        # Order matters: a known file should report why it was really skipped.
        action, payload = D.classify_return(
            {"id": "dup", "title": f"{PHOTO}_front.jpg"}, imported_file_ids=["dup"])
        self.assertEqual(action, D.SKIP)
        self.assertEqual(payload["reason"], "already imported")


if __name__ == "__main__":
    unittest.main()


class TestSyncHealth(unittest.TestCase):
    """Whether the sync is alive — asked because nothing was asking.

    Amit, 2026-08-26, putting the in-house annotator on hold in favour of
    ImageMeter. That made this sync the only route a drawn measurement has to
    reach ERP, and verify_setup was checking only that it was CONFIGURED.
    "enabled, credential present" reads as healthy and said exactly that
    whether the sync ran an hour ago or died a week back.
    """

    def test_a_sync_that_never_ran_says_so_in_its_own_words(self):
        # Different from stale, and worth a different sentence: never-run
        # points at wiring, stale points at something that broke after
        # working. A single "unhealthy" would send you looking in one place
        # for two different faults.
        ok, why = D.sync_health(True, None)
        self.assertFalse(ok)
        self.assertIn("NEVER", why)

    def test_a_recent_run_is_healthy_and_says_when(self):
        ok, why = D.sync_health(True, 12)
        self.assertTrue(ok)
        self.assertIn("12 minutes", why)

    def test_two_missed_hours_are_a_blip_and_three_are_a_pattern(self):
        # It runs hourly. The line sits past two missed runs so a deploy or a
        # restart does not cry wolf, and inside three so a real stoppage is
        # caught the same day.
        self.assertTrue(D.sync_health(True, 110)[0])
        self.assertFalse(D.sync_health(True, 200)[0])

    def test_the_failure_says_how_long_not_just_that_it_is_stale(self):
        # "stale" is not actionable. "last completed 31 hours ago" tells you
        # roughly when it stopped, which is where you start looking.
        ok, why = D.sync_health(True, 31 * 60)
        self.assertFalse(ok)
        self.assertIn("31 hours", why)

    def test_a_deliberately_disabled_sync_is_not_a_failure(self):
        # Somebody turned it off on purpose. A health check that fails on a
        # deliberate choice is a check people learn to ignore — and then it is
        # worth nothing on the day it means something.
        ok, why = D.sync_health(False, None)
        self.assertTrue(ok)
        self.assertIn("not enabled", why)

    def test_the_gap_reads_as_a_person_would_say_it(self):
        self.assertEqual(D.human_gap(1), "1 minute")
        self.assertEqual(D.human_gap(59), "59 minutes")
        self.assertEqual(D.human_gap(60), "1 hour")
        self.assertEqual(D.human_gap(31 * 60), "31 hours")
        self.assertEqual(D.human_gap(60 * 24 * 3), "3 days")
        self.assertEqual(D.human_gap(-5), "0 minutes")


class TestMinutesSince(unittest.TestCase):
    """The never-run branch, which was unreachable until 2026-08-28.

    Watching the check go red on mcft-stg for the first time is what found
    this. Every stale case behaved; the never-run case reported "last
    completed 739855 days ago, and it runs hourly — annotations stopped
    coming back at that point", which is a stoppage message for a sync that
    had never started. The cause was not in sync_health at all: install.py
    asked "is last_sync truthy", and a CLEARED Frappe Datetime on a Single is
    stored as the string "0001-01-01 00:00:00", which is.
    """

    NOW = datetime.datetime(2026, 8, 28, 18, 0, 0)

    def test_the_zero_date_is_never_run_not_a_two_thousand_year_gap(self):
        # The literal value read back out of tabSingles on the live bench.
        zero = datetime.datetime(1, 1, 1, 0, 0, 0)
        self.assertIsNone(D.minutes_since(zero, self.NOW))
        ok, why = D.sync_health(True, D.minutes_since(zero, self.NOW))
        self.assertFalse(ok)
        self.assertIn("NEVER completed a run", why)
        # The distinction is the entire point of the branch: a fresh site is
        # told its wiring is unfinished, not that something broke.
        self.assertNotIn("stopped coming back", why)
        self.assertNotIn("739855", why)

    def test_absent_is_never_run(self):
        self.assertIsNone(D.minutes_since(None, self.NOW))
        self.assertIsNone(D.minutes_since("", self.NOW))

    def test_a_real_stamp_measures_the_real_gap(self):
        last = self.NOW - datetime.timedelta(minutes=57)
        self.assertAlmostEqual(D.minutes_since(last, self.NOW), 57.0, places=6)

    def test_the_boundary_matches_what_the_bench_did(self):
        # Proved against mcft-stg the same day: 185 minutes OK, 195 FAIL.
        for mins, expect_ok in ((185, True), (195, False)):
            last = self.NOW - datetime.timedelta(minutes=mins)
            ok, _ = D.sync_health(True, D.minutes_since(last, self.NOW))
            self.assertIs(ok, expect_ok, "%d minutes" % mins)

    def test_a_stamp_in_the_future_is_fresh_not_negative(self):
        # Clock skew between a worker and the web node, or a hand edit. It is
        # not a stoppage, and it must not read as one.
        last = self.NOW + datetime.timedelta(minutes=5)
        ok, why = D.sync_health(True, D.minutes_since(last, self.NOW))
        self.assertTrue(ok)
        self.assertIn("0 minutes", why)

    def test_the_floor_year_is_a_floor_not_the_zero_date_alone(self):
        # A 1999 stamp is as impossible as a year-1 one, and both mean the
        # same thing: nothing here came from a run.
        self.assertIsNone(D.minutes_since(datetime.datetime(1999, 12, 31), self.NOW))
        self.assertIsNotNone(D.minutes_since(datetime.datetime(2026, 1, 1), self.NOW))
