# Pure unit tests for the ImageMeter handover — no database, no frappe.
#   python -m unittest mallet_estimator.tests.test_handover
# The contract under test is identity: a face must come home from ImageMeter
# still saying which capture and which face it is, because ImageMeter renames
# exports after the date and keeps nothing else.
import io
import unittest

from PIL import Image

from mallet_estimator import handover as H


def _face(px=400, colour=(90, 100, 120)):
    buf = io.BytesIO()
    Image.new("RGB", (px, px), colour).save(buf, format="JPEG", quality=90)
    return buf.getvalue()


class TestCaption(unittest.TestCase):
    def test_a_caption_survives_a_round_trip_as_text(self):
        text = H.caption_text("MEST-PH-2026-00007", "Master Bedroom", "front",
                              "2026-08-15", "Carpentry")
        self.assertIn("MEST-PH-2026-00007", text)
        self.assertIn("Front", text)
        photo, face = H.parse_caption(text)
        self.assertEqual(photo, "MEST-PH-2026-00007")
        self.assertEqual(face, "front")

    def test_top_and_bottom_read_back_as_up_and_down(self):
        # The doctype field says up/down; the Drive files say top/bottom,
        # which is the naming already in use there.
        for face in ("up", "down"):
            text = H.caption_text("MEST-PH-2026-00001", "Kitchen", face, "2026-08-15")
            photo, got = H.parse_caption(text)
            self.assertEqual(got, face, text)
        self.assertIn("Top", H.caption_text("X", "K", "up"))
        self.assertIn("Bottom", H.caption_text("X", "K", "down"))

    def test_the_older_ceiling_floor_words_still_parse(self):
        # Anything already named that way must not become unmatchable.
        self.assertEqual(H.parse_caption("MEST-PH-2026-00001_ceiling.jpg")[1], "up")
        self.assertEqual(H.parse_caption("MEST-PH-2026-00001_floor.jpg")[1], "down")

    def test_our_own_filenames_parse(self):
        # Regression: \b does not fire between a digit and an underscore, so
        # the original pattern refused to read the very names we write —
        # which would have broken the whole automated return path.
        for face in ("front", "right", "back", "left", "top", "bottom"):
            fn = f"MEST-PH-2026-00001_{face}.jpg"
            photo, got = H.parse_caption(fn)
            self.assertEqual(photo, "MEST-PH-2026-00001", fn)
            self.assertEqual(got, H.LABEL_TO_FACE.get(face, face), fn)

    def test_a_near_miss_id_is_not_accepted(self):
        # A longer number, or an id glued to other text, is not our capture.
        self.assertEqual(H.parse_caption("MEST-PH-2026-000012.jpg"), (None, None))
        self.assertEqual(H.parse_caption("xMEST-PH-2026-00001.jpg"), (None, None))

    def test_a_foreign_file_is_refused_not_guessed(self):
        # ImageMeter's own exports look like this. Claiming one belongs to a
        # capture would attach a stranger's photo to a client's room.
        for junk in ("image_from_27._Jun_2026-4.jpg", "", None, "IMG_20260806_183352.jpg"):
            self.assertEqual(H.parse_caption(junk), (None, None), junk)

    def test_the_caption_is_added_below_not_painted_over(self):
        # Covering the photo could hide the defect being annotated.
        src = _face(400)
        out = H.caption_face(src, H.caption_text("MEST-PH-2026-00007", "MB", "front"))
        a = Image.open(io.BytesIO(src))
        b = Image.open(io.BytesIO(out))
        self.assertEqual(b.width, a.width)
        self.assertGreater(b.height, a.height)
        # the original pixels are untouched
        self.assertEqual(b.convert("RGB").getpixel((200, 200)),
                         a.convert("RGB").getpixel((200, 200)))

    def test_a_long_caption_still_fits_inside_the_strip(self):
        text = H.caption_text("MEST-PH-2026-00007",
                              "Master Bedroom With A Very Long Name Indeed",
                              "front", "2026-08-15", "Finishing")
        out = H.caption_face(_face(400), text)
        img = Image.open(io.BytesIO(out)).convert("RGB")
        # the strip stays dark at both ends => the text was shrunk, not clipped
        # (JPEG is lossy, so the bar colour is compared with tolerance)
        bottom = img.height - 3
        for x in (2, img.width - 3):
            px = img.getpixel((x, bottom))
            self.assertTrue(all(abs(a - b) <= 6 for a, b in zip(px, H.BAR_RGB)),
                            f"caption text ran into the edge at x={x}: {px}")


class TestLayout(unittest.TestCase):
    def test_folders_read_client_project_room_capture(self):
        f = H.handover_folders("Yogesh Sharma", "YS_1402_SKYI_Interior",
                               "Master Bedroom", "2026-08-15", "Carpentry")
        self.assertEqual(f, ["Yogesh_Sharma", "YS_1402_SKYI_Interior",
                             "Master_Bedroom", "2026-08-15_Carpentry"])

    def test_a_revisit_gets_its_own_folder(self):
        a = H.handover_folders("C", "P", "Kitchen", "2026-08-15", "Civil")
        b = H.handover_folders("C", "P", "Kitchen", "2026-09-02", "Carpentry")
        self.assertNotEqual(a[-1], b[-1], "a revisit must not overwrite the last visit")

    def test_path_parts_are_filesystem_safe(self):
        f = H.handover_folders("A/B \\ C", "P:1*?", "Room <1>", "2026-08-15")
        for part in f:
            for ch in '/\\:*?"<>|':
                self.assertNotIn(ch, part, part)

    def test_filenames_name_the_face(self):
        self.assertEqual(H.handover_filename("MEST-PH-2026-00007", "up"),
                         "MEST-PH-2026-00007_top.jpg")
        self.assertEqual(H.handover_filename("MEST-PH-2026-00007", "front"),
                         "MEST-PH-2026-00007_front.jpg")


if __name__ == "__main__":
    unittest.main()
