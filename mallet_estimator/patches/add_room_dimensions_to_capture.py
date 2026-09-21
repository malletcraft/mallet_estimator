"""Site Photo 360 learns the room it was shot in.

Amit, 2026-09-21: "capture all 3 sides of a room in inches by the user and
decide correct FOV based on the room ... you can automatically adjust FOV for
all 6 faces so that all 4 corners of each face is available by just showing
10% of the adjacent walls."

room_length_in / room_width_in / room_height_in are what the per-face FOV is
computed from at split time. Storing the ROOM and not the six angles keeps one
source of truth, so the phone and the bench cannot disagree about a capture.

A PATCH and not just a doctype change, for the reason this repo has paid for
before: a Python-only deploy runs no migrate at all, so the new fields would
sit in the JSON and never reach the database -- and create_capture guards on
meta.has_field, so the dimensions would be dropped SILENTLY on every upload
while everything reported success.

Nothing is backfilled. A capture taken before this existed has no room
measurement and never will; plan_for_room returns None for it and the split
falls back to the single fov exactly as it always did.
"""

import frappe


def execute():
    frappe.reload_doc("mallet_estimator", "doctype", "site_photo_360")
