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

    def walk_files(self, root_id, _trail=(), since=None, name_prefixes=()):
        # Accepts the narrowing arguments and returns everything anyway, on
        # purpose. Making the fake filter too seemed more honest and was
        # wrong: the cutoff is stamped at the first sync, so every fixture
        # with a past timestamp vanished, and the tests for the CAP and for
        # HISTORY — which exist precisely to watch what happens to files that
        # do arrive — were left with nothing to watch.
        #
        # The narrowing is a property of the QUERY, and it is checked where it
        # lives: test_the_walk_asks_drive_for_a_narrowed_set pins the query
        # string, and it was verified against the real folder before shipping
        # (462 files to 21, losing neither an id-carrying nor a recent file).
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
        "capture_date": "2026-08-15", "stage": "Joinery", "fov": 110,
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

    def test_history_is_passed_over_not_queued(self):
        # ImageMeter's folder holds years of other clients' photos. The first
        # real run queued 394 review rows; anything older than the watermark
        # is now passed over instead.
        _split_capture()
        s = frappe.get_single("Site Photo Settings")
        s.queue_files_since = "2026-08-15 12:00:00"
        s.save(ignore_permissions=True)
        drive = FakeDrive(returning=[
            {"id": "old1", "title": "image_from_2._Jan_2024.jpg",
             "parents_path": ["someone_else"], "modified": "2024-01-02T09:00:00Z"},
            {"id": "new1", "title": "image_from_15._Aug_2026.jpg",
             "parents_path": ["yogesh"], "modified": "2026-08-15T23:00:00Z"}])
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["history"], 1, out)
        self.assertEqual(out["queued"], 1, out)
        self.assertFalse(frappe.db.exists("Site Photo Inbox", {"drive_file_id": "old1"}))
        self.assertTrue(frappe.db.exists("Site Photo Inbox", {"drive_file_id": "new1"}))

    def test_a_file_with_no_timestamp_is_queued_not_dropped(self):
        # Unknown age must not read as "old": that would silently discard the
        # very files a person needs to see.
        _split_capture()
        s = frappe.get_single("Site Photo Settings")
        s.queue_files_since = "2026-08-15 12:00:00"
        s.save(ignore_permissions=True)
        drive = FakeDrive(returning=[
            {"id": "nots", "title": "mystery.jpg", "parents_path": [], "modified": ""}])
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["history"], 0, out)
        self.assertEqual(out["queued"], 1, out)

    def test_an_old_file_that_names_a_capture_still_attaches(self):
        # The watermark suppresses GUESSWORK, never proof: a file that names
        # its capture is attached however old it is.
        doc = _split_capture()
        s = frappe.get_single("Site Photo Settings")
        s.queue_files_since = "2026-08-15 12:00:00"
        s.save(ignore_permissions=True)
        drive = FakeDrive(returning=[
            {"id": "oldbut", "title": f"{doc.name}_front.jpg",
             "parents_path": [], "modified": "2020-01-01T00:00:00Z"}])
        out = imagemeter_sync.pull_annotations(client=drive)
        self.assertEqual(out["attached"], 1, out)

    def test_the_watermark_is_compared_in_utc(self):
        # The site clock is Asia/Kolkata and Drive reports UTC; comparing them
        # as written would call the last five and a half hours "history".
        got = imagemeter_sync.to_drive_utc("2026-08-15 23:30:00")
        self.assertTrue(got.endswith("Z"), got)
        self.assertLess(got, "2026-08-15T23:30:00Z",
                        f"{got} should be EARLIER than the same wall clock in UTC")

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

    def test_a_face_from_a_synced_device_capture_attaches_itself(self):
        # The whole return leg for the phone app, through the real queries.
        # The pure tests proved the DECISION was right while the DB query
        # feeding it was wrong: filtering ["not in", ["", None]] is never true
        # in SQL, so the device map came back empty and every device face was
        # told its capture had not synced when it plainly had. Found on the
        # live site, 2026-08-17 — nothing in the suite went near that query.
        dev = "MCAP-0f1e2d3c4b5a"
        doc = _split_capture()
        doc.db_set("device_capture_id", dev, update_modified=False)
        returning = [{"id": "drvdev", "title": f"{dev}_front.jpg",
                      "parents_path": ["Client", "Room"],
                      "modified": "2026-08-17T04:46:44Z", "bytes": _jpeg(80, (30, 180, 90))}]
        out = imagemeter_sync.pull_annotations(client=FakeDrive(returning=returning))
        self.assertEqual(out["attached"], 1, f"must attach, got {out}")
        doc.reload()
        self.assertTrue(any(a.face == "front" for a in doc.annotations))

    def test_a_device_capture_is_never_handed_over_twice(self):
        # It was handed to ImageMeter on the phone already. Pushing it to
        # Drive would put a second copy of the wall in front of the annotator
        # with nothing to say which is current. Same NULL trap on the other
        # side: ["in", ["", None]] happened to work, which is not a reason to
        # keep depending on it.
        device = _split_capture()
        device.db_set("device_capture_id", "MCAP-99887766aabb", update_modified=False)
        server_born = _split_capture()

        drive = FakeDrive()
        imagemeter_sync.push_handovers(client=drive)
        names = set(drive.uploads)
        self.assertIn(f"{server_born.name}_front.jpg", names,
                      "an ordinary capture must still be handed over")
        self.assertNotIn(f"{device.name}_front.jpg", names,
                         "a device capture was already handed over on the phone")

    def test_the_cap_can_only_ever_drop_the_oldest(self):
        # Drive's order is not chronological, so capping it dropped arbitrary
        # files — and the real folder already held more than the cap (462 vs
        # 400 on 2026-08-17). A returning photo outside the window is never
        # seen and nothing reports an error.
        doc = _split_capture()
        old = [{"id": f"cap-old{i}", "title": f"image_from_1._Jan_2020-{i}.jpg",
                "parents_path": [], "modified": "2020-01-01T00:00:00Z",
                "bytes": _jpeg()} for i in range(5)]
        fresh = {"id": "cap-fresh", "title": f"{doc.name}_front.jpg",
                 "parents_path": [], "modified": "2026-08-17T04:46:44Z",
                 "bytes": _jpeg(80, (10, 20, 200))}
        # The newest file LAST in Drive order, which is what broke it.
        out = imagemeter_sync.pull_annotations(
            client=FakeDrive(returning=old + [fresh]), limit=3)
        self.assertEqual(out["attached"], 1, f"the newest file must be seen: {out}")
        self.assertEqual(out["not_scanned"], 3, "and the drop must be reported")

    def test_a_file_with_no_timestamp_outranks_the_cap(self):
        # Undatable means we cannot call it old, so it must not be what the
        # cap discards.
        doc = _split_capture()
        dated = [{"id": f"nd-d{i}", "title": f"image_from_1._Jan_2026-{i}.jpg",
                  "parents_path": [], "modified": "2026-01-01T00:00:00Z",
                  "bytes": _jpeg()} for i in range(4)]
        undated = {"id": "nd-undated", "title": f"{doc.name}_back.jpg",
                   "parents_path": [], "bytes": _jpeg(80, (200, 10, 10))}
        out = imagemeter_sync.pull_annotations(
            client=FakeDrive(returning=dated + [undated]), limit=2)
        self.assertEqual(out["attached"], 1, f"undated file must survive: {out}")

    def test_imagemeters_own_spreadsheets_never_reach_the_queue(self):
        before = frappe.db.count("Site Photo Inbox")
        junk = [{"id": "junk-x1", "title": "Kids_Bedroom.xlsx", "parents_path": [],
                 "modified": "2026-08-17T04:47:43Z", "bytes": b"not an image"},
                {"id": "junk-x2", "title": "Kids_Bedroom-copy.xlsx", "parents_path": [],
                 "modified": "2026-08-17T04:47:49Z", "bytes": b"not an image"}]
        out = imagemeter_sync.pull_annotations(client=FakeDrive(returning=junk))
        self.assertEqual(out["queued"], 0, f"nothing to ask a person about: {out}")
        self.assertEqual(frappe.db.count("Site Photo Inbox"), before)

    def test_the_walk_asks_drive_for_a_narrowed_set(self):
        # The real fix: 383 of 400 files scanned were history, fetched and
        # dismissed every hour. Narrowing has to happen AT Drive, and it has
        # to be the QUERY that does it — a client that fetches everything and
        # filters afterwards costs the same and still crowds the cap.
        from mallet_estimator import drive_client
        seen = {}

        class Spy(drive_client.DriveClient):
            def __init__(self):
                pass

            def list_children(self, parent_id, only_folders=False,
                              page_size=200, extra_q=None):
                seen["q"] = extra_q
                return []

        Spy().walk_files("ROOT", since="2026-08-15T00:00:00",
                         name_prefixes=("MEST-PH-", "MCAP-"))
        q = seen["q"] or ""
        self.assertIn("modifiedTime > '2026-08-15T00:00:00'", q)
        self.assertIn("name contains 'MEST-PH-'", q)
        self.assertIn("name contains 'MCAP-'", q)
        # Folders must survive their own timestamp or an old folder hides
        # every new photo inside it.
        self.assertIn("application/vnd.google-apps.folder", q)

    def test_a_capture_named_file_still_attaches_however_old_it_looks(self):
        # Naming a capture outranks age. Narrowing must not quietly repeal
        # that rule by never fetching the file in the first place.
        doc = _split_capture()
        ancient = [{"id": "ancient-1", "title": f"{doc.name}_front.jpg",
                    "parents_path": [], "modified": "2019-01-01T00:00:00Z",
                    "bytes": _jpeg(80, (5, 5, 200))}]
        out = imagemeter_sync.pull_annotations(client=FakeDrive(returning=ancient))
        self.assertEqual(out["attached"], 1, f"age must not beat a named capture: {out}")

    def test_the_masters_exist(self):
        self.assertTrue(frappe.db.exists("DocType", "Site Photo Settings"))
        self.assertTrue(frappe.db.exists("DocType", "Site Photo Inbox"))
        meta = frappe.get_meta("Site Photo Annotation")
        for f in ("source", "drive_file_id", "drive_modified"):
            self.assertTrue(meta.has_field(f), f)
        self.assertEqual(handover.handover_filename("X", "up"), "X_top.jpg")
