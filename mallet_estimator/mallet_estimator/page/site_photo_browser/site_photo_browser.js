/* Site Photos — the folder browser.
 *
 * Client → project → room → captures, which is how ImageMeter's own folders
 * read and therefore how these photos are already thought about on site. The
 * tree is built from captures that EXIST, not from the room master: a room
 * nobody photographed is not a folder, it is noise.
 */
frappe.pages['site-photo-browser'].on_page_load = function (wrapper) {
	const page = frappe.ui.make_app_page({
		parent: wrapper, title: __('Site Photos'), single_column: true,
	});
	const $body = $(wrapper).find('.layout-main-section');
	$body.html(`
		<div class="mcft-browser" style="display:flex;gap:0;align-items:stretch;
			border:1px solid var(--border-color);border-radius:8px;overflow:hidden;min-height:520px">
			<div class="mcft-col mcft-clients" style="flex:0 0 220px;border-right:1px solid var(--border-color);
				overflow-y:auto;max-height:70vh"></div>
			<div class="mcft-col mcft-rooms" style="flex:0 0 240px;border-right:1px solid var(--border-color);
				overflow-y:auto;max-height:70vh"></div>
			<div class="mcft-col mcft-caps" style="flex:1;overflow-y:auto;max-height:70vh;padding:12px"></div>
		</div>`);

	const $clients = $body.find('.mcft-clients');
	const $rooms = $body.find('.mcft-rooms');
	const $caps = $body.find('.mcft-caps');
	const esc = frappe.utils.escape_html;

	function row(label, sub, active) {
		return `<div class="mcft-row" style="padding:9px 12px;cursor:pointer;border-bottom:1px solid var(--border-color);
			${active ? 'background:var(--bg-light-gray);font-weight:600' : ''}">
			<div>${esc(label)}</div>
			${sub ? `<div class="text-muted" style="font-size:11px">${esc(sub)}</div>` : ''}</div>`;
	}

	let TREE = { clients: [] };

	function paintClients(sel) {
		$clients.html('');
		if (!TREE.clients.length) {
			$clients.html(`<div class="text-muted" style="padding:16px">${__('No site photos yet.')}</div>`);
			return;
		}
		TREE.clients.forEach((c, i) => {
			const $r = $(row(c.client, `${c.captures} capture(s)`, i === sel));
			$r.on('click', () => { paintClients(i); paintRooms(i); });
			$clients.append($r);
		});
	}

	function paintRooms(ci) {
		$rooms.html(''); $caps.html('');
		const c = TREE.clients[ci];
		if (!c) return;
		c.projects.forEach((p) => {
			$rooms.append(`<div style="padding:8px 12px;background:var(--bg-light-gray);
				font-size:11px;text-transform:uppercase;letter-spacing:.03em;color:var(--text-muted)">
				${esc(p.title)}</div>`);
			p.rooms.forEach((rm) => {
				const $r = $(row(rm.room, `${rm.captures} · latest ${rm.latest || '—'}`));
				$r.on('click', () => {
					$rooms.find('.mcft-row').css({ background: '', fontWeight: '' });
					$r.css({ background: 'var(--bg-light-gray)', fontWeight: 600 });
					loadRoom(p.project, rm.room, `${c.client} / ${p.title} / ${rm.room}`);
				});
				$rooms.append($r);
			});
		});
	}

	function loadRoom(project, room, crumb) {
		$caps.html(`<div class="text-muted">${__('Loading…')}</div>`);
		frappe.call({
			method: 'mallet_estimator.sitephoto.room_captures',
			args: { project, room },
			callback: (r) => {
				const rows = r.message || [];
				let html = `<div style="font-weight:600;margin-bottom:10px">${esc(crumb)}</div>`;
				if (!rows.length) html += `<div class="text-muted">${__('No captures.')}</div>`;
				rows.forEach((d) => {
					const faces = ['front', 'right', 'back', 'left', 'up', 'down']
						.map((f) => d['face_' + f]).filter(Boolean);
					html += `<div style="border:1px solid var(--border-color);border-radius:8px;
						padding:10px;margin-bottom:10px">
						<div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
							<b>${esc(d.capture_date || '')}</b>
							${d.stage ? `<span class="text-muted">${esc(d.stage)}</span>` : ''}
							<span class="indicator-pill ${d.status === 'Split' ? 'green' : 'orange'}">${esc(d.status)}</span>
							${d.annotations ? `<span class="indicator-pill blue">${d.annotations} note(s)</span>` : ''}
							<a href="/app/site-photo-360/${encodeURIComponent(d.name)}"
								style="margin-left:auto">${__('Open')}</a>
						</div>
						<div style="display:flex;gap:6px;overflow-x:auto">
							${faces.map((u) => `<img src="${esc(u)}" loading="lazy"
								style="width:104px;height:104px;object-fit:cover;border-radius:6px;
								border:1px solid var(--border-color);background:#000">`).join('')}
						</div></div>`;
				});
				$caps.html(html);
			},
		});
	}

	frappe.call({
		method: 'mallet_estimator.sitephoto.tree',
		callback: (r) => {
			TREE = r.message || { clients: [] };
			paintClients(0);
			if (TREE.clients.length) paintRooms(0);
		},
	});

	page.set_primary_action(__('New capture'), () => {
		frappe.new_doc('Site Photo 360');
	}, 'add');
	page.add_menu_item(__('Open capture app'), () => { window.open('/sitephoto', '_blank'); });
};
