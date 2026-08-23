"""Minifix Boring is 15 minutes a housing, not 30.

Amit, 2026-08-23: "minifix boaring is 15 min per unit."

This was the largest single line on the labour card and it was wrong by
double: at 30 min/unit a wardrobe with 24 minifix housings booked 12 hours of
a ~27 hour estimate to one operation. Halving it takes about 6 hours off that
article, so it moves quoted prices — which is exactly why it ships as a patch
and is announced rather than slipped in.

Code alone could not have done it, twice over. OPERATION_STANDARDS is read
only when an Operation is CREATED, and Minifix Boring was created months ago;
and install.ensure_manufacturing_masters backfills the standard time ONLY
where it is unset, deliberately, so it never clobbers a hand-tuned value. A
value that is already wrong is exactly the case that rule protects, so the
correction has to arrive as its own act.

Nor could the correction go through the steward, which is where a data fix
normally belongs: the steward's roles cover operational data and Operation is
not among them, and neither is the read-only API role. A patch runs as
Administrator, which is the only identity that can write this.

Set to whatever OPERATION_STANDARDS says rather than to a number repeated
here, and set it regardless of what the site currently holds. The patch runs
once and is recorded in the Patch Log, so there is no re-run to guard against;
guarding on the old 30 would quietly do nothing if the value had drifted to
something else wrong in the meantime.
"""

import frappe

from mallet_estimator.estimator import OPERATION_STANDARDS

OPERATION = "Minifix Boring"


def execute():
    # Read the standard rather than restate it. A number written here as well
    # as in OPERATION_STANDARDS is two numbers that have to be kept equal by
    # hand, and the whole reason this patch exists is that a standard time
    # drifted away from what the source said.
    minutes = OPERATION_STANDARDS.get(OPERATION, {}).get("min_per_unit")
    if not minutes:
        return
    if not frappe.db.exists("DocType", "Operation"):
        return
    if not frappe.get_meta("Operation").has_field("mallet_min_per_unit"):
        # The custom field is created by install.after_migrate in this same
        # migrate. Ordering is not something to bet a quoted price on.
        print("minifix_boring_std_time: mallet_min_per_unit absent, skipped")
        return
    if not frappe.db.exists("Operation", OPERATION):
        return

    have = frappe.db.get_value("Operation", OPERATION, "mallet_min_per_unit")
    if float(have or 0) == float(minutes):
        return
    frappe.db.set_value("Operation", OPERATION, "mallet_min_per_unit", minutes)
    print("minifix_boring_std_time: %s %s -> %s min/unit" % (OPERATION, have, minutes))
