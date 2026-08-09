"""The read-only integration user, created by the app rather than by hand.

An outside tool that reads this site — an assistant checking whether an
estimate looks right, a dashboard, a script — needs an account scoped to
reading and nothing else. Building that by hand is eleven clicks across three
doctypes, and every one of them is a chance to grant too much: it is easier to
tick `write` than to notice you did.

It is also work that has to be repeated verbatim at every studio this
implementation is sold into, which is the real argument for putting it in
code. So the role, its permissions and the user are created here, idempotently,
the same way every other master in this app is — and `verify_setup` asserts
they are still shaped right afterwards.

What the role can read is a deliberate list. `Estimate Settings` is NOT on it:
that Single holds salaries, rent, markups and supplier MRPs, and the standing
rule is that cost data never leaves the site. A reader that cannot open it
cannot leak it, which is a stronger guarantee than remembering not to look.
"""

import frappe
from frappe import _

READONLY_ROLE = "Mallet Read Only"
READONLY_USER = "mallet-readonly@example.invalid"

# Everything needed to answer "does this estimate look right?" and nothing
# that answers "what does it cost us?".
READONLY_DOCTYPES = (
    "Estimate",
    "Estimate SKU",
    "Estimate Room",
    "Mallet Decor",
    "Item",
    "Item Price",
    "Project",
    "Customer",
)

# Named so the exclusion is a decision on the page, not an omission.
NEVER_READABLE = ("Estimate Settings", "Supplier Rate Sheet")


def ensure_readonly_role():
    """Create the role and pin it to read-only on each doctype.

    Every write-ish permission is explicitly set to 0 rather than left at its
    default: a role that merely *starts* read-only can be widened by a later
    ERPNext upgrade shipping different defaults, and nobody would notice."""
    if not frappe.db.exists("Role", READONLY_ROLE):
        frappe.get_doc({
            "doctype": "Role", "role_name": READONLY_ROLE,
            "desk_access": 1,          # the REST API needs desk access
            "disabled": 0,
        }).insert(ignore_permissions=True)

    from frappe.permissions import add_permission, update_permission_property

    for dt in READONLY_DOCTYPES:
        if not frappe.db.exists("DocType", dt):
            continue
        try:
            add_permission(dt, READONLY_ROLE, 0)
        except Exception:
            pass                        # already present
        update_permission_property(dt, READONLY_ROLE, 0, "read", 1)
        for perm in ("write", "create", "delete", "submit", "cancel", "amend",
                     "import", "export", "print", "email", "share", "report"):
            try:
                update_permission_property(dt, READONLY_ROLE, 0, perm, 0)
            except Exception:
                pass                    # not every perm exists on every doctype
    return READONLY_ROLE


def role_is_read_only():
    """True when no doctype grants this role anything beyond reading. Asserted
    by verify_setup, because the guarantee is the point of the role."""
    rows = frappe.get_all(
        "Custom DocPerm", filters={"role": READONLY_ROLE},
        fields=["parent", "read", "write", "create", "delete", "submit", "cancel", "amend"])
    for r in rows:
        if any(r.get(p) for p in ("write", "create", "delete", "submit", "cancel", "amend")):
            return False, f"{r.parent} grants more than read"
    for dt in NEVER_READABLE:
        if any(r.parent == dt for r in rows):
            return False, f"{dt} must never be readable by {READONLY_ROLE}"
    return True, f"{len(rows)} doctype(s), read only"


@frappe.whitelist()
def create_readonly_api_user(email=None, full_name="Mallet Read Only", regenerate=0):
    """Create (or re-key) the read-only API user and return its credentials.

    The secret is returned ONCE and never stored anywhere readable — Frappe
    keeps only an encrypted copy, so a lost secret is re-keyed, not recovered.
    Re-keying invalidates the previous pair, which is also how you revoke.

    Returns {user, api_key, api_secret, role, doctypes}."""
    if not frappe.has_permission("User", "create"):
        frappe.throw(_("Only an administrator can create the integration user."),
                     frappe.PermissionError)
    email = (email or READONLY_USER).strip().lower()
    ensure_readonly_role()

    if frappe.db.exists("User", email):
        user = frappe.get_doc("User", email)
        if not int(regenerate or 0):
            frappe.throw(
                _("<b>{0}</b> already exists. Tick <b>Regenerate keys</b> to issue a new "
                  "key and secret — the old pair stops working immediately, which is "
                  "also how you revoke access.").format(email),
                title=_("User already exists"))
    else:
        user = frappe.get_doc({
            "doctype": "User", "email": email, "first_name": full_name,
            # An API caller is a desk user in Frappe's model — a Website User
            # cannot read doctype records over REST at all. The narrow ROLE is
            # what limits it, not the user type.
            "user_type": "System User",
            "send_welcome_email": 0,
            "enabled": 1,
        })
        user.flags.ignore_permissions = True
        user.insert(ignore_permissions=True)

    # Exactly one role. Anything ERPNext added by default is removed, because a
    # reader that also happens to hold "Sales User" is not a reader.
    user.set("roles", [])
    user.append("roles", {"role": READONLY_ROLE})
    api_key = frappe.generate_hash(length=15)
    api_secret = frappe.generate_hash(length=15)
    user.api_key = api_key
    user.api_secret = api_secret
    user.flags.ignore_permissions = True
    user.save(ignore_permissions=True)
    frappe.db.commit()

    return {
        "user": email,
        "api_key": api_key,
        "api_secret": api_secret,
        "role": READONLY_ROLE,
        "doctypes": list(READONLY_DOCTYPES),
        "header": f"Authorization: token {api_key}:{api_secret}",
    }
