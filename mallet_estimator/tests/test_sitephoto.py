# The PWA's server contract. The phone never holds an API key — it calls these
# as the logged-in user — so what is asserted here is the SHAPE the app binds
# to plus the guard rails (real masters only, faces validated, annotations
# additive).
import frappe

from mallet_estimator import install, panorama, sitephoto

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
