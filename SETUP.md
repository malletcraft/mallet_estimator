# Setting up a new studio

Everything the app can create for itself, it creates for itself. This file is
only the residue: the steps that need a human because they involve a payment,
a password, or a console this app has no access to.

Read it as a checklist. Anything not on it should be automatic — if you find
yourself doing something here that the app could have done, that is a bug in
the app, not a missing line in this document.

---

## 1. The site

Frappe Cloud → new site → install `erpnext`, `payments`, `hrms`, then
`mallet_estimator`. Mumbai region for an Indian studio; the latency matters
more than it sounds when the shop floor is loading photos.

`after_install` runs on its own and creates: item groups, UOMs, warehouses,
workstations, operations, the `Mallet Standard Build` routing, the four print
formats, the workspace, the `Estimation (Assumed)` price list, GST masters,
manufacturers, brands, suppliers and the 13 rooms.

**Nothing on that list is a manual step.** If one is missing, press
**Create / refresh manufacturing masters** on Estimate Settings — it is
idempotent, so pressing it twice costs nothing.

## 2. Verify it

Estimate Settings → **Verify setup**. A ✅/❌ table covering every master
above. All ✅ before anyone touches an estimate: an estimate built on a
half-configured site produces numbers that look fine and are not.

## 3. The numbers only the studio knows

These are the manual steps, and they are manual on purpose — **cost data never
lives in this repo**, so the app ships every one of them as 0 and the site is
the only place the real figure exists.

Estimate Settings:

| Field | What it is |
|---|---|
| Carpenter / helper / designer salary | monthly, before bonus |
| Bonus months, paid holidays, national holidays, lunch hours | the working calendar |
| Monthly rent | the whole premises; the app splits it by workstation footprint |
| Markup % — material, labor, overhead, design | the client-facing margins |
| **Bought-out goods margin %** | the thinner margin on supply-and-install work |
| Repair visit charge | the per-visit floor a repair job cannot price below |
| Machine capital + electricity + consumables per workstation | on each Workstation |

A zero here is not a default — it is an unset value that will quote at cost.

## 4. Supplier rates

Import a **Supplier Rate Sheet** per supplier. The assumed rate the estimator
prices at is the *maximum* MRP across suppliers, deliberately: a ceiling
under-promises and over-delivers, and the alternative quietly loses money when
the cheap supplier is out of stock.

## 5. Read-only API access (optional)

Needed only if something outside the site reads it — an assistant checking an
estimate, a dashboard, a script.

Estimate Settings → **Integrations → Create read-only API user**. One button.
It creates the `Mallet Read Only` role, pins every permission except `read` to
0 on eight doctypes, creates the user with that single role, and shows you a
key and secret **once**.

It can read the cost doctypes too — `Estimate Settings` and `Supplier Rate
Sheet` — on an explicit decision (Amit, 2026-08-09): a reader that cannot see
the rates can only say a number looks odd, never why it is wrong. What it can
never do is WRITE one. That is the whole guarantee, and `verify_setup`
asserts it rather than trusting it.

The consequence is worth stating plainly: salaries, rent, markups and MRPs can
appear in an assistant's session transcript. What still never happens is a
cost figure in this repository — it is public, and a committed number is
permanent and world-readable. Reading is reversible; a commit is not.

To revoke: press the button again with **Regenerate keys** ticked. The old
pair stops working immediately.

Use it as:

```
Authorization: token <api_key>:<api_secret>
```

## 6. Claude Code cloud sessions (only if an assistant works on this repo)

Two settings on the Anthropic side, neither of which the app can reach. Both
live at [claude.ai/code](https://claude.ai/code) → the cloud icon in the row
**above** the message box → hover the environment → settings gear.

1. **Network access → Custom**, and add the site plus the Frappe Cloud API:
   ```
   <site>.frappe.cloud
   cloud.frappe.io
   ```
   Tick **Also include default list of common package managers**, or pip and
   npm stop working mid-session.

2. **Environment variables** — the key from step 5:
   ```
   MCFT_API_KEY=...
   MCFT_API_SECRET=...
   ```
   Anthropic's docs are explicit that cloud environments have **no secrets
   store** and anyone using the environment can read these values. That is
   why the credential is a read-only one that takes one click to revoke:
   the scope is what makes it acceptable, not the storage.

Never paste the secret into a chat message — that puts it in a transcript
permanently, which is worse than the environment variable.

---

## What is deliberately NOT automated

- **The cost figures.** Shipping them would put a studio's salaries and
  margins in a git repository. They are typed once, on the site.
- **The read-only user.** Created on request, not at install: a site that
  nothing reads from should not carry an API account waiting to be used.
- **Start Over.** Estimate Settings → Danger Zone, behind a typed phrase. It
  deletes every estimate, SKU and client-article Item. A deploy must never do
  that on its own.
