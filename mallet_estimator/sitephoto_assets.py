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

# Digital Asset Links: how Android proves the installed app owns this site.
# Without it a TWA still runs but shows the browser URL bar, which is the
# difference between "an app" and "a web page in a chrome-less tab".
ASSETLINKS = ".well-known/assetlinks.json"


def _path(filename):
    return os.path.join(frappe.get_app_path("mallet_estimator"), ASSET_DIR, filename)


class SitePhotoAssetRenderer(BaseRenderer):
    """Answers /sitephoto/sw.js and /sitephoto/manifest.json."""

    def can_render(self):
        route = (self.path or "").strip("/")
        if route == ASSETLINKS:
            return bool(_assetlinks())
        return route in ROUTES and os.path.exists(_path(ROUTES[route][0]))

    def render(self):
        if (self.path or "").strip("/") == ASSETLINKS:
            r = Response(frappe.as_json(_assetlinks()), mimetype="application/json")
            # Chrome fetches this anonymously, from Google's servers as well as
            # the device — it must never require a session.
            r.headers["Cache-Control"] = "public, max-age=300"
            return r
        return self._render_asset()

    def _render_asset(self):
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


def _assetlinks():
    """The statement list, or [] when the app is not configured yet.

    Returning [] rather than a half-filled statement is deliberate: a
    malformed assetlinks file is cached by Google and by the device, so
    publishing a wrong one is slower to undo than publishing none."""
    try:
        s = frappe.get_cached_doc("Site Photo Settings")
    except Exception:
        return []
    package = (s.get("twa_package") or "").strip()
    prints = [f.strip().upper() for f in (s.get("twa_fingerprints") or "").splitlines()
              if f.strip()]
    if not package or not prints:
        return []
    return [{
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {"namespace": "android_app", "package_name": package,
                   "sha256_cert_fingerprints": prints},
    }]
