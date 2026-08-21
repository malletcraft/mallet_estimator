"""The lists and the naming rule — deliberately free of frappe.

Everything here is data plus one pure function, so the ordering rules can be
asserted by a unit test that needs no bench: sequences strictly increasing,
every job type reachable, phases appearing in trade order and never twice.
Those are the rules a hand edit is most likely to break and the least likely
to notice breaking, because a wrong order still saves and still renders.

worksite.py re-exports all of it, so `worksite.WORK_STAGES` keeps working.

Three masters live here, all seeded imperatively and idempotently the way
every other master in this app is (there are no fixtures):

  Mallet Site        a place a client owns — Client → SITE → Project
  Mallet Article     the third token of an SKU code (WAR, PVC, HNG)
  Mallet Work Stage  one step of a fit-out, in the order the trades run

The interesting decision is the last one. A repair and a supply-and-install
job are not a different sequence from new work — they are a SLICE of it. A
PVC bathroom door is 'Door & window frames' plus 'Doors, shutters &
hardware'. A sagging wardrobe shutter is 'Modular carpentry install'. So a
stage carries the job types that can REACH it, instead of each job type
owning a private list, and one vocabulary covers the six-lakh fit-out and
the eight-thousand-rupee door job alike. Progress reporting then works
across all three without translating anything.
"""

import re

NEW, REPAIR, INSTALL = "New work", "Repair", "Supply & install"
JOB_TYPES = (NEW, REPAIR, INSTALL)
ALL = f"{NEW}, {REPAIR}, {INSTALL}"

PHASES = ("Survey", "Civil", "First fix", "Ceiling", "Surfaces",
          "Delivery", "Joinery", "Second fix", "Finishing", "Closing")

# (code, article, job types)
ARTICLES = [
    ("WAR", "Wardrobe",              f"{NEW}, {REPAIR}"),
    ("BED", "Bed",                   f"{NEW}, {REPAIR}"),
    ("LOF", "Loft",                  NEW),
    ("STU", "Study table",           NEW),
    ("TVU", "TV unit",               NEW),
    ("CRD", "Crockery unit",         NEW),
    ("KIT", "Kitchen base",          f"{NEW}, {REPAIR}"),
    ("KWL", "Kitchen wall unit",     NEW),
    ("VAN", "Vanity",                NEW),
    ("SHO", "Shoe rack",             NEW),
    ("PAR", "Partition",             NEW),
    ("PUJ", "Pooja unit",            NEW),
    ("STO", "Storage",               NEW),
    ("SHT", "Shutter",               REPAIR),
    ("DRW", "Drawer set",            REPAIR),
    ("HNG", "Hinges & hardware",     REPAIR),
    ("LAM", "Laminate patch",        REPAIR),
    ("DOR", "Flush door",            f"{REPAIR}, {INSTALL}"),
    ("FRM", "Door frame",            INSTALL),
    ("PVC", "PVC bathroom door",     INSTALL),
    ("WIN", "Window",                INSTALL),
    ("MSH", "Mosquito mesh",         INSTALL),
    ("GRL", "Safety grill",          INSTALL),
    ("BLD", "Blinds",                INSTALL),
    ("WLP", "Wallpaper",             INSTALL),
    ("FCL", "False ceiling",         INSTALL),
]

# (sequence, phase, stage, job types, why it sits here)
#
# Sequences step by 10 so a stage can be inserted between two others without
# renumbering thirty-nine rows — and without the order silently becoming
# alphabetical, which would put 'Deep clean' before 'Demolition'.
WORK_STAGES = [
    (10,  "Survey", "As-built site measure", ALL,
     "Before a hammer moves — the 360 set the estimate is built on."),
    (20,  "Survey", "Existing condition — builder handover", NEW,
     "What you inherited, before you touch it."),
    (30,  "Survey", "Defect recorded", REPAIR,
     "A repair job starts here: what is wrong, photographed."),
    (40,  "Survey", "Opening measured", INSTALL,
     "An installation starts from the built hole, never from the drawing."),

    (50,  "Civil", "Demolition & debris removal", NEW,
     "Everything wet and dusty happens first."),
    (60,  "Civil", "Masonry & block work", NEW, "New walls, niches, ledges."),
    (70,  "Civil", "Plaster & waterproofing", f"{NEW}, {REPAIR}",
     "Must cure fully before primer, or the paint blisters later."),
    (80,  "Civil", "Door & window frames (chowkat)", f"{NEW}, {INSTALL}",
     "Set into masonry before plaster closes around them."),

    (90,  "First fix", "Electrical first fix — conduit & wiring", NEW,
     "Concealed. Changing it once the ceiling boards up is the most expensive "
     "mistake on the job."),
    (100, "First fix", "Plumbing first fix — concealed lines", f"{NEW}, {REPAIR}",
     "Same rule, same bill."),
    (110, "First fix", "HVAC / AC piping & drain", NEW,
     "Drain slope is set now or never."),
    (120, "First fix", "Data, network & CCTV conduit", NEW,
     "Cheap now, a chase in the wall later."),

    (130, "Ceiling", "False ceiling framing (GI grid)", f"{NEW}, {INSTALL}",
     "After the services are proven, before any wall finish."),
    (140, "Ceiling", "POP / gypsum board & finish", ALL,
     "The dirtiest trade in the flat — finish it before painting anything."),
    (150, "Ceiling", "Cove & profile-light recesses", NEW,
     "Set out now; the fittings arrive at second fix."),

    (160, "Surfaces", "Floor tiling", NEW,
     "Wet trades together, then protect the floor."),
    (170, "Surfaces", "Wall tiling / dado", f"{NEW}, {REPAIR}",
     "After plumbing first fix, before the CP fittings."),
    (180, "Surfaces", "Wooden, vinyl or laminate flooring", f"{NEW}, {INSTALL}",
     "Dry floors go down late, once the wet work is out."),
    (190, "Surfaces", "Stone & marble polish", f"{NEW}, {REPAIR}",
     "Polishing slurry ruins finished joinery — do it first."),

    (200, "Delivery", "Material delivered on site", ALL,
     "The one stage every job type shares in the middle."),
    (210, "Delivery", "Modular units delivered", NEW,
     "Photographed on arrival — this is the damage-in-transit record, and "
     "nobody takes it unless the app asks."),

    (220, "Joinery", "Wall panelling", ALL,
     "Services may run behind it — it closes them in."),
    (230, "Joinery", "Wall moulding & trims", ALL,
     "Fixed, filled and sanded BEFORE primer, never after."),
    (240, "Joinery", "Modular carpentry install", f"{NEW}, {REPAIR}",
     "The shop's own work: wardrobes, beds, lofts, kitchen."),
    (250, "Joinery", "Loose furniture", NEW,
     "Last thing in, first thing damaged."),

    (260, "Second fix", "Electrical second fix — switches & points", f"{NEW}, {REPAIR}",
     "The cables from first fix finally get their fittings."),
    (270, "Second fix", "Light installation", ALL,
     "Into the recesses set out at the ceiling stage."),
    (280, "Second fix", "Plumbing second fix — CP & sanitary", f"{NEW}, {REPAIR}",
     "After the dado tiling is grouted and cured."),
    (290, "Second fix", "Window jali, mesh & grill", ALL,
     "Measured off the finished opening, not the drawing."),
    (300, "Second fix", "Doors, shutters & hardware", ALL,
     "Hung after the floors are laid, so the gap under them is right."),

    (310, "Finishing", "Putty & primer", f"{NEW}, {REPAIR}",
     "Only once every fixing hole exists and is filled."),
    (320, "Finishing", "Paint, wallpaper or texture", ALL,
     "Ceiling, then walls, then trim."),
    (330, "Finishing", "Glass & mirrors", ALL, "Cut to the built opening."),
    (340, "Finishing", "Curtains & blinds", f"{NEW}, {INSTALL}",
     "After the final coat is hard."),
    (350, "Finishing", "Deep clean", ALL,
     "The flat has to be clean to be snagged honestly."),

    (360, "Closing", "Snagging", ALL,
     "The 360 set taken here is the defect record."),
    (370, "Closing", "Snag rectification", NEW,
     "Photographed against the snag shot."),
    (380, "Closing", "Rectified & verified", REPAIR,
     "A repair job's closing proof."),
    (390, "Closing", "Handover", ALL, "The set that goes to the client."),
]

# The six values Site Photo 360.stage carried before this. They were PHASES
# all along, which is why replacing them loses nothing: each one maps onto a
# phase of the same meaning. The patch rewrites them and parks the original
# in mallet_stage_legacy — legacy is hidden, never deleted.
STAGE_RENAMES = {
    "Baseline": "Survey",
    "Civil": "Civil",
    "Wiring": "First fix",
    "Carpentry": "Joinery",
    "Finishing": "Finishing",
    "Handover": "Closing",
}

DEFAULT_SITE_NAME = "Main site"


def site_key(text):
    """Case-, space- and underscore-insensitive identity, the same rule
    sitephoto.ensure_site already uses for clients and projects. 'Kothrud
    Flat' and 'kothrud_flat' are one place; treating them as two is how a
    client ends up with half their photos under each spelling."""
    return re.sub(r"[\s_]+", " ", (text or "").strip()).casefold()
