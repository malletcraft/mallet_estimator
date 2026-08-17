# Deciding WHAT to sync with Drive. The HTTP transport is deliberately not
# here: this module is the part that can be wrong in ways tests catch, and it
# is pure so they can.
#
# A correction worth recording. The caption burned into each face (handover.py)
# is primarily for a HUMAN to identify a returning photo — reading it back
# automatically would mean OCR on the bench, a heavy dependency for a job that
# has a cheaper answer. Automatic matching therefore relies on the filename,
# which survives if ImageMeter preserves it; anything else is handed to a
# person with the caption visible, and is never guessed. Attaching a stranger's
# photo to a client's room is far worse than asking.
from mallet_estimator import handover, panorama

# What came back and what we should do about it.
SKIP, ATTACH, REVIEW = "skip", "attach", "review"


def plan_uploads(photo_name, faces, existing_filenames=()):
    """Which faces still need handing over. Re-running a handover must not
    duplicate what is already in the folder, because ImageMeter users would
    then see the same wall twice with no way to tell which is current."""
    have = {str(f).lower() for f in existing_filenames}
    plan = []
    for face in panorama.FACE_NAMES:
        if not faces.get(face):
            continue
        fn = handover.handover_filename(photo_name, face)
        if fn.lower() in have:
            continue
        plan.append({"face": face, "filename": fn, "source": faces[face]})
    return plan


def classify_return(drive_file, imported_file_ids=(), known_photos=None,
                    device_ids=None):
    """Decide what a file sitting in ImageMeter's upload folder is.

    drive_file: {"id", "title", "parents_path": [...]} — parents_path is the
    folder trail below the ImageMeter root, e.g. ["yogesh_sar", "master_Bad"].
    device_ids: {MCAP-… : MEST-PH-…} for captures born on a phone, whose faces
    reached ImageMeter under the device's id because the server had not named
    them yet.

    Returns (action, payload). ATTACH only when the file names a capture we
    know; everything else is REVIEW with a reason a person can act on."""
    fid = drive_file.get("id")
    if fid and fid in set(imported_file_ids):
        return SKIP, {"reason": "already imported", "file_id": fid}

    title = drive_file.get("title") or ""
    photo, face = handover.parse_caption(title)

    if photo and handover.is_device_id(photo):
        # A device id is a claim, not a link. It resolves only through the
        # capture that actually synced; an unsynced or unknown one goes to a
        # person, because attaching a stranger's wall is worse than asking.
        resolved = (device_ids or {}).get(photo)
        if not resolved:
            return REVIEW, {"reason": f"device capture not synced yet: {photo}",
                            "file_id": fid, "title": title,
                            "folder": drive_file.get("parents_path") or []}
        photo = resolved

    if photo and known_photos is not None and photo not in set(known_photos):
        # The id looks like ours but names a capture this site does not have —
        # a typo or a file from another site. Never invent the link.
        return REVIEW, {"reason": f"no such capture: {photo}", "file_id": fid,
                        "title": title, "folder": drive_file.get("parents_path") or []}

    if photo and face:
        return ATTACH, {"photo": photo, "face": face, "file_id": fid, "title": title}

    if photo:
        return REVIEW, {"reason": "capture known but face unclear", "photo": photo,
                        "file_id": fid, "title": title}

    # The ordinary case: ImageMeter renamed it after the date. A person picks
    # the capture; the folder trail and the burned-in caption tell them which.
    return REVIEW, {"reason": "no capture id in the filename — identify from the caption",
                    "file_id": fid, "title": title,
                    "folder": drive_file.get("parents_path") or []}


def summarise(decisions):
    """Counts for the sync log, so a run that quietly did nothing says so."""
    out = {SKIP: 0, ATTACH: 0, REVIEW: 0}
    for action, _ in decisions:
        out[action] = out.get(action, 0) + 1
    return out
