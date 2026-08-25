import frappe
from frappe.model.document import Document

from mallet_estimator.estimator import (
    WORKSTATIONS, working_hours_per_month, working_days_per_month,
    productive_hours_per_day, workstation_rates, staff_rates, transport_rates,
)


class EstimateSettings(Document):
    pass


@frappe.whitelist()
def cost_calculator():
    """Reference breakdown of each workstation's hourly charge from the current
    settings — direct labour (carpenter+helper crew), indirect machine
    depreciation, and prorated factory space cost. Use it to decide/verify the
    Workstation master rates. Nothing here is stored; the live rates live on the
    ERPNext Workstation records."""
    s = frappe.get_single("Estimate Settings")
    whm = working_hours_per_month(s)
    billable_area = sum(w["area_sqft"] for w in WORKSTATIONS if w["area_sqft"] > 0)
    factory_area = (s.factory_length_ft or 0) * (s.factory_width_ft or 0)
    rows = workstation_rates(s)
    total_month = sum(r["rent_hr"] * whm for r in rows)
    roles = staff_rates(s)
    return {
        "rows": rows,
        "working_days_per_month": working_days_per_month(s),
        "productive_hours_per_day": productive_hours_per_day(s),
        "working_hours_per_month": whm,
        "monthly_rent": s.monthly_rent,
        "billable_area": billable_area,
        "factory_area": factory_area,
        "free_area": max(factory_area - billable_area, 0),
        "rent_per_sqft_month": (s.monthly_rent / billable_area) if billable_area else 0,
        "staff_rates": roles,
        "crew_rate": roles.get("carpenter", 0) + roles.get("helper", 0),
        "transport_rates": transport_rates(s),
        "rent_recovered_month": round(total_month),
        # WHAT ERP ACTUALLY CHARGES, beside what these settings imply.
        #
        # Amit, 2026-08-24: "Workstation Cost Calculator table on this page
        # should show live figures from the actual erp workstation operation
        # side." Everything above is computed FROM THE SETTINGS — the reference
        # answer, what a rate OUGHT to be. The money an estimate is actually
        # made of comes off the Workstation records, and the two can differ: a
        # rate hand-tuned on the master, a component somebody added, a station
        # whose costs were never keyed at all.
        #
        # Showing only one of them is how a page like this becomes reassuring
        # and wrong. Both are here, so a divergence is something you can SEE.
        "live": _live_reference(),
    }


def _live_reference():
    """The operations and workstation rates as ERP holds them, right now.

    Read through api.cost_card rather than re-queried here, deliberately: that
    is the same call the SketchUp plugin prices against, so this page cannot
    drift from the estimate it exists to explain. A second implementation would
    be a second set of numbers somebody has to keep equal, and this app has
    spent a whole day on exactly that class of failure.
    """
    from mallet_estimator import api, estimator

    try:
        card = api.cost_card()
    except Exception:
        # A settings page that refuses to open because one rate is missing
        # helps nobody. The tables say they could not be read instead.
        frappe.log_error(frappe.get_traceback(), "estimate settings live reference")
        return {"workstations": [], "operations": [], "hardware": [],
                "error": "Could not read the live ERP masters."}

    hardware = []
    for kind, label in estimator.HARDWARE_INSTALL_TYPES:
        op_name = estimator.hardware_operation(kind)
        mins, src = api._op_minutes(op_name, estimator.HARDWARE_STANDARDS.get(kind, 0))
        hardware.append({
            "kind": kind, "name": op_name, "label": label,
            "min_per_unit": mins, "min_source": src,
            "workstation": estimator.OPERATION_WORKSTATION.get(op_name, ""),
        })

    # The canonical component order, sent so the page can build one column per
    # component with every workstation lined up under it. Derived from the same
    # list the Workstation seeder uses, plus anything a live workstation carries
    # that the list does not name — an unrecognised component is the one most
    # worth seeing, not the one to hide.
    order, seen = [], set()
    for c in estimator.WS_COMPONENTS:
        order.append(c)
        seen.add(c)
    for w in card.get("workstations") or []:
        for name, _v in (w.get("components") or []):
            if name not in seen:
                order.append(name)
                seen.add(name)

    return {
        "workstations": card.get("workstations") or [],
        "components": order,
        "operations": card.get("operations") or [],
        "hardware": hardware,
        "parent": estimator.HARDWARE_PARENT,
        "sku_rule": SKU_RULE,
    }


# THE SKU RULE, published where the person estimating can read it.
#
# Amit, 2026-08-24: "code yourself as rule and publish this rule on [Estimate
# Settings]." A convention that lives only in a commit message is a convention
# the next person breaks.
SKU_RULE = {
    "title": "Every SKU is one assembly, sized L / M / S",
    "lines": [
        "A SKU is a TOP-LEVEL component in the SketchUp model. One component, "
        "one SKU, one set of operations, one material list.",
        "Its name declares its size: MCFT_ASMBL_L_…, MCFT_ASMBL_M_… or "
        "MCFT_ASMBL_S_…. The tail after the size is ROOM_ARTICLE, so "
        "MCFT_ASMBL_L_MB_WAR becomes SKU YS_MB_WAR for customer YS.",
        "A name with no size token counts as LARGE — every model drawn before "
        "this convention says plain ASMBL_WAR, and those are carcasses.",
        "The size decides the WORK, not just the label: only large assemblies "
        "are disassembled, travel apart and are re-assembled on site. A drawer "
        "box goes in one piece and is never taken apart.",
        "Estimating covers the whole project; EXECUTION covers whichever SKUs "
        "the job needs next. One SKU per assembly is what makes it possible to "
        "pull them in working order — pasting, cutlist, label printing — "
        "instead of all or nothing.",
    ],
}
