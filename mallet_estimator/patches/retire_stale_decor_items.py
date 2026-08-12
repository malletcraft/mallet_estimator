import frappe

# Three generations of laminate item grammar briefly coexisted on the site;
# only the newest is real. DISABLED, not deleted: the old codes carry price
# rows and appear in document history, deletion would be blocked by those
# links, and a disabled Item vanishes from every picker while staying
# auditable. The slot-letter placeholders (SG_LAM_V0_12mm_a_a, EB_PVC_IN_a…)
# are NOT stale — they are the working "unmapped" state every push lands in
# until the décor map is applied, and must stay enabled.
STALE_ITEMS = (
    # Gen-2 laminate codes (board grade + thickness + décor) — superseded by
    # collapse_board_item_codes (2026-08-10): laminate identity is the décor
    # alone (SG_LAM_VM6534); grade and thickness belong to the board item.
    "SG_LAM_V0_12mm_GE1834",
    "SG_LAM_V0_16mm_GE1834",
    "SG_LAM_V1_16mm_GE1834",
    "SG_LAM_V1_16mm_VM1834",
    "SG_LAM_V1_16mm_VM6534",
    # 'Generic' is the ABSENCE of a décor choice, not a décor — these exist
    # because the word was once typed where a brand belongs.
    "SG_LAM_V0_12mm_Generic",
    "SG_LAM_V0_16mm_Generic",
    "SG_LAM_V1_16mm_Generic",
    "SG_LAM_Generic",
    # Misspelt brand-only edge band; the real family is EB_PVC_*_RH<code>.
    "EB_PVC_EX_Rheau",
)


def execute():
    for code in STALE_ITEMS:
        if frappe.db.exists("Item", code):
            frappe.db.set_value("Item", code, "disabled", 1)
