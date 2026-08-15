import frappe


def execute():
    """The Manufacturer 'Rheau' was a misspelling of Rehau; the steward
    renamed the master and fixed the item names, but Mallet Decor documents
    are NAMED from their brand and the doctype forbade renaming — a décor
    called 'Rheau 1834 (Edge Band)' would carry the typo forever. With
    allow_rename now on, migrate the stragglers; idempotent and harmless
    when none remain."""
    for name in frappe.get_all("Mallet Decor",
                               filters={"name": ["like", "%Rheau%"]},
                               pluck="name"):
        want = name.replace("Rheau", "Rehau")
        if not frappe.db.exists("Mallet Decor", want):
            frappe.rename_doc("Mallet Decor", name, want, force=True)
