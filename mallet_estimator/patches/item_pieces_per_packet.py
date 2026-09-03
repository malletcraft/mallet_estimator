"""T2 — the Item field that says how many pieces come in one packet.

A PATCH rather than a bare hooks entry, and this is a rule the project has
already paid for: a Python-only deploy runs no migrate, so after_migrate never
fires and a custom field defined in source never reaches the database. The
patch entry is what forces the migrate.

It creates nothing of its own — the field definition lives in
inventory.CUSTOM_FIELDS and create_custom_fields is idempotent — so this is
deliberately thin. Its job is to make the deploy actually run.
"""

import frappe


def execute():
    from frappe.custom.doctype.custom_field.custom_field import create_custom_fields

    from mallet_estimator import inventory

    create_custom_fields(inventory.CUSTOM_FIELDS, ignore_validate=True)
    frappe.clear_cache(doctype="Item")
