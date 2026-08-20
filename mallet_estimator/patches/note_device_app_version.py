# The device_app_version field on Site Photo 360 arrives via model sync,
# which only runs inside a migrate — and a Python-only deploy runs none.
# This entry exists to force that migrate (proven necessity: the integration
# role grants of 2026-08-13 sat unapplied until a patch dragged migrate in).
def execute():
    pass
