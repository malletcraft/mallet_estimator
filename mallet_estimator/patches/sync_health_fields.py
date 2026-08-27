"""Force the migrate that gives Site Photo Settings its failure fields.

Amit, 2026-08-26, putting the in-house annotator on hold: "we will use
imagemeter for annotation purpose." That makes drive_sync the ONLY route a
drawn measurement has to reach ERP, so a stopped sync now means annotations
quietly never appear.

verify_setup gains a check that the sync has actually RUN, not merely that it
is configured, and the scheduler records WHY a failed run failed onto
last_error / last_error_at — fields that do not exist until a migrate creates
them.

The patch entry is the whole point, and it is the same lesson as the role
grants and the service items before it: a Python-only deploy on Frappe Cloud
runs no migrate. Without an entry here the new fields would reach the source
and never the database, `has_field` would answer False for ever, and the
error-recording path — the one that exists to break the silence — would
itself be silent. That would be a fitting failure and an expensive one.
"""

import frappe


def execute():
    frappe.reload_doctype("Site Photo Settings")
