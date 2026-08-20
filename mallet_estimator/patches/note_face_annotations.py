# Forces the migrate that model-syncs face_annotations onto Site Photo 360
# (the field was renamed after a CI-caught collision with the legacy
# `annotations` child table; a Python-only deploy runs no migrate).
def execute():
    pass
