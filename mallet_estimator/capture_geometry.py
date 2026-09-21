"""Where to stand, and how wide each of the six faces has to be.

The bench's copy of android/pano/.../CaptureGeometry.kt. Two copies exist for
the reason the projection has two: a site has no signal, so the phone must be
able to work the answer out alone, and the bench must reach the same answer
for every capture uploaded as a raw panorama. test_panorama holds the two to
the same numbers.

Amit, 2026-09-21: "capture all 3 sides of a room in inches by the user and
decide correct FOV based on the room. that should tell where camera should be
in the room by all its axes x,y,z. you can automatically adjust FOV for all 6
faces so that all 4 corners of each face is available by just showing 10% of
the adjacent walls to distinguish the face being measured."

WHAT WAS WRONG BEFORE: the FOV was sized from the WALLS alone -- for each
wall, the angle to its edges and to its floor and ceiling LINES, at the
perpendicular distance to that wall. The floor and ceiling FACES were never in
the formula, and their corners sit at the room's half-DIAGONAL, always further
than any wall's perpendicular distance. One FOV was then applied to all six
faces, so the floor face inherited a number computed without it. A 10x12 ft
room got 105 degrees where its floor needed 117; a 20x18 got 101 where its
floor needed 141. Small rooms passed by accident, their wall requirement being
large enough to cover the floor too -- which is why the failure was
intermittent and read as "the FOV is a bit small" rather than a missing term.
"""

import math

# The border of adjacent wall each face carries beyond its own edges, as a
# fraction of that face's own dimension. A face cropped exactly at the corner
# gives the eye nothing to place it by, and a face that might be the left wall
# or the back wall is one nobody can measure from with confidence.
MARGIN_FRACTION = 0.10

MIN_ROOM_IN = 36.0
MAX_ROOM_IN = 1200.0
# Under this a room has no usable headroom; over it, somebody typed feet into
# a field that reads inches -- the likeliest input error, and it silently
# halves every FOV if it gets through.
MIN_CEILING_IN = 72.0
MAX_CEILING_IN = 240.0


def station_for(length_in, width_in, height_in):
    """Stand in the middle of the floor, at half the ceiling height.

    Centring horizontally equalises the two walls of each pair. Half height is
    the minimising choice in z: raising the camera shrinks what the floor face
    needs and grows what the ceiling needs by the same geometry, so any
    departure makes the worse of the two worse. It also minimises the vertical
    span each WALL face must carry.
    """
    return (length_in / 2.0, width_in / 2.0, height_in / 2.0)


def fov_by_face(length_in, width_in, height_in, station=None):
    """Per-face FOV in degrees, keyed by the names panorama.FACES uses.

    A gnomonic face of FOV f, aimed perpendicular at a plane D away, covers a
    SQUARE on that plane of half-size D*tan(f/2) centred on the foot of the
    perpendicular -- so the requirement is set by the furthest edge from that
    foot, per axis, and a square face takes the larger of the two.

    Floor and ceiling use the half-DIAGONAL rather than the longer side: the
    deliberate cost of not knowing which way the camera points. Amit chose it
    on 2026-09-21 -- the aligned case needs about 14 fewer degrees on a 10x12,
    and buying them back truncates the floor whenever he stands rotated, which
    is discovered back at the desk long after the room is inaccessible.
    """
    if not (length_in > 0 and width_in > 0 and height_in > 0):
        raise ValueError("room dims must be positive")
    cx, cy, cz = station or station_for(length_in, width_in, height_in)
    if not (0 < cz < height_in):
        raise ValueError("camera must be inside the room")

    m = MARGIN_FRACTION

    def wall(distance, span_a, off_a, span_b, off_b):
        a = off_a + m * span_a
        b = off_b + m * span_b
        return 2 * math.degrees(math.atan(max(a, b) / distance))

    vert_off = max(cz, height_in - cz)
    front_back = wall(
        min(cy, width_in - cy),
        length_in, max(cx, length_in - cx),
        height_in, vert_off)
    left_right = wall(
        min(cx, length_in - cx),
        width_in, max(cy, width_in - cy),
        height_in, vert_off)

    reach = math.hypot(
        max(cx, length_in - cx), max(cy, width_in - cy)) * (1.0 + m)

    return {
        "front": front_back, "back": front_back,
        "left": left_right, "right": left_right,
        "down": 2 * math.degrees(math.atan(reach / cz)),
        "up": 2 * math.degrees(math.atan(reach / (height_in - cz))),
    }


def plan_for_room(length_in, width_in, height_in):
    """The whole plan, or None when the numbers are not a room.

    None rather than a confident wrong answer is the point: a mistyped
    dimension produces a plausible FOV, and a plausible FOV is
    indistinguishable from a correct one until the photographs come back
    cropped.
    """
    try:
        l = float(length_in)
        w = float(width_in)
        h = float(height_in)
    except (TypeError, ValueError):
        return None
    if not (MIN_ROOM_IN <= l <= MAX_ROOM_IN):
        return None
    if not (MIN_ROOM_IN <= w <= MAX_ROOM_IN):
        return None
    if not (MIN_CEILING_IN <= h <= MAX_CEILING_IN):
        return None
    cx, cy, cz = station_for(l, w, h)
    return {
        "station": {"x_in": cx, "y_in": cy, "z_in": cz},
        "fov_by_face": fov_by_face(l, w, h),
        "length_in": l, "width_in": w, "height_in": h,
    }
