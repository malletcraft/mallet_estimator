"""Re-pin the steward role after Customer gained delete.

Amit, 2026-08-24, asked to remove a throwaway probe customer and told me to do
it rather than do it himself: "1 - delete yourself." The steward could not —
Customer was in the read/write list, so the identity that exists precisely to
clean up operational debris could create a customer and correct one but never
remove one.

The guard is not a list in this app. It is Frappe's link check: a Customer
reached by a Quotation, a Sales Invoice, a Project, a Mallet Site or an
Estimate SKU cannot be deleted at all, and the attempt says what is in the way.
So this reaches only customers nothing anywhere references.

The patch entry is what makes the grant real. STEWARD_RWD_DOCTYPES is plain
Python and a Python-only deploy on Frappe Cloud runs no migrate, so
after_migrate never fires and the permission would live in the source and never
in the database — which is exactly how the steward's Site Photo grant went
missing in August.
"""

import frappe

from mallet_estimator import install


def execute():
    if not frappe.db.exists("DocType", "Customer"):
        return
    # sync_readonly_role re-pins EVERY integration role, the steward included,
    # each inside its own guard. The name is older than the job it does.
    install.sync_readonly_role()
