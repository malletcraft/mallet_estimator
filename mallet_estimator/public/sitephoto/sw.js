/* Site Photos — offline shell.
 *
 * A site visit is exactly where there is no signal, so the app must open and
 * accept a capture with the network down. This caches the shell so it starts
 * cold, keeps photos it has already shown, and NEVER caches an API write —
 * queued captures live in IndexedDB in the page, not here, because a request
 * replayed blindly by a worker is a capture nobody can see or cancel.
 */
const SHELL = 'mcft-shell-v3';
const MEDIA = 'mcft-media-v3';
const SHELL_URLS = [
	'/sitephoto',
	'/assets/mallet_estimator/images/sitephoto-192.png',
	'/assets/mallet_estimator/images/sitephoto-512.png',
];

self.addEventListener('install', (e) => {
	e.waitUntil(
		caches.open(SHELL)
			.then((c) => Promise.allSettled(SHELL_URLS.map((u) => c.add(u))))
			.then(() => self.skipWaiting())
	);
});

self.addEventListener('activate', (e) => {
	e.waitUntil(
		caches.keys()
			.then((keys) => Promise.all(
				keys.filter((k) => k !== SHELL && k !== MEDIA).map((k) => caches.delete(k))))
			.then(() => self.clients.claim())
	);
});

self.addEventListener('message', (e) => {
	if (e.data === 'skipWaiting') self.skipWaiting();
});

self.addEventListener('fetch', (event) => {
	const req = event.request;
	if (req.method !== 'GET') return;                 // writes never touch a cache
	const url = new URL(req.url);
	if (url.origin !== self.location.origin) return;

	// The app shell: fresh when we can reach the site, cached when we cannot.
	if (req.mode === 'navigate') {
		event.respondWith(
			fetch(req)
				.then((r) => {
					// Only a real page replaces the saved one. /sitephoto is
					// session-guarded, so an expired login answers with a
					// redirect to /login — and a navigation request carries
					// redirect:'manual', which makes that an opaque response
					// with ok=false. Cached, it would become the offline
					// shell: the app would open on site showing a login page
					// it cannot possibly complete, and the good copy would be
					// gone. Keeping the last real shell is strictly better.
					if (r.ok && r.type === 'basic' && !r.redirected) {
						const copy = r.clone();
						caches.open(SHELL).then((c) => c.put('/sitephoto', copy));
					}
					return r;
				})
				.catch(() => caches.match('/sitephoto').then((r) => r || offlineCard()))
		);
		return;
	}

	// Photos already looked at stay readable on site with no signal.
	if (url.pathname.startsWith('/private/files/') || url.pathname.startsWith('/files/')) {
		event.respondWith(
			caches.match(req).then((hit) => hit || fetch(req).then((r) => {
				if (r.ok) {
					const copy = r.clone();
					caches.open(MEDIA).then((c) => c.put(req, copy));
				}
				return r;
			}).catch(() => hit || Response.error()))
		);
		return;
	}

	if (url.pathname.startsWith('/assets/')) {
		event.respondWith(
			caches.match(req).then((hit) => hit || fetch(req).then((r) => {
				if (r.ok) {
					const copy = r.clone();
					caches.open(SHELL).then((c) => c.put(req, copy));
				}
				return r;
			}))
		);
		return;
	}
	// Everything else (the API) goes to the network untouched: a stale
	// project list is worse than an honest failure the app can queue around.
});

function offlineCard() {
	return new Response(
		'<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">' +
		'<body style="font:16px system-ui;padding:32px;text-align:center">' +
		'<h2>Site Photos</h2><p>Offline, and the app has not been opened on this device yet.</p>' +
		'<p style="color:#6b7785">Open it once with a connection and it will start without one afterwards.</p>',
		{ headers: { 'Content-Type': 'text/html; charset=utf-8' } });
}
