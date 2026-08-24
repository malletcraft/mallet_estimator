"""Packing and Loading happen at the Assembly Station, and packing takes 30 min.

Amit, 2026-08-24: "step 11 and 12 happens at assembly station and not pasting
station" and "Packing default time is 30 minute per unit."

Both are PRICE changes, not relabelling. The workstation is what carries the
hourly rate, so moving a step moves what its minutes cost; and Packing going
from 8 minutes to 30 is nearly four times the time on a line that runs once per
sheet.

Code alone cannot deliver either, for the two reasons this app keeps meeting:
OPERATION_WORKSTATION and OPERATION_STANDARDS are read when an Operation is
CREATED, and these were created months ago; and
install.ensure_manufacturing_masters backfills a standard time only where it is
UNSET, deliberately, so it will not overwrite the 8 that is already there.

Read from the source maps rather than restated here. A number written in two
places is two numbers somebody has to keep equal by hand, which is the fault
this patch is fixing in the first place.
"""

import frappe

from mallet_estimator.estimator import OPERATION_STANDARDS, OPERATION_WORKSTATION

PHASES = ("Packing", "Loading")


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return
    has_min = frappe.get_meta("Operation").has_field("mallet_min_per_unit")

    for phase in PHASES:
        if not frappe.db.exists("Operation", phase):
            continue

        want_ws = OPERATION_WORKSTATION.get(phase)
        if want_ws and frappe.db.exists("Workstation", want_ws):
            have = frappe.db.get_value("Operation", phase, "workstation")
            if have != want_ws:
                frappe.db.set_value("Operation", phase, "workstation", want_ws)
                print("packing_and_loading_at_assembly: %s %r -> %r"
                      % (phase, have, want_ws))
        elif want_ws:
            # Seeded by install.after_migrate in this same migrate, but
            # ordering is not something to bet a price on.
            print("packing_and_loading_at_assembly: workstation %r absent, "
                  "left %s alone" % (want_ws, phase))

        want_min = OPERATION_STANDARDS.get(phase, {}).get("min_per_unit")
        if has_min and want_min:
            have_min = frappe.db.get_value("Operation", phase, "mallet_min_per_unit")
            if float(have_min or 0) != float(want_min):
                frappe.db.set_value("Operation", phase, "mallet_min_per_unit", want_min)
                print("packing_and_loading_at_assembly: %s %s -> %s min/unit"
                      % (phase, have_min, want_min))
