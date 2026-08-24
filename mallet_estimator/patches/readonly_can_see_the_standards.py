"""Let the read-only role see the Operation and Workstation masters.

2026-08-24. Amit, holding the plugin's estimate beside the Estimate SKU form:
"why workstation on mcft plug in and erp are not consistent? ... on erp its
project room on mcft plugin there is no project room."

Answering that needed one fact — what the Operation masters actually say — and
no assistant identity on this site could read it. Both the read-only user and
the steward got 403 on Operation, so the question could only be answered by a
human opening the desk. That is the same gap Estimate Settings had before
2026-08-09, and the same argument closes it: a reader that cannot see the
standard can only say a number looks odd, never why it is wrong.

Read only. The role is pinned read-only on every doctype it holds and
role_is_read_only() asserts it, so this grants seeing and nothing else.

The patch entry is the point. READONLY_DOCTYPES is plain Python, and a
Python-only deploy on Frappe Cloud runs no migrate, so after_migrate never
fires and the grant would sit in the source without ever reaching the database
— which is exactly how the steward's Site Photo grant went missing in August.
"""

import frappe

from mallet_estimator import install


def execute():
    if not frappe.db.exists("DocType", "Operation"):
        return
    install.sync_readonly_role()
