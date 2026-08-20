"""Raise the site's max_file_size so the OTA mirror can store the camera APK.

Frappe's default cap is 25 MB and it is enforced at File.insert — every
mirror_apk attempt died on MaxFileSizeReachedError (invisible until the
mirror started surfacing its tracebacks, 2026-08-20). The camera build is
~323 MB; 400 MB leaves headroom without inviting abuse. site_config.json
belongs to the site, so the value survives bench rebuilds; the patch is
idempotent and never lowers an already-higher value.
"""

import frappe
from frappe.installer import update_site_config

LIMIT = 400 * 1024 * 1024


def execute():
    current = frappe.utils.cint(frappe.conf.get("max_file_size"))
    if current < LIMIT:
        update_site_config("max_file_size", LIMIT)
