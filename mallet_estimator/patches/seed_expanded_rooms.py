"""The room master reaches 5 BHK and the row house.

Amit, 2026-09-21: "expand a master list as its not even covering 3 BHK in
pune. Cover till 5 bhk and typical row houses."

A PATCH AND NOT JUST after_migrate, for the reason this repo has paid for
before: a Python-only deploy runs no migrate at all, so ensure_rooms would
never fire and the sixteen new rooms would sit in source, unreachable, while
the phone kept offering thirteen. The patch entry is what forces the migrate.

Idempotent by construction — ensure_rooms skips a room that exists — so this
is safe on a site that already has some of them, and safe to re-run.

It ADDS ONLY. Nothing is renamed or removed: "Bathroom" stays exactly as it
is because captures are already filed against it, and a room that vanished
would orphan them.
"""

from mallet_estimator.install import ensure_rooms


def execute():
    ensure_rooms()
