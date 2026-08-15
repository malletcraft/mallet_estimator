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
