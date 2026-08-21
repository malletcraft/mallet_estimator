"""Re-pin the roles so the phone can see the site level.

Three new masters shipped without anyone holding read on them. The reader
could not verify the deploy, the steward could not fix a site name typed on
a roof, and — the one that matters — the phone could not read the tree it
browses, which shows up as an EMPTY tree with no error attached. An app
that looks broken and an app that is missing a grant are indistinguishable
from the outside, which is why this is also asserted in verify_setup.

This patch exists to FORCE THE MIGRATE. A Python-only deploy runs none, so
after_migrate never fires and role grants sit in the source doing nothing.
"""

import frappe

from mallet_estimator import integration


def execute():
    # Re-pin where the role EXISTS; never mint one on a site that merely
    # upgraded. install.sync_readonly_role states that rule and this patch
    # has to honour it: a studio that reads nothing should not acquire an
    # integration identity because it took a deploy.
    if frappe.db.exists("Role", integration.READONLY_ROLE):
        integration.ensure_readonly_role()
    if frappe.db.exists("Role", integration.STEWARD_ROLE):
        integration.ensure_steward_role()
    # The photographer is a PERSON's role, kept in step unconditionally --
    # same contract as sync_readonly_role, and the reason the phone can see
    # the tree at all.
    integration.ensure_photographer_role()
