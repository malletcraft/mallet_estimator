# The scheduled loop, driven against a FAKE Drive so the whole job runs with
# no network and no credential. What matters here is that running it twice
# changes nothing the second time — a scheduler repeats every hour, and a sync
# that duplicates work is worse than no sync at all.
import io

import frappe
from PIL import Image

from mallet_estimator import handover, imagemeter_sync

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


def _jpeg(px=64, colour=(120, 130, 140)):
    b = io.BytesIO()
    Image.new("RGB", (px, px), colour).save(b, format="JPEG")
    return b.getvalue()


class FakeDrive:
    """Enough Drive to run the job: folders, uploads, downloads, walking."""

    def __init__(self, returning=()):
        self.folders = {"ROOT": {}}      # id -> {name: child_id}
        self.files = {}                  # id -> (parent, name, bytes)
        self._n = 0
        self.returning = list(returning)
        self.uploads = []

    def _id(self, p="f"):
        self._n += 1
        return f"{p}{self._n}"

    def ensure_folder(self, parent_id, name):
        kids = self.folders.setdefault(parent_id, {})
        if name in kids:
            return kids[name]
        fid = self._id("dir")
        kids[name] = fid
        self.folders[fid] = {}
        return fid

    def ensure_path(self, root_id, names):
        p = root_id
        for n in names:
            p = self.ensure_folder(p, n)
        return p

    def list_children(self, parent_id, only_folders=False, page_size=200):
        out = [{"id": i, "name": n, "mimeType": "image/jpeg"}
               for i, (par, n, _) in self.files.items() if par == parent_id]
        out += [{"id": i, "name": n, "mimeType": "application/vnd.google-apps.folder"}
                for n, i in self.folders.get(parent_id, {}).items()]
        return out

    def upload(self, parent_id, name, data, mime="image/jpeg"):
        fid = self._id()
        self.files[fid] = (parent_id, name, data)
        self.uploads.append(name)
        return fid

    def download(self, file_id):
        for f in self.returning:
            if f["id"] == file_id:
                return f.get("bytes", _jpeg())
        return self.files[file_id][2]

    def walk_files(self, root_id, _trail=()):
        return list(self.returning)


def _company():
    n = frappe.db.get_value("Company", {}, "name")
    return n or frappe.get_doc({
        "doctype": "Company", "company_name": "Mallet Test Co", "abbr": "MTC",
        "default_currency": "INR", "country": "India"}).insert(
        ignore_permissions=True).name


def _project():
    n = frappe.db.get_value("Project", {"project_name": "ZZ Sync Test"}, "name")
    return n or frappe.get_doc({
        "doctype": "Project", "project_name": "ZZ Sync Test",
        "company": _company()}).insert(ignore_permissions=True).name


def _room():
    return frappe.db.get_value("Estimate Room", {}, "name") or frappe.get_doc({
        "doctype": "Estimate Room", "room_name": "ZZ Sync Room"}).insert(
        ignore_permissions=True).name


def _split_capture():
    """A capture that looks as though the splitter already ran."""
    doc = frappe.get_doc({
        "doctype": "Site Photo 360", "project": _project(), "room": _room(),
        "capture_date": "2026-08-15", "stage": "Carpentry", "fov": 110,
        "face_px": 400}).insert(ignore_permissions=True)
    for face in ("front", "right", "back", "left", "up", "down"):
        f = frappe.get_doc({
            "doctype": "File", "file_name": f"{doc.name}_{face}.jpg",
            "attached_to_doctype": "Site Photo 360", "attached_to_name": doc.name,
            "is_private": 1, "content": _jpeg()}).insert(ignore_permissions=True)
        doc.db_set(f"face_{face}", f.file_url, update_modified=False)
    doc.db_set("status", "Split", update_modified=False)
    return frappe.get_doc("Site Photo 360", doc.name)


class TestImageMeterSync(MalletTestCase):

    def setUp(self):
        s = frappe.get_single("Site Photo Settings")
        s.sync_enabled = 1
        s.handover_folder_id = "ROOT"
        s.imagemeter_folder_id = "IM"
        s.save(ignore_permissions=True)

    # ---- push ----------------------------------------------------------
    def test_a_split_capture_is_handed_over_once(self):
        doc = _split_capture()
        drive = FakeDrive()
        first = imagemeter_sync.push_handovers(client=drive)
        self.assertGreaterEqual(first["uploaded"], 6)
        names = set(drive.uploads)
        self.assertIn(f"{doc.name}_front.jpg", names)
        self.assertIn(f"{doc.name}_top.jpg", names)     # up is named top on Drive

        drive.uploads.clear()
        again = imagemeter_sync.push_handovers(client=drive)
        self.assertEqual(drive.uploads, [],
                         "a second run must not re-upload the same faces")
        self.assertEqual(again["uploaded"], 0)

    def test_the_handover_folder_is_recorded_on_the_capture(self):
        doc = _split_capture()
        imagemeter_sync.push_handovers(client=FakeDrive())
        doc.reload()
        self.assertTrue(doc.handover_folder_id)
        self.assertTrue(doc.handover_at)

    # ---- pull ----------------------------------------------------------
    def test_a_returning_face_attaches_itself_once(self):
        doc = _split_capture()
        returning = [{"id": "drv1", "title": f"{doc.name}_front.jpg",
                      "parents_path": ["Client", "Room"], "modified": "2026-08-15T13:24:25Z",
                      "bytes": _jpeg(80, (200, 40, 40))}]
        drive = FakeDrive(returning=returning)

        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["attached"], 1, out)
        doc.reload()
        self.assertEqual(len(doc.annotations), 1)
        self.assertEqual(doc.annotations[0].face, "front")
        self.assertEqual(doc.annotations[0].source, "ImageMeter")
        self.assertEqual(doc.annotations[0].drive_file_id, "drv1")

        again = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(again["attached"], 0, "the same Drive file must not import twice")
        doc.reload()
        self.assertEqual(len(doc.annotations), 1)

    def test_top_and_bottom_come_back_as_up_and_down(self):
        doc = _split_capture()
        drive = FakeDrive(returning=[
            {"id": "t1", "title": f"{doc.name}_top.jpg", "parents_path": [], "modified": ""},
            {"id": "b1", "title": f"{doc.name}_bottom.jpg", "parents_path": [], "modified": ""}])
        imagemeter_sync.pull_annotations(client=drive)
        doc.reload()
        self.assertEqual({a.face for a in doc.annotations}, {"up", "down"})

    def test_an_unidentifiable_file_waits_in_the_inbox(self):
        _split_capture()
        drive = FakeDrive(returning=[
            {"id": "im9", "title": "image_from_15._Aug_2026.jpg",
             "parents_path": ["yogesh_sar", "master_Bad"], "modified": "2026-08-15T13:00:00Z"}])
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["attached"], 0)
        self.assertEqual(out["queued"], 1)
        row = frappe.get_doc("Site Photo Inbox", {"drive_file_id": "im9"})
        self.assertEqual(row.status, "Pending")
        self.assertEqual(row.folder_path, "yogesh_sar/master_Bad")

        # and it does not pile up a new row every hour
        out2 = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out2["queued"], 0)
        self.assertEqual(frappe.db.count("Site Photo Inbox", {"drive_file_id": "im9"}), 1)

    def test_an_ignored_file_stays_ignored(self):
        _split_capture()
        drive = FakeDrive(returning=[
            {"id": "ig1", "title": "IMG_20260806_183352.jpg", "parents_path": [], "modified": ""}])
        imagemeter_sync.pull_annotations(client=drive)
        row = frappe.get_doc("Site Photo Inbox", {"drive_file_id": "ig1"})
        row.db_set("status", "Ignored")
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["queued"], 0)
        self.assertEqual(out["skipped"], 1, "a rejected file must not return every hour")

    def test_a_file_naming_an_unknown_capture_is_not_attached(self):
        _split_capture()
        drive = FakeDrive(returning=[
            {"id": "gh1", "title": "MEST-PH-2099-00042_front.jpg",
             "parents_path": [], "modified": ""}])
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["attached"], 0)
        self.assertEqual(out["queued"], 1)

    # ---- wiring --------------------------------------------------------
    def test_sync_is_a_no_op_when_disabled(self):
        s = frappe.get_single("Site Photo Settings")
        s.sync_enabled = 0
        s.save(ignore_permissions=True)
        self.assertIn("skipped", imagemeter_sync.sync())

    def test_the_masters_exist(self):
        self.assertTrue(frappe.db.exists("DocType", "Site Photo Settings"))
        self.assertTrue(frappe.db.exists("DocType", "Site Photo Inbox"))
        meta = frappe.get_meta("Site Photo Annotation")
        for f in ("source", "drive_file_id", "drive_modified"):
            self.assertTrue(meta.has_field(f), f)
        self.assertEqual(handover.handover_filename("X", "up"), "X_top.jpg")
