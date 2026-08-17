# Generates the projection contract: the numbers a SECOND implementation of
# the cube-face split must reproduce.
#
# The Android app cannot call panorama.py — it splits on the device, offline,
# because that is the only way ImageMeter gets a face on a site with no
# signal. So the same gnomonic projection will exist twice, in two languages,
# and two implementations of one formula drift silently: nobody notices a face
# that is two degrees off, they just measure the wrong wall six months later.
#
# The goldens are SAMPLED PIXELS, not a file hash. Bilinear interpolation and
# float rounding legitimately differ by a step or two between languages; a
# hash would fail on differences that do not matter and teach everyone to
# ignore it. A tolerance on sampled values fails only on real divergence.
#
# Run:  python3 make_projection_goldens.py   (writes projection_goldens.json)
import json
import os

import numpy as np

from mallet_estimator import panorama

PANO_W, PANO_H = 512, 256
FACE_PX = 64
FOV = 110.0
# Where to sample each face: corners pin the widest projection error, the
# centre pins the axis, the off-centre points catch a mirrored or rotated map.
SAMPLES = [(1, 1), (32, 1), (62, 1), (1, 32), (32, 32), (62, 32),
           (1, 62), (32, 62), (62, 62), (16, 48), (48, 16)]


def synthetic_pano(w, h):
    """A deterministic equirect with structure in BOTH axes.

    A smooth gradient would hide a transposed or mirrored projection, because
    every wrong answer still looks plausible. The bands and the checker make a
    misorientation visible as a number, not as a feeling about an image."""
    yy, xx = np.mgrid[0:h, 0:w]
    lon_band = (xx * 8 // w) * 31           # 8 vertical bands, distinct values
    lat_band = (yy * 4 // h) * 61           # 4 horizontal bands
    checker = ((xx // 16 + yy // 16) % 2) * 45
    r = (lon_band + checker) % 256
    g = (lat_band + checker) % 256
    b = (xx * 255 // max(w - 1, 1)) % 256   # a clean longitude ramp
    return np.dstack([r, g, b]).astype(np.uint8)


def main():
    pano = synthetic_pano(PANO_W, PANO_H)
    out = {
        "pano": {"width": PANO_W, "height": PANO_H, "recipe": "synthetic_pano"},
        "fov": FOV, "face_px": FACE_PX,
        "tolerance": 2,     # per channel, 0-255
        "faces": {},
    }
    for name, yaw, pitch in panorama.FACES:
        face = panorama.face_from_equirect(pano, yaw, pitch, FOV, FACE_PX)
        out["faces"][name] = {
            "yaw": yaw, "pitch": pitch,
            "samples": [
                {"x": x, "y": y, "rgb": [int(c) for c in face[y, x][:3]]}
                for x, y in SAMPLES
            ],
        }
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        "projection_goldens.json")
    with open(path, "w") as f:
        json.dump(out, f, indent=1)
        f.write("\n")
    print(f"wrote {path}: {len(out['faces'])} faces x {len(SAMPLES)} samples")


if __name__ == "__main__":
    main()
