"""Create the service Item behind each Subcontract article.

Amit, 2026-08-25, deciding the question task #41 had been blocked on since
it was written: subcontracted work becomes a FULL Estimate SKU priced from
the vendor's own rate, not a lumpsum line beside the estimate.

A vendor rate needs somewhere to live, and this app already has the right
place — a technical Item carrying a buying Item Price per supplier, exactly
how every material vendor price is held. So each Subcontract article gets a
non-stock Item, SVC_POP for POP and so on, in the article's own unit.

The patch entry is the point, and it is the same lesson as the role grants
before it: ARTICLES is plain Python and a Python-only deploy on Frappe Cloud
runs no migrate, so without an entry in patches.txt the seeder would reach
the source and never the database. The failure would be quiet in the worst
way — a subcontract line with no Item resolves to rate 0, and a quote missing
a trade's cost looks exactly like a complete one.

No rate is created here, nor could one be. Rates are a human act keyed on
the site; this only builds the shelf they sit on.
"""

import frappe

from mallet_estimator import worksite


def execute():
    if not frappe.db.exists("DocType", "Item"):
        return
    worksite.ensure_subcontract_service_items()
