# Re-pin the integration roles from the code's doctype lists.
#
# Those lists are plain Python, and a deploy that changes ONLY Python does not
# get a migrate on Frappe Cloud — so after_migrate, which re-pins the roles,
# never runs. 2026-08-15: the steward's grant on Site Photo Settings / Site
# Photo Inbox shipped in e38cce8, the site reported the new commit, and the
# steward still got 403 because no migrate had touched the permissions.
#
# A patch entry is what forces that migrate. The rule this leaves behind: a
# change to a role's doctype list must ship WITH a patch, or it is a change
# that only exists in the source.
import frappe

from mallet_estimator import install


def execute():
    if not frappe.db.exists("DocType", "Site Photo Settings"):
        return
    install.sync_readonly_role()
