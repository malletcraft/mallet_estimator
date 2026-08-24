# ---------------------------------------------------------------------------
# OpenCutList "Estimate" PDF parser.
#
# The estimate PDF is OpenCutList's authoritative material report (accurate
# sheet counts from its real nesting, plus hardware counts). We extract the
# per-material QUANTITIES from it; prices come from the ERPNext Item rate card /
# inventory, not from the PDF. Sections: Sheet goods, Veneer (laminate), Edge
# banding, Hardware. Rows are complete on one line, or wrap after a material
# description note — both are handled.
# ---------------------------------------------------------------------------

import re
from io import BytesIO

SECTION_KIND = {
    "Sheet goods": "sheet",
    "Veneer": "laminate",
    "Edge banding": "edge",
    "Hardware": "hardware",
    "Solid wood": "solidwood",
    "Dimensional": "dimensional",
}
_MAT = re.compile(r"^(SG_|HWD_|EB_|DL_|SW_)\S")
_SPEC = re.compile(r"\s*\d+(?:\.\d+)?\s*mm(?:\s*x\s*\d+(?:\.\d+)?\s*mm)?")


def read_pdf_text(content):
    from pypdf import PdfReader
    reader = PdfReader(BytesIO(content))
    return "\n".join((p.extract_text() or "") for p in reader.pages)


def parse_estimate_pdf(text):
    """Return a list of {name, thickness, qty, kind} from the estimate PDF text."""
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    # Some exports wrap the thickness unit: 'SG_LAM_V0_12mm_a_a / 1' + next line
    # 'mm' — the stray '1' then reads as the QUANTITY (every laminate qty 1).
    # Re-join the spec before parsing.
    merged, i = [], 0
    while i < len(lines):
        l = lines[i]
        if re.search(r"/\s*\d+(?:\.\d+)?$", l) and i + 1 < len(lines) \
                and lines[i + 1].lower().startswith("mm"):
            l = f"{l} {lines[i + 1]}"
            i += 2
        else:
            i += 1
        merged.append(l)
    lines = merged
    section = None
    out = []
    i = 0
    n = len(lines)
    while i < n:
        s = lines[i]
        if s in SECTION_KIND:
            section = SECTION_KIND[s]
            i += 1
            continue
        if section and _MAT.match(s) and not s.startswith("Total"):
            if " / " in s:
                name = s.split(" / ", 1)[0].strip()
                rest = s.split(" / ", 1)[1]
                sm = _SPEC.match(rest)
                thickness = float(re.search(r"\d+(?:\.\d+)?", rest).group()) if sm else 0
                tail = rest[sm.end():] if sm else rest
            else:
                name = s.split()[0]
                thickness = 0
                tail = s[len(name):]
            # Append following DATA lines (start with a digit); skip description notes.
            j = i + 1
            while j < n:
                nx = lines[j]
                if _MAT.match(nx) or nx.startswith("Total") or nx in SECTION_KIND:
                    break
                if re.match(r"^\d", nx):
                    tail += " " + nx
                j += 1
            qm = re.search(r"\d+", tail)
            if qm:
                out.append({"name": name, "thickness": thickness, "qty": int(qm.group()), "kind": section})
            i = j
            continue
        i += 1
    # A HWD_* designation is hardware no matter which PDF section it was listed
    # under (some exports put modelled fittings in the veneer/laminate table) —
    # without this it dodges the part-list dedupe and lands twice.
    for m in out:
        if str(m["name"]).upper().startswith("HWD"):
            m["kind"] = "hardware"
    return out


def _hw(materials):
    """Sum hardware quantities by distinctive substring of the material name."""
    def total(sub):
        return sum(m["qty"] for m in materials if m["kind"] == "hardware" and sub in m["name"].lower())
    return {
        "minifix": total("minifix"),
        "screws": total("screw"),
        "hinges": total("hinge"),
        "rails": total("rail"),
        "handles": total("handle"),
        "tower": total("tower"),
        "shelf": total("shelf"),
    }


def hardware_by_type(materials):
    """Every HWD_ line bucketed by the classifier, as {bucket: qty}.

    _hw() above sums by substring and its buckets OVERLAP and LEAK — a fitting
    matching none of its seven words is counted nowhere, and one matching two
    is counted twice. That is tolerable for the drivers it feeds, which each
    want one specific word. It is not tolerable here, because these numbers
    become the child ROWS under Install Hardware and their sum has to equal the
    parent or the screen contradicts itself in front of a client.

    So this goes through opencutlist.classify_hardware, which returns exactly
    one bucket per line and has an "other" for everything it does not know.
    Nothing is dropped and nothing is counted twice — which is the whole
    difference between a total and a breakdown.

    FEED IT THE CATEGORY, NOT THE CODE. The classifier matches words, and a
    real OCL designation carries none of them: HWD_AH_SC_0 is an auto hinge,
    soft close, 0 degrees, and contains no "hinge" anywhere. Passed raw it
    lands in "other" — twenty-four hinges filed beside a magic corner, with
    the total still correct so nothing looks wrong. api.py already shapes
    hardware rows to the OCL "Material name" (HWD_Hinge) before calling
    operation_quantities, and that is the shape this needs.
    """
    from mallet_estimator import opencutlist
    out = {}
    for m in materials:
        if m.get("kind") != "hardware":
            continue
        bucket = opencutlist.classify_hardware(m.get("name") or "")
        out[bucket] = out.get(bucket, 0) + (m.get("qty") or 0)
    return out


def hardware_install_total(materials):
    """What Install Hardware's quantity is: every fitting that is INSTALLED.

    Amit, 2026-08-24, on whether the widening was intended: "keep locks and
    other". So the parent is no longer hinges + rails + handles + shelf — it is
    every HWD_ line except the two buckets that another step already prices
    (minifix at step 5, screws at step 6). The total went up, deliberately: a
    tower bolt was always being fitted by somebody and never being charged.
    """
    from mallet_estimator.estimator import HARDWARE_INSTALL_TYPES
    counts = hardware_by_type(materials)
    return sum(counts.get(k, 0) for k, _ in HARDWARE_INSTALL_TYPES)


def sheet_goods_sheets(materials):
    """Full plywood sheets from the 'Sheet goods' table only (excludes veneer/
    laminate). This is what gets cut/taped/laminated."""
    return sum(m["qty"] for m in materials if m["kind"] == "sheet")


def operation_quantities(materials, part_count):
    """Compute each operation's Qty from the estimate PDF quantities + the parts
    count. Follows the fixed mapping (locked ops 1-4; the rest are editable
    defaults). Returns {operation_name: qty}."""
    hw = _hw(materials)
    sg = sheet_goods_sheets(materials)
    assembly = 1 + hw["rails"]
    q = {
        "Sheet Lamination": sg,
        "Sheet Tape Removal": sg,
        "Sheet Cutting": sg,
        "Edge Banding": part_count,
        "Minifix Boring": hw["minifix"],
        # Screws are part of Drilling (their count is the step qty).
        "Drilling": hw["screws"],
        "Grooving": 2,
        "Assembly": assembly,
        # Install Hardware is now every fitting that gets INSTALLED, not the
        # old four — see hardware_install_total for which two are excluded and
        # why. It is also the sum of the child rows shown under it, which is
        # the property that matters on screen.
        "Install Hardware": hardware_install_total(materials),
        "Disassembly": assembly,
        "Miscellaneous - extra": 0,
    }
    q["Packing"] = q["Disassembly"]
    q["Loading"] = q["Packing"]
    q["Transport"] = q["Loading"]
    q["Unloading"] = q["Transport"]
    q["Assembly (on-site)"] = assembly
    q["Installation"] = q["Assembly (on-site)"]
    return q


# Operations whose Qty is fully computed and must not be edited (ops 1-6 + 9):
# sheet ops + Edge Banding from the import drivers; Minifix Boring / Drilling /
# Install Hardware live-derived from the HWD_* material lines. Extra work means
# extra ROWS (reviewable), never a hand-edited qty on a computed step.
LOCKED_OPERATIONS = {
    "Sheet Lamination", "Sheet Tape Removal", "Sheet Cutting", "Edge Banding",
    "Minifix Boring", "Drilling", "Install Hardware",
}
