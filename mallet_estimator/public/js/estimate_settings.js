// Estimate Settings: manufacturing-masters setup button + workstation cost calculator.
frappe.ui.form.on("Estimate Settings", {
  refresh(frm) {
    frm.add_custom_button(__("Create / refresh manufacturing masters"), () => {
      frappe.call({
        method: "mallet_estimator.install.setup",
        freeze: true,
        freeze_message: __("Creating Workstations, Operations, Routing…"),
      }).then((r) => {
        const m = (r && r.message) || {};
        const inv = m.inventory || {}, wh = m.warehouses || {};
        let body = __("Workstations created: {0}<br>Operations created: {1}<br>Routing created: {2}<br>Workspace present: {3}", [
          m.workstations || 0, m.operations || 0, m.routing || 0, m.workspace_exists ? "yes ✓" : "NO",
        ]);
        body += __("<br>Item Groups created: {0} · UOMs: {1} · Item fields: {2}<br>Warehouses created: {3}", [
          inv.item_groups || 0, inv.uoms || 0, inv.custom_fields || 0, wh.warehouses || 0,
        ]);
        const allErr = [].concat(m.errors || [], inv.errors || [], wh.errors || []);
        if (allErr.length) { m.errors = allErr; }
        if (m.errors && m.errors.length) {
          body += "<br><br><b>" + __("Errors") + ":</b><br>" + frappe.utils.escape_html(m.errors.join("\n")).replace(/\n/g, "<br>");
        }
        frappe.msgprint({ title: __("Manufacturing setup"), message: body, indicator: (m.errors && m.errors.length) ? "orange" : "green" });
        render_calculator(frm);
      });
    });

    frm.add_custom_button(__("Verify setup"), () => {
      frappe.call({ method: "mallet_estimator.install.verify_setup", freeze: true }).then((r) => {
        const d = (r && r.message) || { checks: [] };
        const rows = (d.checks || []).map((c) =>
          `<tr><td>${c.ok ? "✅" : "❌"}</td><td>${frappe.utils.escape_html(c.name)}</td>` +
          `<td class="text-muted">${frappe.utils.escape_html(c.detail || "")}</td></tr>`
        ).join("");
        frappe.msgprint({
          title: d.all_ok ? __("Setup verified ✓") : __("Setup incomplete"),
          indicator: d.all_ok ? "green" : "red",
          message: `<table class="table table-bordered" style="font-size:12.5px;margin:0"><tbody>${rows}</tbody></table>` +
            (d.all_ok ? "" : `<p class="text-muted" style="margin-top:8px">Run <b>Create / refresh manufacturing masters</b> to fix the ❌ rows.</p>`),
        });
      });
    });
    // A read-only account for an outside reader. Building it by hand is eleven
    // clicks across three doctypes, every one of them a chance to grant too
    // much — and it has to be repeated verbatim at every studio. One button.
    frm.add_custom_button(__("Create read-only API user"), () => {
      const d = new frappe.ui.Dialog({
        title: __("Read-only API user"),
        fields: [
          { fieldtype: "HTML", options:
            `<p>${__("Creates a user that can READ estimates, SKUs, items, prices, décors, projects and customers — and nothing else. It cannot write, and it cannot open Estimate Settings, so cost data stays on this site.")}</p>` },
          { fieldname: "email", fieldtype: "Data", options: "Email", reqd: 1,
            label: __("Email"), default: "mallet-readonly@example.invalid" },
          { fieldname: "regenerate", fieldtype: "Check",
            label: __("Regenerate keys if the user already exists"),
            description: __("Issues a new key and secret. The old pair stops working immediately — this is also how you revoke access.") },
        ],
        primary_action_label: __("Create"),
        primary_action(values) {
          d.hide();
          frappe.call({
            method: "mallet_estimator.integration.create_readonly_api_user",
            args: values, freeze: true,
          }).then((r) => {
            const m = (r && r.message) || {};
            if (!m.api_key) return;
            // Shown ONCE. Frappe keeps only an encrypted copy of the secret,
            // so a lost secret is re-keyed, never recovered.
            frappe.msgprint({
              title: __("Copy the secret now — it is shown once"),
              indicator: "orange",
              message:
                `<p>${__("User")}: <b>${frappe.utils.escape_html(m.user)}</b><br>` +
                `${__("Role")}: <b>${frappe.utils.escape_html(m.role)}</b> (${__("read only")})</p>` +
                `<pre style="white-space:pre-wrap;user-select:all">MCFT_API_KEY=${frappe.utils.escape_html(m.api_key)}\nMCFT_API_SECRET=${frappe.utils.escape_html(m.api_secret)}</pre>` +
                `<p class="text-muted">${__("Frappe stores only an encrypted copy of the secret. If you lose it, regenerate — it cannot be read back.")}</p>`,
            });
          });
        },
      });
      d.show();
    }, __("Integrations"));

    // Clearing the estimating work is destructive and irreversible, so it sits
    // under Danger Zone behind a typed phrase rather than a confirm dialog — a
    // button that needs one careless click eventually gets one.
    frm.add_custom_button(__("Start Over — delete all estimates"), () => {
      const d = new frappe.ui.Dialog({
        title: __("Start Over"),
        fields: [
          { fieldtype: "HTML", options:
            `<p>${__("This deletes every <b>Estimate</b>, every <b>Estimate SKU</b> and the client-article <b>Items</b> those SKUs created.")}</p>` +
            `<p class="text-muted">${__("Your masters are left alone: material Items and their prices, suppliers, décors, rooms, settings, workstations, operations and routings.")}</p>` +
            `<p><b>${__("This cannot be undone.")}</b></p>` },
          { fieldname: "confirm", fieldtype: "Data", reqd: 1,
            label: __("Type DELETE ESTIMATES to confirm") },
        ],
        primary_action_label: __("Delete them"),
        primary_action(values) {
          d.hide();
          frappe.call({
            method: "mallet_estimator.reset.start_over",
            args: { confirm: values.confirm }, freeze: true,
            freeze_message: __("Clearing estimates …"),
          });
        },
      });
      d.show();
      d.get_primary_btn().removeClass("btn-primary").addClass("btn-danger");
    }, __("Danger Zone"));
    render_calculator(frm);
  },
  carpenter_salary: (frm) => render_calculator(frm),
  helper_salary: (frm) => render_calculator(frm),
  designer_salary: (frm) => render_calculator(frm),
  bonus_months: (frm) => render_calculator(frm),
  paid_holidays_per_month: (frm) => render_calculator(frm),
  national_holidays_per_year: (frm) => render_calculator(frm),
  lunch_hours_per_day: (frm) => render_calculator(frm),
  monthly_rent: (frm) => render_calculator(frm),
  working_days_per_month: (frm) => render_calculator(frm),
  working_hours_per_day: (frm) => render_calculator(frm),
});

function money(v) { return format_currency(v || 0); }

function render_calculator(frm) {
  frappe.call({ method: "mallet_estimator.mallet_estimator.doctype.estimate_settings.estimate_settings.cost_calculator" })
    .then((r) => {
      const d = r && r.message;
      const wrap = frm.get_field("cost_calculator_html").$wrapper;
      if (!d) { wrap.empty(); return; }
      const sr = d.staff_rates || {};
      const pct = (a) => d.factory_area ? ((a / d.factory_area) * 100).toFixed(1) + "%" : "—";
      const rows = d.rows.map((w) => `
        <tr>
          <td>${frappe.utils.escape_html(w.name)}<br>
            <span class="text-muted" style="font-size:11px">${w.dims && w.dims[0] ? `${w.dims[0]}×${w.dims[1]} ft · ${w.area_sqft} sq ft · ${pct(w.area_sqft)}` : "no footprint"}</span></td>
          <td class="text-right">${money(w.rent_hr)}</td>
          <td class="text-right">${money(w.machine_hr || 0)}</td>
          <td class="text-right">${money(w.wages_hr != null ? w.wages_hr : w.labour_hr)}<br>
            <span class="text-muted" style="font-size:11px">${(w.crew || []).join(" + ")}</span></td>
          <td class="text-right">${money(w.elec_hr || 0)}</td>
          <td class="text-right">${money(w.consumable_hr || 0)}</td>
          <td class="text-right"><b>${money(w.net_hr != null ? w.net_hr : w.total_hr)}</b></td>
        </tr>`).join("");
      wrap.html(`
        <div style="font-size:12.5px">
          <p class="text-muted" style="margin-bottom:8px">
            These seed each ERPNext <b>Workstation</b>'s <b>Operating Components Cost</b> (Net Hour Rate) — what every process step is charged.
            <b>Rent</b> = pure space rent (${money(d.monthly_rent)}/mo over ${d.billable_area} billable sq ft) prorated by footprint over
            <b>${(d.working_hours_per_month || 0).toFixed(0)} productive hrs/mo</b>
            (${(d.working_days_per_month || 0).toFixed(1)} working days × ${(d.productive_hours_per_day || 0).toFixed(1)} hrs after lunch).
            <b>Depreciation</b> = machine capital straight-line, its own component.
            <b>Wages</b> = salary-derived per role: carpenter ${money(sr.carpenter)}/hr · helper ${money(sr.helper)}/hr · designer ${money(sr.designer)}/hr
            (salary × 13 ÷ 12 ÷ productive hrs — includes the Diwali bonus and paid holidays).
            <b>Electricity</b> and <b>Consumables</b> are separate per-workstation components.
            Once seeded, edit any cell on the Workstation and the estimator reads the live rate.
          </p>
          <table class="table table-bordered" style="margin:0">
            <thead><tr>
              <th>Workstation (footprint)</th>
              <th class="text-right">Rent ₹/hr</th>
              <th class="text-right">Depreciation ₹/hr</th>
              <th class="text-right">Wages ₹/hr</th>
              <th class="text-right">Electricity ₹/hr</th>
              <th class="text-right">Consumables ₹/hr</th>
              <th class="text-right">Net ₹/hr</th>
            </tr></thead>
            <tbody>${rows}</tbody>
          </table>
          <p class="text-muted" style="margin-top:6px">
            Factory ${d.factory_area} sq ft → workstations occupy ${d.billable_area} sq ft;
            <b>${d.free_area} sq ft free</b> (${pct(d.free_area)}) for you to consume.
            Rent rows recover ${money(d.rent_recovered_month)}/month = 100% of rent.
            Transport trips: tempo ${money((d.transport_rates || {}).tempo)} · ext-laminate ${money((d.transport_rates || {}).ext_lam)}
            · client-hw ${money((d.transport_rates || {}).client_hw)} · outward ${money((d.transport_rates || {}).outward)}.
          </p>
          ${live_workstation_table(d)}
          ${steps_table(d)}
          ${sku_rule_block(d)}
        </div>`);
    });
}

// --- links into the MRP module ---------------------------------------------
//
// Amit, 2026-08-25: "every workstation and operation should be a link which i
// when click takes me to its actual erp in mrp module." A page that shows you a
// number and then makes you go and find the record it came from is a page you
// read once.
function erp_link(doctype, name, label) {
  if (!name) return "—";
  const route = `/app/${doctype}/${encodeURIComponent(name)}`;
  return `<a href="${route}">${frappe.utils.escape_html(label || name)}</a>`;
}

// --- what ERP actually holds, component by component -----------------------
//
// Amit, 2026-08-25: "the page is supposed to display all cost components from
// live erp ... so that i don't need to go to every workstation / operation one
// by one. Idea is to see one page where i can see what every workstation cost
// me (along with its child cost component) and every operation takes how many
// minutes at a glance."
//
// The table above is arithmetic from the settings — what a rate OUGHT to be.
// This one is the Workstation records themselves, one column per operating
// component, so the whole factory's costing is legible without opening eight
// forms. Where the two disagree, THIS is the one the estimates use.
function live_workstation_table(d) {
  const live = d.live || {};
  const rows = live.workstations || [];
  if (!rows.length) {
    return `<h5 style="margin-top:18px">Live in ERP</h5>
      <p class="text-muted">${frappe.utils.escape_html(live.error || "No workstations found in ERP.")}</p>`;
  }
  const comps = live.components || [];
  const computed = {};
  (d.rows || []).forEach((w) => { computed[w.name] = (w.net_hr != null ? w.net_hr : w.total_hr) || 0; });

  const head = comps.map((c) => `<th class="text-right">${frappe.utils.escape_html(c)}<br>₹/hr</th>`).join("");
  const body = rows.map((w) => {
    const have = {};
    (w.components || []).forEach(([name, v]) => { have[name] = v; });
    // A component the workstation simply does not carry is left blank rather
    // than shown as zero. Zero is a value somebody chose; blank is a row that
    // was never created, and on a setup-checking page that difference is the
    // whole point.
    const cells = comps.map((c) => {
      const v = have[c];
      return `<td class="text-right">${v === undefined ? '<span class="text-muted">—</span>' : money(v)}</td>`;
    }).join("");
    const want = computed[w.name];
    const got = w.hour_rate || 0;
    const off = want != null && Math.abs(got - want) >= 1;
    let note = "";
    if (got === 0) {
      note = `<br><span class="text-muted" style="font-size:10px;color:#a94442">no costs keyed</span>`;
    } else if (!(w.components || []).length) {
      note = `<br><span class="text-muted" style="font-size:10px">computed — no cost rows on the master</span>`;
    } else if (off) {
      note = `<br><span class="text-muted" style="font-size:10px;color:#8a6d3b">differs from the settings figure</span>`;
    }
    return `<tr>
      <td>${erp_link("workstation", w.name)}</td>
      ${cells}
      <td class="text-right"><b>${money(got)}</b>${note}</td>
    </tr>`;
  }).join("");

  return `
    <h5 style="margin-top:22px">Live in ERP — what every workstation actually costs</h5>
    <p class="text-muted" style="margin-bottom:6px">
      Read straight off each ERPNext <b>Workstation</b>'s Operating Components Cost, which is what
      every estimate is priced from. A dash means that component has no row on the master at all —
      different from a row set to zero. Click a workstation to open it.
    </p>
    <div style="overflow-x:auto">
      <table class="table table-bordered" style="margin:0">
        <thead><tr><th>Workstation</th>${head}<th class="text-right">Net ₹/hr</th></tr></thead>
        <tbody>${body}</tbody>
      </table>
    </div>`;
}

// --- the seventeen steps, published so the maths is readable ---------------
//
// "Publish the 17 operations as well on that page so its easy for user to
// understand how this labor estimation works." An estimate is a number a
// client is asked to trust; the least it can do is show its working.
function steps_table(d) {
  const live = d.live || {};
  const ops = live.operations || [];
  if (!ops.length) {
    return `<p class="text-muted">${frappe.utils.escape_html(live.error || "No operations found in ERP.")}</p>`;
  }
  const zone_label = { factory: "Factory", logistics: "Logistics", "on-site": "On-site" };
  const hw = live.hardware || [];
  let body = "";
  ops.forEach((o) => {
    body += `<tr>
      <td>${o.seq}. ${erp_link("operation", o.name)}</td>
      <td>${erp_link("workstation", o.workstation)}</td>
      <td>${frappe.utils.escape_html(zone_label[o.zone] || o.zone || "")}</td>
      <td class="text-right">${o.min_per_unit}</td>
      <td>${frappe.utils.escape_html(o.qty_source || "")}</td>
      <td class="text-right">${money(o.hour_rate)}</td>
    </tr>`;
    // The hardware children sit under their parent, indented, because that is
    // where they are on the estimate screen and a person should not have to
    // hold two layouts in their head for one line.
    if (o.name === live.parent) {
      hw.forEach((h) => {
        body += `<tr style="background:#fbfbfc">
          <td style="padding-left:26px">${erp_link("operation", h.name)}</td>
          <td>${erp_link("workstation", h.workstation)}</td>
          <td></td>
          <td class="text-right">${h.min_per_unit}</td>
          <td class="text-muted">per fitting counted in the model</td>
          <td></td>
        </tr>`;
      });
    }
  });
  return `
    <h5 style="margin-top:18px">The labour steps, in the order the shop works</h5>
    <p class="text-muted" style="margin-bottom:6px">
      Every estimate is these steps and nothing else. <b>Std min/unit</b> comes off each
      ERPNext <b>Operation</b> master and <b>₹/hr</b> off its <b>Workstation</b> — change either
      there and every new estimate follows. <b>Qty from</b> is what the model counts to
      multiply those minutes by; where it says <i>manual</i>, a person decides.
      Existing Estimate SKUs keep the numbers they were built with until
      &ldquo;Reset times from Operations&rdquo; is pressed on them — a re-price is a decision, not a side effect.
      Every operation and workstation below opens its own record.
    </p>
    <table class="table table-bordered" style="margin:0">
      <thead><tr>
        <th>Step</th><th>Workstation</th><th>Zone</th>
        <th class="text-right">Std min/unit</th><th>Qty from</th><th class="text-right">₹/hr</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table>`;
}

// --- the SKU rule ----------------------------------------------------------
function sku_rule_block(d) {
  const rule = (d.live || {}).sku_rule;
  if (!rule) return "";
  const lines = (rule.lines || [])
    .map((l) => `<li style="margin-bottom:4px">${frappe.utils.escape_html(l)}</li>`)
    .join("");
  return `
    <h5 style="margin-top:18px">${frappe.utils.escape_html(rule.title)}</h5>
    <ul style="font-size:12.5px;padding-left:18px;margin-bottom:0">${lines}</ul>`;
}
