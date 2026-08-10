import re

import frappe

from mallet_estimator import decor, inventory


# The OpenCutList material name and the stock Item code answer different
# questions, and this patch un-picks the place they were treated as one.
#
#   ply       SG_PLY_V1_a_b_16mm  ->  SG_PLY_V1_16mm
#   laminate  SG_LAM_V1_16mm_VM6534 -> SG_LAM_VM6534
#
# A ply board is the same board whatever gets pasted on it, and a 1 mm sheet of
# a décor is the same sheet whatever board it lands on — so each of those was
# minting an Item, and a rate to key, per décor / per board thickness. The
# letters stay in the OpenCutList name (that is what makes OpenCutList lay two
# décors out on separate boards); they just stop reaching inventory.
#
# Legacy Items are LEFT IN PLACE (house rule: hide, never delete — no data
# migration). What moves is the ASSUMED RATE, so the collapsed Item is priced
# the moment it appears instead of landing at 0 and printing "NOT quotable".
# Where several legacy Items collapse onto one, the HIGHEST assumed rate wins,
# matching the standing rule that the assumed rate is a ceiling (max MRP across
# suppliers). Rates are read and copied inside the site DB; none is ever logged.

_MM = re.compile(r"\d+(?:\.\d+)?mm", re.I)


def collapsed_code(code):
    """The Item code `code` would be minted as today, or None if unchanged."""
    text = str(code or "")
    up = text.upper()
    if up.startswith(inventory.LAM_PREFIX):
        # An UNMAPPED placeholder still ends in its slot letters. It is not a
        # purchasing identity at all — nothing has said what it is — and
        # stripping the board tokens off it would merge every unmapped laminate
        # in the site into one meaningless SG_LAM_a_a. Leave it; it is replaced
        # by the real code the moment the décor map is filled in.
        if decor.trailing_slots(text):
            return None
        new = decor.stock_base(text)
    elif up.startswith(inventory.PLY_PREFIX):
        th = next((t for t in text.split("_") if _MM.fullmatch(t)), "")
        base = "_".join(t for t in text.split("_") if not _MM.fullmatch(t))
        slots = decor.trailing_slots(base)
        if slots:
            base = "_".join(base.split("_")[: -len(slots)])
        new = f"{base}_{th}" if th else base
    else:
        return None
    return new if new and new != text else None


def execute():
    """Collapse ply and laminate Item codes to their purchasing identity and
    carry each one's assumed rate onto the survivor. Idempotent: a second run
    finds nothing to move, because the collapsed codes collapse to themselves."""
    price_list = inventory.ESTIMATION_PRICE_LIST
    moves, priced, skipped = {}, 0, 0

    for code in frappe.get_all("Item", pluck="name"):
        new = collapsed_code(code)
        if new:
            moves.setdefault(new, []).append(code)

    for new, olds in sorted(moves.items()):
        # The COLLAPSED code is what we hand to ensure_material_item. Handing it
        # a legacy OpenCutList name would just mint the legacy Item again —
        # laminate collapses in decor.substitute_real_code, not in
        # item_code_for, so item_code_for(SG_LAM_V1_16mm_VM6534) is a no-op.
        th = next((float(t[:-2]) for t in new.split("_") if _MM.fullmatch(t)), 0.0)
        try:
            # ensure_material_item is the one place that knows groups, UOMs,
            # conversions and batch settings — reuse it rather than rebuild it.
            inventory.ensure_material_item(
                new, kind=inventory.kind_for_code(new), thickness=th)
        except Exception:
            frappe.log_error(frappe.get_traceback(), f"collapse_board_item_codes {new}")
            skipped += 1
            continue

        if not frappe.db.exists("Item", new):
            skipped += 1
            continue
        if frappe.db.exists("Item Price", {"item_code": new, "price_list": price_list}):
            continue

        # The ceiling across everything collapsing here — an assumed rate is the
        # highest MRP, so merging must not quietly cheapen the board.
        rates = [
            r for r in (
                frappe.db.get_value(
                    "Item Price", {"item_code": o, "price_list": price_list},
                    "price_list_rate")
                for o in olds
            ) if r
        ]
        if not rates:
            continue
        price = frappe.new_doc("Item Price")
        price.item_code = new
        price.price_list = price_list
        price.price_list_rate = max(rates)
        price.insert(ignore_permissions=True)
        priced += 1

    frappe.db.commit()
    print(f"collapse_board_item_codes: {len(moves)} collapsed code(s), "
          f"{priced} rate(s) carried forward, {skipped} skipped")
