# The Site Photo PWA's server contract (execution/DESIGN.md §7).
#
# Everything here runs as the LOGGED-IN user over their desk session — no API
# key ever reaches a phone browser — so every call goes through normal
# permissions (frappe.get_list, doc.insert, check_permission) and never
# ignore_permissions. The PWA is a client, not an identity.
#
# Binary uploads deliberately do NOT pass through here: the phone posts them
# to frappe's native /api/method/upload_file (multipart), which streams
# instead of inflating a 20 MB pano into ~27 MB of base64 JSON. These methods
# create the record, bind the uploaded file, and read back.
import frappe
from frappe.utils import cint, today

from mallet_estimator import panorama

DOCTYPE = "Site Photo 360"
STAGES = ("Baseline", "Civil", "Wiring", "Carpentry", "Finishing", "Handover")

# Field list the timeline/detail views read. Kept in one place so the PWA and
# the tests agree on the shape.
LIST_FIELDS = ["name", "project", "room", "capture_date", "stage", "status",
               "fov", "face_px", "split_error", "pano"] + \
    [f"face_{f}" for f in panorama.FACE_NAMES]


@frappe.whitelist()
def bootstrap():
    """Everything the pickers need. A capture can only ever land on a REAL
    project and a REAL room because both lists come from the masters — the
    room list is the same Estimate Room master the SKU codes use, so a photo
    files itself beside YS_MB_WAR rather than beside a free-text 'master
    bedrm' someone typed on site."""
    projects = []
    for p in frappe.get_list(
            "Project", filters={"status": ("!=", "Cancelled")},
            fields=["name", "project_name", "customer"],
            order_by="modified desc", limit_page_length=200):
        projects.append({
            "project": p.name,
            "title": p.project_name or p.name,
            "customer": p.customer or "",
            "customer_name": (frappe.db.get_value("Customer", p.customer, "customer_name")
                              if p.customer else "") or "",
        })
    rooms = [r.name for r in frappe.get_list(
        "Estimate Room", fields=["name"], order_by="name", limit_page_length=200)]
    return {
        "projects": projects,
        "rooms": rooms,
        "stages": list(STAGES),
        "faces": list(panorama.FACE_NAMES),
        "default_fov": int(panorama.DEFAULT_FOV),
        "fov_min": int(panorama.FOV_MIN),
        "fov_max": int(panorama.FOV_MAX),
        "user": frappe.session.user,
        "can_create": bool(frappe.has_permission(DOCTYPE, "create")),
    }


@frappe.whitelist()
def create_capture(project, room, capture_date=None, stage=None, fov=None):
    """Step 1 of a capture: the record. The phone then uploads the pano
    against this docname and calls bind_pano()."""
    doc = frappe.get_doc({
        "doctype": DOCTYPE,
        "project": project,
        "room": room,
        "capture_date": capture_date or today(),
        "stage": stage or "",
        "fov": cint(fov) or int(panorama.DEFAULT_FOV),
    })
    doc.insert()
    return {"name": doc.name, "status": doc.status}


@frappe.whitelist()
def bind_pano(name, file_url):
    """Step 3: point the record at the uploaded 360. This save is what queues
    the split — the same path the desk form uses, not a parallel one."""
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("write")
    doc.pano = file_url
    doc.save()
    return {"name": doc.name, "status": doc.status}


@frappe.whitelist()
def timeline(project, room=None, limit=100):
    """A room's progress IS this list — one row per capture, newest first."""
    filters = {"project": project}
    if room:
        filters["room"] = room
    return frappe.get_list(
        DOCTYPE, filters=filters, fields=LIST_FIELDS,
        order_by="capture_date desc, creation desc",
        limit_page_length=cint(limit) or 100)


@frappe.whitelist()
def detail(name):
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("read")
    out = {f: doc.get(f) for f in LIST_FIELDS if f != "name"}
    out["name"] = doc.name
    out["annotations"] = [
        {"idx": a.idx, "face": a.face, "image": a.image, "note": a.note,
         "owner": a.owner, "creation": str(a.creation)}
        for a in (doc.annotations or [])
    ]
    return out


@frappe.whitelist()
def annotate(photo, face, file_url, note=None):
    """Attach a marked-up COPY. The generated faces stay pristine — annotation
    is a layer, so a re-split at a different FOV never destroys someone's
    markup, and the same face can carry several people's notes."""
    if face not in panorama.FACE_NAMES:
        frappe.throw(f"unknown face '{face}' — expected one of {', '.join(panorama.FACE_NAMES)}")
    doc = frappe.get_doc(DOCTYPE, photo)
    doc.check_permission("write")
    doc.append("annotations", {"face": face, "image": file_url, "note": note})
    doc.save()
    return {"count": len(doc.annotations)}


@frappe.whitelist()
def delete_annotation(photo, idx):
    doc = frappe.get_doc(DOCTYPE, photo)
    doc.check_permission("write")
    doc.set("annotations", [a for a in (doc.annotations or []) if a.idx != cint(idx)])
    doc.save()
    return {"count": len(doc.annotations)}


@frappe.whitelist()
def tree():
    """Client → project → room, the way ImageMeter's own folders read.

    Built from the captures that exist rather than from the masters: a room
    nobody has photographed is not a folder, it is noise. Counts come from one
    grouped query so the browser stays fast as the history grows."""
    rows = frappe.get_list(
        DOCTYPE, fields=["project", "room", "capture_date", "name", "status"],
        order_by="capture_date desc, creation desc", limit_page_length=5000)
    if not rows:
        return {"clients": []}

    projects = {}
    for p in frappe.get_all(
            "Project", filters={"name": ("in", list({r.project for r in rows if r.project}))},
            fields=["name", "project_name", "customer"]):
        projects[p.name] = p
    cust_names = {}
    for c in frappe.get_all(
            "Customer",
            filters={"name": ("in", list({p.customer for p in projects.values() if p.customer}))},
            fields=["name", "customer_name"]):
        cust_names[c.name] = c.customer_name or c.name

    tree_ = {}
    for r in rows:
        p = projects.get(r.project)
        client = cust_names.get(p.customer if p else None) or "(no client)"
        ptitle = (p.project_name if p else None) or r.project or "(no project)"
        c = tree_.setdefault(client, {})
        pr = c.setdefault((r.project, ptitle), {})
        room = pr.setdefault(r.room or "(no room)", {"captures": 0, "latest": None})
        room["captures"] += 1
        if not room["latest"] or str(r.capture_date or "") > room["latest"]:
            room["latest"] = str(r.capture_date or "")

    out = []
    for client in sorted(tree_):
        plist = []
        for (pname, ptitle) in sorted(tree_[client], key=lambda x: x[1]):
            rooms = tree_[client][(pname, ptitle)]
            plist.append({
                "project": pname, "title": ptitle,
                "captures": sum(v["captures"] for v in rooms.values()),
                "rooms": [{"room": k, **v} for k, v in
                          sorted(rooms.items(), key=lambda kv: kv[0])],
            })
        out.append({"client": client,
                    "captures": sum(p["captures"] for p in plist),
                    "projects": plist})
    return {"clients": out}


@frappe.whitelist()
def room_captures(project, room, limit=60):
    """Every capture for one room, newest first, with its faces — the right
    pane of the browser and the progress record for that wall."""
    rows = frappe.get_list(
        DOCTYPE, filters={"project": project, "room": room}, fields=LIST_FIELDS,
        order_by="capture_date desc, creation desc", limit_page_length=cint(limit) or 60)
    names = [r["name"] for r in rows]
    counts = {}
    if names:
        for a in frappe.get_all("Site Photo Annotation",
                                filters={"parent": ("in", names)},
                                fields=["parent", "count(name) as n"], group_by="parent"):
            counts[a["parent"]] = a["n"]
    for r in rows:
        r["annotations"] = counts.get(r["name"], 0)
    return rows
