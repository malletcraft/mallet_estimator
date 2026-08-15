# /sitephoto — the site-photo PWA shell. Session-guarded: the app holds no
# API key of its own, it acts as whoever is logged in on the phone.
import frappe

no_cache = 1


def get_context(context):
    if frappe.session.user == "Guest":
        frappe.local.flags.redirect_location = "/login?redirect-to=/sitephoto"
        raise frappe.Redirect

    context.csrf_token = frappe.sessions.get_csrf_token()
    context.user = frappe.session.user
    context.full_name = frappe.utils.get_fullname(frappe.session.user)
    return context
