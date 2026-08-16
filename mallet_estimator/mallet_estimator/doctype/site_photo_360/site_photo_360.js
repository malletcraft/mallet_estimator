// Site Photo 360 — the split runs on save whenever pano/FOV/size changed;
// the button exists to FORCE a re-render (e.g. after a projection fix).
//
// The gallery is the point of this form. Before it, looking at a photo meant
// clicking an attachment and landing on a bare image in a new tab — and the
// ANNOTATIONS, the whole reason the photo came back from ImageMeter, were
// buried in a collapsed child table where nobody found them.

const MCFT_FACES = [
	{ key: 'front', label: 'Front' },
	{ key: 'right', label: 'Right' },
	{ key: 'back', label: 'Back' },
	{ key: 'left', label: 'Left' },
	{ key: 'up', label: 'Top' },
	{ key: 'down', label: 'Bottom' },
];

function mcft_esc(s) {
	return String(s == null ? '' : s).replace(/[&<>"']/g, (c) =>
		({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function mcft_shots(doc) {
	// Faces first, then every annotation under the face it marks — so a photo
	// and the notes drawn on it sit together instead of in separate places.
	const byFace = {};
	(doc.annotations || []).forEach((a) => {
		(byFace[a.face] = byFace[a.face] || []).push(a);
	});
	const out = [];
	MCFT_FACES.forEach((f) => {
		const url = doc['face_' + f.key];
		if (url) {
			out.push({ url, label: f.label, kind: 'face', face: f.key,
				marks: (byFace[f.key] || []).length });
		}
		(byFace[f.key] || []).forEach((a, i) => {
			out.push({
				url: a.image, label: `${f.label} · note ${i + 1}`, kind: 'note',
				face: f.key, note: a.note || '', source: a.source || '',
			});
		});
	});
	// The pano last: it is the source, not something you read.
	if (doc.pano) out.push({ url: doc.pano, label: '360 (source)', kind: 'pano' });
	return out;
}

function mcft_render_gallery(frm) {
	const field = frm.get_field('gallery_html');
	if (!field) return;
	const $w = field.$wrapper;
	const shots = mcft_shots(frm.doc);

	if (!shots.length) {
		$w.html(`<div class="text-muted" style="padding:16px">
			${frm.doc.pano ? __('Splitting — reload in a minute.')
				: __('Attach the 360 photo above and save.')}</div>`);
		return;
	}

	$w.html(`
		<div class="mcft-gal" style="display:flex;gap:14px;align-items:flex-start">
			<div class="mcft-thumbs" style="flex:0 0 132px;max-height:560px;overflow-y:auto;
				display:flex;flex-direction:column;gap:8px"></div>
			<div style="flex:1;min-width:0">
				<div class="mcft-main" style="border:1px solid var(--border-color);border-radius:8px;
					overflow:hidden;background:#111;text-align:center">
					<img class="mcft-main-img" style="max-width:100%;max-height:560px;display:block;margin:0 auto">
				</div>
				<div class="mcft-cap text-muted" style="padding:8px 2px;font-size:12px"></div>
			</div>
		</div>`);

	const $thumbs = $w.find('.mcft-thumbs');
	const $img = $w.find('.mcft-main-img');
	const $cap = $w.find('.mcft-cap');

	function show(i) {
		const s = shots[i];
		$img.attr('src', s.url);
		$cap.html(
			`<b>${mcft_esc(s.label)}</b>` +
			(s.note ? ' — ' + mcft_esc(s.note) : '') +
			(s.source ? ` <span class="text-muted">(${mcft_esc(s.source)})</span>` : '') +
			` · <a href="${mcft_esc(s.url)}" target="_blank">${__('open full size')}</a>`);
		$thumbs.find('.mcft-thumb').removeClass('mcft-on')
			.css({ outline: 'none' });
		$thumbs.find('.mcft-thumb').eq(i).addClass('mcft-on')
			.css({ outline: '3px solid var(--primary)' });
	}

	shots.forEach((s, i) => {
		const badge = s.marks
			? `<span style="position:absolute;top:3px;right:3px;background:var(--primary);color:#fff;
				font-size:10px;font-weight:700;border-radius:99px;padding:1px 6px">${s.marks}</span>` : '';
		const tint = s.kind === 'note' ? 'var(--primary)'
			: (s.kind === 'pano' ? 'var(--gray-400)' : 'transparent');
		$thumbs.append(`
			<div class="mcft-thumb" data-i="${i}" style="position:relative;cursor:pointer;
				border-radius:6px;overflow:hidden;border-left:3px solid ${tint};background:#111">
				<img src="${mcft_esc(s.url)}" loading="lazy"
					style="width:100%;height:84px;object-fit:cover;display:block">
				${badge}
				<div style="position:absolute;left:0;right:0;bottom:0;background:rgba(0,0,0,.6);
					color:#fff;font-size:10px;padding:2px 4px">${mcft_esc(s.label)}</div>
			</div>`);
	});
	$thumbs.find('.mcft-thumb').on('click', function () { show($(this).data('i')); });
	show(0);
}

frappe.ui.form.on('Site Photo 360', {
	refresh(frm) {
		if (frm.is_new()) return;
		mcft_render_gallery(frm);

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
		if (frm.doc.project && frm.doc.room) {
			frm.add_custom_button(__('This room over time'), () => {
				frappe.set_route('List', 'Site Photo 360', {
					project: frm.doc.project, room: frm.doc.room,
				});
			});
		}
	},
});
