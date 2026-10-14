# Force the migrate that carries the rate-writer role to a live site.
#
# Amit, 14 Oct 2026: "Get yourself rights to key in the rates as well in
# assumed price list so that estimate won't hold up for my input". The role's
# doctype lists are plain Python and its guard is a doc_events hook; a
# Python-only deploy runs no migrate on Frappe Cloud, so the pins never re-apply
# and the hook cache is never cleared. This entry is what forces that migrate.
#
# It re-pins through sync_readonly_role, which touches the rate writer only
# where the role already exists — a site nothing writes rates from does not
# acquire the role because it upgraded. The button on Estimate Settings mints it.
import frappe

from mallet_estimator import install


def execute():
    if not frappe.db.exists("DocType", "Item Price"):
        return
    install.sync_readonly_role()
