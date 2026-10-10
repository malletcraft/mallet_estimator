"""The room master gains "Dry Balcony".

Amit, 2026-10-10: "dry balcony is one room. add it in list." A flat's dry
balcony (the washing / utility balcony off the kitchen) is a room of its own,
not the sitting balcony, and the Site Photos 2 BHK preset now lists it.

A PATCH AND NOT JUST after_migrate, for the reason seed_expanded_rooms gives:
a Python-only deploy runs no migrate, so a room added only to DEFAULT_ROOMS
would sit in source while the phone could not file a photo against it. The
patch entry forces the migrate. ensure_rooms adds only and skips what exists,
so this is idempotent. Its abbreviation is DB, which no other room uses.
"""

from mallet_estimator.install import ensure_rooms


def execute():
    ensure_rooms()
