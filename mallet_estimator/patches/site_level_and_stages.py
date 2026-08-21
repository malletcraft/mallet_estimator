"""Give every project a site, and replace the six stage words with phases.

Two migrations that have to happen together, because the app screen that
reads one reads the other.

SITE. ERPNext links a Project straight to a Customer, so until now the
photo tree had two folder levels and both were fields. Adding a container
above Project means every existing project needs one, or the tree shows an
orphan the first time somebody opens it on a phone. Each project gets a
site named after its own project name where that reads like a place, and
'Main site' otherwise — a real row the office can rename, rather than a
null the UI has to special-case forever.

STAGES. The six values Site Photo 360.stage carried — Baseline, Civil,
Wiring, Carpentry, Finishing, Handover — turned out to be PHASES rather
than stages, which is why replacing them loses nothing: each maps onto a
phase of the same meaning. Amit chose replacement over coexistence
(2026-08-21), so the value is rewritten in place and the original is
parked in the hidden mallet_stage_legacy field. Legacy is hidden, never
deleted: nothing is lost, and nobody has to read two vocabularies.

post_model_sync, and not negotiable: it writes to Project.mallet_site and
Site Photo 360.work_stage, neither of which exists until the model sync
has created them.
"""

import frappe

from mallet_estimator import worksite

PLACE_WORDS = ("flat", "bungalow", "villa", "house", "row house", "apartment",
               "office", "shop", "plot", "residence", "home")


def _looks_like_a_place(text):
    """A project called 'Kothrud Flat' names a place; one called 'Wardrobes &
    Beds Ph 1' names work. Only the first is worth reusing as a site name —
    the second would put 'Wardrobes & Beds Ph 1' in the folder tree where a
    building should be, which reads wrong the moment a second project starts
    at the same address."""
    low = (text or "").casefold()
    return any(w in low for w in PLACE_WORDS)


def execute():
    frappe.reload_doc("mallet_estimator", "doctype", "mallet_site")
    frappe.reload_doc("mallet_estimator", "doctype", "mallet_article")
    frappe.reload_doc("mallet_estimator", "doctype", "mallet_work_stage")
    frappe.reload_doc("mallet_estimator", "doctype", "project_stage_log")
    frappe.reload_doc("mallet_estimator", "doctype", "site_photo_360")

    worksite.ensure_articles()
    worksite.ensure_work_stages()

    _backfill_sites()
    _rename_stages()
    frappe.db.commit()


def _backfill_sites():
    meta = frappe.get_meta("Project")
    if not meta.has_field("mallet_site"):
        # The custom field is created by ensure_project_customization on
        # after_migrate, which runs AFTER patches. Create it now rather than
        # skipping — a patch that quietly does nothing is worse than one that
        # fails, because the next migrate will not run it again.
        from mallet_estimator.install import ensure_project_customization
        ensure_project_customization()
        frappe.clear_cache(doctype="Project")
        meta = frappe.get_meta("Project")
        if not meta.has_field("mallet_site"):
            frappe.log_error("Project.mallet_site absent after customization",
                             "site_level_and_stages")
            return

    made = 0
    for p in frappe.get_all("Project",
                            fields=["name", "project_name", "customer"],
                            limit_page_length=0):
        if not p.customer:
            continue        # nothing to hang a site off; the tree shows it under (no client)
        if frappe.db.get_value("Project", p.name, "mallet_site"):
            continue
        title = p.project_name or p.name
        site_name = title if _looks_like_a_place(title) else worksite.DEFAULT_SITE_NAME
        site = worksite.ensure_site(p.customer, site_name)
        frappe.db.set_value("Project", p.name, "mallet_site", site,
                            update_modified=False)
        if not frappe.db.get_value("Project", p.name, "mallet_job_type"):
            frappe.db.set_value("Project", p.name, "mallet_job_type",
                                worksite.NEW, update_modified=False)
        made += 1
    if made:
        frappe.logger().info(f"site_level_and_stages: {made} project(s) given a site")


def _rename_stages():
    meta = frappe.get_meta("Site Photo 360")
    if not (meta.has_field("stage") and meta.has_field("mallet_stage_legacy")):
        return
    for old, new in worksite.STAGE_RENAMES.items():
        rows = frappe.get_all("Site Photo 360", filters={"stage": old},
                              pluck="name", limit_page_length=0)
        for name in rows:
            # The legacy word is written first. If this run dies between the
            # two writes, the next one still finds stage=<old> and repeats
            # both — whereas renaming first would lose the original with no
            # way to tell it had ever been set.
            frappe.db.set_value("Site Photo 360", name,
                                {"mallet_stage_legacy": old, "stage": new},
                                update_modified=False)
        if rows:
            frappe.logger().info(
                f"site_level_and_stages: {len(rows)} capture(s) {old} -> {new}")
