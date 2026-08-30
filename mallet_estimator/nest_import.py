# ---------------------------------------------------------------------------
# CSV-Nest mode (Nest Estimate, Phase 1): build an Estimate SKU's material
# lines from the OpenCutList Part List CSV ALONE — the nesting engine computes
# sheet counts server-side (no Material Estimate PDF, no Part List PDF).
#
# The CSV fully specifies the job: per-part dims + ply material, per-face
# laminate codes (Frontside/Backside), per-edge banding codes (Edge Length/
# Width 1-2) and hardware rows. Everything downstream (décor map, rates,
# margins, ops, prints) sees ordinary material lines and works unchanged.
# Standard-mode SKUs never enter this module.
# ---------------------------------------------------------------------------

import json

import frappe
from frappe import _

from mallet_estimator import estimate_pdf, inventory, nesting, opencutlist, decor
from mallet_estimator.estimator import op_phase
from mallet_estimator.opencutlist import _material_from, _num

# Calibrated against the shop's real OpenCutList exports. On YS_MB_WAR
# (2026-08-09 export, checked line by line against OCL's own Estimate PDF) the
# engine now reproduces OCL exactly: ply 9/9 per code, edge banding 18.28 m and
# 20.28 m to the centimetre, hardware 99/99. Earlier calibration numbers were
# taken while the importer read CSV ROWS rather than part QUANTITIES, so treat
# any figure quoted before 2026-08-09 as unverified.
KERF_MM = 4.0
TRIM_MM = 10.0


def collect(rows):
    """Aggregate the CSV part rows into nesting inputs:
    (ply {(code, th): [(l,w)..]}, lam {code: [(l,w)..]}, edges {code: meters},
    hardware [ {code, category, qty, ...} ], banded_edge_count,
    faces {(code, th): {face_column: {lam_code: part_area_mm2}}}).

    Every row is expanded by opencutlist.part_qty — OpenCutList groups
    identical parts onto ONE row and puts the count in `Quantity`, so a row is
    usually several pieces.

    `faces` is what makes laminate follow the panel: it records which laminate
    is pressed onto which face of which board, so the laminate sheet count
    comes off the ply nest (nesting.laminate_from_panels) instead of a nest of
    the laminate's own. `lam` stays as the per-face part list — the décor map
    and the rate lookup key off it, and it is what says a code exists at all.

    Hardware comes from opencutlist.hardware_list — the same aggregator the
    PDF path uses — so a line is the REAL designation (HWD_AH_SC_0 = Auto
    Hinge Soft Close 0°), never the coarse Material name (HWD_Hinge), which
    can hide several distinct SKUs at different rates."""
    ply, lam, edges, faces = {}, {}, {}, {}
    banded = 0
    for r in rows:
        name = (r.get("Material name") or "").strip()
        mtype = (r.get("Material type") or "").strip().lower()
        if mtype == "sheet goods" and name.upper().startswith("SG"):
            l = _num(r.get("Length") or r.get("Length - raw"))
            w = _num(r.get("Width") or r.get("Width - raw"))
            th = _num(r.get("Thickness") or r.get("Thickness - raw"))
            if not (l and w):
                continue
            qty = opencutlist.part_qty(r)
            ply.setdefault((name, th), []).extend([(l, w)] * qty)
            for col, dim in (("Edge Length 1", l), ("Edge Length 2", l),
                             ("Edge Width 1", w), ("Edge Width 2", w)):
                eb = _material_from(r.get(col))
                if eb:
                    edges[eb] = edges.get(eb, 0.0) + dim * qty / 1000.0
                    banded += qty
            for col in ("Frontside", "Backside"):
                lc = _material_from(r.get(col))
                if lc:
                    lam.setdefault(lc, []).extend([(l, w)] * qty)
                    by_face = faces.setdefault((name, th), {}).setdefault(col, {})
                    by_face[lc] = by_face.get(lc, 0.0) + l * w * qty
    hw = opencutlist.hardware_list(rows)
    return ply, lam, edges, hw, banded, faces


def envelope_issues(doc, ply):
    """Part-list-vs-views cross-check (engine in nesting.envelope_check):
    parts that exceed the outer envelope annotated on the 7 Views PDF cannot
    build the SKU the views show."""
    outer = (doc.get("outer_w"), doc.get("outer_d"), doc.get("outer_h"))
    return [
        _("{0}: part {1:g} × {2:g} mm exceeds the outer envelope "
          "{3:g} × {4:g} mm from the views PDF").format(code, l, w, d0, d1)
        for (code, l, w, d0, d1) in nesting.envelope_check(outer, ply)
    ]


def run(doc):
    """The CSV-Nest import: mirrors do_import()'s contract (lines, parts table,
    décor blank rows, op drivers, design qty, unpriced flag) with quantities
    from the nesting engine instead of the OCL estimate PDF."""
    from mallet_estimator.mallet_estimator.doctype.estimate_sku.estimate_sku import _file_content

    content = _file_content(doc.parts_csv)
    if isinstance(content, bytes):
        content = content.decode("utf-8", "ignore")
    rows = opencutlist.parse_opencutlist_csv(content)
    if not rows:
        frappe.throw(_("The Part List CSV could not be parsed — is it the OpenCutList export?"))
    ply, lam, edges, hw, banded_edges, faces = collect(rows)
    if not ply:
        frappe.throw(_("No sheet-good parts found in the CSV."))
    issues = envelope_issues(doc, ply)

    manual_rows = [m.as_dict() for m in (doc.materials or []) if m.get("is_manual")]
    doc.set("materials", [])
    unpriced, mats_shape, nest_info = [], [], {}

    SQFT_MM2 = 92903.04
    sheet_sqft = nesting.SHEET_L * nesting.SHEET_W / SQFT_MM2

    panel_sheets = {}
    for (code, th), parts in sorted(ply.items()):
        r = nesting.pack_sheets(parts, kerf=KERF_MM, trim=TRIM_MM, allow_rotate=False)
        for (l, w) in r["too_big"]:
            issues.append(_("{0}: part {1:g} × {2:g} mm cannot fit a sheet at all").format(code, l, w))
        panel_sheets[(code, th)] = r["sheets"]
        # Wastage is what the client pays for and the shop keeps: whole boards
        # in, parts out, the difference in square feet AND rupees — per ply
        # code, because the board is the unit MCFT tracks (Amit, 2026-08-13).
        used_sqft = sum(l * w for (l, w) in parts) / SQFT_MM2
        waste_sqft = max(0.0, r["sheets"] * sheet_sqft - used_sqft)
        _item, sheet_rate, _src = inventory.ensure_material_item(code, kind="sheet", thickness=th)
        waste_money = waste_sqft / sheet_sqft * (sheet_rate or 0)
        waste_txt = (f", waste {waste_sqft:.1f} sqft ≈ ₹{waste_money:,.0f}"
                     if sheet_rate else f", waste {waste_sqft:.1f} sqft")
        nest_info[f"{code}@{th:g}mm"] = {"sheets": r["sheets"], "util": round(r["utilization"], 3),
                                         "parts": len(parts),
                                         "waste_sqft": round(waste_sqft, 1),
                                         "waste_money": round(waste_money, 0)}
        doc._add_material_line(
            code, "sheet", th, r["sheets"],
            f"{code} — {len(parts)} parts → {r['sheets']} sheet(s) ({r['utilization']:.0%} used{waste_txt}) [CSV-Nest]",
            unpriced)
        mats_shape.append({"name": code, "kind": "sheet", "thickness": th, "qty": r["sheets"]})

    # Laminate is pressed onto the WHOLE board before the panel saw runs, so it
    # is bought per ply sheet per laminated face, not nested on its own.
    lam_sheets = nesting.laminate_from_panels(panel_sheets, faces)
    for code in sorted(set(lam) | set(lam_sheets)):
        sheets = lam_sheets.get(code, 0)
        if not sheets:
            continue
        boards = ", ".join(
            f"{c}@{t:g}mm" for (c, t), by_face in sorted(faces.items())
            if any(code in bf for bf in by_face.values()))
        nest_info[code] = {"sheets": sheets, "faces": len(lam.get(code) or []), "pressed_on": boards}
        doc._add_material_line(
            code, "laminate", 0, sheets,
            f"{code} — pressed on {boards or 'ply'} → {sheets} sheet(s), "
            f"one per board face [CSV-Nest]",
            unpriced)
        mats_shape.append({"name": code, "kind": "laminate", "thickness": 0, "qty": sheets})

    for code, meters in sorted(edges.items()):
        rolls = nesting.edge_rolls(meters)
        doc._add_material_line(
            code, "edge", 0, rolls,
            f"{code} — {meters:.2f} m banding → {rolls} whole roll(s) of {inventory.EDGE_ROLL_METERS:g} m [CSV-Nest]",
            unpriced, uom="Roll", rate_factor=inventory.EDGE_ROLL_METERS)
        mats_shape.append({"name": code, "kind": "edge", "thickness": 0, "qty": rolls})

    # Designation-level hardware lines, matching the PDF path exactly (the
    # category rides along as the item's group, so rates resolve per SKU).
    # A GROUPED EXPORT CANNOT BE SAVED. Hardware quantity is the row count —
    # OpenCutList's "Qty", what you actually buy: two drawers is two rail sets
    # (Amit, 2026-08-30). When OpenCutList groups identical parts onto one row
    # and puts the count in Quantity, that row count understates the purchase:
    # YS_MB_WAR's own export carries 24 MiniFix and 32 shelf supports on a
    # single row each, and counting rows there would order one of each.
    #
    # That is the failure that once "bought 10 pieces of hardware where the
    # model has 99", so it REFUSES rather than warns. A preview may show a
    # wrong number with a banner over it; a saved estimate becomes a
    # quotation, and a quotation with 1 MiniFix on it is not recoverable by
    # anybody reading it later.
    _grouped = opencutlist.grouped_hardware(hw)
    if _grouped:
        frappe.throw(_(
            "This CSV is a GROUPED export — {0} hardware line(s) put several "
            "pieces on one row, so the quantity that reaches the estimate "
            "would be the number of rows, not the number of pieces: {1}. "
            "Re-export the part list with grouping off, or the estimate will "
            "under-order hardware."
        ).format(len(_grouped),
                 ", ".join("%s (%d rows, %d pieces)"
                           % (h["code"], h["qty"], h["pieces"]) for h in _grouped)))

    unnamed = [h["code"] for h in hw if not h.get("named", True)]
    if unnamed:
        issues.append(_(
            "No part designation in the CSV for {0} hardware line(s) — priced by "
            "CATEGORY, which can hide several SKUs at different rates: {1}. "
            "Name those parts in SketchUp (e.g. HWD_AH_SC_0) and re-export."
        ).format(len(unnamed), ", ".join(unnamed)))
    for h in hw:
        cat = f" · {h['category']}" if h.get("category") and h["category"] != h["code"] else ""
        doc._add_material_line(
            h["code"], "hardware", 0, h["qty"],
            f"{h['code']} — {h['qty']} nos{cat} [CSV-Nest]", unpriced,
            dims={"category": h.get("category")})
        mats_shape.append({"name": h.get("category") or h["code"], "kind": "hardware",
                           "thickness": 0, "qty": h["qty"]})

    for r in manual_rows:
        doc.append("materials", {
            "item": r.get("item"), "material": r.get("material"),
            "description": r.get("description"), "qty": r.get("qty") or 0,
            "uom": r.get("uom"), "unit_cost": r.get("unit_cost") or 0,
            "line_cost": (r.get("qty") or 0) * (r.get("unit_cost") or 0),
            "customer_supplied": r.get("customer_supplied") or 0, "is_manual": 1,
        })

    doc.unpriced_materials = ", ".join(unpriced)
    if unpriced:
        frappe.msgprint(
            _("UNPRICED lines entered at ₹0 — key each rate on the <b>Estimation (Assumed)</b> "
              "price list and Refresh rates:<br><b>{0}</b>").format(", ".join(unpriced)),
            title=_("Materials need a price"), indicator="red")

    # blank décor rows per slot instance (CSV has no description blocks)
    have = {("Laminate" if (r.get("domain") or "Laminate") != "Edge Band" else "Edge Band",
             (r.slot or "").strip().lower()) for r in (doc.get("sku_decors") or [])}
    have |= {("Edge Band", (r.slot or "").strip().lower()) for r in (doc.get("sku_decor_edges") or [])}
    ply_max = max([th for (_c, th) in ply.keys()] or [16])
    eb_thick, eb_wide = (1.0, 50.0) if ply_max > 18 else (0.8, 22.0)
    live = set()
    for m in doc.materials or []:
        base = str(m.material or "")
        up = base.upper()
        if not (up.startswith("SG_LAM") or up.startswith("EB_")):
            continue
        key = decor.slot_key(base)
        if not key:
            continue
        dom = "Edge Band" if up.startswith("EB_") else "Laminate"
        live.add((dom, key))
        if (dom, key) not in have:
            if dom == "Edge Band":
                doc.append("sku_decor_edges", {"slot": key, "thickness": eb_thick, "width": eb_wide})
            else:
                doc.append("sku_decors", {"slot": key, "domain": dom})
            have.add((dom, key))
    # A re-import from a changed CSV used to LEAVE every slot the SKU had ever
    # seen, so the décor map filled with letters no material line refers to —
    # eight edge bands for two real slots. Slots the current lines don't use
    # are dropped, EXCEPT ones the user has already filled in: a mapped décor
    # is their work, and silently deleting it would be worse than the clutter.
    dropped = []
    for table, dom in (("sku_decors", "Laminate"), ("sku_decor_edges", "Edge Band")):
        keep = []
        for row in doc.get(table) or []:
            key = (row.slot or "").strip().lower()
            row_dom = row.get("domain") or dom
            mapped = any(row.get(f) for f in ("decor", "brand", "code", "decor_name"))
            if (row_dom, key) in live or mapped:
                keep.append(row)
            else:
                dropped.append(f"{dom} {row.slot}")
        doc.set(table, keep)
    if dropped:
        issues.append(_("Dropped {0} unused décor slot(s) no material line refers to: {1}")
                      .format(len(dropped), ", ".join(dropped)))

    # operation quantities from the nested materials + banded edge count
    opq = estimate_pdf.operation_quantities(mats_shape, banded_edges)
    for row in doc.labor:
        op = op_phase(row)
        if op in opq:
            row.qty = opq[op]
    opq["__nest__"] = nest_info
    # Estimate-level consolidation re-nests ALL its SKUs' parts together, so
    # the raw nesting inputs ride along (part dims per material, lam faces,
    # edge meters). JSON-in-hidden-Code is the house pattern for driver blobs.
    opq["__nest_inputs__"] = {
        "ply": {f"{code}@{th:g}": parts for (code, th), parts in ply.items()},
        "lam": lam,
        "edges": edges,
        # {ply key: {face: {laminate code: part area}}} — consolidation needs it
        # to derive laminate from the COMBINED panel nest (and to carry the
        # offcut credit through the sandwich), same rule as the SKU above.
        "faces": {f"{code}@{th:g}": by_face for (code, th), by_face in faces.items()},
    }
    if issues:
        opq["__issues__"] = issues
    doc.import_drivers = json.dumps(opq)

    for row in doc.get("design_labor") or []:
        if not float(row.qty or 0):
            row.qty = 1

    # parts table (QR/job-card tracking) — same as standard mode
    parts = opencutlist.parts_list(rows)
    if parts:
        doc.set("parts", [])
        for p in parts:
            doc.append("parts", {
                "part_no": p["part_no"], "designation": p["designation"], "material": p["material"],
                "qty": p.get("qty", 1),
                "tag": p["tag"], "length": p["length"], "width": p["width"], "thickness": p["thickness"],
                "cut": p.get("cut", 1), "edge_banded": p.get("edge_banded", 0),
                "laminated": p.get("laminated", 0),
            })

    frappe.msgprint(
        _("CSV-Nest import: {0} — sheets computed by the calibrated nesting engine "
          "(kerf {1} mm, trim {2} mm, grain-locked).").format(
            ", ".join(f"{k}: {v['sheets']}" for k, v in nest_info.items()),
            KERF_MM, TRIM_MM),
        title=_("Nest details"), indicator="blue")
    if issues:
        frappe.msgprint(
            _("Part list vs views check — {0} issue(s):<br>{1}").format(
                len(issues), "<br>".join(issues[:12]) + ("<br>…" if len(issues) > 12 else "")),
            title=_("Part list may not build this SKU"), indicator="orange")
