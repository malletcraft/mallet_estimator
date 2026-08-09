"""Clear the estimating work off a site without touching its masters.

Staging accumulates trial estimates whose shape no longer matches the app —
SKUs from a retired intake mode, codes from a superseded grammar. Migrating
that data costs more than it is worth when nobody wants to keep it, but
"delete everything" is not the alternative: the masters underneath it are
expensive to rebuild and were never the problem.

So this deletes exactly the estimating layer:

    Estimates -> Estimate SKUs -> the client-article Items those SKUs made

and leaves everything the shop actually curates — material Items and their
prices, suppliers, decors, rooms, settings, workstations, operations,
routings. A client-article Item is only ever created BY an SKU, so removing
it with its SKU restores the site to "no estimates yet", not "empty".

Guarded by a typed phrase rather than a confirm dialog: this is not
reversible, and a button that only needs one careless click eventually gets
one.
"""

import frappe
from frappe import _

from mallet_estimator import inventory

CONFIRM = "DELETE ESTIMATES"


@frappe.whitelist()
def start_over(confirm=None, delete_items=1):
    """Delete every Estimate and Estimate SKU, and the Items the SKUs made.

    `confirm` must be the exact phrase CONFIRM. `delete_items` can be turned
    off to keep the client-article Items — useful if any of them already carry
    stock or sit on a submitted document, since those refuse deletion anyway."""
    if not frappe.has_permission("Estimate", "delete"):
        frappe.throw(_("You do not have permission to delete Estimates."),
                     frappe.PermissionError)
    if (confirm or "").strip() != CONFIRM:
        frappe.throw(
            _("Type <b>{0}</b> to confirm. This deletes every estimate and SKU "
              "on this site and cannot be undone.").format(CONFIRM),
            title=_("Confirmation required"))

    report = {"estimates": 0, "skus": 0, "items": 0, "orphans": 0, "kept": []}

    # Cancel before delete: a submitted Estimate refuses deletion, and the
    # cancel also unfreezes its SKUs, which is what lets them go next.
    for name in frappe.get_all("Estimate", pluck="name"):
        try:
            doc = frappe.get_doc("Estimate", name)
            if doc.docstatus == 1:
                doc.cancel()
            doc.delete(ignore_permissions=True)
            report["estimates"] += 1
        except Exception:
            frappe.log_error(frappe.get_traceback(), f"start_over estimate {name}")
            report["kept"].append(f"Estimate {name}")

    for name, item in frappe.get_all("Estimate SKU", fields=["name", "item"],
                                     as_list=True):
        try:
            frappe.delete_doc("Estimate SKU", name, force=1, ignore_permissions=True,
                              delete_permanently=True)
            report["skus"] += 1
        except Exception:
            frappe.log_error(frappe.get_traceback(), f"start_over sku {name}")
            report["kept"].append(f"Estimate SKU {name}")
            continue
        if int(delete_items or 0) and item:
            report["items"] += _delete_client_item(item, report)

    # Then sweep the group itself. Following the SKU's `item` link only reaches
    # Items whose SKU still exists to be followed — an Item whose SKU was
    # deleted by any other means was invisible to the loop above, and survived
    # every run. That also made this NOT idempotent: running it twice could
    # never clean up what the first run could not see. The group is the real
    # definition of "a client article", so it is what gets swept.
    if int(delete_items or 0):
        for name in frappe.get_all(
                "Item", filters={"item_group": inventory.CLIENT_SKU_GROUP}, pluck="name"):
            report["orphans"] += _delete_client_item(name, report)
        report["items"] += report["orphans"]

    frappe.db.commit()
    frappe.msgprint(
        _("Deleted {0} estimate(s), {1} SKU(s) and {2} client-article Item(s)"
          "{3}. Masters — material Items, prices, suppliers, décors, rooms, "
          "settings — were left alone.{4}")
        .format(report["estimates"], report["skus"], report["items"],
                _(" (of which {0} orphaned — no SKU pointed at them)").format(report["orphans"])
                if report["orphans"] else "",
                "<br><br>Could not delete: " + ", ".join(report["kept"])
                if report["kept"] else ""),
        title=_("Start Over"), indicator="orange")
    return report


def _delete_client_item(item, report):
    """Remove an Item this app created for a finished article.

    Only the client-article group is in scope. A material Item that an SKU
    happens to link to belongs to the rate card, and the rate card is a
    master — deleting it here would take the shop's prices with it."""
    group = frappe.db.get_value("Item", item, "item_group")
    if group != inventory.CLIENT_SKU_GROUP:
        return 0
    try:
        frappe.delete_doc("Item", item, force=1, ignore_permissions=True,
                          delete_permanently=True)
        return 1
    except Exception:
        # Stock, a BOM or a submitted document holds it. That is a reason to
        # leave it, not a reason to fail the whole clear-out.
        frappe.log_error(frappe.get_traceback(), f"start_over item {item}")
        report["kept"].append(f"Item {item}")
        return 0
