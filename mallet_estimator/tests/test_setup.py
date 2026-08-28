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

    def test_the_manufacturing_standards_are_readable_but_not_writable(self):
        # 2026-08-24. Amit asked why the plugin and ERP disagreed about which
        # workstation an operation runs at, and the answer lived in the
        # Operation masters — which no assistant identity could read. The
        # question could only be answered by a human opening the desk, which
        # is the failure this role exists to prevent.
        #
        # A standard time and a workstation are not money, but they DECIDE
        # money: the workstation carries the hourly rate. Reading them is what
        # makes "this step is priced at the wrong station" a sentence anybody
        # can say from outside.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        for dt in ("Operation", "Workstation"):
            self.assertIn(dt, integration.READONLY_DOCTYPES, dt)
            perm = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.READONLY_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            self.assertTrue(perm and perm.read, f"{dt} should be readable")
            for p in ("write", "create", "delete"):
                self.assertFalse(perm.get(p), f"{dt} must never be {p}-able")
        # The standards decide prices, so the steward must not be able to move
        # one any more than it can move a rate.
        for dt in ("Operation", "Workstation"):
            self.assertNotIn(dt, integration.STEWARD_RWC_DOCTYPES, dt)

    def test_the_company_config_is_readable_but_never_writable(self):
        # Amit, 2026-08-25: "config only, no HR". A go-live is mostly checking
        # that configuration is right, and this morning's audit could not read
        # the Company record at all — it inferred the abbreviation from a
        # warehouse name inside an item default and happened to be correct.
        from mallet_estimator import integration
        integration.ensure_readonly_role()
        config = ("Company", "Fiscal Year", "Account", "Cost Center",
                  "Warehouse", "Supplier", "Supplier Group")
        for dt in config:
            self.assertIn(dt, integration.READONLY_DOCTYPES, dt)
            perm = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.READONLY_ROLE, "parent": dt},
                ["read", "write", "create", "delete"], as_dict=True)
            self.assertTrue(perm and perm.read, f"{dt} should be readable")
            for p in ("write", "create", "delete"):
                self.assertFalse(perm.get(p), f"{dt} must never be {p}-able")

        # AND HR STAYS OUT. This is the half worth a test: the list grew twice
        # in two days, and the next widening is the one that quietly takes a
        # date of birth with it.
        for dt in ("Employee", "Salary Slip", "Salary Structure", "Attendance",
                   "Employee Checkin"):
            self.assertNotIn(dt, integration.READONLY_DOCTYPES, dt)
            self.assertNotIn(dt, integration.STEWARD_RWC_DOCTYPES, dt)
            self.assertNotIn(dt, integration.STEWARD_RW_DOCTYPES, dt)

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

    def test_the_steward_can_remove_a_customer_but_nothing_else_it_could_not(self):
        # Amit, 2026-08-24, asked to remove a throwaway probe customer and told
        # me to do it rather than do it himself: "1 - delete yourself." The
        # steward could not — Customer was read/write only, so the identity
        # that exists to clean up operational debris could create one and
        # correct one but never remove one.
        #
        # The real guard is Frappe's link check, not this grant: a Customer
        # reached by a Quotation, an Invoice, a Project, a Site or an SKU
        # cannot be deleted at all. So what is asserted here is the narrowness
        # of the widening — Customer gained delete, and nothing else did.
        from mallet_estimator import integration
        integration.ensure_steward_role()
        self.assertIn("Customer", integration.STEWARD_RWD_DOCTYPES)
        self.assertNotIn("Customer", integration.STEWARD_RW_DOCTYPES,
                         "Customer must be in exactly one grant list")

        perm = frappe.db.get_value(
            "Custom DocPerm", {"role": integration.STEWARD_ROLE, "parent": "Customer"},
            ["read", "write", "create", "delete", "submit", "cancel", "amend"],
            as_dict=True)
        self.assertTrue(perm and perm.delete, "the steward still cannot delete a Customer")
        # Customer is not submittable, so these would be noise dressed as power.
        for p in ("submit", "cancel", "amend"):
            self.assertFalse(perm.get(p), f"Customer must never be {p}-able")

        # The masters that carry history retire by `disabled`, never deletion,
        # and this must not have quietly swept them along.
        for dt in ("Item", "Manufacturer", "Project", "Mallet Site"):
            row = frappe.db.get_value(
                "Custom DocPerm", {"role": integration.STEWARD_ROLE, "parent": dt},
                ["delete"], as_dict=True)
            self.assertFalse(row and row.delete,
                             f"{dt} gained delete along with Customer")
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

    def test_the_photo_browser_exists_and_reads_client_site_project_room(self):
        # "Easy folder structure like ImageMeter" — the tree is built from
        # captures that exist, so a room nobody photographed is not a folder.
        # Four levels since 2026-08-21: a client owns several places, and a
        # project belongs to a building rather than to a person.
        self.assertTrue(frappe.db.exists("Page", "site-photo-browser"))
        from mallet_estimator import sitephoto
        t = sitephoto.tree()
        self.assertIn("clients", t)
        for c in t["clients"]:
            self.assertIn("sites", c)
            self.assertEqual(c["captures"], sum(s["captures"] for s in c["sites"]))
            for st in c["sites"]:
                self.assertIn("projects", st)
                self.assertEqual(st["captures"],
                                 sum(p["captures"] for p in st["projects"]))
                for p in st["projects"]:
                    self.assertIn("rooms", p)
                    self.assertEqual(p["captures"],
                                     sum(r["captures"] for r in p["rooms"]))
                    for r in p["rooms"]:
                        self.assertGreater(r["captures"], 0,
                                           "an empty room is not a folder")

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

    def test_the_settings_page_publishes_the_live_masters_and_the_rule(self):
        """Amit, 2026-08-24: the Workstation Cost Calculator "should show live
        figures from the actual erp workstation operation side", the 17
        operations should be published "so its easy for user to understand how
        this labor estimation works", and the SKU rule should be on the page.

        Asserted on the payload the page renders from, because a table that
        silently comes back empty looks exactly like a table with nothing to
        say — and the settings page is where somebody goes to find out why a
        number is what it is.
        """
        from mallet_estimator.mallet_estimator.doctype.estimate_settings import (
            estimate_settings as es)
        from mallet_estimator import estimator

        out = es.cost_calculator()
        live = out.get("live") or {}
        self.assertNotIn("error", live, live.get("error"))

        # Live workstation rates, read the same way the plugin reads them.
        self.assertTrue(live.get("workstations"), "no live workstations")
        names = {w["name"] for w in live["workstations"]}
        for w in estimator.WORKSTATIONS:
            self.assertIn(w["name"], names, w["name"])

        # AND THEIR COMPONENTS. Amit, 2026-08-25: "the page is supposed to
        # display all cost components from live erp ... so that i don't need to
        # go to every workstation / operation one by one." live_workstation_rates
        # folds the child table into five totals for the costing maths and used
        # to throw the rows away, so the page had a net figure and nothing to
        # explain it with.
        self.assertTrue(live.get("components"), "no component column order sent")
        for c in estimator.WS_COMPONENTS:
            self.assertIn(c, live["components"], c)
        for w in live["workstations"]:
            self.assertIn("rate_source", w, w["name"])
            comps = w.get("components") or []
            if comps:
                self.assertAlmostEqual(
                    sum(v for _c, v in comps), w["hour_rate"], 2,
                    "%s: components must add up to the rate charged" % w["name"])

        # A workstation that CARRIES cost rows must publish them. Asserted by
        # creating one rather than by looking for one, because on CI every
        # workstation rate is zero by design and no station has cost rows at
        # all — the first version of this test asserted the bench's data and
        # could only ever pass on a live site.
        ws = frappe.get_doc({
            "doctype": "Workstation", "workstation_name": "ZZ Component Probe",
        }).insert(ignore_permissions=True)
        try:
            ws.append("workstation_costs",
                      {"operating_component": "Rent", "operating_cost": 11})
            ws.append("workstation_costs",
                      {"operating_component": "Electricity", "operating_cost": 7})
            ws.save(ignore_permissions=True)

            probe = next(w for w in es.cost_calculator()["live"]["workstations"]
                         if w["name"] == ws.name)
            got = {c: v for c, v in probe["components"]}
            self.assertEqual(got.get("Rent"), 11)
            self.assertEqual(got.get("Electricity"), 7)
            self.assertAlmostEqual(probe["hour_rate"], 18, 2,
                                   "the net must be the sum of the rows shown")
            self.assertEqual(probe["rate_source"], "erp:Workstation")
        finally:
            frappe.delete_doc("Workstation", ws.name, ignore_permissions=True,
                              force=True)

        # All seventeen, each carrying what it costs and where it runs.
        ops = live.get("operations") or []
        self.assertGreaterEqual(len(ops), 17, "the 17 steps are not published")
        for o in ops:
            for key in ("seq", "name", "workstation", "min_per_unit", "qty_source"):
                self.assertIn(key, o, o.get("name"))

        # The hardware children, under their parent.
        hw = {h["kind"] for h in live.get("hardware") or []}
        self.assertEqual(hw, {k for k, _ in estimator.HARDWARE_INSTALL_TYPES})
        self.assertEqual(live.get("parent"), estimator.HARDWARE_PARENT)

        # And the rule itself, in words rather than only in code.
        rule = live.get("sku_rule") or {}
        self.assertTrue(rule.get("title"))
        self.assertGreaterEqual(len(rule.get("lines") or []), 4)
        self.assertTrue(any("MCFT_ASMBL_L" in l for l in rule["lines"]),
                        "the rule must name the convention it is a rule about")


class TestNewMaterialGetsAHome(MalletTestCase):
    """Where a material created from the plugin defaults to in stock.

    Amit, 2026-08-29, asked while adding a "create in ERP" button: "as its
    estimate, what would be location in stock for it to be created?" The
    literal answer is that estimation needs none — a warehouse matters the day
    stock moves. He chose to set one anyway, and it is the better call: it
    pre-fills the picker with the place the thing really goes, so whoever
    receives boards is not choosing from thirteen warehouses under pressure.
    """

    def test_each_family_lands_where_that_material_actually_lives(self):
        _ensure_company()
        inventory.ensure_warehouses()
        want = {
            "sheet": "Board & Sheet Store",
            "laminate": "Board & Sheet Store",
            "edge": "Board & Sheet Store",
            "hardware": "Hardware Store",
            "joinery": "Hardware Store",
        }
        for kind, store in want.items():
            got = inventory.default_warehouse_for(kind)
            self.assertTrue(got, "%s has no default warehouse" % kind)
            self.assertTrue(got.startswith(store),
                            "%s -> %s, expected %s" % (kind, got, store))

    def test_every_material_family_is_mapped(self):
        # A family missing from the map gets no default and nobody notices
        # until a receipt. The Item Group map is the authority on what
        # families exist, so the two are compared rather than the warehouse
        # map being trusted on its own.
        self.assertEqual(set(inventory.KIND_WAREHOUSE),
                         set(inventory.KIND_SPEC))

    def test_a_warehouse_that_does_not_exist_is_not_guessed(self):
        # Returning a name nobody created would push the error to a receipt,
        # in front of somebody holding a delivery note. None is the honest
        # answer, and ERPNext simply asks for the warehouse at that point.
        saved = dict(inventory.KIND_WAREHOUSE)
        try:
            inventory.KIND_WAREHOUSE["sheet"] = "ZZ No Such Store"
            self.assertIsNone(inventory.default_warehouse_for("sheet"))
        finally:
            inventory.KIND_WAREHOUSE.clear()
            inventory.KIND_WAREHOUSE.update(saved)

    def test_an_unknown_family_gets_no_default_rather_than_a_wrong_one(self):
        self.assertIsNone(inventory.default_warehouse_for("nonsense"))
