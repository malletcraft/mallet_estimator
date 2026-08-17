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
