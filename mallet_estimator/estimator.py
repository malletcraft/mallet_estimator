# ---------------------------------------------------------------------------
# Cost calculation engine for the SKU execution estimator.
#
# Cost buildup for one SKU (one article / Item):
#   Material  (line items)
#   + Labor   (carpenter + helper minutes per process step x rates)
#   + Machine (depreciation-based machine-hour rate x machine-op minutes)
#   + Rent    (rent-per-hour x in-factory hours the SKU occupies)
#   + Design  (design hours x design rate + flat)
#   = Internal execution cost
#   -> Client price = internal cost with per-category markup, where the client
#      view folds machine + rent overhead into the "design & execution" line.
#
# This mirrors the standalone React app's src/model.js so both stay in sync.
# ---------------------------------------------------------------------------

# 16 fixed process steps (1-16) plus one editable miscellaneous / extra step
# at line 17. `machine` links a step to a machine key in Estimate Settings.
# `in_factory` decides whether the step's hours attract factory rent.
import math
import re

# Amit, 2026-08-24: "Step 1 to 11 happens in factory. steps 12,13,14 is
# logistics. steps 15,16 happens at onsite."
#
# A ZONE is what the reader is told; in_factory is what the money is worked out
# from, and they are not the same question. Loading is logistics to a person
# reading the card and still happens at a factory workstation on factory time —
# so it carries zone "logistics" and in_factory 1, and neither field has to lie
# to keep the other honest.
ZONE_FACTORY = "factory"
ZONE_LOGISTICS = "logistics"
ZONE_ONSITE = "on-site"

STEP_TEMPLATE = [
    {"phase": "Sheet Lamination",     "machine": None,          "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Sheet Tape Removal",   "machine": None,          "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Sheet Cutting",        "machine": "panel_saw",   "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Edge Banding",         "machine": "edge_bander", "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Minifix Boring",       "machine": "drill_press", "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Drilling",             "machine": "drill_press", "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Grooving",             "machine": "panel_saw",   "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Assembly",             "machine": "assembly",    "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Install Hardware",     "machine": "assembly",    "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Disassembly",          "machine": "assembly",    "in_factory": 1, "zone": ZONE_FACTORY},
    {"phase": "Packing",              "machine": "packing",     "in_factory": 1, "zone": ZONE_FACTORY},
    # Amit, 2026-08-22: "12. Loading On-Site should be in factory as loading is
    # done at factory for packed articles." Packed articles go onto the vehicle
    # before it leaves the works, so both the minutes and the RATE are factory
    # ones. Unloading stays off-site — that half genuinely happens at the site.
    {"phase": "Loading",              "machine": "packing",     "in_factory": 1, "zone": ZONE_LOGISTICS},
    {"phase": "Transport",            "machine": None,          "in_factory": 0, "zone": ZONE_LOGISTICS},
    {"phase": "Unloading",            "machine": None,          "in_factory": 0, "zone": ZONE_LOGISTICS},
    {"phase": "Assembly (on-site)",   "machine": None,          "in_factory": 0, "zone": ZONE_ONSITE},
    {"phase": "Installation",         "machine": None,          "in_factory": 0, "zone": ZONE_ONSITE},
    {"phase": "Miscellaneous - extra", "machine": None,         "in_factory": 1, "zone": ZONE_FACTORY, "is_misc": 1},
]

# --- ERPNext Manufacturing masters -----------------------------------------
# Workstations with their factory footprint (sq ft) and machine capital for
# depreciation. Space-based rent is recovered in full across the billable
# footprints (inventory rack + aisles absorbed) over the working hours per
# month. Every operation is worked by a 2-person crew (1 carpenter + 1 helper),
# so labour per hour = carpenter_rate + helper_rate from Estimate Settings.
# On-Site has no footprint (off-site work, no rent).
#
# Each workstation's hourly cost is broken into the SAME native ERPNext
# "Operating Components Cost" the Workstation master uses: Rent + Wages +
# Machinery (depreciation) + Electricity + Consumables. `elec_hr`/`consumable_hr`
# are per-workstation defaults (₹/hr) you can override directly on the Workstation.
# COST DATA IS SENSITIVE (###): capital / electricity / consumables seed values
# are NOT stored in this repo — key them on each ERPNext Workstation (the live
# site already carries them). Zeros here mean "no code-side seed".
# `dims` = (length, width) ft — shown in the settings footprint table. `crew` =
# which staff work the station (their salary-derived rates become the Wage
# components); default is the 2-person carpenter+helper crew.
WORKSTATIONS = [
    {"name": "Panel Saw",        "dims": (26, 15), "area_sqft": 26 * 15, "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    {"name": "Edge Bander",      "dims": (16, 4),  "area_sqft": 16 * 4,  "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    {"name": "Drill Press",      "dims": (16, 3),  "area_sqft": 16 * 3,  "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    {"name": "Pasting Station",  "dims": (12, 8),  "area_sqft": 12 * 8,  "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    {"name": "Assembly Station", "dims": (14, 15), "area_sqft": 14 * 15, "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    {"name": "Project Room",     "dims": (14, 15), "area_sqft": 14 * 15, "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
    # D1 — the designer's desk: 6x4 ft, designer crew (capital ###, keyed on site).
    {"name": "Design Desk",      "dims": (6, 4),   "area_sqft": 6 * 4,   "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0, "crew": ("designer",)},
    {"name": "On-Site",          "dims": (0, 0),   "area_sqft": 0,       "capital": 0, "life_years": 10, "elec_hr": 0, "consumable_hr": 0},
]

# Canonical order of the operating-cost components on every Workstation (OPS3 —
# modular): Rent = pure space rent; Depreciation = machine capital straight-line
# (its own component, no longer folded into Consumables); one Wage component per
# crew member (salary-derived, L1). Zero-value components are skipped per station.
WS_COMPONENTS = ["Rent", "Depreciation", "Carpenter Wage", "Helper Wage", "Designer Wage",
                 "Electricity", "Consumables"]
DEFAULT_CREW = ("carpenter", "helper")
WAGE_COMPONENT = {"carpenter": "Carpenter Wage", "helper": "Helper Wage", "designer": "Designer Wage"}

# Which workstation each of the 17 operations runs on (matches STEP_TEMPLATE).
OPERATION_WORKSTATION = {
    "Sheet Lamination": "Pasting Station",
    "Sheet Tape Removal": "Pasting Station",
    "Sheet Cutting": "Panel Saw",
    "Edge Banding": "Edge Bander",
    "Minifix Boring": "Drill Press",
    "Drilling": "Drill Press",
    "Grooving": "Panel Saw",
    "Assembly": "Assembly Station",
    "Install Hardware": "Assembly Station",
    # STEPS 10, 11 AND 12 RUN IN THE PROJECT ROOM. Amit, 2026-08-24, settling
    # it against the live ERP setup: "keep it as per current erp setup. record
    # it as rule as well."
    #
    # This supersedes his earlier message the same day — "step 11 and 12
    # happens at assembly station and not pasting station" — which was
    # correcting the Pasting Station, not choosing against the Project Room.
    # The bench had it right and the code did not.
    #
    # THE RULE, and it is the reason this matters: the Project Room is 14x15
    # ft of a floor that is rented whole, so it is in the rent spread whether
    # or not anything is billed to it. A workstation with no operation is
    # therefore not free — its share of the rent is computed and then charged
    # to nothing, and it disappears. Every costed workstation must carry work,
    # or its cost has to be deliberately allocated somewhere that does.
    "Disassembly": "Project Room",
    "Packing": "Project Room",
    "Loading": "Project Room",
    "Transport": "On-Site",
    "Unloading": "On-Site",
    "Assembly (on-site)": "On-Site",
    "Installation": "On-Site",
    "Miscellaneous - extra": "Assembly Station",
}

# D1 — the design pipeline as first-class labor: 7 steps, all worked by the
# designer at the Design Desk (site measurement happens on-site but is still the
# designer's time; in_factory=0 marks it off-site). Std minutes live on the
# Operation master (mallet_min_per_unit) like every other operation — these are
# the seed defaults, tune them on the Operation.
DESIGN_STEP_TEMPLATE = [
    {"phase": "Site Measurement (ImageMeter + Laser)", "in_factory": 0},
    {"phase": "Live 3D Floor Plan",                    "in_factory": 1},
    {"phase": "Export 3D Plan to SKP",                 "in_factory": 1},
    {"phase": "SKU in SketchUp (OCL)",                 "in_factory": 1},
    {"phase": "7 Views PDF (Layout)",                  "in_factory": 1},
    {"phase": "Estimate PDF (OCL)",                    "in_factory": 1},
    {"phase": "Part List PDF (OCL)",                   "in_factory": 1},
]
DESIGN_STANDARDS = {
    "Site Measurement (ImageMeter + Laser)": {"qty_source": "manual", "min_per_unit": 120},
    "Live 3D Floor Plan":                    {"qty_source": "manual", "min_per_unit": 240},
    "Export 3D Plan to SKP":                 {"qty_source": "manual", "min_per_unit": 30},
    "SKU in SketchUp (OCL)":                 {"qty_source": "manual", "min_per_unit": 180},
    "7 Views PDF (Layout)":                  {"qty_source": "manual", "min_per_unit": 60},
    "Estimate PDF (OCL)":                    {"qty_source": "manual", "min_per_unit": 15},
    "Part List PDF (OCL)":                   {"qty_source": "manual", "min_per_unit": 15},
}
OPERATION_WORKSTATION.update({t["phase"]: "Design Desk" for t in DESIGN_STEP_TEMPLATE})
# The hardware children are added further down, once their names exist.

# Phase -> zone, built from the templates rather than restated, so a step that
# moves cannot end up filed under one zone here and another there.
OPERATION_ZONE = {t["phase"]: t.get("zone", ZONE_FACTORY)
                  for t in STEP_TEMPLATE + DESIGN_STEP_TEMPLATE}
OPERATION_STANDARDS_DESIGN = DESIGN_STANDARDS  # alias for controllers

# C1 — inward material trips (₹/trip, defaults; live values come from settings):
#   tempo  = ply + internal laminate + joinery hardware (one big tempo)
#   ext    = external laminate (separate careful trip)
#   client = client hardware (hinges/rails/handles/lifts)
#   outward = finished goods to site
# Trip rates are SENSITIVE (###) — live values are keyed in Estimate Settings.
TRANSPORT_DEFAULTS = {"tempo": 0, "ext_lam": 0, "client_hw": 0,
                      "outward": 0, "handles": 0, "edge_band": 0}

# THE SIX LOGISTICS TRIPS one planned execution needs, in the order they
# happen: (key, label, default trips).
#
# Amit, 2026-08-30, naming all six: "Below trips are required in a estimate of
# skus within one planned execution for each work order." Handles used to ride
# inside the client-hardware trip and edge banding had no trip at all; they are
# separate runs, and edge banding is three of them because a roll comes from a
# different market than the boards do.
#
# NO RATES LIVE HERE and none ever will. Trip costs are on the sensitive list
# and this repo is public: the figures Amit gave are keyed into Estimate
# Settings on the site, and TRANSPORT_DEFAULTS stays zeros. A zero rate reads
# as "unset" on screen rather than as free.
#
# The QUANTITY is not sensitive — it is a fact about how the shop runs — so
# the defaults are here where a reader can see why a number is what it is.
TRIPS = (
    ("tempo", "Sheet Goods (ply / Fevicol / internal laminate / Abrotape)", 1),
    ("handles", "Handles", 1),
    # Amit gave no quantity for this one. One trip, like its neighbours; it is
    # the assumption most easily corrected, since the row is editable.
    ("client_hw", "Hardware", 1),
    ("ext_lam", "External Laminate", 1),
    ("edge_band", "Edge Banding", 3),
    ("outward", "Finished Goods Delivery", 1),
)

TRIP_KEYS = tuple(k for k, _l, _q in TRIPS)
TRIP_LABELS = {k: l for k, l, _q in TRIPS}
TRIP_DEFAULT_QTY = {k: q for k, _l, q in TRIPS}


def transport_rates(settings):
    """{trip key: ₹ per trip} from Estimate Settings, zero where unset."""
    out = {}
    for key in TRIP_KEYS:
        out[key] = (_get(settings, "trip_rate_" + key, TRANSPORT_DEFAULTS[key])
                    or TRANSPORT_DEFAULTS[key])
    return out


def logistics_lines(settings, qty_by_trip=None, rate_by_trip=None):
    """The Logistics block: one row per trip, both columns overridable.

    Amit, 2026-08-30: "make quantity and rate both editable under head
    logistics." A typed value wins over the default and the row says so, the
    same way the labour table distinguishes a standard from a hand-set time —
    otherwise nobody can tell which numbers were decided and which were
    inherited.

    rate 0 means the trip rate has not been keyed into Estimate Settings yet.
    It is reported as unset, never as a free trip.
    """
    rates = transport_rates(settings)
    qty_by_trip = qty_by_trip or {}
    rate_by_trip = rate_by_trip or {}
    rows = []
    for key, label, default_qty in TRIPS:
        qty, qty_src = default_qty, "standard"
        if str(qty_by_trip.get(key, "")).strip() != "":
            qty, qty_src = _num(qty_by_trip[key]), "edited here"
        rate, rate_src = _num(rates.get(key)), "erp:Estimate Settings"
        if str(rate_by_trip.get(key, "")).strip() != "":
            rate, rate_src = _num(rate_by_trip[key]), "edited here"
        elif not rate:
            rate_src = "unset"
        rows.append({
            "trip": key, "name": label,
            "qty": qty, "qty_source": qty_src,
            "rate": rate, "rate_source": rate_src,
            "amount": round(qty * rate, 2),
            "quotable": bool(rate),
        })
    return rows


def logistics_total(rows):
    """What the trips come to. Unset rates contribute nothing and are counted
    separately, so a total is never quietly short."""
    total = sum(r["amount"] for r in rows if r["quotable"])
    unset = [r["name"] for r in rows if not r["quotable"]]
    return round(total, 2), unset

# What each process step is supposed to take care of — seeded into the step's
# Remarks (editable per SKU; Assembly/Packing are meant to be refined by the user).
STEP_REMARKS = {
    "Sheet Lamination": "Apply glue on BOTH sides of the ply and paste laminate according to the material code.",
    "Sheet Tape Removal": "Remove the holding tape from the laminated sheets.",
    "Sheet Cutting": "Cut sheets per the cutting diagram (CD).",
    "Edge Banding": "Band exposed edges per part list (internal/external tape per code).",
    "Minifix Boring": "Bore minifix housings (qty = minifix count).",
    "Drilling": "Pilot holes + screws — qty is the SCREW count.",
    "Assembly": "Specify what gets assembled here: typically carcass and/or drawer boxes.",
    "Install Hardware": "Parent line — splits into one child per hardware TYPE (qty = their total).",
    "Disassembly": "Dismantle what must travel flat.",
    "Packing": "Dismantle where required before packing — a wardrobe carcass is dismantled, drawer boxes are not.",
    "Loading": "Load packed parts + hardware boxes.",
    "Transport": "Factory → site trip.",
    "Unloading": "Unload and stage at site.",
    "Assembly (on-site)": "Re-assemble carcass, hang doors/drawers.",
    "Installation": "Fix to wall/level, final alignment and handover.",
}

ROUTING_NAME = "Mallet Standard Build"


def workstation_rates(settings):
    """Compute per-workstation hourly rates broken into the native ERPNext
    operating components: Rent + Wages + Machinery + Electricity + Consumables.

    Rent recovers the FULL monthly rent across billable footprints over the
    working hours/month. Wages is the 2-person crew (carpenter + helper).
    Machinery is straight-line depreciation. Returns each WORKSTATIONS entry plus:
      - rent_hr, wages_hr, machine_hr, elec_hr, consumable_hr
      - components: [(label, ₹/hr), ...] in WS_COMPONENTS order
      - net_hr: the Net Hour Rate (sum of components)
      - legacy aliases labour_hr/dep_hr/total_hr so older callers still work.
    """
    whm = working_hours_per_month(settings)
    monthly_rent = _num(settings.monthly_rent)
    roles = staff_rates(settings)
    billable_area = sum(w["area_sqft"] for w in WORKSTATIONS if w["area_sqft"] > 0)
    rent_per_sqft = (monthly_rent / billable_area) if billable_area else 0.0
    out = []
    for w in WORKSTATIONS:
        rent_hr = (w["area_sqft"] * rent_per_sqft / whm) if whm else 0.0
        # Depreciation is its OWN component now (OPS3) — Rent stays pure space rent
        # and Consumables stays true consumables.
        dep_hr = (w["capital"] / (w["life_years"] * whm * 12)) if (w["life_years"] and whm) else 0.0
        elec_hr = _num(w.get("elec_hr"))
        consumable_hr = _num(w.get("consumable_hr"))
        crew = w.get("crew", DEFAULT_CREW)
        wage_vals = {WAGE_COMPONENT[role]: roles.get(role, 0.0) for role in crew}
        wages_hr = sum(wage_vals.values())
        comp_vals = {
            "Rent": rent_hr, "Depreciation": dep_hr,
            "Electricity": elec_hr, "Consumables": consumable_hr, **wage_vals,
        }
        # Canonical order; a component a station doesn't have (zero) is skipped.
        components = [(c, comp_vals[c]) for c in WS_COMPONENTS if comp_vals.get(c)]
        net_hr = sum(comp_vals.get(c, 0.0) for c in WS_COMPONENTS)
        out.append({
            **w,
            "rent_hr": rent_hr, "wages_hr": wages_hr, "machine_hr": dep_hr,
            "elec_hr": elec_hr, "consumable_hr": consumable_hr,
            "components": components, "net_hr": net_hr, "crew": crew,
            # legacy aliases
            "labour_hr": wages_hr, "dep_hr": dep_hr, "total_hr": net_hr,
        })
    return out


def live_workstation_rates(settings):
    """Return {workstation_name: rate_dict} using the ERPNext Workstation master
    as the source of truth: each row of the native `workstation_costs` child table
    (Rent/Wages/Machinery/Electricity/Consumables) plus the computed `hour_rate`
    (Net Hour Rate). Falls back to the computed rate for any workstation that has
    no operating-cost rows yet, so nothing prices at zero. Import-safe: only used
    inside the ERPNext controller.
    """
    import frappe  # local import keeps this module unit-testable without frappe

    computed = {w["name"]: w for w in workstation_rates(settings)}
    rates = {}
    for name in frappe.get_all("Workstation", pluck="name"):
        # Read fresh (not cached) so an edit to the workstation's operating costs
        # is picked up immediately.
        doc = frappe.get_doc("Workstation", name)
        rows = getattr(doc, "workstation_costs", None) or []
        if not rows:
            if name in computed:
                rates[name] = computed[name]
            continue
        comp = {r.operating_component: _num(r.operating_cost) for r in rows}
        rent_hr = comp.get("Rent", 0)
        # Wages = every per-role Wage component (Carpenter/Helper/Designer) plus
        # the legacy folded "Wages"; Depreciation plus the legacy "Machinery".
        wages_hr = comp.get("Wages", 0) + sum(v for c, v in comp.items() if c.endswith("Wage"))
        machine_hr = comp.get("Machinery", 0) + comp.get("Depreciation", 0)
        elec_hr = comp.get("Electricity", 0)
        consumable_hr = comp.get("Consumables", 0)
        # Net Hour Rate = the sum of ALL component rows (never trust a possibly
        # stale stored hour_rate). This includes any component we don't name
        # explicitly, so nothing is dropped.
        named = rent_hr + wages_hr + machine_hr + elec_hr + consumable_hr
        total_rows = sum(comp.values())
        # fold any extra/unrecognised components into consumables for the split
        consumable_hr += (total_rows - named)
        net_hr = total_rows
        # THE ROWS THEMSELVES, not just the buckets they fold into. Amit,
        # 2026-08-25: "the page is supposed to display all cost components from
        # live erp ... so that i don't need to go to every workstation /
        # operation one by one."
        #
        # Everything above collapses the child table into five named totals,
        # which is what the costing maths wants and is exactly the wrong shape
        # for a person checking whether a workstation is set up correctly. The
        # raw rows are carried through in the canonical order, with anything
        # unrecognised appended rather than dropped — a component nobody named
        # is precisely the one worth seeing.
        seen = set()
        components = []
        for c in WS_COMPONENTS:
            if c in comp:
                components.append([c, comp[c]])
                seen.add(c)
        for c, v in sorted(comp.items()):
            if c not in seen:
                components.append([c, v])
        rates[name] = {
            "rent_hr": rent_hr, "wages_hr": wages_hr, "machine_hr": machine_hr,
            "elec_hr": elec_hr, "consumable_hr": consumable_hr, "net_hr": net_hr,
            "labour_hr": wages_hr, "dep_hr": machine_hr, "total_hr": net_hr,
            "components": components,
            # Which of the two answers this is. A workstation with no cost rows
            # falls back to the computed figure above, and a reader has to be
            # able to tell those apart — one is what ERP charges, the other is
            # what it would charge if somebody keyed it.
            "rate_source": "erp:Workstation",
        }
    # include any computed workstation that ERPNext doesn't have yet
    for name, r in computed.items():
        rates.setdefault(name, dict(r, rate_source="computed:no cost rows"))
    return rates


def mm_to_ftin(mm):
    """1524 → 5′-0″ ; 598 → 1′-11½″. Millimetres stay the single source of
    truth (factory language); feet-inches are PRESENTATION ONLY, rounded to the
    nearest half inch (client language)."""
    mm = _num(mm)
    if not mm:
        return ""
    total_in = round(mm / 25.4 * 2) / 2.0
    ft = int(total_in // 12)
    rem = total_in - ft * 12
    whole = int(rem)
    frac = "½" if (rem - whole) >= 0.49 else ""
    inch = f"{whole}{frac}″"
    return f"{ft}′-{inch}" if ft else inch


def dims_ftin(w, d, h):
    """W x D x H in feet-inches, skipping missing dims."""
    parts = [mm_to_ftin(v) for v in (w, d, h)]
    return " x ".join(p for p in parts if p)


def _num(v):
    try:
        return float(v) if v not in (None, "") else 0.0
    except (TypeError, ValueError):
        return 0.0


def _get(settings, field, default=0):
    return _num(getattr(settings, field, default) or default)


def working_days_per_month(settings):
    """L1 — days the factory actually runs: working days (excl. weekly offs) minus
    the 2 paid holidays/month minus the 10 national holidays/yr prorated."""
    return max(
        _get(settings, "working_days_per_month", 26)
        - _get(settings, "paid_holidays_per_month")
        - _get(settings, "national_holidays_per_year") / 12.0,
        0.0,
    )


def productive_hours_per_day(settings):
    """L1 — the 8-hr shift includes a 1-hr paid lunch: productive hrs = shift − lunch."""
    return max(_get(settings, "working_hours_per_day", 8) - _get(settings, "lunch_hours_per_day"), 0.0)


def working_hours_per_month(settings):
    """Productive (billable) hours the factory runs per month — the base over
    which rent, depreciation and salaries are recovered."""
    return working_days_per_month(settings) * productive_hours_per_day(settings)


def staff_rates(settings):
    """L1 — effective ₹/hr per role, derived from MONTHLY SALARY + the holiday
    calendar + the Diwali bonus (annual cost = salary x (12 + bonus months),
    spread over the productive hours). Falls back to the legacy keyed hourly
    rates when no salary is set, so old data/tests still price."""
    whm = working_hours_per_month(settings)
    bonus = _get(settings, "bonus_months")

    def rate(salary_field, legacy_field):
        salary = _get(settings, salary_field)
        if salary and whm:
            return salary * (12 + bonus) / 12.0 / whm
        return _get(settings, legacy_field)

    return {
        "carpenter": rate("carpenter_salary", "carpenter_rate"),
        "helper": rate("helper_salary", "helper_rate"),
        "designer": rate("designer_salary", "design_rate"),
    }


def rent_per_hour(settings):
    h = working_hours_per_month(settings)
    return _num(settings.monthly_rent) / h if h > 0 else 0.0


# A name is written for a person to read, so people separate words however
# reads best — "Master Bedroom", "MB_WAR_CSV", "Wardrobe-Left". All three
# separators mean the same thing here, so splitting treats them alike rather
# than making the code grammar depend on which key someone happened to press.
_WORD_SPLIT = re.compile(r"[\s_\-]+")


def name_words(text):
    return [w for w in _WORD_SPLIT.split(str(text or "").strip()) if w]


def initials(text):
    return "".join(w[0] for w in name_words(text)).upper()


# Initials alone stop being a name and start being a puzzle — "Base Cabinet"
# as "BC" tells nobody anything. Every word keeps its first three letters and
# the words stay separated, so the abbreviation still reads as the name it
# came from: Base Cabinet → BAS_CAB, Wardrobe-Left → WAR_LEF.
ABBR_LETTERS_PER_WORD = 3


def abbr(text):
    """The article's short form: each word gives up to its first three
    letters, rejoined with underscores.

    A word with fewer than three letters gives what it has — nothing can
    invent a third character out of "TV".

    `MB_WAR_CSV` used to yield `MB_` — the whole name read as ONE word, so the
    first three characters included the separator and the article itself never
    reached the code."""
    return "_".join(w[:ABBR_LETTERS_PER_WORD].upper() for w in name_words(text))


# A room with several words has enough initials to be read at a glance —
# Master Bedroom is MB and nobody needs more. A one-word room does not: "K"
# for Kitchen is a letter, not a room, so it takes three letters like any
# other word.
def room_abbr(room):
    words = name_words(room)
    if len(words) == 1:
        return words[0][:ABBR_LETTERS_PER_WORD].upper()
    return "".join(w[0] for w in words).upper()


def customer_initials(customer_name):
    parts = str(customer_name or "").split()
    cf = parts[0][0] if parts else ""
    cl = parts[-1][0] if len(parts) > 1 else ""
    return (cf + cl).upper()


def article_token(article_name, article_code=None):
    """The article half of an SKU code.

    A code from the Mallet Article master wins outright over one derived from
    the article's prose name, and that is the whole reason the master exists.
    abbr() reads 'PVC bathroom door' as three words and answers
    PVC_BAT_DOO — correct by its own rule, and useless as a token. The master
    says PVC. Anything with no master row keeps the derived form, so an
    article typed in a hurry still produces a code."""
    code = (article_code or "").strip().upper()
    return code or abbr(article_name)


def sku_code(customer_name, room, article_name, article_code=None):
    return "_".join(x for x in [customer_initials(customer_name), room_abbr(room),
                                article_token(article_name, article_code)] if x)


# Default operation standards: which material driver fills each operation's Qty
# and the crew minutes per unit. Editable per SKU on the labor table.
# Amit, 2026-08-27: "every install hardware operation is 15 minutes per unit."
# The per-type guesses these started as (hinges 4, rails 6, handles 3, shelf
# supports 1, locks 5, other 2) were mine, not measured, and they are replaced
# by his single figure. The SPLIT still earns its place — the counts differ
# per type and the minutes remain editable per type — but the starting number
# is now one he chose rather than six I invented.
HARDWARE_MIN_PER_UNIT = 15


OPERATION_STANDARDS = {
    "Sheet Lamination":       {"qty_source": "laminate_sheets", "min_per_unit": 15},
    "Sheet Tape Removal":     {"qty_source": "sheets",          "min_per_unit": 3},
    "Sheet Cutting":          {"qty_source": "sheets",          "min_per_unit": 20},
    "Edge Banding":           {"qty_source": "edge_parts",      "min_per_unit": 3},
    # Amit, 2026-08-23: "minifix boaring is 15 min per unit." The 1 here was a
    # placeholder nobody had measured; staging had been hand-tuned to 30, which
    # made this the biggest line on the labour card at double its real time.
    # Changing the seed only helps a FRESH install — existing sites are
    # corrected by patches/minifix_boring_std_time.py, because the seeder
    # deliberately refuses to overwrite a standard someone has already set.
    "Minifix Boring":         {"qty_source": "minifix",         "min_per_unit": 15},
    "Drilling":               {"qty_source": "hinges",          "min_per_unit": 3},
    "Grooving":               {"qty_source": "manual",          "min_per_unit": 5},
    "Assembly":               {"qty_source": "panels",          "min_per_unit": 4},
    # Amit, 2026-08-27, on the parent being left at 30 while its six children
    # went to 15: "Set it to 15 as well." Its own figure is unused whenever
    # children are present — the parent's time is their sum — so this is the
    # FALLBACK: what a hardware line costs when no child type matches, and
    # what a reader scanning the Operation list sees. One figure, not two.
    "Install Hardware":       {"qty_source": "hardware_total",
                               "min_per_unit": HARDWARE_MIN_PER_UNIT},
    "Disassembly":            {"qty_source": "manual",          "min_per_unit": 15},
    # Amit, 2026-08-24: "Packing default time is 30 minute per unit."
    "Packing":                {"qty_source": "sheets",          "min_per_unit": 30},
    "Loading":                {"qty_source": "manual",          "min_per_unit": 30},
    "Transport":              {"qty_source": "manual",          "min_per_unit": 30},
    "Unloading":              {"qty_source": "manual",          "min_per_unit": 30},
    "Assembly (on-site)":     {"qty_source": "manual",          "min_per_unit": 45},
    "Installation":           {"qty_source": "manual",          "min_per_unit": 60},
    "Miscellaneous - extra":  {"qty_source": "manual",          "min_per_unit": 0},
}


MISC_OPERATION = "Miscellaneous - extra"

# --- Install Hardware, split by what is being installed --------------------
#
# Amit, 2026-08-24: "rather than a single install hardware line, let this be a
# parent line and always divide the hardware by its type, like Install Hinges,
# Install shelf buttons, install drawer rails etc. that way quantity will not
# be editable but its time will be editable depending on type of hardware,
# hardware always starts with HWD."
#
# One line reading "Install Hardware x 96 at 2 min" is a number nobody can
# argue with, because it hides three kinds of work that take different times.
# A soft-close hinge is not a shelf pin. So the parent keeps the total and each
# TYPE gets its own row, its own standard time from its own Operation master,
# and its own editable minutes.
#
# WHAT IS NOT HERE, and why. Two buckets that classify_hardware() returns are
# deliberately absent, because both are already priced by a step of their own
# and listing them again would charge the same fitting twice:
#   minifix -> step 5 Minifix Boring, at the Drill Press. Amit settled this the
#             same day: boring stays step 5, no install child.
#   screws  -> step 6 Drilling, whose quantity IS the screw count.
# The key is the bucket classify_hardware() returns, so the two stay in step by
# construction rather than by a second list somebody has to keep equal.
HARDWARE_INSTALL_TYPES = (
    ("hinges",         "Hinges"),
    ("rails",          "Drawer Rails"),
    ("handles",        "Handles"),
    ("shelf_supports", "Shelf Supports"),
    ("locks",          "Locks & Tower Bolts"),
    # The catch-all earns its place: a fitting nobody has taught the classifier
    # about must still be installed by somebody, and a bucket that silently
    # dropped it would quietly under-price every model containing one.
    ("other",          "Other Hardware"),
)
HARDWARE_PARENT = "Install Hardware"


def hardware_operation(kind):
    """The Operation master name for one hardware type — "Install Hinges"."""
    label = dict(HARDWARE_INSTALL_TYPES).get(kind)
    return ("Install %s" % label) if label else None


# Seed minutes per type. Every one is the old flat 2 min/unit except where a
# fitting is obviously slower — these are STARTING points, tuned on the
# Operation master like every other standard, never in code.
HARDWARE_STANDARDS = {
    "hinges": HARDWARE_MIN_PER_UNIT,
    "rails": HARDWARE_MIN_PER_UNIT,
    "handles": HARDWARE_MIN_PER_UNIT,
    "shelf_supports": HARDWARE_MIN_PER_UNIT,
    "locks": HARDWARE_MIN_PER_UNIT,
    "other": HARDWARE_MIN_PER_UNIT,
}

# Every hardware child runs where its parent runs. DERIVED from the parent
# rather than restated, so the day Install Hardware moves station its children
# move with it — the alternative is six more places to forget, and the
# workstation is a price.
OPERATION_WORKSTATION.update({
    hardware_operation(k): OPERATION_WORKSTATION[HARDWARE_PARENT]
    for k, _ in HARDWARE_INSTALL_TYPES
})
# Same for the zone: a child cannot belong to a different part of the day than
# the step it is part of.
OPERATION_ZONE.update({
    hardware_operation(k): OPERATION_ZONE[HARDWARE_PARENT]
    for k, _ in HARDWARE_INSTALL_TYPES
})


def op_phase(row):
    """Canonical Operation name for a labor row (the misc/custom row is generic).
    Prefers the native `operation` link; falls back to the legacy `phase` text."""
    if getattr(row, "is_misc", 0):
        return MISC_OPERATION
    return getattr(row, "operation", None) or getattr(row, "phase", None)


# ---------------------------------------------------------------------------
# J1 — the joinery consumables, in ONE place.
#
# Fevicol and Abrotape are not in any cut list: they are DERIVED from how many
# boards go through the press. That derivation used to live only inside
# EstimateSKU.derive_joinery, which meant the SketchUp plugin's on-the-fly
# estimate simply left them out — the same model priced two ways, and the
# cheaper way was the one shown to a client. Amit, 2026-08-29: "Need fevicol
# and abrotape logic in mcft plugin as well."
#
# Copying the arithmetic into the preview would have been the fast answer and
# the wrong one. The plugin and the real estimate disagreeing is the specific
# failure this project keeps paying for — the workstation mismatch, the
# assembly count, the hardware children — so the rule moves HERE, pure and
# testable, and both callers ask it rather than each knowing it.

FEVICOL_PACKETS_PER_BOARD = 3
ABROTAPE_METERS_PER_BOARD = 11

# Two faces to a board, so a laminate SHEET is half a board's worth of glue.
LAM_SHEETS_PER_BOARD = 2.0


def joinery_boards(ply_qty=0, lam_qty=0, lamination_step_qty=0):
    """How many pressed boards the consumables follow.

    Ply first, because the ply sheet IS the board. Laminate is the fallback
    and is halved: it briefly WAS the primary count, which silently doubled
    the figure the day purchasing split each board's two faces onto their own
    laminate line (Amit, 2026-08-13 — 9 boards x 3 = 27 packets, not 18 x 3 =
    54).

    The typed Sheet Lamination quantity is the LAST resort and only for a
    hand-built SKU with no part list at all, because a number typed into a
    labour row once conjured material nothing was buying: 21 packets and 77 m,
    a third of that SKU's internal cost, out of a 7 typed into a row.
    """
    if ply_qty:
        return float(ply_qty)
    if lam_qty:
        return float(lam_qty) / LAM_SHEETS_PER_BOARD
    return float(lamination_step_qty or 0)


def joinery_lines(boards):
    """[(item_code, qty, uom, note)] for J1. Empty when nothing is pressed.

    Abrotape is quantified in METRES and priced per metre; it is BOUGHT in
    20 m rolls, which is a purchasing fact the Item carries, not a factor to
    multiply here. Getting that backwards is how the edge-banding line was
    once out by 50x.
    """
    boards = float(boards or 0)
    if boards <= 0:
        return []
    return [
        ("JH_Fevicol", FEVICOL_PACKETS_PER_BOARD * boards, "Nos",
         "%g packets x %g laminated board(s)"
         % (FEVICOL_PACKETS_PER_BOARD, boards)),
        ("JH_Abrotape", ABROTAPE_METERS_PER_BOARD * boards, "Meter",
         "%g m x %g laminated board(s) — 20 m rolls"
         % (ABROTAPE_METERS_PER_BOARD, boards)),
    ]


def calc_sku(sku, settings, ws_rates=None):
    """Compute all cost figures for one Estimate SKU (native workstation model).

    Each phase's crew minutes = qty x carp_min (carp_min = crew minutes per unit;
    the 2-person crew is priced inside the workstation hour-rate). Phase cost =
    crew-hours x that phase's workstation rate, split into labour / machine (dep)
    / rent for the breakdown, and written back to each row's op_cost.

    `ws_rates` is {workstation_name: {rent_hr, dep_hr, labour_hr, total_hr}} — the
    controller passes the live ERPNext Workstation master rates; if omitted we fall
    back to the computed rates so the function stays unit-testable.
    """
    if ws_rates is None:
        ws_rates = {w["name"]: w for w in workstation_rates(settings)}
    default_ws = "Assembly Station"
    # Margins live on the SELLABLE THING: a SKU with custom margins prices
    # itself; everything else inherits the house policy (Estimate Settings).
    custom = bool(_num(getattr(sku, "use_custom_margins", 0) or 0))
    if custom:
        markup = {
            "material": _num(sku.get("margin_material")),
            "labor": _num(sku.get("margin_labor")),
            "overhead": _num(sku.get("margin_overhead")),
            "design": _num(sku.get("margin_design")),
        }
    else:
        markup = {
            "material": _num(settings.markup_material),
            "labor": _num(settings.markup_labor),
            "overhead": _num(settings.markup_overhead),
            "design": _num(settings.markup_design),
        }
    markup_display = dict(markup, __custom__=custom)

    def cost_rows(rows, fallback_ws, skip_misc):
        """Price a table of step rows at their workstation rates. Returns totals
        split by component bucket; writes each row's op_cost back."""
        t = {"min": 0.0, "wages": 0.0, "dep": 0.0, "rent": 0.0, "other": 0.0}
        # A PARENT WITH CHILDREN IS NOT CHARGED ITSELF. Assembly splits into
        # three sizes and Install Hardware into its fitting types; the child
        # rows carry the work, and pricing the parent's own qty x minutes as
        # well would bill every one of them twice. The parent's displayed
        # totals become its children's sum below, so the row still reads
        # honestly against the numbers underneath it — the same rule the
        # estimate screen already follows.
        child_min, child_cost = {}, {}
        parents = {getattr(r, "parent_step", "") for r in (rows or [])
                   if getattr(r, "parent_step", "")}
        for s in rows or []:
            if getattr(s, "operation", None) in parents and not getattr(s, "parent_step", ""):
                continue
            if skip_misc and getattr(s, "is_misc", 0) and not sku.include_misc:
                s.carp_total = 0
                s.helper_total = 0
                s.op_cost = 0
                continue
            crew_min = _num(s.qty) * _num(s.carp_min)  # carp_min = workstation minutes/unit
            s.carp_total = crew_min
            s.helper_total = crew_min
            t["min"] += crew_min
            ws_name = getattr(s, "workstation", None) or OPERATION_WORKSTATION.get(op_phase(s), fallback_ws)
            r = ws_rates.get(ws_name) or ws_rates.get(fallback_ws) or {}
            hrs = crew_min / 60.0
            wages_hr = r.get("wages_hr", r.get("labour_hr", 0))
            machine_hr = r.get("machine_hr", r.get("dep_hr", 0))
            rent_hr = r.get("rent_hr", 0)
            elec_hr = r.get("elec_hr", 0)
            consumable_hr = r.get("consumable_hr", 0)
            # Net Hour Rate: prefer the live ERPNext Workstation total when supplied.
            net_hr = r.get("net_hr", wages_hr + machine_hr + rent_hr + elec_hr + consumable_hr)
            t["wages"] += hrs * wages_hr
            t["dep"] += hrs * machine_hr
            t["rent"] += hrs * rent_hr
            t["other"] += hrs * (elec_hr + consumable_hr)
            s.op_cost = hrs * net_hr
            owner = getattr(s, "parent_step", "")
            if owner:
                child_min[owner] = child_min.get(owner, 0.0) + crew_min
                child_cost[owner] = child_cost.get(owner, 0.0) + s.op_cost

        # The parents, second, now that their children are known.
        for s in rows or []:
            if getattr(s, "parent_step", ""):
                continue
            op = getattr(s, "operation", None)
            if op in parents:
                s.carp_total = child_min.get(op, 0.0)
                s.helper_total = s.carp_total
                s.op_cost = child_cost.get(op, 0.0)
        return t

    lab = cost_rows(sku.labor, default_ws, skip_misc=True)
    crew_min_total = lab["min"]
    labor_cost = lab["wages"]
    machine_cost = lab["dep"]
    rent_cost = lab["rent"]
    other_cost = lab["other"]

    carp_min_total = crew_min_total
    helper_min_total = crew_min_total
    carpenter_cost = labor_cost  # labour is the 2-person crew (folded into wages)
    helper_cost = 0.0
    # Client-supplied lines keep their full pricing on screen (the estimate has
    # to read as a whole picture) but are not money WE spend, so they never
    # enter cost. Filtering here is what keeps the two views honest.
    material_cost = sum(_num(m.line_cost) for m in (sku.materials or [])
                        if not getattr(m, "customer_supplied", 0))
    # J1 — fevicol/abrotape derived consumables: material, but its own bucket.
    joinery_cost = sum(_num(j.amount) for j in (getattr(sku, "joinery_items", None) or []))

    # D1 — design as real workstation labor (Design Desk rates, full component
    # split). Falls back to the legacy hours x rate + flat model when the design
    # table is empty.
    design_rows = getattr(sku, "design_labor", None) or []
    if design_rows:
        des = cost_rows(design_rows, "Design Desk", skip_misc=False)
        design_cost = des["wages"] + des["dep"] + des["rent"] + des["other"]
        design_min_total = des["min"]
        design_wages = des["wages"]
        design_overhead = design_cost - des["wages"]
    else:
        # _get, not settings.design_rate. There IS no design_rate field on
        # Estimate Settings — only designer_salary and markup_design — so the
        # direct attribute access raised AttributeError on any Frappe document
        # that reached this branch. It went unseen because the branch only runs
        # when a SKU has NO design rows, which every article SKU has; the first
        # SKU without them was a Subcontract one, and it took down Recompute
        # with a traceback rather than a message (Amit, 2026-08-26).
        #
        # _get returns the default for a field that does not exist, which is
        # the honest answer here: an unset design rate is zero design cost, and
        # zero is what every rate in this file ships as anyway.
        design_cost = (_num(sku.design_hours) * _get(settings, "design_rate")
                       + _num(sku.design_flat))
        design_min_total = 0.0
        design_wages = design_cost
        design_overhead = 0.0

    # C1 — inward/outward transport at the settings trip rates. Standalone view of
    # THIS SKU; consolidated (shared-trip) billing happens on the Estimate.
    trates = transport_rates(settings)
    transport_cost = (
        _num(getattr(sku, "trips_tempo", 0)) * trates["tempo"]
        + _num(getattr(sku, "trips_ext_lam", 0)) * trates["ext_lam"]
        + _num(getattr(sku, "trips_client_hw", 0)) * trates["client_hw"]
        + _num(getattr(sku, "trips_outward", 0)) * trates["outward"]
    )

    overhead_cost = machine_cost + rent_cost + other_cost
    rent_hours = crew_min_total / 60.0
    internal_cost = material_cost + joinery_cost + labor_cost + overhead_cost + design_cost + transport_cost

    # Joinery consumables are material to the client (same markup bucket).
    client_material = (material_cost + joinery_cost) * (1 + markup["material"] / 100.0)
    client_labor = labor_cost * (1 + markup["labor"] / 100.0)
    client_overhead = overhead_cost * (1 + markup["overhead"] / 100.0)
    client_design = design_cost * (1 + markup["design"] / 100.0)
    # Client view folds labour + overhead + design into one "design & execution" line.
    client_design_exec = client_labor + client_overhead + client_design
    # Transport is NOT in the SKU's client total — trips are shared across the
    # project, so the Estimate bills its consolidated trips (at cost, no markup).
    # It IS inside internal_cost so the SKU's margin view stays honest.
    client_total = client_material + client_design_exec

    return {
        "carp_min_total": carp_min_total,
        "helper_min_total": helper_min_total,
        "carpenter_cost": carpenter_cost,
        "helper_cost": helper_cost,
        "labor_cost": labor_cost,
        "machine_cost": machine_cost,
        "rent_cost": rent_cost,
        "rent_hours": rent_hours,
        "overhead_cost": overhead_cost,
        "material_cost": material_cost,
        "joinery_cost": joinery_cost,
        "design_cost": design_cost,
        "design_min_total": design_min_total,
        "design_wages": design_wages,
        "design_overhead": design_overhead,
        "transport_cost": transport_cost,
        "internal_cost": internal_cost,
        "client_material": client_material,
        "client_labor": client_labor,
        "client_overhead": client_overhead,
        "client_design": client_design,
        "client_design_exec": client_design_exec,
        "client_total": client_total,
        "markup_pct": markup_display,
    }


# ---------------------------------------------------------------------------
# Repair work (R1) — a different UNIT of estimation.
#
# New work prices an ARTICLE from its parts; repair prices an ACTIVITY on
# something that already exists in the client's home. There are no parts to
# nest, the material is small and often not stocked, and the labour IS the
# cost. So repair has its own engine rather than a special case bolted into
# calc_sku — the two never share a code path, only the wage rates.
#
# The three rules the shop actually works by:
#   * a row's minutes are crew x per-unit minutes x QTY (six chairs at 30 min
#     each is 180 min, not 30);
#   * a row that cannot be priced until someone looks at it ("To Inspect")
#     carries its provisional minutes but is kept OUT of the firm total —
#     these are the rows that eat the margin when they hide at qty 0;
#   * a visit has a floor price. Going out at all costs a day; the day rate
#     stops binding once a full day of carpenter AND helper has been worked.
# ---------------------------------------------------------------------------

# The three kinds of work. New Work builds an article from parts; the other two
# happen at the client's home and share one labour model — what separates them
# is where the MATERIAL comes from. Repair barely buys anything; Supply &
# Install buys a FINISHED article and fits it, which is a different margin
# question because the client can price-check a door and cannot price-check ply.
ASSEMBLY_SIZES = ("large", "medium", "small")

# ASMBL_L_WAR, ASMBL_M_DRW, ASMBL_S_SHELF — Amit, 2026-08-23: "we deal with
# three size of assembly, Large - carcass, medium drawers , small like
# shelfs ... so that i can do a better job of estimating the time."
#
# The size is the token straight after ASMBL. A name with no size token is
# read as LARGE, deliberately: every model drawn before this convention
# existed says plain ASMBL_WAR, and those are carcasses. Reading them as
# small would quietly shrink the estimate of every existing model.
_ASMBL_SIZE = re.compile(r"\AASMBL[_\-]?([LMS])(?:[_\-]|\Z)", re.I)
_SIZE_OF = {"L": "large", "M": "medium", "S": "small"}

# THE TOP-LEVEL MARKER. Amit, 2026-08-27, with the Outliner beside the labour
# table: "Large / Medium / Small assemblies are not captured or identified
# correctly. Qualifier is TOp level component MCFT_ASMBL_L_ MCFT_ASMBL_M_
# MCFT_ASMBL_S_".
#
# Two faults, one cause. The pattern above anchors on ASMBL at the START of
# the name, so `MCFT_ASMBL_M_BOOKCAB` did not match it at all — the actual
# assembly was invisible — while the nested parts inside it (ASMBL_DRW_Box,
# ASMBL_Door_Loft_Left, ASMBL_CARCASS_SHELF …) all matched, carried no size
# token, and were each counted as a LARGE assembly. A model with two medium
# assemblies was priced as ten large ones.
#
# The MCFT_ prefix is what distinguishes the SKU from its parts, which makes
# it both the size qualifier and the top-level marker, and the rule published
# on Estimate Settings has said MCFT_ASMBL_ since it was written. The parser
# is what disagreed with it.
_MCFT_ASMBL = re.compile(r"\AMCFT[_\-]?ASMBL[_\-]?(?:([LMS])(?:[_\-]|\Z))?", re.I)


def _asmbl_classify(name):
    """(is_top_level, size, sized) for an assembly name, or None if not one.

    A MCFT_-prefixed name is the SKU itself. A bare ASMBL name is a part
    inside one — or, on any model drawn before this convention, the assembly
    itself, which is why the caller keeps both and decides between them.

    `sized` says whether the NAME declared a size, which is not the same as
    the size being large. "Nobody said" is what tells you a model predates the
    convention; "somebody said large" is a choice. Returning it here beats
    re-deriving it at the call site by matching the same patterns twice.
    """
    n = (name or "").strip()
    m = _MCFT_ASMBL.match(n)
    if m:
        tok = (m.group(1) or "").upper()
        return True, _SIZE_OF.get(tok, "large"), bool(tok)
    m = _ASMBL_SIZE.match(n)
    if m:
        return False, _SIZE_OF[m.group(1).upper()], True
    if n.upper().startswith("ASMBL"):
        return False, "large", False
    return None


def _asmbl_counts(rows):
    """DISTINCT assemblies per size class.

    Distinct, not total: two copies of one assembly are two units of the same
    thing and both are counted, but the same component appearing on twenty
    part rows is still one assembly. Case-insensitive because SketchUp names
    are typed by people.
    """
    tops, bare = set(), set()
    for r in rows:
        for key in ("name", "designation", "part", "Name", "Designation"):
            v = str((r.get(key) if hasattr(r, "get") else "") or "").strip()
            if not v:
                continue
            got = _asmbl_classify(v)
            if got is None:
                continue
            (tops if got[0] else bare).add(v.upper())
            break

    # TOP-LEVEL WINS OUTRIGHT when any is present. The parts inside an
    # assembly are named ASMBL_* too, and counting them alongside their parent
    # is what turned two medium assemblies into ten large ones.
    #
    # The fallback is not laziness: every model drawn before this convention
    # says plain ASMBL_WAR at top level with no MCFT_ prefix, and requiring
    # the prefix outright would silently price those at zero assemblies.
    # Explicit marking wins where it exists; the old reading holds where it
    # does not.
    chosen, from_top = (tops, True) if tops else (bare, False)
    out = {k: 0 for k in ASSEMBLY_SIZES}
    unsized = 0
    for name in chosen:
        _top, size, sized = _asmbl_classify(name)
        out[size] += 1
        if not sized:
            unsized += 1
    out["unsized"] = unsized
    # Which reading was used, so the estimate can say so rather than leaving
    # the reader to wonder why a model with MCFT_ names counted differently
    # from one without.
    out["top_level"] = from_top
    return out


NEW_WORK = "New Work"
REPAIR = "Repair"
SUPPLY_INSTALL = "Supply & Install"
# WORK SOMEBODY ELSE DOES. Amit, 2026-08-25, deciding the question task #41
# had been blocked on: POP, tiling, electrical, plumbing and the rest become
# FULL SKUs priced from the vendor's rate — not lumpsum lines beside the
# estimate, and not quoted outside it.
#
# The reason is the project total. Price subcontract work anywhere else and
# the margin has to be assembled by hand out of two places, which is the
# failure this app exists to remove, and a third of every fit-out becomes
# invisible to every report. As an SKU it is tagged by site photos, carried
# by work stages, and counted in Project Margin like anything else.
#
# What it is NOT: it takes no cut list, no BOM, no Work Order and none of the
# seventeen operations. Those describe the shop floor, and the shop floor is
# exactly what is not involved.
SUBCONTRACT = "Subcontract"
SITE_WORK = (REPAIR, SUPPLY_INSTALL)
# Everything that happens at the client's place rather than on the floor.
# SITE_WORK stays what it was — those two share a labour model, and
# subcontract has none of its own — but all three skip parts and nesting.
OFF_FLOOR = (REPAIR, SUPPLY_INSTALL, SUBCONTRACT)

PRODUCTIVE_MIN_PER_DAY = 360.0
ON_SITE_WORKSTATION = "On-Site"
TO_INSPECT = "To Inspect"


def repair_row_minutes(row):
    """(carpenter minutes, helper minutes) for one activity row, quantity
    included. `row` is anything with .get() or attributes — a Frappe child row
    or a plain dict, so the engine stays testable without a database."""
    def f(name):
        if isinstance(row, dict):
            return _num(row.get(name))
        return _num(getattr(row, name, 0))

    qty = f("qty") or 1
    return (qty * (f("carpenters") or 0) * f("carp_min"),
            qty * (f("helpers") or 0) * f("helper_min"))


def _row_status(row):
    if isinstance(row, dict):
        return (row.get("status") or "").strip()
    return (getattr(row, "status", "") or "").strip()


def _cell(row, field, default=None):
    """One field off a table row, as it is, from either a dict or a doc.

    Distinct from _get, which coerces to float and reads attributes only:
    that is right for a Settings number and wrong for both a plain dict and a
    row's article name. Tests pass dicts; the desk passes documents; neither
    should have to care."""
    if isinstance(row, dict):
        v = row.get(field, default)
    else:
        v = getattr(row, field, default)
    return default if v is None else v


def calc_repair(activities, settings, markup_pct=None, visit_charge=None, visits=None):
    """Cost a repair SKU's activity table.

    `markup_pct` / `visit_charge` default to the Estimate Settings policy
    (`markup_repair`, `repair_visit_charge`); both are 0 in code on purpose —
    the real values live only in the site DB.

    `visits` overrides the derived day count for a job that is spread over
    more trips than its minutes imply (two half-days a week apart is two
    visits, not one).
    """
    rates = staff_rates(settings)
    markup = _num(markup_pct if markup_pct is not None else _get(settings, "markup_repair"))
    day_rate = _num(visit_charge if visit_charge is not None else _get(settings, "repair_visit_charge"))

    carp_min = helper_min = 0.0
    hold_carp = hold_helper = 0.0
    to_inspect = 0
    for row in activities or []:
        c, h = repair_row_minutes(row)
        if _row_status(row) == TO_INSPECT:
            to_inspect += 1
            hold_carp += c
            hold_helper += h
            continue
        carp_min += c
        helper_min += h

    # A day on site is the same 360 productive minutes as a day on the floor.
    est_days = max(carp_min, helper_min) / PRODUCTIVE_MIN_PER_DAY
    derived_visits = int(math.ceil(est_days)) if est_days > 0 else 0
    if visits is not None and _num(visits) > 0:
        billed_visits = int(_num(visits))
    else:
        billed_visits = derived_visits

    labor_cost = carp_min / 60.0 * rates["carpenter"] + helper_min / 60.0 * rates["helper"]
    client_labor = labor_cost * (1 + markup / 100.0)
    visit_amount = billed_visits * day_rate
    # The day rate is a FLOOR, not an addition: a full day's work already pays
    # for the day, so the two are never charged on top of each other.
    client_repair = max(client_labor, visit_amount)
    return {
        "carp_min": carp_min,
        "helper_min": helper_min,
        "est_days": est_days,
        "visits": billed_visits,
        "derived_visits": derived_visits,
        "labor_cost": labor_cost,
        "client_labor": client_labor,
        "day_rate": day_rate,
        "visit_amount": visit_amount,
        "visit_topup": max(0.0, visit_amount - client_labor),
        "client_repair": client_repair,
        "markup_pct": markup,
        "to_inspect": to_inspect,
        "to_inspect_carp_min": hold_carp,
        "to_inspect_helper_min": hold_helper,
    }


def calc_subcontract(lines, settings, markup_pct=None):
    """Cost a subcontract SKU from its vendor lines.

    One line is one trade at one vendor's rate, in THAT ARTICLE'S OWN UNIT —
    sqft of POP, points of wiring, running feet of conduit, or a lumpsum. The
    unit belongs to the article precisely so a person quoting on site types
    one number rather than reconciling three, which is also why electrical is
    three articles instead of one with three columns.

    `markup_pct` defaults to the Estimate Settings policy for subcontracted
    work, and like every rate in this file it is 0 in code on purpose: the
    real percentage lives only in the site DB.

    A line whose rate is 0 is NOT priced at zero and quietly added in. It is
    counted and named, the same way an unpriced material is, because a
    subcontract quote missing one vendor's rate looks exactly like a complete
    one — the total is simply too low, and nothing on the page says so.
    """
    markup = _num(markup_pct if markup_pct is not None else
                  _get(settings, "markup_subcontract"))
    rows, cost, unpriced = [], 0.0, []
    for line in lines or []:
        qty = _num(_cell(line, "qty"))
        rate = _num(_cell(line, "rate"))
        amount = qty * rate
        article = _cell(line, "article", "") or _cell(line, "article_code", "")
        if not rate:
            unpriced.append(str(article))
        cost += amount
        rows.append({
            "article": article,
            "vendor": _cell(line, "vendor", ""),
            "qty": qty,
            "uom": _cell(line, "uom", ""),
            "rate": rate,
            "rate_source": _cell(line, "rate_source", "") or ("unset" if not rate else ""),
            "amount": amount,
        })
    client = cost * (1 + markup / 100.0)
    return {
        "lines": rows,
        "cost": cost,
        "markup_pct": markup,
        "client_total": client,
        # The studio's own number, and it never leaves this dict for a
        # client-facing payload — same rule every other margin follows.
        "margin": client - cost,
        "unpriced": unpriced,
    }


def bought_out_value(cost, settings, markup_pct=None):
    """What a bought-in finished article is billed at.

    Its own margin policy, deliberately thin: a client can look up what a door
    costs, so the money on this work is in the fitting, not in the trading.
    0 in code as every rate is — the real percentage lives in the site DB."""
    markup = _num(markup_pct if markup_pct is not None else _get(settings, "markup_bought_out"))
    return _num(cost) * (1 + markup / 100.0), markup
