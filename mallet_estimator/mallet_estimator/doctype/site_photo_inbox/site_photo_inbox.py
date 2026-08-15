import frappe
from frappe.model.document import Document


class SitePhotoInbox(Document):
    """A returning file the sync could not prove belongs to a capture.

    It sits here until a person names the capture and face — attaching a
    stranger's photo to a client's room is worse than asking."""

    def validate(self):
        if self.photo and self.face and self.status == "Pending":
            self.status = "Ready"

    def on_update(self):
        if self.status == "Ready" and self.photo and self.face:
            from mallet_estimator import imagemeter_sync
            imagemeter_sync.attach_from_inbox(self.name)
