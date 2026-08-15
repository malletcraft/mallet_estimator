// Site Photo 360 — the split runs on save whenever pano/FOV/size changed;
// the button exists to FORCE a re-render (e.g. after a projection fix).
frappe.ui.form.on('Site Photo 360', {
	refresh(frm) {
		if (frm.is_new()) return;
		if (frm.doc.pano) {
			frm.add_custom_button(__('Re-split faces'), () => {
				frappe.call({
					method: 'mallet_estimator.mallet_estimator.doctype.site_photo_360.site_photo_360.resplit',
					args: { name: frm.doc.name },
					callback: () => {
						frappe.show_alert({ message: __('Split queued — faces refresh in a minute.'), indicator: 'blue' });
					},
				});
			});
		}
		if (frm.doc.status === 'Processing') {
			frm.dashboard.set_headline(__('Splitting in the background — reload in a minute.'));
		}
	},
});
