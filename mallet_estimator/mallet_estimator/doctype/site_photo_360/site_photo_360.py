# Site Photo 360 — one doc per capture, never overwritten: a room's progress
# timeline IS the list of these docs (project + room, ordered by capture_date).
# The split runs as a background job — a 60 MB pano upload must never hold a
# web worker hostage.
import frappe
from frappe.model.document import Document

from mallet_estimator import panorama

FACE_FIELDS = {name: f"face_{name}" for name in panorama.FACE_NAMES}


class SitePhoto360(Document):

    def validate(self):
        fov, face_px = panorama.clamp_params(self.fov, self.face_px)
        self.fov, self.face_px = int(fov), face_px
        self.title = " · ".join(filter(None, [
            self.room, str(self.capture_date or ""), self.stage]))
        if not self.pano:
            self.status = "Pending"

    def on_update(self):
        if self.pano and self._signature() != (self.split_signature or ""):
            self.queue_split()

    def _signature(self):
        return f"{self.pano}|{int(self.fov or 0)}|{int(self.face_px or 0)}"

    def queue_split(self):
        self.db_set("status", "Processing", update_modified=False)
        kwargs = dict(queue="long", timeout=600, enqueue_after_commit=True,
                      name=self.name)
        try:
            frappe.enqueue(run_split, job_id=f"mcft_split_{self.name}",
                           deduplicate=True, **kwargs)
        except TypeError:
            # Older RQ wrapper without dedup — a doubled job is merely wasteful,
            # the signature check makes the second one a no-op.
            frappe.enqueue(run_split, **kwargs)


def run_split(name):
    """The job: pano → 6 gnomonic faces attached as private Files."""
    doc = frappe.get_doc("Site Photo 360", name)
    try:
        _split(doc)
    except Exception as exc:
        doc.db_set("status", "Failed", update_modified=False)
        doc.db_set("split_error", str(exc)[:500], update_modified=False)
        frappe.log_error(frappe.get_traceback(), f"Site Photo split: {name}")


def _split(doc):
    content = _private_pano_content(doc)
    faces = panorama.split_to_jpeg(content, doc.fov, doc.face_px)

    for face, field in FACE_FIELDS.items():
        _drop_old_face(doc, field)
        f = frappe.get_doc({
            "doctype": "File",
            "file_name": f"{doc.name.replace('/', '-')}_{face}.jpg",
            "attached_to_doctype": doc.doctype,
            "attached_to_name": doc.name,
            "attached_to_field": field,
            "is_private": 1,
            "content": faces[face],
        }).insert(ignore_permissions=True)
        doc.db_set(field, f.file_url, update_modified=False)

    doc.db_set("split_signature", doc._signature(), update_modified=False)
    doc.db_set("split_error", "", update_modified=False)
    doc.db_set("status", "Split", update_modified=False)


def _private_pano_content(doc):
    """Fetch the pano bytes — and force the File private first. These are the
    insides of clients' homes; nothing here may sit on a guessable public URL
    (the same leak-safety-by-construction rule the prints follow)."""
    file_doc = frappe.get_doc("File", {"file_url": doc.pano,
                                       "attached_to_name": doc.name})
    if not file_doc.is_private:
        file_doc.is_private = 1
        file_doc.save(ignore_permissions=True)
        doc.db_set("pano", file_doc.file_url, update_modified=False)
        doc.pano = file_doc.file_url
    return file_doc.get_content()


def _drop_old_face(doc, field):
    """A re-split replaces the face File instead of piling orphans — the
    VERSION axis is the document series, never stacked files on one doc."""
    old = doc.get(field)
    if not old:
        return
    for f in frappe.get_all("File", filters={
            "attached_to_doctype": doc.doctype, "attached_to_name": doc.name,
            "file_url": old}, pluck="name"):
        frappe.delete_doc("File", f, ignore_permissions=True, force=True)


@frappe.whitelist()
def resplit(name):
    """The button. Save-driven splitting covers the normal path; this forces a
    regeneration (e.g. after a code-side projection fix) without a field edit."""
    doc = frappe.get_doc("Site Photo 360", name)
    doc.check_permission("write")
    if not doc.pano:
        frappe.throw("Attach the 360 photo first.")
    doc.queue_split()
    return {"queued": True}
