# Handing the six faces over to ImageMeter, and taking the annotated ones back.
#
# ImageMeter's two-way sync directory is "fully managed by ImageMeter" and its
# manual forbids writing into it by hand, so ERPNext never touches it. Instead
# the faces go to a SEPARATE handover folder laid out client → project → room →
# capture, which is imported on the phone; ImageMeter's own image-upload mode
# then drops the annotated JPEGs back into its own folder, where we read them.
#
# The identity problem this module solves: ImageMeter names its exports after
# the date ("image_from_27._Jun_2026-4.jpg"), so nothing in the returning file
# says which capture or which face it came from. We therefore BURN a caption
# into each face before handover. ImageMeter draws on top of it, so the caption
# survives annotation and comes home legible to a person and parseable by us.
import io
import re

from PIL import Image, ImageDraw, ImageFont

FONT_CANDIDATES = (
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
)
BAR_FRACTION = 0.052          # caption strip height, as a fraction of the face
BAR_RGB = (17, 24, 31)
TEXT_RGB = (255, 255, 255)
SEP = " · "

# What a returning file's caption must match for us to re-attach it without a
# human deciding. Anchored on the document name, which is unique site-wide.
# NOT \b on either side: our own handover filenames put an underscore right
# after the id ("MEST-PH-2026-00001_front.jpg") and \b does not fire between a
# digit and an underscore, so \b would refuse to parse the very names we write.
#
# TWO id shapes, for one reason. A capture made on a phone with no signal has
# no document name yet — the server assigns MEST-PH-… on sync, and by then the
# faces are long since in ImageMeter under whatever they were called at birth.
# So the device mints MCAP-<12 hex> the moment the shutter fires, the server
# adopts that id when the capture syncs, and a face annotated in a basement
# still finds its way back to the right wall. Fixed length and hex-only keeps
# the pattern tight enough that it cannot match an ordinary word.
CAPTION_ID_RE = re.compile(
    r"(?<![A-Za-z0-9])(MEST-PH-[0-9]{4}-[0-9]{5}|MCAP-[0-9a-f]{12})(?![0-9a-f])")
DEVICE_ID_RE = re.compile(r"^MCAP-[0-9a-f]{12}$")


def is_device_id(token):
    """True when an id was minted on a device rather than by the server."""
    return bool(token and DEVICE_ID_RE.match(str(token)))


CAPTION_FACE_RE = re.compile(
    r"(?<![A-Za-z])(front|right|back|left|up|down|top|bottom|ceiling|floor)(?![A-Za-z])",
    re.I)
# Top/Bottom rather than Up/Down or Ceiling/Floor because that is the naming
# already in use on this Drive: Yogesh_Sahasrabudhe-2/YS_GB/YS_GB_top.jpg and
# 42 siblings, hand-made before this existed. One vocabulary in one folder
# tree beats a tidier one that argues with the files already there.
FACE_LABELS = {"front": "Front", "right": "Right", "back": "Back",
               "left": "Left", "up": "Top", "down": "Bottom"}
# Older/spoken synonyms still read back, so nothing already named that way
# becomes unmatchable.
LABEL_TO_FACE = {v.lower(): k for k, v in FACE_LABELS.items()}
LABEL_TO_FACE.update({"ceiling": "up", "floor": "down"})


def _font(px):
    for path in FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, px)
        except Exception:
            continue
    return ImageFont.load_default()


def caption_text(photo_name, room, face, capture_date=None, stage=None):
    """The one line that makes a returning JPEG identifiable."""
    bits = [photo_name, room or "", FACE_LABELS.get(face, face)]
    tail = " ".join(x for x in (str(capture_date or ""), stage or "") if x).strip()
    if tail:
        bits.append(tail)
    return SEP.join(b for b in bits if b)


def parse_caption(text):
    """Read an OCR'd or filename-carried caption back into (photo, face).
    Returns (None, None) when it isn't ours — never a guess."""
    if not text:
        return None, None
    m = CAPTION_ID_RE.search(text)
    if not m:
        return None, None
    photo = m.group(1)
    face = None
    fm = CAPTION_FACE_RE.search(text[m.end():]) or CAPTION_FACE_RE.search(text)
    if fm:
        token = fm.group(1).lower()
        face = LABEL_TO_FACE.get(token, token)
    return photo, face


def caption_face(jpeg_bytes, text, quality=92):
    """Burn the caption into a strip along the bottom of the face.

    The strip is ADDED below the image rather than drawn over it: covering
    part of the photo could hide the very defect being annotated."""
    img = Image.open(io.BytesIO(jpeg_bytes)).convert("RGB")
    bar = max(18, int(round(img.height * BAR_FRACTION)))
    out = Image.new("RGB", (img.width, img.height + bar), BAR_RGB)
    out.paste(img, (0, 0))

    d = ImageDraw.Draw(out)
    size = max(10, int(bar * 0.58))
    font = _font(size)
    # Shrink until it fits the width — a truncated ID would defeat the purpose.
    pad = max(6, bar // 5)
    while size > 9 and d.textlength(text, font=font) > img.width - 2 * pad:
        size -= 1
        font = _font(size)
    tw = d.textlength(text, font=font)
    d.text(((img.width - tw) / 2, img.height + (bar - size) / 2 - size * 0.08),
           text, fill=TEXT_RGB, font=font)

    buf = io.BytesIO()
    out.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


def _safe(part):
    """Drive/Android-safe path component."""
    s = re.sub(r"[^\w.\- ]+", "_", str(part or "").strip())
    return re.sub(r"\s+", "_", s).strip("_") or "unknown"


def handover_folders(customer_name, project_title, room, capture_date, stage=None):
    """client → project → room → capture. Mirrors how ImageMeter's own folders
    already read on this Drive (client, then room), with a capture level so a
    revisit never overwrites the previous visit's faces."""
    leaf = _safe(capture_date)
    if stage:
        leaf = f"{leaf}_{_safe(stage)}"
    return [_safe(customer_name), _safe(project_title), _safe(room), leaf]


def handover_filename(photo_name, face):
    return f"{_safe(photo_name)}_{FACE_LABELS.get(face, face).lower()}.jpg"
