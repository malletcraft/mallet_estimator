// Estimate list: the kind of work is the first thing you need to know about an
// estimate — building an article, making a repair visit and fitting something
// bought in are three different businesses priced on three different bases.
// Reading it used to mean opening the doc, so the row indicator carries both
// the approval state and the kind, and the kind is a sidebar filter as well.
//
// Estimation mode is gone from here: there is only one intake now, so naming
// it on every row said nothing.
frappe.listview_settings["Estimate"] = {
  add_fields: ["work_type"],

  get_indicator(doc) {
    const kind = doc.work_type || __("New Work");
    // Colour stays the ERPNext docstatus convention (people scan for it);
    // the kind rides in the label so one glance answers both questions.
    if (doc.docstatus === 2) {
      return [__("Cancelled") + " · " + kind, "red", "docstatus,=,2"];
    }
    if (doc.docstatus === 1) {
      return [__("Approved") + " · " + kind, "green", "docstatus,=,1"];
    }
    return [__("Draft") + " · " + kind, "orange", "docstatus,=,0"];
  },
};
