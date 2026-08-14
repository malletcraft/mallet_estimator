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

What the role can read is a deliberate list, and as of 2026-08-09 it includes
the cost doctypes. That was Amit's call, made explicitly: being able to check
the cost maths end to end is worth more than keeping the figures out of a
session transcript. The rule it replaces is narrowed, not dropped — cost
figures still never enter this repository, which is public, and where a
committed number is permanent and world-readable. Reading is reversible; a
commit is not.
"""

import frappe
from frappe import _

READONLY_ROLE = "Mallet Read Only"
READONLY_USER = "mallet-readonly@example.invalid"

# Everything needed to answer both "does this estimate look right?" and "does
# its cost maths add up?". The second half — Estimate Settings and the supplier
# rate sheets — carries salaries, rent, markups and MRPs, and is here on an
# explicit decision rather than by drift: a reader that cannot see the rates
# cannot tell you why a number is wrong, only that it looks odd.
READONLY_DOCTYPES = (
    "Estimate",
    "Estimate SKU",
    "Estimate Room",
    "Mallet Decor",
    "Item",
    "Item Price",
    "Project",
    "Customer",
    "Estimate Settings",
    "Supplier Rate Sheet",
)

# Desk UI metadata, not business data: reading these lets the reader RENDER a
# form in a browser (the desk router refuses to route without Page read), so
# an assistant can SEE what a user sees instead of inferring it from doctype
# JSON. Pinned by the same explicit-zero loop as the business doctypes.
READONLY_UI_DOCTYPES = (
    "Page",
    "Workspace",
)

# Reading a cost is now allowed; changing one never is. That is the whole
# guarantee this role makes, and role_is_read_only() asserts it.
COST_DOCTYPES = ("Estimate Settings", "Supplier Rate Sheet")


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

    for dt in READONLY_DOCTYPES + READONLY_UI_DOCTYPES:
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


# ---------------------------------------------------------------------------
# The PLUGIN user — the SketchUp bridge's own identity.
#
# The read-only role above exists so an assistant can VERIFY numbers and can
# never change one. The plugin's job is the opposite: it writes panel part
# lists. Giving it the reader's keys fails by design (the first live push,
# 2026-08-11, did exactly that and got 403), and giving it a human's broad
# keys would park System-Manager powers in a laptop file. So it gets its own
# user with exactly the two doctypes the push/pull surface touches.
# ---------------------------------------------------------------------------

PLUGIN_ROLE = "Mallet Plugin"
PLUGIN_USER = "mallet-plugin@example.invalid"

# read+write+CREATE Estimate SKU: import_parts_csv loads, mutates and saves
# the SKU, and — with the SketchUp file's project binding — creates a missing
# one from the component name (execution/DESIGN.md §1). Items/Files created
# inside the import use ignore_permissions internally.
# read Estimate: so a later plugin version can list an estimate's SKU codes.
# read Project/Customer: the model-binding picker is SELECT-ONLY — clients
# and projects are born in the lead/opportunity phase, never in SketchUp,
# and this role's permissions are what enforce that.
PLUGIN_RWC_DOCTYPES = ("Estimate SKU",)
PLUGIN_RO_DOCTYPES = ("Estimate", "Estimate Room", "Project", "Customer")
# Kept for callers that predate the create grant.
PLUGIN_RW_DOCTYPES = PLUGIN_RWC_DOCTYPES


def ensure_plugin_role():
    """Create the plugin role: read+write on the SKU, read on its context —
    and every other permission explicitly 0, same defence as the reader."""
    if not frappe.db.exists("Role", PLUGIN_ROLE):
        frappe.get_doc({
            "doctype": "Role", "role_name": PLUGIN_ROLE,
            "desk_access": 1, "disabled": 0,
        }).insert(ignore_permissions=True)

    from frappe.permissions import add_permission, update_permission_property

    def pin(dt, allowed):
        if not frappe.db.exists("DocType", dt):
            return
        try:
            add_permission(dt, PLUGIN_ROLE, 0)
        except Exception:
            pass
        all_perms = ("read", "write", "create", "delete", "submit", "cancel",
                     "amend", "import", "export", "print", "email", "share",
                     "report")
        for perm in all_perms:
            try:
                update_permission_property(dt, PLUGIN_ROLE, 0, perm,
                                           1 if perm in allowed else 0)
            except Exception:
                pass

    for dt in PLUGIN_RWC_DOCTYPES:
        pin(dt, ("read", "write", "create"))
    for dt in PLUGIN_RO_DOCTYPES:
        pin(dt, ("read",))
    return PLUGIN_ROLE


@frappe.whitelist()
def create_plugin_api_user(email=None, full_name="Mallet Plugin", regenerate=0):
    """Create (or re-key) the SketchUp plugin's API user and return its
    credentials ONCE — same contract as create_readonly_api_user: a lost
    secret is re-keyed, not recovered, and re-keying is how you revoke."""
    if not frappe.has_permission("User", "create"):
        frappe.throw(_("Only an administrator can create the integration user."),
                     frappe.PermissionError)
    email = (email or PLUGIN_USER).strip().lower()
    ensure_plugin_role()

    if frappe.db.exists("User", email):
        user = frappe.get_doc("User", email)
        if not int(regenerate or 0):
            frappe.throw(
                _("<b>{0}</b> already exists. Tick <b>Regenerate keys</b> to issue a "
                  "new key and secret — the old pair stops working immediately, which "
                  "is also how you revoke access.").format(email),
                title=_("User already exists"))
    else:
        user = frappe.get_doc({
            "doctype": "User", "email": email, "first_name": full_name,
            "user_type": "System User",
            "send_welcome_email": 0,
            "enabled": 1,
        })
        user.flags.ignore_permissions = True
        user.insert(ignore_permissions=True)

    user.set("roles", [])
    user.append("roles", {"role": PLUGIN_ROLE})
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
        "role": PLUGIN_ROLE,
        "read_write": list(PLUGIN_RW_DOCTYPES),
        "read_only": list(PLUGIN_RO_DOCTYPES),
        "header": f"Authorization: token {api_key}:{api_secret}",
    }


# ---------------------------------------------------------------------------
# The DATA STEWARD — the assistant's writing hand for DATA fixes.
#
# Data fixes and code fixes are different things (Amit, 2026-08-13): a wrong
# UOM, a stale item, a decor row pointing nowhere should be corrected in
# minutes over the API, not ride a CI build + bench deploy the way CODE must.
# The steward writes operational DATA; it still cannot touch a rate — keying
# money stays a human act on every assistant identity, by design, and
# steward_is_rate_safe() asserts it the way role_is_read_only() asserts the
# reader.
# ---------------------------------------------------------------------------

STEWARD_ROLE = "Mallet Data Steward"
STEWARD_USER = "data-steward-claude@malletcrafts.com"

# Operational data: full lifecycle (Estimate is submittable, so a fix may need
# cancel -> amend). Item gets no delete: history-carrying masters retire by
# `disabled`, never by deletion.
STEWARD_RWC_DOCTYPES = ("Estimate", "Estimate SKU", "Mallet Decor",
                        "Estimate Room", "File")
STEWARD_RW_DOCTYPES = ("Item", "Manufacturer", "Project", "Customer", "UOM")
# Money: LISTED here so the exclusion is explicit and asserted, not implied.
STEWARD_FORBIDDEN = ("Estimate Settings", "Supplier Rate Sheet", "Item Price")


def ensure_steward_role():
    """Create the steward role: write on operational data, explicit zero on
    everything money — the same explicit-zero pinning as the reader and the
    plugin, including rows for the FORBIDDEN doctypes so an upgrade shipping
    new defaults cannot quietly widen them."""
    if not frappe.db.exists("Role", STEWARD_ROLE):
        frappe.get_doc({
            "doctype": "Role", "role_name": STEWARD_ROLE,
            "desk_access": 1, "disabled": 0,
        }).insert(ignore_permissions=True)

    from frappe.permissions import add_permission, update_permission_property

    def pin(dt, allowed):
        if not frappe.db.exists("DocType", dt):
            return
        try:
            add_permission(dt, STEWARD_ROLE, 0)
        except Exception:
            pass
        all_perms = ("read", "write", "create", "delete", "submit", "cancel",
                     "amend", "import", "export", "print", "email", "share",
                     "report")
        for perm in all_perms:
            try:
                update_permission_property(dt, STEWARD_ROLE, 0, perm,
                                           1 if perm in allowed else 0)
            except Exception:
                pass

    for dt in STEWARD_RWC_DOCTYPES:
        pin(dt, ("read", "write", "create", "delete", "submit", "cancel",
                 "amend", "export", "report"))
    for dt in STEWARD_RW_DOCTYPES:
        pin(dt, ("read", "write", "create", "export", "report"))
    for dt in STEWARD_FORBIDDEN:
        pin(dt, ())        # a row of explicit zeros, on purpose
    # Desk rendering, same read-only grants as the reader.
    for dt in READONLY_UI_DOCTYPES:
        pin(dt, ("read",))
    return STEWARD_ROLE


def steward_is_rate_safe():
    """True when the steward role grants NOTHING on the money doctypes.
    Asserted by verify_setup — the exclusion is the point of the design."""
    rows = frappe.get_all(
        "Custom DocPerm",
        filters={"role": STEWARD_ROLE, "parent": ("in", STEWARD_FORBIDDEN)},
        fields=["parent", "read", "write", "create", "delete", "submit",
                "cancel", "amend"])
    for r in rows:
        if any(r.get(p) for p in ("read", "write", "create", "delete",
                                  "submit", "cancel", "amend")):
            return False, f"{r.parent} grants the steward access to money"
    return True, f"{len(rows)} money doctype(s) pinned to zero"


@frappe.whitelist()
def create_steward_api_user(email=None, full_name="Mallet Data Steward", regenerate=0):
    """Create (or re-key) the data steward's API user and return credentials
    ONCE — same contract as the reader and the plugin: a lost secret is
    re-keyed, not recovered, and re-keying is how you revoke."""
    if not frappe.has_permission("User", "create"):
        frappe.throw(_("Only an administrator can create the integration user."),
                     frappe.PermissionError)
    email = (email or STEWARD_USER).strip().lower()
    ensure_steward_role()

    if frappe.db.exists("User", email):
        user = frappe.get_doc("User", email)
        if not int(regenerate or 0):
            frappe.throw(
                _("<b>{0}</b> already exists. Tick <b>Regenerate keys</b> to issue a "
                  "new key and secret — the old pair stops working immediately, which "
                  "is also how you revoke access.").format(email),
                title=_("User already exists"))
    else:
        user = frappe.get_doc({
            "doctype": "User", "email": email, "first_name": full_name,
            "user_type": "System User",
            "send_welcome_email": 0,
            "enabled": 1,
        })
        user.flags.ignore_permissions = True
        user.insert(ignore_permissions=True)

    user.set("roles", [])
    user.append("roles", {"role": STEWARD_ROLE})
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
        "role": STEWARD_ROLE,
        "full_lifecycle": list(STEWARD_RWC_DOCTYPES),
        "read_write": list(STEWARD_RW_DOCTYPES),
        "never": list(STEWARD_FORBIDDEN),
        "header": f"Authorization: token {api_key}:{api_secret}",
    }
