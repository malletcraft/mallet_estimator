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
    "Site Photo 360",
    "Site Photo Settings",
    "Site Photo Inbox",
    # The site level and the two masters under it. A reader that cannot see
    # them cannot check whether a deploy actually seeded them, which is the
    # one thing a green deploy does not prove.
    "Mallet Site",
    "Mallet Article",
    "Mallet Work Stage",
    # The manufacturing standards. 2026-08-24: Amit asked why the plugin and
    # ERP disagreed about which workstation an operation runs at, and the
    # answer was in the Operation masters — which no assistant identity could
    # read, so the question could not be answered from outside the desk at
    # all. Same principle that opened Estimate Settings on 2026-08-09: a
    # reader that cannot see the standard can only say a number looks odd,
    # never why it is wrong. Read only, like everything else here.
    "Operation",
    "Workstation",
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
                        "Estimate Room", "File", "Site Photo 360",
                        # The steward operates the Drive sync, so it configures
                        # it and works its inbox. Neither holds money.
                        "Site Photo Settings", "Site Photo Inbox")
STEWARD_RW_DOCTYPES = ("Item", "Manufacturer", "Project", "UOM",
                       # A site name typed one-handed on a roof is exactly the
                       # kind of operational typo the steward exists to fix.
                       "Mallet Site")
# Read/write/create AND delete. No submit/cancel/amend: Customer is not a
# submittable doctype, so granting those would be noise rather than power.
#
# Amit, 2026-08-24, asked to remove a throwaway probe customer: "1 - delete
# yourself." The steward could not — Customer sat in the RW list above, on the
# reasoning that a steward fixes operational data and does not remove master
# records.
#
# What makes this safe is not a list kept here. It is Frappe's own link check,
# which is a better guard than any list could be: a Customer reached by a
# Quotation, a Sales Invoice, a Project, a Mallet Site or an Estimate SKU
# cannot be deleted AT ALL — the attempt raises LinkExistsError and names what
# stands in the way. So the only customers this can remove are ones nothing
# anywhere references, which is exactly the debris case: a probe record, a
# duplicate typed twice, a name entered against the wrong person and corrected
# by creating the right one. A customer with a single document behind it is out
# of reach and stays that way.
STEWARD_RWD_DOCTYPES = ("Customer",)
# Configuration rather than operational data: the trade order and the article
# list are changed by a person at a desk, not by a data fix. Read, never write.
STEWARD_RO_DOCTYPES = ("Mallet Article", "Mallet Work Stage")
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
    for dt in STEWARD_RWD_DOCTYPES:
        pin(dt, ("read", "write", "create", "delete", "export", "report"))
    for dt in STEWARD_RO_DOCTYPES:
        pin(dt, ("read", "export", "report"))
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
def role_report():
    """What each integration role can actually DO right now, read from the
    live Custom DocPerm rows — so a remote session can VERIFY a role's grants
    without holding that role's key (2026-08-15: the plugin role silently
    missed its Project grant and only a failing user found out). Structure
    only — doctype names and permission bits, no data, no secrets; readable
    by any identity that can read an Estimate."""
    frappe.has_permission("Estimate", "read", throw=True)
    out = {}
    for role in INTEGRATION_ROLES:
        rows = frappe.get_all(
            "Custom DocPerm", filters={"role": role},
            fields=["parent", "read", "write", "create", "delete"],
            order_by="parent")
        out[role] = {
            r.parent: "".join(p[0] for p in ("read", "write", "create", "delete") if r.get(p))
            for r in rows}
    return out


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
        "read_write_delete": list(STEWARD_RWD_DOCTYPES),
        "never": list(STEWARD_FORBIDDEN),
        "header": f"Authorization: token {api_key}:{api_secret}",
    }


# ---------------------------------------------------------------------------
# The SITE PHOTOGRAPHER role — a person with a phone, not an integration.
#
# The capture app runs as whoever is logged in, so a site user needs real
# permissions. Handing them a broad ERPNext role to get there would give a
# technician the estimator's cost screens along with the camera. This grants
# exactly the capture surface: make a photo record, attach a file, and read
# the project and room lists needed to file it. Nothing about money, and no
# ability to create users — a role that can make accounts is not a role you
# hand to a phone.
PHOTOGRAPHER_ROLE = "Mallet Site Photographer"
PHOTOGRAPHER_RWC = ("Site Photo 360", "File")
PHOTOGRAPHER_RO = ("Project", "Customer", "Estimate Room", "Site Photo Inbox",
                   # Everything bootstrap() hands the phone. Without these the
                   # app opens on an empty tree with no error to explain it,
                   # which reads as a broken app rather than a missing grant.
                   "Mallet Site", "Mallet Article", "Mallet Work Stage",
                   "Estimate SKU")
PHOTOGRAPHER_FORBIDDEN = ("Estimate Settings", "Supplier Rate Sheet", "Item Price",
                          "User", "Role")


def ensure_photographer_role():
    """Create/re-pin the site-photographer role. Idempotent."""
    if not frappe.db.exists("Role", PHOTOGRAPHER_ROLE):
        frappe.get_doc({
            "doctype": "Role", "role_name": PHOTOGRAPHER_ROLE,
            "desk_access": 1, "disabled": 0,
        }).insert(ignore_permissions=True)

    from frappe.permissions import add_permission, update_permission_property

    def pin(dt, perms):
        if not frappe.db.exists("DocType", dt):
            return
        try:
            add_permission(dt, PHOTOGRAPHER_ROLE, 0)
        except Exception:
            pass
        for perm, value in perms.items():
            try:
                update_permission_property(dt, PHOTOGRAPHER_ROLE, 0, perm, value)
            except Exception:
                pass

    for dt in PHOTOGRAPHER_RWC:
        pin(dt, {"read": 1, "write": 1, "create": 1, "delete": 0,
                 "submit": 0, "cancel": 0, "amend": 0})
    for dt in PHOTOGRAPHER_RO:
        pin(dt, {"read": 1, "write": 0, "create": 0, "delete": 0})
    # Stated, so the exclusion is asserted rather than assumed.
    for dt in PHOTOGRAPHER_FORBIDDEN:
        pin(dt, {"read": 0, "write": 0, "create": 0, "delete": 0})
    return PHOTOGRAPHER_ROLE


def photographer_is_scoped():
    """True when the role cannot reach money, users or roles."""
    rows = frappe.get_all(
        "Custom DocPerm", filters={"role": PHOTOGRAPHER_ROLE},
        fields=["parent", "read", "write", "create", "delete"])
    for r in rows:
        if r.parent in PHOTOGRAPHER_FORBIDDEN and any(
                r.get(p) for p in ("read", "write", "create", "delete")):
            return False, f"{r.parent} must be closed to a site photographer"
    return True, f"{len(rows)} doctype(s), capture only"


# Every integration role, named once, because role_report() is the ONLY way a
# remote session can see whether a role's grants actually reached the database
# — and on 2026-08-16 it listed three roles while a fourth was live, so the
# answer to "did the patch run?" was unobtainable from outside. A report that
# can silently omit a role is worse than no report: it reads as an all-clear.
INTEGRATION_ROLES = (READONLY_ROLE, PLUGIN_ROLE, STEWARD_ROLE, PHOTOGRAPHER_ROLE)

# The doctypes user_access_report will answer about, and no others. A fixed
# list is the containment: the question it can be asked is "can this person do
# the capture job, and are the money doctypes still shut", never "enumerate
# what anyone can reach across this site".
ACCESS_PROBE_DOCTYPES = PHOTOGRAPHER_RWC + PHOTOGRAPHER_RO + PHOTOGRAPHER_FORBIDDEN


@frappe.whitelist()
def grant_photographer(user):
    """Give an EXISTING user the site-photographer role, and nothing else.

    Amit, 2026-08-17: "tick Mallet Site Photographer on pm@… Do it yourself.
    If required, get steward those rights." Handing the steward write on User
    would have satisfied that literally and been the wrong answer — a role
    that can edit users can grant itself any role, so the steward's careful
    exclusion from money would last exactly as long as nobody thought about
    it. This is the narrow version of the same permission.

    What it can do: add ONE named role to a user who already exists.
    What it cannot do: create a user, remove a role, grant any other role, or
    touch a thing this role is not allowed to reach. The role it grants is
    itself asserted harmless before every grant, so widening the role by
    accident disables the granting rather than quietly spreading it.

    Gated on write access to Site Photo Settings: the steward and a System
    Manager have it, a photographer does not — so holding the camera never
    becomes the power to hand it out."""
    frappe.has_permission("Site Photo Settings", "write", throw=True)

    user = (user or "").strip()
    if not frappe.db.exists("User", user):
        # Never create one. Deciding that a person should have a login is not
        # a decision any assistant identity gets to make.
        frappe.throw(_("No such user: {0}. Create the account first.").format(user))

    ensure_photographer_role()
    ok, detail = photographer_is_scoped()
    if not ok:
        frappe.throw(_("Refusing to grant a role that is no longer scoped: {0}")
                     .format(detail))

    doc = frappe.get_doc("User", user)
    if any(r.role == PHOTOGRAPHER_ROLE for r in (doc.roles or [])):
        return {"user": user, "granted": False, "reason": "already had it",
                "access": user_access_report(user)}

    doc.append("roles", {"role": PHOTOGRAPHER_ROLE})
    doc.save(ignore_permissions=True)
    frappe.db.commit()
    return {"user": user, "granted": True, "role": PHOTOGRAPHER_ROLE,
            # Returned so the grant is verified by the same call that made it,
            # rather than trusted because it did not raise.
            "access": user_access_report(user)}


@frappe.whitelist()
def user_access_report(user):
    """What ONE named user can actually do across the capture surface.

    role_report answers "did the grants land on the ROLE". This answers the
    question that actually matters after someone creates an account: did the
    role land on the PERSON, and does the framework agree. It asks
    frappe.has_permission with an explicit user — the same call every real
    request goes through — so a yes here is the yes the site will give, not an
    inference from permission rows.

    It never switches session and never touches credentials: no password, no
    key, no impersonation. Structure only, over a fixed doctype list."""
    frappe.has_permission("Estimate", "read", throw=True)
    user = (user or "").strip()
    if not frappe.db.exists("User", user):
        return {"user": user, "exists": False}

    roles = sorted(frappe.get_roles(user))
    out = {}
    for dt in ACCESS_PROBE_DOCTYPES:
        if not frappe.db.exists("DocType", dt):
            continue
        out[dt] = "".join(
            p[0] for p in ("read", "write", "create", "delete")
            if frappe.has_permission(dt, ptype=p, user=user))

    leaks = [dt for dt in PHOTOGRAPHER_FORBIDDEN if out.get(dt)]
    can_capture = "c" in out.get("Site Photo 360", "") and "c" in out.get("File", "")
    return {
        "user": user, "exists": True,
        "enabled": bool(frappe.db.get_value("User", user, "enabled")),
        "user_type": frappe.db.get_value("User", user, "user_type"),
        "roles": roles,
        "access": out,
        "can_capture": can_capture,
        # Named explicitly rather than left for the reader to spot: a person
        # who can also open the cost screens is the failure this whole role
        # exists to prevent, and it must not be a subtle line in a table.
        "leaks": leaks,
    }
