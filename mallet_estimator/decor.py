# S9 — décor slot mapping: the OCL placeholder codes (SG_LAM_V1_16mm_b_a,
# EB_PVC_EX_c …) stay the small fixed SketchUp palette; the REAL laminate /
# edge-band décor per slot is written in the OCL material Description using the
# B-C-N standard ("say it like you order it"):
#
#     b = Merino 1834 Moonlit Gray          (Name optional: "b = Merino 1834")
#     b = Merino 6534; c = RT 6575          (multi-slot, ';' or newline)
#
# The description flows into the estimate/part-list PDFs as a sub-line under the
# material row; import parses it, auto-creates the décor Item (LAMINATE_* for
# laminates, EBD_* for edge banding — structure only, rate keyed once on the
# price list) and fills the SKU's slot map. Legacy freeform descriptions still
# map (slug item, no manufacturer).

import re

_SLOT_DEF_RE = re.compile(r"^\s*([a-z]\d*)\s*=\s*([^;\n]+)", re.M)
_INLINE_SLOT_RE = re.compile(r"\b([a-z]\d*)\s*=\s*([^;\n]+)")
_LABEL_RE = re.compile(r"^\s*(brand|code|name|year|short)\s*=\s*(.+)$", re.I)
_PLACEHOLDER_RE = re.compile(r"^\s*(SG_LAM_[A-Za-z0-9_]+|SG_PLY_[A-Za-z0-9_]+|EB_[A-Za-z0-9_]+)")
_CODE_TOKEN_RE = re.compile(r"^[0-9][0-9A-Za-z\-]*$")

# Fallback maker names when the caller can't supply the live Manufacturer list
# (pure/unit contexts). At runtime the list comes from ERPNext's Manufacturer
# master — add a new maker there ONCE and the parser knows it (full name,
# squashed name, or its initials: "RT" = Royal Touch, "VM" = Virgo Mica).
DEFAULT_BRANDS = ("Merino", "Royal Touch", "Virgo Mica")


def brand_aliases(brands=None):
    """{alias(lower) -> canonical maker name} built from the maker list:
    full name, name without spaces, and initials for multi-word names."""
    out = {}
    for b in (brands or DEFAULT_BRANDS):
        b = (b or "").strip()
        if not b:
            continue
        out[b.lower()] = b
        squashed = b.replace(" ", "").lower()
        out.setdefault(squashed, b)
        words = b.split()
        if len(words) > 1:
            out.setdefault("".join(w[0] for w in words).lower(), b)
    return out


def _slug(text, maxlen=60):
    s = re.sub(r"[^A-Za-z0-9]+", "_", str(text or "")).strip("_")
    return s[:maxlen]


def parse_slot_value(value, brands=None):
    """Parse one slot's décor spec per M-C-N (Maker, Code, Name — name optional).
    `brands` = the live Manufacturer list (falls back to DEFAULT_BRANDS). Returns
    {brand, catalogue, name, raw}; brand None when unrecognised (legacy freeform
    keeps working via the raw text)."""
    raw = (value or "").strip()
    # a leaked "b=" / "c =" / "b1 =" prefix must never enter the identity slug
    raw = re.sub(r"^[a-z]\d*\s*=\s*", "", raw)
    tokens = raw.split()
    if not tokens:
        return None
    aliases = brand_aliases(brands)
    # maker may span up to three tokens ("Virgo Mica", "Royal Touch") or be initials
    brand = None
    rest = tokens
    for take in (3, 2, 1):
        cand = " ".join(tokens[:take]).lower()
        if cand in aliases:
            brand = aliases[cand]
            rest = tokens[take:]
            break
    catalogue = None
    if rest and _CODE_TOKEN_RE.match(rest[0]):
        catalogue = rest[0]
        rest = rest[1:]
    return {"brand": brand, "catalogue": catalogue, "name": " ".join(rest).strip(), "raw": raw}


def material_slots(code):
    """The placeholder slot tokens a material code carries (non-'a'), incl.
    paste-rename suffixes: SG_PLY_V2_b_c → ['b','c']; SG_LAM_V1_16mm_b_a →
    ['b']; EB_PVC_EX_c1 → ['c1']."""
    tokens = str(code or "").split("_")
    return [t for t in tokens if SLOT_TOKEN_RE.match(t) and t != "a"]


def parse_description(desc, placeholder_code, brands=None):
    """Slot definitions from one material's description text. Two formats:

    LABELLED BLOCK (clear titles; Year optional; also maps slot 'a'):
        b = External Laminate
        Brand = Virgo Mica
        Code = 1834
        Name = Moonlight
        Year = 2025-26

    COMPACT one-liner: 'b = Virgo Mica 1834 Moonlight' (maker must be known).
    Prefixless legacy text still maps to the material's first slot."""
    out = {}
    lines = (desc or "").splitlines()
    current_slot, current_title, block = None, None, {}

    def close_block():
        nonlocal current_slot, current_title, block
        if block.get("brand"):
            aliases = brand_aliases(brands)
            block["brand"] = aliases.get(block["brand"].lower(), block["brand"])
        if current_slot and block.get("brand") or current_slot and block.get("code"):
            name = block.get("name", "")
            year = block.get("year", "")
            out[current_slot] = {
                "brand": block.get("brand"), "catalogue": block.get("code"),
                "short": block.get("short"),
                "name": name, "year": year, "title": current_title,
                "raw": " ".join(x for x in (block.get("brand"), block.get("code"), name,
                                            f"({year})" if year else "") if x),
            }
        current_slot, current_title, block = None, None, {}

    for line in lines:
        for seg in line.split(";"):
            seg = seg.strip()
            if not seg:
                continue
            lm = _LABEL_RE.match(seg)
            if lm and current_slot:
                block[lm.group(1).lower()] = lm.group(2).strip()
                continue
            sm = _INLINE_SLOT_RE.match(seg)
            if sm and SLOT_TOKEN_RE.match(sm.group(1)):
                close_block()
                slot, value = sm.group(1), sm.group(2).strip()
                parsed = parse_slot_value(value, brands)
                if parsed and parsed.get("brand"):
                    parsed["title"] = None
                    parsed.setdefault("year", "")
                    out[slot] = parsed          # compact form, maker recognised
                else:
                    current_slot, current_title = slot, value  # block header
                continue
    close_block()

    if not out and (desc or "").strip():
        slots = material_slots(placeholder_code)
        if slots:
            parsed = parse_slot_value(desc, brands)
            if parsed:
                parsed.setdefault("year", "")
                parsed.setdefault("title", None)
                out[slots[0]] = parsed
    return out


def extract_slot_map(pdf_text, brands=None):
    """Walk a PDF's text: a line starting with a placeholder material code opens a
    row; following non-numeric, non-material lines are its description. Returns
    [{placeholder, slot, brand, catalogue, name, raw}] in reading order."""
    results, seen = [], set()
    current, desc_lines = None, []

    def flush():
        if not current:
            return
        for slot, parsed in parse_description("\n".join(desc_lines), current, brands).items():
            parsed.setdefault("year", "")
            parsed.setdefault("title", None)
            key = (current, slot)
            if key in seen:
                # The same slot described again (estimate PDF + part list): a
                # page break can TRUNCATE one copy (brand without Code=) — the
                # more complete description upgrades the earlier entry.
                for r in results:
                    if (r["placeholder"], r["slot"]) == key \
                            and not r.get("catalogue") and parsed.get("catalogue"):
                        r.update(parsed)
                continue
            seen.add(key)
            results.append({"placeholder": current, "slot": slot, **parsed})

    NOISE = re.compile(r"m\u00b2|m\u00b3|\bRs\b|\bQty\b|Designation|\d+\s*mm\b|^No\.|^Total|^\d[\d\s.,]*$", re.I)
    for line in pdf_text.splitlines():
        # same normalisation as views_pdf._norm_line: NULs / nbsp / icon glyphs
        line = re.sub(r"[\x00-\x1f\xa0\ue000-\uf8ff]", " ", line).strip()
        if not line:
            continue
        m = _PLACEHOLDER_RE.match(line)
        if m:
            flush()
            current, desc_lines = m.group(1), []
            # description may share the row line after the spec ("... b=...")
            tail = line[m.end():]
            if "=" in tail:
                eq = tail.find("=")
                desc_lines.append(tail[max(0, eq - 2):])
            continue
        if current is None:
            continue
        # labelled block / slot lines always belong to the description
        if _LABEL_RE.match(line) or _INLINE_SLOT_RE.match(line):
            desc_lines.append(line)
            continue
        # table/summary debris ends the block; unlabelled description text is at
        # most the two lines right under the material row
        if NOISE.search(line) or len([l for l in desc_lines if not _LABEL_RE.match(l)]) >= 2:
            flush()
            current, desc_lines = None, []
            continue
        desc_lines.append(line)
    flush()
    return results



SLOT_TOKEN_RE = re.compile(r"^[a-z]\d*$")


def trailing_slots(code):
    """The trailing slot tokens of a placeholder, INCLUDING 'a' and including
    SketchUp's paste-rename digits: SG_LAM_V1_16mm_b_a → ['b','a'];
    SG_LAM_V1_16mm_b_a1 → ['b','a1'];  EB_PVC_EX_b1 → ['b1'];
    SG_LAM_V1_16mm_VM6534 → [] (already real)."""
    tokens = str(code or "").split("_")
    out = []
    for t in reversed(tokens):
        if SLOT_TOKEN_RE.match(t):
            out.append(t)
        else:
            break
    return list(reversed(out))


def slot_key(code):
    """The slot INSTANCE that decides this placeholder's décor: the FIRST
    trailing letter + the paste-rename suffix (the digits SketchUp appends to
    the LAST token when a same-named material with a DIFFERENT definition is
    pasted in). b_a → 'b'; b_a1 → 'b1' (a different décor than 'b');
    EB_PVC_EX_b1 → 'b1'; a_b1 → 'a1'."""
    toks = trailing_slots(code)
    if not toks:
        return None
    return toks[0][0] + re.sub(r"^[a-z]", "", toks[-1])


def panel_slots(code):
    """EVERY slot instance a ply placeholder carries — a panel has two faces,
    so SG_PLY_V1_a_b names TWO laminates (internal a, external b), where
    slot_key would keep only the deciding first one. The paste-rename suffix
    rides the whole material definition, so it applies to each letter:
    SG_PLY_V1_a_b → ['a','b']; SG_PLY_V2_b_c1 → ['b1','c1'];
    SG_PLY_V0_a_a → ['a']; no trailing slots → []."""
    toks = trailing_slots(code)
    if not toks:
        return []
    suffix = re.sub(r"^[a-z]", "", toks[-1])
    return sorted({t[0] + suffix for t in toks})


def short_code(parsed):
    """The décor SHORT code that replaces the slot letters in a real item name:
    explicit Short= label wins; else brand initials (multi-word: 'Virgo Mica' →
    VM; single word: first two letters, 'Merino' → ME) + catalogue number; else
    a slug of the raw text (legacy)."""
    if not parsed:
        return None
    if parsed.get("short"):
        return _slug(parsed["short"]).upper()
    brand, cat = parsed.get("brand"), parsed.get("catalogue")
    if brand and cat:
        words = brand.split()
        ini = "".join(w[0] for w in words).upper() if len(words) > 1 else brand[:2].upper()
        return f"{ini}{cat}"
    return _slug(parsed.get("raw"))[:20] or None


_BOARD_TOKEN = re.compile(r"V\d+|\d+(?:\.\d+)?mm", re.I)


def stock_base(base):
    """Drop the BOARD's attributes from a laminate placeholder's base.

        SG_LAM_V1_16mm → SG_LAM

    A 1 mm sheet of Virgo Mica GRAY is ONE stock item. The grade and thickness
    in the OpenCutList name describe the board the sheet gets pressed onto —
    they are there so OpenCutList keeps each board's laminate on its own layout
    — and carrying them into the Item mints a separate Item, and a separate rate
    to key, for every board the same laminate happens to land on. Edge bands and
    anything else pass through untouched."""
    if not str(base).upper().startswith("SG_LAM"):
        return base
    return "_".join(t for t in str(base).split("_") if not _BOARD_TOKEN.fullmatch(t))


def substitute_real_code(code, slot_shorts):
    """Turn a laminate/edge PLACEHOLDER into the REAL item code by replacing the
    trailing slot letters with the FIRST letter's décor short code (the pair only
    indicates which side gets what — the purchase is ONE laminate):
        SG_LAM_V1_16mm_b_a + {b: VM6534} → SG_LAM_VM6534
        SG_LAM_V0_a_a      + {a: GE1834} → SG_LAM_GE1834
        EB_PVC_EX_b        + {b: VM6534} → EB_PVC_EX_VM6534
    Suffixed placeholders (SketchUp paste-rename) are their OWN slot instance:
        SG_LAM_V1_16mm_b_a1 + {b1: VM6534} → SG_LAM_VM6534
    Returns (real_code, slot_key) — or (code, None) when no décor is defined
    for the deciding slot (the placeholder itself stays the item)."""
    letters = trailing_slots(code)
    if not letters:
        return code, None
    key = slot_key(code)
    short = slot_shorts.get(key)
    if not short:
        return code, None
    base = stock_base("_".join(str(code).split("_")[: -len(letters)]))
    return f"{base}_{short}", key


# A panel saw cuts a SANDWICH, not a board. What occupies a sheet is the
# pre-pasted assembly — ply core plus the laminate on each face — so what
# decides whether two articles can share a sheet is the assembly, not the ply
# code. `SG_PLY_V1_a_b` in a wardrobe and the same string in a bed are the same
# ply and different panels the moment `b` means two different laminates.
#
# The slot grammar makes this cheap to read: `a` is ALWAYS the internal face,
# and `b` onwards — b, b1, c, d, d1 — are ALWAYS external (Amit, 2026-08-09).
# So the external décor is the first trailing slot that is not an `a`.
INTERNAL_SLOT = "a"


def panel_faces(code):
    """(internal_slot, external_slot) for a ply placeholder.

        SG_PLY_V0_a_a  -> ('a', 'a')     internal both sides
        SG_PLY_V1_a_b  -> ('a', 'b')     b is the face the client sees
        SG_PLY_V1_a_b1 -> ('a', 'b1')

    Both come back 'a' for a V0 board, which is what makes every article's
    internal-grade panels shareable: `a` is one décor for a whole project."""
    slots = trailing_slots(code)
    if not slots:
        return None, None
    external = next((s for s in slots if not s.startswith(INTERNAL_SLOT)), None)
    internal = next((s for s in slots if s.startswith(INTERNAL_SLOT)), None)
    if external is None:
        external = internal          # V0: internal décor on both faces
    if internal is None:
        internal = external
    return internal, external


def panel_key(code, thickness, slot_shorts):
    """What this pasted panel IS, as a nesting bucket.

    Two SKUs share sheets when this key matches, because that is exactly when
    the sheets coming off the saw are physically interchangeable. A V0 board
    keyed on internal `a` matches project-wide — the saving the shop actually
    gets. A V1 board carries its external décor, so a wardrobe in Merino and a
    bed in Virgo Mica never pool, however identical their ply codes look.

    An unresolved external slot yields None: nothing has yet said what the
    panel is, and guessing it into someone else's sheet is the error this
    exists to prevent."""
    internal, external = panel_faces(code)
    if not external:
        return None
    ext = (slot_shorts or {}).get(external)
    intl = (slot_shorts or {}).get(internal) if internal else None
    if not ext:
        return None
    base = str(code or "").split("_")
    grade = next((t for t in base if re.fullmatch(r"V\d", t)), "V?")
    return f"PANEL_{grade}_{float(thickness or 0):g}mm_{intl or '?'}_{ext}"


# The purchasing identity of a material, in ONE frappe-free place.
#
# This is not where it started. inventory.item_code_for held the real rule and
# opencutlist.item_code_for held a naive copy — same name, different module,
# different answer — and estimate_preview imported the naive one. So the
# plugin priced ply as SG_PLY_V0_a_a_16mm while every Item the bench mints is
# SG_PLY_V0_16mm, and the code it quoted against was a stub left behind by
# patches/collapse_board_item_codes. Found 2026-08-29 by running one CSV down
# both paths and diffing.
#
# Deleting the copy was the obvious fix and it does not work on its own:
# inventory imports frappe, so the pure suite cannot reach the rule at all,
# which is exactly why a frappe-free copy existed. The rule therefore lives
# HERE, where both callers can have it and a test can reach it without a
# bench. inventory.item_code_for delegates and keeps its name for callers.
#
# It is deliberately NOT called item_code_for. Two functions of that name are
# what caused this; a third would be asking for it again.
_PC_MM = re.compile(r"\d+(?:\.\d+)?mm", re.I)
PLY_CODE_PREFIX = "SG_PLY"


def purchasing_code(name, thickness=0, kind=None):
    """The ERPNext item_code for an OpenCutList material name.

    A BOARD loses its décor slot letters: two décors on one board is still one
    board to buy, and the letters belong to the cutting diagram rather than to
    the purchase. Its millimetres never go — collapsing 12 mm and 16 mm onto
    one Item would be a worse bug than the one this fixes.

    A LAMINATE keeps everything: the décor IS its identity, and stripping it
    would merge every laminate on the site into one meaningless code.
    """
    text = str(name or "")
    if kind == "sheet" and text.upper().startswith(PLY_CODE_PREFIX):
        tokens = text.split("_")
        own_mm = next((t for t in tokens if _PC_MM.fullmatch(t)), "")
        base = "_".join(t for t in tokens if not _PC_MM.fullmatch(t))
        slots = trailing_slots(base)
        if slots:
            base = "_".join(base.split("_")[: -len(slots)])
        if thickness:
            return "%s_%gmm" % (base, thickness)
        # No thickness passed: the code's own mm token IS the thickness.
        return "%s_%s" % (base, own_mm) if own_mm else base
    if kind == "sheet" and thickness and "mm" not in text.lower():
        return "%s_%gmm" % (text, thickness)
    return text
