"""Set the PARENT, Install Hardware, to 15 minutes per unit as well.

Amit, 2026-08-27, on reading the six children at 15 and the parent still at
30: "Set it to 15 as well."

WHY THIS IS A SECOND PATCH AND NOT AN EDIT TO THE FIRST. hardware_install_minutes
had already run on mcft-stg by the time he said it. Frappe records applied
patches in the Patch Log and never runs one twice, so editing that file would
have changed the source, passed CI, deployed green — and left the bench at 30
for ever. That is precisely the shape this repo keeps meeting: a change that
reports success having done nothing. A new entry is the only thing that
executes again.

The parent's own figure is unused whenever children are present — its time is
computed as their sum — so 30 was stale rather than wrong. It still matters
twice over: it is the FALLBACK for a hardware line whose type the classifier
could not match, and it is what a person scanning the Operation list reads.
Two figures for one idea is how a stale number outlives its reason.

Overwrites deliberately, for the same reason the first one did: the
instruction is to change the master, so respecting the existing value would
do nothing and call it done.
"""

import frappe

from mallet_estimator import estimator as E


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return
    if not frappe.get_meta("Operation").has_field("mallet_min_per_unit"):
        return
    if frappe.db.exists("Operation", E.HARDWARE_PARENT):
        frappe.db.set_value("Operation", E.HARDWARE_PARENT, "mallet_min_per_unit",
                            E.HARDWARE_MIN_PER_UNIT, update_modified=False)
        frappe.db.commit()
