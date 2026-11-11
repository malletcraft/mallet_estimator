// Port 62 slice A2: S5 QUOTE's OWN ANSWERS to the site-name route, for :pano's S5Site.kt (the fixtures RenameRouteTest
// pins the phone's requests and verdicts to). It drives S5's router (functions/_lib/router.js) on its REAL SQL -- every
// migration, on SQLite, as S5's own "(SQL)" tests run it -- with a fake ERP answering whoami, and prints each answer and
// the body that drew it as Kotlin constants. Every name in it is invented: mallet_estimator is public.
//
//   cd <mcft-interior-estimate checkout, at the commit to pin>  &&  node <this file>  >  out.txt
//
// When S5 changes an answer, run it again against the new S5 and paste its output into S5Site.kt: a body there is never
// edited by hand. (Kept in the repository, not the session scratchpad: the rule of 3 Nov 2026.)
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
const { handle, d1Store } = await import(pathToFileURL(resolve('functions/_lib/router.js')).href);
const { sqliteD1 } = await import(pathToFileURL(resolve('tests/sqlite-d1.mjs')).href);

const ERP = 'https://erp.test';
const WHO = {
  'token staff:1': { user: 'staff@mcft.test', full_name: 'Staff', kind: 'staff', suppliers: [], customers: [] },
  'token lead:1': { user: 'lead@mcft.test', full_name: 'Lead', kind: 'staff', suppliers: [], customers: [] },
  'token vend:1': { user: 'v@vend.test', full_name: 'Vendor', kind: 'vendor', suppliers: ['SUP-1'], customers: [] },
  'token cli:1': { user: 'c@cli.test', full_name: 'Client', kind: 'client', suppliers: [], customers: ['CUST-1'] },
};
const jsonRes = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
global.fetch = async (url, opts = {}) => {
  const u = String(url); const h = opts.headers || {};
  if (u === ERP + '/api/method/mallet_estimator.api.whoami') { const who = h.authorization ? WHO[h.authorization] : null; return who ? jsonRes({ message: who }) : jsonRes({ exc_type: 'AuthenticationError' }, 401); }
  return jsonRes({}, 404);
};
const bucket = () => { const m = new Map(), meta = new Map(), up = new Map(); return {
  async put(k, v, o = {}) { m.set(k, v); meta.set(k, (o && o.customMetadata) || {}); up.set(k, new Date()); },
  async get(k) { if (!m.has(k)) return null; const b = m.get(k); return { body: b, customMetadata: meta.get(k) || {}, uploaded: up.get(k), async arrayBuffer() { return new Uint8Array(b).buffer; } }; },
  async head(k) { return m.has(k) ? { key: k, customMetadata: meta.get(k) || {}, uploaded: up.get(k) } : null; },
  async delete(ks) { [].concat(ks).forEach(k => { m.delete(k); meta.delete(k); up.delete(k); }); },
  async list({ prefix }) { return { objects: [...m.keys()].filter(k => k.startsWith(prefix)).map(key => ({ key, uploaded: up.get(key) })), truncated: false }; } }; };
const db = sqliteD1(); const st = d1Store(db);
const env = { APP_ENV: 'stage', ADMIN_TOKEN: 'secret', DB: db, ERP_URL: ERP, PHOTOS: bucket() };
const areq = (method, path, { auth, body } = {}) => new Request('https://x.test/api/' + path, { method,
  headers: { ...(auth ? { authorization: auth } : {}), ...(body !== undefined ? { 'content-type': 'application/json' } : {}) }, body: body === undefined ? undefined : (typeof body === 'string' ? body : JSON.stringify(body)) });
const surfaces = Object.fromEntries(['front', 'right', 'back', 'left', 'floor', 'ceil'].map(s => [s, { a: 3000, b: 2700, image: false }]));
const record = (project, room, capture, extra = {}) => ({ v: 1, project, project_title: 'Demo flat', customer: '', room, capture, captured_at: '2026-11-11T08:30:00+05:30', app_version: '0.3.320',
  L: 3319, W: 2678, H: 2677, prep: { v: 1, L: 3319, W: 2678, H: 2677, w: {}, actual: {} }, surfaces, work: 0, ...extra });
const out = [];
const say = (name, doc, text) => out.push(`    /** ${doc} */\n    const val ${name} = """${text}"""`);
async function upload(project, room, capture, extra) {
  const r = await handle(areq('PUT', `asis/${project}/${encodeURIComponent(room)}/c/${capture}`, { auth: 'token staff:1', body: record(project, room, capture, extra) }), env, st);
  if (r.status !== 200) throw new Error(`upload ${project}: ${r.status} ${await r.text()}`);
}
async function site(name, doc, project, body, auth = 'token staff:1', store = st) {
  const sent = typeof body === 'string' ? body : JSON.stringify(body);
  const r = await handle(areq('PUT', `asis/${project}/site`, { auth, body: sent }), env, store);
  const text = await r.text();
  say(name, `${r.status} · ${doc}`, text);
  say('SENT_' + name, `sent · PUT /api/asis/${project}/site`, sent);
  return r.status;
}
async function list(name, doc, auth = 'token staff:1') {
  const r = await handle(areq('GET', 'asis', { auth }), env, st); say(name, `${r.status} · ${doc}`, await r.text());
}
const tick = () => new Promise(r => setTimeout(r, 7));

await upload('PROJ-DEMO', 'Living Room', 'MCAP-000000000001'); await tick();
await upload('PROJ-TWO', 'Kitchen', 'MCAP-000000000002'); await tick();
await list('LIST_NONE', 'GET /api/asis before any site is named: site and site_at are blank on every row');
await site('NAMED_FIRST', 'the first name, base none (the seed, and a rename of a site S5 holds no name for)', 'PROJ-DEMO', { site: 'Demo Site, Pune', base: '' }); await tick();
await site('SAME', 'the same name again, whatever its base: the retry after a lost answer', 'PROJ-DEMO', { site: 'Demo Site, Pune', base: '' }); await tick();
await site('RENAMED', 'a rename with the name last seen as its base', 'PROJ-DEMO', { site: 'Demo Site, Kothrud', base: 'Demo Site, Pune' }); await tick();
await site('RENAMED_BY_LEAD', 'another staff login renames it meanwhile', 'PROJ-DEMO', { site: 'Demo Site, Baner', base: 'Demo Site, Kothrud' }, 'token lead:1'); await tick();
await site('STALE', 'a stale base: somebody renamed the site meanwhile; S5 wins', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Kothrud' }); await tick();
await site('STALE_NONE', 'a base, for a project S5 holds no name for', 'PROJ-TWO', { site: 'Demo Site, Aundh', base: 'Demo Site, Pune' }); await tick();
// the name moved away and back between the UPDATE and the re-read: S5's own handler, its store's UPDATE made to change nothing once
await site('STALE_AGAIN', 'the stored name is the base after all (it moved away and back): send it again', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Baner' }, 'token staff:1',
  new Proxy(st, { get: (t, k) => (k === 'setSite' ? async () => false : (typeof t[k] === 'function' ? t[k].bind(t) : t[k])) })); await tick();
await site('VENDOR', 'a vendor', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Baner' }, 'token vend:1');
await site('CLIENT', 'a client', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Baner' }, 'token cli:1');
await site('NEVER', 'a project S5 has never been sent', 'PROJ-NEVER', { site: 'Demo Site, Aundh', base: '' });
await site('BAD_NAME', 'a name over 140 characters', 'PROJ-DEMO', { site: 'D'.repeat(141), base: 'Demo Site, Baner' });
await site('BAD_BASE', 'no base', 'PROJ-DEMO', { site: 'Demo Site, Aundh' });
await site('NO_SIGN_IN', 'no sign-in at all', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Baner' }, null);
await site('KEY_REFUSED', 'a key ERP does not accept', 'PROJ-DEMO', { site: 'Demo Site, Aundh', base: 'Demo Site, Baner' }, 'token gone:9');
// the upload's first value: a record naming a site sets it only while S5 holds none
await upload('PROJ-UP', 'Guest Bedroom', 'MCAP-000000000003', { site: 'Demo Office, Pune' }); await tick();
await upload('PROJ-UP', 'Guest Bedroom', 'MCAP-000000000003', { site: 'Demo Office, Wakad' }); await tick();
await upload('PROJ-DEMO', 'Living Room', 'MCAP-000000000001', { site: 'Demo Site, Pune' }); await tick();
await list('LIST_NAMED', 'GET /api/asis afterwards: PROJ-DEMO as last renamed, PROJ-TWO still unnamed, PROJ-UP named by its first upload alone');
console.log(out.join('\n'));
