# Pure unit tests for the 360 → faces projection — no database, no frappe.
#   python -m unittest mallet_estimator.tests.test_panorama
# The synthetic pano paints a known colour at each face direction, so a wrong
# camera basis (swapped axes, flipped pitch, broken seam wrap) turns into a
# wrong colour at a face centre — not a subtly skewed photo nobody notices.
import io
import math
import unittest

import numpy as np
from PIL import Image

from mallet_estimator import capture_geometry as G
from mallet_estimator import panorama as P

W, H = 512, 256

RED = (255, 0, 0)        # front  (yaw 0)
GREEN = (0, 255, 0)      # right  (yaw 90)
BLUE = (0, 0, 255)       # back   (yaw 180)
YELLOW = (255, 255, 0)   # left   (yaw 270)
WHITE = (255, 255, 255)  # up     (pitch +90)
BLACK = (0, 0, 0)        # down   (pitch -90)


def _synthetic_pano():
    """Equator band split into four longitude quadrants, polar caps solid."""
    img = np.zeros((H, W, 3), dtype=np.uint8)
    lon = (np.arange(W) + 0.5) / W * 360.0 - 180.0   # -180 → 180
    band = np.zeros((W, 3), dtype=np.uint8)
    band[(np.abs(lon) <= 45)] = RED
    band[(lon > 45) & (lon <= 135)] = GREEN
    band[(np.abs(lon) > 135)] = BLUE
    band[(lon < -45) & (lon >= -135)] = YELLOW
    img[:, :] = band[None, :, :]
    img[: H // 4, :] = WHITE                          # latitude > 45° = up cap
    img[-H // 4:, :] = BLACK                          # latitude < -45° = down cap
    return img


def _pano_bytes():
    buf = io.BytesIO()
    Image.fromarray(_synthetic_pano()).save(buf, format="PNG")
    return buf.getvalue()


def _center(face_img):
    a = np.asarray(face_img)
    return tuple(a[a.shape[0] // 2, a.shape[1] // 2])


class TestProjection(unittest.TestCase):
    def test_each_face_centre_lands_on_its_colour(self):
        faces = P.split_equirect(_pano_bytes(), fov=110, face_px=256)
        expected = {"front": RED, "right": GREEN, "back": BLUE,
                    "left": YELLOW, "up": WHITE, "down": BLACK}
        self.assertEqual(set(faces), set(P.FACE_NAMES))
        for name, want in expected.items():
            got = _center(faces[name])
            for g, w in zip(got, want):
                self.assertLess(abs(g - w), 8, f"{name}: {got} != {want}")

    def test_wide_fov_sees_the_neighbours(self):
        # At 110° the front face's horizontal edges pass ±45°, so its left and
        # right borders must show the adjacent quadrants — the overlap that
        # makes corner annotations possible. At 60° they must not.
        wide = np.asarray(P.split_equirect(_pano_bytes(), fov=110, face_px=256)["front"])
        mid = wide.shape[0] // 2
        self.assertLess(abs(int(wide[mid, 2][1]) - 255), 8, "wide left edge should be YELLOW-adjacent")
        self.assertLess(abs(int(wide[mid, -3][1]) - 255), 8, "wide right edge should be GREEN")
        narrow = np.asarray(P.split_equirect(_pano_bytes(), fov=60, face_px=256)["front"])
        self.assertEqual(tuple(narrow[mid, 2]), RED)
        self.assertEqual(tuple(narrow[mid, -3]), RED)

    def test_back_face_is_seamless_across_the_date_line(self):
        # At 80° the whole back face sits inside the BLUE zone, which straddles
        # longitude ±180 — a projection error shows as a foreign colour.
        back = np.asarray(P.split_equirect(_pano_bytes(), fov=80, face_px=256)["back"])
        mid = back.shape[0] // 2
        for col in (0, back.shape[1] // 2, back.shape[1] - 1):
            self.assertEqual(tuple(back[mid, col]), BLUE, f"col {col} broke the seam")

    def test_longitude_wraps_instead_of_clamping(self):
        # Sampling exactly AT ±180° must bilinearly blend the pano's LAST and
        # FIRST columns — a clamp would double the last column and the two
        # BLUE quadrant halves could never join seamlessly.
        pano = np.zeros((2, 4, 3), dtype=np.uint8)
        pano[:, 0] = (200, 0, 0)
        pano[:, 3] = (0, 0, 100)
        face = P.face_from_equirect(pano, 180.0, 0.0, 1.0, 8)
        got = face[4, 4]
        self.assertEqual((got[0], got[2]), (100, 50), f"wrap blend wrong: {tuple(got)}")

    def test_face_size_and_format(self):
        faces = P.split_to_jpeg(_pano_bytes(), fov=110, face_px=256)
        for name, data in faces.items():
            img = Image.open(io.BytesIO(data))
            self.assertEqual(img.format, "JPEG")
            self.assertEqual(img.size, (256, 256), name)

    def test_params_are_clamped(self):
        self.assertEqual(P.clamp_params(None, None), (P.DEFAULT_FOV, P.DEFAULT_FACE_PX))
        self.assertEqual(P.clamp_params(500, 999999), (P.FOV_MAX, P.FACE_PX_MAX))
        self.assertEqual(P.clamp_params(1, 1), (P.FOV_MIN, P.FACE_PX_MIN))

    def test_a_normal_photo_is_refused(self):
        # Splitting a non-equirect produces convincing-looking garbage, which
        # is worse than refusing.
        buf = io.BytesIO()
        Image.new("RGB", (640, 480), RED).save(buf, format="PNG")
        with self.assertRaises(ValueError):
            P.split_equirect(buf.getvalue())
        self.assertTrue(P.looks_equirect(5376, 2688))    # Theta Z1
        self.assertFalse(P.looks_equirect(640, 480))


class TestProjectionContract(unittest.TestCase):
    """The numbers a SECOND implementation must reproduce.

    The Android app has to split on the device — that is the only way
    ImageMeter gets a face on a site with no signal — so this projection will
    exist twice, in two languages. Two implementations of one formula drift
    silently: nobody sees a face that is two degrees off, they measure the
    wrong wall months later. The goldens are the contract between them, and
    this test holds THIS side to it, so changing the projection has to be a
    deliberate act of regenerating the file, not an accident."""

    def test_the_published_goldens_still_describe_this_projection(self):
        import json
        import os

        from mallet_estimator.tests.golden import make_projection_goldens as G

        path = os.path.join(os.path.dirname(os.path.abspath(G.__file__)),
                            "projection_goldens.json")
        with open(path) as f:
            golden = json.load(f)

        # Load the SHIPPED pano, not a freshly generated one. Both this test
        # and the Kotlin one read the same PNG, so a drift in the fixture
        # cannot masquerade as agreement — or as a projection bug.
        png = os.path.join(os.path.dirname(os.path.abspath(G.__file__)),
                           golden["pano"]["file"])
        pano = np.asarray(Image.open(png).convert("RGB"))
        self.assertEqual(pano.shape[1], golden["pano"]["width"])
        self.assertEqual(pano.shape[0], golden["pano"]["height"])
        tol = golden["tolerance"]
        for name, spec in golden["faces"].items():
            face = P.face_from_equirect(pano, spec["yaw"], spec["pitch"],
                                        golden["fov"], golden["face_px"])
            for s in spec["samples"]:
                got = [int(c) for c in face[s["y"], s["x"]][:3]]
                for ch, (a, b) in enumerate(zip(got, s["rgb"])):
                    self.assertLessEqual(
                        abs(a - b), tol,
                        f"{name} ({s['x']},{s['y']}) channel {ch}: {got} vs "
                        f"golden {s['rgb']} — regenerate the goldens ON "
                        f"PURPOSE if the projection really changed")


if __name__ == "__main__":
    unittest.main()


class TestCaptureGeometry(unittest.TestCase):
    """The room → per-face FOV maths, and its agreement with the phone.

    Two copies of this exist -- Python here, Kotlin in android/pano -- because
    a site has no signal and the phone must reach the answer alone. Numbers
    are PINNED rather than recomputed by the same formula twice, which is the
    only way a pair of implementations can be shown to agree.
    CaptureGeometryTest asserts the same values on the other side.
    """

    def _plan(self, l_ft, w_ft, h_ft=9.5):
        return G.plan_for_room(l_ft * 12, w_ft * 12, h_ft * 12)

    def test_a_10x12_room_matches_the_phones_numbers_exactly(self):
        p = self._plan(10, 12)
        self.assertEqual(p["station"], {"x_in": 60.0, "y_in": 72.0, "z_in": 57.0})
        for face, want in {"front": 90.0, "back": 90.0, "left": 110.4,
                           "right": 110.4, "down": 122.1, "up": 122.1}.items():
            self.assertAlmostEqual(p["fov_by_face"][face], want, delta=0.05,
                                   msg=f"{face} disagrees with the phone")

    def test_the_floor_needs_more_than_any_wall(self):
        # THE DEFECT THIS REPLACED, in one assertion. The old maths sized every
        # face from the walls, so the floor inherited a number computed without
        # it -- and the floor's corners are at the half-diagonal, always
        # further than any wall's perpendicular distance.
        for l, w in ((10, 12), (12, 14), (20, 18)):
            f = self._plan(l, w)["fov_by_face"]
            widest_wall = max(f[k] for k in ("front", "back", "left", "right"))
            self.assertGreater(f["down"], widest_wall, f"{l}x{w}")

    def test_a_small_room_is_the_accidental_pass_that_hid_it(self):
        f = self._plan(5, 7)["fov_by_face"]
        widest_wall = max(f[k] for k in ("front", "back", "left", "right"))
        self.assertGreater(widest_wall, f["down"])

    def test_the_floor_face_reaches_the_corner_at_any_yaw(self):
        l, w, h = 120.0, 144.0, 114.0
        f = G.fov_by_face(l, w, h)["down"]
        half_side = (h / 2) * math.tan(math.radians(f / 2))
        self.assertGreaterEqual(half_side, math.hypot(l / 2, w / 2))

    def test_feet_typed_into_an_inches_field_are_refused(self):
        # The likeliest input error, and the one that must not get through:
        # it silently halves every FOV and the photographs look plausible.
        self.assertIsNone(G.plan_for_room(10, 12, 9.5))

    def test_junk_is_none_not_a_guess(self):
        for bad in ((None, 120, 114), (120, None, 114), (120, 120, None),
                    ("x", 120, 114), (24, 120, 114), (120, 120, 60)):
            self.assertIsNone(G.plan_for_room(*bad), bad)

    def test_a_partial_map_falls_back_for_the_faces_it_omits(self):
        # Every capture taken before the dimensions existed splits exactly as
        # it used to, which is what makes this safe to ship without backfill.
        faces = P.split_equirect(_pano_bytes(), fov=110, face_px=64,
                                 fov_by_face={"down": 140.0})
        self.assertEqual(len(faces), 6)

    def test_a_per_face_split_really_differs_from_a_flat_one(self):
        # Guards the wiring, not the maths: a fov_by_face that is accepted and
        # then ignored would leave every test above passing and the floor
        # still cropped. That is the exact shape of the last FOV bug.
        flat = np.asarray(P.split_equirect(
            _pano_bytes(), fov=110, face_px=64)["down"])
        wide = np.asarray(P.split_equirect(
            _pano_bytes(), fov=110, face_px=64, fov_by_face={"down": 150.0})["down"])
        self.assertFalse(np.array_equal(flat, wide),
                         "fov_by_face was accepted and ignored")
