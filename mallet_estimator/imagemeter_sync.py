# The scheduled loop between ERPNext and ImageMeter, both directions.
#
#   push — a split capture's six faces, captioned, into the handover folder
#   pull — annotated JPEGs out of ImageMeter's upload folder, attached to the
#          capture they came from
#
# Everything here is idempotent by construction, because a scheduler runs it
# again every hour and a "sync" that duplicates work is worse than none: a
# face already in the folder is not re-uploaded, and an annotation already
# imported is recognised by its Drive file id, not by its name or its bytes.
#
# What it will NOT do is guess. A returning file that does not name a capture
# goes to Site Photo Inbox for a person; attaching a stranger's photo to a
# client's room is worse than asking.
import frappe
from frappe.utils import get_datetime, now_datetime

from mallet_estimator import drive_client, drive_sync, handover, panorama

SETTINGS = "Site Photo Settings"
PHOTO = "Site Photo 360"
INBOX = "Site Photo Inbox"


def _settings():
    return frappe.get_single(SETTINGS)


def _client(client=None):
    return client or drive_client.DriveClient()


# ---------------------------------------------------------------- push -----
def push_handovers(client=None, limit=25):
    """Hand over every split capture that has not been handed over yet."""
    s = _settings()
    root = (s.handover_folder_id or "").strip()
    if not root:
        return {"skipped": "no handover folder configured"}
    client = _client(client)

    # A capture born on a phone was split there and handed to ImageMeter on
    # the device, before the server ever saw it. Pushing its faces to Drive
    # would put a second copy of every wall in front of the annotator, with
    # nothing to say which one is current.
    rows = frappe.get_all(
        PHOTO, filters={"status": "Split", "device_capture_id": ["is", "not set"]},
        fields=["name", "project", "room", "capture_date", "stage",
                "handover_folder_id"],
        order_by="creation asc", limit_page_length=limit)

    out = {"captures": 0, "uploaded": 0, "errors": []}
    for row in rows:
        try:
            out["uploaded"] += _push_one(client, root, row["name"])
            out["captures"] += 1
        except Exception as exc:
            out["errors"].append(f"{row['name']}: {exc}")
            frappe.log_error(frappe.get_traceback(), f"handover push {row['name']}")
    return out


def _push_one(client, root, name):
    doc = frappe.get_doc(PHOTO, name)
    customer, project_title = _project_labels(doc.project)
    folder = client.ensure_path(root, handover.handover_folders(
        customer, project_title, doc.room, doc.capture_date, doc.stage))

    faces = {f: doc.get(f"face_{f}") for f in panorama.FACE_NAMES}
    existing = [f["name"] for f in client.list_children(folder)]
    plan = drive_sync.plan_uploads(doc.name, faces, existing_filenames=existing)

    for item in plan:
        raw = _file_content(item["source"])
        if raw is None:
            continue
        text = handover.caption_text(doc.name, doc.room, item["face"],
                                     doc.capture_date, doc.stage)
        client.upload(folder, item["filename"], handover.caption_face(raw, text))

    if doc.handover_folder_id != folder or plan:
        doc.db_set("handover_folder_id", folder, update_modified=False)
        doc.db_set("handover_at", now_datetime(), update_modified=False)
    return len(plan)


def _project_labels(project):
    if not project:
        return "unknown", "unknown"
    p = frappe.db.get_value("Project", project, ["project_name", "customer"],
                            as_dict=True) or {}
    customer = (frappe.db.get_value("Customer", p.get("customer"), "customer_name")
                if p.get("customer") else "") or p.get("customer") or "unknown"
    return customer, p.get("project_name") or project


def _file_content(file_url):
    if not file_url:
        return None
    name = frappe.db.get_value("File", {"file_url": file_url}, "name")
    if not name:
        return None
    return frappe.get_doc("File", name).get_content()


# ---------------------------------------------------------------- pull -----
def pull_annotations(client=None, limit=400):
    """Bring annotated photos home. Matched ones attach themselves; the rest
    wait in the inbox for a person."""
    s = _settings()
    root = (s.imagemeter_folder_id or "").strip()
    if not root:
        return {"skipped": "no ImageMeter folder configured"}
    client = _client(client)

    seen = _imported_file_ids()
    known = set(frappe.get_all(PHOTO, pluck="name"))
    # Faces from a phone carry the id the device minted, not the docname the
    # server assigned afterwards — so a returning file has to be translated
    # before it can be matched.
    # "is set", NOT ["not in", ["", None]] — SQL says NOT IN (…, NULL) is
    # never true, so that filter returned an empty map on every run and every
    # device face went to review saying its capture had not synced when it
    # plainly had (caught live, 2026-08-17). "is set" is the only spelling
    # that means what it says about NULL.
    device_ids = {r["device_capture_id"]: r["name"] for r in frappe.get_all(
        PHOTO, filters={"device_capture_id": ["is", "set"]},
        fields=["name", "device_capture_id"])}
    files = client.walk_files(root)[:limit]

    # ImageMeter's folder is not ours: it holds years of photos for other
    # clients. Anything modified before we ever handed a face over cannot be a
    # reply to a handover, so it is passed over rather than queued — the first
    # run against a real folder would otherwise raise hundreds of review rows
    # nobody will ever triage (2026-08-15: it raised 394).
    cutoff = _queue_cutoff(s)

    out = {"scanned": len(files), "attached": 0, "queued": 0, "skipped": 0,
           "history": 0, "errors": []}
    for f in files:
        action, payload = drive_sync.classify_return(
            f, imported_file_ids=seen, known_photos=known, device_ids=device_ids)
        try:
            if action == drive_sync.SKIP:
                out["skipped"] += 1
            elif action == drive_sync.ATTACH:
                _attach(client, payload, f)
                out["attached"] += 1
            elif cutoff and f.get("modified") and f["modified"] < cutoff:
                # Only a KNOWN older timestamp counts as history. A file whose
                # age we cannot read is not assumed old and dropped — the whole
                # point of the queue is that unprovable things get looked at.
                out["history"] += 1
            else:
                if _queue(f, payload):
                    out["queued"] += 1
                else:
                    out["skipped"] += 1
        except Exception as exc:
            out["errors"].append(f"{f.get('title')}: {exc}")
            frappe.log_error(frappe.get_traceback(), f"imagemeter pull {f.get('title')}")
    return out


def _queue_cutoff(settings):
    """The moment this site started expecting replies, as UTC.

    Stamped on first sync so a fresh install does not inherit somebody else's
    photo history. Returned in UTC because Drive reports UTC and the site
    clock is Asia/Kolkata: comparing the two as written would call the last
    five and a half hours of files "history" and silently drop them."""
    since = settings.get("queue_files_since")
    if not since:
        since = now_datetime()
        settings.db_set("queue_files_since", since, update_modified=False)
    return to_drive_utc(since)


def to_drive_utc(value):
    """Site-local Datetime -> the RFC-3339 UTC string Drive uses."""
    import datetime as _dt

    dt = get_datetime(value)
    if dt.tzinfo is None:
        try:
            import zoneinfo
            from frappe.utils import get_system_timezone
            dt = dt.replace(tzinfo=zoneinfo.ZoneInfo(get_system_timezone()))
        except Exception:
            dt = dt.replace(tzinfo=_dt.timezone.utc)
    return dt.astimezone(_dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _imported_file_ids():
    """Every Drive file already accounted for — attached OR deliberately
    ignored. Without the ignored ones a rejected file returns every hour."""
    ids = set(frappe.get_all("Site Photo Annotation",
                             filters={"drive_file_id": ["!=", ""]},
                             pluck="drive_file_id") or [])
    ids |= set(frappe.get_all(INBOX, filters={"status": ["in", ["Attached", "Ignored"]]},
                              pluck="drive_file_id") or [])
    return ids


def _attach(client, payload, drive_file):
    doc = frappe.get_doc(PHOTO, payload["photo"])
    data = client.download(payload["file_id"])
    f = frappe.get_doc({
        "doctype": "File",
        "file_name": _safe_name(payload.get("title") or payload["file_id"]),
        "attached_to_doctype": PHOTO, "attached_to_name": doc.name,
        "is_private": 1, "content": data,
    }).insert(ignore_permissions=True)
    doc.append("annotations", {
        "face": payload["face"], "image": f.file_url, "source": "ImageMeter",
        "drive_file_id": payload["file_id"],
        "drive_modified": drive_file.get("modified") or "",
        "note": "/".join(drive_file.get("parents_path") or []),
    })
    doc.save(ignore_permissions=True)


def _safe_name(title):
    import re
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", str(title)) or "annotated.jpg"


def _queue(drive_file, payload):
    fid = drive_file.get("id")
    if not fid or frappe.db.exists(INBOX, {"drive_file_id": fid}):
        return False
    frappe.get_doc({
        "doctype": INBOX, "drive_file_id": fid,
        "title": drive_file.get("title") or fid,
        "folder_path": "/".join(drive_file.get("parents_path") or []),
        "drive_modified": drive_file.get("modified") or "",
        "status": "Pending", "note": payload.get("reason") or "",
        "photo": payload.get("photo") or None,
    }).insert(ignore_permissions=True)
    return True


@frappe.whitelist()
def attach_from_inbox(name):
    """A person named the capture and face — do what the automatic path would
    have done, then mark the row so it never comes back."""
    row = frappe.get_doc(INBOX, name)
    row.check_permission("write")
    if row.status == "Attached":
        return {"already": True}
    if not (row.photo and row.face):
        frappe.throw("Choose the capture and the face first.")
    _attach(_client(), {"photo": row.photo, "face": row.face,
                        "file_id": row.drive_file_id, "title": row.title},
            {"modified": row.drive_modified, "parents_path": (row.folder_path or "").split("/")})
    row.db_set("status", "Attached", update_modified=False)
    return {"attached": True}


# ----------------------------------------------------------- scheduler -----
@frappe.whitelist()
def sync(push=True, pull=True):
    """Both directions. Whitelisted so it is also the 'Sync now' button."""
    s = _settings()
    if not s.sync_enabled:
        return {"skipped": "sync disabled in Site Photo Settings"}
    result = {}
    if push:
        result["push"] = push_handovers()
    if pull:
        result["pull"] = pull_annotations()
    s.db_set("last_sync", now_datetime(), update_modified=False)
    s.db_set("last_summary", frappe.as_json(result)[:2000], update_modified=False)
    return result


def scheduled_sync():
    """Hourly. Silent when disabled or unconfigured — a site without Drive
    wiring must not fill its error log every hour."""
    try:
        if not frappe.db.get_single_value(SETTINGS, "sync_enabled"):
            return
        sync()
    except Exception:
        frappe.log_error(frappe.get_traceback(), "imagemeter scheduled sync")
