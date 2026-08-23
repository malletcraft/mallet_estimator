"""Obviously-fake rates, so CI can check the arithmetic it otherwise cannot.

WHY THIS EXISTS
---------------
Every cost value in this repo is 0 on purpose — salaries, rent, MRPs, markups
live only in the site database, and `mallet_estimator` is public. That rule is
right and nothing here weakens it.

But it left the cost engine untestable end to end. On a CI bench every rate is
0, so "24 minifix housings at 15 minutes costs X" cannot be asserted: the
answer is always 0 and the assertion passes for a reason that has nothing to
do with the maths. A test written that way is worse than none, because it
looks like coverage. That is not hypothetical — 2026-08-23, a test asserted
`labour_total` went UP when a quantity was typed in, and went red on a bench
that was behaving perfectly.

So: this installs rates that are unmistakably synthetic, and only into a test
site. Nothing here is a real Woodugift number, and the values are chosen to be
round enough that a person can check the arithmetic in their head — which is
the whole point. If a number below ever looks plausible as a real rate,
somebody has broken the rule this file exists to respect.

THE NUMBERS, AND WHY THESE
--------------------------
    25 working days x 8 productive hours   = 200 productive hours/month
    carpenter 200,000/month, no bonus      = 200,000 / 200 = 1000/hr
    helper    100,000/month                = 100,000 / 200 =  500/hr
    designer  400,000/month                = 400,000 / 200 = 2000/hr
    monthly rent 0

Those salaries are an order of magnitude above any real wage, deliberately.
An earlier draft used 20,000 for a carpenter, which is entirely plausible for
Pune — and a plausible number in a public repo is one somebody can mistake for
the real one. These cannot be mistaken for anything.

Rent is deliberately zero. It is recovered per square foot across a billable
floor area of 1,042 sqft, which divides into nothing clean and would turn
every expected value into a long decimal nobody can verify by eye. With rent
at zero the identity is exact and memorable:

    any default-crew station  net = carpenter + helper = 1500.00 /hr
    the Design Desk (designer only)                    = 2000.00 /hr

That is enough to prove the whole chain — settings to salary to hourly rate to
operation minutes to a line total — which is what was missing. Rent's own
arithmetic is asserted separately, by composition rather than by a magic
number: net_hr must equal the sum of its components whatever they are.
"""

import frappe

# Productive hours per month, by construction: 25 x 8.
PRODUCTIVE_HOURS_PER_MONTH = 200.0

CARPENTER_SALARY = 200000.0
HELPER_SALARY = 100000.0
DESIGNER_SALARY = 400000.0

# What those salaries mean per hour, which is what the estimate actually uses.
CARPENTER_HR = CARPENTER_SALARY / PRODUCTIVE_HOURS_PER_MONTH   # 1000.00
HELPER_HR = HELPER_SALARY / PRODUCTIVE_HOURS_PER_MONTH         #  500.00
DESIGNER_HR = DESIGNER_SALARY / PRODUCTIVE_HOURS_PER_MONTH     # 2000.00

#: Every station crewed by a carpenter and a helper bills this.
CREW_HR = CARPENTER_HR + HELPER_HR                             # 1500.00

SETTINGS = {
    # The calendar, chosen so the divisor is exactly 200.
    "working_days_per_month": 25,
    "paid_holidays_per_month": 0,
    "national_holidays_per_year": 0,
    "working_hours_per_day": 8,
    "lunch_hours_per_day": 0,
    "bonus_months": 0,
    # The people.
    "carpenter_salary": CARPENTER_SALARY,
    "helper_salary": HELPER_SALARY,
    "designer_salary": DESIGNER_SALARY,
    # The building. Zero on purpose — see the module docstring.
    "monthly_rent": 0,
    # Markups off, so a total is the cost and not the cost plus an opinion.
    # A test that wants to prove a markup applies should set it and say so.
    "markup_material": 0,
    "markup_labor": 0,
    "markup_overhead": 0,
    "markup_design": 0,
}


def install():
    """Put the synthetic rates on this site and return what was there before.

    The return value is not decoration — pass it to restore() in tearDownClass.
    Frappe runs the whole suite in one process against ONE site, so a fixture
    that does not put things back changes the world for every test that runs
    after it. Alphabetically this module lands before test_cost_card, which
    would then be reading rates it never asked for. Leaving that in place would
    be introducing the exact order-dependence that cost a red build on
    2026-08-23.

    REFUSES outside a test run. frappe.flags.in_test is set by the test runner
    and by nothing else, so this cannot be imported into a patch, a hook or a
    console session and quietly overwrite a real Estimate Settings — which
    holds the only copy of the real numbers, and has no undo.
    """
    if not frappe.flags.in_test:
        raise RuntimeError(
            "synthetic_rates.install() is for tests only. Estimate Settings on "
            "a real site holds the ONLY copy of the real cost data.")

    doc = frappe.get_single("Estimate Settings")
    before = {f: doc.get(f) for f in SETTINGS if doc.meta.has_field(f)}
    for field, value in SETTINGS.items():
        if doc.meta.has_field(field):
            doc.set(field, value)
    doc.flags.ignore_permissions = True
    doc.save(ignore_permissions=True)
    frappe.db.commit()
    return before


def restore(before):
    """Put back exactly what install() found. Safe to call with None."""
    if not before:
        return
    doc = frappe.get_single("Estimate Settings")
    for field, value in before.items():
        if doc.meta.has_field(field):
            doc.set(field, value)
    doc.flags.ignore_permissions = True
    doc.save(ignore_permissions=True)
    frappe.db.commit()


#: Item Price rows this module CREATED, so clear_prices() can remove exactly
#: those and never a row that was already there.
_CREATED_PRICES = []


def price(item_code, rate):
    """Give one Item a deliberate planning rate.

    Writes the Estimation (Assumed) price list, which is where material_rate()
    looks first — so this exercises the real resolution order rather than a
    shortcut around it.
    """
    if not frappe.flags.in_test:
        raise RuntimeError("synthetic_rates.price() is for tests only.")

    from mallet_estimator import inventory

    existing = frappe.db.get_value(
        "Item Price",
        {"item_code": item_code, "price_list": inventory.ESTIMATION_PRICE_LIST},
        "name")
    if existing:
        frappe.db.set_value("Item Price", existing, "price_list_rate", rate)
    else:
        doc = frappe.get_doc({
            "doctype": "Item Price",
            "item_code": item_code,
            "price_list": inventory.ESTIMATION_PRICE_LIST,
            "price_list_rate": rate,
            "buying": 1,
        }).insert(ignore_permissions=True)
        _CREATED_PRICES.append(doc.name)
    frappe.db.commit()


def clear_prices():
    """Remove only the Item Prices this module created.

    The material fixture prices a code the rest of the suite also uses, so
    leaving the row behind would hand every later test a rate it never set —
    the same leak restore() exists to prevent on the settings side. Rows that
    were already there are not this module's to touch.
    """
    while _CREATED_PRICES:
        name = _CREATED_PRICES.pop()
        if frappe.db.exists("Item Price", name):
            frappe.delete_doc("Item Price", name, ignore_permissions=True,
                              delete_permanently=True)
    frappe.db.commit()
