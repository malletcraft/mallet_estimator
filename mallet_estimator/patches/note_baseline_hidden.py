"""No-op: forces the migrate that hides the geometry-baseline fields.

A Python-only deploy runs no migrate, so the hidden flags would sit in the
source and the fields would stay visible in the desk — which is the thing
being removed. Registering a patch is what makes the model sync happen.
"""


def execute():
    pass
