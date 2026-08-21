import frappe
from frappe import _
from frappe.model.document import Document


class MalletSite(Document):
    """A place a client owns. People own more than one flat, and a project
    belongs to a building rather than to a person — which is why this sits
    between Customer and Project rather than being a field on either."""

    def validate(self):
        self.site_name = (self.site_name or "").strip()
        if not self.site_name:
            frappe.throw(_("A site needs a name"))
        # Two sites of the same name under one client are always a mistake —
        # someone typed the second one on a phone with no signal and the
        # matcher missed it. Catching it here is cheaper than merging photos
        # out of the wrong folder later.
        clash = frappe.db.exists("Mallet Site", {
            "customer": self.customer, "site_name": self.site_name,
            "name": ("!=", self.name)})
        if clash:
            frappe.throw(_("{0} already has a site called {1} ({2})").format(
                self.customer_name or self.customer, self.site_name, clash))
