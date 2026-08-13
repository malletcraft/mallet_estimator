import frappe


def execute():
    """The joinery rule (glue per BOARD) and the wastage display (sqft + Rs
    per panel code) change what an import writes — but an already-imported
    SKU only picks that up when its import re-runs, and the change-detection
    guard rightly sees an unchanged CSV. Re-run it here, as the deploy, so no
    human has to click Re-import per SKU. Frozen (quoted) SKUs are skipped:
    their numbers are a promise already made; they update on Cancel-Amend."""
    for name in frappe.get_all(
            "Estimate SKU",
            filters={"estimation_mode": "CSV-Nest"},
            pluck="name"):
        doc = frappe.get_doc("Estimate SKU", name)
        if doc.get("rates_frozen") or not doc.get("parts_csv"):
            continue
        try:
            doc.reimport()
        except Exception:
            frappe.log_error(frappe.get_traceback(), f"reimport_nest_skus: {name}")
