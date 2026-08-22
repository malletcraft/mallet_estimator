"""Move Loading from the site to the factory.

Amit, 2026-08-22: "12. Loading On-Site should be in factory as loading is done
at factory for packed articles."

The workstation on an Operation is what carries the hourly RATE, so this is a
price change and not a relabelling: the same minutes cost factory money now
rather than site money. Only Loading moves — Unloading, Transport, on-site
Assembly and Installation genuinely happen at the site and are left alone.

Code alone would not have done it. OPERATION_WORKSTATION is read when an
Operation is CREATED, and these were created months ago; without this patch
staging would go on quoting Loading at the site rate while the source said
otherwise, which is the quietest kind of wrong.
"""

import frappe

from mallet_estimator.estimator import OPERATION_WORKSTATION


def execute():
    want = OPERATION_WORKSTATION.get("Loading")
    if not want or not frappe.db.exists("DocType", "Operation"):
        return
    if not frappe.db.exists("Operation", "Loading"):
        return
    if not frappe.db.exists("Workstation", want):
        # The station is seeded by install.after_migrate, which runs in the
        # same migrate — but ordering is not something to bet a price on.
        print("loading_is_factory_work: workstation %r absent, skipped" % want)
        return

    have = frappe.db.get_value("Operation", "Loading", "workstation")
    if have == want:
        return
    frappe.db.set_value("Operation", "Loading", "workstation", want)
    print("loading_is_factory_work: Loading moved from %r to %r" % (have, want))
