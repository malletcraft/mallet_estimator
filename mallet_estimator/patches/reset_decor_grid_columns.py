import frappe


def execute():
    """The Laminates décor grid shows brand/code/name but NOT the 'Décor
    (search or create)' Link — a per-user grid layout saved in __UserSettings
    before the decor column existed overrides the doctype defaults (the edge
    grid, opened later, shows it fine). Clear the stale layout so the search
    column shows for everyone. Mirrors reset_material_grid_columns; runs once
    (future Configure-Columns choices persist)."""
    try:
        frappe.db.sql("delete from `__UserSettings` where doctype = %s", "Estimate SKU")
    except Exception:
        frappe.log_error(frappe.get_traceback(), "reset decor grid columns")
    frappe.clear_cache()
