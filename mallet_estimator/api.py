# ---------------------------------------------------------------------------
# Whitelisted API — the integration contract for the mcft-ocl SketchUp
# companion plugin (and any other client). The human attaching a CSV in the
# UI and the plugin POSTing one land on the SAME import path (nest_import via
# the SKU's save pipeline), so estimation behaves identically either way.
#
#   POST /api/method/mallet_estimator.api.import_parts_csv
#        {sku, csv_content, filename?}
#   GET  /api/method/mallet_estimator.api.get_sku_context?sku=MEST-SKU-00001
#
# Auth: normal Frappe token/session auth; permissions are the Estimate SKU
# doc permissions — nothing extra granted here.
# ---------------------------------------------------------------------------

import json

import frappe
from frappe import _


@frappe.whitelist(allow_guest=False)
def version_info():
    """The estimator code ACTUALLY running on this site — the antidote to a
    deploy that claims success. Frappe Cloud builds an image from the repo and
    the container may carry no git binary and no .git tree, so the commit is
    resolved by falling back through every source that can exist there:
      1. `git rev-parse` (dev benches)
      2. .git/HEAD + refs/packed-refs read as plain files (no git binary)
      3. the bench's apps.json (what FC's image build recorded)
    `source` says which one answered, so a missing badge is diagnosable."""
    import json as _json
    import os
    import subprocess

    from mallet_estimator import __version__

    out = {"version": __version__, "commit": None, "branch": None, "source": "none"}
    try:
        app_root = os.path.dirname(frappe.get_app_path("mallet_estimator"))
    except Exception:
        return out

    # 1) real git
    try:
        def git(*args):
            return subprocess.check_output(
                ["git", "-C", app_root] + list(args),
                text=True, stderr=subprocess.DEVNULL, timeout=5).strip()
        out["commit"] = git("rev-parse", "--short", "HEAD")
        out["branch"] = git("rev-parse", "--abbrev-ref", "HEAD")
        out["source"] = "git"
        return out
    except Exception:
        pass

    # 2) plain-file read of .git (no git binary needed)
    try:
        head_path = os.path.join(app_root, ".git", "HEAD")
        with open(head_path) as fh:
            head = fh.read().strip()
        sha = None
        if head.startswith("ref:"):
            ref = head.split(" ", 1)[1].strip()
            out["branch"] = ref.rsplit("/", 1)[-1]
            ref_file = os.path.join(app_root, ".git", ref)
            if os.path.exists(ref_file):
                with open(ref_file) as fh:
                    sha = fh.read().strip()
            else:  # packed refs
                packed = os.path.join(app_root, ".git", "packed-refs")
                if os.path.exists(packed):
                    with open(packed) as fh:
                        for line in fh:
                            if line.rstrip().endswith(" " + ref):
                                sha = line.split(" ", 1)[0].strip()
                                break
        else:
            sha = head
        if sha:
            out["commit"] = sha[:7]
            out["source"] = "git-files"
            return out
    except Exception:
        pass

    # 3) whatever the image build recorded
    try:
        bench_root = os.path.dirname(os.path.dirname(app_root))  # …/apps/<app> -> bench
        for candidate in (os.path.join(bench_root, "apps.json"),
                          os.path.join(bench_root, "sites", "apps.json")):
            if not os.path.exists(candidate):
                continue
            with open(candidate) as fh:
                data = _json.load(fh)
            entry = data.get("mallet_estimator") if isinstance(data, dict) else None
            if isinstance(entry, dict):
                sha = entry.get("commit") or entry.get("hash") or entry.get("version")
                if sha:
                    out["commit"] = str(sha)[:7]
                    out["branch"] = entry.get("branch") or out["branch"]
                    out["source"] = "apps.json"
                    return out
    except Exception:
        pass

    return out


def _find_sku(sku):
    """The non-throwing half of resolution: doc by NAME or unique SKU CODE,
    else None. A code matching SEVERAL SKUs still throws — silence there
    would import into an arbitrary one."""
    if frappe.db.exists("Estimate SKU", sku):
        return frappe.get_doc("Estimate SKU", sku)
    matches = frappe.get_all("Estimate SKU", filters={"sku_code": sku}, pluck="name")
    if len(matches) == 1:
        return frappe.get_doc("Estimate SKU", matches[0])
    if len(matches) > 1:
        frappe.throw(_("SKU code {0} matches {1} Estimate SKUs — address it by name instead.")
                     .format(sku, len(matches)))
    return None


def _resolve_sku(sku):
    """An Estimate SKU addressed by NAME (MEST-SKU-00008) or by SKU CODE
    (YS_MB_WAR). The code path is what the SketchUp plugin uses: with one
    container model per project, each article is a component NAMED with its
    sku_code (Amit, 2026-08-11), so the component name IS the address and
    nobody re-types document ids into SketchUp."""
    doc = _find_sku(sku)
    if doc:
        return doc
    # The component was named naturally (LOFT) while the code generator
    # abbreviates (LOF) — the commonest miss, so the refusal names the
    # nearest real codes instead of leaving the renamer to guess.
    import difflib
    codes = frappe.get_all("Estimate SKU", pluck="sku_code")
    near = difflib.get_close_matches(sku, [c for c in codes if c], n=3, cutoff=0.6)
    hint = _(" Did you mean: {0}?").format(", ".join(near)) if near else ""
    frappe.throw(_("No Estimate SKU named or coded {0}.{1} Create it on the Estimate "
                   "first (Add SKUs), or rename the component in SketchUp to the "
                   "exact sku_code.").format(sku, hint))


def _project_customer(project):
    """(customer, display_name, initials) for a Project — the binding the
    SketchUp file carries decides these, never the component name."""
    from mallet_estimator.estimator import customer_initials
    if not frappe.db.exists("Project", project):
        frappe.throw(_("No Project named {0} — re-link the SketchUp model.").format(project))
    customer = frappe.db.get_value("Project", project, "customer")
    if not customer:
        frappe.throw(_("Project {0} has no Customer — set it on the Project first.").format(project))
    display = frappe.db.get_value("Customer", customer, "customer_name") or customer
    return customer, display, customer_initials(display)


def _room_for_token(token):
    """The Estimate Room whose abbreviation IS the token (MB → Master
    Bedroom), using the same room_abbr the code generator uses — one
    grammar, both directions. Unknown token refuses with the valid list."""
    from mallet_estimator.estimator import room_abbr
    rooms = frappe.get_all("Estimate Room", pluck="name")
    by_abbr = {}
    for r in rooms:
        by_abbr.setdefault(room_abbr(r), r)
    room = by_abbr.get((token or "").upper())
    if not room:
        frappe.throw(_("No room matches token {0}. Valid room tokens: {1}").format(
            token, ", ".join(f"{a} ({r})" for a, r in sorted(by_abbr.items()))))
    return room


def _create_sku_from_component(project, tail):
    """Create an Estimate SKU from a SketchUp component tail (MB_WAR_OPT.1):
    first _-token names the room, the rest is the article. The code is kept
    VERBATIM (auto_name off) so the component name and sku_code never drift
    apart — the whole point of the convention (execution/DESIGN.md §1)."""
    customer, display, ci = _project_customer(project)
    parts = str(tail or "").split("_", 1)
    if len(parts) < 2 or not parts[1]:
        frappe.throw(_("Component name {0} must read ROOM_ARTICLE (e.g. MB_WAR).").format(tail))
    room = _room_for_token(parts[0])
    doc = frappe.new_doc("Estimate SKU")
    doc.project = project
    doc.customer = customer
    doc.room = room
    doc.article_name = parts[1].replace("_", " ")
    doc.auto_name = 0
    doc.sku_code = tail if tail.upper().startswith(ci + "_") else f"{ci}_{tail}"
    doc.insert()
    return doc


@frappe.whitelist()
def attach_sku_image(sku, filename, filedata, project=None):
    """Attach a rendered view (base64 PNG) as the SKU's article image — the
    client estimate prints article_image, so the concept picture the customer
    sees IS the current model, never a stale render. Same address resolution
    as the CSV push (name → binding initials + tail); replaces the previous
    image on every push."""
    import base64
    doc = _find_sku(sku)
    if not doc and project:
        _, _, ci = _project_customer(project)
        if not sku.upper().startswith(ci + "_"):
            doc = _find_sku(f"{ci}_{sku}")
    if not doc:
        doc = _resolve_sku(sku)   # throws with the did-you-mean refusal
    doc.check_permission("write")
    content = base64.b64decode(filedata)
    if len(content) > 8 * 1024 * 1024:
        frappe.throw(_("Image too large ({0} KB) — the iso render should be well under 8 MB.")
                     .format(len(content) // 1024))
    f = frappe.get_doc({
        "doctype": "File",
        "file_name": filename or f"{doc.name}_iso.png",
        "attached_to_doctype": "Estimate SKU",
        "attached_to_name": doc.name,
        "attached_to_field": "article_image",
        "is_private": 0,          # article_image prints on the client estimate
        "content": content,
    }).insert(ignore_permissions=True)
    doc.db_set("article_image", f.file_url)
    return {"sku": doc.name, "sku_code": doc.get("sku_code"), "file_url": f.file_url}


@frappe.whitelist()
def list_projects():
    """Open Projects with their customers, for the plugin's model-binding
    picker. Select-only by design: clients and projects are created in the
    lead/opportunity phase, never from SketchUp."""
    frappe.has_permission("Project", "read", throw=True)
    out = []
    for p in frappe.get_all("Project", filters={"status": "Open"},
                            fields=["name", "project_name", "customer"],
                            order_by="modified desc", limit_page_length=50):
        display = initials = ""
        if p.customer:
            from mallet_estimator.estimator import customer_initials
            display = frappe.db.get_value("Customer", p.customer, "customer_name") or p.customer
            initials = customer_initials(display)
        out.append({"project": p.name, "title": p.project_name or p.name,
                    "customer": p.customer, "customer_name": display,
                    "initials": initials})
    return out


@frappe.whitelist()
def import_parts_csv(sku, csv_content, filename=None, project=None, create_if_missing=0):
    """Attach an OpenCutList Part List CSV to the SKU, switch it to CSV-Nest
    mode, run the import (nesting, décor slots, ops, costing) and return the
    result the caller needs to display: nest details, part-list-vs-views
    issues, unpriced materials, and the client totals. `sku` may be the
    document name, the sku_code, or — when `project` carries the SketchUp
    file's binding — a component tail (MB_WAR): resolution tries the exact
    address, then the binding's initials + tail, and with `create_if_missing`
    finally CREATES the SKU on the bound project (execution/DESIGN.md §1)."""
    doc = _find_sku(sku)
    if not doc and project:
        _, _, ci = _project_customer(project)
        if not sku.upper().startswith(ci + "_"):
            doc = _find_sku(f"{ci}_{sku}")
        if not doc and frappe.utils.cint(create_if_missing):
            doc = _create_sku_from_component(project, sku)
    if not doc:
        doc = _resolve_sku(sku)   # throws with the did-you-mean refusal
    doc.check_permission("write")
    if doc.get("rates_frozen"):
        frappe.throw(_("Rates are frozen (quoted) — amend/cancel the Estimate first."))
    if not (csv_content or "").strip():
        frappe.throw(_("csv_content is empty."))

    f = frappe.get_doc({
        "doctype": "File",
        "file_name": filename or f"{doc.name}_partlist.csv",
        "attached_to_doctype": "Estimate SKU",
        "attached_to_name": doc.name,
        "attached_to_field": "parts_csv",
        "is_private": 1,
        "content": csv_content,
    }).insert(ignore_permissions=True)

    doc.estimation_mode = "CSV-Nest"
    doc.parts_csv = f.file_url
    doc.save()

    drivers = {}
    try:
        drivers = json.loads(doc.import_drivers or "{}") or {}
    except Exception:
        pass
    return {
        "sku": doc.name,
        "sku_code": doc.get("sku_code"),
        "nest": drivers.get("__nest__") or {},
        "issues": drivers.get("__issues__") or [],
        "unpriced": doc.get("unpriced_materials") or "",
        "material_cost": doc.get("material_cost"),
        "client_total": doc.get("client_total"),
        "est_days": doc.get("est_days"),
    }


@frappe.whitelist()
def attach_sku_file(sku, fieldname, file_url):
    """Attach/replace one of an SKU's source files from the Estimate's
    per-SKU files panel; saving runs the normal import pipeline."""
    if fieldname not in ("parts_csv", "views_pdf"):
        frappe.throw(_("fieldname must be parts_csv or views_pdf"))
    doc = frappe.get_doc("Estimate SKU", sku)
    doc.check_permission("write")
    if doc.get("rates_frozen"):
        frappe.throw(_("Rates are frozen (quoted) — amend/cancel the Estimate first."))
    doc.set(fieldname, file_url)
    if fieldname == "parts_csv" and not doc.get("estimation_mode"):
        doc.estimation_mode = "CSV-Nest"
    for fname in frappe.get_all("File", filters={"file_url": file_url}, pluck="name"):
        frappe.db.set_value("File", fname, {
            "attached_to_doctype": "Estimate SKU",
            "attached_to_name": doc.name,
            "attached_to_field": fieldname,
        }, update_modified=False)
    doc.save()
    return {"sku": doc.name, "client_total": doc.get("client_total"),
            "unpriced": doc.get("unpriced_materials") or ""}


@frappe.whitelist()
def get_sku_context(sku):
    """Everything the SketchUp side needs to set a model up for this SKU:
    identity (customer/project/room/code), the décor slot map (which generic
    codes to paint with), current material lines with rates, and state flags.
    Read-only. `sku` may be the document name or the sku_code."""
    doc = _resolve_sku(sku)
    doc.check_permission("read")
    return {
        "sku": doc.name,
        "sku_code": doc.get("sku_code"),
        "article_name": doc.get("article_name"),
        "customer": doc.get("customer"),
        "project": doc.get("project"),
        "room": doc.get("room"),
        "estimation_mode": doc.get("estimation_mode"),
        "rates_frozen": bool(doc.get("rates_frozen")),
        "outer_mm": {"w": doc.get("outer_w"), "d": doc.get("outer_d"), "h": doc.get("outer_h")},
        "decor_map": [
            {"slot": r.get("slot"), "domain": r.get("domain") or "Laminate",
             "brand": r.get("brand"), "code": r.get("code"), "name": r.get("decor_name"),
             "short": r.get("short")}
            for r in list(doc.get("sku_decors") or [])
        ] + [
            {"slot": r.get("slot"), "domain": "Edge Band", "brand": r.get("brand"),
             "code": r.get("code"), "name": r.get("decor_name"), "short": r.get("short")}
            for r in list(doc.get("sku_decor_edges") or [])
        ],
        "materials": [
            {"material": m.get("material"), "item": m.get("item"), "qty": m.get("qty"),
             "uom": m.get("uom"), "rate": m.get("unit_cost"), "manual": bool(m.get("is_manual"))}
            for m in (doc.get("materials") or [])
        ],
        "client_total": doc.get("client_total"),
    }


@frappe.whitelist()
def apply_decor(sku):
    """Re-point every generic line at the décor now assigned to it, and report
    how many are still generic — the 'did it actually map?' answer.

    The grouped board this used to return is gone; the material lines are an
    ordinary grid again, so the caller reloads the form and reads them there.
    What comes back is the one number the button was pressed to find out."""
    doc = frappe.get_doc("Estimate SKU", sku)
    doc.check_permission("write")
    if doc.get("rates_frozen"):
        frappe.throw(_("{0} is frozen (quoted on an approved estimate) — cancel and "
                       "amend that estimate to change its lines.").format(sku))
    doc.save()  # apply_decor_map runs inside the ordinary validate pipeline
    unmapped = sum(1 for m in (doc.materials or [])
                   if "NOT MAPPED" in str(m.get("remarks") or ""))
    return {"applied": 1, "unmapped": unmapped, "lines": len(doc.materials or [])}


# ---------------------------------------------------------------------------
# The cost card — everything the SketchUp plugin needs to price a SKU on the
# spot, with ERP as the only authority for money.
#
#   GET /api/method/mallet_estimator.api.cost_card
#       ?codes=SG_PLY_V1_a_b_18mm,SG_LAM_V1_1mm_a_a,EB_...
#
# Amit, 2026-08-22, as the rule: "always pull data of cost for labor and
# material from erp ... plugin own cost data which is material linked or part
# linked should get overriden ... i should clearly know from where cost data
# is coming erp or plugin as plugin also have capability to store material
# cost data."
#
# So every number here carries its own provenance, and the envelope is stamped
# as ERP's. The plugin overrides its stored rates with these and shows the
# badge; anything ERP could not price comes back with source "unset" and rate
# 0 rather than silently letting the plugin's own figure through — an estimate
# quoted from the wrong price list is worse than one that says it is missing.
#
# The split of responsibility, in one line: ERP owns the masters — the 17
# operations, their standard minutes, the workstation hour rates and the
# material prices — and the MODEL owns the quantities.
# ---------------------------------------------------------------------------


def _op_minutes(op_name, default):
    """Standard minutes for one operation.

    The Operation master wins: CLAUDE.md makes mallet_min_per_unit the source
    of truth, and the dict in estimator.py is the seed that populated it. A
    tuned number in ERP must reach the plugin, or the two quote differently
    for the same wardrobe.
    """
    if not frappe.db.exists("DocType", "Operation"):
        return float(default), "code default"
    if not frappe.get_meta("Operation").has_field("mallet_min_per_unit"):
        return float(default), "code default"
    v = frappe.db.get_value("Operation", op_name, "mallet_min_per_unit")
    if v in (None, "", 0):
        return float(default), "code default"
    return float(v), "erp:Operation"


@frappe.whitelist()
def cost_card(codes=None, create_missing=0):
    """Labour and material rates for the plugin, live from this bench.

    create_missing=1 CREATES an Item for any OpenCutList code ERP has never
    seen, instead of reporting it absent. Amit, 2026-08-22: "ADD material TO
    ERP." That is the right direction — ERP is the master, so a material the
    model knows about and ERP does not is a gap in ERP, not a reason to quote
    from the plugin's own figure.

    What it creates carries NO rate. ensure_material_item sets the group, the
    stock and purchase UOMs and the dimensions; pricing stays a human act on
    the Estimation (Assumed) price list, exactly as it does for every other
    material. So a newly created line comes back quotable=false with source
    erp:unset — visible, named, and waiting for a rate rather than silently
    carrying one from the wrong place.
    """
    from mallet_estimator import estimator, inventory

    settings = frappe.get_single("Estimate Settings")
    rates = estimator.live_workstation_rates(settings)

    stations = []
    for name, r in sorted(rates.items()):
        stations.append({
            "name": name,
            "hour_rate": round(float(r.get("net_hr") or 0), 2),
            "components": [[c, round(float(v), 2)] for c, v in (r.get("components") or [])],
            "source": "erp:Workstation",
        })
    by_station = {s["name"]: s["hour_rate"] for s in stations}

    # ALL SEVENTEEN, in process order — pasting through installation, nothing
    # dropped. Amit, 2026-08-22: "keep all 17 operations as is . no drop."
    # Python preserves insertion order, and the dict is written in the order
    # the shop actually works, so seq is simply its position.
    operations = []
    for seq, (op, std) in enumerate(estimator.OPERATION_STANDARDS.items(), start=1):
        ws = estimator.OPERATION_WORKSTATION.get(op, "")
        mins, min_src = _op_minutes(op, std["min_per_unit"])
        operations.append({
            "seq": seq,
            "name": op,
            "workstation": ws,
            "hour_rate": by_station.get(ws, 0.0),
            "qty_source": std["qty_source"],
            "min_per_unit": mins,
            "min_source": min_src,
            "rate_source": "erp:Workstation" if ws in by_station else "unset",
        })

    materials = []
    created = []
    want_create = str(create_missing) not in ("", "0", "False", "false", "None")
    for code in _split_codes(codes):
        item = _item_for_code(code)
        if not item and want_create and inventory.is_material_code(code):
            # Created with no rate: the Item is a fact about what the model
            # uses, the rate is a decision somebody has to make. Guarded on
            # is_material_code so a typo in a component name cannot mint an
            # Item — the grammar is the gate.
            try:
                item, _rate, _src = inventory.ensure_material_item(code)
                created.append(item)
            except Exception as exc:
                frappe.log_error(frappe.get_traceback(), f"cost_card create {code}")
                materials.append({"code": code, "item_code": "", "base_rate": 0.0,
                                  "gst_pct": 0.0, "landed_rate": 0.0,
                                  "source": f"could not create: {exc}",
                                  "quotable": False})
                continue
        if not item:
            # Named rather than skipped: a material the plugin priced itself
            # and ERP has never heard of is exactly the case a person needs to
            # see, not the case to paper over.
            materials.append({"code": code, "item_code": "", "base_rate": 0.0,
                              "gst_pct": 0.0, "landed_rate": 0.0,
                              "source": "not in erp", "quotable": False})
            continue
        landed, base, gst, src = inventory.landed_rate(item)
        materials.append({
            "code": code,
            "item_code": item,
            "uom": frappe.db.get_value("Item", item, "stock_uom") or "",
            "base_rate": round(float(base), 4),
            "gst_pct": round(float(gst), 2),
            "landed_rate": round(float(landed), 4),
            "source": f"erp:{src}",
            # The same gate the ERP estimate uses: rate 0 with source 'unset'
            # is NOT quotable, and saying so is the whole point of sending the
            # source along with the number.
            "quotable": bool(base) and src != "unset",
        })

    return {
        # The envelope says who is speaking. The plugin shows this against
        # every line it overrode.
        "authority": "erp",
        "site": frappe.local.site,
        "as_of": str(frappe.utils.now()),
        "price_list": inventory.ESTIMATION_PRICE_LIST,
        "rates_are": "post-tax (landed = base + GST)",
        "productive_min_per_day": estimator.PRODUCTIVE_MIN_PER_DAY,
        "workstations": stations,
        "operations": operations,
        "materials": materials,
        # Items this call MINTED. Named so the plugin can say "3 new materials
        # went to ERP and need a rate" rather than leaving somebody to wonder
        # why a line is suddenly not quotable.
        "created_items": created,
        # The assemblies line, declared rather than hardcoded on the plugin
        # side. Amit, 2026-08-22: "the component which starts with ASMBL is the
        # assembly ... aggregate number of ASMBL components into that line and
        # then let me modify how much time assembly can take."
        "assembly_rule": {
            "operation": "Assembly",
            "component_prefix": "ASMBL",
            "counts": "distinct components whose name starts with the prefix",
            "min_per_unit": _op_minutes("Assembly",
                                        estimator.OPERATION_STANDARDS["Assembly"]["min_per_unit"])[0],
            "editable": True,
        },
        # Labour only, and explicitly so: this is a gauge to walk a client
        # through while designing, not a quotation. Amit: "it will be pure
        # labor and no transport installation etc will come over there."
        "excludes": ["transport trip charges", "allowances", "markup",
                     "installation charges beyond the operation minutes"],
        # Amit, 2026-08-22: "wastage treatment - charge full board." Material
        # is priced by the BOARDS consumed, not by the area the parts occupy —
        # the offcut is bought and paid for whether or not it is used. On the
        # plugin side that is OCL's total_cost, never total_used_cost.
        "wastage": "full board — price whole boards consumed, not used area",
    }


def _split_codes(codes):
    if not codes:
        return []
    if isinstance(codes, str):
        try:
            parsed = json.loads(codes)
            if isinstance(parsed, list):
                return [str(c).strip() for c in parsed if str(c).strip()]
        except Exception:
            pass
        return [c.strip() for c in codes.split(",") if c.strip()]
    return [str(c).strip() for c in codes if str(c).strip()]


def _item_for_code(code):
    """An OCL code, or an Item code, or nothing.

    The plugin speaks OpenCutList grammar (SG_PLY_V1_...); ERP stores that on
    Item.mallet_oc_code and usually names the Item the same. Both are tried so
    a caller does not have to know which world its string came from.
    """
    if frappe.db.exists("Item", code):
        return code
    if frappe.get_meta("Item").has_field("mallet_oc_code"):
        hit = frappe.db.get_value("Item", {"mallet_oc_code": code}, "name")
        if hit:
            return hit
    return None


# ---------------------------------------------------------------------------
# The on-the-fly estimate — priced entirely by ERP, from the part list the
# SketchUp model already knows how to produce.
#
#   POST /api/method/mallet_estimator.api.estimate_preview
#        {csv_content, assembly_min?, assembly_count?}
#
# WHY THE SERVER AND NOT THE PLUGIN. The plugin could derive its own labour
# quantities in Ruby, and then there would be two implementations of the same
# rules drifting apart the moment either changed. Everything needed already
# lives here: opencutlist.aggregate turns the CSV into material lines and
# operation drivers, estimate_pdf.operation_quantities turns those into the
# seventeen quantities, and inventory.landed_rate prices them. The plugin
# posts the CSV it already builds for import_parts_csv and renders the answer.
#
# Amit, 2026-08-22: "so erp cost data is base for estimating in plugin as
# well" and "sketchup model plugin will give quick printable estimate on the
# fly". Nothing here is stored, nothing is created, no SKU is touched — it is
# a quotation-shaped answer to "what would this cost", computed the same way
# the real estimate computes it.
# ---------------------------------------------------------------------------


@frappe.whitelist()
def estimate_preview(csv_content, assembly_min=None, assembly_count=None,
                     create_missing=0):
    """Material + labour for one SKU, priced from ERP. Saves nothing."""
    from mallet_estimator import (estimate_pdf, estimator, inventory, nest_import,
                                  nesting, opencutlist)

    rows = opencutlist.parse_opencutlist_csv(csv_content or "")
    if not rows:
        frappe.throw(_("No part rows in that CSV."))

    # THE NESTING ENGINE, not the area one. The first version of this called
    # opencutlist.aggregate(), which divides "Area - final" by the sheet area
    # — and the plugin's part-list CSV has no such column, so every sheet
    # measured 0 m², every board count came out 0, and the screen showed four
    # material lines priced at nothing while OpenCutList's own table beside it
    # said 2 + 2 boards. Only hardware looked right, because hardware is
    # COUNTED rather than measured.
    #
    # aggregate() is for the OCL estimate PDF export. The part-list CSV is
    # nest_import's input, and nest_import is what the real CSV-Nest import
    # runs — the path whose material numbers Amit already trusts. Same
    # functions here, same answers, nothing saved.
    ply, lam, edges, hw, banded_edges, faces = nest_import.collect(rows)
    if not ply:
        frappe.throw(_("No sheet-good parts found in the CSV."))

    SQFT_MM2 = 92903.04
    sheet_sqft = nesting.SHEET_L * nesting.SHEET_W / SQFT_MM2
    lines, panel_sheets = [], {}

    # Sheets: really packed, so the count is the count the shop will buy.
    # Wastage is inside it by construction — whole boards in, parts out —
    # which is the rule Amit set: "wastage treatment - charge full board."
    for (code, th), parts in sorted(ply.items()):
        r = nesting.pack_sheets(parts, kerf=nest_import.KERF_MM,
                                trim=nest_import.TRIM_MM, allow_rotate=False)
        panel_sheets[(code, th)] = r["sheets"]
        used_sqft = sum(l * w for (l, w) in parts) / SQFT_MM2
        waste_sqft = max(0.0, r["sheets"] * sheet_sqft - used_sqft)
        lines.append({
            "kind": "sheet", "material": code, "thickness": th,
            "qty": r["sheets"], "uom": "Nos", "rate_factor": 1,
            "desc": "%s — %d parts → %d sheet(s) (%.0f%% used, waste %.1f sqft)"
                    % (code, len(parts), r["sheets"], r["utilization"] * 100, waste_sqft),
        })

    # Laminate follows the BOARD, not its own nest: it is pressed onto the
    # whole panel before the saw runs, so it is one sheet per laminated face.
    for code, sheets in sorted(nesting.laminate_from_panels(panel_sheets, faces).items()):
        if not sheets:
            continue
        lines.append({
            "kind": "laminate", "material": code, "thickness": 0,
            "qty": sheets, "uom": "Nos", "rate_factor": 1,
            "desc": "%s — %d sheet(s), one per board face" % (code, sheets),
        })

    # Edge banding is stocked in metres and BOUGHT in whole rolls, so the rate
    # has to be multiplied by the roll length or the line is out by 50x.
    for code, meters in sorted(edges.items()):
        rolls = nesting.edge_rolls(meters)
        lines.append({
            "kind": "edge", "material": code, "thickness": 0,
            "qty": rolls, "uom": "Roll", "rate_factor": inventory.EDGE_ROLL_METERS,
            "desc": "%s — %.2f m banding → %d roll(s) of %g m"
                    % (code, meters, rolls, inventory.EDGE_ROLL_METERS),
        })

    for h in hw:
        lines.append({
            "kind": "hardware", "material": h["code"], "thickness": 0,
            "qty": h["qty"], "uom": "Nos", "rate_factor": 1,
            "category": h.get("category"),
            "desc": "%s — %d nos" % (h["code"], h["qty"]),
        })

    # Edge Banding's operation qty is the banded-EDGE count, which is what
    # nest_import passes as part_count. Matching it keeps the two paths from
    # quoting different labour for the same model.
    part_count = banded_edges

    card = cost_card(codes=[opencutlist.item_code_for(l) for l in lines],
                     create_missing=create_missing)
    rate_by_code = {m["code"]: m for m in card["materials"]}

    material_rows, material_total, unpriced = [], 0.0, 0
    for l in lines:
        code = opencutlist.item_code_for(l)
        m = rate_by_code.get(code, {})
        rate = float(m.get("landed_rate") or 0) * (l.get("rate_factor") or 1)
        amount = rate * float(l["qty"] or 0)
        if not m.get("quotable"):
            unpriced += 1
        material_rows.append({
            "kind": l["kind"], "code": code, "desc": l["desc"],
            "qty": l["qty"], "uom": l["uom"],
            "rate": rate, "amount": round(amount, 2),
            "source": m.get("source", "not in erp"),
            "quotable": bool(m.get("quotable")),
        })
        material_total += amount

    # ---- labour, all seventeen --------------------------------------------
    # operation_quantities() reads m["name"]; aggregate() calls that key
    # "material". They do not share a shape and never did — every other caller
    # adapts before calling (nest_import builds mats_shape, estimate_sku builds
    # materials), and so does this one. Passing aggregate()'s lines straight in
    # raised KeyError: 'name' the moment a hardware line existed, which is the
    # first thing _hw touches.
    #
    # The hardware name matters beyond being present: _hw matches SUBSTRINGS
    # against it ("hinge", "rail", "minifix"), and "Assembly" is 1 + rails. The
    # OCL code carries that word — HWD_Rail lowercases to "hwd_rail" — which is
    # the same thing nest_import relies on when it passes a category or a code.
    # Hardware carries its CATEGORY here, exactly as nest_import's mats_shape
    # does: _hw matches substrings ("hinge", "rail", "minifix") and a real
    # designation like HWD_AH_SC_0 contains none of them, so pricing the
    # labour off the raw code would silently count zero hinges.
    shaped = [{"name": (l.get("category") or l["material"]) if l["kind"] == "hardware"
                       else l["material"],
               "kind": l["kind"],
               "thickness": l.get("thickness") or 0, "qty": l["qty"]}
              for l in lines]
    qty = estimate_pdf.operation_quantities(shaped, part_count)

    # THE ASSEMBLIES LINE. The default rule is 1 + drawer rails, which is a
    # guess at how many things get assembled. The MODEL knows: Amit, 2026-08-22,
    # "the component which starts with ASMBL is the assembly ... aggregate
    # number of ASMBL components into that line and then let me modify how much
    # time assembly can take." A count supplied by the plugin wins; failing
    # that we count ASMBL part names in the CSV itself, so the rule holds even
    # for a caller that does not send one.
    #
    # A count of ZERO is not a count — it is a model with no ASMBL components
    # in it, which is exactly what a first run looks like before anyone has
    # adopted the naming. Treating it as an override would badge the line
    # "plugin:ASMBL count" and then, because the override is skipped, price it
    # off ERP's own rule anyway: the screen would read "0 assemblies (plugin:
    # ASMBL count)" directly above an Assembly line costed for one. So zero
    # falls through to the ERP rule and SAYS it did.
    counted = _asmbl_count(rows)
    sent = None
    if assembly_count not in (None, ""):
        sent = int(float(assembly_count))
    if sent:
        counted = sent
        assembly_source = "plugin:ASMBL count"
    elif sent == 0:
        assembly_source = "erp:1 + drawer rails (no ASMBL component in model)"
        counted = 0
    elif counted:
        assembly_source = "csv:ASMBL count"
    else:
        assembly_source = "erp:1 + drawer rails"
    if counted:
        for op in ("Assembly", "Disassembly", "Packing", "Loading", "Transport",
                   "Unloading", "Assembly (on-site)", "Installation"):
            # These all follow the assembly count in operation_quantities; if
            # the count changes, they change with it or the chain lies.
            qty[op] = counted

    ws_rate = {s["name"]: s["hour_rate"] for s in card["workstations"]}
    labour_rows, labour_total = [], 0.0
    for op in card["operations"]:
        name = op["name"]
        mins = float(op["min_per_unit"])
        if name == "Assembly" and assembly_min not in (None, ""):
            mins = float(assembly_min)
        q = float(qty.get(name, 0) or 0)
        hours = (q * mins) / 60.0
        rate = float(ws_rate.get(op["workstation"], 0))
        amount = hours * rate
        labour_rows.append({
            "seq": op["seq"], "name": name, "workstation": op["workstation"],
            "qty": q, "min_per_unit": mins, "hours": round(hours, 3),
            "hour_rate": rate, "amount": round(amount, 2),
            "min_source": ("plugin:edited" if (name == "Assembly" and
                                               assembly_min not in (None, ""))
                           else op["min_source"]),
            "rate_source": op["rate_source"],
        })
        labour_total += amount

    return {
        "authority": "erp",
        "site": frappe.local.site,
        "as_of": str(frappe.utils.now()),
        "price_list": card["price_list"],
        "rates_are": card["rates_are"],
        "wastage": card["wastage"],
        "parts": len(rows),
        "panels": part_count,
        # What the Assembly line was actually PRICED at, not what was sent.
        # These differ whenever the fallback ran, and the screen shows this
        # number beside that line — printing the input there would contradict
        # the row underneath it.
        "assembly_count": int(qty.get("Assembly", counted) or 0),
        "assembly_source": assembly_source,
        "materials": material_rows,
        "labour": labour_rows,
        "material_total": round(material_total, 2),
        "labour_total": round(labour_total, 2),
        "total": round(material_total + labour_total, 2),
        # Loud on purpose. A total that quietly omits three unpriced boards is
        # worse than no total: it looks like an answer.
        "unpriced_lines": unpriced,
        "created_items": card.get("created_items", []),
        "excludes": card["excludes"],
    }


def _asmbl_count(rows):
    """How many DISTINCT assemblies the model carries.

    Distinct, not total: two copies of one assembly are two units of the same
    thing and both are counted, but the same component appearing on twenty
    part rows is still one assembly. Case-insensitive because SketchUp names
    are typed by people.
    """
    seen = set()
    for r in rows:
        for key in ("name", "designation", "part", "Name", "Designation"):
            v = str((r.get(key) if hasattr(r, "get") else "") or "").strip()
            if v.upper().startswith("ASMBL"):
                seen.add(v.upper())
                break
    return len(seen)
