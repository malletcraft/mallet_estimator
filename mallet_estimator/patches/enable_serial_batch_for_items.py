import frappe

from mallet_estimator.install import ensure_stock_settings


def execute():
    """ERPNext v16 refuses has_serial_no on an Item while Stock Settings'
    'Enable Serial and Batch No for Item' is off, and every Estimate SKU
    creates its finished article serialized. Fresh sites get the flag from
    after_install; this entry forces it onto sites that already exist."""
    ensure_stock_settings()
    frappe.db.commit()
