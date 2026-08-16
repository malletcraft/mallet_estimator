# Serving the PWA's service worker and manifest at /sitephoto/*.
#
# Two Frappe facts force this file to exist. First, frappe refuses to serve
# .js and .json statically from www/ (UNSUPPORTED_STATIC_PAGE_TYPES), so the
# worker cannot simply live beside the page. Second, a service worker may only
# control paths at or below its own URL unless the response carries
# Service-Worker-Allowed — so serving it from /assets would scope it to
# /assets and leave /sitephoto uncontrolled, which means no offline shell and
# no installable app.
#
# A page renderer is the supported way to answer a URL with our own response,
# headers included.
import os

import frappe
from frappe.website.page_renderers.base_renderer import BaseRenderer
from werkzeug.wrappers import Response

ASSET_DIR = "public/sitephoto"
ROUTES = {
    "sitephoto/sw.js": ("sw.js", "application/javascript"),
    "sitephoto/manifest.json": ("manifest.json", "application/manifest+json"),
}


def _path(filename):
    return os.path.join(frappe.get_app_path("mallet_estimator"), ASSET_DIR, filename)


class SitePhotoAssetRenderer(BaseRenderer):
    """Answers /sitephoto/sw.js and /sitephoto/manifest.json."""

    def can_render(self):
        route = (self.path or "").strip("/")
        return route in ROUTES and os.path.exists(_path(ROUTES[route][0]))

    def render(self):
        filename, mime = ROUTES[(self.path or "").strip("/")]
        with open(_path(filename), "rb") as f:
            body = f.read()
        r = Response(body, mimetype=mime)
        # Without this the worker's scope is /sitephoto/ only — which happens
        # to be enough here, but stating it keeps the app free to move.
        r.headers["Service-Worker-Allowed"] = "/"
        # The worker is the update mechanism for the whole app; a cached copy
        # would pin users to an old build.
        r.headers["Cache-Control"] = "no-cache, must-revalidate"
        return r
