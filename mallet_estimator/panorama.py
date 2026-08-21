# Equirectangular 360 → flat faces (gnomonic projection). Pure layer: numpy +
# Pillow only, no frappe — unit-testable without a bench (TESTING.md layer 1).
#
# A site 360 is the SOURCE OF TRUTH; the six faces are derived artifacts,
# regenerable at any FOV. The default 110° (not the cube-map 90°) is a
# deliberate annotation choice: faces sit 90° apart, so 110° gives ~20° of
# overlap on every edge — a defect near a wall corner shows on BOTH adjacent
# faces with context. The price is that the faces no longer reassemble into a
# seamless cube; an interactive viewer renders from the equirect instead.
import io
import math

import numpy as np
from PIL import Image

DEFAULT_FOV = 110.0
DEFAULT_FACE_PX = 1600

# Face name → (yaw°, pitch°). Yaw 0 = wherever the camera's front was; the
# names are camera-relative, not compass-relative, and that is fine because
# annotation happens per photo, not across photos.
FACES = (
    ("front", 0.0, 0.0),
    ("right", 90.0, 0.0),
    ("back", 180.0, 0.0),
    ("left", 270.0, 0.0),
    ("up", 0.0, 90.0),
    ("down", 0.0, -90.0),
)
FACE_NAMES = tuple(f[0] for f in FACES)

# A flat photograph — a phone snap of one wall, taken instead of a 360 — is
# its own single "face". It is NOT one of the six and must never be projected
# like one, but everything downstream of the split (annotating it, attaching a
# marked-up copy, filing it against a face) treats it exactly the same way.
# The Android side writes this same token (FaceWriter.PHOTO_FACE).
PHOTO_FACE = "photo"

# What a face token is allowed to be wherever an ANNOTATION is concerned.
# Kept apart from FACE_NAMES on purpose: the splitter must still see six.
ANNOTATABLE_FACES = FACE_NAMES + (PHOTO_FACE,)

# Guard rails: below 30° a face is a keyhole, above 160° the gnomonic plane
# stretches to uselessness (180° is mathematically infinite).
FOV_MIN, FOV_MAX = 30.0, 160.0
FACE_PX_MIN, FACE_PX_MAX = 256, 4096

# Panos wider than this are downscaled before sampling so one 100-megapixel
# upload can't blow a shared bench worker's memory. 12k × 6k stays plenty for
# 1600 px faces.
MAX_PANO_WIDTH = 12000


def clamp_params(fov=None, face_px=None):
    fov = float(fov or DEFAULT_FOV)
    face_px = int(face_px or DEFAULT_FACE_PX)
    return (min(max(fov, FOV_MIN), FOV_MAX),
            min(max(face_px, FACE_PX_MIN), FACE_PX_MAX))


def looks_equirect(width, height, tolerance=0.10):
    """A full equirectangular pano is exactly 2:1. Anything far off is a normal
    photo or a cropped pano — splitting it would produce convincing-looking
    garbage, which is worse than refusing."""
    if not width or not height:
        return False
    return abs(width / height - 2.0) <= 2.0 * tolerance


def face_from_equirect(pano, yaw_deg, pitch_deg, fov_deg, face_px):
    """Sample one gnomonic face out of an equirect uint8 array (H×W×3).
    Bilinear, longitude wraps (the 'back' face straddles the ±180° seam),
    latitude clamps at the poles."""
    h, w = pano.shape[:2]
    lam0 = math.radians(yaw_deg)
    phi0 = math.radians(pitch_deg)

    # Camera basis. `up` is the analytic ∂forward/∂pitch, which stays valid at
    # the poles where cross-with-world-up would degenerate.
    forward = np.array([math.cos(phi0) * math.sin(lam0),
                        math.sin(phi0),
                        math.cos(phi0) * math.cos(lam0)], dtype=np.float64)
    right = np.array([math.cos(lam0), 0.0, -math.sin(lam0)], dtype=np.float64)
    up = np.array([-math.sin(phi0) * math.sin(lam0),
                   math.cos(phi0),
                   -math.sin(phi0) * math.cos(lam0)], dtype=np.float64)

    t = math.tan(math.radians(fov_deg) / 2.0)
    a = np.linspace(-t, t, face_px, dtype=np.float64)      # left → right
    b = np.linspace(t, -t, face_px, dtype=np.float64)      # top → bottom
    A, B = np.meshgrid(a, b)
    d = (forward[None, None, :]
         + A[..., None] * right[None, None, :]
         + B[..., None] * up[None, None, :])

    lam = np.arctan2(d[..., 0], d[..., 2])
    phi = np.arctan2(d[..., 1], np.hypot(d[..., 0], d[..., 2]))

    # Angle → source pixel, sampling at pixel centres.
    x = (lam / (2.0 * np.pi) + 0.5) * w - 0.5
    y = (0.5 - phi / np.pi) * h - 0.5

    x0 = np.floor(x).astype(np.int64)
    y0 = np.floor(y).astype(np.int64)
    wx = (x - x0).astype(np.float32)[..., None]
    wy = (y - y0).astype(np.float32)[..., None]
    x0m, x1m = np.mod(x0, w), np.mod(x0 + 1, w)
    y0c = np.clip(y0, 0, h - 1)
    y1c = np.clip(y0 + 1, 0, h - 1)

    # Gather the four neighbours as uint8 and only widen the gathered pixels —
    # never the whole pano — to keep peak memory a few face-sized buffers.
    out = (pano[y0c, x0m].astype(np.float32) * (1 - wx) * (1 - wy)
           + pano[y0c, x1m].astype(np.float32) * wx * (1 - wy)
           + pano[y1c, x0m].astype(np.float32) * (1 - wx) * wy
           + pano[y1c, x1m].astype(np.float32) * wx * wy)
    return np.clip(out + 0.5, 0, 255).astype(np.uint8)


def split_equirect(image_bytes, fov=None, face_px=None):
    """The feature: 360 bytes in, {face_name: PIL.Image} out."""
    fov, face_px = clamp_params(fov, face_px)
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    if not looks_equirect(img.width, img.height):
        raise ValueError(
            f"not an equirectangular 360 photo: {img.width}×{img.height} "
            f"(expected width ≈ 2 × height)")
    if img.width > MAX_PANO_WIDTH:
        img = img.resize((MAX_PANO_WIDTH, MAX_PANO_WIDTH // 2), Image.LANCZOS)
    pano = np.asarray(img, dtype=np.uint8)
    return {
        name: Image.fromarray(face_from_equirect(pano, yaw, pitch, fov, face_px))
        for name, yaw, pitch in FACES
    }


def split_to_jpeg(image_bytes, fov=None, face_px=None, quality=90):
    """{face_name: JPEG bytes} — what the frappe layer attaches as Files.
    JPEG, not PNG: these are photographs, and six 1600² PNGs of a building
    site would cost megabytes each for nothing."""
    out = {}
    for name, img in split_equirect(image_bytes, fov=fov, face_px=face_px).items():
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=quality, optimize=True)
        out[name] = buf.getvalue()
    return out
