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

from mallet_estimator import integration


def execute():
    integration.ensure_readonly_role()
    integration.ensure_steward_role()
    integration.ensure_photographer_role()
