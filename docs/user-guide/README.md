# MCFT Toolkit — User Guide

The Mallet Crafts toolkit is three pieces of software that share one server
and one set of masters, so a wardrobe designed in SketchUp, priced in
ERPNext, and photographed on site all speak about the same project, the same
rooms, the same SKUs.

| Tool | What it is | Guide |
|---|---|---|
| **Mallet Estimator** | The ERPNext app on `mcft-stg.frappe.cloud`: estimates, SKUs, BOMs, quotations, site-photo records | [estimator.md](estimator.md) |
| **MCFT OpenCutList plugin** | Our fork of the OpenCutList SketchUp extension: pushes cut lists, ISO views and SKUs from the model straight into estimates | [sketchup-plugin.md](sketchup-plugin.md) |
| **MCFT Site Photos** | The Android app (phone + Chromebook): captures 360s on site, splits them into faces for ImageMeter, syncs to ERPNext — works fully offline | [site-photos.md](site-photos.md) |

## Logins and roles, in one paragraph

Everyone signs into the same site, `https://mcft-stg.frappe.cloud`. What you
can see is decided by role, not by which tool you use: **System Manager**
(admin) sees everything; **Mallet Site Photographer** gets exactly the
camera — captures, files, the project and room lists — and none of the cost
screens. Rates, salaries and markups live only in the site's database and are
keyed only by a human; no tool, plugin, app or assistant can write them.

## A note on this guide

It lives beside the code on purpose: a change that alters behaviour is
expected to update the page that describes it, in the same reviewed batch.
If the guide and the software disagree, the software shipped without its
paperwork — say so, and it gets fixed as a bug.

*No prices, rates or client-confidential figures belong in these pages —
this repository is public.*
