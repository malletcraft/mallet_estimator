"""Carry the Mallet Site address field to the database.

Amit, 2026-08-22: "address should be one more separate field where address of
taht site will be keyed in." The field itself rides in mallet_site.json and
model sync creates the column; this patch exists to FORCE that sync to happen.
A Python-only deploy runs no migrate, and a doctype whose column never lands
is a field the app writes into a 500.

It also carries the one-time data move that makes the new field honest: the
legacy `address` was a Link to ERPNext's Address doctype, and where one was
actually chosen its readable text is copied across so nothing that was
recorded stops being visible. The Link is hidden, never dropped, so the
original value stays exactly where it was.
"""

import frappe


def execute():
    frappe.reload_doc("mallet_estimator", "doctype", "mallet_site")

    meta = frappe.get_meta("Mallet Site")
    if not meta.has_field("site_address") or not meta.has_field("address"):
        return

    moved = 0
    for row in frappe.get_all("Mallet Site", filters={"address": ["!=", ""]},
                              fields=["name", "address", "site_address"]):
        if row.site_address:
            continue          # someone already typed one; it wins
        try:
            text = frappe.db.get_value(
                "Address", row.address,
                ["address_line1", "address_line2", "city", "pincode"],
                as_dict=True)
        except Exception:
            text = None
        if not text:
            continue
        parts = [text.get(k) for k in
                 ("address_line1", "address_line2", "city", "pincode")]
        joined = ", ".join(p for p in parts if p)
        if joined:
            frappe.db.set_value("Mallet Site", row.name, "site_address", joined,
                                update_modified=False)
            moved += 1

    if moved:
        print("site_address: filled %d site(s) from the legacy Address link" % moved)
