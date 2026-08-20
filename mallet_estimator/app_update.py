"""The app's own update channel — no Mac, no Play, no manual APK handling.

The camera APK embeds the licensed Insta360 SDK, so it can never sit at a
public URL. CI publishes each build into the private Drive folder
"MCFT App Releases (private)" (under the handover root, which the
mcft-erpnext-drive service account organises) together with a
camera-latest.json manifest. This module relays it to phones:

    app_update_info()  ->  the manifest, plus where to download once the
                           APK has been mirrored into the site's private
                           files (a background job does the 300+ MB pull
                           from Drive — request threads never carry it).

The phone compares version codes, downloads over its own token auth, and
hands the file to Android's installer. The one human act left is the
Install tap on the phone — the OS requires a person for that, by design.
"""

import json
import os

import frappe
from frappe import _

from mallet_estimator.drive_client import DriveClient, DriveError

RELEASES_FOLDER = "MCFT App Releases (private)"
MANIFEST = "camera-latest.json"
SETTINGS = "Site Photo Settings"
FILE_PREFIX = "mcft-site-photos-camera-"
# The mirror runs on a worker, where an uncaught death is invisible from
# outside (Error Log needs System Manager, and an OOM-killed worker writes
# nothing at all). The job parks its last traceback here so app_update_info
# can show it to an operator instead of answering 'preparing' forever.
MIRROR_ERR_KEY = "mcft_mirror_apk_error"


def _releases_folder(client):
    root = frappe.db.get_single_value(SETTINGS, "handover_folder_id")
    if not root:
        frappe.throw(_("Site Photo Settings has no handover folder id"))
    found = client.find_child(root, RELEASES_FOLDER)
    return found["id"] if found else None


def _cached_file(version_code):
    return frappe.db.get_value(
        "File", {"file_name": f"{FILE_PREFIX}{version_code}.apk"},
        ["name", "file_url"], as_dict=True)


@frappe.whitelist()
def app_update_info():
    """The newest camera build, per the Drive manifest. status:
    'ready' (file_url downloadable with the caller's token) |
    'preparing' (mirror job enqueued — ask again next sync) |
    'none' (nothing published yet)."""
    frappe.has_permission("Site Photo 360", "read", throw=True)
    try:
        client = DriveClient()
        folder = _releases_folder(client)
    except DriveError:
        return {"status": "none"}   # bench without Drive creds = no OTA
    if not folder:
        return {"status": "none"}
    manifest = client.find_child(folder, MANIFEST)
    if not manifest:
        return {"status": "none"}
    info = json.loads(client.download(manifest["id"]).decode("utf-8"))
    version_code = int(info.get("version_code") or 0)
    out = {"status": "preparing",
           "version_name": info.get("version_name"),
           "version_code": version_code}
    cached = _cached_file(version_code)
    if cached:
        out.update({"status": "ready", "file_url": cached.file_url})
        return out
    last_error = frappe.cache().get_value(MIRROR_ERR_KEY)
    if last_error:
        out["mirror_error"] = last_error   # for operators; the app ignores it
    # Mirror in the background: the pull is hundreds of MB and must never
    # ride a request thread. Deduped by job name; repeat calls while it
    # runs just keep answering 'preparing'. timeout is explicit because the
    # long queue's default (1500s) once killed the pull mid-download.
    frappe.enqueue(
        "mallet_estimator.app_update.mirror_apk",
        queue="long", job_id=f"mirror-apk-{version_code}", timeout=3600,
        deduplicate=True, apk_name=info.get("apk"), version_code=version_code)
    return out


def mirror_apk(apk_name, version_code):
    """Pull the APK from Drive into the site's private files (standalone
    File — the photographer role's File read is what authorises the phone's
    download). The payload is touched exactly once, by a streaming download
    to disk; nothing in this function ever holds 323 MB in memory. Older
    mirrored builds are deleted: the newest is the only one anybody should
    install."""
    try:
        if _cached_file(version_code):
            return
        client = DriveClient()
        folder = _releases_folder(client)
        apk = client.find_child(folder, apk_name) if folder else None
        if not apk:
            raise DriveError(f"{apk_name} is not in the releases folder")
        fname = f"{FILE_PREFIX}{version_code}.apk"
        path = frappe.get_site_path("private", "files", fname)
        want = frappe.utils.cint(apk.get("size"))
        if want and os.path.exists(path) and os.path.getsize(path) == want:
            md5 = None      # already pulled by an earlier run that died later
        else:
            md5 = client.download_to(apk["id"], path)
        # db_insert, NOT insert: File.before_insert calls get_content(),
        # which reads the whole payload back into RAM to size-check it.
        # For a 323 MB APK that is an OOM kill — the worker dies by SIGKILL,
        # so no traceback is ever written and the phone sees 'preparing'
        # forever (2026-08-20; raising max_file_size only moved the death
        # from the size check to the allocation). db_insert writes the row
        # directly, so the bytes are touched exactly once, by the streaming
        # download above. The row is complete because we fill in by hand
        # what the controller would have computed.
        doc = frappe.new_doc("File")
        doc.update({
            "file_name": fname,
            "file_url": f"/private/files/{fname}",
            "is_private": 1,
            "folder": "Home",
            "file_size": os.path.getsize(path),
            "content_hash": md5,
        })
        doc.name = frappe.generate_hash(length=10)
        doc.db_insert()
        for old in frappe.get_all(
                "File", filters={"file_name": ["like", f"{FILE_PREFIX}%"]},
                fields=["name", "file_name"]):
            if old.file_name != fname:
                frappe.delete_doc("File", old.name, ignore_permissions=True,
                                  delete_permanently=True)
        frappe.db.commit()
        frappe.cache().delete_value(MIRROR_ERR_KEY)
    except Exception:
        frappe.cache().set_value(MIRROR_ERR_KEY, frappe.get_traceback()[-1500:])
        raise
