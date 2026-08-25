import json
import os

import frappe

from mallet_estimator.estimator import (
    STEP_TEMPLATE, WORKSTATIONS, OPERATION_WORKSTATION, OPERATION_STANDARDS,
    ROUTING_NAME, WS_COMPONENTS, workstation_rates,
    DESIGN_STEP_TEMPLATE, DESIGN_STANDARDS,
)

# Legacy operating components (pre-OPS3 folded model) — still valid on a
# workstation until the rework patch rebuilds it; never auto-stripped.
LEGACY_WS_COMPONENTS = ("Wages", "Machinery")
from mallet_estimator import estimator
from mallet_estimator import inventory
from mallet_estimator import worksite
from mallet_estimator.inventory import (
    ensure_inventory_masters, ensure_warehouses, ensure_pricing_masters,
)

PRINT_FORMAT_NAME = "Mallet Client Estimate"
JOB_CARD_PRINT_FORMAT_NAME = "Mallet Job Card"
EXECUTION_PRINT_FORMAT_NAME = "Mallet Execution Estimate"
WORKSPACE_NAME = "Mallet Estimator"

# ERPNext auto-computes Operation.total_operation_time from sub-operations (so a
# value set directly is wiped). We keep the estimator's per-unit standard time in
# our own field on the Operation master — the single source of truth for step time.
OPERATION_CUSTOM_FIELDS = {
    "Operation": [
        {"fieldname": "mallet_min_per_unit", "fieldtype": "Float", "label": "Std Time (min/unit)",
         "insert_after": "total_operation_time",
         "description": "Standard minutes the workstation is occupied per unit — the estimator's per-step time."},
        {"fieldname": "mallet_batch_tiers", "fieldtype": "Table", "label": "Batch Efficiency Tiers",
         "options": "Mallet Operation Batch Tier", "insert_after": "mallet_min_per_unit",
         "description": "Bulk speed-ups: from this estimate-wide qty, minutes/unit scale by this "
                        "factor (e.g. 10 sheets → 0.85). Applied during CSV-Nest consolidation."},
    ]
}

# F4 — a per-Project map from abstract material code (a/b/c, generic hardware) to
# the client's actual chosen Item + vendor + negotiated rate. Table + Section Break
# add no column to the Project table, so this is light enough for after_migrate.
#
# S-level (2026-08-21) adds the site and the job type. ERPNext links a Project
# straight to a Customer, which was fine while one client meant one place —
# people own more than one flat, and a project belongs to a building rather
# than to a person. mallet_site sits BESIDE the customer link rather than
# replacing it, so everything that reads Project today keeps working.
PROJECT_CUSTOM_FIELDS = {
    "Project": [
        {"fieldname": "mallet_choices_section", "fieldtype": "Section Break",
         "label": "Mallet — Material Choices", "insert_after": "notes", "collapsible": 1},
        {"fieldname": "mallet_material_choices", "fieldtype": "Table",
         "label": "Material Choices", "options": "Project Material Choice",
         "insert_after": "mallet_choices_section",
         "description": "Abstract code → chosen Item + vendor + actual rate. 'Apply choices' pushes the actual to procurement; the estimate keeps valuing at the assumed rate."},

        {"fieldname": "mallet_site_section", "fieldtype": "Section Break",
         "label": "Mallet — Site & Stage", "insert_after": "mallet_material_choices",
         "collapsible": 0},
        {"fieldname": "mallet_site", "fieldtype": "Link", "label": "Site",
         "options": "Mallet Site", "insert_after": "mallet_site_section",
         "in_standard_filter": 1,
         "description": "Which of the client's places this job is at. Photos are filed Client → Site → Project → Room."},
        {"fieldname": "mallet_job_type", "fieldtype": "Select", "label": "Job Type",
         "options": "New work\nRepair\nSupply & install", "default": "New work",
         "insert_after": "mallet_site", "in_standard_filter": 1,
         "description": "Decides which work stages this project can reach, which articles its SKUs may use, and whether a cut list is expected at all."},
        {"fieldname": "mallet_stage_column", "fieldtype": "Column Break",
         "insert_after": "mallet_job_type"},
        {"fieldname": "mallet_stage", "fieldtype": "Link", "label": "Current Stage",
         "options": "Mallet Work Stage", "insert_after": "mallet_stage_column",
         "in_standard_filter": 1,
         "description": "Where the work has actually reached. New captures default to it, so nobody picks from 39 stages standing in a dusty flat."},
        {"fieldname": "mallet_stage_since", "fieldtype": "Date", "label": "At Stage Since",
         "read_only": 1, "insert_after": "mallet_stage"},
        {"fieldname": "mallet_stage_log", "fieldtype": "Table", "label": "Stage Log",
         "options": "Project Stage Log", "insert_after": "mallet_stage_since",
         "description": "Every move, with its date and who made it — the progress timeline nobody had to type."},
    ]
}


def ensure_project_customization():
    """F4 + S-level — the Project custom fields (material choices, site, job
    type, current stage and its log). Idempotent."""
    from frappe.custom.doctype.custom_field.custom_field import create_custom_fields
    create_custom_fields(PROJECT_CUSTOM_FIELDS, ignore_validate=True)
    frappe.db.commit()


GST_TEMPLATE_TITLE = "Mallet GST 18% (Intra-State)"
GST_SALES_TEMPLATE_TITLE = "Mallet Output GST 18% (Intra-State)"
ITEM_TAX_TEMPLATE = "GST 18%"


def ensure_gst_masters():
    """Native GST end-to-end (T2): tax ACCOUNTS (input + output CGST/SGST),
    an ITEM TAX TEMPLATE ('GST 18%') applied on every material Item Group (so
    each item inherits its rate on POs/invoices item-wise), and SALES +
    PURCHASE taxes templates for the document-level charge. Idempotent; skips
    cleanly when the chart of accounts has no Duties-and-Taxes group yet."""
    company = _company_for_tax()
    if not company or not frappe.db.exists("DocType", "Item Tax Template"):
        return {"skipped": "no company / doctype"}
    abbr = frappe.db.get_value("Company", company, "abbr")
    result = {"accounts": 0, "item_tax_template": 0, "groups": 0, "sales_template": 0, "errors": []}

    parent = frappe.db.get_value(
        "Account", {"company": company, "account_name": ["like", "%Duties and Taxes%"], "is_group": 1}, "name"
    )
    if not parent:
        result["errors"].append("no 'Duties and Taxes' group account — set up the chart of accounts first")
        return result

    def acc(name):
        existing = frappe.db.get_value(
            "Account", {"company": company, "account_name": name, "is_group": 0}, "name")
        if existing:
            return existing
        try:
            a = frappe.new_doc("Account")
            a.account_name = name
            a.parent_account = parent
            a.company = company
            a.account_type = "Tax"
            a.insert(ignore_permissions=True)
            result["accounts"] += 1
            return a.name
        except Exception as exc:
            result["errors"].append(f"Account {name}: {exc}")
            return None

    in_cgst, in_sgst = acc("Input Tax CGST"), acc("Input Tax SGST")
    out_cgst, out_sgst = acc("Output Tax CGST"), acc("Output Tax SGST")

    # Item Tax Template — matched item-wise on both cycles (input + output heads).
    if not frappe.db.exists("Item Tax Template", {"title": ITEM_TAX_TEMPLATE, "company": company}):
        try:
            t = frappe.new_doc("Item Tax Template")
            t.title = ITEM_TAX_TEMPLATE
            t.company = company
            for a in (in_cgst, in_sgst, out_cgst, out_sgst):
                if a:
                    t.append("taxes", {"tax_type": a, "tax_rate": 9})
            t.insert(ignore_permissions=True)
            result["item_tax_template"] = 1
        except Exception as exc:
            result["errors"].append(f"Item Tax Template: {exc}")
    itt = frappe.db.get_value("Item Tax Template", {"title": ITEM_TAX_TEMPLATE, "company": company}, "name")

    # Apply on every material Item Group (items inherit — nothing per-item to key).
    if itt:
        for grp in inventory.ITEM_GROUPS:
            try:
                if not frappe.db.exists("Item Group", grp):
                    continue
                g = frappe.get_doc("Item Group", grp)
                if g.meta.has_field("taxes") and not (g.get("taxes") or []):
                    g.append("taxes", {"item_tax_template": itt})
                    g.save(ignore_permissions=True)
                    result["groups"] += 1
            except Exception as exc:
                result["errors"].append(f"Item Group {grp}: {exc}")

    # Sales (output) document template — used on Quotation / Sales Invoice.
    if out_cgst and out_sgst and frappe.db.exists("DocType", "Sales Taxes and Charges Template") \
            and not frappe.db.exists("Sales Taxes and Charges Template",
                                     {"title": GST_SALES_TEMPLATE_TITLE, "company": company}):
        try:
            st = frappe.new_doc("Sales Taxes and Charges Template")
            st.title = GST_SALES_TEMPLATE_TITLE
            st.company = company
            for a, d in ((out_cgst, "CGST @ 9%"), (out_sgst, "SGST @ 9%")):
                st.append("taxes", {"charge_type": "On Net Total", "account_head": a,
                                    "rate": 9, "description": d})
            st.insert(ignore_permissions=True)
            result["sales_template"] = 1
        except Exception as exc:
            result["errors"].append(f"Sales template: {exc}")

    frappe.db.commit()
    return result


def ensure_gst_purchase_template():
    """S5 — seed one intra-state GST purchase template (CGST 9% + SGST 9% on net),
    so a PO computes tax on the discounted amount natively (MRP → discount → tax).
    Defensive: needs a Company with GST input-tax accounts (India Compliance setup);
    if they aren't there it skips cleanly. Idempotent."""
    if not frappe.db.exists("DocType", "Purchase Taxes and Charges Template"):
        return {"created": 0, "skipped": "no purchase-tax doctype"}
    company = _company_for_tax()
    if not company:
        return {"created": 0, "skipped": "no company"}

    def find_account(*keys):
        for k in keys:
            n = frappe.db.get_value(
                "Account", {"company": company, "account_name": ["like", f"%{k}%"], "is_group": 0}, "name"
            )
            if n:
                return n
        return None

    cgst = find_account("Input Tax CGST", "CGST")
    sgst = find_account("Input Tax SGST", "SGST")
    if not (cgst and sgst):
        return {"created": 0, "skipped": "no CGST/SGST input accounts — set up GST first"}
    if frappe.db.exists("Purchase Taxes and Charges Template", {"title": GST_TEMPLATE_TITLE, "company": company}):
        return {"created": 0}
    try:
        t = frappe.new_doc("Purchase Taxes and Charges Template")
        t.title = GST_TEMPLATE_TITLE
        t.company = company
        for acc_head, rate, desc in ((cgst, 9, "CGST @ 9%"), (sgst, 9, "SGST @ 9%")):
            t.append("taxes", {
                "category": "Total", "add_deduct_tax": "Add", "charge_type": "On Net Total",
                "account_head": acc_head, "rate": rate, "description": desc,
            })
        t.insert(ignore_permissions=True)
        frappe.db.commit()
        return {"created": 1}
    except Exception as exc:
        frappe.log_error(frappe.get_traceback(), "mallet_estimator GST template")
        return {"created": 0, "skipped": str(exc)}


def _company_for_tax():
    return (
        frappe.defaults.get_user_default("Company")
        or frappe.db.get_default("company")
        or frappe.db.get_value("Company", {}, "name")
    )


DEFAULT_ROOMS = [
    "Master Bedroom", "Kids Bedroom", "Guest Bedroom", "Living Room", "Dining Room",
    "Kitchen", "Study", "Pooja Room", "Foyer", "Balcony", "Bathroom", "Utility", "Other",
]


def after_install():
    from mallet_estimator import integration
    seed_settings()
    _safe(ensure_rooms)
    _safe(worksite.ensure_articles)
    _safe(worksite.ensure_work_stages)
    # A PERSON's role, not an integration's — the same policy after_migrate
    # already applies through sync_readonly_role, which is why it is minted
    # unconditionally rather than only re-pinned where it exists. Doing it on
    # install too is what stops a fresh site (a UI-test bench, a new
    # deployment) from having the tree doctypes with nobody able to read them.
    _safe(integration.ensure_photographer_role)
    _safe(ensure_inventory_masters)
    # AFTER the inventory masters, not with the other article-derived seeds.
    # A service Item needs an Item Group, and the groups are created by
    # ensure_inventory_masters — running this beside ensure_articles put it
    # before them, so all fifteen inserts died on "Could not find Item Group"
    # and the master came out empty on every fresh site.
    _safe(worksite.ensure_subcontract_service_items)
    _safe(ensure_warehouses)
    _safe(ensure_pricing_masters)
    _safe(ensure_project_customization)
    _safe(ensure_gst_purchase_template)
    _safe(ensure_gst_masters)
    _safe(ensure_manufacturing_masters)
    _safe(ensure_print_format)
    _safe(ensure_workspace)


def after_migrate():
    # Keep only the LIGHT, fast masters in sync on every migrate. The heavier
    # ones — inventory Item custom fields (a schema rebuild on the big Item table)
    # and the warehouse tree — are created by after_install and the "Create /
    # refresh manufacturing masters" button, NOT here, so a deploy's site-migration
    # step never stalls on them.
    _safe(ensure_rooms)
    # Both are light: two flat masters with no Item/Project schema behind them.
    _safe(worksite.ensure_articles)
    _safe(worksite.ensure_work_stages)
    # Same dependency as after_install: Item Groups first. They already exist
    # on a migrating site, and stating the order here keeps the two paths
    # readable as one rule rather than two coincidences.
    _safe(worksite.ensure_subcontract_service_items)
    _safe(ensure_pricing_masters)          # F5 — light: one Price List, no Item schema
    _safe(ensure_project_customization)    # F4 — light: Table + Section, no Project column
    _safe(inventory.ensure_vendor_masters) # S1 — light: Manufacturer/Brand/Supplier records
    _safe(ensure_manufacturing_masters)
    _safe(ensure_print_format)
    _safe(ensure_workspace)
    _safe(sync_readonly_role)
    # Regenerate the bootinfo/module-app cache so the app tile + workspace grouping
    # (module -> app) reflect the current state on the app switcher.
    _safe(frappe.clear_cache)


def sync_readonly_role():
    """Re-apply the read-only role's permissions if the site has that role.

    The doctype list is CODE, so widening or narrowing it is a deploy — but
    nothing re-applied it, and the permissions stayed frozen at whatever the
    button wrote the day it was pressed. Adding the cost doctypes to the list
    therefore changed nothing on any live site, silently, which is exactly the
    class of failure a productised app must not carry into its second studio.

    Only for sites that ALREADY have the role: a site nothing reads from
    should not acquire an integration role because it upgraded."""
    from mallet_estimator import integration
    # Each role re-pins INSIDE ITS OWN guard: one ensure throwing must not
    # starve the others (2026-08-15: the plugin role missed its new Project
    # grant on a migrate, invisibly, because the three shared one try).
    if frappe.db.exists("Role", integration.READONLY_ROLE):
        _safe(integration.ensure_readonly_role)
    # A person's role, not an integration's: always kept in step so a new
    # site user can be given the camera and nothing else.
    _safe(integration.ensure_photographer_role)
    # Same contract for the plugin writer role: re-pin where it exists,
    # never mint it on a site that merely upgraded.
    if frappe.db.exists("Role", integration.PLUGIN_ROLE):
        _safe(integration.ensure_plugin_role)
    # And for the data steward: data fixes bypass CI/CD, so its pins must
    # survive upgrades exactly as strictly.
    if frappe.db.exists("Role", integration.STEWARD_ROLE):
        _safe(integration.ensure_steward_role)


def ensure_core_seed():
    """Core ERPNext records the estimator cannot live without, created
    idempotently: fresh sites seed these through ERPNext's own fixtures, but
    partly in background jobs — CI's tests raced that seeding and lost
    ('Could not find Warehouse Type: Transit', intermittent, 2026-08-13).
    A no-op everywhere the records already exist, so safe on every site."""
    for wt in ("Transit",):
        if frappe.db.exists("DocType", "Warehouse Type") and \
                not frappe.db.exists("Warehouse Type", wt):
            frappe.get_doc({"doctype": "Warehouse Type", "name": wt}).insert(
                ignore_permissions=True)
    for uom in ("Nos", "Meter", "Roll", "Sheet"):
        if not frappe.db.exists("UOM", uom):
            frappe.get_doc({"doctype": "UOM", "uom_name": uom}).insert(
                ignore_permissions=True)
    # The stock default group old builds misfiled into — tests simulate that
    # misfile, and without the group the simulation lands somewhere valid.
    if frappe.db.exists("Item Group", "All Item Groups") and \
            not frappe.db.exists("Item Group", "Products"):
        frappe.get_doc({"doctype": "Item Group", "item_group_name": "Products",
                        "parent_item_group": "All Item Groups"}).insert(
            ignore_permissions=True)
    frappe.db.commit()


def ensure_rooms():
    for r in DEFAULT_ROOMS:
        if not frappe.db.exists("Estimate Room", r):
            doc = frappe.new_doc("Estimate Room")
            doc.room_name = r
            doc.insert(ignore_permissions=True)
    frappe.db.commit()


def _safe(fn):
    try:
        fn()
    except Exception:
        frappe.log_error(frappe.get_traceback(), f"mallet_estimator {fn.__name__}")


def _op_name(phase):
    # Doc names avoid "/" which is awkward in Frappe routing/urls.
    return phase.replace(" / ", " - ").replace("/", "-")


def _hardware_standard(phase):
    """Seed minutes for one of the Install <type> children, or 0.

    Keyed by the OPERATION NAME rather than the bucket, because that is what
    the seeding loop has in hand. Reversed off the same tuple the estimate
    reads, so there is one list of hardware types in this app and not two.
    """
    from mallet_estimator import estimator as _e
    for kind, _label in _e.HARDWARE_INSTALL_TYPES:
        if _e.hardware_operation(kind) == phase:
            return _e.HARDWARE_STANDARDS.get(kind, 0)
    return 0


def _ensure_operating_components():
    """Make sure the native 'Workstation Operating Component' masters
    (Rent/Wages/Electricity/Consumables) exist. ERPNext ships these, but create
    any that are missing so seeding a workstation's costs never fails on a bad
    link. (autoname = field:component_name.)"""
    dt = "Workstation Operating Component"
    if not frappe.db.exists("DocType", dt):
        return
    for c in WS_COMPONENTS:
        if not frappe.db.exists(dt, c):
            try:
                doc = frappe.new_doc(dt)
                doc.component_name = c
                doc.insert(ignore_permissions=True)
            except Exception:
                frappe.log_error(frappe.get_traceback(), f"mallet_estimator operating component: {c}")


def _strip_invalid_costs(ws):
    """B1 — drop any workstation_costs row whose operating_component isn't one of
    the current valid components. An old seed left a 'Machinery' row (folded into
    Consumables when the component was removed); its dangling link fails validation
    on every workstation re-save. Returns True if a row was removed."""
    if not ws.meta.has_field("workstation_costs"):
        return False
    rows = ws.get("workstation_costs") or []

    def _ok(comp):
        if comp in WS_COMPONENTS:
            return True
        # A legacy component (pre-OPS3 folded "Wages"/"Machinery") is only valid
        # while its master still exists — a dangling link is exactly what B1
        # crashed on, so those rows are stripped.
        return comp in LEGACY_WS_COMPONENTS and \
            frappe.db.exists("Workstation Operating Component", comp)

    valid = [r for r in rows if _ok(r.operating_component)]
    if len(valid) != len(rows):
        ws.set("workstation_costs", valid)
        return True
    return False


def _set_workstation_costs(ws, rate):
    """Populate a Workstation's native `workstation_costs` child table from the
    computed component rates. Only used when the table is empty (never clobbers
    hand-tuned rates). Returns True if rows were set."""
    if not ws.meta.has_field("workstation_costs"):
        return False
    if getattr(ws, "workstation_costs", None):
        return False  # already configured (e.g. a hand-set Panel Saw) — leave it
    for label, val in rate["components"]:
        if not val:
            continue
        ws.append("workstation_costs", {
            "operating_component": label,
            "operating_cost": round(val, 2),
        })
    return bool(getattr(ws, "workstation_costs", None))


def ensure_manufacturing_masters():
    """Create the 7 Workstations (native operating-component hour rates), 17
    Operations and the standard Routing as ERPNext manufacturing masters.
    Idempotent: existing records keep their hand-tuned rates; a workstation that
    has no operating-cost rows yet gets them backfilled. Each record is created
    independently so one failure doesn't abort the rest."""
    settings = frappe.get_single("Estimate Settings")
    rates = {w["name"]: w for w in workstation_rates(settings)}
    result = {"workstations": 0, "workstations_costed": 0, "operations": 0, "routing": 0, "errors": []}

    def fail(label, exc):
        result["errors"].append(f"{label}: {exc}")
        frappe.log_error(frappe.get_traceback(), f"mallet_estimator masters: {label}")

    _ensure_operating_components()

    for w in WORKSTATIONS:
        try:
            r = rates[w["name"]]
            if frappe.db.exists("Workstation", w["name"]):
                # Backfill operating costs only if this workstation has none yet,
                # so a manually configured station (your Panel Saw) is preserved.
                # First strip any stale/invalid cost row (B1) so the re-save below
                # doesn't fail link validation on a removed component.
                ws = frappe.get_doc("Workstation", w["name"])
                changed = _strip_invalid_costs(ws)
                if _set_workstation_costs(ws, r):
                    changed = True
                if changed:
                    ws.save(ignore_permissions=True)
                    result["workstations_costed"] += 1
                continue
            ws = frappe.new_doc("Workstation")
            ws.workstation_name = w["name"]
            _set_workstation_costs(ws, r)
            ws.insert(ignore_permissions=True)
            result["workstations"] += 1
        except Exception as exc:
            fail(f"Workstation {w['name']}", exc)

    try:
        from frappe.custom.doctype.custom_field.custom_field import create_custom_fields
        create_custom_fields(OPERATION_CUSTOM_FIELDS, ignore_validate=True)
    except Exception as exc:
        fail("Operation custom field", exc)
    op_meta = frappe.get_meta("Operation")
    has_min = op_meta.has_field("mallet_min_per_unit")

    # The hardware children are Operations in their own right — that is the
    # whole point of splitting the line. Each carries its own standard time and
    # is tuned on its own master, so "Install Hinges" can be four minutes while
    # "Install Shelf Supports" is one, which a single averaged figure could
    # never say. Built from the same tuple the estimate reads, so a type added
    # there appears here without a second edit.
    hardware_steps = [{"phase": estimator.hardware_operation(k)}
                      for k, _label in estimator.HARDWARE_INSTALL_TYPES]

    for t in STEP_TEMPLATE + DESIGN_STEP_TEMPLATE + hardware_steps:
        op_name = _op_name(t["phase"])
        mins = OPERATION_STANDARDS.get(t["phase"], {}).get("min_per_unit", 0) \
            or DESIGN_STANDARDS.get(t["phase"], {}).get("min_per_unit", 0) \
            or _hardware_standard(t["phase"])
        ws = OPERATION_WORKSTATION.get(t["phase"])
        try:
            if frappe.db.exists("Operation", op_name):
                # Backfill the standard time (min/unit) + default workstation on an
                # existing Operation only where unset — never clobber a hand-tuned value.
                op = frappe.get_doc("Operation", op_name)
                changed = False
                if has_min and mins and not (op.get("mallet_min_per_unit") or 0):
                    op.mallet_min_per_unit = mins
                    changed = True
                if ws and not op.workstation and frappe.db.exists("Workstation", ws):
                    op.workstation = ws
                    changed = True
                if changed:
                    op.save(ignore_permissions=True)
                    result["operations"] += 1
                continue
            op = frappe.new_doc("Operation")
            op.name = op_name
            if ws and frappe.db.exists("Workstation", ws):
                op.workstation = ws
            if has_min and mins:
                op.mallet_min_per_unit = mins
            op.insert(ignore_permissions=True, set_name=op_name)
            result["operations"] += 1
        except Exception as exc:
            fail(f"Operation {op_name}", exc)

    try:
        if not frappe.db.exists("Routing", ROUTING_NAME):
            routing = frappe.new_doc("Routing")
            routing.routing_name = ROUTING_NAME
            for i, t in enumerate(STEP_TEMPLATE, start=1):
                routing.append("operations", {
                    "sequence_id": i,
                    "operation": _op_name(t["phase"]),
                    "workstation": OPERATION_WORKSTATION.get(t["phase"]),
                    "time_in_mins": 0,
                })
            routing.insert(ignore_permissions=True)
            result["routing"] = 1
    except Exception as exc:
        fail("Routing", exc)

    frappe.db.commit()
    return result


@frappe.whitelist()
def setup():
    """Manually (re)create all app masters — callable from the Estimate Settings
    button. Returns a summary so the UI can report what was created and any error."""
    if not frappe.has_permission("Estimate Settings", "write"):
        frappe.throw("Not permitted")
    seed_settings()
    _safe(ensure_rooms)
    _safe(worksite.ensure_articles)
    _safe(worksite.ensure_work_stages)
    inv, wh = {}, {}
    try:
        inv = ensure_inventory_masters()
        # Item Groups exist only once the line above has run.
        _safe(worksite.ensure_subcontract_service_items)
    except Exception as exc:
        frappe.log_error(frappe.get_traceback(), "mallet_estimator ensure_inventory_masters")
        inv = {"errors": [f"inventory: {exc}"]}
    try:
        wh = ensure_warehouses()
    except Exception as exc:
        frappe.log_error(frappe.get_traceback(), "mallet_estimator ensure_warehouses")
        wh = {"errors": [f"warehouses: {exc}"]}
    result = ensure_manufacturing_masters()
    result["inventory"] = inv
    result["warehouses"] = wh
    for fn in (ensure_project_customization, ensure_gst_purchase_template, ensure_gst_masters, ensure_print_format, ensure_workspace):
        try:
            fn()
        except Exception as exc:
            frappe.log_error(frappe.get_traceback(), f"mallet_estimator {fn.__name__}")
            result.setdefault("errors", []).append(f"{fn.__name__}: {exc}")
    result["workspace_exists"] = bool(frappe.db.exists("Workspace", WORKSPACE_NAME))
    return result


WAREHOUSE_LEAVES = [
    "Board & Sheet Store", "Hardware Store", "Cut Parts - Table 1", "Cut Parts - Table 2",
    "Assembly Area", "Project Room", "Packed / Dispatch", "Customer Provided",
]
ITEM_CUSTOM_FIELDS = ["mallet_oc_code", "mallet_thickness_mm", "mallet_sheet_length_mm", "mallet_sheet_width_mm"]


def ensure_demo_company():
    """Create a demo Company if the site has none — used by CI (so warehouses can
    be created) and for a quick local demo. No-op when a company already exists.
    Seeds the 'Transit' Warehouse Type first: ERPNext's default company warehouses
    need it, and it is normally created by the setup wizard (absent on a bare
    install-app erpnext)."""
    if frappe.db.get_value("Company", {}, "name"):
        return
    if frappe.db.exists("DocType", "Warehouse Type") and not frappe.db.exists("Warehouse Type", "Transit"):
        try:
            wt = frappe.new_doc("Warehouse Type")
            if wt.meta.has_field("warehouse_type_name"):
                wt.warehouse_type_name = "Transit"
            wt.insert(ignore_permissions=True, set_name="Transit")
        except Exception:
            frappe.log_error(frappe.get_traceback(), "mallet_estimator seed Warehouse Type")
    frappe.get_doc({
        "doctype": "Company", "company_name": "Mallet Demo", "abbr": "MD",
        "default_currency": "INR", "country": "India",
    }).insert(ignore_permissions=True)
    frappe.db.commit()


@frappe.whitelist()
def verify_setup():
    """Config health-check — assert every master this app needs exists and is
    shaped right. Returns {checks:[{name, ok, detail}], all_ok, failed}. Drives
    the 'Verify setup' button AND is asserted by the automated tests, so the same
    contract is checked by hand and in CI."""
    checks = []

    def chk(name, ok, detail=""):
        checks.append({"name": name, "ok": bool(ok), "detail": detail})

    def missing(dt, names, by_field=None):
        out = []
        for n in names:
            exists = frappe.db.exists(dt, {by_field: n}) if by_field else frappe.db.exists(dt, n)
            if not exists:
                out.append(n)
        return out

    groups = [inventory.PARENT_GROUP, inventory.CLIENT_SKU_GROUP] + inventory.ITEM_GROUPS
    m = missing("Item Group", groups)
    chk("Item Groups", not m, ("missing: " + ", ".join(m)) if m else f"{len(groups)} present")

    uoms = ["Sheet", "Meter", "Roll", "Square Meter"]
    m = missing("UOM", uoms)
    chk("UOMs", not m, ("missing: " + ", ".join(m)) if m else "Sheet, Meter, Roll, Square Meter ✓")

    meta = frappe.get_meta("Item")
    m = [f for f in ITEM_CUSTOM_FIELDS if not meta.has_field(f)]
    chk("Item custom fields", not m, ("missing: " + ", ".join(m)) if m else "thickness + sheet L/W + OC code ✓")

    m = missing("Warehouse", WAREHOUSE_LEAVES, by_field="warehouse_name")
    chk("Warehouses", not m, ("missing: " + ", ".join(m)) if m else f"{len(WAREHOUSE_LEAVES)} present")

    m = missing("Workstation", [w["name"] for w in WORKSTATIONS])
    chk("Workstations", not m, ("missing: " + ", ".join(m)) if m else f"{len(WORKSTATIONS)} present")

    n_ops = frappe.db.count("Operation")
    chk("Operations", n_ops >= len(STEP_TEMPLATE), f"{n_ops} operations")
    chk("Batch tiers (CSV-Nest)",
        frappe.db.exists("DocType", "Mallet Operation Batch Tier")
        and frappe.get_meta("Operation").has_field("mallet_batch_tiers"),
        "Operation.mallet_batch_tiers → Mallet Operation Batch Tier")
    chk("Routing", frappe.db.exists("Routing", ROUTING_NAME), ROUTING_NAME)

    # Client → Site → Project → Room. The site level is the one ERPNext has no
    # concept of, so its absence is silent until a photo files itself under the
    # wrong folder — checked here rather than discovered there.
    chk("Mallet Site", bool(frappe.db.exists("DocType", "Mallet Site")),
        "Client → Site → Project → Room")
    n_art = frappe.db.count("Mallet Article")
    chk("Articles", n_art >= len(worksite.ARTICLES),
        f"{n_art} of {len(worksite.ARTICLES)} seeded")
    # The KIND decides whether an SKU needs a cut list at all and the BASIS
    # decides which unit the site is asked for. An article missing either is
    # one the phone cannot offer, which shows up as a picker with holes in it
    # rather than as an error.
    if frappe.db.exists("DocType", "Mallet Article") and \
            frappe.get_meta("Mallet Article").has_field("basis"):
        vague = frappe.get_all(
            "Mallet Article",
            filters={"disabled": 0},
            or_filters=[{"kind": ("in", ["", None])},
                        {"basis": ("in", ["", None])}],
            pluck="name", limit_page_length=8)
        chk("Articles know their kind and unit", not vague,
            ("no kind/unit on: " + ", ".join(vague)) if vague
            else f"{len(worksite.KINDS)} kinds, {len(worksite.BASES)} units")
    n_stage = frappe.db.count("Mallet Work Stage")
    n_sub = sum(1 for c, _n, _j, k, _b in worksite.ARTICLES
                if k == worksite.SUBCONTRACT)
    n_svc = sum(1 for c, _n, _j, k, _b in worksite.ARTICLES
                if k == worksite.SUBCONTRACT
                and frappe.db.exists("Item", worksite.subcontract_item_code(c)))
    # WHY it is short, not merely THAT it is. The first run of this check said
    # "0 of 15" and nothing else; the cause was one bad item group and the
    # explanation sat in an Error Log nobody had reason to open. A health check
    # that cannot point at the next step just relocates the question.
    svc_detail = (f"{n_svc} of {n_sub} — a missing one is a trade whose vendor "
                  f"rate has nowhere to live, so it quotes at zero")
    if n_svc < n_sub:
        why = frappe.db.get_value(
            "Error Log",
            {"method": ["like", "%ensure_subcontract_service_items%"]},
            "error", order_by="creation desc")
        if why:
            svc_detail += " — last error: " + str(why).strip().split("\n")[0][:160]
    chk("Subcontract service items", n_svc >= n_sub, svc_detail)

    chk("Work stages", n_stage >= len(worksite.WORK_STAGES),
        f"{n_stage} of {len(worksite.WORK_STAGES)} seeded, "
        f"{len(worksite.PHASES)} phases")
    # A stage nobody can reach is a seeding bug that only shows up as an empty
    # picker on a phone, which is the worst place to discover it.
    orphan = []
    for job in worksite.JOB_TYPES:
        if not frappe.get_all("Mallet Work Stage",
                              filters={"job_types": ("like", f"%{job}%"), "disabled": 0},
                              limit_page_length=1):
            orphan.append(job)
    chk("Every job type has stages", not orphan,
        ("no stages for: " + ", ".join(orphan)) if orphan else
        ", ".join(worksite.JOB_TYPES))

    # The phone browses the tree through the photographer role. A missing
    # grant shows up as an EMPTY tree with no error attached, which reads as a
    # broken app rather than a missing permission — so it is asserted here.
    from mallet_estimator import integration
    tree_dts = [dt for dt in ("Mallet Site", "Mallet Article", "Mallet Work Stage")
                if frappe.db.exists("DocType", dt)]
    if not frappe.db.exists("Role", integration.PHOTOGRAPHER_ROLE):
        # Not a failure: the role is minted on migrate, and a bench that has
        # only installed the app has not reached that yet. Reporting it as
        # broken would make a correct fresh install look wrong.
        chk("Phone can read the tree", True, "photographer role not minted yet")
    else:
        blind = [dt for dt in tree_dts
                 if not frappe.db.exists("Custom DocPerm", {
                     "role": integration.PHOTOGRAPHER_ROLE, "parent": dt, "read": 1})]
        chk("Phone can read the tree", not blind,
            ("photographer cannot read: " + ", ".join(blind)) if blind
            else "site + article + work stage ✓")

    pmeta = frappe.get_meta("Project")
    pf = ["mallet_site", "mallet_job_type", "mallet_stage", "mallet_stage_log"]
    m = [f for f in pf if not pmeta.has_field(f)]
    chk("Project site & stage fields", not m,
        ("missing: " + ", ".join(m)) if m else "site + job type + stage + log ✓")
    chk("Print format", frappe.db.exists("Print Format", PRINT_FORMAT_NAME), PRINT_FORMAT_NAME)
    chk("Job Card print", frappe.db.exists("Print Format", JOB_CARD_PRINT_FORMAT_NAME), JOB_CARD_PRINT_FORMAT_NAME)
    chk("Execution print", frappe.db.exists("Print Format", EXECUTION_PRINT_FORMAT_NAME), EXECUTION_PRINT_FORMAT_NAME)
    chk("Workspace", frappe.db.exists("Workspace", WORKSPACE_NAME), WORKSPACE_NAME)

    # Site 360 photos: the doctype AND the projection engine. numpy rides the
    # app's pyproject, but a bench that skipped dependency install would
    # otherwise only find out at the first split — surface it here instead.
    try:
        from mallet_estimator import panorama
        pano_detail = f"splitter ready (default {int(panorama.DEFAULT_FOV)}°, {panorama.DEFAULT_FACE_PX}px)"
        pano_ok = True
    except ImportError as exc:
        pano_detail, pano_ok = f"projection engine unavailable: {exc}", False
    chk("Site Photo 360", bool(frappe.db.exists("DocType", "Site Photo 360")) and pano_ok,
        pano_detail)

    # The capture PWA is a page ON this bench (/sitephoto), so a missing shell
    # is a silent 404 on someone's phone at a site visit — checked, not assumed.
    import os
    _app = frappe.get_app_path("mallet_estimator")
    _pwa = [r for r in ("www/sitephoto/index.html", "www/sitephoto/index.py",
                        "public/sitephoto/manifest.json",
                        "public/images/sitephoto-192.png")
            if not os.path.exists(os.path.join(_app, r))]
    # The ImageMeter loop: masters present, and whether it is actually wired.
    _im_ok = bool(frappe.db.exists("DocType", "Site Photo Settings")) and \
        bool(frappe.db.exists("DocType", "Site Photo Inbox"))
    _im_on = bool(frappe.db.get_single_value("Site Photo Settings", "sync_enabled")) \
        if _im_ok else False
    import os as _os
    _im_key = bool(_os.environ.get("MCFT_GDRIVE_SA_JSON"))
    chk("ImageMeter sync", _im_ok,
        ("masters ✓ — " + ("enabled" if _im_on else "not enabled")
         + (", credential present" if _im_key else ", MCFT_GDRIVE_SA_JSON not set on this bench"))
        if _im_ok else "missing Site Photo Settings / Site Photo Inbox")

    # A workspace edit is only applied when the on-disk record is NEWER than
    # the row (2026-08-16: it was not, and three shipped edits did nothing at
    # all, silently). Compare the live record against what the code expects.
    _want = {"Estimate SKU", "Estimate", "Estimate Settings",
             "Site Photo 360", "Site Photo Inbox", "Site Photo Settings"}
    try:
        _ws = frappe.get_doc("Workspace", WORKSPACE_NAME)
        _have = {l.label for l in _ws.links}
        _miss = sorted(_want - _have)
        _short = {s.label for s in (_ws.shortcuts or [])}
    except Exception:
        _miss, _short = sorted(_want), set()
    chk("Workspace links", not _miss,
        ("NOT applied — bump `modified` in the workspace JSON; missing: "
         + ", ".join(_miss)) if _miss
        else f"{len(_want)} links + {len(_short)} shortcut(s) ✓")

    chk("Site Photo PWA", not _pwa,
        ("missing: " + ", ".join(_pwa)) if _pwa else "/sitephoto shell + manifest + icons ✓")

    # F5 — assumed price list; F4 — Project material-choice table; F6 — allowance table.
    chk("Assumed price list", frappe.db.exists("Price List", inventory.ESTIMATION_PRICE_LIST),
        inventory.ESTIMATION_PRICE_LIST)
    chk("Project material choices", frappe.get_meta("Project").has_field("mallet_material_choices"),
        "Project.mallet_material_choices")
    chk("Allowance table", frappe.get_meta("Estimate").has_field("allowances"), "Estimate.allowances")

    # F3 — structured coding Item fields; F2/S3 — maker/brand + part-no fields.
    imeta = frappe.get_meta("Item")
    cf = [f for f in ("mallet_visible_sides", "mallet_lam_internal", "mallet_lam_external",
                      "mallet_mfr_part_no", "mallet_gst_pct") if not imeta.has_field(f)]
    chk("Coding/sourcing fields", not cf, ("missing: " + ", ".join(cf)) if cf else "coding + mfr part no + GST% ✓")
    miss_mfr = missing("Manufacturer", inventory.MANUFACTURERS)
    miss_brand = missing("Brand", inventory.BRAND_NAMES)
    chk("Manufacturers/Brands", not miss_mfr and not miss_brand,
        ("missing mfr: " + ", ".join(miss_mfr) + " brand: " + ", ".join(miss_brand))
        if (miss_mfr or miss_brand) else f"{len(inventory.MANUFACTURERS)} OEM makers + brands ✓")
    # S1 — the real vendors + Paint category.
    miss_sup = [s for s in inventory.SUPPLIER_SCOPE if not inventory.supplier_docname(s)]
    chk("Suppliers", not miss_sup, ("missing: " + ", ".join(miss_sup)) if miss_sup else f"{len(inventory.SUPPLIER_SCOPE)} vendors ✓")
    chk("Paint category", frappe.db.exists("Item Group", "Paint"), "Paint item group")

    # T2 — native GST: item tax template applied on the material groups. A site
    # whose chart of accounts has no Duties-and-Taxes group (bare CI) can't hold
    # tax accounts — the checks report but never fail there. Scoped to the
    # DEFAULT company: ERPNext's test machinery can conjure a _Test Company
    # with a full chart mid-run, and accounts on a company this site does not
    # serve must not flip the verdict (CI, 2026-08-13).
    itt = frappe.db.get_value("Item Tax Template", {"title": ITEM_TAX_TEMPLATE}, "name")
    default_company = frappe.defaults.get_global_default("company")
    has_tax_parent = bool(default_company) and bool(frappe.db.exists(
        "Account", {"account_name": ["like", "%Duties and Taxes%"], "is_group": 1,
                    "company": default_company}))
    grp_applied = bool(itt) and bool(frappe.db.exists("Item Tax", {"parenttype": "Item Group"}))
    chk("Item tax template", bool(itt) or not has_tax_parent,
        ITEM_TAX_TEMPLATE if itt else ("no Duties-and-Taxes accounts on this site" if not has_tax_parent
                                       else "missing — run Create/refresh masters"))
    chk("Groups carry GST template", grp_applied or not itt,
        "applied on material groups" if grp_applied else
        ("n/a — no template" if not itt else "TEMPLATE EXISTS but not applied — run Create/refresh masters"))

    # T2b — EVERY material Item must resolve to the GST template: an explicit
    # item-level row, or (at minimum) its group's row. Names the offenders.
    if itt:
        mat_groups = [g for g in inventory.ITEM_GROUPS if frappe.db.exists("Item Group", g)]
        taxed_groups = {r.parent for r in frappe.get_all(
            "Item Tax", filters={"parenttype": "Item Group"}, fields=["parent"])}
        taxed_items = {r.parent for r in frappe.get_all(
            "Item Tax", filters={"parenttype": "Item"}, fields=["parent"])}
        untaxed = [
            it.name for it in frappe.get_all(
                "Item", filters={"item_group": ["in", mat_groups], "disabled": 0},
                fields=["name", "item_group"])
            if it.name not in taxed_items and it.item_group not in taxed_groups
        ]
        chk("Items carry GST", not untaxed,
            ("NO tax template resolves for: " + ", ".join(untaxed[:12])
             + ("…" if len(untaxed) > 12 else "")) if untaxed
            else "every material item resolves to GST 18% ✓")

    n_unpriced = frappe.db.count("Item", {
        "item_group": ["in", inventory.ITEM_GROUPS], "disabled": 0, "valuation_rate": 0,
        "last_purchase_rate": 0, "standard_rate": 0,
    })
    chk("Material prices", True, f"{n_unpriced} material(s) still unpriced" if n_unpriced else "all materials priced")

    # The read-only integration role. Its whole value is the guarantee that it
    # cannot write and cannot open Estimate Settings — so that is what is
    # asserted. Existing is not the same as still being safe.
    from mallet_estimator import integration
    if frappe.db.exists("Role", integration.READONLY_ROLE):
        ok, detail = integration.role_is_read_only()
        chk("Read-only API role", ok, detail)
    else:
        chk("Read-only API role", True,
            "not created — optional, see the button on Estimate Settings")

    # Unlike the three API identities, the photographer role is minted on every
    # site by sync_readonly_role — so absence here does not mean "optional, not
    # set up", it means the migrate never ran. That is the one deploy failure
    # this app keeps hitting, and until now nothing on the outside could see it.
    if frappe.db.exists("Role", integration.PHOTOGRAPHER_ROLE):
        ok, detail = integration.photographer_is_scoped()
        chk("Site photographer role", ok, detail)
    else:
        chk("Site photographer role", False,
            "missing — the migrate did not run; ship a patch to force one")

    # The role only means anything while the doctypes are NOT open to everyone.
    # They shipped granting "All" read/write/create, which made the role
    # decorative: every login had the camera and the inbox (2026-08-17).
    _open = []
    for _dt in ("Site Photo 360", "Site Photo Inbox"):
        if not frappe.db.exists("DocType", _dt):
            continue
        for _tbl in ("Custom DocPerm", "DocPerm"):
            for _r in frappe.get_all(_tbl, filters={"parent": _dt, "role": "All"},
                                     fields=["read", "write", "create"]):
                if any(_r.get(p) for p in ("read", "write", "create")):
                    _open.append(f"{_dt} ({_tbl})")
    chk("Site photos are not open to everyone", not _open,
        ("'All' can still reach " + ", ".join(sorted(set(_open)))) if _open
        else "only System Manager and the photographer role ✓")

    # The NAME alone is not enough to act on. "Articles" tells you a check is
    # red; "Articles: 0 of 26 seeded" tells you which of two very different
    # bugs it is, without a second CI round trip to find out. Every detail
    # string was already written for exactly this moment; it just was not
    # being carried out of the function (2026-08-21: two red UI runs sat
    # unexplained because the failure list was a list of headings).
    failed = [f"{c['name']}: {c['detail']}" if c["detail"] else c["name"]
              for c in checks if not c["ok"]]
    return {"checks": checks, "all_ok": not failed, "failed": failed}


def ensure_workspace():
    """The workspace now ships as a STANDARD on-disk file
    (mallet_estimator/workspace/mallet_estimator/mallet_estimator.json), which the
    migrate syncs — that is what makes it tile on the app switcher (module -> app).
    This stays only as a fallback: create it if the disk sync somehow didn't, and
    NEVER clobber the synced standard one."""
    if frappe.db.exists("Workspace", WORKSPACE_NAME):
        return

    ws = frappe.new_doc("Workspace")
    ws.name = WORKSPACE_NAME
    ws.label = WORKSPACE_NAME
    ws.title = WORKSPACE_NAME
    ws.public = 1
    ws.module = "Mallet Estimator"
    ws.app = "mallet_estimator"  # groups it under our app on the switcher (module->app)
    ws.icon = "project"
    # Match ERPNext's public top-level workspaces so it tiles in the app switcher:
    # empty (not NULL) parent_page/for_user and a non-zero sequence.
    ws.parent_page = ""
    ws.for_user = ""
    ws.sequence_id = 20
    ws.content = json.dumps([{"id": "mest_card", "type": "card", "data": {"card_name": "Estimating", "col": 4}}])
    for typ, label, dt in [
        ("Card Break", "Estimating", None),
        ("Link", "Estimate SKU", "Estimate SKU"),
        ("Link", "Estimate", "Estimate"),
        ("Link", "Estimate Settings", "Estimate Settings"),
    ]:
        row = {"type": typ, "label": label}
        if dt:
            row.update({"link_type": "DocType", "link_to": dt})
        ws.append("links", row)
    ws.insert(ignore_permissions=True)
    frappe.db.commit()
    frappe.clear_cache()


def seed_settings():
    # Persist the single so default rates/rent are stored (workstation costing
    # reads them). Machine masters live as ERPNext Workstations, not here.
    settings = frappe.get_single("Estimate Settings")
    settings.flags.ignore_permissions = True
    settings.save()
    frappe.db.commit()


def has_app_permission():
    """Whether to show the Mallet Estimator tile on the /apps launcher — visible to
    anyone who can read an Estimate SKU. Defensive: never break the apps screen."""
    try:
        return bool(frappe.has_permission("Estimate SKU", "read"))
    except Exception:
        return True


def _upsert_print_format(name, doc_type, template_file):
    path = os.path.join(
        frappe.get_app_path("mallet_estimator"), "templates", "print", template_file,
    )
    with open(path, "r", encoding="utf-8") as f:
        html = f.read()

    if frappe.db.exists("Print Format", name):
        pf = frappe.get_doc("Print Format", name)
    else:
        pf = frappe.new_doc("Print Format")
        pf.name = name

    pf.doc_type = doc_type
    pf.module = "Mallet Estimator"
    pf.print_format_type = "Jinja"
    pf.custom_format = 1
    pf.standard = "No"
    pf.disabled = 0
    pf.html = html
    pf.flags.ignore_permissions = True
    pf.save()


def ensure_print_format():
    _upsert_print_format(PRINT_FORMAT_NAME, "Estimate", "mallet_client_estimate.html")
    _upsert_print_format(JOB_CARD_PRINT_FORMAT_NAME, "Job Card", "mallet_job_card.html")
    _upsert_print_format(EXECUTION_PRINT_FORMAT_NAME, "Estimate", "mallet_execution_estimate.html")
    frappe.db.commit()
