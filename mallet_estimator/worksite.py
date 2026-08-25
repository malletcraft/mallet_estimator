"""Where the work happens, and how far along it is.

Three masters live here, all seeded imperatively and idempotently the way
every other master in this app is (there are no fixtures):

  Mallet Site        a place a client owns — Client → SITE → Project
  Mallet Article     the third token of an SKU code (WAR, PVC, HNG)
  Mallet Work Stage  one step of a fit-out, in the order the trades run

The interesting decision is the last one. A repair and a supply-and-install
job are not a different sequence from new work — they are a SLICE of it. A
PVC bathroom door is 'Door & window frames' plus 'Doors, shutters &
hardware'. A sagging wardrobe shutter is 'Modular carpentry install'. So a
stage carries the job types that can REACH it, instead of each job type
owning a private list, and one vocabulary covers the six-lakh fit-out and
the eight-thousand-rupee door job alike. Progress reporting then works
across all three without translating anything.

The lists themselves are in worksite_data, which imports no frappe — that
is what lets a unit test assert the trade order without a bench.
"""

import frappe
from frappe import _

from mallet_estimator.worksite_data import (      # noqa: F401  (re-exported)
    ALL, ARTICLES, BASES, BUILD, DEFAULT_SITE_NAME, INSTALL, INSTALL_KIND,
    JOB_TYPES, KINDS, LUMPSUM, NEW, NOS, PHASES, POINT, REPAIR, RFT,
    SERVICE_PREFIX, SQFT, STAGE_RENAMES, SUBCONTRACT, WORK_STAGES, site_key,
    subcontract_item_code,
)


# ---------------------------------------------------------------- masters --

def ensure_articles():
    """Seed the article master. Existing rows are left alone apart from the
    job-type list, which is the one field a later batch may legitimately
    widen; the name is not touched because somebody may have corrected it."""
    _ensure_nos()
    made, errors = 0, []
    for code, name, jobs, kind, basis in ARTICLES:
        try:
            made += _one_article(code, name, jobs, kind, basis)
        except Exception as exc:
            # PER ROW, not per master. The whole loop used to sit inside one
            # _safe() in after_install, so the first row that threw left the
            # master EMPTY and said so nowhere anyone reads — a phone with an
            # empty article picker, and a green deploy (2026-08-21).
            errors.append(f"{code}: {exc}")
    frappe.db.commit()
    if errors:
        frappe.log_error("\n".join(errors), "mallet_estimator ensure_articles")
    return {"made": made, "errors": errors,
            "total": frappe.db.count("Mallet Article")}


def _one_article(code, name, jobs, kind, basis):
    """One article. Returns 1 if it was created, 0 if it already existed."""
    if frappe.db.exists("Mallet Article", code):
        # job types, kind and basis are the MODEL and are kept in step on
        # every migrate — the same rule the stage sequence follows. The name
        # is left alone because somebody may have corrected it. A hand-edited
        # basis is a mistake rather than a preference: it decides which unit
        # the site is asked for, and two articles disagreeing about that is
        # how a quantity ends up meaning nothing.
        current = frappe.db.get_value(
            "Mallet Article", code, ["job_types", "kind", "basis"], as_dict=True)
        want = {"job_types": jobs, "kind": kind, "basis": basis}
        drift = {k: v for k, v in want.items() if (current or {}).get(k) != v}
        if drift:
            frappe.db.set_value("Mallet Article", code, drift)
        return 0
    doc = frappe.get_doc({"doctype": "Mallet Article", "article_code": code,
                          "article_name": name, "job_types": jobs,
                          "kind": kind, "basis": basis})
    doc.insert(ignore_permissions=True)
    return 1


def _ensure_nos():
    """The unit the article master needs, made by the master that needs it.

    Three attempts and two red CI days went into this one function, so the
    reasoning is worth keeping. mallet_article.json carries
    `"default": "Nos"` on default_uom, and nothing creates that UOM before
    after_install seeds the articles. The first fix declined to SET the field
    — useless, because frappe fills defaults itself. The second set it to
    None — also useless, because Document.insert() runs _set_defaults() and
    update_if_missing() treats None as missing and puts "Nos" straight back.

    A default cannot be argued out of existence, so the dependency is simply
    made real. A master that declares a unit should not be at the mercy of
    which order somebody wired the install hooks in."""
    try:
        if not frappe.db.exists("UOM", "Nos"):
            frappe.get_doc({"doctype": "UOM", "uom_name": "Nos"}).insert(
                ignore_permissions=True)
    except Exception:
        # A site whose UOM doctype is missing or whose "Nos" was created by a
        # racing job: seeding carries on, and the per-row errors report what
        # actually happened rather than this guess about it.
        pass


def ensure_subcontract_service_items():
    """An Item per Subcontract article, so a vendor rate has somewhere to live.

    Amit, 2026-08-25: subcontracted work is a full SKU priced from the
    agency's own rate. A rate needs a home, and this app already has exactly
    the right one — a technical Item carrying a buying Item Price per
    supplier, which is how every material vendor price is held. Reusing it
    means the desk screen, the vendor comparison and the ceiling recompute
    all work on day one instead of being rebuilt for services.

    Deliberately NOT stock items: nothing is received, stored or valued. And
    deliberately no rate, here or anywhere in this repo — rates are a human
    act keyed on the site, and this function only makes the place to key them.
    """
    made, errors = 0, []
    for code, name, jobs, kind, basis in ARTICLES:
        if kind != SUBCONTRACT:
            continue
        item_code = subcontract_item_code(code)
        try:
            _ensure_uom(basis)
            if frappe.db.exists("Item", item_code):
                continue
            frappe.get_doc({
                "doctype": "Item",
                "item_code": item_code,
                "item_name": name,
                "description": "%s — subcontracted, quoted per %s" % (name, basis),
                "item_group": _service_item_group(),
                "stock_uom": basis,
                "is_stock_item": 0,
                "is_purchase_item": 1,
                "is_sales_item": 0,
            }).insert(ignore_permissions=True)
            made += 1
        except Exception as exc:
            # Per row, for the same reason ensure_articles is: one throwing
            # row used to leave the whole master empty and say so nowhere.
            errors.append("%s: %s" % (item_code, exc))
    frappe.db.commit()
    if errors:
        frappe.log_error("\n".join(errors),
                         "mallet_estimator ensure_subcontract_service_items")
    return {"made": made, "errors": errors}


def _ensure_uom(name):
    if name and not frappe.db.exists("UOM", name):
        try:
            frappe.get_doc({"doctype": "UOM", "uom_name": name}).insert(
                ignore_permissions=True)
        except Exception:
            pass


def _service_item_group():
    """Services sit in their own group or, failing that, wherever items go.

    A missing group must not stop the seed: an Item in the wrong group is a
    tidiness problem, an Item that does not exist is a rate with nowhere to
    live and a quote that cannot be made."""
    for g in ("Services", "Subcontract Services", "All Item Groups"):
        if frappe.db.exists("Item Group", g):
            return g
    return frappe.db.get_value("Item Group", {"is_group": 0}, "name") or "All Item Groups"


def ensure_work_stages():
    """Seed the 39-stage master. Sequence, phase and job types are kept in
    step with this file on every migrate — they are the ordering rules, and a
    hand edit that reorders the trades is a mistake rather than a preference.
    The note and the disabled flag are the site's own."""
    made = 0
    for seq, phase, stage, jobs, note in WORK_STAGES:
        if frappe.db.exists("Mallet Work Stage", stage):
            frappe.db.set_value("Mallet Work Stage", stage, {
                "phase": phase, "sequence": seq, "job_types": jobs},
                update_modified=False)
            continue
        frappe.get_doc({"doctype": "Mallet Work Stage", "stage_name": stage,
                        "phase": phase, "sequence": seq, "job_types": jobs,
                        "note": note}).insert(ignore_permissions=True)
        made += 1
    frappe.db.commit()
    return made


# ------------------------------------------------------------------ sites --

def find_site(customer, site_name):
    """Match a typed site name against what the office already has."""
    want = site_key(site_name)
    if not customer or not want:
        return None
    for row in frappe.get_all("Mallet Site", filters={"customer": customer},
                              fields=["name", "site_name"], limit_page_length=0):
        if site_key(row.site_name) == want:
            return row.name
    return None


SITE_TYPES = ("Flat", "Bungalow", "Row House", "Office", "Shop", "Other")


def site_types():
    """The Site Type options, read from the doctype rather than restated here.

    The app shows these as chips and must not carry its own copy: a list added
    in the Select and not in the phone is a type nobody can pick, and the
    phone would go on offering a type the server has dropped."""
    try:
        for f in frappe.get_meta("Mallet Site").fields:
            if f.fieldname == "site_type" and f.options:
                got = [o.strip() for o in f.options.split("\n") if o.strip()]
                if got:
                    return got
    except Exception:
        pass
    return list(SITE_TYPES)


def ensure_site(customer, site_name=None, site_type=None, city=None,
                site_address=None):
    """Resolve — or create — a site. Matching comes first, always: a
    technician on a roof with one bar of signal types a name, and the job
    here is to land on the office's existing row rather than to make a
    second one beside it."""
    if not customer:
        frappe.throw(_("A site needs a client"))
    site_name = (site_name or "").strip() or DEFAULT_SITE_NAME
    found = find_site(customer, site_name)
    if found:
        # An existing site is never renamed or retyped from here, but a BLANK
        # address or type is filled: the phone is often where those are first
        # known, and refusing to record them would send someone to the desk to
        # retype what they already typed on site.
        _fill_blanks(found, site_type=site_type, city=city,
                     site_address=site_address)
        return found
    doc = frappe.get_doc({
        "doctype": "Mallet Site", "customer": customer, "site_name": site_name,
        "site_type": site_type or "Flat", "city": city or ""})
    if site_address and doc.meta.has_field("site_address"):
        doc.site_address = site_address
    doc.insert(ignore_permissions=True)
    return doc.name


def _fill_blanks(site, site_type=None, city=None, site_address=None):
    """Fill only what is EMPTY. Overwriting is how a phone with a stale copy
    silently reverts what the office corrected an hour ago."""
    meta = frappe.get_meta("Mallet Site")
    for field, value in (("site_type", site_type), ("city", city),
                         ("site_address", site_address)):
        if not value or not meta.has_field(field):
            continue
        if not frappe.db.get_value("Mallet Site", site, field):
            frappe.db.set_value("Mallet Site", site, field, value,
                                update_modified=False)
