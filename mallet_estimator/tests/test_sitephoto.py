# The PWA's server contract. The phone never holds an API key — it calls these
# as the logged-in user — so what is asserted here is the SHAPE the app binds
# to plus the guard rails (real masters only, faces validated, annotations
# additive).
import frappe

from mallet_estimator import panorama, sitephoto

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


def _project():
    name = frappe.db.get_value("Project", {"project_name": "ZZ Site Photo Test"}, "name")
    if name:
        return name
    return frappe.get_doc({
        "doctype": "Project", "project_name": "ZZ Site Photo Test",
    }).insert(ignore_permissions=True).name


def _room():
    name = frappe.db.get_value("Estimate Room", {}, "name")
    if name:
        return name
    return frappe.get_doc({
        "doctype": "Estimate Room", "room_name": "ZZ Test Room",
    }).insert(ignore_permissions=True).name


class TestSitePhotoApi(MalletTestCase):

    def test_bootstrap_offers_only_real_masters(self):
        # A capture that could name a room freehand would file itself outside
        # the room vocabulary the SKU codes use — so the picker is fed by the
        # masters, and this asserts it.
        _project()
        boot = sitephoto.bootstrap()
        for key in ("projects", "rooms", "stages", "faces", "default_fov",
                    "fov_min", "fov_max", "user", "can_create"):
            self.assertIn(key, boot, f"bootstrap missing {key}")
        self.assertEqual(boot["default_fov"], int(panorama.DEFAULT_FOV))
        self.assertEqual(tuple(boot["faces"]), panorama.FACE_NAMES)
        self.assertTrue(boot["rooms"], "no rooms offered")
        for r in boot["rooms"]:
            self.assertTrue(frappe.db.exists("Estimate Room", r), r)
        for p in boot["projects"]:
            self.assertTrue(frappe.db.exists("Project", p["project"]), p["project"])
            self.assertIn("title", p)

    def test_create_capture_records_where_it_belongs(self):
        made = sitephoto.create_capture(
            project=_project(), room=_room(), capture_date="2026-08-15",
            stage="Carpentry", fov=110)
        doc = frappe.get_doc("Site Photo 360", made["name"])
        self.assertEqual(doc.project, _project())
        self.assertEqual(doc.room, _room())
        self.assertEqual(doc.stage, "Carpentry")
        self.assertEqual(doc.fov, 110)
        self.assertEqual(doc.status, "Pending")   # nothing to split yet
        self.assertTrue(doc.name.startswith("MEST-PH-"), doc.name)

    def test_an_absurd_fov_is_clamped_not_obeyed(self):
        made = sitephoto.create_capture(project=_project(), room=_room(), fov=999)
        self.assertEqual(frappe.db.get_value("Site Photo 360", made["name"], "fov"),
                         int(panorama.FOV_MAX))

    def test_annotations_stack_and_never_touch_the_generated_face(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        name = made["name"]
        sitephoto.annotate(name, "front", "/private/files/a.jpg", "cracked edge")
        out = sitephoto.annotate(name, "front", "/private/files/b.jpg", "again")
        self.assertEqual(out["count"], 2, "a second note must not replace the first")
        doc = sitephoto.detail(name)
        self.assertEqual(len(doc["annotations"]), 2)
        self.assertEqual(doc["annotations"][0]["note"], "cracked edge")
        # the face field itself is untouched by annotating
        self.assertFalse(doc["face_front"])

    def test_an_unknown_face_is_refused(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        with self.assertRaises(frappe.ValidationError):
            sitephoto.annotate(made["name"], "sideways", "/private/files/x.jpg")

    def test_delete_annotation_leaves_the_others(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        sitephoto.annotate(made["name"], "up", "/private/files/1.jpg", "one")
        sitephoto.annotate(made["name"], "down", "/private/files/2.jpg", "two")
        doc = sitephoto.detail(made["name"])
        sitephoto.delete_annotation(made["name"], doc["annotations"][0]["idx"])
        left = sitephoto.detail(made["name"])["annotations"]
        self.assertEqual(len(left), 1)
        self.assertEqual(left[0]["note"], "two")

    def test_timeline_is_scoped_to_one_room(self):
        proj = _project()
        rooms = [r.name for r in frappe.get_all("Estimate Room", limit_page_length=2)]
        if len(rooms) < 2:
            self.skipTest("need two rooms")
        sitephoto.create_capture(project=proj, room=rooms[0], capture_date="2026-08-01")
        sitephoto.create_capture(project=proj, room=rooms[1], capture_date="2026-08-02")
        only = sitephoto.timeline(project=proj, room=rooms[0])
        self.assertTrue(only)
        self.assertTrue(all(r["room"] == rooms[0] for r in only))
        # and the fields the app binds to are all present
        for f in ("name", "capture_date", "stage", "status", "face_front"):
            self.assertIn(f, only[0])

    def test_the_pwa_shell_is_installed(self):
        # The app is a page on the bench, not a separate deploy — if the shell
        # or its manifest goes missing the phone silently 404s.
        import os
        base = frappe.get_app_path("mallet_estimator")
        for rel in ("www/sitephoto/index.html", "www/sitephoto/index.py",
                    "public/sitephoto/manifest.json",
                    "public/images/sitephoto-192.png",
                    "public/images/sitephoto-512.png"):
            self.assertTrue(os.path.exists(os.path.join(base, rel)), f"missing {rel}")
