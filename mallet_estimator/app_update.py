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

import frappe
from frappe import _

from mallet_estimator.drive_client import DriveClient, DriveError

RELEASES_FOLDER = "MCFT App Releases (private)"
MANIFEST = "camera-latest.json"
SETTINGS = "Site Photo Settings"
FILE_PREFIX = "mcft-site-photos-camera-"


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
    # Mirror in the background: the pull is hundreds of MB and must never
    # ride a request thread. Deduped by job name; repeat calls while it
    # runs just keep answering 'preparing'.
    frappe.enqueue(
        "mallet_estimator.app_update.mirror_apk",
        queue="long", job_id=f"mirror-apk-{version_code}",
        deduplicate=True, apk_name=info.get("apk"), version_code=version_code)
    return out


def mirror_apk(apk_name, version_code):
    """Pull the APK from Drive into the site's private files (standalone
    File — the photographer role's File read is what authorises the phone's
    download). Older mirrored builds are deleted: the newest is the only
    one anybody should install."""
    if _cached_file(version_code):
        return
    client = DriveClient()
    folder = _releases_folder(client)
    apk = client.find_child(folder, apk_name) if folder else None
    if not apk:
        return
    data = client.download(apk["id"])
    frappe.get_doc({
        "doctype": "File",
        "file_name": f"{FILE_PREFIX}{version_code}.apk",
        "is_private": 1,
        "content": data,
    }).insert(ignore_permissions=True)
    for old in frappe.get_all(
            "File", filters={"file_name": ["like", f"{FILE_PREFIX}%"]},
            fields=["name", "file_name"]):
        if old.file_name != f"{FILE_PREFIX}{version_code}.apk":
            frappe.delete_doc("File", old.name, ignore_permissions=True,
                              delete_permanently=True)
    frappe.db.commit()
