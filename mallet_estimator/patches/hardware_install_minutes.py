"""Set every Install <type> operation to 15 minutes per unit.

Amit, 2026-08-27: "every install hardware operation is 15 minutes per unit.
update it in erp and let it reflect in mcft-plug in i as well."

The six per-type figures these replace (hinges 4, rails 6, handles 3, shelf
supports 1, locks 5, other 2) were my guesses when the hardware split was
built, not measurements. His single number replaces all of them.

THIS PATCH DELIBERATELY OVERWRITES. Every other seeder in this app fills
`mallet_min_per_unit` only where it is UNSET, precisely so a figure tuned by
hand on the master is never clobbered by a deploy — and that rule is right.
This is the exception that proves it: the instruction IS to change the master,
so leaving the existing values alone would do nothing at all and report
success, which is the failure mode this repo keeps meeting.

It is also why it is a patch rather than a change to HARDWARE_STANDARDS alone.
That constant is only a seed for a site that has never had these operations;
on a bench that already carries them it is read from nowhere. The Operation
master is the source of truth for std times, and the only way to move it is
to write to it.

Existing Estimate SKUs are NOT re-priced. Their labour rows are a snapshot
taken when they were built, and a re-price is a person's decision — the
"Reset times from Operations" button on the SKU is the deliberate way to pull
a changed standard into an existing estimate.
"""

import frappe

from mallet_estimator import estimator as E


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return
    if not frappe.get_meta("Operation").has_field("mallet_min_per_unit"):
        return
    for kind, _label in E.HARDWARE_INSTALL_TYPES:
        name = E.hardware_operation(kind)
        if name and frappe.db.exists("Operation", name):
            frappe.db.set_value("Operation", name, "mallet_min_per_unit",
                                E.HARDWARE_MIN_PER_UNIT, update_modified=False)
    frappe.db.commit()
