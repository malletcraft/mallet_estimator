"""Check a room JSON before SketchUp ever sees it.

The Ruby builder runs inside SketchUp where a mistake is a silent wrong
model; these are the checks worth doing where they can be run cheaply and
repeatedly: is the outline a real simple polygon, does its perimeter agree
with what was measured on site, and does every opening fit on the wall it
claims to be on.
"""
import json
import math
import sys


def seg(a, b):
    return math.dist(a, b)


def cross(o, a, b):
    return (a[0]-o[0])*(b[1]-o[1]) - (a[1]-o[1])*(b[0]-o[0])


def segments_cross(p1, p2, p3, p4):
    d1, d2 = cross(p3, p4, p1), cross(p3, p4, p2)
    d3, d4 = cross(p1, p2, p3), cross(p1, p2, p4)
    return ((d1 > 0) != (d2 > 0)) and ((d3 > 0) != (d4 > 0))


def check(path):
    d = json.loads(open(path).read())
    poly = d["floor_polygon"]
    n = len(poly)
    problems = []

    # 1. simple polygon — a self-crossing outline makes SketchUp refuse a face
    for i in range(n):
        for j in range(i + 1, n):
            if abs(i - j) <= 1 or (i == 0 and j == n - 1):
                continue
            if segments_cross(poly[i], poly[(i+1) % n], poly[j], poly[(j+1) % n]):
                problems.append(f"outline crosses itself at edges {i} and {j}")

    # 2. perimeter vs what ImageMeter measured on site
    per = sum(seg(poly[i], poly[(i+1) % n]) for i in range(n))
    edges = [round(seg(poly[i], poly[(i+1) % n])) for i in range(n)]
    print(f"edges (mm): {edges}")
    print(f"perimeter:  {per:.0f} mm")
    measured = None
    for c in d.get("source", {}).get("cross_checks", []):
        if "floor perimeter" in c:
            measured = int(c.split()[2])
    if measured:
        err = per - measured
        print(f"measured:   {measured} mm   (difference {err:+.0f} mm, "
              f"{abs(err)/measured*100:.2f}%)")
        if abs(err) > 50:
            problems.append(f"perimeter is {err:+.0f} mm off the site measurement")

    # 3. shoelace area, as a sanity figure a person recognises
    area = abs(sum(poly[i][0]*poly[(i+1) % n][1] - poly[(i+1) % n][0]*poly[i][1]
                   for i in range(n))) / 2
    print(f"floor area: {area/1e6:.2f} m²  ({area/92903.04:.1f} sq ft)")

    # 4. every opening must fit on its own wall
    wall_edge = {w["id"]: w["edge"] for w in d.get("walls", [])}
    for op in d.get("openings", []):
        e = wall_edge.get(op["wall"])
        if e is None:
            problems.append(f"{op['kind']} names wall '{op['wall']}' which is not listed")
            continue
        length = seg(poly[e], poly[(e+1) % n])
        end = op["from_start"] + op["width"]
        fit = "ok" if end <= length else "DOES NOT FIT"
        print(f"{op['kind']:6s} on {op['wall']:5s}: {op['from_start']}..{end:.0f} "
              f"of {length:.0f} mm — {fit}")
        if end > length:
            problems.append(f"{op['kind']} on {op['wall']} runs {end-length:.0f} mm past the wall")
        if op["sill"] + op["height"] > d["ceiling_height"]:
            problems.append(f"{op['kind']} on {op['wall']} is taller than the ceiling")

    print()
    if problems:
        print("PROBLEMS:")
        for p in problems:
            print("  -", p)
        return 1
    print("room JSON is buildable.")
    return 0


if __name__ == "__main__":
    sys.exit(check(sys.argv[1] if len(sys.argv) > 1
                   else "tools/sketchup/rooms/YS_MB.json"))
