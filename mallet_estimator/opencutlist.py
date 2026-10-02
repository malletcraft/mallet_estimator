# ---------------------------------------------------------------------------
# Native OpenCutList CSV parser + material aggregator.
#
# OpenCutList's "parts" export (semicolon-delimited) lists every part instance
# with its material, final area, edge-banding and laminate faces — but no
# prices. This module turns that part list into an aggregated material estimate:
#
#   Sheet Goods -> total area per (material, thickness) -> whole sheets
#                  (area x (1+wastage) / sheet area, rounded UP)
#   Hardware    -> count of instances per material
#   Edge banding -> running meters per edge-banding material
#   Laminate    -> face area per laminate material -> whole sheets
#
# Prices are looked up separately from the ERPNext Item rate card; this module
# is framework-free so it can be unit-tested without a bench.
# ---------------------------------------------------------------------------

import csv
import io
import math
import re

SHEET_TYPES = {"sheet goods"}
HARDWARE_TYPES = {"hardware"}


def _num(v):
    """'2060 mm' / '1.19 m²' / '1,19' -> float (first number found)."""
    if v is None:
        return 0.0
    s = str(v).strip().replace(",", ".")
    m = re.search(r"-?\d+(?:\.\d+)?", s)
    return float(m.group()) if m else 0.0


def part_qty(row):
    """How many physical pieces one CSV row stands for.

    OpenCutList GROUPS identical parts onto a single row and puts the count in
    `Quantity` ("Group181#2 ( CARCASS_SHELF x3 ) → 3"). Every consumer of the
    CSV has to expand by it. Reading the row and not the count is what nested
    a 34-part wardrobe as 21 parts (7 sheets against OpenCutList's 9) and
    bought 10 pieces of hardware where the model has 99 — 24 MiniFix on one
    row counted once. Missing/zero reads as 1: a row that exists is at least
    one part."""
    q = int(_num(row.get("Quantity")) or 0)
    return q if q > 0 else 1


def _material_from(cell):
    """'EB_PVC_IN_a (1 mm x 22 mm)' -> 'EB_PVC_IN_a'; '' -> None."""
    s = (cell or "").strip()
    if not s:
        return None
    return s.split("(")[0].strip() or None


def parse_opencutlist_csv(text):
    """Parse the semicolon-delimited native OpenCutList export into dict rows.

    OpenCutList sometimes repeats the header before each material-type block;
    we key every data row off the first header and skip any repeated headers
    and blank lines.
    """
    text = text.replace("﻿", "")
    reader = csv.reader(io.StringIO(text), delimiter=";")
    rows = [r for r in reader if any((c or "").strip() for c in r)]
    if not rows:
        return []
    header = [h.strip() for h in rows[0]]
    out = []
    for r in rows[1:]:
        if [c.strip() for c in r] == header:  # repeated header block
            continue
        rec = {header[i]: (r[i] if i < len(r) else "") for i in range(len(header))}
        out.append(rec)
    return out


def parts_list(rows):
    """Extract the panel part list (Material type = Sheet Goods, name starts SG)
    from the parts CSV — one entry per OpenCutList ROW, carrying the part number
    (the id encoded in the OpenCutList QR label) for job-card tracking and `qty`,
    how many identical pieces that row stands for. The row is kept whole rather
    than expanded so the part number on the job card still matches the QR label
    on the shop floor; the operator cuts `qty` of it."""
    out = []
    for r in rows:
        if (r.get("Material type") or "").strip().lower() != "sheet goods":
            continue
        name = (r.get("Material name") or "").strip()
        if not name.upper().startswith("SG"):
            continue
        # Which stations this part passes through, derived from the CSV so the
        # Job Card print can show each operator only the parts they must work.
        edged = any(
            _material_from(r.get(c))
            for c in ("Edge Length 1", "Edge Length 2", "Edge Width 1", "Edge Width 2")
        )
        laminated = any(_material_from(r.get(c)) for c in ("Frontside", "Backside"))
        out.append({
            "part_no": (r.get("No.") or "").strip(),
            "designation": (r.get("Designation") or r.get("Instance") or "").strip(),
            "qty": part_qty(r),
            "material": name,
            "length": _num(r.get("Length") or r.get("Length - raw")),
            "width": _num(r.get("Width") or r.get("Width - raw")),
            "thickness": _num(r.get("Thickness") or r.get("Thickness - raw")),
            "tag": (r.get("Tag") or "").strip(),
            "cut": 1,                       # every sheet part is cut on the saw
            "edge_banded": 1 if edged else 0,
            "laminated": 1 if laminated else 0,
        })
    return out


# Where a part's own name can live in an OpenCutList CSV, most specific first.
# The export's column set is configurable, so this is a search, not a lookup.
DESIGNATION_COLUMNS = ("Designation", "Instance", "Name", "Part", "Label")


def canonical_hw_code(designation):
    """OpenCutList suffixes duplicate instances of the same part with '#N'
    (e.g. HWD_Handle_150mm#3). Strip it so every instance rolls up to one SKU."""
    return re.sub(r"#\d+$", "", (designation or "").strip())


def hardware_list(rows):
    """Aggregate hardware into REAL SKUs from the parts CSV.

    The actual hardware is the part's own name (HWD_AH_SC_0 = Auto Hinge Soft
    Close 0°), not the coarse Material name (HWD_Hinge) — which can hide
    several distinct SKUs at different rates. Which COLUMN carries that name
    depends on how the OpenCutList export is configured, so every column it
    can land in is tried (parts_list already knew about 'Instance'; reading
    only 'Designation' here is what made hardware fall back to its category).

    Returns one entry per canonical designation:
        {code, category, qty, pieces, rows, grouped, length, width, thickness, named}

    QTY IS THE SUMMED "Quantity" COLUMN, which IS OpenCutList's own Qty — and
    getting here took one wrong turn worth recording, because the wrong answer
    survived a week and looked reasonable throughout.

    Amit, 2026-08-30: "quantity in ocl and quantity in mcft material list are
    mismatched. you are picking total and not quantity." He was right that the
    two disagreed. I concluded the CSV's Quantity column WAS the report's
    "Σ Unit Total" and switched to counting ROWS, which matched his rail
    example (two drawers, two rail sets) and matched nothing else.

    His OpenCutList report of 2026-09-03 settles it against that reading, line
    by line. The report's Hardware table gives, per part, a Qty and a Σ Unit:
    HWD_AH_SC_0 is Qty 5 / Σ Unit 10, HWD_Screw_8x32 is Qty 51 / Σ Unit 51,
    HWD_MiniFix 9 / 9, HWD_ShelfSupport 4 / 4, HWD_DR_SC_550mm 2 / 4. The
    summed Quantity column of the SAME export is 5, 51, 9, 4, 2 — it is Qty
    exactly, every time, including where Qty and Σ Unit differ. The row count
    is 2, 4, 1, 1, 1: neither column, just an artefact of how the export was
    grouped.

    OpenCutList costs hardware the same way — EstimateHardwareRun computes
    `h_price[:val] * cutlist_part.def.count`, price times Qty — so Qty is the
    thing you buy and Σ Unit is the pieces inside it. That is precisely Amit's
    rule of 2026-09-03: "hinge always comes as two parts per packet. drawer
    rails comes as two parts per packet ... in erp i want to store per packet
    price." Qty is packets, the Item price is per packet, and his rail set was
    never evidence for row-counting — a set is one packet and OpenCutList had
    always said 2.

    `rows` is kept for diagnostics only. Nothing prices off it, and `grouped`
    no longer means the numbers are wrong: a grouped export is now read
    correctly, which is what a Quantity column is for.

    `named` is False when the CSV carried no designation at all and the
    category had to stand in — the caller says so out loud rather than
    letting a category masquerade as a real SKU.
    """
    out, order = {}, []
    for r in rows:
        if (r.get("Material type") or "").strip().lower() not in HARDWARE_TYPES:
            continue
        category = (r.get("Material name") or "").strip()
        code = next(
            (c for c in (canonical_hw_code(r.get(col) or "") for col in DESIGNATION_COLUMNS)
             if c and c != category),
            "",
        )
        named = bool(code)
        code = code or category
        if not code:
            continue
        if code not in out:
            out[code] = {
                "code": code,
                "named": named,
                "category": category,
                "qty": 0,
                # pieces is kept equal to qty so every existing caller keeps
                # working; rows is the diagnostic that used to be qty.
                "pieces": 0,
                "rows": 0,
                "grouped": False,
                "length": _num(r.get("Length") or r.get("Length - raw")),
                "width": _num(r.get("Width") or r.get("Width - raw")),
                "thickness": _num(r.get("Thickness") or r.get("Thickness - raw")),
            }
            order.append(code)
        n = part_qty(r)
        out[code]["qty"] += n
        out[code]["pieces"] += n
        out[code]["rows"] += 1
        if n > 1:
            out[code]["grouped"] = True
    return [out[c] for c in order]


def grouped_hardware(hw):
    """The hardware entries whose rows each stand for several pieces.

    This is now INFORMATION, not an alarm. It used to mean the quantity was
    understated, because the quantity was the row count; the quantity is the
    summed Quantity column, so a grouped export reads exactly the same as an
    ungrouped one. Kept because a caller may still want to know how the export
    was configured — nothing may refuse or discount an estimate over it.
    """
    return [h for h in hw if h.get("grouped")]


def classify_hardware(name):
    """Bucket a hardware material name into an operation-driver category."""
    n = (name or "").lower()
    if "minifix" in n:
        return "minifix"
    if "hinge" in n:
        return "hinges"
    if "handle" in n:
        return "handles"
    if "rail" in n:
        return "rails"
    if "shelf" in n or "support" in n:
        return "shelf_supports"
    if "lock" in n or "tower" in n or "bolt" in n:
        return "locks"
    if "screw" in n:
        return "screws"
    return "other"


# `aggregate()` WAS HERE AND IS GONE (2026-10-02).
#
# It turned parsed part rows into material lines by dividing "Area - final" by
# a sheet area, with `wastage_pct=12.0` as a default — the last invented
# percentage in this app, in the one direction Amit's rule forbids: "all
# wastage and cossumed will always be driven by MOP and not by erp"
# (2026-09-28).
#
# It had NO production caller. Checked behind a control rather than asserted:
# `classify_hardware` came back with five real call sites across four modules,
# `aggregate` with zero and four comment mentions. It was the OpenCutList
# ESTIMATE-PDF path, superseded by `nest_import.collect` + `nesting.pack_sheets`
# when the plugin started sending a part-list CSV with no area column — a CSV
# that made aggregate() measure every sheet at 0 m² and price four materials at
# nothing.
#
# Deleted rather than left alone because an invented default in unreachable
# code is the cheapest way for the rule to be broken again: the next person
# wanting sheets from rows finds a function that looks like the answer and
# silently reintroduces 12%. Reviving the estimate-PDF path means writing one
# that takes its offcut from MOP, which is a different function from this one.
