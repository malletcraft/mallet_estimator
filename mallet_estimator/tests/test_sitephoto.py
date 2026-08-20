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


def _company():
    name = frappe.db.get_value("Company", {}, "name")
    if name:
        return name
    return frappe.get_doc({
        "doctype": "Company", "company_name": "Mallet Test Co", "abbr": "MTC",
        "default_currency": "INR", "country": "India",
    }).insert(ignore_permissions=True).name


def _project():
    name = frappe.db.get_value("Project", {"project_name": "ZZ Site Photo Test"}, "name")
    if name:
        return name
    # Company is mandatory on ERPNext's Project — a bare insert fails in CI.
    return frappe.get_doc({
        "doctype": "Project", "project_name": "ZZ Site Photo Test",
        "company": _company(),
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

    def test_capture_records_the_phones_app_version(self):
        # The fleet's version ledger: "which build is that phone running"
        # answered server-side, one row per capture.
        made = sitephoto.create_capture(
            project=_project(), room=_room(), app_version="0.3.41")
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", made["name"],
                                "device_app_version"),
            "0.3.41")
        # Absent stays empty, never invented.
        made = sitephoto.create_capture(project=_project(), room=_room())
        self.assertFalse(
            frappe.db.get_value("Site Photo 360", made["name"],
                                "device_app_version"))

    def test_annotations_round_trip_per_face(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        payload = {"lines": [{"x1": 0.1, "y1": 0.2, "x2": 0.8, "y2": 0.2,
                              "mm": 1220}],
                   "pins": [{"x": 0.5, "y": 0.9, "text": "damp patch"}]}
        out = sitephoto.save_annotations(made["name"], "front", payload)
        self.assertEqual(out["faces"], ["front"])
        got = sitephoto.get_annotations(made["name"])
        self.assertEqual(got["front"]["lines"][0]["mm"], 1220)
        self.assertEqual(got["front"]["pins"][0]["text"], "damp patch")
        # emptied on the device = removed here
        sitephoto.save_annotations(made["name"], "front",
                                   {"lines": [], "pins": []})
        self.assertEqual(sitephoto.get_annotations(made["name"]), {})

    def test_annotations_refuse_junk(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        with self.assertRaises(frappe.exceptions.ValidationError):
            sitephoto.save_annotations(made["name"], "ceiling", {"lines": []})
        with self.assertRaises(frappe.exceptions.ValidationError):
            sitephoto.save_annotations(made["name"], "front", "not json {{")

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

    def test_room_captures_counts_the_annotations(self):
        # The browser's right-hand pane. It shipped untested and threw on the
        # very first click — frappe refuses a SQL function written as a string
        # in `fields`, so the pane came up empty behind an error dialog.
        proj = _project()
        room = _room()
        made = sitephoto.create_capture(project=proj, room=room)
        sitephoto.create_capture(project=proj, room=room)
        sitephoto.annotate(made["name"], "front", "/private/files/a.jpg", "one")
        sitephoto.annotate(made["name"], "front", "/private/files/b.jpg", "two")

        rows = sitephoto.room_captures(proj, room)
        self.assertGreaterEqual(len(rows), 2)
        by_name = {r["name"]: r for r in rows}
        self.assertEqual(by_name[made["name"]]["annotations"], 2)
        # a capture nobody annotated reports zero, not a missing key
        self.assertTrue(all("annotations" in r for r in rows))
        self.assertIn(0, [r["annotations"] for r in rows])

    def test_a_new_site_typed_on_a_phone_becomes_a_real_project_once(self):
        # A technician at a NEW site has no project row and no signal. They
        # type the client and project; sync turns the words into masters —
        # and typing them again must match, not duplicate.
        made = sitephoto.ensure_site("ZZ New Client", "ZZ_NEW_SITE_PROJECT")
        self.assertTrue(made["created"])
        self.assertTrue(frappe.db.exists("Project", made["project"]))
        self.assertEqual(made["customer_name"], "ZZ New Client")

        again = sitephoto.ensure_site("ZZ New Client", "ZZ_NEW_SITE_PROJECT")
        self.assertFalse(again["created"])
        self.assertEqual(again["project"], made["project"])
        # and a capture can immediately file against it
        cap = sitephoto.create_capture(project=made["project"], room=_room())
        self.assertTrue(cap["name"])

    def test_site_matching_ignores_case_spaces_and_underscores(self):
        # 'Yogesh_Sahasrabudhe' typed as 'yogesh sahasrabudhe' is the same
        # person. Photos split across two spellings of one client are worse
        # than either spelling alone.
        first = sitephoto.ensure_site("ZZ Match Client", "ZZ_MATCH_PROJECT")
        variant = sitephoto.ensure_site("zz  match_client", "zz match project")
        self.assertFalse(variant["created"])
        self.assertEqual(variant["project"], first["project"])
        self.assertEqual(
            1, len([p for p in frappe.get_all("Project", fields=["project_name"],
                                              limit_page_length=0)
                    if "match" in (p.project_name or "").lower()
                    and "zz" in (p.project_name or "").lower()]))

    def test_a_site_is_created_even_with_no_selling_defaults(self):
        # Staging's exact shape when the FIRST phone-minted site failed:
        # Selling Settings had NO customer_group, the old fallback 'All
        # Customer Groups' is the tree ROOT, and ERPNext refuses a group node
        # on a Customer — so the capture sat in 'waiting to retry'. (A group
        # node cannot even be STORED in Selling Settings — its own validation
        # refuses, which is what my first version of this test tripped over —
        # so EMPTY is the reachable hazard, and the one reproduced here.)
        s = frappe.get_single("Selling Settings")
        old = s.customer_group
        s.customer_group = None
        s.save(ignore_permissions=True)
        try:
            made = sitephoto.ensure_site("ZZ No Defaults Client",
                                         "ZZ_NO_DEFAULTS_PROJECT")
            self.assertTrue(made["created"])
            group = frappe.db.get_value(
                "Customer", {"customer_name": "ZZ No Defaults Client"},
                "customer_group")
            self.assertFalse(
                frappe.db.get_value("Customer Group", group, "is_group"),
                f"{group} is a group node — a Customer needs a leaf")
        finally:
            s.customer_group = old
            s.save(ignore_permissions=True)

    def test_a_blank_site_name_is_refused(self):
        # A blank that slipped through would mint a nameless customer that
        # every later blank matches — a black hole for photos.
        with self.assertRaises(frappe.ValidationError):
            sitephoto.ensure_site("", "Some Project")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.ensure_site("Some Client", "   ")

    def test_a_device_capture_syncs_once_however_often_it_is_retried(self):
        # The phone queues captures offline and retries. A connection that
        # drops the acknowledgement AFTER the insert succeeded would otherwise
        # file the same room twice, with nobody able to say which is real.
        dev = "MCAP-0123456789ab"
        first = sitephoto.create_capture(project=_project(), room=_room(),
                                         device_capture_id=dev)
        again = sitephoto.create_capture(project=_project(), room=_room(),
                                         device_capture_id=dev)
        self.assertEqual(first["name"], again["name"])
        self.assertTrue(again.get("already_synced"))
        self.assertEqual(
            frappe.db.count("Site Photo 360", {"device_capture_id": dev}), 1)

    def test_a_made_up_device_id_is_refused(self):
        # The id is what an annotated face comes home by. Accepting a loose
        # string would let a typo become a permanent, unmatchable capture.
        with self.assertRaises(frappe.ValidationError):
            sitephoto.create_capture(project=_project(), room=_room(),
                                     device_capture_id="not-a-device-id")

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

    def test_the_worker_is_registered_for_the_page_it_must_control(self):
        # Registering /sitephoto/sw.js without a scope gives it the DEFAULT
        # scope /sitephoto/ — and the app lives at /sitephoto, no trailing
        # slash, which is not under that string. The worker then controls
        # nothing: invisible online, and on site the app does not open at all.
        # Verified in a browser (2026-08-16): pre-fix the page reported
        # controller=false with scope /sitephoto/.
        import os
        import re
        base = frappe.get_app_path("mallet_estimator")
        with open(os.path.join(base, "www/sitephoto/index.html")) as f:
            html = f.read()
        reg = re.search(r"serviceWorker\.register\(([^)]*)\)", html)
        self.assertTrue(reg, "the shell must register a service worker")
        self.assertIn("scope", reg.group(1),
                      "the worker must be registered with an EXPLICIT scope")
        self.assertIn("'/sitephoto'", reg.group(1),
                      "the scope must cover /sitephoto itself, not just /sitephoto/")

    def test_the_masters_survive_a_dead_network(self):
        # An offline shell that opens to empty pickers is not offline support:
        # nothing can be chosen, so nothing reaches the queue. The last good
        # bootstrap is kept on the phone and used when the call fails.
        import os
        base = frappe.get_app_path("mallet_estimator")
        with open(os.path.join(base, "www/sitephoto/index.html")) as f:
            html = f.read()
        self.assertIn("mcft_boot_v1", html, "the masters must be cached on the device")
        self.assertIn("stale_masters", html,
                      "and the app must SAY the list is old, or a new room goes missing silently")

    def test_only_a_real_page_replaces_the_saved_shell(self):
        # /sitephoto is session-guarded. An expired login answers a navigation
        # with a redirect to /login, which arrives as an opaque response — and
        # caching THAT as the shell means the app opens on site showing a
        # login page it cannot complete, with the good copy overwritten.
        # Reproduced in a browser: with the guard removed, the offline load
        # navigates away chasing the cached redirect instead of rendering.
        import os
        base = frappe.get_app_path("mallet_estimator")
        with open(os.path.join(base, "public/sitephoto/sw.js")) as f:
            sw = f.read()
        for guard in ("r.ok", "r.type === 'basic'", "!r.redirected"):
            self.assertIn(guard, sw,
                          f"the shell cache must be guarded by {guard}")
