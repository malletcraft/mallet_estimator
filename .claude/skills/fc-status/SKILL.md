---
name: fc-status
description: Read Frappe Cloud's real deploy state for mcft-stg — dispatch the read-only fc-status workflow, interpret the bench probe and site agent jobs, and decide the next move for a stalled or uncertain deploy.
---

# /fc-status — what is the bench ACTUALLY doing

Read-only. Never triggers a deploy. Use when a deploy looks stalled, a
workflow verdict disagrees with the site, or the footer's hash needs
verification. (frappecloud.com is network-blocked from cloud sessions — this
workflow is the only window.)

## 1. Fast check first — the site itself

```
curl -sS "https://mcft-stg.frappe.cloud/api/method/mallet_estimator.api.version_info" \
  -H "Authorization: token $MCFT_API_KEY:$MCFT_API_SECRET"
```
`message.commit` is the short sha the site RUNS right now. If that already
answers the question, stop here.

## 2. Dispatch the probe

Trigger `fc-status.yml` (`workflow_dispatch`, ref `main`) in
`malletcraft/mallet_estimator`; wait for completion; read the job log
(GitHub MCP `get_job_logs`, `return_content: true`).

## 3. Read it

**Bench probe** (`press.api.bench.deploy_information`):
- `mallet_estimator.current_hash` — newest BUILT release (not what the site runs).
- `update_available: true` — the site has a pending move to a newer bench.
- `last_deploy.status: Success` — the BUILD succeeded; says nothing about the site.
- `deploy_in_progress` / `bench_creation_underway` — a build is still running; wait.

**Agent jobs** (`press.api.client.get_list` on `Agent Job` — the one endpoint
a team API key answers; `press.api.site.jobs` returns "API access not
permitted", expected, ignore it). Timestamps are IST. Job types:
- `Update Site Pull` — site moved benches without schema change.
- `Update Site Migrate` — site moved WITH migrate (patches ran). Its success
  time is the footer's "since" time.
- `Backup Site` — routine, not a deploy signal.

## 4. Decision table

| Observation | Meaning | Move |
|---|---|---|
| site hash == expected | landed | update footer since-time from the migrate job |
| `update_available` + a FAILED Update Site job | migration errored | the job's step output is the traceback; fix, new batch |
| `update_available`, all jobs Success, none newer than the build | FC never queued the site move (2026-08-12 pattern) | force ONCE (`deploy-staging.yml`, input `force: true`); if still unmoved → dashboard **Update Now**, stop forcing |
| `deploy_in_progress` true | build running | wait; poll version_info in background |
| 503 from the site | migrate window in progress | keep polling, it is landing |

## House rules

- Times to the user in IST, always.
- The workflow badge is never the verdict; the site's version_info is.
