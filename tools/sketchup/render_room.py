"""Build the room from its JSON and draw it — no SketchUp required.

Two outputs from one source of truth:
  <room>.dae  a COLLADA mesh SketchUp imports directly (File > Import),
              which is one click instead of the Ruby Console.
  <room>.png  a picture, so the shape can be judged before anyone opens a
              CAD program at all. A room shell that is wrong is obvious in
              a picture and invisible in a list of numbers.

Walls with openings are cut by subdividing each wall at every opening edge
and dropping the cells that fall inside one — always correct, and it gives
real geometry rather than a texture with a door painted on it.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw


def load(path):
    return json.loads(Path(path).read_text())


def wall_cells(length, height, openings):
    """Rectangles making up a wall once its openings are removed."""
    xs = {0.0, length}
    zs = {0.0, height}
    for o in openings:
        xs.update((o["x0"], o["x1"]))
        zs.update((o["z0"], o["z1"]))
    xs = sorted(v for v in xs if -1e-6 <= v <= length + 1e-6)
    zs = sorted(v for v in zs if -1e-6 <= v <= height + 1e-6)
    cells = []
    for i in range(len(xs) - 1):
        for j in range(len(zs) - 1):
            x0, x1, z0, z1 = xs[i], xs[i + 1], zs[j], zs[j + 1]
            if x1 - x0 < 1e-6 or z1 - z0 < 1e-6:
                continue
            cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
            inside = any(o["x0"] < cx < o["x1"] and o["z0"] < cz < o["z1"]
                         for o in openings)
            if not inside:
                cells.append((x0, x1, z0, z1))
    return cells


def build(room):
    """Faces as (points, kind). Points are 3D mm in room coordinates."""
    poly = room["floor_polygon"]
    n = len(poly)
    h = float(room["ceiling_height"])
    faces = []

    faces.append(([(x, y, 0.0) for x, y in poly], "floor"))

    edge_of = {w["id"]: w["edge"] for w in room.get("walls", [])}
    by_wall = {}
    for op in room.get("openings", []):
        by_wall.setdefault(op["wall"], []).append({
            "x0": float(op["from_start"]),
            "x1": float(op["from_start"]) + float(op["width"]),
            "z0": float(op["sill"]),
            "z1": float(op["sill"]) + float(op["height"]),
            "kind": op["kind"],
        })

    for wid, e in edge_of.items():
        a = poly[e]
        b = poly[(e + 1) % n]
        dx, dy = b[0] - a[0], b[1] - a[1]
        length = math.hypot(dx, dy)
        if length < 1:
            continue
        ux, uy = dx / length, dy / length
        for x0, x1, z0, z1 in wall_cells(length, h, by_wall.get(wid, [])):
            p = [(a[0] + ux * x0, a[1] + uy * x0, z0),
                 (a[0] + ux * x1, a[1] + uy * x1, z0),
                 (a[0] + ux * x1, a[1] + uy * x1, z1),
                 (a[0] + ux * x0, a[1] + uy * x0, z1)]
            faces.append((p, "wall"))

    for f in room.get("features", []):
        if f.get("kind") != "beam":
            continue
        wid = (f.get("at_corner") or [None])[0]
        e = edge_of.get(wid)
        if e is None:
            continue
        a, b = poly[e], poly[(e + 1) % n]
        dx, dy = b[0] - a[0], b[1] - a[1]
        L = math.hypot(dx, dy)
        if L < 1:
            continue
        ux, uy = dx / L, dy / L
        nx, ny = -uy, ux                      # inward
        d = float(f.get("width") or 150)
        drop = float(f["drop"])
        z0, z1 = h - drop, h
        c = [(a[0], a[1]), (b[0], b[1]),
             (b[0] + nx * d, b[1] + ny * d), (a[0] + nx * d, a[1] + ny * d)]
        faces.append(([(x, y, z0) for x, y in c], "beam"))
        for i in range(4):
            p, q = c[i], c[(i + 1) % 4]
            faces.append(([(p[0], p[1], z0), (q[0], q[1], z0),
                           (q[0], q[1], z1), (p[0], p[1], z1)], "beam"))
    return faces


def render(faces, out, size=(1500, 1050), az=-52, el=38, title="",
           cutaway=True):
    pts = [p for f, _ in faces for p in f]
    cx = sum(p[0] for p in pts) / len(pts)
    cy = sum(p[1] for p in pts) / len(pts)
    cz = sum(p[2] for p in pts) / len(pts)
    span = max(max(p[0] for p in pts) - min(p[0] for p in pts),
               max(p[1] for p in pts) - min(p[1] for p in pts))

    ar, er = math.radians(az), math.radians(el)
    eye = (cx + span * 1.9 * math.cos(er) * math.cos(ar),
           cy + span * 1.9 * math.cos(er) * math.sin(ar),
           cz + span * 1.9 * math.sin(er))
    fwd = (cx - eye[0], cy - eye[1], cz - eye[2])
    fl = math.dist((0, 0, 0), fwd)
    fwd = tuple(v / fl for v in fwd)
    right = (fwd[1], -fwd[0], 0)
    rl = math.hypot(right[0], right[1]) or 1
    right = (right[0] / rl, right[1] / rl, 0)
    up = (right[1] * fwd[2] - right[2] * fwd[1],
          right[2] * fwd[0] - right[0] * fwd[2],
          right[0] * fwd[1] - right[1] * fwd[0])

    W, H = size
    f = W * 0.9

    def project(p):
        d = (p[0] - eye[0], p[1] - eye[1], p[2] - eye[2])
        z = sum(d[i] * fwd[i] for i in range(3))
        if z <= 1:
            return None, 1e9
        x = sum(d[i] * right[i] for i in range(3))
        y = sum(d[i] * up[i] for i in range(3))
        return (W / 2 + f * x / z, H / 2 - f * y / z), z

    img = Image.new("RGB", size, (250, 250, 248))
    d = ImageDraw.Draw(img)

    drawn = []
    for poly, kind in faces:
        if cutaway and kind == "wall":
            fx = sum(q[0] for q in poly) / len(poly)
            fy = sum(q[1] for q in poly) / len(poly)
            inward = (cx - fx, cy - fy)
            tocam = (eye[0] - fx, eye[1] - fy)
            if inward[0] * tocam[0] + inward[1] * tocam[1] < 0:
                continue      # camera is outside this wall; it would block the view
        proj = [project(p) for p in poly]
        if any(pt is None for pt, _ in proj):
            continue
        depth = sum(z for _, z in proj) / len(proj)
        drawn.append((depth, [pt for pt, _ in proj], poly, kind))
    drawn.sort(key=lambda t: -t[0])           # far to near

    base = {"floor": (214, 205, 190), "wall": (243, 241, 236),
            "beam": (196, 168, 120), "opening": (120, 170, 210)}
    for depth, pts2, poly3, kind in drawn:
        # flat shading from the face normal, so walls separate visually
        ax = (poly3[1][0] - poly3[0][0], poly3[1][1] - poly3[0][1], poly3[1][2] - poly3[0][2])
        bx = (poly3[2][0] - poly3[0][0], poly3[2][1] - poly3[0][1], poly3[2][2] - poly3[0][2])
        nrm = (ax[1]*bx[2]-ax[2]*bx[1], ax[2]*bx[0]-ax[0]*bx[2], ax[0]*bx[1]-ax[1]*bx[0])
        nl = math.dist((0, 0, 0), nrm) or 1
        nrm = tuple(v / nl for v in nrm)
        lit = abs(nrm[0]*0.4 + nrm[1]*0.35 + nrm[2]*0.85)
        shade = 0.62 + 0.38 * lit
        col = tuple(min(255, int(c * shade)) for c in base.get(kind, (200, 200, 200)))
        d.polygon(pts2, fill=col, outline=(90, 90, 90))

    if title:
        d.rectangle([0, H - 34, W, H], fill=(28, 32, 38))
        d.text((14, H - 24), title, fill=(255, 255, 255))
    img.save(out, quality=90)
    return out


def to_dae(faces, out):
    tris, verts = [], []
    for poly, _ in faces:
        if len(poly) < 3:
            continue
        base = len(verts)
        verts.extend(poly)
        for i in range(1, len(poly) - 1):
            tris.extend([base, base + i, base + i + 1])
    pos = " ".join(f"{v[0]/1000:.5f} {v[1]/1000:.5f} {v[2]/1000:.5f}" for v in verts)
    idx = " ".join(str(i) for i in tris)
    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<COLLADA xmlns="http://www.collada.org/2005/11/COLLADASchema" version="1.4.1">
 <asset><unit name="meter" meter="1"/><up_axis>Z_UP</up_axis></asset>
 <library_geometries><geometry id="room" name="room"><mesh>
  <source id="room-pos"><float_array id="room-pos-a" count="{len(verts)*3}">{pos}</float_array>
   <technique_common><accessor source="#room-pos-a" count="{len(verts)}" stride="3">
    <param name="X" type="float"/><param name="Y" type="float"/><param name="Z" type="float"/>
   </accessor></technique_common></source>
  <vertices id="room-vtx"><input semantic="POSITION" source="#room-pos"/></vertices>
  <triangles count="{len(tris)//3}"><input semantic="VERTEX" source="#room-vtx" offset="0"/>
   <p>{idx}</p></triangles>
 </mesh></geometry></library_geometries>
 <library_visual_scenes><visual_scene id="s" name="s"><node id="room-n" name="room">
  <instance_geometry url="#room"/></node></visual_scene></library_visual_scenes>
 <scene><instance_visual_scene url="#s"/></scene>
</COLLADA>
"""
    Path(out).write_text(xml)
    return out, len(verts), len(tris) // 3


if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else "tools/sketchup/rooms/YS_MB.json"
    room = load(src)
    faces = build(room)
    stem = Path(src).with_suffix("")
    png = render(faces, f"{stem}.png",
                 title=f"{room['project']} — {room['room']}  "
                       f"({room['ceiling_height']} mm ceiling)")
    dae, nv, nt = to_dae(faces, f"{stem}.dae")
    print(f"faces: {len(faces)}   vertices: {nv}   triangles: {nt}")
    print("wrote", png)
    print("wrote", dae)


def plan(room, out, size=(1100, 1050)):
    """Top-down plan with the numbers on it — the fastest way for the person
    who stood in the room to say 'that is not my room'."""
    poly = room["floor_polygon"]
    n = len(poly)
    W, H = size
    pad = 110
    xs = [p[0] for p in poly]
    ys = [p[1] for p in poly]
    sx = (W - 2 * pad) / (max(xs) - min(xs))
    sy = (H - 2 * pad) / (max(ys) - min(ys))
    s = min(sx, sy)

    def P(x, y):
        return (pad + (x - min(xs)) * s, H - pad - (y - min(ys)) * s)

    img = Image.new("RGB", size, (252, 252, 250))
    d = ImageDraw.Draw(img)
    d.polygon([P(*p) for p in poly], fill=(233, 228, 216), outline=(40, 40, 40))

    edge_of = {w["id"]: w["edge"] for w in room.get("walls", [])}
    name_of = {v: k for k, v in edge_of.items()}

    for i in range(n):
        a, b = poly[i], poly[(i + 1) % n]
        length = math.dist(a, b)
        mx, my = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        px, py = P(mx, my)
        label = f"{length:.0f}"
        if i in name_of:
            label = f"{name_of[i]}  {length:.0f}"
        d.line([P(*a), P(*b)], fill=(30, 30, 30), width=4)
        d.text((px - 26, py - 8), label, fill=(20, 20, 20))

    # Openings, drawn where they actually sit along their wall.
    for op in room.get("openings", []):
        e = edge_of.get(op["wall"])
        if e is None:
            continue
        a, b = poly[e], poly[(e + 1) % n]
        L = math.dist(a, b)
        ux, uy = (b[0] - a[0]) / L, (b[1] - a[1]) / L
        x0 = float(op["from_start"])
        x1 = x0 + float(op["width"])
        p0 = P(a[0] + ux * x0, a[1] + uy * x0)
        p1 = P(a[0] + ux * x1, a[1] + uy * x1)
        d.line([p0, p1], fill=(0, 150, 90), width=11)
        d.text(((p0[0] + p1[0]) / 2 - 30, (p0[1] + p1[1]) / 2 + 10),
               f"{op['kind']} {op['width']}", fill=(0, 110, 65))

    for f in room.get("features", []):
        if f.get("kind") != "beam":
            continue
        e = edge_of.get((f.get("at_corner") or [None])[0])
        if e is None:
            continue
        a, b = poly[e], poly[(e + 1) % n]
        d.line([P(*a), P(*b)], fill=(200, 140, 40), width=7)
        mx, my = P((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        d.text((mx - 40, my + 14), f"beam drop {f['drop']}", fill=(150, 100, 20))

    d.rectangle([0, H - 34, W, H], fill=(28, 32, 38))
    d.text((14, H - 24),
           f"PLAN (mm) — {room['room']}   ceiling {room['ceiling_height']}",
           fill=(255, 255, 255))
    img.save(out, quality=92)
    return out
