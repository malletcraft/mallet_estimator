"""The rooms x services matrix, before it is a document.

Amit, 2026-09-25: "while estimating for a full service, i would like to have a
matrix of rooms and services to be discussed with client. along with its
approximately sqft rate which should be configurable."

WHAT THIS IS FOR, and why it is not the Estimate. An Estimate is the detailed
cost model: cut lists, parts, workstation minutes, a number defensible to the
rupee and unavailable until somebody has modelled the article in SketchUp. A
scope conversation happens BEFORE any of that, on a first visit, and needs a
believable total in ten minutes from three inputs a person can pace out --
which rooms, which services, roughly how many square feet. Two different
questions at two different stages; mixing them into one document would make
both harder to read.

Pure functions on purpose: this is estimating maths, so it is unit-tested like
estimating maths, with no bench and no database.

NO RATE APPEARS IN THIS FILE, or anywhere else in this repo. Rates live in the
Service Rate doctype in the site database, keyed by a person. mallet_estimator
is public, so a committed figure would be permanent and world-readable.
"""


def cell_amount(area_sqft, rate):
    """One cell: a room's area at a service's rate.

    Either side missing prices at NOTHING rather than guessing. A service
    nobody has rated yet must not quietly contribute zero to a total that
    reads as complete -- suspect_cells below is what says so out loud.
    """
    try:
        a = float(area_sqft or 0)
        r = float(rate or 0)
    except (TypeError, ValueError):
        return 0.0
    if a <= 0 or r <= 0:
        return 0.0
    return round(a * r, 2)


def build_matrix(rooms, services, kept=None):
    """Every room crossed with every service, preserving what was unticked.

    `rooms`    : [{"room":..., "area_sqft":...}]
    `services` : [{"article":..., "rate":...}]
    `kept`     : {(room, article): include} from the rows already on the doc.

    A REBUILD MUST NOT SILENTLY RE-TICK A CELL somebody deliberately cleared.
    The matrix is regenerated on every save, because adding a room has to add
    its column -- and if that also resurrected the six services this room does
    not need, the document would fight the person editing it. Anything absent
    from `kept` is a NEW cell and starts ticked, which is right: a room just
    added is presumed to want the services already in the conversation.
    """
    kept = kept or {}
    out = []
    for r in rooms:
        room = (r.get("room") or "").strip()
        if not room:
            continue
        area = r.get("area_sqft") or 0
        for s in services:
            art = (s.get("article") or "").strip()
            if not art:
                continue
            rate = s.get("rate") or 0
            include = kept.get((room, art), 1)
            out.append({
                "room": room,
                "article": art,
                "include": 1 if include else 0,
                "area_sqft": float(area or 0),
                "rate": float(rate or 0),
                "amount": cell_amount(area, rate) if include else 0.0,
            })
    return out


def totals(lines, rooms):
    """Total sqft and total money.

    Sqft comes from the ROOMS and not from the lines: a room appears once in
    the matrix per service, so summing the lines would multiply the floor area
    by the number of services -- a number that looks plausible and is six
    times too big.
    """
    total_sqft = 0.0
    for r in rooms:
        try:
            total_sqft += float(r.get("area_sqft") or 0)
        except (TypeError, ValueError):
            pass
    money = 0.0
    for l in lines:
        if l.get("include"):
            money += float(l.get("amount") or 0)
    return round(total_sqft, 2), round(money, 2)


def suspect_cells(lines):
    """Cells that contribute nothing while looking like they should.

    A ticked service with no rate, or a ticked room with no area, adds zero to
    a total the client is being shown as the answer. Silence there is the
    failure this codebase keeps meeting, so the document says which cells are
    hollow rather than quietly under-quoting.
    """
    bad = []
    for l in lines:
        if not l.get("include"):
            continue
        if float(l.get("rate") or 0) <= 0:
            bad.append((l.get("room"), l.get("article"), "no rate"))
        elif float(l.get("area_sqft") or 0) <= 0:
            bad.append((l.get("room"), l.get("article"), "no area"))
    return bad
