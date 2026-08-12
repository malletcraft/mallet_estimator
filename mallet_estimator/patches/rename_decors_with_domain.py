import frappe


def execute():
    """A Mallet Decor was named {brand} {code} — but a laminate and its
    MATCHING edge band naturally share both (the band is catalogued to the
    laminate it matches), so the second create collided. The autoname now
    carries the domain: 'Virgo Mica 1834 (Laminate)' / 'Virgo Mica 1834
    (Edge Band)'. Rename existing records to the new shape; rename_doc
    re-points every Link (the SKU décor rows) itself."""
    for name in frappe.get_all("Mallet Decor", pluck="name"):
        d = frappe.db.get_value(
            "Mallet Decor", name, ["brand", "code", "domain"], as_dict=True)
        if not d:
            continue
        want = f"{d.brand} {d.code} ({d.domain})"
        if name == want or frappe.db.exists("Mallet Decor", want):
            continue
        frappe.rename_doc("Mallet Decor", name, want, force=True)
