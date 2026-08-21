# Every stage a photograph has ever been filed under.
#
# Amit, 2026-08-21: "foto should be captured by selecting correct stage on
# first instance. if my mistake its done, a history of stage change should be
# recorded like audit trail on that foto. but let user alter the stage as it
# can be by mistake."
#
# He is right and it beats the lock I shipped an hour earlier. A hard block
# produces the worse failure: a photo permanently mis-staged because the only
# person who noticed is the one who cannot fix it. A log gives both halves —
# correcting a mistake stays cheap, and what was corrected stays permanent.
#
# Read-only in every column, because a mutable audit trail is decoration.
from frappe.model.document import Document


class SitePhotoStageLog(Document):
    pass
