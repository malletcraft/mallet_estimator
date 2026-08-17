# The site-photo doctypes shipped granting read/write/create to the "All"
# role, which every account carries. So the Mallet Site Photographer role — the
# whole point of which is to be the thing that grants capture access — decided
# nothing at all for them: any login could create, edit and read captures and
# the review inbox.
#
# Guest was still refused (checked live, 2026-08-17), so nothing was public.
# The hole was internal: a sales user, an accounts clerk, any future account
# had the camera whether or not anyone meant to give it to them.
#
# Removing the row from the doctype JSON fixes a FRESH install. It does not
# fix this site: the moment ensure_photographer_role called add_permission,
# frappe copied every standard permission into Custom DocPerm, and from then
# on the custom rows are the ones that count. So the copied "All" row has to
# be deleted here.
import frappe

DOCTYPES = ("Site Photo 360", "Site Photo Inbox")


def execute():
    for dt in DOCTYPES:
        if not frappe.db.exists("DocType", dt):
            continue
        rows = frappe.get_all("Custom DocPerm",
                              filters={"parent": dt, "role": "All"}, pluck="name")
        for name in rows:
            frappe.db.delete("Custom DocPerm", {"name": name})
        if rows:
            frappe.clear_cache(doctype=dt)

    # Re-pin the role that is supposed to carry this access, so a site whose
    # only grant was the "All" row is not left with nobody able to capture.
    from mallet_estimator import integration
    integration.ensure_photographer_role()
    frappe.db.commit()
