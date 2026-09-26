"""Solid wood and dimensional lumber are stocked and bought by the CUBIC FOOT.

Amit, 2026-09-26, having been shown that the estimate priced neither: "Fix it
on the bench anyway."

WHAT THIS PATCH IS ACTUALLY FOR. The code change is in inventory.KIND_SPEC and
estimator.lumber_lines; neither needs a patch. What does is the UOM. It is
created by install.ensure_manufacturing_masters under after_migrate, and a
PYTHON-ONLY DEPLOY RUNS NO MIGRATE — so on a deploy carrying only .py changes
the unit would sit in the source and never reach the database, and the first
solid-wood Item created would lose its unit in silence
(_add_material_line writes `uom` only `if frappe.db.exists("UOM", line_uom)`).
A patch entry is what forces the migrate.

Idempotent, and deliberately does NOT touch an existing Item's unit: ERPNext
fixes an Item's stock UOM at creation and changing it after a stock ledger
entry is not a thing you undo. The two SW_ Items on staging are test probes
(SW_ZZ_BTN_PROBE, SW_ZZ_VERB_TEST) created at Nos before anything priced
lumber; they are named here so nobody later reads their unit as the rule.
"""

import frappe


def execute():
    if not frappe.db.exists("UOM", "Cubic Foot"):
        frappe.get_doc({"doctype": "UOM", "uom_name": "Cubic Foot"}).insert(
            ignore_permissions=True)
        frappe.db.commit()
