import frappe
from frappe.model.document import Document

from mallet_estimator.mallet_estimator.doctype.mallet_article.mallet_article import (
    normalise_job_types,
)


class MalletWorkStage(Document):
    """One step of a fit-out, in the order the trades actually run.

    The sequence is not a preference. Three rules hold it together and each
    is a rework bill when broken: services close before the ceiling does;
    the ceiling finishes before plaster and paint, being the dirtiest trade
    in the flat; and panelling and moulding go up before primer, because you
    fill and sand the pin-holes and then paint over the lot."""

    def validate(self):
        self.stage_name = (self.stage_name or "").strip()
        self.job_types = normalise_job_types(self.job_types)


def stages_for(job_type=None, include_disabled=False):
    """The stages a job type can reach, in trade order.

    Filtering happens in Python rather than in SQL because job_types is a
    comma-joined string: a LIKE '%Repair%' would also match a job type that
    merely contains the word, and there is no third table worth adding for
    thirty-nine rows."""
    filters = {} if include_disabled else {"disabled": 0}
    rows = frappe.get_all(
        "Mallet Work Stage", filters=filters,
        fields=["name", "stage_name", "phase", "sequence", "job_types"],
        order_by="sequence asc", limit_page_length=0)
    if not job_type:
        return rows
    return [r for r in rows
            if job_type in [p.strip() for p in (r.job_types or "").split(",")]]


def phases_for(job_type=None):
    """Phase names in trade order, deduped — the nine or ten headings the
    phone filters by. Thirty-nine chips is a wall, not a filter."""
    out = []
    for r in stages_for(job_type):
        if r.phase not in out:
            out.append(r.phase)
    return out
