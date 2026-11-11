#!/usr/bin/env python3
# proto87: builds mock-up v62a (port 62 prototype items P0 + P2) from the published v61c.2 (Version 86).
# Every edit is an exact, counted replacement on the base page, so the build is repeatable and anything that moved fails loudly.
import json, bisect, hashlib, sys, datetime, zoneinfo

S = '/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad'
BASE = S + '/v61cbuild/index61c2.html'
OUT = S + '/proto87/index61c2.html'
BASE_MD5 = '5aa0a190210faa15822716293aa7c071'

src = open(BASE, encoding='utf-8').read()
assert hashlib.md5(src.encode('utf-8')).hexdigest() == BASE_MD5, 'the base is not Version 86'
EYE4 = 'phone APK <b>0.3.318</b> · <span style="white-space:nowrap;letter-spacing:0;font-size:min(1em,2.7vw)">carries the whole v61c port, survey tour included</span></span></div>'
assert src.count(EYE4) == 1

def rep(old, new, n=1):
    global src
    c = src.count(old)
    assert c == n, f'expected {n} of {old[:90]!r}, found {c}'
    src = src.replace(old, new)

STAMP = sys.argv[1] if len(sys.argv) > 1 else datetime.datetime.now(zoneinfo.ZoneInfo('Asia/Kolkata')).strftime('%d %b %Y, %H:%M IST')

# ---------------------------------------------------------------- the texts (EN, MR)
# for the phone, to approve
T_S5LINE = ('This site is on S5, so the new name is saved there and reaches the web and every other phone. Photos already filed under it keep their place — only the label changes.',
            'ही साइट S5 वर आहे, त्यामुळे नवीन नाव तिथे सेव्ह होते आणि वेबवर व इतर प्रत्येक फोनवर पोहोचते. आधीच त्याखाली ठेवलेले फोटो जागीच राहतात — फक्त नाव बदलते.')
T_OFFLINE = ('Offline — the name changes only when S5 answers; not saved', 'ऑफलाइन — S5 उत्तर देईल तेव्हाच नाव बदलते; सेव्ह झाले नाही')
T_SIGNED = ('Not signed in to S5, so not saved — sign in (menu → Server) and Save again', 'S5 मध्ये साइन इन केलेले नाही, म्हणून सेव्ह झाले नाही — साइन इन करा (मेनू → सर्व्हर) आणि पुन्हा सेव्ह करा')
T_RENAMED = ('Renamed by {by} meanwhile — now {site}; not saved', 'मधल्या काळात {by} यांनी नाव बदलले — आता {site}; सेव्ह झाले नाही')
T_PART = ('Renamed on {done} of {of} projects — Save again to finish', '{of} पैकी {done} प्रोजेक्टवर नाव बदलले — पूर्ण करण्यासाठी पुन्हा सेव्ह करा')
T_ONWAY = ('A capture of this site has not reached S5 yet — rename the site once it has; not saved', 'या साइटचे एक कॅप्चर अजून S5 वर पोहोचलेले नाही — ते पोहोचल्यावर साइटचे नाव बदला; सेव्ह झाले नाही')
T_BLOCKED = ('This site cannot be renamed from this phone: S5 does not hold it and it was not made here.', 'या फोनवरून या साइटचे नाव बदलता येत नाही: ती S5 वर नाही आणि इथे बनवलेलीही नाही.')
T_PLAN = ('Plan & elevations', 'प्लॅन व एलिव्हेशन')
T_NOPREP = ('no survey prep yet', 'अजून सर्व्हे तयारी नाही')
PHONE_S = [('app.site_on_s5_so_new', T_S5LINE, 'P0 (b) · the grey line of the rename dialog, for a site S5 holds'),
           ('refuse.site.offline', T_OFFLINE, 'P0 (a) · under the name field: no signal'),
           ('refuse.site.signed_out', T_SIGNED, 'P0 (a) · under the name field: signed out of S5'),
           ('refuse.site.capture_on_way', T_ONWAY, 'P0 (d) · under the name field: a capture of the site still on its way to S5'),
           ('app.site_not_renamed_here', T_BLOCKED, 'P0 (c) · in the grey line’s place: a site that cannot be renamed'),
           ('room.plan_elevations', T_PLAN, 'P2 · the room screen’s chip'),
           ('room.no_survey_prep_yet', T_NOPREP, 'P2 · the chip’s second part for a room with no survey prep: “Plan & elevations · no survey prep yet”')]
PHONE_T = [('refuse.site.renamed_x', T_RENAMED, {'by': 'w', 'site': 'w'}, 'P0 (a) · under the name field: S5’s own refusal (its 409), in S5’s words'),
           ('refuse.site.part_x', T_PART, {'done': 'd', 'of': 'd'}, 'P0 (a) · under the name field: a site with several projects on S5, not all renamed')]
# the prototype test row and its sheet: never on the phone
PROTO = [('proto.rn.test', 'Prototype test', 'प्रोटोटाइप चाचणी'),
         ('proto.rn.title', 'This rename — prototype test', 'हा नाव-बदल — प्रोटोटाइप चाचणी'),
         ('proto.rn.ok', 'Saves', 'सेव्ह होते'), ('proto.rn.ok.sup', 'the new name is saved', 'नवीन नाव सेव्ह होते'),
         ('proto.rn.offline', 'No signal', 'नेटवर्क नाही'), ('proto.rn.offline.sup', 'nothing reaches S5', 'S5 पर्यंत काहीही पोहोचत नाही'),
         ('proto.rn.signedout', 'Signed out of S5', 'S5 मधून साइन आउट'), ('proto.rn.signedout.sup', 'no S5 sign-in on this phone', 'या फोनवर S5 चे साइन इन नाही'),
         ('proto.rn.refused', 'S5 refuses', 'S5 नाकारते'), ('proto.rn.refused.sup', 'somebody renamed the site meanwhile', 'मधल्या काळात कोणीतरी साइटचे नाव बदलले'),
         ('proto.rn.partial', 'Lands on 1 project only', 'फक्त 1 प्रोजेक्टवर पोहोचते'), ('proto.rn.partial.sup', 'this site has more than one project on S5', 'या साइटचे S5 वर एकापेक्षा जास्त प्रोजेक्ट आहेत'),
         ('proto.rn.uploading', 'A capture still on its way', 'एक कॅप्चर अजून वाटेत'), ('proto.rn.uploading.sup', 'a 360 of this site has not reached S5 yet', 'या साइटचा एक 360 अजून S5 वर पोहोचलेला नाही'),
         ('proto.rn.blocked', 'Cannot be renamed here', 'इथे नाव बदलता येत नाही'), ('proto.rn.blocked.sup', 'not on S5 and not made on this phone', 'S5 वर नाही आणि या फोनवर बनवलेली नाही')]
P = dict((k, en) for k, en, mr in PROTO)

def js(s):   # a JS single-quoted string literal
    return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"

# ---------------------------------------------------------------- 1. the dictionary (RAW): keys inserted in key order, the rest byte for byte
i0 = src.index('const RAW=') + len('const RAW=')
raw, end = json.JSONDecoder().raw_decode(src[i0:])
assert json.dumps(raw, ensure_ascii=False, separators=(',', ':')) == src[i0:i0 + end], 'RAW does not round-trip'
known = {' '.join(e[1].split()) for sec in 'st' for e in raw[sec]}
def add(sec, row):
    keys = [e[0] for e in raw[sec]]
    assert row[0] not in keys, row[0]
    assert ' '.join(row[1].split()) not in known, 'English already a key: ' + row[1]
    raw[sec].insert(bisect.bisect_left(keys, row[0]), row)
for k, (en, mr), _ in PHONE_S: add('s', [k, en, mr])
for k, (en, mr), ph, _ in PHONE_T: add('t', [k, en, mr, ph])
for k, en, mr in PROTO: add('s', [k, en, mr])
for sec in 'stnk': assert [e[0] for e in raw[sec]] == sorted(e[0] for e in raw[sec])
src = src[:i0] + json.dumps(raw, ensure_ascii=False, separators=(',', ':')) + src[i0 + end:]

# ---------------------------------------------------------------- 2. CSS
rep('''.mr61n{text-align:right;white-space:nowrap}
''', '''.mr61n{text-align:right;white-space:nowrap}
/* ================= port 62 · P0 + P2 (mock-up v62a -- to approve, not on the phone). .m3 tokens only; no font-family =================
   P0 (c): a name that is shown, not edited -- the outlined field in the page's own disabled tones (.chip.dis, .tbtn[disabled]).
   P0 (a)'s line is .bs.errc under the field, P0's prototype test row is the album's .alb60as, and P2's chip is .chip.wq / .chip.dis:
   no rule of their own. */
.otf.dis .box{box-shadow:inset 0 0 0 1px rgba(29,27,32,.12)}
.otf.dis input,.otf.dis label{color:rgba(29,27,32,.38)}
.otf.dis input{-webkit-text-fill-color:rgba(29,27,32,.38);opacity:1}
''')

# ---------------------------------------------------------------- 3. the app shell: state
rep("showSettings:false, renaming:null, renameText:'', deleteBlocker:null, deleteFatal:false,",
    "showSettings:false, renaming:null, renameText:'', deleteBlocker:null, deleteFatal:false, rnTest:'ok', rnSheet:false,   /* port 62 · P0: the prototype test pick (memory only) and its sheet */")

# ---------------------------------------------------------------- 4. the rename dialog (P0)
OLD_RD = '''function renameDialog(){ const t=S.renaming, busy=!!S.renameBusy, changed=S.renameText.trim()&&S.renameText.trim()!==t.current, canDelete=t.kind!=='client', onServer=!!t.serverId;
  let body=field({k:'rn',label:t.kind[0].toUpperCase()+t.kind.slice(1)+' name',value:S.renameText,set:v=>S.renameText=v})+`<div style="height:10px"></div>`+
    `<div class="bs mut">${onServer?`This ${t.kind} is on the server, so the new name reaches ERP and every other device. Photos already filed under it keep their place — only the label changes.`:`This ${t.kind} has not synced yet, so this is a change on this phone. It will go up under the corrected name.`}</div>`;
'''
NEW_RD = '''// ================= port 62 · P0 (mock-up v62a -- TO APPROVE, not on the phone; android/docs/port62/DESIGN-port62.md §2) =================
// Amit, 11 Nov 2026: "Yes — S5 keeps the site name". A SITE renames through S5, and a rename that cannot be saved says why in
// a line under the name field while the dialog STAYS OPEN: set on failure, cleared on edit and on dismiss, nothing renamed.
// (a) the line and its words: no signal · signed out of S5 · S5's own refusal, in S5's words · a site with several projects
//     on S5 of which not all took the name ("Save again to finish");
// (b) the grey line for a site S5 holds (no "ERP");
// (c) a site that cannot be renamed here: the name shown, not edited, Save off, the reason in the grey line's place;
// (d) a site with a capture still on its way to S5 (R9): refused until it has landed.
// The prototype has no S5 and no signal to lose, so a site's dialog ends with a PROTOTYPE TEST row, as the album's
// "Viewing as" is: it picks what THIS rename meets (S.rnTest, memory only). The row never ships. A client's and a project's
// dialog, the delete half of every dialog and everything under the v53 lens are v61c.2 byte for byte.
const RN62=[['ok',__OK__,__OKS__],['offline',__OFF__,__OFFS__],['signedout',__SO__,__SOS__],['refused',__RF__,__RFS__],
  ['partial',__PA__,__PAS__],['uploading',__UP__,__UPS__],['blocked',__BL__,__BLS__]];
const RN62WHY={offline:__W_OFF__,
  signedout:__W_SO__,
  uploading:__W_UP__,
  blocked:__W_BL__};
const RN62S5=__S5LINE__;
const RN62BY='Priya Shinde', RN62NOW='Demo Site, Baner';   // invented, like every demo name on the page: who renamed it meanwhile, and to what
const rn62on=t=>!!t&&t.kind==='site'&&!LENS();
const rn62n=t=>projectsOf(t.client,t.current).filter(p=>p.serverId&&!p.local).length;   // the site's projects S5 lists: one write each
// a site that is not on S5 renames on the phone: there is no S5 answer to lose, so only three picks apply
const rn62opts=t=>RN62.filter(([id])=>t.serverId?(id!=='partial'||rn62n(t)>1):id==='ok'||id==='uploading'||id==='blocked');
const rn62mode=t=>rn62opts(t).some(o=>o[0]===S.rnTest)?S.rnTest:'ok';
// where the dialog is short (landscape) its body scrolls: a reason, or a new pick, is shown from the top, with the name field
const rn62Top=()=>setTimeout(()=>{ const v=document.querySelector('#ov [data-sk="pg:dlg-rename"]'); if(v) v.scrollTop=0; },0);
function rn62Sheet(t){ const cur=rn62mode(t);
  return sheet('rn62',sheetTitle(__TITLE__)+rn62opts(t).map(([id,head,sup])=>li({head:esc(head),sup:esc(sup),sel:id===cur,onClick:()=>{ S.rnTest=id; S.rnSheet=false; t.why=null; t.whyId=null; t.hit=false; if(id==='blocked') S.renameText=t.current; rn62Top(); }})).join('')+'<div style="height:12px"></div>',()=>{ S.rnSheet=false; }); }
function renameDialog(){ const t=S.renaming, busy=!!S.renameBusy, changed=S.renameText.trim()&&S.renameText.trim()!==t.current, canDelete=t.kind!=='client', onServer=!!t.serverId;
  const x62=rn62on(t), mode=x62?rn62mode(t):'ok', blocked=mode==='blocked';
  let body=field({k:'rn',label:t.kind[0].toUpperCase()+t.kind.slice(1)+' name',value:S.renameText,err:!!t.why,set:v=>{ S.renameText=v; t.why=null; t.whyId=null; }});
  if(blocked) body=body.replace('<div class="otf ','<div class="otf dis ').replace('<input ','<input disabled ');   // (c)
  if(t.why) body+=`<div class="bs errc" role="alert" data-rn62why="${esc(t.whyId||'')}" style="padding:4px 16px 0">${esc(t.why)}</div>`;   // (a) · (d)
  body+=`<div style="height:10px"></div>`+
    `<div class="bs mut"${x62?` data-rn62line="${blocked?'blocked':onServer?'s5':'local'}"`:''}>${blocked?esc(RN62WHY.blocked):onServer&&x62?esc(RN62S5):onServer?`This ${t.kind} is on the server, so the new name reaches ERP and every other device. Photos already filed under it keep their place — only the label changes.`:`This ${t.kind} has not synced yet, so this is a change on this phone. It will go up under the corrected name.`}</div>`;
'''
subs = {'__OK__': P['proto.rn.ok'], '__OKS__': P['proto.rn.ok.sup'], '__OFF__': P['proto.rn.offline'], '__OFFS__': P['proto.rn.offline.sup'],
        '__SO__': P['proto.rn.signedout'], '__SOS__': P['proto.rn.signedout.sup'], '__RF__': P['proto.rn.refused'], '__RFS__': P['proto.rn.refused.sup'],
        '__PA__': P['proto.rn.partial'], '__PAS__': P['proto.rn.partial.sup'], '__UP__': P['proto.rn.uploading'], '__UPS__': P['proto.rn.uploading.sup'],
        '__BL__': P['proto.rn.blocked'], '__BLS__': P['proto.rn.blocked.sup'], '__W_OFF__': T_OFFLINE[0], '__W_SO__': T_SIGNED[0], '__W_UP__': T_ONWAY[0],
        '__W_BL__': T_BLOCKED[0], '__S5LINE__': T_S5LINE[0], '__TITLE__': P['proto.rn.title']}
for k, v in subs.items():
    assert NEW_RD.count(k) == 1, k
    NEW_RD = NEW_RD.replace(k, js(v))
rep(OLD_RD, NEW_RD)

OLD_RET = '''  return `<div style="position:absolute;inset:0;background:#FAFAFA"></div>`+dialog({id:'rename',title:canDelete?`Rename or remove ${t.kind}`:`Rename ${t.kind}`,body,onDismiss:()=>{ if(!busy) set({renaming:null,deleteBlocker:null,deleteFatal:false}); },
    dismiss:tbtn('Cancel',()=>set({renaming:null,deleteBlocker:null,deleteFatal:false}),busy),confirm:tbtn(busy?'Saving…':'Save',()=>doRename(t,S.renameText.trim()),!changed||busy)}); }
function doRename(t,n){ const go=()=>{'''
NEW_RET = '''  // port 62 · P0: the prototype test row (the album's "Viewing as" bar) -- what this rename meets; never on the phone
  if(x62) body+=`<div style="height:12px"></div><button class="alb60as rip" data-rn62as="${mode}" aria-haspopup="dialog"${B(()=>{ S.rnSheet=true; },busy)}><span class="ls" style="white-space:normal">${esc(__TEST__+' · '+(RN62.find(o=>o[0]===mode)||RN62[0])[1])}</span><span class="alb59car" aria-hidden="true">▾</span></button>`;
  return `<div style="position:absolute;inset:0;background:#FAFAFA"></div>`+dialog({id:'rename',title:canDelete?`Rename or remove ${t.kind}`:`Rename ${t.kind}`,body,onDismiss:()=>{ if(!busy) set({renaming:null,deleteBlocker:null,deleteFatal:false}); },
    dismiss:tbtn('Cancel',()=>set({renaming:null,deleteBlocker:null,deleteFatal:false}),busy),confirm:tbtn(busy?'Saving…':'Save',()=>doRename(t,S.renameText.trim()),!changed||busy||blocked)})+(x62&&S.rnSheet?rn62Sheet(t):''); }
// port 62 · P0: what a SITE's Save meets. Signed out, and a capture on its way for a site that is not on S5, are known on the
// phone: said at once, nothing sent. Everything else is S5's answer, after the same short pause a rename always had. A refusal
// and a part-rename happen once per dialog: the next Save goes through (S5's answer carried the name to send against; the
// projects left over take theirs).
function rn62Save(t,n,go){ const mode=rn62mode(t), s5=!!t.serverId, fail=(id,why)=>{ t.why=why; t.whyId=id; S.renameBusy=false; render(); rn62Top(); };
  if(mode==='blocked') return;
  if(mode==='signedout'&&s5) return fail(mode,RN62WHY.signedout);
  if(!s5) return mode==='uploading'?fail(mode,RN62WHY.uploading):go();
  t.why=null; t.whyId=null; S.renameBusy=true; render();
  setTimeout(()=>{ if(S.renaming!==t) return;
    if(mode==='offline') return fail(mode,RN62WHY.offline);
    if(mode==='uploading') return fail(mode,RN62WHY.uploading);   // S5's list is read first: the capture's project is still not on it
    if(mode==='refused'&&!t.hit){ t.hit=true; return fail(mode,`Renamed by ${RN62BY} meanwhile — now ${RN62NOW}; not saved`); }
    if(mode==='partial'&&!t.hit){ t.hit=true; return fail(mode,`Renamed on 1 of ${rn62n(t)} projects — Save again to finish`); }
    go(); },700); }
function doRename(t,n){ const go=()=>{'''
assert NEW_RET.count('__TEST__') == 1
NEW_RET = NEW_RET.replace('__TEST__', js(P['proto.rn.test']))
rep(OLD_RET, NEW_RET)

rep('''    S.lastResult=`Renamed to ${n}`; S.renaming=null; S.renameBusy=false; render(); };
  if(!t.serverId) go(); else { S.renameBusy=true; render(); setTimeout(go,700); } }''',
    '''    S.lastResult=`Renamed to ${n}`; S.renaming=null; S.renameBusy=false; render(); };
  if(rn62on(t)) return rn62Save(t,n,go);   // port 62 · P0: a site's Save can fail, and says why
  if(!t.serverId) go(); else { S.renameBusy=true; render(); setTimeout(go,700); } }''')

# Android Back: the prototype test sheet closes first
rep("  if(S.renaming){ if(!S.renameBusy){ S.renaming=null; S.deleteBlocker=null; S.deleteFatal=false; } return render(); }",
    "  if(S.renaming){ if(S.rnSheet){ S.rnSheet=false; return render(); }   /* port 62 · P0: the prototype test sheet closes first */ if(!S.renameBusy){ S.renaming=null; S.deleteBlocker=null; S.deleteFatal=false; } return render(); }")

# ---------------------------------------------------------------- 5. the room screen (P2)
OLD_RS = '''function roomScreen(){ const room=S.navRoom, p=S.navProject, cards=roomCards();'''
NEW_RS = '''// ================= port 62 · P2 (mock-up v62a -- TO APPROVE, not on the phone; android/docs/port62/DESIGN-port62.md §4) =================
// Amit, 11 Nov 2026: "Found it, but it should be on the room". The room screen opens the room's floor plan and elevations
// in ONE tap: survey prep's own Plan tab (Floor · Ceiling · Front · Right · Back · Left), nothing new drawn, for the room's
// CURRENT capture -- the newest 360 on this phone that has a survey prep (S.queue is newest first). The chip leads the room
// screen in every phase (Capture 360 is a Survey tool and hides); a room with no survey prep says so on the chip itself, and
// that chip is not a tap. Nothing under the v53 lens.
const size62=z=>Array.isArray(z)&&z.length===3&&z.every(v=>v>=300&&v<=30000)?z:null;   // the sizes survey prep opens with (openSurveyFor's own range)
function planCap62(p,room){ const sp=spApi(); if(!sp||!p) return null;
  for(const q of S.queue){ if(q.kind!=='360'||q.room!==room||key(q.projectTitle)!==key(p.title)||!q.pano) continue;
    const z=sp.summary(q.deviceId,docOf(q),q.s5); if(z&&(size62(z.size)||size62(q.size))) return q; }
  return null; }
function openPlan62(row){ if(!size62(row.size)){ const z=spApi().summary(row.deviceId,docOf(row),row.s5); row.size=size62(z&&z.size).slice(); }   // the prep's own L · W · H, as surveySize hands the row
  openSurvey(row.deviceId,{tab:'plan'}); }
function planRow62(p,room){ if(LENS()||!p) return ''; const row=planCap62(p,room);
  return `<div class="hsc chips" data-sk="rmchips" style="flex:none">`+(row?chip(__PLAN__,{f:false,cls:'wq',onClick:()=>openPlan62(row)}).replace('<button ','<button data-rp62="open" ')
    :chip(__PLAN__+' · '+__NOPREP__,{f:false,dis:true}).replace('<button ','<button data-rp62="none" aria-disabled="true" '))+`</div>`; }
function roomScreen(){ const room=S.navRoom, p=S.navProject, cards=roomCards();'''
assert NEW_RS.count('__PLAN__') == 2 and NEW_RS.count('__NOPREP__') == 1
NEW_RS = NEW_RS.replace('__PLAN__', js(T_PLAN[0])).replace('__NOPREP__', js(T_NOPREP[0]))
rep(OLD_RS, NEW_RS)
rep('''  if(S.lastResult) body+=`<div class="rip bs" style="background:var(--scn);color:var(--onscn);padding:9px 16px;white-space:pre-line;flex:none"${A(()=>{ S.lastResult=null; })}>${esc(S.lastResult)}</div>`;
  if(!cards.length) body+=`<div class="bm mut" style="padding:24px">No captures in this room yet. Use Capture 360 below.</div>`;''',
    '''  if(S.lastResult) body+=`<div class="rip bs" style="background:var(--scn);color:var(--onscn);padding:9px 16px;white-space:pre-line;flex:none"${A(()=>{ S.lastResult=null; })}>${esc(S.lastResult)}</div>`;
  body+=planRow62(p,room);   // port 62 · P2: Plan & elevations, one tap from the room
  if(!cards.length) body+=`<div class="bm mut" style="padding:24px">No captures in this room yet. Use Capture 360 below.</div>`;''')
# survey prep can be asked to open on its Plan tab
rep("function openSurvey(id,{pending}){ const row=pending?S.pending:capOf(id); S.survey={id,pending:!!pending}; S.drawer=false;",
    "function openSurvey(id,{pending,tab}){ const row=pending?S.pending:capOf(id); S.survey={id,pending:!!pending}; S.drawer=false;   // port 62 · P2: tab 'plan' opens on the six drawings")
rep("title:rlabel(row.room),L:row.size[0],W:row.size[1],H:row.size[2],discard:!!pending,meta:{",
    "title:rlabel(row.room),L:row.size[0],W:row.size[1],H:row.size[2],discard:!!pending,tab:tab||null,meta:{")
# sp.js: open(o) takes o.tab
OLD_OPEN = "st.tool='sel'; st.more=false; toolsFor=null; st.ctx=null; st.swipeOpen=null; hideSnack(); clrArm=null;\n  Object.keys(rect).forEach(k=>delete rect[k]); surfV.img=null; savedMsg('');\n  usePano(o.pano||'pano.jpg');"
NEW_OPEN = "st.tool='sel'; st.more=false; toolsFor=null; st.ctx=null; st.swipeOpen=null; hideSnack(); clrArm=null;\n  if(o.tab==='plan'){ st.tab='plan'; PLAN.view='floor'; PLAN.fitted=true; PLAN.vb=null; $('planx').hidden=true; }   // port 62 · P2: the room screen's Plan & elevations opens here, on the Floor drawing, fitted\n  Object.keys(rect).forEach(k=>delete rect[k]); surfV.img=null; savedMsg('');\n  usePano(o.pano||'pano.jpg');"
rep(OLD_OPEN, NEW_OPEN)

# ---------------------------------------------------------------- 6. the page around the phone: version line, notes
rep('<span class="eyeln"><b>mock-up v61c.2</b></span><span class="eysep"> · </span><span class="eyeln">10 Nov 2026, 11:44 IST</span>',
    f'<span class="eyeln"><b>mock-up v62a</b></span><span class="eysep"> · </span><span class="eyeln">{STAMP}</span>')
rep(' · 16 Oct: feedback is one action · v61c: one project state</div>',
    ' · 16 Oct: feedback is one action · v61c: one project state · v62a: a failed site rename says why + Plan &amp; elevations on the room · not yet approved, not on the phone</div>')
rep('sync, rename, delete and Import from ImageMeter answer after a short pause with nothing sent anywhere.',
    'sync, rename, delete and Import from ImageMeter answer after a short pause with nothing sent anywhere; a site’s rename can be made to fail from the <b>Prototype test</b> row at the foot of its dialog (v62a).')
rep('<li>Hold a client, site or project → <b>Rename</b> (and remove, for a site or project). <b>New client / site / project</b> → the New site dialog.</li>',
    '<li>Hold a client, site or project → <b>Rename</b> (and remove, for a site or project); a site’s dialog ends with the <b>Prototype test</b> row, which picks what its Save meets (v62a). <b>New client / site / project</b> → the New site dialog.</li>')
rep('<li>A room: <b>Capture 360</b> → the capture sheet (X3, gallery 360, single photo);',
    '<li>A room: <b>Plan &amp; elevations</b> → survey prep’s Plan tab for the room’s newest 360 with a survey prep, ← back to the room (v62a); <b>Capture 360</b> → the capture sheet (X3, gallery 360, single photo);')

def h(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
def row(k, en, mr, where): return f'<tr><td>{h(en)}<div class="mr61k"><code>{k}</code></div></td><td lang="mr" class="mr61m">{h(mr)}</td><td class="mr61x">{h(where)}</td></tr>'
phone_rows = ''.join([row(k, en, mr, w) for k, (en, mr), w in PHONE_S[:1]] +
                     [row(k, en, mr, w) for k, (en, mr), w in PHONE_S[1:3]] +
                     [row(k, en, mr, w) for k, (en, mr), ph, w in PHONE_T] +
                     [row(k, en, mr, w) for k, (en, mr), w in PHONE_S[3:]])
proto_rows = ''.join(row(k, en, mr, 'the Prototype test row and its sheet') for k, en, mr in PROTO)
NPHONE, NPROTO = len(PHONE_S) + len(PHONE_T), len(PROTO)
CARD = f'''      <div class="card navmap" id="ch62a">
        <h2>Changed from v61c.2 — to approve</h2>
        <p style="margin:0 0 8px">v62a draws the two prototype items of port 62, P0 and P2, before anything is built for the phone. Amit, 11 Nov 2026, on the site name: “Yes — S5 keeps the site name”; and on the room's plan: “Found it, but it should be on the room”. <b>Neither is approved yet, and neither is on the phone:</b> APK 0.3.318 still sends a site's rename the old way and says nothing when it fails, and it still opens a room's plan only from inside a 360's survey prep.</p>
        <ul>
          <li><b>P0 · A rename that cannot be saved says why, and the dialog stays open.</b> Hold a site → Rename or remove site. When Save cannot be done, the reason shows in red directly under the name field and the field is outlined in red; nothing is renamed and the dialog stays open. Typing in the field, or closing the dialog, clears the line. Four reasons are drawn: no signal; signed out of S5; S5's own refusal, in S5's words (somebody renamed the site meanwhile); and a site with several projects on S5 of which not all took the name — “Renamed on 1 of 2 projects — Save again to finish”.</li>
          <li><b>P0 · The grey line for a site S5 holds no longer says ERP.</b> It reads “This site is on S5, so the new name is saved there and reaches the web and every other phone. Photos already filed under it keep their place — only the label changes.” A site that has not synced keeps its own line. A client's and a project's dialog are unchanged.</li>
          <li><b>P0 · A site that cannot be renamed here.</b> A site S5 does not hold and this phone did not make (a site only an old ERP sync named, for one) opens the same dialog with its name shown but not editable, Save off, and the reason in the grey line's place. The other choice — offering no rename at all on such a row — is not drawn: a hold that does nothing reads as a broken row.</li>
          <li><b>P0 · A site with a capture still on its way to S5</b> is not renamed until that capture has landed: Save answers “A capture of this site has not reached S5 yet — rename the site once it has; not saved”.</li>
          <li><b>P0 · The Prototype test row (it never ships).</b> The prototype has no S5 and no signal to lose, so a site's rename dialog ends with a <b>Prototype test</b> row, as the album has Viewing as. It picks what this rename meets: Saves · No signal · Signed out of S5 · S5 refuses · Lands on 1 project only · A capture still on its way · Cannot be renamed here. “Lands on 1 project only” is offered where the site has two projects on S5 (Demo Site, Pune). A site that is not on S5 (Demo Office, Pune) renames on the phone alone, so it offers only Saves, A capture still on its way and Cannot be renamed here. A refusal and a part-rename happen once: Save again then goes through. The pick lasts until the page is reloaded. In landscape the dialog is short, so the row is reached by scrolling the dialog.</li>
          <li><b>P2 · The room's plan opens from the room.</b> Every room screen now leads with a <b>Plan &amp; elevations</b> chip, under the crumbs and above the work chips. One tap opens the room's floor plan — survey prep's own Plan tab with its Floor, Ceiling, Front, Right, Back and Left chips, fit, pinch and Export. Nothing new is drawn. It opens the room's current capture: the newest 360 on this phone that has a survey prep. ← comes back to the room.</li>
          <li><b>P2 · A room with no survey prep yet</b> shows the same chip greyed, reading “Plan &amp; elevations · no survey prep yet”, and it is not a tap: a room with no 360, a 360 whose survey prep has not been started, or a 360 that is no longer on this phone. The chip is there in every phase, also where Capture 360 is hidden.</li>
          <li><b>Kotlin needs</b> (after approval; slices A2 and PL of <code>android/docs/port62/DESIGN-port62.md</code>): the error line under the rename field (set on failure, cleared on edit and dismiss) with these words, the S5 grey line, the not-editable form, and the chip on the room screen opening survey prep on its Plan tab. In landscape (780 × 360) the site's dialog has no room to spare: its body fills its space exactly today, so the reason line makes it taller than that space — by 20 dp for one line, which is what each of the five reasons takes there in English and in Marathi, and by more when a long name wraps it. So the dialog's body must scroll, and it comes back to the top, with the name field and the reason in view, each time a reason is set — as this page does.</li>
          <li><b>S5 QUOTE needs</b> (slice A1, on stage before the phone build): the site name kept on each project and the guarded <code>PUT …/site</code>, whose 409 is the “Renamed by … meanwhile” line above. P2 needs nothing from S5. <b>S1 MEST needs: nothing.</b></li>
        </ul>
        <h3 id="ch62a-tx">The new texts · {NPHONE} for the phone, to approve as a whole</h3>
        <p style="margin:0 0 6px">English and Marathi, as the phone would show them. Marathi keeps Latin digits and the name S5. <code>{{by}}</code> and <code>{{site}}</code> are S5's own values (who renamed it, and the name it holds now); <code>{{done}}</code> and <code>{{of}}</code> are counts.</p>
        <details class="mr61d" open><summary><b>Rename dialog and room screen</b> · {NPHONE}</summary><div class="mr61w"><table class="mr61t"><thead><tr><th>English</th><th>मराठी</th><th>where</th></tr></thead><tbody>{phone_rows}</tbody></table></div></details>
        <details class="mr61d"><summary><b>The Prototype test row (never on the phone)</b> · {NPROTO}</summary><div class="mr61w"><table class="mr61t"><thead><tr><th>English</th><th>मराठी</th><th>where</th></tr></thead><tbody>{proto_rows}</tbody></table></div></details>
      </div>
'''
rep('''      <div class="card navmap" id="ch61c">
        <h2>Changed from v61b — to approve</h2>''', CARD + '''      <div class="card navmap" id="ch61c">
        <h2>Changed from v61b — to approve</h2>''')

assert src.count(EYE4) == 1, 'the 4th eyebrow line moved'
open(OUT, 'w', encoding='utf-8').write(src)
print('built', OUT, hashlib.md5(src.encode('utf-8')).hexdigest(), len(src.splitlines()), 'lines · stamp', STAMP)
