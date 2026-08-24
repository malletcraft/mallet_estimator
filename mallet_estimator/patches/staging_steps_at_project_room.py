"""Disassembly, Packing and Loading run in the Project Room.

Amit, 2026-08-24, settling it against what the bench already had: "keep it as
per current erp setup. record it as rule as well." The ERP Operation masters
were right and the code was not.

This SUPERSEDES packing_and_loading_at_assembly, shipped earlier the same day,
which moved Packing and Loading to the Assembly Station on the strength of
"step 11 and 12 happens at assembly station and not pasting station". He was
correcting the Pasting Station, not choosing against the Project Room. That
patch has already run, so its effect has to be undone by a new entry rather
than by editing it — a patch that has executed never runs again. Its OTHER
change, Packing at 30 min/unit, stands and is untouched here.

Disassembly comes with them: it is the same staging work at the same bench,
and it was the one of the three the earlier patch never covered.

THE RULE THIS RECORDS. The workstation carries the hourly rate, so where a
step runs decides what it costs. And a costed workstation with no operation
assigned is not free: the Project Room is 14x15 ft of a floor rented whole, so
its share of the rent is computed and then charged to nothing at all. Every
workstation in WORKSTATIONS must carry work — test_every_costed_workstation_
carries_work asserts it — or its cost has to be deliberately allocated to one
that does.

Force-set, not backfilled. install.ensure_manufacturing_masters deliberately
never overwrites a workstation already set, which is why the code and the
bench could disagree for months without anything saying so.
"""

import frappe

from mallet_estimator.estimator import OPERATION_WORKSTATION

PHASES = ("Disassembly", "Packing", "Loading")


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return

    for phase in PHASES:
        if not frappe.db.exists("Operation", phase):
            continue
        want = OPERATION_WORKSTATION.get(phase)
        if not want:
            continue
        if not frappe.db.exists("Workstation", want):
            # Seeded by install.after_migrate in this same migrate, but
            # ordering is not something to bet a price on.
            print("staging_steps_at_project_room: workstation %r absent, "
                  "left %s alone" % (want, phase))
            continue
        have = frappe.db.get_value("Operation", phase, "workstation")
        if have != want:
            frappe.db.set_value("Operation", phase, "workstation", want)
            print("staging_steps_at_project_room: %s %r -> %r"
                  % (phase, have, want))
