# MCFT OpenCutList plugin — SketchUp to estimate

*Our fork of the OpenCutList extension
([malletcraft/mcft-opencutlist-sketchup-extension](https://github.com/malletcraft/mcft-opencutlist-sketchup-extension),
branch `mcft`). It self-updates: the autoupdater pulls the fork, so a
restart of SketchUp picks up the latest shipped version.*

## Setup, once per machine

The plugin holds its own API identity (**Mallet Plugin** — can create SKUs
and read projects, can never see a rate). The key is entered once in the
plugin's settings; if pushes start failing with 403, the key is the first
thing to check.

## Modelling conventions the import depends on

- Articles are Profile Builder assemblies with **tagged parts**:
  `carcass_vert/horz/back`, `cab_door`, `drawer_side/bottom/facia`,
  `bed_*`, `loft_*`, `hwd_*`.
- Material names follow the coding grammar
  (`SG_PLY_V{v}_{int}_{ext}_{th}mm`, `SG_LAM_…`, `EB_`, `HWD_`, `JH_`…);
  `V0/V1/V2` is visible sides, trailing `a/b/c` are décor slots resolved
  later in ERPNext.

## What the plugin pushes

- **Link the model**: pick the ERPNext project (and client) the `.skp`
  belongs to — shown on the OCL screen so it is never ambiguous.
- **SKU auto-create**: a component named by the convention becomes — or
  binds to — its Estimate SKU on the server.
- **Cut list / nest CSV push**: sends the export straight into the SKU, the
  same import the manual PDF-attach performs.
- **ISO view push**: any SKU component can send its isometric view to its
  estimate, so the estimate carries a picture of the thing being priced.

## Rule of thumb

If a number looks wrong in ERPNext, fix the **model or the mapping**, not
the ERPNext row — computed rows are recomputed on every save, and hand edits
do not survive.
