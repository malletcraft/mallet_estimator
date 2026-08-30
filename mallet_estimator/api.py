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
import re

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
    def fallback():
        # A zero seed is not a standard that failed to arrive — it is the
        # absence of one. Amit, 2026-08-23, on Miscellaneous - extra: "seed
        # default - why this is not ERP Operation?" The Operation IS in ERP;
        # what ERP has no opinion about is how long it takes, because the row
        # exists precisely for work the other sixteen do not name. Saying
        # "seed default" there blamed a fallback for a number nobody ever set.
        return (float(default), "code default") if default else (0.0, "no standard")

    if not frappe.db.exists("DocType", "Operation"):
        return fallback()
    if not frappe.get_meta("Operation").has_field("mallet_min_per_unit"):
        return fallback()
    v = frappe.db.get_value("Operation", op_name, "mallet_min_per_unit")
    if v in (None, "", 0):
        return fallback()
    return float(v), "erp:Operation"


@frappe.whitelist(methods=["POST"])
def create_materials(codes=None):
    """Create Items for OpenCutList codes ERP has never heard of. Nothing else.

    Amit, 2026-08-29: "give me a button to create material in erp. dont
    directly create on run. need bot button." cost_card(create_missing=1)
    already did the creating, but only as a rider on a full re-price, which
    ties two unrelated decisions together: a person who wants to mint one
    Item has to re-run the whole estimate to do it, and the run they get back
    is the answer to a question they did not ask.

    So this endpoint does the one thing. It scans no model, prices nothing,
    and returns exactly what happened to each code — which is what a
    per-line button needs in order to say anything truthful afterwards.

    POST-ONLY, and that is load-bearing rather than tidy. Frappe rolls back
    on a GET: the insert happens, everything later in the request sees it,
    the reply says created — and it is gone when the request ends. Proved on
    mcft-stg, 2026-08-29. A creating endpoint reachable over GET is a
    success message with no Item behind it.

    Never a rate. The Item is a fact about what the model uses; the rate is a
    decision a person makes, and no assistant identity may write one.
    """
    # Imported HERE, not at module scope, because that is how every other
    # whitelisted method in this file does it — api.py loads on every request
    # and the heavy siblings stay out of import time. Written the module-level
    # way it threw NameError on the first live call, green CI and green deploy
    # notwithstanding: nothing in the pure suite imports frappe, so nothing in
    # it could ever have executed this line.
    from mallet_estimator import decor, inventory

    out = {"created": [], "existed": [], "failed": {}}
    for code in _split_codes(codes):
        # A PLACEHOLDER IS NOT A MATERIAL. A code still carrying its décor
        # slot letters — SG_LAM_V0_16mm_a_a, EB_PVC_EX_b — means nothing has
        # said what that laminate IS yet; the slot is resolved from the SKU's
        # décor map, and until then there is no purchasing identity to create.
        #
        # This button minted two of them within hours of shipping
        # (SG_LAM_V1_16mm_a_c and _c_a, 2026-08-29, from my own test), which
        # is exactly the master-data pollution patches/collapse_board_item_codes
        # was written to clear up.
        if decor.trailing_slots(code):
            out["failed"][code] = ("unresolved décor slot — set the décor on "
                                   "the SKU first, then this becomes a real code")
            continue
        if not inventory.is_material_code(code):
            # The grammar is the gate. A component name that slipped into the
            # code column must not be able to mint an Item just because
            # somebody pressed a button next to it.
            out["failed"][code] = "not a material code"
            continue
        existing = _item_for_code(code)
        if existing:
            out["existed"].append(existing)
            continue
        try:
            item, _rate, _src = inventory.ensure_material_item(code)
            # Committed per code, deliberately. A batch of fifteen where the
            # twelfth throws should leave eleven Items behind, not none — the
            # person pressed "create all" to make progress, and losing the
            # successes to one bad code is the opposite of that.
            frappe.db.commit()
            out["created"].append(item)
        except Exception as exc:
            frappe.db.rollback()
            frappe.log_error(frappe.get_traceback(), "create_materials %s" % code)
            out["failed"][code] = str(exc)
    return out


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
        # WHERE THIS RATE CAME FROM, honestly. This used to hardcode
        # "erp:Workstation" for every station, including the ones whose master
        # carries no operating-cost rows at all and whose rate is therefore
        # computed from Estimate Settings. A label that says the same thing
        # whatever happened is not a source, and it lied in the one direction
        # that matters: a station nobody has costed looked exactly like a
        # station somebody had.
        src = r.get("rate_source") or "erp:Workstation"
        stations.append({
            "name": name,
            "hour_rate": round(float(r.get("net_hr") or 0), 2),
            "components": [[c, round(float(v), 2)] for c, v in (r.get("components") or [])],
            "source": src,
            "rate_source": src,
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
            # Factory / logistics / on-site. A grouping for the reader, not a
            # rate: Loading is logistics and is still charged at a factory
            # workstation.
            "zone": estimator.OPERATION_ZONE.get(op, ""),
        })
        # THE HARDWARE CHILDREN ARE OPERATIONS TOO, and this list was leaving
        # them out. Amit, 2026-08-25: "mcft plugin is not showing hardware,
        # list correct on operation side."
        #
        # cost_card walks OPERATION_STANDARDS, which holds the seventeen. The
        # six Install <type> masters live in HARDWARE_INSTALL_TYPES, so
        # anything reading the operation side saw a parent with a standard
        # time and no sign that six real Operations sit under it — including
        # the settings page, which had to be handed them through a separate
        # array to show them at all.
        #
        # They carry `parent`, and estimate_preview skips any row that has
        # one: the estimate hangs them off the parent as children, so a
        # top-level row here as well would price every fitting twice.
        if op == estimator.HARDWARE_PARENT:
            for kind, label in estimator.HARDWARE_INSTALL_TYPES:
                child = estimator.hardware_operation(kind)
                cws = estimator.OPERATION_WORKSTATION.get(child, ws)
                cmins, csrc = _op_minutes(
                    child, estimator.HARDWARE_STANDARDS.get(kind, 0))
                operations.append({
                    "seq": seq,
                    "parent": op,
                    "split_key": kind,
                    "name": child,
                    "workstation": cws,
                    "hour_rate": by_station.get(cws, 0.0),
                    "qty_source": "hardware:%s" % kind,
                    "min_per_unit": cmins,
                    "min_source": csrc,
                    "rate_source": "erp:Workstation" if cws in by_station else "unset",
                    "zone": estimator.OPERATION_ZONE.get(child, ""),
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
                # COMMITTED EXPLICITLY, because otherwise whether the Item
                # survives depends on the HTTP VERB. Frappe rolls a GET back:
                # the insert happens, everything later in the request sees it,
                # the reply says created_items — and it is gone when the
                # request ends. Found 2026-08-29 probing this over GET, which
                # reported item_code populated and source erp:unset for three
                # materials that did not exist a second later.
                #
                # The plugin POSTs, so the button was never broken. That is
                # luck, not design. A write that persists on one verb and
                # vanishes on another, reporting success either way, is the
                # same failure this app has now met three times, and it should
                # not be waiting for whoever next calls this from a browser.
                frappe.db.commit()
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
        # A RULE, Amit 2026-08-22: "never mention about any markup or profit
        # margin on any printable document." This list is printed, so the word
        # goes — and naming it in a list of exclusions is the worst place for
        # it, since it tells a client a markup exists and that this number is
        # not the whole of it. What is excluded here is stated as WORK not
        # priced, which is the true and sufficient statement.
        "excludes": ["transport trip charges", "allowances",
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


# Amit, 2026-08-22: "all number should be rounded to one decimal like 3.333
# should 3.4" — 3.333 to one decimal is 3.3 by arithmetic, so what he described
# is not rounding but rounding UP. That is the right way round for a quote:
# every fraction of an hour lands on the shop's side of the line, never the
# client's, and a number can never read lower than the work it stands for.
def _up1(v):
    import math
    return math.ceil((float(v or 0)) * 10.0 - 1e-9) / 10.0


# Which of the seventeen a person may overrule, and in which column.
#
# Amit, 2026-08-22: "7. Grooving - quanity along with time will be decided by
# designer as article decide how many grooving are required ... 8 to 17 steps
# number should be editable for time as its carpenters judgment how much time
# it takes to that operation but quantity should not be editable."
#
# So: 1-6 are computed outright, 7 is the designer's call on both counts, and
# 8-17 take the carpenter's minutes over a quantity the model already knows.
# Install Hardware (9) keeps its computed quantity deliberately — it is
# hinges + rails + handles + shelf supports off the hardware lines, the rule
# stated earlier, and hand-editing it would silently unhook it from the model.
#
# Miscellaneous - extra (17) is the exception the rule above did not consider.
# Amit, 2026-08-23: "not able to key in quantity. so result will always be
# zero." It is right: the row exists to carry work the other sixteen do not
# name, so the model has nothing to infer a quantity FROM — its qty_source is
# "manual" and it comes back 0. A row whose quantity is fixed at zero can
# never add anything to an estimate no matter what minutes are typed beside
# it, which makes it decoration rather than a line.
#
# Transport (13) is the third. Amit, 2026-08-24: "Transport quantity should be
# editable as its related to number of trips so quantity here will be number of
# trips than the assemblies itself." A tempo does not care how many wardrobes
# are in it, and counting articles priced one trip per article — which is right
# only when a trip carries exactly one.
QTY_EDITABLE = {"Grooving", "Miscellaneous - extra", "Transport"}
MIN_EDITABLE_FROM_SEQ = 7          # Grooving onwards


def _decor_shorts_for_sku(sku):
    """({slot: short}, {slot: short}) for laminate and edge, off the SKU's map.

    The plugin's preview has no document of its own, so the décor map has to
    come from the SKU it is bound to. Reuses the SKU's own
    _decor_maps_from_table rather than re-reading the child tables here: the
    map is read in exactly one place, so the preview and the saved estimate
    cannot come to different conclusions about the same slot — which is the
    failure this whole audit keeps turning up.

    Anything missing — no sku, no permission, a SKU that has no map yet —
    returns empty maps, and the placeholder then stays a placeholder and is
    reported as unpriced. Never a guess.
    """
    from mallet_estimator import decor

    if not sku:
        return {}, {}
    try:
        doc = _find_sku(sku) or _resolve_sku(sku)
        doc.check_permission("read")
        lam, edge = doc._decor_maps_from_table()
    except Exception:
        # A preview must never fail because a binding is stale. It falls back
        # to unresolved, which is now loud rather than silently wrong.
        return {}, {}
    return ({k: decor.short_code(v) for k, v in lam.items() if decor.short_code(v)},
            {k: decor.short_code(v) for k, v in edge.items() if decor.short_code(v)})


@frappe.whitelist()
def estimate_preview(csv_content, assembly_min=None, assembly_count=None,
                     create_missing=0, overrides=None, hours_per_day=6,
                     assembly_counts=None, assembly_min_by_size=None,
                     misc_remarks=None, hardware_min_by_type=None, sku=None,
                     trip_qty=None, trip_rate=None):
    """Material + labour for one SKU, priced from ERP. Saves nothing."""
    from mallet_estimator import (decor, estimate_pdf, estimator, inventory,
                                  nest_import, nesting, opencutlist)

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

    # J1 — Fevicol and Abrotape, which are in no cut list because they are
    # DERIVED from how many boards go through the press. Amit, 2026-08-29:
    # "Need fevicol and abrotape logic in mcft plugin as well."
    #
    # They were missing here while the real estimate has always had them, so
    # the same model priced two ways and the CHEAPER way was the one on screen
    # in front of a client. The rule itself is not repeated here — it lives in
    # estimator and EstimateSKU.derive_joinery asks the same function — because
    # a plugin and a bench that disagree about one model is the specific
    # failure this project keeps paying for.
    _ply_sheets = sum(l["qty"] for l in lines if l["kind"] == "sheet")
    _lam_sheets = sum(l["qty"] for l in lines if l["kind"] == "laminate")
    _boards = estimator.joinery_boards(ply_qty=_ply_sheets, lam_qty=_lam_sheets)
    for _code, _qty, _uom, _note in estimator.joinery_lines(_boards):
        lines.append({
            "kind": "joinery", "material": _code, "thickness": 0,
            "qty": _qty, "uom": _uom,
            # Abrotape is quantified and PRICED per metre; the 20 m roll is a
            # purchasing fact the Item carries, not a factor to multiply here.
            # Getting that backwards is how edge banding was once out by 50x.
            "rate_factor": 1,
            "desc": "%s — %s" % (_code, _note),
        })

    # DÉCOR, RESOLVED — the abstract slot letters turned into the real
    # laminate and edge band, exactly as the saved Estimate SKU does it.
    #
    # Without this the preview priced the PLACEHOLDERS. On the real YS_MB_WAR
    # wardrobe (2026-08-30) that read ₹56,325 of material against the saved
    # estimate's ₹38,406 — 47% high — and reported unpriced_lines: 0, because
    # July placeholder Items happen to carry assumed rates and so look
    # perfectly healthy. Silent and wrong, on the screen Amit shares with a
    # client.
    #
    # The map lives on the SKU (sku_decors / sku_decor_edges), not the
    # project, so the plugin sends its bound SKU. Without one there is nothing
    # to resolve against and the placeholder stays — marked unquotable below,
    # because a slot nobody has filled in is not a price.
    lam_shorts, edge_shorts = _decor_shorts_for_sku(sku)
    for l in lines:
        if l["kind"] not in ("laminate", "edge"):
            continue
        shorts = edge_shorts if l["kind"] == "edge" else lam_shorts
        real, _slot = decor.substitute_real_code(l["material"], shorts)
        if real != l["material"]:
            l["desc"] = l["desc"].replace(l["material"], real)
            l["material"] = real

    # Edge Banding's operation qty is the banded-EDGE count, which is what
    # nest_import passes as part_count. Matching it keeps the two paths from
    # quoting different labour for the same model.
    part_count = banded_edges

    # inventory.item_code_for, NEVER a local rule: it is what every minted
    # Item obeys, so anything else prices the plugin against a code the bench
    # would not create.
    def _code_of(l):
        return decor.purchasing_code(l["material"], l.get("thickness") or 0, l["kind"])

    card = cost_card(codes=[_code_of(l) for l in lines],
                     create_missing=create_missing)
    rate_by_code = {m["code"]: m for m in card["materials"]}

    material_rows, material_total, unpriced = [], 0.0, 0
    for l in lines:
        code = _code_of(l)
        m = rate_by_code.get(code, {})
        rate = float(m.get("landed_rate") or 0) * (l.get("rate_factor") or 1)
        amount = rate * float(l["qty"] or 0)
        # AN UNRESOLVED PLACEHOLDER IS NEVER QUOTABLE, whatever rate an old
        # stub Item happens to carry. This is the half of the décor fix that
        # matters when resolution CANNOT happen — no SKU bound, or a slot with
        # no map row. Pricing it anyway is what made the wardrobe read 47%
        # high while claiming nothing was wrong.
        placeholder = bool(decor.trailing_slots(l["material"]))
        quotable = bool(m.get("quotable")) and not placeholder
        source = m.get("source", "not in erp")
        if placeholder:
            source = "décor not set — slot %s" % "/".join(decor.trailing_slots(l["material"]))
            rate, amount = 0.0, 0.0
        if not quotable:
            unpriced += 1
        material_rows.append({
            "kind": l["kind"], "code": code, "desc": l["desc"],
            "qty": l["qty"], "uom": l["uom"],
            "rate": rate, "amount": round(amount, 2),
            "source": source,
            "quotable": quotable,
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
    # Counts per size. The plugin sends them when it can see the model; the
    # CSV designations are the fallback; ERP's own rule is the last resort.
    if isinstance(assembly_counts, str):
        assembly_counts = json.loads(assembly_counts or "{}")
    sizes = dict(_asmbl_counts(rows))
    if assembly_counts:
        for k in ASSEMBLY_SIZES:
            if assembly_counts.get(k) not in (None, ""):
                sizes[k] = int(float(assembly_counts[k]))
        sizes["unsized"] = int(float(assembly_counts.get("unsized") or 0))
        assembly_source = "plugin:ASMBL size counts"
    counted = sum(sizes[k] for k in ASSEMBLY_SIZES)

    sent = None
    if assembly_count not in (None, ""):
        sent = int(float(assembly_count))
    if not assembly_counts:
        if sent:
            # An older plugin sends one total and no sizes. Treated as all
            # large, which is what it meant before sizes existed.
            counted = sent
            sizes = {"large": sent, "medium": 0, "small": 0, "unsized": sent}
            assembly_source = "plugin:ASMBL count"
        elif sent == 0:
            assembly_source = "erp:1 + drawer rails (no ASMBL component in model)"
            counted = 0
            sizes = {k: 0 for k in ASSEMBLY_SIZES}
            sizes["unsized"] = 0
        elif counted:
            assembly_source = "csv:ASMBL count"
        else:
            assembly_source = "erp:1 + drawer rails"

    # THE RULE ITSELF is estimate_pdf.apply_assembly_count, so the saved
    # Estimate SKU can apply the identical one. It lived here, which is why
    # the plugin honoured the model's ASMBL count and the document did not.
    estimate_pdf.apply_assembly_count(qty, counted, sizes["large"])

    # Per-operation overrides from the estimate screen: {"Grooving": {"qty": 4,
    # "min": 12}}. Refused rather than silently ignored where the column is not
    # editable — a number a person typed that does nothing is worse than an
    # error, because the total moves on without them.
    if isinstance(overrides, str):
        overrides = json.loads(overrides or "{}")
    overrides = overrides or {}

    # Every size starts at ERP's own Assembly standard and is overruled from
    # the screen. Seeding all three from one number is deliberate: the shop
    # has one std time today, and inventing three different ones here would be
    # putting numbers in Amit's mouth.
    _erp_asm = next((float(o["min_per_unit"]) for o in card["operations"]
                     if o["name"] == "Assembly"), 0.0)
    size_min = {k: _erp_asm for k in ASSEMBLY_SIZES}
    assembly_edited = False

    # The OLD single-number door still works, and now means "this many minutes
    # for every size". A plugin that has not learnt about sizes sends it, and
    # its estimate must not silently revert to ERP's standard.
    if assembly_min not in (None, ""):
        size_min = {k: float(assembly_min) for k in ASSEMBLY_SIZES}
        assembly_edited = True

    if isinstance(assembly_min_by_size, str):
        assembly_min_by_size = json.loads(assembly_min_by_size or "{}")
    for k in ASSEMBLY_SIZES:
        v = (assembly_min_by_size or {}).get(k)
        if v not in (None, ""):
            size_min[k] = float(v)
            assembly_edited = True
    assembly_min_used = dict(size_min)

    # --- Install Hardware, per fitting type --------------------------------
    #
    # The counts come from the SAME shaped materials operation_quantities was
    # given, so the children cannot disagree with the parent above them: both
    # are the one bucketing of the one list.
    hw_counts = {k: v for k, v in estimate_pdf.hardware_by_type(shaped).items()
                 if k in dict(estimator.HARDWARE_INSTALL_TYPES)}

    # Each type's standard comes from ITS OWN Operation master, not from a
    # number here. Amit, 2026-08-24: "you have workstation / operation master
    # data. use it." Assembly's three sizes deliberately share one seed because
    # the shop has one assembly standard; hardware is the opposite case — a
    # hinge and a shelf pin have never taken the same time, so each carries its
    # own and the code default is only what a fresh site starts from.
    hw_min, hw_min_source = {}, {}
    for kind, _label in estimator.HARDWARE_INSTALL_TYPES:
        op_name = estimator.hardware_operation(kind)
        m, src = _op_minutes(op_name, estimator.HARDWARE_STANDARDS.get(kind, 0))
        hw_min[kind] = m
        hw_min_source[kind] = src

    if isinstance(hardware_min_by_type, str):
        hardware_min_by_type = json.loads(hardware_min_by_type or "{}")
    for kind in list(hw_min):
        v = (hardware_min_by_type or {}).get(kind)
        if v not in (None, ""):
            hw_min[kind] = float(v)
            hw_min_source[kind] = "plugin:edited"

    # What each type was ACTUALLY costed at, including per-child edits that
    # only become visible inside the row loop. The payload reports this rather
    # than the pre-override seeds, so a plugin prefilling its boxes from the
    # reply shows the number the money was worked out from.
    hardware_min_used = dict(hw_min)

    ws_rate = {s["name"]: s["hour_rate"] for s in card["workstations"]}
    labour_rows, labour_total = [], 0.0
    for op in card["operations"]:
        # A child is priced by the parent it hangs under, never on its own
        # line here. Without this the six Install <type> rows would be charged
        # once as top-level operations and once again as children.
        if op.get("parent"):
            continue
        name = op["name"]
        seq = int(op["seq"])
        qty_editable = name in QTY_EDITABLE
        min_editable = seq >= MIN_EDITABLE_FROM_SEQ

        mins = float(op["min_per_unit"])
        min_source = op["min_source"]
        q = float(qty.get(name, 0) or 0)
        qty_source = "erp:rule"


        ov = overrides.get(name) or {}
        if ov.get("min") not in (None, ""):
            if not min_editable:
                frappe.throw(_("{0}'s time is computed and cannot be edited.").format(name))
            mins = float(ov["min"])
            min_source = "plugin:edited"
        if ov.get("qty") not in (None, ""):
            if not qty_editable:
                frappe.throw(_("{0}'s quantity comes from the model and cannot "
                               "be edited.").format(name))
            q = float(ov["qty"])
            qty_source = "plugin:edited"

        # ASSEMBLY IS PRICED PER SIZE. Amit, 2026-08-23: a carcass, a drawer
        # and a shelf are not the same job, and one averaged minute-figure
        # cannot say so. The row stays ONE of the seventeen — the structure he
        # asked be kept — but its hours are the three sizes summed, and the
        # minutes it displays are the average those hours imply, so the column
        # still reads honestly against the qty beside it.
        size_hours = None
        children = None
        if name == "Assembly" and counted:
            per = dict(size_min)
            edited = assembly_edited
            for k in ASSEMBLY_SIZES:
                v = (ov.get("min_" + k) if ov else None)
                if v not in (None, ""):
                    per[k] = float(v)
                    edited = True
            if edited:
                min_source = "plugin:edited"
            assembly_min_used = per

            # ONE CHILD ROW PER SIZE. Amit, 2026-08-23: "this should split into
            # child rows and user should be able to key in directly minutes
            # against the quantity, quantity is inferred from our size model
            # which should not get altered but minutes should be changeable in
            # line only. no point in giving a box below on screen."
            #
            # The quantity is the model's answer and is not an input. The
            # minutes are, and they belong beside the count they multiply —
            # a box somewhere else makes the reader hold two numbers in their
            # head to check one line.
            children = []
            ch_rate = float(ws_rate.get(op["workstation"], 0))
            for k in ASSEMBLY_SIZES:
                # Each child rounds UP on its own, then the PARENT is the sum
                # of the children as shown. Rounding the true total once
                # instead would print three rows that do not add up to the
                # line above them — 0.1 + 0.1 + 0.1 under a parent reading
                # 0.2 — which on a screen shared with a client is
                # indefensible whatever the arithmetic behind it.
                ch_hours = _up1(sizes[k] * per[k] / 60.0)
                children.append({
                    "size": k,
                    "name": "Assembly — %s" % k.capitalize(),
                    "qty": _up1(sizes[k]),
                    "min_per_unit": _up1(per[k]),
                    "hours": ch_hours,
                    "amount": round(ch_hours * ch_rate, 2),
                    # The rule, per child: the count comes from the model, the
                    # time comes from the person.
                    "qty_editable": False,
                    "min_editable": True,
                })
            size_hours = sum(c["hours"] for c in children)
            mins = (size_hours * 60.0 / q) if q else mins

        # INSTALL HARDWARE IS PRICED PER FITTING TYPE, for the same reason and
        # in the same shape. Amit, 2026-08-24: "always divide the hardware by
        # its type ... that way quantity will not be editable but its time will
        # be editable depending on type of hardware."
        #
        # Only the types actually PRESENT get a row. A model with no locks in
        # it should not carry a "Install Locks & Tower Bolts x 0" line — an
        # estimate reads as a list of what is being done, and a row for work
        # nobody will do is noise a reader has to dismiss every single time.
        if name == estimator.HARDWARE_PARENT and hw_counts:
            children = []
            ch_rate = float(ws_rate.get(op["workstation"], 0))
            # THE SAME DOOR THE ASSEMBLY CHILDREN USE. A per-child edit arrives
            # as min_<token> inside the PARENT's override — min_large for a
            # size, min_hinges for a type — because the plugin already sends
            # child edits that way and one mechanism understood everywhere
            # beats two that have to be kept equal.
            per = dict(hw_min)
            for kind, _label in estimator.HARDWARE_INSTALL_TYPES:
                v = (ov.get("min_" + kind) if ov else None)
                if v not in (None, ""):
                    per[kind] = float(v)
                    hw_min_source[kind] = "plugin:edited"
            hardware_min_used.update(per)
            for kind, label in estimator.HARDWARE_INSTALL_TYPES:
                n = int(hw_counts.get(kind, 0) or 0)
                if not n:
                    continue
                per_min = per.get(kind, 0.0)
                ch_hours = _up1(n * per_min / 60.0)
                children.append({
                    "kind": kind,
                    "name": "Install %s" % label,
                    "qty": n,
                    "min_per_unit": _up1(per_min),
                    "hours": ch_hours,
                    "amount": round(ch_hours * ch_rate, 2),
                    # Same rule as Assembly's children, stated the other way
                    # round: the model knows how many hinges there are and a
                    # person does not; the person knows how long a hinge takes
                    # and the model does not.
                    "qty_editable": False,
                    "min_editable": True,
                    "min_source": hw_min_source.get(kind, "erp:Operation"),
                })
            if children:
                # Each child rounds up on its own and the parent is their sum,
                # exactly as Assembly does — three rows that do not add up to
                # the line above them is indefensible on a shared screen
                # whatever the arithmetic behind it.
                size_hours = sum(c["hours"] for c in children)
                mins = (size_hours * 60.0 / q) if q else mins

        hours = size_hours if size_hours is not None else (q * mins) / 60.0
        rate = float(ws_rate.get(op["workstation"], 0))
        # Same reasoning for the money: a parent that is not its children's
        # sum invites the one question nobody wants asked mid-quotation.
        amount = (sum(c["amount"] for c in children) if children
                  else hours * rate)
        labour_rows.append({
            "seq": seq, "name": name, "workstation": op["workstation"],
            "qty": _up1(q), "min_per_unit": _up1(mins), "hours": _up1(hours),
            "hour_rate": rate, "amount": round(amount, 2),
            "min_source": min_source, "qty_source": qty_source,
            "rate_source": op["rate_source"],
            # Carried through from the card. The card had it and this did not,
            # so the zone reached a test and never reached the screen — the
            # plugin renders THESE rows, not cost_card's.
            "zone": op.get("zone", ""),
            # What the screen may turn into an input. Sent rather than
            # re-derived on the plugin side, so the rule lives in one place.
            "min_editable": min_editable, "qty_editable": qty_editable,
            # Assembly alone carries children; every other row sends none, so
            # the screen can render one shape and not branch on the name.
            "children": children,
        })
        labour_total += amount

    # ---- logistics: the trips one execution needs ------------------------
    if isinstance(trip_qty, str):
        trip_qty = json.loads(trip_qty or "{}")
    if isinstance(trip_rate, str):
        trip_rate = json.loads(trip_rate or "{}")
    # Rates come from Estimate Settings, which is where every sensitive figure
    # in this app lives. Nothing here reads a rate out of code.
    logistics_rows = estimator.logistics_lines(
        frappe.get_single("Estimate Settings"), trip_qty, trip_rate)
    logistics_sum, logistics_unset = estimator.logistics_total(logistics_rows)

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
        # The breakdown behind that number, and the minutes each size was
        # costed at — so the screen can show "3 = 1 large + 1 medium + 1 small"
        # rather than a total nobody can check.
        "assembly_sizes": {k: sizes[k] for k in ASSEMBLY_SIZES},
        "assembly_unsized": sizes.get("unsized", 0),
        "assembly_min_by_size": assembly_min_used,
        "assembly_source": assembly_source,
        # The same two facts for hardware: what the model counted of each type,
        # and the minutes each was costed at. Sent whether or not any type is
        # present, so a plugin can prefill its boxes without having to guess
        # which of the six exist in this model.
        "hardware_counts": hw_counts,
        "hardware_min_by_type": hardware_min_used,
        "materials": material_rows,
        "labour": labour_rows,
        # Amit, 2026-08-22: "Also need number of days required to make that
        # happen. assume 6 hours working day." Rounded UP like everything else
        # — half a day of work still occupies a day on a calendar, and a
        # promise that reads shorter than the work is the one that gets
        # broken. Six hours is the same productive day the bench costs with
        # (est_days = minutes / 360).
        "hours_per_day": float(hours_per_day or 6),
        "labour_hours": _up1(sum(l["hours"] for l in labour_rows)),
        # Amit, 2026-08-24: "Steps 17 is whatever we can not typically fit into
        # like removal of existing furniture or special shaping of parts. get
        # me box where i can comment it as remarks to understand what
        # miscellaneous is." A line called Miscellaneous with a number beside
        # it and no words is a number nobody can defend a month later.
        "misc_remarks": (misc_remarks or "").strip()[:500],
        "days": _up1(sum(l["hours"] for l in labour_rows) / float(hours_per_day or 6)),
        "material_total": round(material_total, 2),
        "labour_total": round(labour_total, 2),
        "total": round(material_total + labour_total, 2),
        # LOGISTICS IS ITS OWN SUBTOTAL and is deliberately NOT in `total`.
        # Trips belong to one planned execution, shared across every SKU in
        # the estimate — Amit, 2026-08-30: "trips are required in a estimate
        # of skus within one planned execution for each work order" — so
        # adding them into a single article's figure would bill the same
        # tempo once per wardrobe. The Estimate consolidates them; this screen
        # shows what the execution costs beside what the article costs.
        "logistics": logistics_rows,
        "logistics_total": logistics_sum,
        "logistics_unset": logistics_unset,
        # Loud on purpose. A total that quietly omits three unpriced boards is
        # worse than no total: it looks like an answer.
        "unpriced_lines": unpriced,
        "created_items": card.get("created_items", []),
        "excludes": card["excludes"],
    }


# The assembly naming rule lives in estimator.py — frappe-free, so the unit
# suite can hold it, and beside every other naming convention in this app.
# Re-exported here because callers and tests already reach for api.*.
from mallet_estimator.estimator import (      # noqa: F401  (re-exported)
    ASSEMBLY_SIZES, _ASMBL_SIZE, _MCFT_ASMBL, _SIZE_OF, _asmbl_classify,
    _asmbl_counts,
)


def _asmbl_count(rows):
    """Total assemblies, whatever their size. Kept because a plugin that has
    not updated yet still asks this question."""
    c = _asmbl_counts(rows)
    return sum(c[k] for k in ASSEMBLY_SIZES)
