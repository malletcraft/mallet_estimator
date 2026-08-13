---
name: ship
description: Ship the current batch to staging — CI-gate batch-next, fast-forward main, then hash-verify the deploy against the site itself (never trust a green badge). Use when the user says "ship" / "ship it".
---

# /ship — the MCFT staging ship, exactly as the site taught us

Ship ONLY on the user's explicit approval ("ship"). One batch = whatever is
committed on `batch-next` beyond `main`.

## 1. Gate on CI (batch-next)

CI auto-runs only on `main` and PRs, so dispatch it:

- Trigger `ci.yml` with `workflow_dispatch` on ref `batch-next`
  (GitHub MCP `actions_run_trigger`, or `gh workflow run ci.yml --ref batch-next`).
- Poll the run for THIS head sha until `completed`. `failure` → stop, report,
  fix on batch-next, re-dispatch. Never merge a red or unrun batch.

## 2. Merge and push

```
git checkout main && git merge --ff-only batch-next && git push -u origin main
```
Fast-forward only — batch-next is always cut from main, so a real merge
commit means something is wrong; stop and look. On push network errors retry
up to 4 times with backoff (2s/4s/8s/16s).

## 3. Verify against the SITE, not the workflow

The push triggers the scoped auto-deploy (`deploy-staging.yml`). Its badge is
NOT the truth — this project has seen false greens (c5277bb) and false reds
(the 2026-08-12 stalls). The only authority is the site's own version:

```
curl -sS "https://mcft-stg.frappe.cloud/api/method/mallet_estimator.api.version_info" \
  -H "Authorization: token $MCFT_API_KEY:$MCFT_API_SECRET"
```

Poll (30–40 s interval, ≥60 min budget — FC builds are slow) until
`message.commit` equals the shipped short sha. A 503 mid-poll is the site's
migrate window, not an outage. Run the poll in the background; do not block
the conversation on it.

## 4. If the deploy workflow fails or the hash never flips

Diagnose before re-forcing — run the `/fc-status` skill and read its
decision table. The known stall (2026-08-12, three times): build=Success,
bench carries the new hash, but NO site-move agent job was ever queued.
Playbook:

1. `fc-status` shows a FAILED `Update Site Migrate` → that job's step output
   is the traceback; fix the migration (usually a patch), new batch.
2. No failed job, site just never moved → force ONCE:
   dispatch `deploy-staging.yml` on `main` with input `force: true`.
3. Still not moved → stop forcing. The pending update needs the dashboard's
   **Update Now** (Site → Updates) — the API key cannot schedule a site
   move. Tell the user; the change also rides along with the next ship.

## 5. After it lands

- Functional spot-check anything the batch changed behavior on (an endpoint
  hit, a Playwright screenshot of a changed form — see the session's
  ui_probe pattern).
- Update the deployment-status footer: commit + since-time in IST. The
  since-time is the `Update Site Migrate` success time from fc-status
  (press timestamps are IST), or the first version_info confirmation.

## House rules that bind this skill

- Cost data (rates, salaries, markups, MRPs) never enters a commit. Ever.
- Report all times in IST.
- One batch at a time: do not push a second batch to main while a deploy is
  in flight — stacked deploy candidates are what caused the 2026-08-12
  stall pattern.
