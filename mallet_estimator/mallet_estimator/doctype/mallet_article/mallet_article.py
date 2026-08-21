import re

import frappe
from frappe import _
from frappe.model.document import Document

JOB_TYPES = ("New work", "Repair", "Supply & install")


class MalletArticle(Document):
    """The third token of an SKU code, as a master rather than as a habit.

    YS_MB_WAR was always customer + room + article, but only the first two
    halves had a master behind them; the article was whatever someone typed.
    A picker cannot be offered from a habit, so this is the list."""

    def validate(self):
        self.article_code = (self.article_code or "").strip().upper()
        if not re.fullmatch(r"[A-Z0-9]{2,6}", self.article_code or ""):
            frappe.throw(_("Article code is 2–6 letters or digits, no spaces: {0}")
                         .format(self.article_code))
        self.job_types = normalise_job_types(self.job_types)


def normalise_job_types(value):
    """Accept the three job types in any order and spacing; refuse anything
    else. Stored as a comma-joined string rather than a child table because
    there are exactly three of them and a table costs a join on every read."""
    parts = [p.strip() for p in (value or "").split(",") if p.strip()]
    if not parts:
        frappe.throw(_("An article must apply to at least one job type"))
    bad = [p for p in parts if p not in JOB_TYPES]
    if bad:
        frappe.throw(_("Unknown job type(s): {0}. Expected any of: {1}").format(
            ", ".join(bad), ", ".join(JOB_TYPES)))
    return ", ".join([j for j in JOB_TYPES if j in parts])   # canonical order
