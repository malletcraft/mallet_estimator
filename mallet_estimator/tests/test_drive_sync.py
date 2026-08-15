# Pure unit tests for the Drive sync decisions — no database, no frappe, no
# network.  python -m unittest mallet_estimator.tests.test_drive_sync
#
# The contract: never upload the same face twice, and never attach a photo to
# a capture we cannot prove it belongs to.
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
        self.assertIn(f"{PHOTO}_ceiling.jpg", [p["filename"] for p in plan])

    def test_rerunning_a_handover_uploads_nothing(self):
        # Otherwise the same wall appears twice in ImageMeter with no way to
        # tell which one is current.
        first = D.plan_uploads(PHOTO, FACES)
        again = D.plan_uploads(PHOTO, FACES,
                               existing_filenames=[p["filename"] for p in first])
        self.assertEqual(again, [])

    def test_only_the_missing_face_is_re_sent(self):
        done = [f"{PHOTO}_front.jpg", f"{PHOTO}_right.jpg", f"{PHOTO}_back.jpg",
                f"{PHOTO}_left.jpg", f"{PHOTO}_ceiling.jpg"]
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
            {"id": "x1", "title": f"{PHOTO}_ceiling.jpg"}, known_photos=[PHOTO])
        self.assertEqual(action, D.ATTACH)
        self.assertEqual(payload["photo"], PHOTO)
        self.assertEqual(payload["face"], "up")     # Ceiling is the up face

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


if __name__ == "__main__":
    unittest.main()
