# Who is calling, in the vocabulary the MCFT Interior Estimate web half (S5)
# understands: staff, vendor, client or none.
#
# S5 holds no ERP credential of its own. It forwards whatever the caller
# brought (the APK's API key, or the browser's sid after a login) to
# `mallet_estimator.api.whoami`, and decides what that caller may see from
# the answer. So the classification lives HERE, once, beside the user records
# it reads — never re-derived on the Cloudflare side from guesses.
#
# Pure on purpose: `classify` takes plain values so the rule is testable
# without a bench (tests/test_identity.py). `whoami` in api.py does the reads.

SUPPLIER = "Supplier"
CUSTOMER = "Customer"


def classify(user, full_name, user_type, links):
    """The whoami answer for one user.

    user_type -- the User's user_type ("System User", "Website User", ...).
    links     -- iterable of (link_doctype, link_name) from the Dynamic Links
                 of every Contact whose `user` is this user.

    staff  = System User, whatever else it is linked to.
    vendor = linked to at least one Supplier (wins over client when both).
    client = linked to at least one Customer.
    none   = anything else. Never raises."""
    suppliers, customers = [], []
    for row in links or ():
        try:
            doctype, name = row[0], row[1]
        except Exception:
            continue
        if not name:
            continue
        if doctype == SUPPLIER and name not in suppliers:
            suppliers.append(name)
        elif doctype == CUSTOMER and name not in customers:
            customers.append(name)
    suppliers.sort()
    customers.sort()

    if user_type == "System User":
        kind = "staff"
    elif suppliers:
        kind = "vendor"
    elif customers:
        kind = "client"
    else:
        kind = "none"
    return {"user": user or "", "full_name": full_name or user or "",
            "kind": kind, "suppliers": suppliers, "customers": customers}
