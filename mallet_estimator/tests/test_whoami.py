# mallet_estimator.api.whoami — the identity MCFT Interior Estimate (S5) reads.
#
# S5 forwards a caller's own ERP credential here and shows them only what the
# answer allows, so the three kinds it distinguishes are asserted against REAL
# records: a System User, a Website User whose Contact links to a Supplier,
# and one whose Contact links to a Customer.
import frappe

from mallet_estimator import api

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase


STAFF = "zz-whoami-staff@example.com"
VENDOR = "zz-whoami-vendor@example.com"
CLIENT = "zz-whoami-client@example.com"
LONER = "zz-whoami-none@example.com"


def _user(email, first_name, user_type, roles=()):
    if not frappe.db.exists("User", email):
        frappe.get_doc({
            "doctype": "User", "email": email, "first_name": first_name,
            "user_type": user_type, "send_welcome_email": 0,
            # Frappe derives user_type from roles on save: a System User with
            # no desk role is saved as a Website User.
            "roles": [{"role": r} for r in roles],
        }).insert(ignore_permissions=True)
    return email


def _supplier():
    name = "ZZ Whoami Vendor"
    existing = frappe.db.get_value("Supplier", {"supplier_name": name}, "name")
    if existing:
        return existing
    doc = {"doctype": "Supplier", "supplier_name": name}
    group = frappe.db.get_value("Supplier Group", {"is_group": 0}, "name")
    if group:
        doc["supplier_group"] = group
    return frappe.get_doc(doc).insert(ignore_permissions=True).name


def _customer():
    name = "ZZ Whoami Client"
    existing = frappe.db.get_value("Customer", {"customer_name": name}, "name")
    if existing:
        return existing
    return frappe.get_doc({
        "doctype": "Customer", "customer_name": name, "customer_type": "Individual",
    }).insert(ignore_permissions=True).name


def _contact(email, first_name, link_doctype, link_name):
    if frappe.db.exists("Contact", {"user": email, "first_name": first_name}):
        return
    frappe.get_doc({
        "doctype": "Contact", "first_name": first_name, "user": email,
        "email_ids": [{"email_id": email, "is_primary": 1}],
        "links": [{"link_doctype": link_doctype, "link_name": link_name}],
    }).insert(ignore_permissions=True)


class TestWhoami(MalletTestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        frappe.set_user("Administrator")
        _user(STAFF, "ZZ Staff", "System User", roles=("System Manager",))
        _user(VENDOR, "ZZ Vendor", "Website User")
        _user(CLIENT, "ZZ Client", "Website User")
        _user(LONER, "ZZ Nobody", "Website User")
        cls.supplier = _supplier()
        cls.customer = _customer()
        _contact(VENDOR, "ZZ Vendor Contact", "Supplier", cls.supplier)
        _contact(CLIENT, "ZZ Client Contact", "Customer", cls.customer)

    def tearDown(self):
        frappe.set_user("Administrator")

    def _as(self, user):
        frappe.set_user(user)
        out = api.whoami()
        frappe.set_user("Administrator")
        for key in ("user", "full_name", "kind", "suppliers", "customers"):
            self.assertIn(key, out, f"whoami missing {key}")
        self.assertEqual(out["user"], user)
        return out

    def test_whitelisted_and_not_for_guests(self):
        self.assertIn(api.whoami, frappe.whitelisted)
        self.assertNotIn(api.whoami, frappe.guest_methods)

    def test_system_user_is_staff(self):
        self.assertEqual(self._as(STAFF)["kind"], "staff")

    def test_administrator_is_staff(self):
        self.assertEqual(self._as("Administrator")["kind"], "staff")

    def test_website_user_linked_to_supplier_is_vendor(self):
        out = self._as(VENDOR)
        self.assertEqual(out["kind"], "vendor")
        self.assertEqual(out["suppliers"], [self.supplier])
        self.assertEqual(out["customers"], [])

    def test_website_user_linked_to_customer_is_client(self):
        out = self._as(CLIENT)
        self.assertEqual(out["kind"], "client")
        self.assertEqual(out["customers"], [self.customer])
        self.assertEqual(out["suppliers"], [])

    def test_unlinked_website_user_is_none(self):
        out = self._as(LONER)
        self.assertEqual(out["kind"], "none")
        self.assertEqual(out["suppliers"], [])
        self.assertEqual(out["customers"], [])
