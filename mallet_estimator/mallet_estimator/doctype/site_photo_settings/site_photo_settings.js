// Sync now — the same job the hourly scheduler runs.
frappe.ui.form.on('Site Photo Settings', {
	refresh(frm) {
		frm.add_custom_button(__('Sync now'), () => {
			frappe.call({
				method: 'mallet_estimator.imagemeter_sync.sync',
				freeze: true, freeze_message: __('Talking to Google Drive…'),
				callback: (r) => {
					frappe.msgprint({
						title: __('Drive sync'), indicator: 'blue',
						message: '<pre>' + frappe.utils.escape_html(
							JSON.stringify(r.message || {}, null, 1)) + '</pre>',
					});
					frm.reload_doc();
				},
			});
		});
	},
});
