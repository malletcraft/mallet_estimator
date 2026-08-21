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

from mallet_estimator import handover, panorama, worksite

DOCTYPE = "Site Photo 360"
STAGES = ("Baseline", "Civil", "Wiring", "Carpentry", "Finishing", "Handover")

# Field list the timeline/detail views read. Kept in one place so the PWA and
# the tests agree on the shape.
LIST_FIELDS = ["name", "project", "room", "capture_date", "stage", "status",
               "fov", "face_px", "split_error", "pano"] + \
    [f"face_{f}" for f in panorama.FACE_NAMES]


@frappe.whitelist()
def bootstrap():
    """Everything the pickers need, as the four-level tree the app browses:
    Client → SITE → Project → Room. A capture can only ever land on a REAL
    project and a REAL room because both lists come from the masters — the
    room list is the same Estimate Room master the SKU codes use, so a photo
    files itself beside YS_MB_WAR rather than beside a free-text 'master
    bedrm' someone typed on site.

    Stages ride along per job type rather than as one flat list: a repair has
    no Wiring stage and an installation has no Carpentry stage, and offering
    them is how a picker becomes a wall of thirty-nine rows nobody reads."""
    sites = {}
    if frappe.db.exists("DocType", "Mallet Site"):
        for st in frappe.get_list(
                "Mallet Site", fields=["name", "site_name", "customer",
                                       "customer_name", "site_type", "city"],
                order_by="modified desc", limit_page_length=500):
            sites[st.name] = st

    pmeta = frappe.get_meta("Project")
    has_site = pmeta.has_field("mallet_site")
    pfields = ["name", "project_name", "customer", "status"]
    # The dates and the status are what turn a project row from a name into a
    # thing with a shape — "Active, 12 Aug → 30 Sep" answers on the list what
    # otherwise costs a tap and a round trip. They are read defensively
    # because a Project doctype can be customised out from under us.
    for f in ("expected_start_date", "expected_end_date",
              "mallet_site", "mallet_job_type", "mallet_stage",
              "mallet_stage_since"):
        if pmeta.has_field(f):
            pfields.append(f)

    rows = frappe.get_list(
        "Project", filters={"status": ("!=", "Cancelled")},
        fields=pfields, order_by="modified desc", limit_page_length=200)
    # One query for every project's SKUs, not one per project. Two hundred
    # round trips is not a slow bootstrap, it is a phone on 3G at a site
    # visit deciding the app is broken.
    skus = _skus_by_project([p.name for p in rows])

    projects = []
    for p in rows:
        site = sites.get(p.get("mallet_site")) if has_site else None
        projects.append({
            "project": p.name,
            "title": p.project_name or p.name,
            "customer": p.customer or "",
            "customer_name": (frappe.db.get_value("Customer", p.customer, "customer_name")
                              if p.customer else "") or "",
            "site": p.get("mallet_site") or "",
            "site_name": (site.site_name if site else ""),
            "site_type": (site.site_type if site else ""),
            "site_city": (site.city if site else ""),
            "job_type": p.get("mallet_job_type") or worksite.NEW,
            "stage": p.get("mallet_stage") or "",
            "stage_since": str(p.get("mallet_stage_since") or ""),
            "status": p.get("status") or "",
            "start": str(p.get("expected_start_date") or ""),
            "end": str(p.get("expected_end_date") or ""),
            "skus": skus.get(p.name, []),
        })

    rooms = [r.name for r in frappe.get_list(
        "Estimate Room", fields=["name"], order_by="name", limit_page_length=200)]
    return {
        "projects": projects,
        "sites": [dict(v) for v in sites.values()],
        "rooms": rooms,
        "job_types": list(worksite.JOB_TYPES),
        "phases": list(worksite.PHASES),
        "stages": stage_master(),
        "articles": article_master(),
        # The six old words, still answered so a phone that has not updated
        # yet keeps working through one release. Removed next batch.
        "legacy_stages": list(STAGES),
        "faces": list(panorama.FACE_NAMES),
        "default_fov": int(panorama.DEFAULT_FOV),
        "fov_min": int(panorama.FOV_MIN),
        "fov_max": int(panorama.FOV_MAX),
        "user": frappe.session.user,
        "can_create": bool(frappe.has_permission(DOCTYPE, "create")),
    }


def _skus_by_project(projects):
    """Every listed project's SKUs, in one query, keyed by project.

    They ride bootstrap because the phone needs them the moment it LOSES
    signal — a technician tagging a photo to YS_MB_WAR in a basement cannot
    fetch a picker. A project has a handful of SKUs, so the whole set is
    small enough to carry."""
    if not projects or not frappe.db.exists("DocType", "Estimate SKU"):
        return {}
    fields = ["name", "project", "sku_code"]
    meta = frappe.get_meta("Estimate SKU")
    for f in ("room", "article_name", "mallet_article"):
        if meta.has_field(f):
            fields.append(f)
    out = {}
    for r in frappe.get_all("Estimate SKU", filters={"project": ("in", projects)},
                            fields=fields, limit_page_length=0):
        out.setdefault(r.project, []).append({
            "name": r.name, "code": r.get("sku_code") or r.name,
            "room": r.get("room") or "",
            "article": r.get("article_name") or "",
            "article_code": r.get("mallet_article") or ""})
    return out


def _project_skus(project):
    """One project's SKUs. Kept as its own name because the app and the tests
    both ask that question directly."""
    return _skus_by_project([project]).get(project, [])


@frappe.whitelist()
def stage_master(job_type=None):
    """The work-stage master, in trade order, optionally narrowed to one job
    type. A repair and an installation are a SLICE of the same sequence, not
    a different one, so this is one list with job types ticked on it."""
    if not frappe.db.exists("DocType", "Mallet Work Stage"):
        return []
    from mallet_estimator.mallet_estimator.doctype.mallet_work_stage.mallet_work_stage \
        import stages_for
    return [{"stage": r.name, "phase": r.phase, "sequence": r.sequence,
             "job_types": r.job_types} for r in stages_for(job_type)]


@frappe.whitelist()
def article_master(job_type=None):
    """The article master — the third token of an SKU code, as a picker."""
    if not frappe.db.exists("DocType", "Mallet Article"):
        return []
    rows = frappe.get_all(
        "Mallet Article", filters={"disabled": 0},
        fields=["name", "article_code", "article_name", "job_types"],
        order_by="article_code asc", limit_page_length=0)
    if job_type:
        rows = [r for r in rows
                if job_type in [x.strip() for x in (r.job_types or "").split(",")]]
    return [{"code": r.article_code, "article": r.article_name,
             "job_types": r.job_types} for r in rows]


@frappe.whitelist()
def create_capture(project, room, capture_date=None, stage=None, fov=None,
                   device_capture_id=None, app_version=None, work_stage=None,
                   sku=None, capture_kind=None):
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

    # The stage the project has actually reached is the right default, and
    # it is the reason stage lives on the Project at all: nobody standing in
    # a dusty flat should be picking from thirty-nine rows to file one photo.
    work_stage, stage = _resolve_stage(project, work_stage, stage)

    doc = frappe.get_doc({
        "doctype": DOCTYPE,
        "project": project,
        "room": room,
        "capture_date": capture_date or today(),
        "stage": stage or "",
        "fov": cint(fov) or int(panorama.DEFAULT_FOV),
        "device_capture_id": device_capture_id,
    })
    meta = frappe.get_meta(DOCTYPE)
    # A flat photograph is a first-class capture, not a degraded 360. A repair
    # job is a close-up of a broken hinge; splitting that into six faces would
    # be nonsense, and refusing to file it at all is why people fall back to
    # the phone's own camera app and lose the filing.
    kind = (capture_kind or "").strip() or "360"
    if meta.has_field("capture_kind"):
        doc.capture_kind = kind if kind in ("360", "Photo") else "360"
    if kind == "Photo":
        # Nothing to split, so it is born finished rather than Pending — a
        # queue of things that will never be processed is a queue that stops
        # meaning anything.
        doc.status = "Split"
    if work_stage and meta.has_field("work_stage"):
        doc.work_stage = work_stage
    if sku and meta.has_field("sku"):
        # Refused rather than silently dropped: a photo tagged to another
        # project's SKU would file itself beside the wrong estimate line, and
        # nothing downstream would ever question it.
        if frappe.db.get_value("Estimate SKU", sku, "project") != project:
            frappe.throw(_("{0} does not belong to project {1}").format(sku, project))
        doc.sku = sku
    # The phone reports its versionName with every capture — the server-side
    # answer to "which build is that phone actually running". Guarded: a
    # bench that has not migrated yet simply drops it.
    if app_version and frappe.get_meta(DOCTYPE).has_field("device_app_version"):
        doc.device_app_version = str(app_version)[:40]
    doc.insert()
    return {"name": doc.name, "status": doc.status}


def _resolve_stage(project, work_stage=None, stage=None):
    """Work out (work_stage, phase) for a capture.

    Precedence is deliberate: what the phone explicitly chose beats what the
    project is at, which beats nothing. The phase is always DERIVED from the
    stage rather than trusted from the caller — two fields that can disagree
    are two fields that eventually will.

    A phone still running yesterday's build sends one of the six old words.
    They were phases all along, so they are TRANSLATED rather than refused:
    'Carpentry' becomes 'Joinery' and the capture lands. Refusing it would
    mean every unupdated phone silently failing to sync at a site visit,
    which is the one failure this whole queue exists to prevent."""
    stage = worksite.STAGE_RENAMES.get(stage, stage)
    if not frappe.db.exists("DocType", "Mallet Work Stage"):
        return None, stage          # bench that has not migrated yet
    if not work_stage:
        work_stage = frappe.db.get_value("Project", project, "mallet_stage") \
            if frappe.get_meta("Project").has_field("mallet_stage") else None
    if not work_stage:
        return None, stage
    phase = frappe.db.get_value("Mallet Work Stage", work_stage, "phase")
    if not phase:
        return None, stage          # a stage that no longer exists: keep the phase given
    return work_stage, phase


@frappe.whitelist()
def set_project_stage(project, work_stage, remark=None):
    """Move a project to a stage, and write the move down.

    The log is the point. 'When did carpentry actually start' is a question
    every one of these jobs eventually asks, and the honest answer has to be
    recorded when it happens rather than reconstructed afterwards from photo
    timestamps — which only ever tells you when somebody remembered to take
    a picture."""
    # Gated on the CAPTURE permission, not on Project write, and then written
    # with ignore_permissions — the same gate and the same reasoning as
    # ensure_site. A site photographer holds Project read and nothing more, by
    # design: handing a phone blanket write on Project would hand it the
    # costing fields too. But the person standing in the flat is the one who
    # knows carpentry started, so this endpoint is the narrow hole they move
    # the stage through. What it changes is a word, not a rate.
    frappe.has_permission(DOCTYPE, "create", throw=True)
    doc = frappe.get_doc("Project", project)
    if not frappe.db.exists("Mallet Work Stage", work_stage):
        frappe.throw(_("No such work stage: {0}").format(work_stage))

    job = doc.get("mallet_job_type") or worksite.NEW
    jobs = frappe.db.get_value("Mallet Work Stage", work_stage, "job_types") or ""
    if job not in [j.strip() for j in jobs.split(",")]:
        frappe.throw(_("{0} is not a stage a {1} job reaches").format(work_stage, job))

    if doc.get("mallet_stage") == work_stage:
        return {"project": doc.name, "stage": work_stage, "changed": False}

    doc.mallet_stage = work_stage
    doc.mallet_stage_since = today()
    if doc.meta.has_field("mallet_stage_log"):
        doc.append("mallet_stage_log", {
            "stage": work_stage, "on_date": today(),
            "moved_by": frappe.session.user, "remark": (remark or "")[:140]})
    doc.save(ignore_permissions=True)
    return {"project": doc.name, "stage": work_stage,
            "phase": frappe.db.get_value("Mallet Work Stage", work_stage, "phase"),
            "changed": True}


@frappe.whitelist()
def set_capture_tags(name, work_stage=None, sku=None):
    """Re-file one capture: its stage, its SKU, or both.

    Both are set at the shutter from what the project is at, and both are
    routinely wrong by the time anyone looks — the photo of the wardrobe was
    taken on the way past while the project sat at First fix, and it belongs
    to YS_MB_WAR rather than to the room at large. Making them fixable on the
    phone is what stops the alternative, which is nobody fixing them at all.

    Passing None leaves a field alone; passing "" clears it. That distinction
    matters: "untag this photo" is a real thing to want, and a picker whose
    only options are 'some SKU' is a trap.
    """
    frappe.has_permission(DOCTYPE, "write", doc=name, throw=True)
    doc = frappe.get_doc(DOCTYPE, name)
    meta = frappe.get_meta(DOCTYPE)
    changed = []

    if work_stage is not None:
        stage = (work_stage or "").strip()
        if stage:
            if not frappe.db.exists("Mallet Work Stage", stage):
                frappe.throw(_("No such work stage: {0}").format(stage))
            # The phase is derived, never trusted — the same rule the shutter
            # path follows, for the same reason: two fields that can disagree
            # are two fields that eventually will.
            resolved, phase = _resolve_stage(doc.project, stage, None)
            if meta.has_field("work_stage"):
                doc.work_stage = resolved or stage
            doc.stage = phase or doc.stage
        else:
            if meta.has_field("work_stage"):
                doc.work_stage = None
        changed.append("stage")

    if sku is not None:
        code = (sku or "").strip()
        if code:
            if not meta.has_field("sku"):
                frappe.throw(_("This bench has no SKU field on a capture yet"))
            if frappe.db.get_value("Estimate SKU", code, "project") != doc.project:
                frappe.throw(
                    _("{0} does not belong to project {1}").format(code, doc.project))
            doc.sku = code
        elif meta.has_field("sku"):
            doc.sku = None
        changed.append("sku")

    if not changed:
        return {"name": doc.name, "changed": []}
    doc.save()
    return {
        "name": doc.name,
        "changed": changed,
        "stage": doc.get("stage") or "",
        "work_stage": doc.get("work_stage") or "",
        "sku": doc.get("sku") or "",
    }


def _site_key(text):
    """Case-, space- and underscore-insensitive identity for matching a name
    typed on a site against a master typed in an office. 'Yogesh_Sahasrabudhe'
    and 'yogesh sahasrabudhe' are the same person; treating them as two is how
    a customer ends up with half their photos under each spelling."""
    return re.sub(r"[\s_]+", " ", (text or "").strip()).casefold()


@frappe.whitelist()
def ensure_site(customer_name, project_title, site_name=None, site_type=None,
                job_type=None):
    """Resolve — or create — the client, SITE and project a device capture named.

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
    site_name = (site_name or "").strip()
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
            # An existing project may predate the site level, or may have been
            # created before this phone knew which site it was standing in.
            # Filling the gap is safe; overwriting a site the office chose is
            # not, so this only ever fills a blank.
            site = _attach_site(p.name, p.customer, site_name, site_type,
                                fill_only=True)
            return {"project": p.name, "project_title": p.project_name,
                    "customer_name": cust, "site": site, "created": False}

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
    if not company:
        # Company is mandatory on Project, and without this the phone gets a
        # MandatoryError raised three frames down inside frappe — true, and
        # useless to whoever is standing at the site wondering why the sync
        # failed.
        frappe.throw(_("This site has no Company yet, so a project cannot be "
                       "created. Ask the office to set one up first."))
    project = frappe.get_doc({
        "doctype": "Project", "project_name": project_title,
        "customer": customer, "company": company,
    })
    if project.meta.has_field("mallet_job_type"):
        project.mallet_job_type = job_type or worksite.NEW
    project.insert(ignore_permissions=True)
    site = _attach_site(project.name, customer, site_name, site_type)
    frappe.db.commit()
    return {"project": project.name, "project_title": project.project_name,
            "customer_name": frappe.db.get_value("Customer", customer,
                                                 "customer_name") or "",
            "site": site, "created": True}


def _attach_site(project, customer, site_name, site_type=None, fill_only=False):
    """Point a project at a site, creating the site if the office has none by
    that name. Returns the site docname, or "" on a bench whose model sync has
    not created the field yet — the app treats a blank site as 'not synced'
    rather than as an error, so an old bench keeps working."""
    if not frappe.get_meta("Project").has_field("mallet_site") or not customer:
        return ""
    have = frappe.db.get_value("Project", project, "mallet_site")
    if have and fill_only:
        return have
    site = worksite.ensure_site(customer, site_name or worksite.DEFAULT_SITE_NAME,
                                site_type)
    frappe.db.set_value("Project", project, "mallet_site", site,
                        update_modified=False)
    return site


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
    # A frozen baseline is what the SketchUp model was built from. Letting a
    # phone edit it afterwards would put the drawing and the site silently
    # out of step — the same reason a submitted Estimate freezes its rates.
    # A real change on site is a NEW baseline, not an edit of this one.
    if doc.get("baseline_frozen"):
        frappe.throw(_("{0} is a frozen geometry baseline — annotations "
                       "cannot change. Capture the room again and make the "
                       "new capture the baseline.").format(doc.name))
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
    # face_annotations, NOT annotations: that name already belongs to the
    # legacy child table of ImageMeter image-annotations (proven in CI,
    # 2026-08-20 — reusing it turned the table into a Code field and broke
    # every attach path).
    if not doc.meta.has_field("face_annotations"):
        frappe.throw(_("This bench has not migrated the face_annotations field yet"))
    allfaces = json.loads(doc.face_annotations or "{}")
    # quads are the TAGGED openings (window/door/column/beam) — a face that
    # carries only those is annotated, and forgetting them here would drop
    # exactly the marks the room model is built from.
    if data.get("lines") or data.get("pins") or data.get("quads"):
        allfaces[face] = data
    else:
        allfaces.pop(face, None)   # emptied on the device = removed here
    doc.db_set("face_annotations", json.dumps(allfaces, separators=(",", ":")),
               update_modified=True)
    return {"name": doc.name, "faces": sorted(allfaces)}


@frappe.whitelist()
def get_annotations(name):
    """Cross-device pull: every face's annotation JSON for one capture."""
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("read")
    if not doc.meta.has_field("face_annotations"):
        return {}
    return json.loads(doc.face_annotations or "{}")


@frappe.whitelist()
def freeze_baseline(name, frozen=1):
    """Freeze (or release) the geometry baseline for a room.

    Freezing is what makes a model reproducible: build the room again from
    the same baseline and you get the same shell. Releasing is deliberately
    possible — the six faces are rarely marked perfectly on the first pass —
    but it is an act with a name and a timestamp on it, not a silent edit.
    """
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("write")
    frozen = cint(frozen)
    if frozen and not doc.geometry_baseline:
        frappe.throw(_("Mark this capture as the geometry baseline first"))
    doc.db_set({
        "baseline_frozen": frozen,
        "baseline_frozen_on": frappe.utils.now() if frozen else None,
        "baseline_frozen_by": frappe.session.user if frozen else None,
    })
    return {"name": doc.name, "baseline_frozen": frozen}


@frappe.whitelist()
def room_baseline(project, room):
    """The capture the SketchUp model should be built from, or None.

    The plugin asks this rather than guessing from folder contents: the
    authority is a flag on one capture, not a path anybody can move."""
    name = frappe.db.get_value(DOCTYPE, {
        "project": project, "room": room, "geometry_baseline": 1})
    if not name:
        return None
    doc = frappe.get_doc(DOCTYPE, name)
    doc.check_permission("read")
    return {
        "name": doc.name,
        "project": doc.project,
        "room": doc.room,
        "capture_date": str(doc.capture_date or ""),
        "frozen": bool(doc.baseline_frozen),
        "annotations": json.loads(doc.face_annotations or "{}")
        if doc.meta.has_field("face_annotations") else {},
    }


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
    """Client → SITE → project → room, the way the app's folders read.

    Built from the captures that exist rather than from the masters: a room
    nobody has photographed is not a folder, it is noise. The site level is
    the one ERPNext has no concept of, so a project without one is filed
    under a single '(no site)' bucket rather than being dropped — an
    invisible photo is worse than an awkwardly-named folder."""
    rows = frappe.get_list(
        DOCTYPE, fields=["project", "room", "capture_date", "name", "status"],
        order_by="capture_date desc, creation desc", limit_page_length=5000)
    if not rows:
        return {"clients": []}

    pmeta = frappe.get_meta("Project")
    pfields = ["name", "project_name", "customer"]
    for f in ("mallet_site", "mallet_job_type", "mallet_stage"):
        if pmeta.has_field(f):
            pfields.append(f)
    projects = {}
    for p in frappe.get_all(
            "Project", filters={"name": ("in", list({r.project for r in rows if r.project}))},
            fields=pfields):
        projects[p.name] = p

    cust_names = {}
    for c in frappe.get_all(
            "Customer",
            filters={"name": ("in", list({p.customer for p in projects.values() if p.customer}))},
            fields=["name", "customer_name"]):
        cust_names[c.name] = c.customer_name or c.name

    site_rows = {}
    site_ids = {p.get("mallet_site") for p in projects.values() if p.get("mallet_site")}
    if site_ids and frappe.db.exists("DocType", "Mallet Site"):
        for st in frappe.get_all("Mallet Site", filters={"name": ("in", list(site_ids))},
                                 fields=["name", "site_name", "site_type", "city"]):
            site_rows[st.name] = st

    NO_SITE = ("", "(no site)", "")
    tree_ = {}
    for r in rows:
        p = projects.get(r.project)
        client = cust_names.get(p.customer if p else None) or "(no client)"
        st = site_rows.get(p.get("mallet_site")) if p else None
        site = (st.name, st.site_name, st.site_type) if st else NO_SITE
        ptitle = (p.project_name if p else None) or r.project or "(no project)"
        c = tree_.setdefault(client, {})
        sdict = c.setdefault(site, {})
        pr = sdict.setdefault((r.project, ptitle), {})
        room = pr.setdefault(r.room or "(no room)", {"captures": 0, "latest": None})
        room["captures"] += 1
        if not room["latest"] or str(r.capture_date or "") > room["latest"]:
            room["latest"] = str(r.capture_date or "")

    out = []
    for client in sorted(tree_):
        slist = []
        # '(no site)' sorts last on purpose: it is a bucket, not a place, and
        # a real site should never be pushed below it.
        for site in sorted(tree_[client], key=lambda x: (x[0] == "", x[1].lower())):
            plist = []
            for (pname, ptitle) in sorted(tree_[client][site], key=lambda x: x[1]):
                rooms = tree_[client][site][(pname, ptitle)]
                proj = projects.get(pname) or {}
                plist.append({
                    "project": pname, "title": ptitle,
                    "job_type": proj.get("mallet_job_type") or worksite.NEW,
                    "stage": proj.get("mallet_stage") or "",
                    "captures": sum(v["captures"] for v in rooms.values()),
                    "rooms": [{"room": k, **v} for k, v in
                              sorted(rooms.items(), key=lambda kv: kv[0])],
                })
            slist.append({"site": site[0], "site_name": site[1],
                          "site_type": site[2],
                          "captures": sum(p["captures"] for p in plist),
                          "projects": plist})
        out.append({"client": client,
                    "captures": sum(s["captures"] for s in slist),
                    "sites": slist})
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
