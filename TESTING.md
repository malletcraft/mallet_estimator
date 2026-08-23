# Testing & configuration verification

The app is tested in **five layers**, cheapest/fastest first. Everything runs
automatically on every push via **`.github/workflows/ci.yml`**; you can run each
layer by hand too.

| Layer | What it proves | Needs a bench? | Where |
|---|---|---|---|
| 1. **Unit** | Cost engine + OpenCutList parsing math | No (pure Python) | `mallet_estimator/tests/test_estimator.py`, `test_opencutlist.py` |
| 2. **Config health-check** | Every master exists & is shaped right | Yes | `verify_setup()` + `tests/test_setup.py` |
| 3. **Integration** | Items/UOMs/warehouses + the Estimate SKU flow | Yes | `tests/test_inventory.py`, `doctype/estimate_sku/test_estimate_sku.py` |
| 4. **UI (E2E)** | The desk actually renders it | Yes + browser | `cypress/integration/estimate_sku_ui.js` |
| 5. **Cost arithmetic** | The money is actually right | Yes | `tests/test_cost_arithmetic.py` + `tests/synthetic_rates.py` |

---

## Layer 5 — Cost arithmetic, and why it needed a fixture

Every cost value in this repo is `0` on purpose: salaries, rent, MRPs and
markups live only in the site database, because this repo is public. That rule
is not negotiable and nothing here weakens it.

It did, however, leave the cost engine untestable end to end. On a CI bench
every rate is zero, so "24 minifix housings at 15 minutes costs X" cannot be
asserted — the answer is always 0, and the assertion passes for a reason that
has nothing to do with the maths. **A test written that way is worse than no
test, because it looks like coverage.** On 2026-08-23 one went red for exactly
this: it asserted `labour_total` rose when a quantity was typed in, on a bench
that was behaving perfectly.

`tests/synthetic_rates.py` installs rates that are unmistakably fake, into a
test site only, chosen so a person can check the arithmetic in their head:

    25 working days x 8 productive hours   = 200 productive hours/month
    carpenter 200,000/month                = 1000/hr
    helper    100,000/month                =  500/hr   -> crew = 1500/hr
    designer  400,000/month                = 2000/hr
    monthly rent 0

Those salaries are an order of magnitude above any real wage, on purpose: a
*plausible* number in a public repo is one somebody can mistake for the real
one. These cannot be.

Rent is zero deliberately: it is recovered per square foot over 1,042 sqft of
billable floor, which divides into nothing clean and would turn every expected
value into a decimal nobody can verify by eye. Rent's own arithmetic is
asserted by *composition* instead — a station's rate must equal the sum of its
components, whatever they happen to be.

**Two rules if you extend this:**

1. `install()` **refuses unless `frappe.flags.in_test`**. Estimate Settings on
   a real site holds the only copy of the real cost data and has no undo.
2. `install()` returns the previous values and `restore()` puts them back;
   `clear_prices()` removes only the Item Prices the fixture created. The suite
   shares one site, so a fixture that does not clean up changes the world for
   every test that runs after it.

If a number in `synthetic_rates.py` ever looks plausible as a real Woodugift
rate, someone has broken the rule the file exists to respect.

---

## Layer 1 — Unit tests (run anywhere, no ERPNext)

Pure functions (workstation rates, `calc_sku`, OpenCutList CSV parse/aggregate).
No database, milliseconds to run:

```bash
python -m unittest mallet_estimator.tests.test_estimator mallet_estimator.tests.test_opencutlist -v
```

Add one for any new pure logic — keep it frappe-free so it runs in the fast CI job.

## Layer 2 — Config health-check (`verify_setup`)

`mallet_estimator.install.verify_setup()` asserts that every master the app needs
exists and is correct: Item Groups, UOMs, Item custom fields, Warehouses,
Workstations, Operations, Routing, print format, workspace, and how many materials
are still unpriced. It returns `{checks:[{name, ok, detail}], all_ok, failed}`.

Two ways to run it:
- **In the UI:** Estimate Settings → **Verify setup** → a ✅/❌ table popup. Run it
  after any deploy or "Create / refresh manufacturing masters".
- **In code / CI:** `bench --site <site> execute mallet_estimator.install.verify_setup`
  (and `tests/test_setup.py` asserts `all_ok` after creating the masters).

This is the fastest "is my ERPNext configured right?" check — the same contract is
verified by hand and by CI, so they can't drift.

## Layer 3 — Integration tests (Frappe + ERPNext)

`FrappeTestCase`/`IntegrationTestCase` cases that create real records in a throwaway
test DB and assert behaviour: material Items get the right group/UOM/conversions
(edge banding Meter + Roll = 50 m; plywood Sheet + m² conversion), classification
(`SG_LAM_*` → Laminate), idempotency (no duplicate Items), the Estimate SKU cost
compute, customer-supplied material = free, and the article Item landing in the
**Client SKU** group.

```bash
bench --site <site> run-tests --app mallet_estimator
```

## Layer 4 — UI end-to-end (Cypress)

Drives the real desk in a headless browser — confirms the **Verify setup** popup is
all-green and that the Estimate SKU **Material Lines** grid shows the ERPNext **Item**
link + **UOM** columns (not a plain-text box):

```bash
bench --site <site> run-ui-tests mallet_estimator --headless
```

(Layer 4 needs a running site + Cypress; it's wired for local/optional use. To add it
to CI, start the bench (`bench start`) in the workflow and call `run-ui-tests`.)

---

## CI (`.github/workflows/ci.yml`)

On every push to `main` / PR:
- **unit** job — runs Layer 1 in seconds on plain Python.
- **integration** job — boots MariaDB + Redis, `bench init` (Frappe v16), installs
  **ERPNext** + **mallet_estimator** on a fresh site, and runs Layers 2–3 via
  `bench run-tests --app mallet_estimator`. On failure it dumps the last error logs.

This is separate from **`deploy-staging.yml`** (which builds + updates the Frappe
Cloud site). Tests gate correctness; deploy ships it.

## Adding tests
- New pure helper → add a case to `test_estimator.py`/`test_opencutlist.py`.
- New master/field/warehouse → add it to `verify_setup()` **and** `test_setup.py`.
- New doctype behaviour → a `FrappeTestCase` next to the doctype.
- New screen/field the user must see → a Cypress assertion.
