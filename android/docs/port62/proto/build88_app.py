# proto88 · build88.py's second part: app.js (the shell). exec'd by build88.py: `src`, rep(), cut() and js() are its.

# ================================================================ 6. the demo, re-seeded per room
# Demo flat interiors is in execution (approved 12 Oct 2026); Demo bathroom door is at Revisions, with two surveyed rooms
rep('''title:'Demo flat interiors',serverId:'PROJ-DEMO',clientId:'CUST-DEMO-01',jobType:NEW,stage:'Plaster & waterproofing',status:'Open',start:'2026-09-01',end:'2026-12-20',stageSince:'2026-10-06',local:false,quote:'Survey',quoteSince:Q0},''',
    '''title:'Demo flat interiors',serverId:'PROJ-DEMO',clientId:'CUST-DEMO-01',jobType:NEW,stage:'Plaster & waterproofing',status:'Open',start:'2026-09-01',end:'2026-12-20',stageSince:'2026-10-06',local:false,quote:'Client approved',quoteSince:'2026-10-12T18:00:00+05:30'},   // v62b: in execution -- its rooms, scopes, states and photos are ALB60.SEED's''')
rep('''title:'Demo bathroom door',serverId:'PROJ-DEMO-2',clientId:'CUST-DEMO-01',jobType:INS,stage:'Opening measured',status:'Open',start:'2026-10-05',end:'',stageSince:'2026-10-05',local:false,quote:'Survey',quoteSince:Q0},''',
    '''title:'Demo bathroom door',serverId:'PROJ-DEMO-2',clientId:'CUST-DEMO-01',jobType:INS,stage:'Opening measured',status:'Open',start:'2026-10-05',end:'',stageSince:'2026-10-05',local:false,quote:'Revisions',quoteSince:'2026-11-08T11:30:00+05:30'},   // v62b: not approved yet -- Approve is done here, room by room''')
rep('''title:'Sample wardrobe repair',serverId:'PROJ-DEMO-3',clientId:'CUST-DEMO-02',jobType:REP,stage:'Defect recorded',status:'Open',start:'2026-10-02',end:'2026-10-20',stageSince:'2026-10-02',local:false,quote:'Survey',quoteSince:Q0}],''',
    '''title:'Sample wardrobe repair',serverId:'PROJ-DEMO-3',clientId:'CUST-DEMO-02',jobType:REP,stage:'Defect recorded',status:'Open',start:'2026-10-02',end:'2026-10-20',stageSince:'2026-10-02',local:false,quote:'Client approved',quoteSince:'2026-10-20T17:30:00+05:30'}],   // v62b: approved BEFORE the scope was per room -- "Record the approved scope" is walked here''')
rep('''  scope:{[siteKey('Demo Client','Demo Site, Pune')]:['Guest Bedroom','Kitchen','Living Room']},''',
    '''  scope:{[siteKey('Demo Client','Demo Site, Pune')]:['Guest Bedroom','Kitchen','Living Room','Master Bedroom','Passage','Toilet 1']},   // v62b: the flat's rooms; Passage is the one S5 holds no survey of''')
rep('''    {deviceId:'MCAP-9e2f4a6b1c08',projectTitle:'Sample wardrobe repair',''',
    '''    {deviceId:'MCAP-62e6d1f08b42',projectTitle:'Demo bathroom door',customerName:'Demo Client',room:'Bathroom',stage:'Door & window frames (chowkat)',captureDate:'2026-10-08',kind:'Photo',state:'SYNCED',serverName:'MEST-PH-2026-00409',pano:'photo:44% 52%',sku:''},   // v62b: a photo an older build tagged with a worksite stage -- it files at that stage's work (Civil & masonry) BEFORE approval, as FW2's sweep files it
    {deviceId:'MCAP-62b4d09a1e55',projectTitle:'Demo bathroom door',customerName:'Demo Client',room:'Bathroom',stage:'Opening measured',captureDate:'2026-10-06',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00402',pano:'360',size:[2100,1500,2400],sku:''},   // v62b
    {deviceId:'MCAP-62b7e3c5f018',projectTitle:'Demo bathroom door',customerName:'Demo Client',room:'Passage',stage:'Opening measured',captureDate:'2026-10-06',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00403',pano:'360',size:[3600,1100,2700],sku:''},   // v62b
    {deviceId:'MCAP-9e2f4a6b1c08',projectTitle:'Sample wardrobe repair',''')
rep('''pano:'photo:mb',sku:'MEST-SKU-00904'}],''',
    '''pano:'photo:mb',sku:'MEST-SKU-00904'},
    {deviceId:'MCAP-62d5e8d47a53',projectTitle:'Sample wardrobe repair',customerName:'Sample Customer',room:'Master Bedroom',stage:'As-built site measure',captureDate:'2026-10-02',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00388',pano:'360',size:[3900,3300,2850],sku:''}],   // v62b: surveyed (PREP62)''')
rep('''  wishFor:null, planFor:null, laserState:'OFF',''', '''  wishFor:null, ovFrom62:null, laserState:'OFF',''')   # v62b: ovFrom62 = the overview a room was opened from ({pk, view, log}), for Android Back

# ================================================================ 7. the kits: no arranger; the room module's own
cut('''// v54 D: the Floor plan arranger (arrange54.js) is a project-level page, built with this file's kit on first use
''', '''window.__appPlanReady=()=>{ if(fplan()) render(); };
''', '')
rep('''// v60: the Project Album by works (album60, SPEC60) -- four roles and vendor review; built with this file's kit on first use; null under the v53 lens
let ALw=null; const album=()=>ALw||(window.__mcftAlbum60&&window.ALB60&&window.ALB60.on()?(ALw=window.__mcftAlbum60({S,A:fn=>A(fn),esc,scaffold:o=>scaffold(o),PG:(k,i,o)=>PG(k,i,o),
  field:o=>field(o),sheet:(id,h,d)=>sheet(id,h,d),sheetTitle:t=>sheetTitle(t),li:o=>li(o),ic:(n,z,c)=>ic(n,z,c),thumbBg:x=>thumbBg(x),
  roomsOf:p=>{ const has=r=>S.queue.some(q=>q.room===r&&key(q.projectTitle)===key(p.title)); return roomsInScope(p.client,p.site,siteTypeOf(p.client,p.site),has); },
  today:()=>today(),day:x=>day(x),plural:(n,w)=>plural(n,w),x56:p=>x56(p),render:()=>render()})):null);''',
    '''// v62b: the room's home (room62b, room-centred-album.md §3) -- the room's works, their states, change requests and photos; the
// project screen's two rows; four roles and vendor review. Built with this file's kit on first use; null under the v53 lens.
// openCapture: a capture of this phone drawn as a card under its work opens its own screen (its WORK line re-files it).
let ALw=null; const album=()=>ALw||(window.__mcftAlbum60&&window.ALB60&&window.ALB60.on()?(ALw=window.__mcftAlbum60({S,A:fn=>A(fn),esc,scaffold:o=>scaffold(o),PG:(k,i,o)=>PG(k,i,o),
  field:o=>field(o),sheet:(id,h,d)=>sheet(id,h,d),sheetTitle:t=>sheetTitle(t),li:o=>li(o),ic:(n,z,c)=>ic(n,z,c),thumbBg:x=>thumbBg(x),
  openCapture:id=>{ const c=roomCards().find(x=>x.deviceId===id); if(c){ S.navCapture=c; S.navFace=null; } },
  tok:r=>tok(r),rlabel:r=>rlabel(r),order:L=>order62(L),chip:(l,o)=>chip(l,o),
  today:()=>today(),day:x=>day(x),plural:(n,w)=>plural(n,w),x56:p=>x56(p),render:()=>render()})):null);''')
# a new capture files at the work picked for THIS room, else the room's first approved work In progress
rep('''const newWork=p=>{ if(!approved(p)) return 0; const m=S.capWorkFor||(S.capWorkFor={}), k=pk61(p); if(Object.prototype.hasOwnProperty.call(m,k)) return m[k]; const ow=window.ALB60.openWorks(p); return ow.length?ow[0]:null; };''',
    '''// v62b: per ROOM -- the work picked for this room, else the room's first approved work In progress, else null
const wk62=(p,room)=>pk61(p)+'|'+(room==null?S.navRoom:room);
const newWork=(p,room)=>{ if(!approved(p)) return 0; room=room==null?S.navRoom:room; const m=S.capWorkFor||(S.capWorkFor={}), k=wk62(p,room); if(Object.prototype.hasOwnProperty.call(m,k)) return m[k]; const ow=window.ALB60.openWorks(p,room); return ow.length?ow[0]:null; };''')
rep('''if(window.ALB60) window.ALB60.bind({x56});   // v60: the album needs only x56 (the camera's quote-journey lock); states and shots are read at call time, nothing is restored into S''',
    '''// v62b: the core reads x56 (the execution test), this phone's captures, and whether a 360 has a survey prep saved (the capture
// record that makes a room known to S5); everything is read at call time, nothing is restored into S
if(window.ALB60) window.ALB60.bind({x56,queue:()=>S.queue,hasPrep:q=>{ const sp=spApi(); return !!(sp&&q&&q.pano&&sp.summary(q.deviceId,docOf(q),q.s5)); }});
// v62b (review 1): THE DEMO'S SURVEYED ROOMS. Each 360 named here carries its survey prep AS SAVED: its marks by surface (mm on
// that surface: x from the left corner, y from the floor) and its laser readings. Seeded once into survey prep's memory
// (__sp.seed62), so the room screen, the Plan & elevations chip, the phase strip, the scope sheet's hint and survey prep itself
// all read the same prep. A mark past Existing carries its state as the phone stores it (life, lifeAt, lifeAs, born, act).
const mk62=(id,t,code,x0,x1,y0,y1,o)=>Object.assign({id,t,code,x0,x1,y0,y1},o||{});
const SC62={life:'scope',lifeAt:'2026-11-07T17:10:00+05:30',lifeAs:'staff'};
const PREP62={
  'MCAP-62d1c4a80e31':{els:{front:[mk62('s62a01','W','FW1',1400,3800,900,2250)],back:[mk62('s62a02','D','BD1',300,1300,0,2100)],right:[mk62('s62a03','S','RS1',200,400,1100,1190)],left:[mk62('s62a04','N','LN1',1200,2400,600,1800)]},
    actual:{'FW1.w':2400,'FW1.h':1350,'BD1.w':1000,'BD1.h':2100}},                                                    // Living Room
  'MCAP-62d2f7b19c64':{els:{front:[mk62('s62b01','W','FW1',900,2100,1050,2100)],back:[mk62('s62b02','D','BD1',200,1100,0,2100)],left:[mk62('s62b03','L','LL1',0,2700,2100,2850)]},
    actual:{'FW1.w':1200,'FW1.h':1050,'BD1.w':900}},                                                                  // Kitchen
  'MCAP-62d3a0e52b97':{els:{front:[mk62('s62c01','W','FW1',700,1300,1800,2250)],back:[mk62('s62c02','D','BD1',650,1400,0,2100)]},
    actual:{'FW1.w':600,'BD1.w':750,'BD1.h':2100}},                                                                   // Toilet 1
  'MCAP-62d4b9c36f20':{els:{front:[mk62('s62d01','W','FW1',1200,3000,900,2250)],back:[mk62('s62d02','D','BD1',300,1200,0,2100)],right:[mk62('s62d03','D','RD1',2500,3250,0,2100)],left:[mk62('s62d04','N','LN1',900,2700,0,2400)]},
    actual:{'FW1.w':1800,'FW1.h':1350,'BD1.w':900,'BD1.h':2100,'RD1.w':750}},                                         // Master Bedroom
  'MCAP-62d5e8d47a53':{els:{front:[mk62('s62e01','W','FW1',900,2400,900,2250)],back:[mk62('s62e02','D','BD1',200,1100,0,2100)]},
    actual:{'FW1.w':1500,'BD1.w':900}},                                                                               // Sample wardrobe repair · Master Bedroom
  // Demo bathroom door (at Revisions): three marks reached Scope -- the door to replace, a vanity unit the client wished, the
  // passage wall to repaint. The strip counts them and step 2 of Approve shows them by trade, from these two preps.
  'MCAP-62b4d09a1e55':{els:{front:[mk62('s62f01','D','FD1',350,1100,0,2100,Object.assign({act:'replace'},SC62))],back:[mk62('s62f02','W','BW1',500,1000,1650,2100)],left:[mk62('s62f03','CRP','LCRP1',300,1200,0,850,Object.assign({born:'wish'},SC62))]},
    actual:{'FD1.w':750,'FD1.h':2100}},                                                                               // Bathroom
  'MCAP-62b7e3c5f018':{els:{front:[mk62('s62g01','D','FD1',150,950,0,2100)],left:[mk62('s62g02','DEF','LDEF1',800,2000,300,1500,Object.assign({sub:'peeling',born:'wish'},SC62))]},
    actual:{'FD1.w':800}}};                                                                                           // Passage
{ const sp=spApi(); if(sp&&sp.seed62) for(const q of S.queue){ const d=PREP62[q.deviceId]; if(d&&q.kind==='360'&&q.pano) sp.seed62(q.deviceId,q.size,d.els,d.actual); } }
// the rooms a project shows: the site's chosen rooms, the rooms holding this phone's captures, and the rooms S5 knows
const known62=p=>LENS()||!window.ALB60||!p?[]:window.ALB60.rooms(p);
// (a room S5 knows that this phone's own room list does not have -- one another phone added -- still gets its tile, after the rest)
const roomsOfP=p=>{ const kn=known62(p); return roomsInScope(p.client,p.site,siteTypeOf(p.client,p.site),r=>kn.includes(r)||S.queue.some(q=>q.room===r&&key(q.projectTitle)===key(p.title))).concat(kn.filter(r=>!ROOMS.includes(r))); };
const order62=L=>L.slice().sort((a,b)=>(ROOMS.indexOf(a)<0?1e6:ROOMS.indexOf(a))-(ROOMS.indexOf(b)<0?1e6:ROOMS.indexOf(b)));
// a room's wishes (port 62's count, per room): the marks past Existing and not Dropped in its newest 360 with a survey prep
const wishes62=(p,room)=>{ const sp=spApi(); if(!sp) return 0; for(const q of S.queue){ if(q.kind!=='360'||q.room!==room||key(q.projectTitle)!==key(p.title)||!q.pano) continue;
    const z=sp.summary(q.deviceId,docOf(q),q.s5); if(z&&z.life) return ['wish','vrev','vok','vadd','cok','scope'].reduce((a,k)=>a+(z.life[k]||0),0); } return 0; };''')

# ================================================================ 8. the WORK row, Which work?, the scope sheet -- per room
rep('''  const w=newWork(p); return li({over:'WORK',head:esc(w==null?'Pick the work':C.workName(w)),sup:esc(w==null?'not filed to a work · tap to change':`${C.stateName(C.stateOf(p,w))} · tap to change`),onClick:()=>{ S.showCaptureSheet=false; S.pickWork61c=true; }}); }
// the 16 works only: a photo taken after approval belongs to a work (the album camera's rule) -- never back to the Quote journey
function workSheet61c({current,p,onPick,onDismiss}){ const C=window.ALB60;
  return sheet('work61c',sheetTitle('Which work?')+C.WORKS.map(([id,name])=>li({head:esc(name),sup:esc(C.stateName(C.stateOf(p,id))),sel:id===current,onClick:()=>{ onPick(id); render(); }})).join('')+'<div style="height:12px"></div>',()=>{ onDismiss(); render(); }); }''',
    '''  const room=S.navRoom, w=newWork(p); return li({over:'WORK',head:esc(w==null?'Pick the work':C.workName(w)),sup:esc(w==null?'not filed to a work · tap to change':`${workSup62(p,room,w)} · tap to change`),onClick:()=>{ S.showCaptureSheet=false; S.pickWork61c=true; }}); }
// v62b: a work's line in THIS room -- its state where the room's approved scope has it, else that it is outside it
const workSup62=(p,room,id)=>{ const C=window.ALB60; return C.inScope(p,room,id)?C.stateName(C.stateOf(p,room,id)):"not in this room's scope"; };
// the 16 works only: a photo taken after approval belongs to a work (the album camera's rule) -- never back to the Quote journey.
// v62b: the room's approved works first, then the rest
function workSheet61c({current,p,onPick,onDismiss}){ const C=window.ALB60, room=S.navRoom, sc=C.roomScope(p,room), L=C.WORKS.filter(w=>sc.includes(w[0])).concat(C.WORKS.filter(w=>!sc.includes(w[0])));
  return sheet('work61c',sheetTitle('Which work?')+L.map(([id,name])=>li({head:esc(name),sup:esc(workSup62(p,room,id)),sel:id===current,onClick:()=>{ onPick(id); render(); }})).join('')+'<div style="height:12px"></div>',()=>{ onDismiss(); render(); }); }''')
cut('''// SPEC61c §4.2, in the words of S5 /projects scopeHtml: approve (Approve the estimate ▸), record (Record the approved scope) and
''', '''  return sheet('scope61c',h,()=>{ S.scope61c=null; render(); }); }
''', '''// v62b · 3.3: THE APPROVED SCOPE IS PER ROOM. Two steps in one sheet. Step 1, "Rooms in the approved estimate": the rooms S5
// knows, each "LR · Living Room" with its count of ticked works, and "4 rooms · 19 works ticked". Step 2, a room's sixteen
// ticks -- v61c's sheet, titled with the room; Back returns to the rooms. Move to Client approved / Record the approved scope
// send the ticks ROOM BY ROOM, in WORKS order, through P54 (the S5 stand-in), whose refusal text lands in the status line
// with the sheet left open; nothing ticked anywhere is refused in S5's own words. The read-only view of the approved scope is
// the overview: this sheet has no view mode. Step 2 also carries the room's marks in scope as a HINT, and "Copy the ticks
// from <room>"; "Record the approved scope" shows the old project-wide list beside both steps, with nothing pre-ticked.
// a room's marks that reached Scope, by trade ("Carpentry 2"), read from the room's survey prep -- the SAME preps the phase
// strip counts ("3 marks in Scope"), so the hint and the count cannot disagree (review 1, finding 2). No prep, no hint.
const marks62=(p,room)=>{ const T=window.T53, L=window.L53, c={}; if(!T||!L||!wish()) return [];
  try{ const r=(wish().prepsOf(p)||[]).find(x=>x.room===room);
    for(const s of Object.keys((r&&r.w)||{})){ const W=r.w[s]; for(const e of (W&&Array.isArray(W.els)?W.els:[])) if(e&&e.t&&e.t!=='NOTE'&&L.lifeOf(e)==='scope'){ const tr=T.tradeOf(e.t,s,e.sub,e.cat)||'general'; c[tr]=(c[tr]||0)+1; } } }catch(_){ return []; }
  const order=T.TRADES?T.TRADES.map(t=>t.id):[];
  return Object.keys(c).sort((a,b)=>order.indexOf(a)-order.indexOf(b)).map(id=>`${(T.TRADE&&T.TRADE[id]&&T.TRADE[id].short)||id} ${c[id]}`); };
function scopeSheet(p){ const st=S.scope61c, C=window.ALB60, P=window.P54; if(!st||!C||!P) return '';
  const mode=st.mode, W=C.WORKS, RM=order62(C.rooms(p)), close=()=>{ S.scope61c=null; };
  // PROPOSED (section 4): a project approved before the scope was per room -- the old project-wide list is shown beside the
  // sheet FOR REFERENCE, in both steps, and nothing is pre-ticked (which room each work was approved in, nobody recorded)
  const old=mode==='record'?C.oldScope(p):null, oldH=old&&old.works.length?`<div class="bs mut sc61n" data-sc62old="${old.works.length}"><span>The scope an older build recorded for the whole project, for reference:</span> ${old.works.map(id=>`<span>${esc(C.workName(id))}</span>`).join(' · ')}</div>`:'';
  const ticksOf=r=>(st.cells.find(x=>x.room===r)||{works:[]}).works, cell=r=>{ let c=st.cells.find(x=>x.room===r); if(!c){ c={room:r,works:[]}; st.cells.push(c); } return c; };
  const live=()=>RM.map(r=>({room:r,works:W.map(w=>w[0]).filter(id=>ticksOf(r).includes(id))})).filter(c=>c.works.length);
  const submit=()=>{ const cells=live();
    if(!cells.length){ st.text=P.SCOPE_MISSING_MSG; st.warn=true; return; }
    const r=mode==='record'?P.recordScope(p,cells):P.approve(p,cells);
    if(!r){ S.scope61c=null; return; }
    if(r.refused){ const m=P.msg(); st.text=m?m.text:P.SCOPE_MISSING_MSG; st.warn=true; if(r.refused==='scopestale') st.cells=C.scopeOf(p)||[]; return; }
    S.scope61c=null; };
  let h;
  if(st.room==null||!RM.includes(st.room)){ st.room=null; const cells=live(), nw=cells.reduce((n,c)=>n+c.works.length,0);
    h=sheetTitle('Rooms in the approved estimate')+
      // review 9: one note per mode, S5 /projects scopeHtml verbatim (approve / record): approval is final, a change after it is a change request
      `<div class="bs mut sc61n">${esc(mode==='record'?'Approved before a scope was recorded: tick the works the client approved and Record the approved scope. After that the scope changes only through change requests.':'Tick every work the client is approving: Move to Client approved sends them as the baseline. Approval is final — after it the scope changes only through change requests.')}</div>`+
      oldH+`<div class="bs sc61c" data-sc62n="${cells.length},${nw}">${esc([plural(cells.length,'room'),nw===1?'1 work ticked':`${nw} works ticked`].join(' · '))}</div>`+
      (RM.length?`<div class="sc61g">${RM.map(r=>{ const k=ticksOf(r).filter(id=>W.some(w=>w[0]===id)).length;
          return `<div class="li two sc61r rip" data-sc62room="${esc(r)}" data-n="${k}"${A(()=>{ st.room=r; st.text=null; st.warn=false; })}><div class="tx"><div class="bl">${esc(rlabel(r))}</div><div class="bm mut">${esc(k?plural(k,'work'):'no works ticked')}</div></div><span class="olc">${ic('chev')}</span></div>`; }).join('')}</div>`
        :`<div class="bs mut sc61n" data-sc62none="1">No room of this project is on S5 yet — save a room's survey prep first</div>`)+
      `<div class="bs sc61m${st.warn?' warn':''}" role="status" data-sc61m="${st.text?'1':''}">${esc(st.text||'')}</div>`+
      `<div class="sc61b">`+tbtn('Cancel',close)+btn(mode==='record'?'Record the approved scope':'Move to Client approved',submit)+`</div>`; }
  else { const r=st.room, has=id=>ticksOf(r).includes(id), n=ticksOf(r).filter(id=>W.some(w=>w[0]===id)).length;
    const row=([id,name])=>`<div class="li sc61r rip" data-work="${id}" role="checkbox" aria-checked="${has(id)?'true':'false'}"${A(()=>{ const c=cell(r), i=c.works.indexOf(id); if(i<0) c.works.push(id); else c.works.splice(i,1); st.text=null; st.warn=false; })}><div class="tx"><div class="bl">${esc(name)}</div></div>${cb(has(id),false)}</div>`;
    // section 6: this room's marks that reached Scope, by trade -- a HINT, never ticks (13 trades do not map onto 16 works)
    const mk=marks62(p,r), mkH=mk.length?`<div class="bs mut sc61n" data-sc62marks="${mk.length}"><span>This room's marks in scope:</span> ${mk.map(t=>`<span>${esc(t)}</span>`).join(' · ')}</div>`:'';
    // PROPOSED, a convenience: Copy the ticks from <room> -- every other room that has ticks; it REPLACES this room's ticks
    const src=RM.filter(x=>x!==r&&ticksOf(x).some(id=>W.some(w=>w[0]===id)));
    const cpH=src.length?`<div class="hsc chips sc62cp" data-sk="sc62copy">${src.map(x=>chip(`Copy the ticks from ${x}`,{f:false,cls:'sc62cpc',onClick:()=>{ cell(r).works=ticksOf(x).slice(); st.text=null; st.warn=false; }})).join('')}</div>`:'';
    h=sheetTitle(`Works in the approved estimate · ${r}`)+oldH+mkH+`<div class="bs sc61c" data-sc61n="${n}" data-sc62in="${esc(r)}">${esc(`${n} of ${W.length} works ticked`)}</div>`+cpH+
      `<div class="sc61g">${W.map(row).join('')}</div>`+
      `<div class="sc61b">`+tbtn('Back',()=>{ st.room=null; },false,'data-sc62back="1"')+`</div>`; }
  return sheet('scope61c',h,()=>{ S.scope61c=null; render(); }); }
''')

# ================================================================ 9. the capture sheet: in execution, Photo is the progress shot of THIS room
rep('''  const noWork=!LENS()&&approved(S.navProject)&&newWork(S.navProject)==null;   // v61c: approved, and no work to file at yet
  let h=sheetTitle('Capture 360')+''',
    '''  const noWork=!LENS()&&approved(S.navProject)&&newWork(S.navProject)==null;   // v61c: approved, and no work to file at yet
  // v62b · 3.2 (7): in execution the sheet is the WORK row, then Photo -- the phone camera, the PROGRESS SHOT of this room and that
  // work, with no room row -- then the 360 (the X3). A room S5 does not know takes no progress shot until its survey prep is
  // saved (question 4, option (a)): there the sheet is the WORK row and the 360.
  const ex62=!LENS()&&approved(S.navProject)&&!!album(), kn62=ex62&&window.ALB60.known(S.navProject,S.navRoom);
  const photo62=ex62&&kn62?`<div class="ls b mut" style="padding:14px 0 6px 24px">A SINGLE PHOTO</div><div style="display:flex;gap:8px;padding:0 20px">${btn('Photo',()=>{ const p0=S.navProject, r0=S.navRoom; S.showCaptureSheet=false; album().openCam(p0,r0,newWork(p0,r0)); },busy,'data-rm62photo="1" style="flex:1 1 0;padding:0"')}</div>`:'';
  let h=sheetTitle('Capture 360')+''')
rep(''':workRow61c(S.navProject))+
    `<div class="ls b mut" style="padding:14px 0 4px 24px">ROOM SIZE (MM)</div>`+''',
    ''':workRow61c(S.navProject))+photo62+
    `<div class="ls b mut" style="padding:14px 0 4px 24px">ROOM SIZE (MM)</div>`+''')
rep('''    `<div style="padding:0 20px">${obtn('Pick a 360 from the gallery',()=>{ S.showCaptureSheet=false; S.picker='360'; },busy||!sizesOk||noWork,'style="width:100%"')}</div>`+
    `<div class="ls b mut" style="padding:20px 0 6px 24px">A SINGLE PHOTO</div>''',
    '''    `<div style="padding:0 20px">${obtn('Pick a 360 from the gallery',()=>{ S.showCaptureSheet=false; S.picker='360'; },busy||!sizesOk||noWork,'style="width:100%"')}</div>`+
    (ex62?`<div style="height:24px"></div>`:`<div class="ls b mut" style="padding:20px 0 6px 24px">A SINGLE PHOTO</div>''')
rep('''${obtn('Gallery',()=>{ S.showCaptureSheet=false; S.picker='Photo'; },busy||noWork,'style="flex:1 1 0;padding:0"')}</div><div style="height:24px"></div>`;
  return sheet('capture',h,()=>set({showCaptureSheet:false})); }''',
    '''${obtn('Gallery',()=>{ S.showCaptureSheet=false; S.picker='Photo'; },busy||noWork,'style="flex:1 1 0;padding:0"')}</div><div style="height:24px"></div>`);
  return sheet('capture',h,()=>set({showCaptureSheet:false})); }''')
rep('''  openSurvey:id=>openSurveyFor(id), wish:()=>wish(), plan:()=>fplan(), album:()=>album() };''',
    '''  openSurvey:id=>openSurveyFor(id), wish:()=>wish(), album:()=>album() };''')

# ================================================================ 10. the project screen (3.1)
rep('''  if(p){ const has=r=>S.queue.some(q=>q.room===r&&key(q.projectTitle)===key(p.title)), rooms=roomsInScope(p.client,p.site,siteTypeOf(p.client,p.site),has), cnt=r=>S.queue.filter(q=>q.room===r&&key(q.projectTitle)===key(p.title)).length;''',
    '''  if(p){ const rooms=roomsOfP(p), cnt=r=>S.queue.filter(q=>q.room===r&&key(q.projectTitle)===key(p.title)).length;
    // v62b · 3.1: in execution every room S5 knows is a tile, with or without a capture on this phone, and each tile carries THE
    // ROOM'S OWN line for the project's phase, derived: Survey as before (the dot, the count); the wish phases "3 wishes";
    // execution its roll-up over the works in its approved scope and "2/8" over a thin bar
    const C62=LENS()?null:window.ALB60, ex62=!!C62&&approved(p), kn62=ex62?C62.rooms(p):[], ph62=LENS()||!window.P54?null:window.P54.phaseOfQuote(p.quote), act62=r=>cnt(r)>0||kn62.includes(r);
    const line62=r=>{ if(!C62) return null;
      if(ex62){ if(!kn62.includes(r)) return cnt(r)>0?{k:'nos5'}:null; const R=C62.roomRoll(p,r); return R.state?{k:'roll',R}:{k:'noscope'}; }
      return ph62!=null&&ph62>=1&&ph62<=4&&cnt(r)>0?{k:'wish',n:wishes62(p,r)}:null; };''')
rep('''    const shot=rooms.filter(r=>cnt(r)>0), rest=rooms.filter(r=>cnt(r)===0), pend=shot.filter(r=>!shotAt(r)).length, hidden=Math.max(0,ROOMS.length-rooms.length);''',
    '''    const shot=rooms.filter(act62), rest=rooms.filter(r=>!act62(r)), pend=shot.filter(r=>!shotAt(r)).length, hidden=Math.max(0,ROOMS.length-rooms.length);''')
rep('''openScope:(pp,mode)=>{ S.scope61c={pk:pk61(pp),mode,ticks:mode==='record'?[]:((window.ALB60.scopeOf(pp)||[]).slice()),text:null,warn:false}; },''',
    '''openScope:(pp,mode)=>{ if(mode==='view'){ if(album()) album().open(pp,'overview'); return; }   /* v62b: the approved scope is read in the overview */ S.scope61c={pk:pk61(pp),mode,room:null,cells:mode==='record'?[]:(window.ALB60.scopeOf(pp)||[]),text:null,warn:false}; },''')
rep('''    body+=`<div class="hsc chips" data-sk="pchips">`+chip(plural(shot.length,'room')+' shot',{f:false,dis:true})+''',
    '''    if(album()) body+=album().rows(p);   // v62b · 3.1: "All rooms · works ▸" (execution) and, for staff while it holds something, "Needs a room · …"
    body+=`<div class="hsc chips" data-sk="pchips">`+chip(plural(rooms.filter(r=>cnt(r)>0).length,'room')+' shot',{f:false,dis:true})+''')
rep('''    const tile=(r,n,cur,pano)=>{ const sh=n>0, ph=sh&&pano; return `<div class="tw"><div class="tile ${sh?'shot':''} rip"${A(()=>{ S.navRoom=r; S.capPhase=null; S.capWork=null; })}${tourA('room:'+r)}>`+(ph?`<div class="thumb" style="${thumbBg(pano)}"></div><div class="wash"></div>`:'')+
      `<div class="tm b" style="position:absolute;left:8px;top:8px;color:${ph?'#fff':sh?'var(--onscn)':'var(--onsv)'}">${esc(tok(r))}</div>`+
      (sh?`<span class="dot9" style="background:${cur?'#3F8F5B':'#B0740E'}"></span><span class="ls" style="position:absolute;right:7px;bottom:7px;color:${ph?'#fff':'var(--onscn)'}">${n}</span>`:'')+
      `<div class="ls ell" style="position:absolute;left:7px;bottom:7px;width:72%;color:${ph?'#fff':sh?'var(--onscn)':'var(--onsv)'}">${esc(r)}</div></div></div>`; };
    body+=PG('rooms:'+p.serverId+p.title,shot.map(r=>tile(r,cnt(r),shotAt(r),last(r))).join('')+(fplan()?fplan().tile(p):'')+''',
    '''    const tile=(r,n,cur,pano)=>{ const L=line62(r), sh=n>0||act62(r), ph=n>0&&pano, col=ph?'#fff':sh?'var(--onscn)':'var(--onsv)';
      const ln=!L?'':L.k==='roll'?`<div class="rt62 ls" style="color:${col}"><span class="ell rt62s">${esc(C62.stateName(L.R.state))}</span><span class="rt62n">${L.R.done}/${L.R.total}</span></div><div class="rt62b" aria-hidden="true"><i style="width:${Math.round(100*L.R.done/L.R.total)}%"></i></div>`
        :`<div class="rt62 ls" style="color:${col}"><span class="ell rt62s">${esc(L.k==='noscope'?'no scope yet':L.k==='nos5'?'not on S5 yet':L.n===1?'1 wish':`${L.n} wishes`)}</span></div>`;
      return `<div class="tw"><div class="tile ${sh?'shot':''}${ph&&L&&L.k==='roll'?' ph62':''} rip"${L?` data-rt62="${L.k}"${L.k==='roll'?` data-rt62v="${L.R.done}/${L.R.total}" data-rt62s="${esc(L.R.state)}"`:''}`:''}${A(()=>{ S.navRoom=r; S.capPhase=null; })}${tourA('room:'+r)}>`+(ph?`<div class="thumb" style="${thumbBg(pano)}"></div><div class="wash"></div>`:'')+
      `<div class="tm b" style="position:absolute;left:8px;top:8px;color:${col}">${esc(tok(r))}</div>`+
      (n>0&&!ex62?`<span class="dot9" style="background:${cur?'#3F8F5B':'#B0740E'}"></span>`:'')+(n>0&&!L?`<span class="ls" style="position:absolute;right:7px;bottom:7px;color:${ph?'#fff':'var(--onscn)'}">${n}</span>`:'')+ln+
      `<div class="ls ell" style="position:absolute;left:7px;bottom:7px;width:${L?'calc(100% - 14px)':'72%'};color:${col}">${esc(r)}</div></div></div>`; };
    // v62b: the Floor plan tile and the Album tile are gone from this grid (3.1): rooms, then the rest of the site's rooms
    body+=PG('rooms:'+p.serverId+p.title,shot.map(r=>tile(r,cnt(r),shotAt(r),last(r))).join('')+''')
rep('''<span class="ll pri">+ Room</span></div></div>`:''):'')+(album()?album().tile(p):''),{fill:true,cls:'tiles',cols:S.land?6:3}); }''',
    '''<span class="ll pri">+ Room</span></div></div>`:''):''),{fill:true,cls:'tiles',cols:S.land?6:3}); }''')

# ================================================================ 11. the room screen = the room's home (3.2)
rep('''  const sibs=p?roomsInScope(p.client,p.site,siteTypeOf(p.client,p.site),r=>S.queue.some(q=>q.room===r&&key(q.projectTitle)===key(p.title))):ROOMS;''',
    '''  const sibs=p?roomsOfP(p):ROOMS;''')
rep('''  body+=planRow62(p,room);   // port 62 · P2: Plan & elevations, one tap from the room
  if(!cards.length) body+=`<div class="bm mut" style="padding:24px">No captures in this room yet. Use Capture 360 below.</div>`;
  else if(LENS()){''',
    '''  // v62b · 3.2: THE ROOM'S HOME. Outside the v53 lens the body is the room module's: the Plan & elevations chip (port 62 · P2,
  // unchanged, folded in), then the room's works with their states, change requests and photos, the Quote journey, "Not filed
  // to a work", the folded "Not in this room's scope", and the room's Work log. The work-filter chips are gone: the works
  // section replaces them. The lens keeps v61b's list by stage.
  const R62=!LENS()&&p&&album()?album():null;
  if(R62) body+=R62.room(p,room,{cards,capTile:c=>capTile(c),planRow:planRow62(p,room)});
  else if(!cards.length) body+=`<div class="bm mut" style="padding:24px">No captures in this room yet. Use Capture 360 below.</div>`;
  else {''')
cut('''  else { const gk=w=>w==null?'unfiled':w, wk=k=>k==='unfiled'?null:k, rank=w=>w==null?-2:w===0?-1:w;   // v61c: by WORK -- highest work first, then the Quote journey, then not filed
''', '''    body+=PG('caps:'+(p?p.title:'')+'|'+room+'|'+(S.capWork==null?'':S.capWork),G,{fill:true,cls:'tiles',cols:S.land?4:2}); }
''', '')
rep('''    body:body+`<button class="efab rip" style="bottom:16px"${A(()=>{ S.showCaptureSheet=true; })}${tourA('capture360')}>${ic('cam')}<span class="ll">Capture 360</span></button>`,onTab:k=>{ S.tab=k; S.navRoom=null; }}); }''',
    '''    body:body+(R62&&!window.ALB60.isStaff()?'':`<button class="efab rip" style="bottom:16px"${A(()=>{ S.showCaptureSheet=true; })}${tourA('capture360')}>${ic('cam')}<span class="ll">Capture 360</span></button>`),onTab:k=>{ S.tab=k; S.navRoom=null; }}); }   // v62b: the capture button is staff's; a vendor uploads from its own work's row''')
# the capture screen's WORK line reads the cell
rep('''nm=w===0?'Quote journey':w==null?'Pick the work':C6.workName(w), lead=w===0?qa:w==null?'not filed to a work':C6.stateName(C6.stateOf(p,w));   // v61c: where the capture IS filed (D4)''',
    '''nm=w===0?'Quote journey':w==null?'Pick the work':C6.workName(w), lead=w===0?qa:w==null?'not filed to a work':approved(p)?workSup62(p,S.navRoom,w):C6.stateName('Not started');   // v61c: where the capture IS filed (D4) · v62b: the cell's state, in this room (before approval no cell has started)''')
rep('''  if(S.pickWork61c&&p) ov+=workSheet61c({current:newWork(p),p,onPick:id=>{ (S.capWorkFor||(S.capWorkFor={}))[pk61(p)]=id; S.pickWork61c=false; S.showCaptureSheet=true; },onDismiss:()=>{ S.pickWork61c=false; S.showCaptureSheet=true; }});''',
    '''  if(S.pickWork61c&&p) ov+=workSheet61c({current:newWork(p),p,onPick:id=>{ (S.capWorkFor||(S.capWorkFor={}))[wk62(p)]=id; S.pickWork61c=false; S.showCaptureSheet=true; },onDismiss:()=>{ S.pickWork61c=false; S.showCaptureSheet=true; }});''')

# ================================================================ 12. composition and Android Back
rep('''function view(){ if(S.albumFor&&(S.navProject!==S.albumFor||S.navRoom!=null||S.tab!=='browse'||S.survey)){ S.albumFor=null; if(ALw) ALw.leave(); }   // v59
  if(S.planFor&&(S.navProject!==S.planFor||S.navRoom!=null||S.tab!=='browse'||S.survey)){ S.planFor=null; if(FPw) FPw.leave(); }   // v54 D
''', '''function view(){ if(S.albumFor&&(S.navProject!==S.albumFor||S.navRoom!=null||S.tab!=='browse'||S.survey)){ S.albumFor=null; if(ALw) ALw.leave(); }   // v59 · v62b: the overview / "Needs a room" page
  if(ALw&&(S.navRoom==null||S.tab!=='browse'||S.showSearch||S.closed)) ALw.leaveRoom();   // v62b: a room's viewer, camera and sheets close when its screen is left
  // v62b (review 1): the overview a room was opened from is remembered only while that project's room screens are on -- a crumb, a tab or another project forgets it
  if(S.ovFrom62&&(S.navRoom==null||S.tab!=='browse'||!S.navProject||pk61(S.navProject)!==S.ovFrom62.pk)) S.ovFrom62=null;
''')
rep('''    main=S.albumFor&&p&&album()?album().screen(p):S.planFor&&p&&fplan()?fplan().screen(p):S.wishFor&&p&&wish()?wish().screen(p):treeScreen();   // v54 D: the Floor plan page first · v59: the Album first of all
    if(S.albumFor&&p&&album()) ov+=album().ov(p); }''',
    '''    main=S.albumFor&&p&&album()?album().screen(p):S.wishFor&&p&&wish()?wish().screen(p):treeScreen();   // v62b: the overview / "Needs a room" page first (the Floor plan page and the Album page are gone)
    if(S.albumFor&&p&&album()) ov+=album().ov(p); }   // v62b: that page's own sheet -- Viewing as (the overview), Which room? ("Needs a room")''')
rep('''  else { ov+=roomLevelOverlays(); main=roomScreen();
    if(S.picker) ov+=pickerHtml(); if(S.camera) ov+=cameraHtml(); }''',
    '''  else { ov+=roomLevelOverlays(); main=roomScreen();
    if(S.picker) ov+=pickerHtml(); if(S.camera) ov+=cameraHtml();
    if(!LENS()&&S.navProject&&album()) ov+=album().ov(S.navProject); }   // v62b: the room's own overlay -- a sheet, the progress-shot camera, the photo viewer''')
rep('''  if(S.scope61c){ S.scope61c=null; return render(); }   // v61c: the scope checklist closes first''',
    '''  if(S.scope61c){ if(S.scope61c.room!=null) S.scope61c.room=null; else S.scope61c=null; return render(); }   // v61c: the scope checklist closes first · v62b: a room's ticks go back to the rooms''')
rep('''  if(S.planFor&&S.navRoom==null){ if(FPw&&FPw.back()) return render(); S.planFor=null; if(FPw) FPw.leave(); return render(); }   // v54 D
''', '''  if(S.navRoom!=null&&!S.navCapture&&ALw&&ALw.back()) return render();   // v62b: in a room -- a sheet → the camera's sheet → the camera → the viewer, then the room itself
''')

# ================================================================ 13a. the project line in execution names rooms, so it may take two lines
rep('''<div class="qrow"><i class="qck" aria-hidden="true">✓</i><span class="ts ell qtx">${esc(L.text)}</span></div>''',
    '''<div class="qrow"><i class="qck" aria-hidden="true">✓</i><span class="ts ell2 qtx">${esc(L.text)}</span></div>''')   # v62b

# ================================================================ 6a. one capture of the Living Room that is not filed to a work (3.2 · 4): a delivery photo, which only a person can file
rep('''    {deviceId:'MCAP-3f9a1c2b7d10',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Guest Bedroom',''',
    '''    {deviceId:'MCAP-62c1a7d30b96',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Living Room',stage:'Material delivered on site',captureDate:'2026-10-10',kind:'Photo',state:'SYNCED',serverName:'MEST-PH-2026-00421',pano:'photo:30% 58%',sku:''},   // v62b: not filed to a work
    {deviceId:'MCAP-62d1c4a80e31',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Living Room',stage:'As-built site measure',captureDate:'2026-10-10',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00419',pano:'360',size:[3600,5200,2850],sku:''},   // v62b: surveyed (PREP62) -- the room's current 360; the two older ones below stay as they were
    {deviceId:'MCAP-62d2f7b19c64',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Kitchen',stage:'As-built site measure',captureDate:'2026-10-10',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00420',pano:'360',size:[2700,3000,2850],sku:''},   // v62b: surveyed (PREP62)
    {deviceId:'MCAP-62d3a0e52b97',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Toilet 1',stage:'As-built site measure',captureDate:'2026-10-10',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00417',pano:'360',size:[1500,2100,2700],sku:''},   // v62b: surveyed (PREP62)
    {deviceId:'MCAP-3f9a1c2b7d10',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Guest Bedroom',''')
rep('''    {deviceId:'MCAP-0b7c33e95f12',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Living Room',stage:'As-built site measure',captureDate:'2026-09-28',''',
    '''    {deviceId:'MCAP-62d4b9c36f20',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Master Bedroom',stage:'As-built site measure',captureDate:'2026-10-09',kind:'360',state:'SYNCED',serverName:'MEST-PH-2026-00412',pano:'360',size:[3600,4200,2850],sku:''},   // v62b: surveyed (PREP62)
    {deviceId:'MCAP-0b7c33e95f12',projectTitle:'Demo flat interiors',customerName:'Demo Client',room:'Living Room',stage:'As-built site measure',captureDate:'2026-09-28',''')

# ================================================================ 6b. THE DEMO'S ROOMS READ AS SURVEYED (review 1, finding 1). Question 4 is drawn at option (a): a room carries
# works only once a 360 of it has its survey prep saved and uploaded. So every room S5 knows in the demo has, on this phone, a 360
# whose survey prep IS saved -- room lines on six surfaces, marks, laser readings -- and its room screen says so (the Plan &
# elevations chip opens, the 360 reads "6 of 6 bounded · 4 marks · 4 read"). Passage of Demo flat interiors stays the one room
# with no survey. The preps are seeded into survey prep's own memory (where a capture with no stored document keeps its prep),
# so opening one shows exactly what the room screen counted. Demo bathroom door's marks in Scope live in these preps: ONE
# source for the phase strip ("3 marks in Scope · 2 rooms surveyed") and for the scope sheet's hint (finding 2).
rep('''window.__sp={saved58:()=>cur.doc?lastSaved:(cur.id?(mem[cur.id]||null):null),''',
    '''// v62b: a DEMO capture's survey prep AS SAVED (the mock-up's seed; memory only, as every prep of a capture with no stored document):
// the room lines of the six surfaces at the laser-sized guess (fresh()'s own geometry for that L · W · H), each marked set, with
// the marks and the laser readings given. Never over a prep the capture already has here.
function seed62(id,z,els,actual){ if(!id||mem[id]||!Array.isArray(z)||z.length!==3||!z.every(okSz)) return false;
  const k=[LWH.L,LWH.W,LWH.H], w0=st.w; let w=null;
  try{ LWH.L=z[0]; LWH.W=z[1]; LWH.H=z[2]; st.w={}; fresh(); w=st.w; }finally{ st.w=w0; LWH.L=k[0]; LWH.W=k[1]; LWH.H=k[2]; }
  for(const s of SURF){ SLOTS.forEach(q=>{ w[s].set[q]=true; }); w[s].els=((els&&els[s])||[]).map(e=>Object.assign({},e)); }
  mem[id]=JSON.stringify({w,actual:Object.assign({},actual||{}),L:z[0],W:z[1],H:z[2]}); return true; }
window.__sp={seed62,saved58:()=>cur.doc?lastSaved:(cur.id?(mem[cur.id]||null):null),''')

# ================================================================ 11a. the work-filter chips are gone, and their state with them (S.capWork)
rep('''refileWork:false, capWork:null, capWorkFor:{}, showDetail:false,''', '''refileWork:false, capWorkFor:{}, showDetail:false,''')
rep('''onSibling:k=>{ S.navRoom=k; S.capPhase=null; S.capWork=null; }});''', '''onSibling:k=>{ S.navRoom=k; S.capPhase=null; }});''')
rep('''&&!S.survey){ S.capPhase=null; S.capWork=null; }''', '''&&!S.survey){ S.capPhase=null; }''')

# ================================================================ 12a. Android Back from a room opened from the overview (a row of its grid, a room on its Plan) returns to the
# overview, in the view it was left in -- Works or Plan, its log as it was -- so three rooms can be checked row, Back, row,
# Back. Any other way out of the room (a breadcrumb, a tab) follows the tree, and Back from the overview is the project.
rep('''  if(S.navCapture){ if(S.navFace!=null) S.navFace=null; else S.navCapture=null; return render(); }
  S.navRoom=null; render(); }''',
    '''  if(S.navCapture){ if(S.navFace!=null) S.navFace=null; else S.navCapture=null; return render(); }
  if(S.ovFrom62&&S.navProject&&pk61(S.navProject)===S.ovFrom62.pk&&album()){ const f=S.ovFrom62; S.ovFrom62=null; S.navRoom=null; S.capPhase=null; album().open(S.navProject,'overview',f); return render(); }   // v62b
  S.navRoom=null; render(); }''')
