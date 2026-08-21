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
    ALL, ARTICLES, DEFAULT_SITE_NAME, INSTALL, JOB_TYPES, NEW, PHASES,
    REPAIR, STAGE_RENAMES, WORK_STAGES, site_key,
)


# ---------------------------------------------------------------- masters --

def ensure_articles():
    """Seed the article master. Existing rows are left alone apart from the
    job-type list, which is the one field a later batch may legitimately
    widen; the name is not touched because somebody may have corrected it."""
    made = 0
    for code, name, jobs in ARTICLES:
        if frappe.db.exists("Mallet Article", code):
            if frappe.db.get_value("Mallet Article", code, "job_types") != jobs:
                frappe.db.set_value("Mallet Article", code, "job_types", jobs)
            continue
        frappe.get_doc({"doctype": "Mallet Article", "article_code": code,
                        "article_name": name, "job_types": jobs,
                        "default_uom": "Nos"}).insert(ignore_permissions=True)
        made += 1
    frappe.db.commit()
    return made


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


def ensure_site(customer, site_name=None, site_type=None, city=None):
    """Resolve — or create — a site. Matching comes first, always: a
    technician on a roof with one bar of signal types a name, and the job
    here is to land on the office's existing row rather than to make a
    second one beside it."""
    if not customer:
        frappe.throw(_("A site needs a client"))
    site_name = (site_name or "").strip() or DEFAULT_SITE_NAME
    found = find_site(customer, site_name)
    if found:
        return found
    doc = frappe.get_doc({
        "doctype": "Mallet Site", "customer": customer, "site_name": site_name,
        "site_type": site_type or "Flat", "city": city or ""})
    doc.insert(ignore_permissions=True)
    return doc.name
