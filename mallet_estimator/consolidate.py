# ---------------------------------------------------------------------------
# Cross-SKU consolidation (Nest Estimate, Phase 2): nest ALL of an Estimate's
# CSV-Nest SKUs' parts together per material, so shared sheets/rolls make each
# SKU cheaper than it is alone — and allocate the combined quantities back to
# the SKUs by PART-AREA SHARE per material (decided 2026-08-07: each SKU pays
# its parts' area directly; offcut waste is split pro-rata by that share.
# Facial sqft stays the pricing/display denominator, never the waste key).
#
# Pure module — no frappe import — so the whole engine unit-tests in CI's
# no-DB job. The Estimate controller feeds it the __nest_inputs__ blobs that
# nest_import stashed on each SKU and applies the allocation in memory only
# (SKU docs are shared across estimates and are never saved with
# estimate-specific numbers).
# ---------------------------------------------------------------------------

import math

from mallet_estimator import nesting

KERF_MM = 4.0
TRIM_MM = 10.0


def _area(parts):
    return sum(float(l) * float(w) for (l, w) in parts)


# A retained offcut is worth something only if it gets used, and it will not
# always be. Crediting a whole board back would discount the client for a piece
# that may sit on the rack forever, so the recovery is a POLICY percentage
# (Amit, 2026-08-09: 60%) applied to the sheet-equivalent area kept back.
# 0 here as every rate is; the real number lives in the site DB.
RECOVERY_PCT = 0.0


def consolidate(sku_inputs, kerf=KERF_MM, trim=TRIM_MM, recovery_pct=None,
                retainable=None):
    """sku_inputs: {sku: {"ply": {"CODE@th": [(l, w), ...]},
                          "lam": {code: [(l, w), ...]},
                          "faces": {"CODE@th": {face: {lam code: part area}}},
                          "edges": {code: meters}}}

    Laminate quantities come from `faces` — pressed per ply sheet per laminated
    face off the COMBINED panel nest, so pooling boards pools their laminate
    too and the offcut credit carries through the whole sandwich. SKUs whose
    drivers were stashed before `faces` existed fall back to nesting their
    laminate on its own, so an estimate built on old blobs still costs.

    Returns {"materials": {key: {kind, combined, standalone, util,
                                 alloc: {sku: fractional qty},
                                 standalone_by_sku: {sku: qty}}},
             "sheet_ratio": {sku: allocated_sheets / standalone_sheets}}

    `sheet_ratio` covers ply + laminate sheets together — the driver for
    sheet-count operations (lamination, tape removal, cutting): fewer combined
    sheets means proportionally fewer sheet-level operations per SKU.
    """
    buckets = {}  # key -> {"kind", "per_sku": {sku: parts-or-meters}}
    lam_faces = {}  # ply key -> {face: {lam code: {sku: part area}}}
    for sku, inputs in sku_inputs.items():
        for key, parts in (inputs.get("ply") or {}).items():
            b = buckets.setdefault(key, {"kind": "sheet", "per_sku": {}})
            b["per_sku"].setdefault(sku, []).extend(tuple(p) for p in parts)
        by_panel = inputs.get("faces") or {}
        for key, by_face in by_panel.items():
            for face, by_code in (by_face or {}).items():
                for code, area in (by_code or {}).items():
                    per_sku = lam_faces.setdefault(key, {}).setdefault(face, {}).setdefault(code, {})
                    per_sku[sku] = per_sku.get(sku, 0.0) + float(area or 0)
        if not by_panel:
            # pre-`faces` drivers: nest this SKU's laminate the old way
            for code, sides in (inputs.get("lam") or {}).items():
                b = buckets.setdefault(code, {"kind": "laminate", "per_sku": {}})
                b["per_sku"].setdefault(sku, []).extend(tuple(p) for p in sides)
        for code, meters in (inputs.get("edges") or {}).items():
            b = buckets.setdefault(code, {"kind": "edge", "per_sku": {}})
            b["per_sku"][sku] = b["per_sku"].get(sku, 0.0) + float(meters)

    materials = {}
    sheets_alloc = {}   # sku -> allocated sheet-count (ply + lam)
    sheets_alone = {}   # sku -> standalone sheet-count (ply + lam)
    for key, b in sorted(buckets.items()):
        per_sku = b["per_sku"]
        credit, retained = 0.0, []
        if b["kind"] == "edge":
            total_m = sum(per_sku.values())
            combined = nesting.edge_rolls(total_m)
            standalone_by_sku = {s: nesting.edge_rolls(m) for s, m in per_sku.items()}
            shares = {s: (m / total_m if total_m else 0.0) for s, m in per_sku.items()}
            util = None
        else:
            all_parts = [p for parts in per_sku.values() for p in parts]
            r = nesting.pack_sheets(all_parts, kerf=kerf, trim=trim, allow_rotate=False)
            combined = r["sheets"]
            util = round(r["utilization"], 3)
            standalone_by_sku = {
                s: nesting.pack_sheets(parts, kerf=kerf, trim=trim, allow_rotate=False)["sheets"]
                for s, parts in per_sku.items()
            }
            total_area = _area(all_parts)
            shares = {s: (_area(parts) / total_area if total_area else 0.0)
                      for s, parts in per_sku.items()}
            # Offcuts big enough to build a shelf out of go back on the rack,
            # so the job did not consume that part of the board. Internal-grade
            # panels only: `a` is one décor for a whole project, but a V1
            # external is this client's laminate and worth nothing on the next
            # job. Credit is area-based and capped below one whole sheet — a
            # board can be partly recovered, never un-bought.
            if retainable is None or retainable(key):
                keep = nesting.reusable(r.get("offcuts") or [])
                sheet_area = float(nesting.SHEET_L) * float(nesting.SHEET_W)
                pct = RECOVERY_PCT if recovery_pct is None else float(recovery_pct)
                credit = min(
                    sum(l * w for (l, w) in keep) / sheet_area * (pct / 100.0),
                    max(combined - 1, 0))
                retained = keep
        billable = max(combined - credit, 0.0)
        alloc = {s: round(billable * share, 3) for s, share in shares.items()}
        if b["kind"] in ("sheet", "laminate"):
            for s in per_sku:
                sheets_alloc[s] = sheets_alloc.get(s, 0.0) + alloc[s]
                sheets_alone[s] = sheets_alone.get(s, 0.0) + standalone_by_sku[s]
        materials[key] = {
            "kind": b["kind"],
            "combined": combined,
            "billable": round(billable, 3),
            "credit": round(credit, 3),
            "retained": retained,
            "standalone": sum(standalone_by_sku.values()),
            "util": util,
            "alloc": alloc,
            "standalone_by_sku": standalone_by_sku,
        }

    # ---- laminate, derived from the panels it is pressed onto ----------------
    # A laminate sheet is consumed per ply sheet per laminated face, so its
    # quantity is a function of the panel nest, never a nest of its own. Shares
    # are summed across every board and SKU BEFORE rounding: rounding each
    # contribution up first would buy a part-sheet per board that the press
    # never eats. `billable` (not `combined`) drives the allocation so the
    # panel's offcut credit carries through the whole sandwich — a retained
    # offcut is a LAMINATED board, and the client did not consume it.
    lam_acc = {}
    for panel_key, by_face in sorted(lam_faces.items()):
        info = materials.get(panel_key)
        if not info:
            continue
        alone_by_sku = info["standalone_by_sku"]
        for _face, by_code in sorted(by_face.items()):
            face_area = sum(sum(d.values()) for d in by_code.values())
            if face_area <= 0:
                continue
            own_area = {}
            for d in by_code.values():
                for s, a in d.items():
                    own_area[s] = own_area.get(s, 0.0) + a
            for code, per_sku_area in by_code.items():
                acc = lam_acc.setdefault(
                    code, {"combined": 0.0, "billable": 0.0, "per_sku": {}, "alone": {}})
                for s, area in per_sku_area.items():
                    acc["combined"] += float(info["combined"]) * (area / face_area)
                    part = float(info["billable"]) * (area / face_area)
                    acc["billable"] += part
                    acc["per_sku"][s] = acc["per_sku"].get(s, 0.0) + part
                    if own_area.get(s):
                        acc["alone"][s] = acc["alone"].get(s, 0.0) + \
                            float(alone_by_sku.get(s) or 0) * (area / own_area[s])

    for code, acc in sorted(lam_acc.items()):
        combined = max(1, math.ceil(round(acc["combined"], 6)))
        billable = min(acc["billable"], combined)
        alloc = {s: round(v, 3) for s, v in acc["per_sku"].items()}
        standalone_by_sku = {s: max(1, math.ceil(round(v, 6))) for s, v in acc["alone"].items()}
        for s in alloc:
            sheets_alloc[s] = sheets_alloc.get(s, 0.0) + alloc[s]
            sheets_alone[s] = sheets_alone.get(s, 0.0) + standalone_by_sku.get(s, 0)
        materials[code] = {
            "kind": "laminate",
            "combined": combined,
            "billable": round(billable, 3),
            "credit": round(max(combined - billable, 0.0), 3),
            "retained": [],
            "standalone": sum(standalone_by_sku.values()),
            "util": None,          # not nested — pressed onto whole boards
            "alloc": alloc,
            "standalone_by_sku": standalone_by_sku,
        }

    sheet_ratio = {
        s: (sheets_alloc.get(s, 0.0) / sheets_alone[s]) if sheets_alone.get(s) else 1.0
        for s in set(sheets_alloc) | set(sheets_alone)
    }
    return {"materials": materials, "sheet_ratio": sheet_ratio}


def batch_factor(tiers, qty):
    """Batch-efficiency multiplier for an operation: `tiers` =
    [(from_qty, factor), ...] (any order); the tier with the greatest from_qty
    that is <= qty wins; no tier matched -> 1.0. Factors scale minutes/unit,
    so 0.75 means 'in this batch size the operation runs 25% faster'."""
    best_from, best = -1.0, 1.0
    for from_qty, factor in tiers or []:
        f, fac = float(from_qty or 0), float(factor or 0)
        if fac > 0 and f <= float(qty) and f > best_from:
            best_from, best = f, fac
    return best


CSV_MODE = "CSV-Nest"
PDF_MODE = "OCL PDF (standard)"


def split_by_mode(modes):
    """`modes` = {sku: estimation_mode}. Returns (csv_nest, pdf) name lists.

    The two modes carry material packing by DIFFERENT authorities: CSV-Nest
    sheets are nested here (and re-nested across the estimate's SKUs), while
    PDF-mode sheet counts come from OpenCutList's own nesting, already baked
    into the PDF per SKU. An estimate holding both would add sheet counts
    that were never packed together — and only the CSV subset would show the
    shared-material saving, so the totals read as if the PDF SKUs simply
    never benefit. Estimates must therefore be single-mode."""
    csv_nest, pdf = [], []
    for sku, mode in sorted((modes or {}).items()):
        (csv_nest if (mode or PDF_MODE) == CSV_MODE else pdf).append(sku)
    return csv_nest, pdf


def is_mixed(modes):
    csv_nest, pdf = split_by_mode(modes)
    return bool(csv_nest) and bool(pdf)


def intake_row_mode(has_csv, has_estimate_pdf):
    """Which mode an Add-SKUs row creates, from the files it carries:
    Part List CSV -> CSV-Nest; Material Estimate PDF -> OCL PDF. Returns None
    when the row has neither (incomplete) and raises on both (ambiguous —
    a single SKU cannot be packed by two authorities)."""
    if has_csv and has_estimate_pdf:
        raise ValueError("both")
    if has_csv:
        return CSV_MODE
    if has_estimate_pdf:
        return PDF_MODE
    return None
