"""Let the read-only role see the company's own configuration.

Amit, 2026-08-25, approving it as "config only, no HR": Company, Fiscal Year,
Account, Cost Center, Warehouse, Supplier and Supplier Group join
READONLY_DOCTYPES.

An audit of this site the same morning could not read the Company record and
had to infer the abbreviation from a warehouse name buried inside an item
default — it got the right answer by luck rather than by evidence. These seven
doctypes decide what every other number on the site MEANS: which fiscal year a
document falls in, which account a posting lands on, which warehouse holds the
stock, which supplier is owed what.

That matters most in the weeks ahead. A go-live is mostly the act of checking
that configuration is correct, and a checker that cannot read the configuration
is a checker that hands every check back to a human.

Read only, like everything else in that list — the role is pinned read-only on
every doctype it holds and role_is_read_only() asserts it. HR is deliberately
absent and should stay absent: nothing an assistant does needs somebody's date
of birth or bank account.

The patch entry is the point. READONLY_DOCTYPES is plain Python and a
Python-only deploy on Frappe Cloud runs no migrate, so without this the grant
would reach the source and never the database — which is exactly how the
steward's Site Photo grant went missing in August.
"""

import frappe

from mallet_estimator import install


def execute():
    if not frappe.db.exists("DocType", "Company"):
        return
    install.sync_readonly_role()
