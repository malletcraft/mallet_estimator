# Pure unit tests for the 360 → faces projection — no database, no frappe.
#   python -m unittest mallet_estimator.tests.test_panorama
# The synthetic pano paints a known colour at each face direction, so a wrong
# camera basis (swapped axes, flipped pitch, broken seam wrap) turns into a
# wrong colour at a face centre — not a subtly skewed photo nobody notices.
import io
import unittest

import numpy as np
from PIL import Image

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

        pano = G.synthetic_pano(golden["pano"]["width"], golden["pano"]["height"])
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
