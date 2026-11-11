# proto88 · build88.py's third part: the tours, and the page around the phone. exec'd by build88.py.

# ================================================================ 13. the guided tours: the Floor plan tour goes with its page (3.1, 3.7)
T7_FROM, T7_TO = 'अल्बममधील कामे', 'प्रत्येक खोलीतील कामे'
rep('''//             'help' 'drawer' 'clients' 'client' 'site' 'project' 'room' 'capture' 'survey' 'wish' 'plan'
''', '''//             'help' 'drawer' 'clients' 'client' 'site' 'project' 'room' 'capture' 'survey' 'wish'
//           (v62b: 'plan' went with the Floor plan page -- arranging the rooms is S5 web's)
''')
rep('''//                      service:<id> (its tick line) · service:row:<id> · plan:tile · plan:room:<room> · plan:rotate
//                      plan:fit · plan:reset
''', '''//                      service:<id> (its tick line) · service:row:<id>
''')
rep('''//   go:'<screen>'       'help' 'clients' 'client' 'site' 'project' 'room' 'wish' 'plan' -- leaves survey prep first''',
    '''//   go:'<screen>'       'help' 'clients' 'client' 'site' 'project' 'room' 'wish' -- leaves survey prep first''')
rep('''//   planSel:'<room>'    select that room on the Floor plan page (its ↻ 90° appears)
''', '')
rep('''the phase store and the quote stage, the wish list, the floor-plan layout, the app's position -- and it''',
    '''the phase store and the quote stage, the wish list, the app's position -- and it''')
cut('''
// ---------------------------------------------------------------- 6. Floor plan
{id:'plan', name:'Floor plan',''', '''body:'Reset layout puts every room back in its starting row, after asking. The tour does not press it.'}]}
];''', '''
// (v62b: the sixth flow, Floor plan, went with the Floor plan page and its tile -- room-centred-album.md §3.1)
];''')
rep('''const LSKEYS=['mcft-phase-v54','mcft-wish-v52','mcft-arrange-v54','mcft-placeas-v53','mcft-export-v53'];''',
    '''const LSKEYS=['mcft-phase-v54','mcft-wish-v52','mcft-placeas-v53','mcft-export-v53'];''')
rep('''  'plan:tile':()=>first('#appv [data-fp54tile]'),                            // the project's Floor plan tile
  'plan:room':a=>first(`#fp54cv g[data-piece="${cssq(a)}"]`),               // a room on the Floor plan
  'plan:rotate':()=>first('#fp54cv .fp54rot'),                               // ↻ 90° on the selected room
  'plan:fit':()=>first('#appv [data-fp54fit]'),
  'plan:reset':()=>first('#appv [data-fp54reset]'),
''', '')
rep('''  if(S.navProject) return S.albumFor?'album':S.planFor?'plan':S.wishFor?'wish':'project';   // v59''',
    '''  if(S.navProject) return S.albumFor?'album':S.wishFor?'wish':'project';   // v59 · v62b: 'album' is the overview / "Needs a room" page''')
rep('''  const w=a&&a.wish?a.wish():null, f=a&&a.plan?a.plan():null;
  return {ls,S:sh,''', '''  const w=a&&a.wish?a.wish():null;
  return {ls,S:sh,''')
rep('''wish:w&&w.t55snap?w.t55snap():null,plan:f&&f.t55snap?f.t55snap():null,''', '''wish:w&&w.t55snap?w.t55snap():null,''')
rep('''  const w=a&&a.wish?a.wish():null, f=a&&a.plan?a.plan():null;
  try{ if(f&&f.leave) f.leave(); }catch(_){} try{ if(w&&w.leave) w.leave(); }catch(_){}''',
    '''  const w=a&&a.wish?a.wish():null;
  try{ if(w&&w.leave) w.leave(); }catch(_){}''')
rep('''  try{ if(z.plan!=null&&f&&f.t55restore) f.t55restore(z.plan); }catch(_){}
''', '')
rep('''  const w=a.wish?a.wish():null, f=a.plan?a.plan():null;
  if(S.wishFor&&where!=='wish'&&w&&w.leave) w.leave(); if(S.planFor&&where!=='plan'&&f&&f.leave) f.leave();''',
    '''  const w=a.wish?a.wish():null;
  if(S.wishFor&&where!=='wish'&&w&&w.leave) w.leave();''')
rep('''S.navCapture=null; S.navFace=null; S.wishFor=null; S.planFor=null;
  if(where==='help')''', '''S.navCapture=null; S.navFace=null; S.wishFor=null;
  if(where==='help')''')
rep('''  const d={clients:0,client:1,site:2,project:3,wish:3,plan:3,room:4}[where]; if(d==null) return;''',
    '''  const d={clients:0,client:1,site:2,project:3,wish:3,room:4}[where]; if(d==null) return;''')
rep('''  if(where==='wish') S.wishFor=p; if(where==='plan') S.planFor=p;''', '''  if(where==='wish') S.wishFor=p;''')
rep('''  if(m.planSel!=null&&a&&a.plan){ const f=a.plan(), p=proj(m); if(f&&f.t55select&&p) f.t55select(p,m.planSel); }
''', '')
rep('''// app.js may have rendered before this script: if Help is open, draw it now (mirrors __appPlanReady)''',
    '''// app.js may have rendered before this script: if Help is open, draw it now''')

# ================================================================ 14. the page around the phone: version line, the changes line
rep('<span class="eyeln"><b>mock-up v62a</b></span><span class="eysep"> · </span><span class="eyeln">11 Nov 2026, 18:36 IST</span>',
    f'<span class="eyeln"><b>mock-up v62b</b></span><span class="eysep"> · </span><span class="eyeln">{STAMP}</span>')
rep(' · v62a: a failed site rename says why + Plan &amp; elevations on the room · not yet approved, not on the phone</div>',
    ' · v62a: a failed site rename says why + Plan &amp; elevations on the room · v62b: the room is the scope of work — each room carries its own works, states, change requests and photos, the scope is approved per room, and one read-only overview shows every room’s works; the Album and the Floor plan tile are gone · not yet approved, not on the phone</div>')

exec(open(S + '/proto88/work/build88_notes.py', encoding='utf-8').read())
