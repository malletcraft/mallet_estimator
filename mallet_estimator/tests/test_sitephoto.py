# The PWA's server contract. The phone never holds an API key — it calls these
# as the logged-in user — so what is asserted here is the SHAPE the app binds
# to plus the guard rails (real masters only, faces validated, annotations
# additive).
import frappe

from mallet_estimator import install, panorama, sitephoto, worksite

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
            stage="Joinery", fov=110)
        doc = frappe.get_doc("Site Photo 360", made["name"])
        self.assertEqual(doc.project, _project())
        self.assertEqual(doc.room, _room())
        self.assertEqual(doc.stage, "Joinery")
        self.assertEqual(doc.fov, 110)
        self.assertEqual(doc.status, "Pending")   # nothing to split yet
        self.assertTrue(doc.name.startswith("MEST-PH-"), doc.name)

    def test_a_phone_on_yesterdays_build_still_syncs(self):
        # An unupdated phone sends one of the six old stage words. They were
        # PHASES all along, so they are translated rather than refused —
        # refusing would mean every phone that has not updated silently
        # failing to sync at a site visit, which is the one failure the whole
        # offline queue exists to prevent.
        for old, new in (("Carpentry", "Joinery"), ("Wiring", "First fix"),
                         ("Baseline", "Survey"), ("Handover", "Closing")):
            made = sitephoto.create_capture(
                project=_project(), room=_room(), capture_date="2026-08-15",
                stage=old, fov=110)
            self.assertEqual(
                frappe.db.get_value("Site Photo 360", made["name"], "stage"),
                new, f"{old} should land as {new}")

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

    def test_one_geometry_baseline_per_project_and_room(self):
        project, room = _project(), _room()
        first = sitephoto.create_capture(project=project, room=room)
        doc = frappe.get_doc("Site Photo 360", first["name"])
        doc.geometry_baseline = 1
        doc.save()
        second = sitephoto.create_capture(project=project, room=room)
        rival = frappe.get_doc("Site Photo 360", second["name"])
        rival.geometry_baseline = 1
        with self.assertRaises(frappe.exceptions.ValidationError):
            rival.save()
        doc.geometry_baseline = 0
        doc.save()

    def test_a_frozen_baseline_refuses_annotation_edits(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        sitephoto.save_annotations(made["name"], "front", {
            "lines": [{"x1": 0.1, "y1": 0.1, "x2": 0.9, "y2": 0.1, "mm": 3050}],
            "pins": []})
        doc = frappe.get_doc("Site Photo 360", made["name"])
        doc.geometry_baseline = 1
        doc.save()
        sitephoto.freeze_baseline(made["name"])

        # Frozen: the model was built from these numbers, so they stop moving.
        with self.assertRaises(frappe.exceptions.ValidationError):
            sitephoto.save_annotations(made["name"], "front",
                                       {"lines": [], "pins": []})
        # ...but reading still works, and the plugin finds it by room.
        found = sitephoto.room_baseline(doc.project, doc.room)
        self.assertEqual(found["name"], made["name"])
        self.assertTrue(found["frozen"])
        self.assertEqual(found["annotations"]["front"]["lines"][0]["mm"], 3050)

        # Releasing is allowed, and then edits land again.
        sitephoto.freeze_baseline(made["name"], frozen=0)
        sitephoto.save_annotations(made["name"], "front",
                                   {"lines": [], "pins": []})
        self.assertEqual(sitephoto.get_annotations(made["name"]), {})
        doc.reload()
        doc.geometry_baseline = 0
        doc.save()

    def test_a_face_of_only_tagged_openings_survives(self):
        # The tagged quads ARE the geometry marks — a face carrying only
        # those must not be treated as an empty face and dropped.
        made = sitephoto.create_capture(project=_project(), room=_room())
        sitephoto.save_annotations(made["name"], "left", {
            "lines": [], "pins": [],
            "quads": [{"x1": 0.2, "y1": 0.3, "x2": 0.5, "y2": 0.28,
                       "x3": 0.5, "y3": 0.7, "x4": 0.2, "y4": 0.72,
                       "kind": "window", "note": "grille outside"}]})
        got = sitephoto.get_annotations(made["name"])
        self.assertEqual(got["left"]["quads"][0]["kind"], "window")

    def test_annotations_refuse_junk(self):
        made = sitephoto.create_capture(project=_project(), room=_room())
        with self.assertRaises(frappe.exceptions.ValidationError):
            sitephoto.save_annotations(made["name"], "ceiling", {"lines": []})
        with self.assertRaises(frappe.exceptions.ValidationError):
            sitephoto.save_annotations(made["name"], "front", "not json {{")

    def test_app_update_info_degrades_to_none_without_drive(self):
        # The CI bench has no Drive credential; phones must get a calm
        # 'none', never a 500.
        from mallet_estimator import app_update
        import os
        had = os.environ.pop("MCFT_GDRIVE_SA_JSON", None)
        try:
            self.assertEqual(app_update.app_update_info()["status"], "none")
        finally:
            if had is not None:
                os.environ["MCFT_GDRIVE_SA_JSON"] = had

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

    def test_a_flat_photo_can_be_annotated(self):
        # A phone snap taken instead of a 360 — a repair job, a single wall —
        # is its own single face, token "photo". Refusing it here is what left
        # such a photograph with no way home: the app could hand it to
        # ImageMeter and then had nowhere to put what came back.
        made = sitephoto.create_capture(project=_project(), room=_room())
        out = sitephoto.annotate(made["name"], "photo", "/private/files/p.jpg",
                                 "skirting damaged")
        self.assertEqual(out["count"], 1)
        # A REAL line, not an empty payload: an empty one deletes the face by
        # design ("emptied on the device = removed here"), so asserting it
        # survives tests the opposite of what the code promises.
        sitephoto.save_annotations(made["name"], "photo",
                                   {"lines": [{"x1": 0, "y1": 0,
                                               "x2": 1, "y2": 1, "mm": 1200}]})
        self.assertIn("photo", sitephoto.get_annotations(made["name"]))

    def test_a_name_typed_on_site_can_be_corrected(self):
        made = sitephoto.ensure_site("Renamable Client XYZ", "Renamable Project XYZ",
                                     site_name="Renamable Site XYZ")
        proj = made["project"]
        out = sitephoto.rename_node("project", proj, "Corrected Project XYZ")
        self.assertTrue(out["renamed"])
        self.assertEqual(frappe.db.get_value("Project", proj, "project_name"),
                         "Corrected Project XYZ")
        if made.get("site"):
            sitephoto.rename_node("site", made["site"], "Corrected Site XYZ")
            self.assertEqual(
                frappe.db.get_value("Mallet Site", made["site"], "site_name"),
                "Corrected Site XYZ")

    def test_a_rename_onto_another_records_spelling_is_refused(self):
        # Two customers becoming one folder is the failure ensure_site's
        # insensitive matching exists to prevent; a rename must not open the
        # same door from the other side.
        a = sitephoto.ensure_site("Clash Client Alpha", "Clash Project Alpha")
        sitephoto.ensure_site("Clash Client Beta", "Clash Project Beta")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.rename_node("project", a["project"], "clash_project beta")

    def test_a_capitalisation_fix_is_a_real_edit_not_a_clash_with_itself(self):
        made = sitephoto.ensure_site("Case Client", "case project lower")
        out = sitephoto.rename_node("project", made["project"], "Case Project Lower")
        self.assertTrue(out["renamed"])
        self.assertEqual(frappe.db.get_value("Project", made["project"],
                                             "project_name"),
                         "Case Project Lower")

    def test_rename_refuses_junk(self):
        made = sitephoto.ensure_site("Junk Client", "Junk Project")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.rename_node("project", made["project"], "   ")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.rename_node("elephant", made["project"], "Nope")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.rename_node("project", "PROJ-does-not-exist", "Nope")

    def _sku_and_capture(self, tag, kind=None):
        """A project, a SKU on it, and a capture in it — all this test's own.
        Borrowing whatever SKU happened to exist made the test depend on the
        order the suite runs in, and on that SKU belonging to the right
        project, which it need not."""
        out = sitephoto.ensure_site(f"ZZ Face {tag} Client",
                                    f"ZZ Face {tag} Project",
                                    site_name=f"ZZ Face {tag} Flat")
        sku = frappe.get_doc({
            "doctype": "Estimate SKU", "project": out["project"],
            "room": _room(), "article_name": "Wardrobe",
        })
        sku.insert(ignore_permissions=True)
        cap = sitephoto.create_capture(out["project"], _room(), capture_kind=kind)
        return sku.name, cap["name"], out["project"]

    def test_a_sku_belongs_to_a_face_not_to_the_whole_360(self):
        # Amit, 2026-08-22: "why no sku per foto?" A 360 is a whole ROOM and
        # cannot be one article; each of its six faces is one wall, and that
        # is what a SKU describes.
        sku, name, _proj = self._sku_and_capture("A")
        self.assertEqual(sitephoto.get_face_skus(name), {})
        sitephoto.set_face_sku(name, "front", sku)
        sitephoto.set_face_sku(name, "left", sku)
        self.assertEqual(sitephoto.get_face_skus(name), {"front": sku, "left": sku})
        # …and each face is independent: clearing one leaves the other.
        sitephoto.set_face_sku(name, "front", "")
        self.assertEqual(sitephoto.get_face_skus(name), {"left": sku})
        self.assertEqual(sitephoto.detail(name)["face_skus"], {"left": sku})

    def test_a_face_refuses_a_bad_face_a_missing_sku_or_another_projects_sku(self):
        sku, name, _proj = self._sku_and_capture("B")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_face_sku(name, "sideways", "")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_face_sku(name, "front", "MEST-SKU-nonexistent")
        # A wall in one client's flat cannot carry work quoted on another's
        # project — the same rule create_capture enforces on the capture.
        other_sku, _other_name, _p = self._sku_and_capture("C")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_face_sku(name, "front", other_sku)

    def test_a_flat_photo_tags_its_single_face(self):
        sku, name, _proj = self._sku_and_capture("D", kind="Photo")
        sitephoto.set_face_sku(name, "photo", sku)
        self.assertEqual(sitephoto.get_face_skus(name), {"photo": sku})

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


class TestSiteLevelAndStages(MalletTestCase):
    """Client → SITE → Project → Room, and the work-stage master under it.

    ERPNext links a Project straight to a Customer, so the site is the one
    level with nothing native behind it — which is exactly why it needs
    tests: its absence is silent until a photo files itself under the wrong
    folder, months later, on somebody's phone."""

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        from mallet_estimator import worksite
        # ensure_site creates a Project, and Company is mandatory on one. Test
        # classes do not run in a guaranteed order, so this class cannot rely
        # on another having made the company first.
        _company()
        # The role is minted on migrate, and test classes have no guaranteed
        # order — the same trap the company hit. Make the precondition here.
        from mallet_estimator import integration
        integration.ensure_photographer_role()
        worksite.ensure_articles()
        worksite.ensure_work_stages()
        install.ensure_project_customization()
        frappe.clear_cache(doctype="Project")

    # ---- the masters -----------------------------------------------------

    def test_the_stage_master_is_seeded_in_trade_order(self):
        from mallet_estimator import worksite
        rows = sitephoto.stage_master()
        self.assertGreaterEqual(len(rows), len(worksite.WORK_STAGES))
        seqs = [r["sequence"] for r in rows]
        self.assertEqual(seqs, sorted(seqs), "stage master came back out of order")

    def test_a_repair_reaches_fewer_stages_than_new_work(self):
        from mallet_estimator import worksite
        new = sitephoto.stage_master(worksite.NEW)
        rep = sitephoto.stage_master(worksite.REPAIR)
        ins = sitephoto.stage_master(worksite.INSTALL)
        self.assertTrue(rep and ins)
        self.assertLess(len(rep), len(new))
        self.assertLess(len(ins), len(new))
        # ...and it is a SLICE of the same sequence, not a separate list.
        new_names = {r["stage"] for r in new}
        self.assertTrue({r["stage"] for r in rep} & new_names)
        self.assertTrue({r["stage"] for r in ins} & new_names)

    def test_a_repair_only_stage_is_hidden_from_new_work(self):
        from mallet_estimator import worksite
        names = {r["stage"] for r in sitephoto.stage_master(worksite.NEW)}
        self.assertNotIn("Defect recorded", names)
        self.assertIn("Defect recorded",
                      {r["stage"] for r in sitephoto.stage_master(worksite.REPAIR)})

    def test_articles_are_offered_per_job_type(self):
        from mallet_estimator import worksite
        ins = {a["code"] for a in sitephoto.article_master(worksite.INSTALL)}
        self.assertIn("PVC", ins)
        self.assertNotIn("LOF", ins, "a loft is not a supply-and-install article")

    # ---- the site level --------------------------------------------------

    def test_a_site_typed_on_a_phone_lands_under_the_client(self):
        out = sitephoto.ensure_site("ZZ Site Client", "ZZ Site Project A",
                                    site_name="ZZ Kothrud Flat", site_type="Flat")
        self.assertTrue(out["site"], "no site created")
        site = frappe.get_doc("Mallet Site", out["site"])
        self.assertEqual(site.site_name, "ZZ Kothrud Flat")
        self.assertEqual(frappe.db.get_value("Project", out["project"], "mallet_site"),
                         site.name)
        self.assertEqual(frappe.db.get_value("Customer", site.customer, "customer_name"),
                         "ZZ Site Client")

    def test_the_same_site_typed_twice_is_one_site(self):
        a = sitephoto.ensure_site("ZZ Site Client 2", "ZZ Site Project B",
                                  site_name="ZZ Baner Flat")
        b = sitephoto.ensure_site("ZZ Site Client 2", "ZZ Site Project C",
                                  site_name="zz  baner_flat")
        self.assertEqual(a["site"], b["site"],
                         "spacing and case made a second folder")
        self.assertNotEqual(a["project"], b["project"],
                            "two projects at one site collapsed into one")

    def test_one_client_can_hold_two_sites(self):
        a = sitephoto.ensure_site("ZZ Two Homes", "ZZ Flat Job",
                                  site_name="ZZ City Flat")
        b = sitephoto.ensure_site("ZZ Two Homes", "ZZ Hill Job",
                                  site_name="ZZ Hill Bungalow", site_type="Bungalow")
        self.assertNotEqual(a["site"], b["site"])
        cust = frappe.db.get_value("Mallet Site", a["site"], "customer")
        self.assertEqual(cust, frappe.db.get_value("Mallet Site", b["site"], "customer"))

    def test_two_sites_of_one_name_under_one_client_are_refused(self):
        out = sitephoto.ensure_site("ZZ Dup Client", "ZZ Dup Project",
                                    site_name="ZZ Dup Flat")
        customer = frappe.db.get_value("Mallet Site", out["site"], "customer")
        with self.assertRaises(frappe.ValidationError):
            frappe.get_doc({"doctype": "Mallet Site", "customer": customer,
                            "site_name": "ZZ Dup Flat"}).insert(ignore_permissions=True)

    def test_the_tree_carries_the_site_level(self):
        out = sitephoto.ensure_site("ZZ Tree Client", "ZZ Tree Project",
                                    site_name="ZZ Tree Flat")
        sitephoto.create_capture(out["project"], _room())
        tree = sitephoto.tree()
        client = [c for c in tree["clients"] if c["client"] == "ZZ Tree Client"]
        self.assertTrue(client, "client missing from the tree")
        sites = client[0]["sites"]
        self.assertTrue(sites, "no site level in the tree")
        names = {s["site_name"] for s in sites}
        self.assertIn("ZZ Tree Flat", names)
        projects = [p for s in sites for p in s["projects"]]
        self.assertIn(out["project"], {p["project"] for p in projects})

    def test_bootstrap_carries_site_job_type_and_stages(self):
        sitephoto.ensure_site("ZZ Boot Client", "ZZ Boot Project",
                              site_name="ZZ Boot Flat")
        boot = sitephoto.bootstrap()
        for key in ("sites", "job_types", "phases", "stages", "articles"):
            self.assertIn(key, boot, f"bootstrap missing {key}")
        row = [p for p in boot["projects"] if p["title"] == "ZZ Boot Project"]
        self.assertTrue(row)
        self.assertTrue(row[0]["site"], "project came back with no site")
        self.assertEqual(row[0]["site_name"], "ZZ Boot Flat")
        self.assertTrue(row[0]["job_type"])

    # ---- stage on the project -------------------------------------------

    def test_moving_a_project_stage_is_logged(self):
        from mallet_estimator import worksite
        out = sitephoto.ensure_site("ZZ Stage Client", "ZZ Stage Project",
                                    site_name="ZZ Stage Flat")
        res = sitephoto.set_project_stage(out["project"], "Modular carpentry install")
        self.assertTrue(res["changed"])
        self.assertEqual(res["phase"], "Joinery")
        doc = frappe.get_doc("Project", out["project"])
        self.assertEqual(doc.mallet_stage, "Modular carpentry install")
        self.assertEqual(len(doc.mallet_stage_log), 1)
        self.assertEqual(doc.mallet_stage_log[0].stage, "Modular carpentry install")
        # Setting the same stage again is a no-op, not a second log row —
        # otherwise a phone retrying a sync writes the history twice.
        again = sitephoto.set_project_stage(out["project"], "Modular carpentry install")
        self.assertFalse(again["changed"])
        self.assertEqual(len(frappe.get_doc("Project", out["project"]).mallet_stage_log), 1)

    def test_a_project_cannot_move_to_a_stage_its_job_type_never_reaches(self):
        from mallet_estimator import worksite
        out = sitephoto.ensure_site("ZZ Job Client", "ZZ Install Project",
                                    site_name="ZZ Install Flat",
                                    job_type=worksite.INSTALL)
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_project_stage(out["project"], "Demolition & debris removal")

    def test_a_capture_inherits_the_projects_stage(self):
        out = sitephoto.ensure_site("ZZ Inherit Client", "ZZ Inherit Project",
                                    site_name="ZZ Inherit Flat")
        sitephoto.set_project_stage(out["project"], "Wall moulding & trims")
        cap = sitephoto.create_capture(out["project"], _room())
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual(doc.work_stage, "Wall moulding & trims")
        # The phase is DERIVED, never trusted from the caller: two fields that
        # can disagree are two fields that eventually will.
        self.assertEqual(doc.stage, "Joinery")

    def test_an_explicit_stage_beats_the_projects(self):
        out = sitephoto.ensure_site("ZZ Explicit Client", "ZZ Explicit Project",
                                    site_name="ZZ Explicit Flat")
        sitephoto.set_project_stage(out["project"], "Wall moulding & trims")
        cap = sitephoto.create_capture(out["project"], _room(),
                                       work_stage="Deep clean")
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual(doc.work_stage, "Deep clean")
        self.assertEqual(doc.stage, "Finishing")

    def test_the_phone_can_move_the_stage_without_project_write(self):
        # A site photographer holds Project READ and nothing more, by design.
        # If this endpoint demanded Project write, the stage bar -- the whole
        # point of the room screen -- would fail with a permission error on
        # every phone in the field while passing every test run as
        # Administrator.
        from mallet_estimator import integration
        rows = frappe.get_all(
            "Custom DocPerm",
            filters={"role": integration.PHOTOGRAPHER_ROLE, "parent": "Project"},
            fields=["read", "write"])
        if rows:
            self.assertFalse(rows[0].write,
                             "a phone must not hold blanket Project write")
        for dt in ("Mallet Site", "Mallet Article", "Mallet Work Stage"):
            perm = frappe.get_all(
                "Custom DocPerm",
                filters={"role": integration.PHOTOGRAPHER_ROLE, "parent": dt},
                fields=["read"])
            self.assertTrue(perm and perm[0].read,
                            f"the phone cannot read {dt}, so its tree is empty")

    # ---- SKU tagging -----------------------------------------------------

    def test_a_capture_refuses_another_projects_sku(self):
        a = sitephoto.ensure_site("ZZ Sku Client", "ZZ Sku Project A",
                                  site_name="ZZ Sku Flat")
        b = sitephoto.ensure_site("ZZ Sku Client", "ZZ Sku Project B",
                                  site_name="ZZ Sku Flat")
        sku = frappe.get_doc({
            "doctype": "Estimate SKU", "project": b["project"],
            "room": _room(), "article_name": "Wardrobe",
        })
        sku.insert(ignore_permissions=True)
        with self.assertRaises(frappe.ValidationError):
            sitephoto.create_capture(a["project"], _room(), sku=sku.name)

    def test_a_capture_accepts_its_own_projects_sku(self):
        out = sitephoto.ensure_site("ZZ Sku Own Client", "ZZ Sku Own Project",
                                    site_name="ZZ Sku Own Flat")
        sku = frappe.get_doc({
            "doctype": "Estimate SKU", "project": out["project"],
            "room": _room(), "article_name": "Wardrobe",
        })
        sku.insert(ignore_permissions=True)
        cap = sitephoto.create_capture(out["project"], _room(), sku=sku.name)
        self.assertEqual(frappe.db.get_value("Site Photo 360", cap["name"], "sku"),
                         sku.name)
        self.assertIn(sku.name,
                      {s["name"] for s in sitephoto._project_skus(out["project"])})

    # ---- re-filing a capture after the fact ------------------------------

    def test_a_capture_can_be_moved_to_another_stage(self):
        """The stage set at the shutter is a default, not a verdict.

        Every capture inherits whatever the project was at, which is right
        nine times out of ten and wrong the tenth — the wardrobe photo taken
        on the way past belongs to Joinery even though the flat is still at
        First fix. If that is not fixable on the phone it is not fixed at
        all, and the stage stops meaning anything."""
        from mallet_estimator import worksite
        out = sitephoto.ensure_site("ZZ Retag Client", "ZZ Retag Project",
                                    site_name="ZZ Retag Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        target = "Modular carpentry install"
        moved = sitephoto.set_capture_tags(cap["name"], work_stage=target)
        self.assertIn("stage", moved["changed"])
        self.assertEqual(moved["work_stage"], target)
        # The PHASE is derived, never taken from the caller.
        self.assertEqual(
            moved["stage"],
            frappe.db.get_value("Mallet Work Stage", target, "phase"))
        self.assertIn(moved["stage"], worksite.PHASES)

    def test_a_capture_refuses_a_stage_that_does_not_exist(self):
        out = sitephoto.ensure_site("ZZ Retag Bad Client", "ZZ Retag Bad Project",
                                    site_name="ZZ Retag Bad Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_capture_tags(cap["name"], work_stage="Sanding the moon")

    def test_a_capture_can_be_tagged_and_untagged_from_a_sku(self):
        """Passing "" clears; passing None leaves alone.

        'Untag this photo' is a real thing to want — a room shot filed against
        a wardrobe is worse than one filed against nothing — so the picker
        needs a way back out, and the endpoint has to tell the two apart."""
        out = sitephoto.ensure_site("ZZ Retag Sku Client", "ZZ Retag Sku Project",
                                    site_name="ZZ Retag Sku Flat")
        sku = frappe.get_doc({
            "doctype": "Estimate SKU", "project": out["project"],
            "room": _room(), "article_name": "Wardrobe",
        })
        sku.insert(ignore_permissions=True)
        cap = sitephoto.create_capture(out["project"], _room())

        sitephoto.set_capture_tags(cap["name"], sku=sku.name)
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", cap["name"], "sku"), sku.name)

        # None means "leave it alone" — a stage move must not drop the SKU.
        sitephoto.set_capture_tags(cap["name"], work_stage="Modular carpentry install")
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", cap["name"], "sku"), sku.name)

        sitephoto.set_capture_tags(cap["name"], sku="")
        self.assertFalse(
            frappe.db.get_value("Site Photo 360", cap["name"], "sku"))

    def test_a_capture_refuses_a_sku_from_another_project(self):
        a = sitephoto.ensure_site("ZZ Retag X Client", "ZZ Retag X Project A",
                                  site_name="ZZ Retag X Flat")
        b = sitephoto.ensure_site("ZZ Retag X Client", "ZZ Retag X Project B",
                                  site_name="ZZ Retag X Flat")
        sku = frappe.get_doc({
            "doctype": "Estimate SKU", "project": b["project"],
            "room": _room(), "article_name": "Wardrobe",
        })
        sku.insert(ignore_permissions=True)
        cap = sitephoto.create_capture(a["project"], _room())
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_capture_tags(cap["name"], sku=sku.name)

    def test_bootstrap_carries_the_dates_a_project_row_shows(self):
        """The projects list shows 'New work · 12 Aug → 30 Sep · Active'.

        Every part of that has to arrive in the one bootstrap call, because
        the list is drawn from cached masters on a phone with no signal — a
        field fetched lazily is a field that is blank exactly when it is
        needed."""
        out = sitephoto.ensure_site("ZZ Dates Client", "ZZ Dates Project",
                                    site_name="ZZ Dates Flat")
        proj = frappe.get_doc("Project", out["project"])
        proj.expected_start_date = "2026-08-12"
        proj.expected_end_date = "2026-09-30"
        proj.save(ignore_permissions=True)

        row = next(p for p in sitephoto.bootstrap()["projects"]
                   if p["project"] == out["project"])
        self.assertEqual(row["start"], "2026-08-12")
        self.assertEqual(row["end"], "2026-09-30")
        self.assertTrue(row["status"], "a project row with no status has no pill")

    def test_a_project_with_no_dates_reports_blanks_not_none(self):
        """The app reads these as strings. A JSON null becomes the literal
        "null" in a Kotlin optString, which is how a project with no end date
        ends up displaying one."""
        out = sitephoto.ensure_site("ZZ Nodate Client", "ZZ Nodate Project",
                                    site_name="ZZ Nodate Flat")
        row = next(p for p in sitephoto.bootstrap()["projects"]
                   if p["project"] == out["project"])
        for f in ("start", "end", "stage_since"):
            self.assertEqual(row[f], "", f"{f} should be empty, got {row[f]!r}")

    # ---- a flat photograph is a capture too ------------------------------

    def test_a_flat_photo_is_a_first_class_capture(self):
        """A repair job is a close-up of one broken hinge.

        Forcing that through the equirect splitter is nonsense, and having no
        route for it at all is why people fall back to the phone's own camera
        app — and lose the client, site, room and stage along with the
        picture. It files exactly like a 360 and is born Split, because there
        is nothing to split and a queue of work that will never happen is a
        queue that stops meaning anything."""
        out = sitephoto.ensure_site("ZZ Photo Client", "ZZ Photo Project",
                                    site_name="ZZ Photo Flat")
        cap = sitephoto.create_capture(out["project"], _room(),
                                       capture_kind="Photo")
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual(doc.capture_kind, "Photo")
        # Pending until its image arrives, exactly like a 360: at creation
        # time nothing has been uploaded yet, and validate() says so.
        self.assertEqual(doc.status, "Pending")

        # Once the image lands it is FINISHED, not queued. Sending a flat
        # photograph to the equirect splitter would fail the 2:1 check and
        # park it at Failed, which is a lie about a perfectly good picture.
        doc.pano = "/files/zz-photo.jpg"
        doc.save(ignore_permissions=True)
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", doc.name, "status"), "Split")
        self.assertFalse(
            frappe.db.get_value("Site Photo 360", doc.name, "split_signature"),
            "a flat photo must never enter the splitter")

    def test_a_capture_with_no_kind_is_a_360(self):
        """Every capture made before the field existed was a 360, and every
        phone that has not updated still sends nothing. Both must land as
        360s rather than as a blank later code has to guess about."""
        out = sitephoto.ensure_site("ZZ Kind Client", "ZZ Kind Project",
                                    site_name="ZZ Kind Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", cap["name"], "capture_kind"),
            "360")

    def test_a_made_up_capture_kind_falls_back_rather_than_throwing(self):
        """The phone is the least trustworthy caller and the one that must
        never fail to file. An unknown word becomes a 360; refusing the
        capture would lose the photograph."""
        out = sitephoto.ensure_site("ZZ Kind X Client", "ZZ Kind X Project",
                                    site_name="ZZ Kind X Flat")
        cap = sitephoto.create_capture(out["project"], _room(),
                                       capture_kind="hologram")
        self.assertEqual(
            frappe.db.get_value("Site Photo 360", cap["name"], "capture_kind"),
            "360")

    def test_re_staging_a_photo_is_allowed_and_written_down(self):
        """Logged, not locked.

        An earlier build refused the change unless the caller held write on
        Project. Amit: "let user alter the stage as it can be by mistake" —
        and a hard block produces the worse failure, a photo permanently
        mis-staged because the only person who noticed cannot fix it. So the
        change goes through and the trail is permanent."""
        out = sitephoto.ensure_site("ZZ Log Client", "ZZ Log Project",
                                    site_name="ZZ Log Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        before = frappe.db.get_value("Site Photo 360", cap["name"], "work_stage")

        sitephoto.set_capture_tags(cap["name"],
                                   work_stage="Modular carpentry install")
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual(doc.work_stage, "Modular carpentry install")
        self.assertEqual(len(doc.stage_log), 1)
        row = doc.stage_log[0]
        self.assertEqual(row.stage, "Modular carpentry install")
        self.assertEqual(row.from_stage, before or "(none)")
        self.assertTrue(row.changed_by)
        self.assertTrue(row.changed_on)

    def test_re_staging_to_the_same_stage_writes_nothing(self):
        """An audit trail padded with non-events is one nobody reads."""
        out = sitephoto.ensure_site("ZZ Log Same Client", "ZZ Log Same Project",
                                    site_name="ZZ Log Same Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        sitephoto.set_capture_tags(cap["name"], work_stage="Defect recorded")
        sitephoto.set_capture_tags(cap["name"], work_stage="Defect recorded")
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual(len(doc.stage_log), 1)

    def test_every_move_is_kept_not_just_the_last(self):
        """Two corrections leave two rows. The question the trail answers is
        'what has this photograph been called', not 'what was it called
        once'."""
        out = sitephoto.ensure_site("ZZ Log Two Client", "ZZ Log Two Project",
                                    site_name="ZZ Log Two Flat")
        cap = sitephoto.create_capture(out["project"], _room())
        sitephoto.set_capture_tags(cap["name"], work_stage="Defect recorded")
        sitephoto.set_capture_tags(cap["name"],
                                   work_stage="Modular carpentry install")
        doc = frappe.get_doc("Site Photo 360", cap["name"])
        self.assertEqual([r.stage for r in doc.stage_log],
                         ["Defect recorded", "Modular carpentry install"])

    def test_bootstrap_says_whether_this_user_may_re_stage(self):
        """Asked once, so the phone never offers a picker the bench will
        refuse. An affordance that fails on use is worse than one that was
        never offered."""
        self.assertTrue(sitephoto.bootstrap()["can_restage"],
                        "Administrator can write a capture")

    # ---- work the SITE says is needed ------------------------------------

    def test_the_site_can_record_work_before_anyone_leaves(self):
        """Amit: "idea is to have a SKU / service discussed quickly with client
        when measuring the site so that what high level required is captured."

        Not the office transcribing an estimate — the person standing in the
        room saying this wall needs POP, 120 sqft."""
        out = sitephoto.ensure_site("ZZ Sku Site Client", "ZZ Sku Site Project",
                                    site_name="ZZ Sku Site Flat")
        made = sitephoto.create_sku(out["project"], _room(), "POP", qty=120)
        self.assertFalse(made["already"])
        self.assertEqual(made["basis"], "Sqft")
        self.assertEqual(made["kind"], "Subcontract")
        doc = frappe.get_doc("Estimate SKU", made["name"])
        self.assertEqual(doc.site_qty, 120)
        self.assertEqual(doc.mallet_article, "POP")

    def test_the_code_is_generated_not_typed(self):
        """Two people describing the same wall have to produce the same code,
        or the grammar is decorative."""
        out = sitephoto.ensure_site("Yogesh Sahasrabudhe", "ZZ Code Project",
                                    site_name="ZZ Code Flat")
        made = sitephoto.create_sku(out["project"], _room(), "WAR")
        self.assertTrue(made["code"].startswith("YS_"),
                        f"customer initials missing: {made['code']}")
        self.assertTrue(made["code"].endswith("_WAR"),
                        f"article token missing: {made['code']}")

    def test_the_same_tap_arriving_twice_makes_one_sku(self):
        """A queue retrying after a dropped acknowledgement must not leave the
        project carrying the same wardrobe three times.

        Keyed on the DEVICE ID, not the code — the same distinction
        create_capture makes."""
        out = sitephoto.ensure_site("ZZ Twice Client", "ZZ Twice Project",
                                    site_name="ZZ Twice Flat")
        a = sitephoto.create_sku(out["project"], _room(), "WAR",
                                 device_sku_id="msku-aaaabbbbcccc")
        b = sitephoto.create_sku(out["project"], _room(), "WAR",
                                 device_sku_id="msku-aaaabbbbcccc")
        self.assertTrue(b["already"])
        self.assertEqual(a["name"], b["name"])

    def test_two_real_wardrobes_in_one_room_are_two_skus(self):
        """And this is why idempotency cannot key on the code. Estimate SKU
        supports two wardrobes in one master bedroom deliberately — they
        compute the same code and the second takes a numeric suffix. If
        "already recorded" meant "a wardrobe exists in this room", the second
        one could never be added at all."""
        out = sitephoto.ensure_site("ZZ Pair Client", "ZZ Pair Project",
                                    site_name="ZZ Pair Flat")
        a = sitephoto.create_sku(out["project"], _room(), "WAR",
                                 device_sku_id="msku-111111111111")
        b = sitephoto.create_sku(out["project"], _room(), "WAR",
                                 device_sku_id="msku-222222222222")
        self.assertFalse(b["already"])
        self.assertNotEqual(a["name"], b["name"])
        self.assertNotEqual(a["code"], b["code"],
                            "the second must take its own code, not share one")

    def test_a_made_up_device_sku_id_is_refused(self):
        """The id is minted by the app. Anything else is a bug or a probe, and
        accepting it would let one caller overwrite another's idempotency."""
        out = sitephoto.ensure_site("ZZ Badid Client", "ZZ Badid Project",
                                    site_name="ZZ Badid Flat")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.create_sku(out["project"], _room(), "WAR",
                                 device_sku_id="../../etc")

    def test_an_unknown_article_is_refused(self):
        """The picker offers a master. Anything else arriving here is a bug or
        a stale phone, and inventing an article to accept it would put work on
        a project that nobody can price."""
        out = sitephoto.ensure_site("ZZ Bad Art Client", "ZZ Bad Art Project",
                                    site_name="ZZ Bad Art Flat")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.create_sku(out["project"], _room(), "ZZZ")

    def test_a_built_article_carries_dimensions_and_a_service_carries_area(self):
        """A wardrobe has three dimensions; POP has an area and no shape."""
        out = sitephoto.ensure_site("ZZ Dim Client", "ZZ Dim Project",
                                    site_name="ZZ Dim Flat")
        war = sitephoto.create_sku(out["project"], _room(), "WAR",
                                   width_mm=2400, height_mm=2100, depth_mm=600)
        w = frappe.get_doc("Estimate SKU", war["name"])
        self.assertEqual((w.site_width_mm, w.site_height_mm, w.site_depth_mm),
                         (2400, 2100, 600))

        pop = sitephoto.create_sku(out["project"], _room(), "POP", qty=95.5)
        p = frappe.get_doc("Estimate SKU", pop["name"])
        self.assertEqual(p.site_qty, 95.5)
        self.assertFalse(p.site_width_mm, "an area has no width")

    def test_the_article_master_ships_kind_and_basis_to_the_phone(self):
        """The phone asks for 'sqft' rather than a bare number, which it can
        only do if the unit rides with the master."""
        rows = {r["code"]: r for r in sitephoto.article_master()}
        self.assertEqual(rows["ELP"]["basis"], "Point")
        self.assertEqual(rows["ELW"]["basis"], "Rft")
        self.assertEqual(rows["ELF"]["basis"], "Nos")
        self.assertEqual(rows["POP"]["kind"], "Subcontract")
        self.assertEqual(rows["WAR"]["kind"], "Build")


class TestSiteTypeAndAddress(MalletTestCase):
    """Amit, 2026-08-22: "site should be selectable like flat, bunglow, shop
    etc, address should be one more separate field where address of taht site
    will be keyed in."

    The type already existed on the doctype and in the app's data model — what
    was missing was any way to CHOOSE it, and any field to type an address
    into. The old `address` was a Link to ERPNext's Address doctype, which
    nothing on a phone can create."""

    def _customer(self):
        name = "TypeAddr Client"
        if not frappe.db.exists("Customer", {"customer_name": name}):
            frappe.get_doc({"doctype": "Customer", "customer_name": name,
                            "customer_type": "Individual"}).insert(ignore_permissions=True)
        return frappe.db.get_value("Customer", {"customer_name": name}, "name")

    def test_the_type_list_comes_off_the_doctype(self):
        """The app renders these as chips and must not hold its own copy: a
        type added to the Select and not to the phone is one nobody can pick,
        and a phone with its own list goes on offering a retired one."""
        got = worksite.site_types()
        self.assertIn("Flat", got)
        self.assertIn("Shop", got)
        options = frappe.get_meta("Mallet Site").get_field("site_type").options
        self.assertEqual(got, [o for o in options.split("\n") if o.strip()])

    def test_a_new_site_keeps_the_type_and_address_it_was_given(self):
        c = self._customer()
        site = worksite.ensure_site(c, "Kothrud Duplex", "Bungalow",
                                    site_address="12 Paud Road, Kothrud, Pune")
        doc = frappe.get_doc("Mallet Site", site)
        self.assertEqual(doc.site_type, "Bungalow")
        self.assertEqual(doc.site_address, "12 Paud Road, Kothrud, Pune")

    def test_sync_fills_a_blank_but_never_overwrites(self):
        """The phone carries what it was told; the office's record wins. A
        capture syncing an hour later must not revert a desk correction."""
        c = self._customer()
        site = worksite.ensure_site(c, "Baner Flat", "Flat")
        frappe.db.set_value("Mallet Site", site, "site_address", "Corrected at the desk")
        again = worksite.ensure_site(c, "Baner Flat", "Shop",
                                     site_address="typed on the phone")
        self.assertEqual(again, site, "a second visit minted a second site")
        doc = frappe.get_doc("Mallet Site", site)
        self.assertEqual(doc.site_address, "Corrected at the desk")
        self.assertEqual(doc.site_type, "Flat", "an existing type was overwritten")

    def test_a_blank_address_is_filled_by_the_phone(self):
        """The other half of the rule: the site is often where the address is
        first known, and refusing to record it sends someone to the desk to
        retype what they already typed standing in the doorway."""
        c = self._customer()
        site = worksite.ensure_site(c, "Blank Addr Flat", "Flat")
        worksite.ensure_site(c, "Blank Addr Flat", site_address="9 Fill Me Road")
        self.assertEqual(
            frappe.db.get_value("Mallet Site", site, "site_address"),
            "9 Fill Me Road")

    def test_an_edit_from_the_phone_does_overwrite(self):
        """set_site_details is a deliberate edit, not a guess riding along on
        a capture, so it replaces what is there."""
        c = self._customer()
        site = worksite.ensure_site(c, "Wakad Shop", "Flat",
                                    site_address="wrong address")
        out = sitephoto.set_site_details(site, site_type="Shop",
                                         site_address="Shop 4, Wakad")
        self.assertEqual(out["site_type"], "Shop")
        self.assertEqual(out["site_address"], "Shop 4, Wakad")

    def test_an_unknown_type_is_refused(self):
        c = self._customer()
        site = worksite.ensure_site(c, "Hinjewadi Flat", "Flat")
        with self.assertRaises(frappe.ValidationError):
            sitephoto.set_site_details(site, site_type="Houseboat")

    def test_the_masters_payload_offers_the_types_and_the_address(self):
        c = self._customer()
        worksite.ensure_site(c, "Masters Flat", "Flat",
                             site_address="1 Test Lane")
        m = sitephoto.masters()
        self.assertIn("Flat", m["site_types"])
        row = next((s for s in m["sites"] if s.get("site_name") == "Masters Flat"), None)
        self.assertIsNotNone(row, "the site is missing from masters")
        self.assertEqual(row.get("site_address"), "1 Test Lane")
