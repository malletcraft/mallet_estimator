# A migrate, forced.
#
# Two things in this batch only take effect on a migrate, and a Python-only
# deploy does not run one: the capture doctype gained `capture_kind` (a flat
# photograph is now a first-class capture, not a degraded 360), and
# ensure_articles gained the UOM it depends on. Without a patch entry both sit
# in the source and never reach the database, which is the failure mode this
# repo has already paid for once.
import frappe

from mallet_estimator import worksite


def execute():
    # Every capture that existed before the field did was a 360. Saying so
    # explicitly beats leaving a blank that later code has to guess about.
    if frappe.db.exists("DocType", "Site Photo 360") and \
            frappe.get_meta("Site Photo 360").has_field("capture_kind"):
        frappe.db.sql("""
            UPDATE `tabSite Photo 360`
               SET capture_kind = '360'
             WHERE capture_kind IS NULL OR capture_kind = ''
        """)

    # The stage-history table is new; nothing to backfill, but the migrate
    # this patch forces is what creates it at all.

    # Re-run the seed. Three jobs in one call now: it creates its own UOM,
    # it back-fills kind and basis onto the 26 articles that predate those
    # columns, and it adds the fifteen subcontract trades — POP, tiling,
    # plaster, the three electrical lines — that the site needs somewhere to
    # put. A site where it already succeeded gets a no-op.
    out = worksite.ensure_articles()
    frappe.db.commit()
    if out.get("errors"):
        frappe.log_error(frappe.as_json(out), "photo_kind_and_article_units")
