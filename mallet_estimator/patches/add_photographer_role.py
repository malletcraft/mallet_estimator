# Create the site-photographer role.
#
# A role change is plain Python, and a Python-only deploy gets no migrate on
# Frappe Cloud, so without a patch entry this role would exist in the source
# and never on the site. (2026-08-15 taught this the slow way.)
import frappe  # noqa: F401

from mallet_estimator import integration


def execute():
    integration.ensure_photographer_role()
