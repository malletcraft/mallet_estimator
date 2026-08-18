# Mallet Estimator — from model to quotation

*The ERPNext app. Everything here happens at `https://mcft-stg.frappe.cloud`
under the **Mallet Estimator** workspace.*

## The flow in one line

SketchUp model → OpenCutList export → **Estimate SKU** (costs itself on
save) → **Estimate** (bundles SKUs, freezes on submit) → Quotation → BOMs →
Work Orders.

## Estimate SKU — one article, priced

An Estimate SKU is one article: a wardrobe, a bed, a loft. Its code is
generated as `customer_room_article` (e.g. `YS_MB_WAR`) and an ERPNext Item
is created to match.

- **Import happens on save**: attach the OpenCutList Estimate PDF or Part
  List, save, and the material/labor/joinery tables fill themselves. Nest
  mode (CSV) imports the same way. Hand edits to computed rows do not stick
  — the save recomputes, deliberately.
- **Décor slots**: material codes carry abstract slots (`a`, `b`, `c`…).
  Fill the SKU's décor map to resolve them to real laminates; apply happens
  via the button, never silently.
- **Rates come from the price list** (`Estimation (Assumed)`), never from
  code and never from typing into the grid. An unpriced material shows a red
  "NOT quotable" warning instead of a guessed number.

## Estimate — bundle, compare, freeze

An Estimate collects SKUs (one SKU may appear in several estimates to
compare approaches), adds allowances and transport, and computes client
prices. **Submitting freezes the SKUs' rates**; changes after that go
through Cancel → Amend, never edits. From a submitted estimate the buttons
create the Quotation, build BOMs, and raise Work Orders per SKU.

Client-facing prints only ever contain client numbers — internal costs and
margins are assembled nowhere in a client document, by construction.

## Site photos inside ERPNext

- **Site Photos** (menu) — the folder browser: client → project → room →
  captures with face thumbnails.
- Open any capture (`MEST-PH-…`) — thumbnails left, big image right, faces
  first, pano last; ImageMeter annotations appear beside their face with a
  count badge.
- **This room over time** on a capture shows the same room across dates and
  stages — the site-progress record.

## Where to check the system itself

`Estimate Settings → verify setup` runs the health check — every master,
role, workspace link and integration reports ✅/❌ with a reason.
