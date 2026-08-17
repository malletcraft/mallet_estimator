# Config health-check tests — assert the app's masters get created and that
# verify_setup() reflects reality. Run under `bench run-tests`.
import frappe

from mallet_estimator import install, inventory

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


def _ensure_company():
    name = frappe.db.get_value("Company", {}, "name")
    if name:
        return name
    co = frappe.get_doc({
        "doctype": "Company", "company_name": "Mallet Test Co",
        "abbr": "MTC", "default_currency": "INR", "country": "India",
    }).insert(ignore_permissions=True)
    return co.name


class TestMasters(MalletTestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        install.seed_settings()
        inventory.ensure_inventory_masters()
        install.ensure_project_customization()
        install.ensure_gst_masters()
        install.ensure_manufacturing_masters()
        inventory.ensure_warehouses(_ensure_company())

    def test_item_groups(self):
        for g in [inventory.PARENT_GROUP, inventory.CLIENT_SKU_GROUP] + inventory.ITEM_GROUPS:
            self.assertTrue(frappe.db.exists("Item Group", g), f"missing Item Group {g}")

    def test_uoms(self):
        for u in ["Sheet", "Meter", "Roll", "Square Meter"]:
            self.assertTrue(frappe.db.exists("UOM", u), f"missing UOM {u}")

    def test_item_custom_fields(self):
        meta = frappe.get_meta("Item")
        for f in install.ITEM_CUSTOM_FIELDS:
            self.assertTrue(meta.has_field(f), f"missing Item field {f}")

    def test_workstations(self):
        from mallet_estimator.estimator import WORKSTATIONS
        for w in WORKSTATIONS:
            self.assertTrue(frappe.db.exists("Workstation", w["name"]), f"missing {w['name']}")

    def test_warehouses(self):
        for w in install.WAREHOUSE_LEAVES:
            self.assertTrue(frappe.db.exists("Warehouse", {"warehouse_name": w}), f"missing Warehouse {w}")

    def test_verify_setup_all_ok(self):
        report = install.verify_setup()
        self.assertTrue(report["all_ok"], f"verify_setup failed: {report['failed']}")

    def test_readonly_role_can_only_read(self):
        # The role's guarantee is now exactly one thing: it cannot write.
        # Reading the cost doctypes is allowed on an explicit decision
        # (2026-08-09), so the assertion that matters is that granting that
        # read did not quietly bring write with it — an ERPNext upgrade
        # shipping different permission defaults would widen it silently.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        ok, detail = integration.role_is_read_only()
        self.assertTrue(ok, detail)

    def test_the_cost_doctypes_are_readable_but_not_writable(self):
        # Being able to see a rate is what lets a reader say WHY a number is
        # wrong instead of only that it looks odd. Being able to change one
        # is never part of that.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        for dt in integration.COST_DOCTYPES:
            self.assertIn(dt, integration.READONLY_DOCTYPES, dt)
            perm = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.READONLY_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            self.assertTrue(perm and perm.read, f"{dt} should be readable")
            for p in ("write", "create", "delete"):
                self.assertFalse(perm.get(p), f"{dt} must never be {p}-able")

    def test_the_desk_ui_doctypes_are_readable_but_not_writable(self):
        # Page/Workspace read is what lets the reader RENDER a desk form —
        # see the layout a user sees — while staying strictly read-only.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        for dt in integration.READONLY_UI_DOCTYPES:
            perm = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.READONLY_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            self.assertTrue(perm and perm.read, f"{dt} should be readable")
            for p in ("write", "create", "delete"):
                self.assertFalse(perm.get(p), f"{dt} must never be {p}-able")

    def test_the_steward_writes_data_but_never_money(self):
        # Data fixes bypass CI/CD through the steward — which is exactly why
        # its money exclusion must be asserted, not implied.
        from mallet_estimator import integration
        integration.ensure_steward_role()
        ok, detail = integration.steward_is_rate_safe()
        self.assertTrue(ok, detail)
        perm = frappe.db.get_value(
            "Custom DocPerm",
            {"role": integration.STEWARD_ROLE, "parent": "Estimate SKU"},
            ["read", "write", "create"], as_dict=True)
        self.assertTrue(perm and perm.read and perm.write and perm.create,
                        "the steward must be able to FIX an Estimate SKU")

    def test_the_plugin_role_reads_projects_and_creates_skus(self):
        # 2026-08-15: the binding picker 403'd in the field because the role
        # missed its new Project grant on a migrate. Assert the grants the
        # plugin contract depends on, from the live perm rows.
        from mallet_estimator import integration
        integration.ensure_plugin_role()
        for dt, need, forbid in (("Project", ("read",), ("write", "create", "delete")),
                                 ("Customer", ("read",), ("write", "create", "delete")),
                                 ("Estimate SKU", ("read", "write", "create"), ("delete",))):
            perm = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.PLUGIN_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            self.assertTrue(perm, f"{dt} has no perm row for the plugin role")
            for p in need:
                self.assertTrue(perm.get(p), f"plugin must {p} {dt}")
            for p in forbid:
                self.assertFalse(perm.get(p), f"plugin must never {p} {dt}")

    def test_ensure_readonly_role_is_idempotent(self):
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        before = frappe.db.count("Custom DocPerm", {"role": integration.READONLY_ROLE})
        integration.ensure_readonly_role()
        self.assertEqual(
            frappe.db.count("Custom DocPerm", {"role": integration.READONLY_ROLE}), before)

    def test_batch_tier_masters(self):
        self.assertTrue(frappe.db.exists("DocType", "Mallet Operation Batch Tier"))
        self.assertTrue(frappe.get_meta("Operation").has_field("mallet_batch_tiers"),
                        "missing Operation.mallet_batch_tiers")

    def test_ensure_warehouses_idempotent(self):
        before = frappe.db.count("Warehouse")
        inventory.ensure_warehouses(_ensure_company())
        self.assertEqual(frappe.db.count("Warehouse"), before)

    def test_strip_invalid_workstation_costs(self):
        # B1: a stale 'Machinery' cost row (component removed) is dropped so the
        # workstation re-save no longer fails link validation.
        ws = frappe.new_doc("Workstation")
        ws.workstation_name = "ZZ Strip Test"
        ws.append("workstation_costs", {"operating_component": "Rent", "operating_cost": 10})
        ws.append("workstation_costs", {"operating_component": "Machinery", "operating_cost": 5})
        self.assertTrue(install._strip_invalid_costs(ws))
        comps = {r.operating_component for r in ws.workstation_costs}
        self.assertNotIn("Machinery", comps)
        self.assertIn("Rent", comps)
        self.assertFalse(install._strip_invalid_costs(ws))  # idempotent — nothing left to strip

    def test_single_sku_grid_and_decor_masters(self):
        self.assertTrue(frappe.db.exists("DocType", "Mallet Decor"))
        # The SKUs grid is the ONE table on the estimate: select-or-create in
        # its link column, this SKU's input files in the same row, its numbers
        # beside them. If any of these columns goes missing the estimate screen
        # silently loses the intake it replaced.
        row = frappe.get_meta("Execution Estimate SKU")
        for f in ("estimate_sku", "parts_csv", "views_pdf",
                  "sheets", "client_total", "est_days"):
            self.assertTrue(row.has_field(f), f"missing Execution Estimate SKU.{f}")
        est = frappe.get_meta("Estimate")
        for f in ("sku_materials_html", "sku_summary_html"):
            self.assertTrue(est.has_field(f), f"missing Estimate.{f}")
        # The kind of work is CHOSEN on the estimate and gates which SKUs may
        # join it. Losing this field silently re-allows the mixed total it
        # exists to prevent, so it is pinned with its option list.
        self.assertTrue(est.has_field("work_type"), "missing Estimate.work_type")
        opts = set(filter(None, est.get_field("work_type").options.split("\n")))
        self.assertEqual(opts, {"New Work", "Repair", "Supply & Install"})
        self.assertNotIn("Mixed", opts, "an estimate is never more than one kind")
        # Creating a SKU straight from the grid's link column needs quick entry.
        self.assertTrue(frappe.get_meta("Estimate SKU").quick_entry,
                        "Estimate SKU must allow quick entry (create from the grid)")
        # Repair work (R1): its own activity table and the two policy numbers.
        self.assertTrue(frappe.db.exists("DocType", "Estimate Repair Activity"))
        for f in ("work_type", "repair_activities", "repair_csv", "repair_visits",
                  "client_repair"):
            self.assertTrue(frappe.get_meta("Estimate SKU").has_field(f),
                            f"missing Estimate SKU.{f}")
        for f in ("work_scope", "total_new_work", "total_repair"):
            self.assertTrue(est.has_field(f), f"missing Estimate.{f}")
        for f in ("supplier", "lead_time_weeks", "warranty_note"):
            self.assertTrue(frappe.get_meta("Estimate SKU").has_field(f),
                            f"missing Estimate SKU.{f}")
        self.assertIn("Supply & Install",
                      frappe.get_meta("Estimate SKU").get_field("work_type").options,
                      "Supply & Install must be a work type")
        for f in ("repair_visit_charge", "markup_repair", "markup_bought_out"):
            self.assertTrue(frappe.get_meta("Estimate Settings").has_field(f),
                            f"missing Estimate Settings.{f}")
        for t in ("Estimate SKU Decor", "Estimate SKU Decor Edge"):
            self.assertTrue(frappe.get_meta(t).has_field("decor"),
                            f"missing {t}.decor link")

    def test_site_photo_360_masters(self):
        # One doc per capture is the versioning model — the doctype and its
        # face/annotation fields are the contract the splitter writes into.
        self.assertTrue(frappe.db.exists("DocType", "Site Photo 360"))
        self.assertTrue(frappe.db.exists("DocType", "Site Photo Annotation"))
        meta = frappe.get_meta("Site Photo 360")
        for f in ("project", "room", "capture_date", "stage", "fov", "face_px",
                  "status", "pano", "split_signature", "annotations",
                  "face_front", "face_right", "face_back", "face_left",
                  "face_up", "face_down"):
            self.assertTrue(meta.has_field(f), f"missing Site Photo 360.{f}")
        self.assertEqual(meta.get_field("fov").default, "110")
        # The steward fixes photo records (wrong room, wrong date) like any
        # other operational data; the readonly identity can inspect them.
        from mallet_estimator import integration
        self.assertIn("Site Photo 360", integration.STEWARD_RWC_DOCTYPES)
        self.assertIn("Site Photo 360", integration.READONLY_DOCTYPES)
        # The steward runs the Drive sync, so it must be able to configure it
        # and clear its inbox — neither of which is money.
        for dt in ("Site Photo Settings", "Site Photo Inbox"):
            self.assertIn(dt, integration.STEWARD_RWC_DOCTYPES, dt)
            self.assertIn(dt, integration.READONLY_DOCTYPES, dt)
        # and the money exclusion is untouched by that widening
        ok, detail = integration.steward_is_rate_safe()
        self.assertTrue(ok, detail)

    def test_the_site_photo_screens_are_reachable_from_the_workspace(self):
        # A settings page only reachable by URL is a settings page nobody uses.
        ws = frappe.get_doc("Workspace", "Mallet Estimator")
        links = {l.label for l in ws.links}
        for label in ("Site Photo 360", "Site Photo Settings", "Site Photo Inbox"):
            self.assertIn(label, links, f"{label} missing from the workspace")
        # and they are their own section, not buried among the estimating tools
        breaks = [l.label for l in ws.links if l.type == "Card Break"]
        self.assertIn("Site Photos", breaks, "Site Photos needs its own card")
        # the capture app gets a shortcut tile — the most direct route there is
        shortcuts = {s.label: s for s in (ws.shortcuts or [])}
        self.assertIn("Capture a 360", shortcuts)
        self.assertIn("All Captures", shortcuts)
        self.assertEqual(shortcuts["Capture a 360"].type, "URL")
        self.assertEqual(shortcuts["Capture a 360"].url, "/sitephoto")

    def test_the_capture_form_shows_its_photos_inline(self):
        # Before the gallery, looking at a photo meant opening an attachment in
        # a new tab, and the annotations sat unread in a collapsed table.
        meta = frappe.get_meta("Site Photo 360")
        self.assertTrue(meta.has_field("gallery_html"), "no gallery on the form")
        self.assertEqual(meta.get_field("gallery_html").fieldtype, "HTML")

    def test_the_service_worker_is_served_from_the_scope_it_controls(self):
        # frappe refuses .js/.json from www/, and a worker served from /assets
        # would be scoped to /assets — no offline shell, no installable app.
        # A page renderer answers /sitephoto/sw.js instead.
        import os
        from mallet_estimator import sitephoto_assets as A
        base = frappe.get_app_path("mallet_estimator")
        for rel in ("public/sitephoto/sw.js", "public/sitephoto/manifest.json"):
            self.assertTrue(os.path.exists(os.path.join(base, rel)), rel)
        for route in ("sitephoto/sw.js", "sitephoto/manifest.json"):
            r = A.SitePhotoAssetRenderer(path=route)
            self.assertTrue(r.can_render(), route)
        self.assertFalse(A.SitePhotoAssetRenderer(path="sitephoto").can_render())
        self.assertFalse(A.SitePhotoAssetRenderer(path="something/else.js").can_render())

    def test_asset_links_publish_nothing_until_configured(self):
        # A wrong assetlinks file is cached by Google AND the device, so it is
        # slower to undo than a missing one. Nothing is served until both the
        # package and a fingerprint exist.
        from mallet_estimator import sitephoto_assets as A
        s = frappe.get_single("Site Photo Settings")
        s.twa_package = ""
        s.twa_fingerprints = ""
        s.save(ignore_permissions=True)
        frappe.clear_cache()
        self.assertEqual(A._assetlinks(), [])
        self.assertFalse(A.SitePhotoAssetRenderer(path=".well-known/assetlinks.json").can_render())

        s.twa_package = "com.malletcrafts.sitephotos"
        s.twa_fingerprints = "AA:BB:CC\n dd:ee:ff "
        s.save(ignore_permissions=True)
        frappe.clear_cache()
        links = A._assetlinks()
        self.assertEqual(len(links), 1)
        t = links[0]["target"]
        self.assertEqual(t["package_name"], "com.malletcrafts.sitephotos")
        # both certificates matter: Play re-signs, so the upload key alone
        # leaves the shipped app showing a browser URL bar
        self.assertEqual(t["sha256_cert_fingerprints"], ["AA:BB:CC", "DD:EE:FF"])
        self.assertIn("delegate_permission/common.handle_all_urls", links[0]["relation"])
        self.assertTrue(A.SitePhotoAssetRenderer(path=".well-known/assetlinks.json").can_render())

    def test_the_photo_browser_exists_and_reads_client_project_room(self):
        # "Easy folder structure like ImageMeter" — the tree is built from
        # captures that exist, so a room nobody photographed is not a folder.
        self.assertTrue(frappe.db.exists("Page", "site-photo-browser"))
        from mallet_estimator import sitephoto
        t = sitephoto.tree()
        self.assertIn("clients", t)
        for c in t["clients"]:
            self.assertIn("projects", c)
            for p in c["projects"]:
                self.assertIn("rooms", p)
                self.assertEqual(p["captures"], sum(r["captures"] for r in p["rooms"]))
                for r in p["rooms"]:
                    self.assertGreater(r["captures"], 0, "an empty room is not a folder")

    def test_a_site_photographer_gets_the_camera_and_nothing_else(self):
        # A technician with a phone needs real permissions, but handing them a
        # broad ERPNext role to get there would give them the cost screens too.
        from mallet_estimator import integration
        integration.ensure_photographer_role()
        ok, detail = integration.photographer_is_scoped()
        self.assertTrue(ok, detail)
        cap = frappe.db.get_value(
            "Custom DocPerm",
            {"role": integration.PHOTOGRAPHER_ROLE, "parent": "Site Photo 360"},
            ["read", "write", "create", "delete"], as_dict=True)
        self.assertTrue(cap and cap.read and cap.write and cap.create,
                        "a photographer must be able to make a capture")
        self.assertFalse(cap.delete, "and must not be able to delete one")
        for dt in ("Estimate Settings", "Item Price", "User", "Role"):
            perm = frappe.db.get_value(
                "Custom DocPerm",
                {"role": integration.PHOTOGRAPHER_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            if perm:
                for p in ("read", "write", "create", "delete"):
                    self.assertFalse(perm.get(p), f"photographer must not {p} {dt}")

    def test_the_camera_is_not_handed_to_every_login(self):
        # The site-photo doctypes shipped granting "All" read/write/create.
        # Every account carries "All", so the photographer role — whose entire
        # job is to be the thing that grants capture access — decided nothing:
        # a sales user or an accounts clerk had the camera and the review
        # inbox. Guest was still refused, so nothing was public; the hole was
        # internal, which is exactly the kind nobody trips over until it
        # matters.
        for dt in ("Site Photo 360", "Site Photo Inbox"):
            for table in ("Custom DocPerm", "DocPerm"):
                for row in frappe.get_all(
                        table, filters={"parent": dt, "role": "All"},
                        fields=["read", "write", "create", "delete"]):
                    for p in ("read", "write", "create", "delete"):
                        self.assertFalse(
                            row.get(p),
                            f"'All' must not {p} {dt} — that is the "
                            f"photographer role's job")

    def test_granting_the_camera_grants_only_the_camera(self):
        # The narrow alternative to giving the steward write on User. A role
        # that can edit users can grant itself any role, so that shortcut
        # would have quietly undone every exclusion the steward has.
        from mallet_estimator import integration
        email = "zz-grant-test@example.com"
        if not frappe.db.exists("User", email):
            frappe.get_doc({
                "doctype": "User", "email": email, "first_name": "ZZ Grant",
                "user_type": "System User", "send_welcome_email": 0,
            }).insert(ignore_permissions=True)

        out = integration.grant_photographer(email)
        self.assertTrue(out["granted"])
        roles = set(frappe.get_roles(email))
        self.assertIn(integration.PHOTOGRAPHER_ROLE, roles)
        for other in (integration.STEWARD_ROLE, integration.READONLY_ROLE,
                      integration.PLUGIN_ROLE, "System Manager"):
            self.assertNotIn(other, roles, "it must grant ONE role, not a set")
        self.assertTrue(out["access"]["can_capture"])
        for dt in ("Estimate Settings", "Supplier Rate Sheet", "Item Price"):
            self.assertEqual(out["access"]["access"].get(dt, ""), "")

        # Idempotent: running it again is a no-op, not a duplicate row.
        again = integration.grant_photographer(email)
        self.assertFalse(again["granted"])
        self.assertEqual(
            len([r for r in frappe.get_doc("User", email).roles
                 if r.role == integration.PHOTOGRAPHER_ROLE]), 1)

    def test_granting_refuses_to_invent_a_user(self):
        # Deciding somebody should have a login is not a decision this gets
        # to make — only that an existing person may hold the camera.
        from mallet_estimator import integration
        with self.assertRaises(frappe.ValidationError):
            integration.grant_photographer("nobody-at-all@example.com")

    def test_the_role_report_can_see_every_role(self):
        # role_report exists so a remote session can answer "did the grants
        # actually reach the database?" — and it listed three roles while a
        # fourth was live, so the honest answer was unobtainable. A report
        # that can silently omit a role reads as an all-clear.
        from mallet_estimator import integration
        integration.ensure_photographer_role()
        reported = set(integration.role_report())
        self.assertEqual(
            set(integration.INTEGRATION_ROLES) - reported, set(),
            "every integration role must appear in the report")
        self.assertIn(integration.PHOTOGRAPHER_ROLE, reported)

    def test_a_named_user_carrying_the_role_can_capture_and_nothing_more(self):
        # role_report says the grants landed on the ROLE. What nobody could
        # check after creating an account was whether they landed on the
        # PERSON — so this asks frappe the same question every real request
        # asks, with the user named explicitly.
        from mallet_estimator import integration
        integration.ensure_photographer_role()
        email = "zz-photographer-test@example.com"
        if not frappe.db.exists("User", email):
            frappe.get_doc({
                "doctype": "User", "email": email, "first_name": "ZZ Photographer",
                "user_type": "System User", "send_welcome_email": 0,
                "roles": [{"role": integration.PHOTOGRAPHER_ROLE}],
            }).insert(ignore_permissions=True)

        rep = integration.user_access_report(email)
        self.assertTrue(rep["exists"])
        self.assertIn(integration.PHOTOGRAPHER_ROLE, rep["roles"])
        self.assertTrue(rep["can_capture"], f"cannot make a capture: {rep['access']}")
        self.assertIn("r", rep["access"].get("Project", ""), "must see the project list")
        # The money doctypes are the point of the role. Asserted by name so a
        # future default that widens them fails here rather than on a phone.
        for dt in ("Estimate Settings", "Supplier Rate Sheet", "Item Price"):
            self.assertEqual(rep["access"].get(dt, ""), "",
                             f"a site photographer must not reach {dt}")

    def test_the_access_report_refuses_to_invent_a_user(self):
        from mallet_estimator import integration
        rep = integration.user_access_report("nobody-here@example.com")
        self.assertFalse(rep["exists"])
        self.assertNotIn("access", rep)

    def test_migrate_reapplies_the_readonly_role(self):
        # The doctype list is code, so widening it is a deploy — but nothing
        # re-applied it, and a live site's permissions stayed frozen at
        # whatever the button wrote the day it was pressed. Adding the cost
        # doctypes changed nothing on the real site, silently.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        frappe.db.delete("Custom DocPerm",
                         {"role": integration.READONLY_ROLE, "parent": "Estimate Settings"})
        self.assertFalse(frappe.db.exists(
            "Custom DocPerm",
            {"role": integration.READONLY_ROLE, "parent": "Estimate Settings"}))
        install.sync_readonly_role()
        self.assertTrue(frappe.db.exists(
            "Custom DocPerm",
            {"role": integration.READONLY_ROLE, "parent": "Estimate Settings"}),
            "migrate must bring a live site's role back in line with the code")
