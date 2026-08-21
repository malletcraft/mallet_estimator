# Kind, basis, and fifteen trades the shop does not do itself.
#
# photo_kind_and_article_units already ran on staging, and Frappe never runs a
# patch twice — so the article master would have kept the 26 rows it had and
# none of the new columns. This is its own entry for that reason: a patch is
# not a place to add work retroactively.
#
# What it carries: `kind` (Build / Install / Subcontract) and `basis` (Sqft /
# Rft / Point / Nos / Lumpsum) onto every article, plus POP, tiling, plaster,
# painting, demolition, masonry, plumbing, HVAC, glass, cleaning and the three
# electrical lines. Amit quotes electrical as points, running feet and
# fittings, so it is three articles rather than one carrying three numbers.
import frappe

from mallet_estimator import worksite


def execute():
    if not frappe.db.exists("DocType", "Mallet Article"):
        return
    # ensure_articles keeps job types, kind and basis in step with the source
    # on every run — they are the model, not a preference — and creates what
    # is missing. Both halves of this patch are that one call.
    out = worksite.ensure_articles()
    frappe.db.commit()
    if out.get("errors"):
        frappe.log_error(frappe.as_json(out), "article_kind_and_basis")
