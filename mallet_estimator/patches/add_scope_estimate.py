"""The rooms x services scope matrix, and the rate master behind it.

Amit, 2026-09-25: "while estimating for a full service, i would like to have a
matrix of rooms and services to be discussed with client. along with its
approximately sqft rate which should be configurable."

A PATCH because a Python-only deploy runs no migrate, so new doctypes would
sit in the source and never reach the database -- and every call that touched
them would fail with something unhelpful about a missing table rather than
saying the migrate had not run.

NOTHING IS SEEDED WITH A RATE. Service Rate rows are created by a person on
the bench; this only makes the doctypes exist. A rate in code would be
permanent and world-readable in a public repo, and no assistant identity may
write one by design.
"""

import frappe


def execute():
    for dt in ("service_rate", "scope_estimate_room", "scope_estimate_service",
               "scope_estimate_line", "scope_estimate"):
        frappe.reload_doc("mallet_estimator", "doctype", dt, force=True)
