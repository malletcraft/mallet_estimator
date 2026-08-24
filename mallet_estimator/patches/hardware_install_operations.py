"""Create one Operation per hardware type, so Install Hardware can split.

Amit, 2026-08-24: "rather than a single install hardware line, let this be a
parent line and always divide the hardware by its type ... you have workstation
/ operation master data. use it."

Using the master data means each type needs a master to use. Install Hinges,
Install Drawer Rails and the rest are Operations in their own right, each
carrying its own standard minutes, so a hinge at four minutes and a shelf pin
at one stop being averaged into a single figure that describes neither.

ensure_manufacturing_masters creates them on a fresh install and backfills a
standard only where one is UNSET — which is right, and which is also why this
patch exists: an existing bench has never heard of these names, and a
Python-only deploy runs no migrate to introduce them.

Deliberately does NOT touch Install Hardware itself. The parent keeps its own
standard time; it is simply no longer the only thing on the line. And it does
not invent minutes for an Operation somebody has already tuned — the seeds here
are starting points, and a number a person has set outranks one written in a
tuple months ago.
"""

import frappe

from mallet_estimator import estimator


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return
    has_min = frappe.get_meta("Operation").has_field("mallet_min_per_unit")
    want_ws = estimator.OPERATION_WORKSTATION.get(estimator.HARDWARE_PARENT)

    for kind, _label in estimator.HARDWARE_INSTALL_TYPES:
        name = estimator.hardware_operation(kind)
        mins = estimator.HARDWARE_STANDARDS.get(kind, 0)

        if frappe.db.exists("Operation", name):
            # Fill only what is missing. A standard somebody has tuned on the
            # bench is the truth; the seed is what a site starts from.
            if has_min and mins and not frappe.db.get_value(
                    "Operation", name, "mallet_min_per_unit"):
                frappe.db.set_value("Operation", name, "mallet_min_per_unit", mins)
                print("hardware_install_operations: %s std -> %s" % (name, mins))
            if want_ws and not frappe.db.get_value("Operation", name, "workstation"):
                frappe.db.set_value("Operation", name, "workstation", want_ws)
            continue

        op = frappe.new_doc("Operation")
        op.name = name
        if want_ws and frappe.db.exists("Workstation", want_ws):
            op.workstation = want_ws
        if has_min and mins:
            op.mallet_min_per_unit = mins
        op.insert(ignore_permissions=True)
        print("hardware_install_operations: created %s (%s, %s min/unit)"
              % (name, want_ws, mins))
