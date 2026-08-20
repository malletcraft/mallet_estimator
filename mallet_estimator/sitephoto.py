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
import json
import re

import frappe
from frappe import _
from frappe.utils import cint, today

from mallet_estimator import handover, panorama

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
def create_capture(project, room, capture_date=None, stage=None, fov=None,
                   device_capture_id=None, app_version=None):
    """Step 1 of a capture: the record. The phone then uploads the pano
    against this docname and calls bind_pano().

    device_capture_id is the id a phone minted at the shutter, before it had
    any way to reach the server. Passing it again returns the SAME capture
    rather than making a second one — an offline queue retries, and a network
    that drops an acknowledgement after the insert succeeded would otherwise
    file the same room twice with nobody able to tell which is real."""
    device_capture_id = (device_capture_id or "").strip() or None
    if device_capture_id:
        if not handover.is_device_id(device_capture_id):
            frappe.throw(_("Not a device capture id: {0}").format(device_capture_id))
        existing = frappe.db.get_value(
            DOCTYPE, {"device_capture_id": device_capture_id}, ["name", "status"],
            as_dict=True)
        if existing:
            return {"name": existing.name, "status": existing.status,
                    "already_synced": True}

    doc = frappe.get_doc({
        "doctype": DOCTYPE,
        "project": project,
        "room": room,
        "capture_date": capture_date or today(),
        "stage": stage or "",
        "fov": cint(fov) or int(panorama.DEFAULT_FOV),
        "device_capture_id": device_capture_id,
    })
    # The phone reports its versionName with every capture — the server-side
    # answer to "which build is that phone actually running". Guarded: a
    # bench that has not migrated yet simply drops it.
    if app_version and frappe.get_meta(DOCTYPE).has_field("device_app_version"):
        doc.device_app_version = str(app_version)[:40]
    doc.insert()
    return {"name": doc.name, "status": doc.status}


def _site_key(text):
    """Case-, space- and underscore-insensitive identity for matching a name
    typed on a site against a master typed in an office. 'Yogesh_Sahasrabudhe'
    and 'yogesh sahasrabudhe' are the same person; treating them as two is how
    a customer ends up with half their photos under each spelling."""
    return re.sub(r"[\s_]+", " ", (text or "").strip()).casefold()


@frappe.whitelist()
def ensure_site(customer_name, project_title):
    """Resolve — or create — the client and project a device capture named.

    A technician arriving at a NEW site has no signal and no project row, so
    the app lets them type the client and project offline; this is the sync
    step that turns those words into masters. Matching comes first, always:
    the typed name is compared insensitively against every existing project
    and customer, because the failure mode that matters is not a missing row
    but a DUPLICATE one — photos split across 'Yogesh_Sahasrabudhe' and
    'yogesh sahasrabudhe' are worse than either alone.

    Runs with ignore_permissions on the inserts, deliberately: the
    photographer role stays camera-only (no create on Customer/Project), and
    THIS ENDPOINT is the one gate through which a phone may mint a site —
    gated on the same capture permission the rest of the app needs. What it
    creates carries no money: a Customer and a Project are names, not rates."""
    frappe.has_permission(DOCTYPE, "create", throw=True)

    customer_name = (customer_name or "").strip()
    project_title = (project_title or "").strip()
    if not customer_name or not project_title:
        frappe.throw(_("Both a client name and a project name are needed."))

    # An existing project wins outright, whatever customer was typed — the
    # office's record beats the site's memory of it.
    pkey = _site_key(project_title)
    for p in frappe.get_all("Project", fields=["name", "project_name", "customer"],
                            limit_page_length=0):
        if _site_key(p.project_name) == pkey:
            cust = (frappe.db.get_value("Customer", p.customer, "customer_name")
                    if p.customer else "") or ""
            return {"project": p.name, "project_title": p.project_name,
                    "customer_name": cust, "created": False}

    ckey = _site_key(customer_name)
    customer = None
    for c in frappe.get_all("Customer", fields=["name", "customer_name"],
                            limit_page_length=0):
        if _site_key(c.customer_name) == ckey:
            customer = c.name
            break
    if not customer:
        doc = frappe.get_doc({
            "doctype": "Customer", "customer_name": customer_name,
            "customer_group": _leaf_default("Selling Settings", "customer_group",
                                            "Customer Group"),
            "territory": _leaf_default("Selling Settings", "territory",
                                       "Territory"),
        })
        doc.insert(ignore_permissions=True)
        customer = doc.name

    company = frappe.db.get_single_value("Global Defaults", "default_company") \
        or frappe.db.get_value("Company", {}, "name")
    project = frappe.get_doc({
        "doctype": "Project", "project_name": project_title,
        "customer": customer, "company": company,
    })
    project.insert(ignore_permissions=True)
    frappe.db.commit()
    return {"project": project.name, "project_title": project.project_name,
            "customer_name": frappe.db.get_value("Customer", customer,
                                                 "customer_name") or "",
            "created": True}


def _leaf_default(single, field, doctype):
    """A LEAF of the group tree, never a group node.

    The obvious fallbacks — 'All Customer Groups', 'All Territories' — are the
    tree ROOTS, and ERPNext refuses a group node on a Customer ('Cannot select
    a Group type Customer Group'). Caught live on the very first phone-minted
    site, 2026-08-17: staging's Selling Settings has no default, the fallback
    fired, and the capture sat in 'waiting to retry' until this."""
    v = frappe.db.get_single_value(single, field)
    if v and frappe.db.exists(doctype, v) \
            and not frappe.db.get_value(doctype, v, "is_group"):
        return v
    return frappe.db.get_value(doctype, {"is_group": 0}, "name")


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
def save_annotations(name, face, data):
    """The app's annotation layer for one face: lines ({x1,y1,x2,y2,mm},
    normalized coords) and pins ({x,y,text}). Stored as DATA on the capture —
    rendering happens at view time, the face image is never touched. Last
    writer wins per face; cross-device conflict is a person re-measuring,
    which is exactly when the newer number should win."""
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("write")
    if face not in panorama.FACE_NAMES:
        frappe.throw(_("Not a face: {0}").format(face))
    if isinstance(data, str):
        try:
            data = json.loads(data)
        except Exception:
            frappe.throw(_("Annotations must be JSON"))
    if not isinstance(data, dict):
        frappe.throw(_("Annotations must be a JSON object"))
    blob = json.dumps(data, separators=(",", ":"))
    if len(blob) > 65536:
        frappe.throw(_("Annotation payload too large"))
    if not doc.meta.has_field("annotations"):
        frappe.throw(_("This bench has not migrated the annotations field yet"))
    allfaces = json.loads(doc.annotations or "{}")
    if data.get("lines") or data.get("pins"):
        allfaces[face] = data
    else:
        allfaces.pop(face, None)   # emptied on the device = removed here
    doc.db_set("annotations", json.dumps(allfaces, separators=(",", ":")),
               update_modified=True)
    return {"name": doc.name, "faces": sorted(allfaces)}


@frappe.whitelist()
def get_annotations(name):
    """Cross-device pull: every face's annotation JSON for one capture."""
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("read")
    if not doc.meta.has_field("annotations"):
        return {}
    return json.loads(doc.annotations or "{}")


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
        # Counted in Python rather than SQL: frappe rejects a function written
        # as a string in `fields` ("count(name) as n"), and the rejection is a
        # thrown message that empties the whole pane. At one page of captures
        # the rows are few enough that the query builder is not worth the risk.
        for a in frappe.get_all("Site Photo Annotation",
                                filters={"parent": ("in", names)},
                                fields=["parent"], limit_page_length=0):
            counts[a["parent"]] = counts.get(a["parent"], 0) + 1
    for r in rows:
        r["annotations"] = counts.get(r["name"], 0)
    return rows
