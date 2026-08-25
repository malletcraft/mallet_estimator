from frappe.model.document import Document


class EstimateSubcontractLine(Document):
    """One trade, at one vendor's rate, in the article's own unit.

    Nothing is computed here. The parent SKU resolves the rate and the amount
    in one pass so that every line on a save is priced by the same rules at
    the same moment — a row that costs itself is a row that can disagree with
    the one beside it.
    """
    pass
