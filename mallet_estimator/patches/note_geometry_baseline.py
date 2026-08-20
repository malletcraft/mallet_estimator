"""No-op: exists to force the migrate that model-syncs the baseline fields.

A Python-only deploy runs no migrate, so geometry_baseline / baseline_frozen
would sit in the source and never reach the database. Registering a patch is
what makes the migrate happen.
"""


def execute():
    pass
