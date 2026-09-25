"""Scope Estimate — the rooms x services conversation with a client."""

import frappe
from frappe import _
from frappe.model.document import Document

from mallet_estimator import scope


class ScopeEstimate(Document):
    def validate(self):
        # One ordered pipeline, computed on save, exactly like Estimate SKU.
        # Nothing here is hand-editable downstream: a rate typed into the
        # matrix would be overwritten on the next save and would have looked
        # like it stuck.
        self._fill_service_rates()
        self._rebuild_matrix()
        self._totals()
        self._warn_about_hollow_cells()

    def _fill_service_rates(self):
        """Blank rate on a service line means 'use the master'.

        Left as an override rather than a fetch_from, because a job that
        negotiated its own rate must keep it when the master moves -- the
        same reasoning as the frozen labour rows on an Estimate SKU.
        """
        for s in self.services or []:
            if not s.article or (s.rate or 0) > 0:
                continue
            rate = frappe.db.get_value("Service Rate", s.article, "rate")
            if rate:
                s.rate = rate

    def _rebuild_matrix(self):
        kept = {(l.room, l.article): (1 if l.include else 0)
                for l in (self.lines or []) if l.room and l.article}
        rows = scope.build_matrix(
            [{"room": r.room, "area_sqft": r.area_sqft} for r in (self.rooms or [])],
            [{"article": s.article, "rate": s.rate} for s in (self.services or [])],
            kept)
        self.set("lines", [])
        for row in rows:
            self.append("lines", row)

    def _totals(self):
        sqft, money = scope.totals(
            [{"include": l.include, "amount": l.amount} for l in (self.lines or [])],
            [{"area_sqft": r.area_sqft} for r in (self.rooms or [])])
        self.total_sqft = sqft
        self.client_total = money

    def _warn_about_hollow_cells(self):
        """A ticked cell worth nothing is under-quoting, out loud.

        A service nobody has rated contributes zero to a total the client is
        being shown as the answer. Saying so on save is the difference between
        a number that is wrong and a number that is wrong and unremarked.
        """
        bad = scope.suspect_cells(
            [{"include": l.include, "rate": l.rate, "area_sqft": l.area_sqft,
              "room": l.room, "article": l.article} for l in (self.lines or [])])
        if not bad:
            return
        shown = "<br>".join(f"{r} &times; {a} &mdash; {why}" for r, a, why in bad[:12])
        more = f"<br>&hellip; and {len(bad) - 12} more" if len(bad) > 12 else ""
        frappe.msgprint(
            _("These ticked cells add nothing to the total:<br>{0}{1}").format(shown, more),
            title=_("Incomplete scope"), indicator="orange")


@frappe.whitelist()
def services_with_rates():
    """The services the matrix can offer: Install and Subcontract articles
    that somebody has actually rated.

    An article with no Service Rate row is deliberately absent rather than
    offered at zero -- a zero-rate service on a client document is a line
    saying the work is free.
    """
    return frappe.get_all(
        "Service Rate",
        filters={"disabled": 0},
        fields=["article as name", "rate"],
        order_by="article")
