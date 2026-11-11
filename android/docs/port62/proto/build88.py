#!/usr/bin/env python3
# proto88: builds mock-up v62b (the room-centred album: the model, the project screen, the room screen as the room's home,
# the overview, "Needs a room", Approve per room, change requests per room and work) from the published v62a (Version 87).
# Spec: mcft-erpnext-context analysis/room-centred-album.md (item RP0). Stage A built the model and the room; stage B the
# overview, "Needs a room", the scope sheet's extras, the notes card and the clean-up -- ONE script, one run.
# Every edit is an exact, counted replacement on the base page (or a cut between two markers that each occur once), so the
# build is repeatable and anything that moved fails loudly. The two rewritten modules are read from work/src/.
import json, bisect, hashlib, sys, datetime, zoneinfo, re

S = '/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad'
BASE = S + '/v61cbuild/index61c2.html'
OUT = S + '/proto88/index61c2.html'
SRC = S + '/proto88/work/src/'
BASE_MD5 = '3f481eaa521730871a1aaad9e3b97c00'

src = open(BASE, encoding='utf-8').read()
assert hashlib.md5(src.encode('utf-8')).hexdigest() == BASE_MD5, 'the base is not Version 87'
EYE4 = 'phone APK <b>0.3.318</b> · <span style="white-space:nowrap;letter-spacing:0;font-size:min(1em,2.7vw)">carries the whole v61c port, survey tour included</span></span></div>'
assert src.count(EYE4) == 1

def rep(old, new, n=1):
    global src
    c = src.count(old)
    assert c == n, f'expected {n} of {old[:110]!r}, found {c}'
    src = src.replace(old, new)

def cut(start, end, new, keep_end=False):
    """replace from `start` (inclusive) to `end` (inclusive unless keep_end) -- each marker occurs once, start before end"""
    global src
    assert src.count(start) == 1, f'start marker {start[:80]!r}: {src.count(start)}'
    i = src.index(start)
    assert src.count(end, i) >= 1 and src.count(end) == 1, f'end marker {end[:80]!r}: {src.count(end)}'
    j = src.index(end, i)
    src = src[:i] + new + src[(j if keep_end else j + len(end)):]

STAMP = sys.argv[1] if len(sys.argv) > 1 else datetime.datetime.now(zoneinfo.ZoneInfo('Asia/Kolkata')).strftime('%-d %b %Y, %H:%M IST')

# ================================================================ the texts (EN, MR)
# for the phone, to approve: [key, EN, MR, where]
PHONE_S = [
 ('album.all_rooms_works', 'All rooms · works', 'सर्व खोल्या · कामे', 'project screen · the row that opens the overview (execution only)'),
 ('album.needs_room', 'Needs a room', 'खोली ठरवणे बाकी', 'project screen · the row for what has no room, staff only: “Needs a room · 3 photos · 2 changes not sent”'),
 ('album.change_not_sent_1', '1 change not sent', '1 बदल पाठवला गेला नाही', 'the same row, one refused write'),
 ('album.no_room_album', 'In the album with no room', 'अल्बममध्ये, खोली न ठरवलेले', 'the “Needs a room” page · its first group (photos on S5 with no room)'),
 ('album.not_sent_phone', 'Not sent from this phone', 'या फोनवरून पाठवले गेले नाही', 'the “Needs a room” page · its second group (what the phone’s outbox could not send)'),
 ('album.old_build_msg', 'Recorded for the whole project by an older build. States are per room now — set it in the room.', 'जुन्या बिल्डने हे संपूर्ण प्रोजेक्टसाठी नोंदवले होते. आता स्थिती खोलीनुसार असते — ती त्या खोलीत ठरवा.', 'the phone’s own words on a state move or change request an older build made with no room'),
 ('album.no_scope_yet', 'no scope yet', 'अजून स्कोप नाही', 'room tile in execution · a room S5 knows with no work approved in it'),
 ('room.not_on_s5_save_prep', "Not on S5 yet — save this room's survey prep to give it works", 'अजून S5 वर नाही — या खोलीला कामे मिळण्यासाठी तिची सर्व्हे तयारी सेव्ह करा', 'room screen in execution · a room S5 does not know'),
 ('room.no_works_in_scope', "No works in this room's approved scope yet", 'या खोलीच्या मंजूर स्कोपमध्ये अजून कोणतेही काम नाही', 'room screen in execution · a room S5 knows with no work approved in it'),
 ('room.not_in_scope', "Not in this room's scope", 'या खोलीच्या स्कोपमध्ये नाही', 'room screen · the folded row: “Not in this room’s scope · 9 works”'),
 ('room.not_in_scope_lc', "not in this room's scope", 'या खोलीच्या स्कोपमध्ये नाही', 'Which work? and the WORK row · under a work outside the room’s scope'),
 ('phase.scope_rooms_title', 'Rooms in the approved estimate', 'मंजूर अंदाजपत्रकातील खोल्या', 'Approve · the scope sheet’s first step'),
 ('phase.scope_no_works_ticked', 'no works ticked', 'कोणतेही काम टिक केलेले नाही', 'Approve · under a room with no tick'),
 ('phase.scope_work_ticked_1', '1 work ticked', '1 काम टिक केले', 'Approve · the count: “1 room · 1 work ticked”'),
 ('app.work_1', '1 work', '1 काम', 'a count of works: “1 work” (the folded row, a room of the scope sheet, the APPROVED SCOPE line)'),
 ('phase.scope_no_rooms', "No room of this project is on S5 yet — save a room's survey prep first", 'या प्रोजेक्टची एकही खोली अजून S5 वर नाही — आधी एखाद्या खोलीची सर्व्हे तयारी सेव्ह करा', 'Approve · no room to tick'),
 ('app.room_in_progress_1', '1 room in progress', '1 खोली चालू', 'the project line, counting rooms'),
 ('app.room_done_1', '1 room done', '1 खोली पूर्ण', 'the project line, counting rooms'),
 ('app.room_on_hold_1', '1 room on hold', '1 खोली थांबवलेली', 'the project line, counting rooms'),
 ('app.room_abandoned_1', '1 room abandoned', '1 खोली सोडून दिलेली', 'the project line, when nothing else is counted'),
 # review 1, finding 5: the parts AFTER the first count rooms too, and खोली is feminine -- "1 थांबवलेली", "2 थांबवलेल्या"
 ('app.on_hold_1', '1 on hold', '1 थांबवलेली', 'the project line, after its first part: one more room, on hold (“2 rooms in progress · 1 done · 1 on hold”)'),
 ('app.abandoned_1', '1 abandoned', '1 सोडून दिलेली', 'the project line, after its first part: one more room, abandoned (“5 rooms done · 1 abandoned”)'),
 ('app.wish_1', '1 wish', '1 इच्छा', 'room tile in the wish phases'),
 # ---- stage B: the overview, "Needs a room", the scope sheet
 ('album.works', 'Works', 'कामे', 'the overview · the chip for the rooms × works grid (beside “Plan”)'),
 ('album.not_in_scope_lg', 'not in scope', 'स्कोपमध्ये नाही', 'the overview · legend: a work outside a room’s scope'),
 ('album.change_request_lg', 'change request', 'बदल विनंती', 'the overview · legend: the ! on a cell with a live change request'),
 ('album.not_in_any_scope', "not in any room's scope", 'कोणत्याही खोलीच्या स्कोपमध्ये नाही', 'the overview · under a work that no room has'),
 ('album.recorded_whole_project', 'recorded for the whole project', 'संपूर्ण प्रोजेक्टसाठी नोंदवलेले', 'the overview · Work log: a state an older build recorded with no room'),
 ('album.plan_from_s5', 'Arranged on S5 web, read-only here', 'S5 वेबवर मांडलेला, इथे फक्त पाहण्यासाठी', 'the overview · under the Plan view'),
 ('album.plan_none', "The flat's plan shows here once its rooms are arranged on S5 web", 'S5 वेबवर खोल्या मांडल्या की फ्लॅटचा प्लॅन इथे दिसेल', 'the overview · the Plan view of a project S5 web has not arranged'),
 ('album.not_on_plan', 'Not on the plan yet:', 'अजून प्लॅनवर नाही:', 'the overview · the Plan view: rooms S5 knows that are not placed yet'),
 ('album.nothing_needs_room', 'Nothing needs a room', 'खोली ठरवायचे काही बाकी नाही', 'the “Needs a room” page, once everything has a room'),
 ('album.not_sent', 'not sent', 'पाठवला नाही', 'the “Needs a room” page · on a photo this phone has not sent'),
 ('album.dismiss', 'Dismiss', 'यादीतून काढा', 'the “Needs a room” page · under a state move or change request S5 refused'),
 ('phase.scope_old_list', 'The scope an older build recorded for the whole project, for reference:', 'जुन्या बिल्डने संपूर्ण प्रोजेक्टसाठी नोंदवलेला स्कोप, संदर्भासाठी:', 'Record the approved scope · the old list beside the sheet'),
 ('phase.scope_marks_hint', "This room's marks in scope:", 'या खोलीचे स्कोपमधील मार्क:', 'Approve · a room’s sixteen ticks: its marks by trade, as a hint'),
]
# [key, EN, MR, placeholders, where]
PHONE_T = [
 ('app.x_rooms_in_progress', '{n} rooms in progress', '{n} खोल्या चालू', {'n': 'd'}, 'the project line: “Execution · 2 rooms in progress · 1 done · 1 on hold”'),
 ('app.x_rooms_done', '{n} rooms done', '{n} खोल्या पूर्ण', {'n': 'd'}, 'the project line, when no room is in progress'),
 ('app.x_rooms_on_hold', '{n} rooms on hold', '{n} खोल्या थांबवलेल्या', {'n': 'd'}, 'the project line, when the rooms on hold come first'),
 ('app.x_rooms_abandoned', '{n} rooms abandoned', '{n} खोल्या सोडून दिलेल्या', {'n': 'd'}, 'the project line, when nothing else is counted'),
 ('app.all_x_rooms_done', 'all {n} rooms done', 'सर्व {n} खोल्या पूर्ण', {'n': 'd'}, 'the project line, finished: “Execution · all 6 rooms done”'),
 ('app.x_wishes', '{n} wishes', '{n} इच्छा', {'n': 'd'}, 'room tile in the wish phases: “3 wishes”'),
 ('album.x_changes_not_sent', '{n} changes not sent', '{n} बदल पाठवले गेले नाहीत', {'n': 'd'}, 'the “Needs a room” row'),
 ('app.x_works', '{n} works', '{n} कामे', {'n': 'd'}, 'a count of works: the folded row, a room of the scope sheet, the APPROVED SCOPE line'),
 ('phase.scope_works_ticked_x', '{n} works ticked', '{n} कामे टिक केली', {'n': 'd'}, 'Approve · the count: “4 rooms · 19 works ticked”'),
 ('refuse.stage.room_unknown_x', 'S5 holds no survey of {room} yet; not saved', '{room} चा सर्व्हे अजून S5 वर नाही; सेव्ह झाले नाही', {'room': 'w'}, 'S5’s refusal of a scope that names a room it does not know (the sheet offers only rooms it knows)'),
 # ---- stage B
 ('album.x_of_x_rooms_done', '{k} of {n} rooms done', '{n} पैकी {k} खोल्या पूर्ण', {'k': 'd', 'n': 'd'}, 'the overview · a work across the flat: “On hold · 0 of 1 rooms done”'),
 ('album.photo_filed_in_x', 'Photo filed in {room}', 'फोटो "{room}" मध्ये ठेवला', {'room': 'w'}, 'the “Needs a room” page · after Which room? on a photo S5 holds'),
 ('album.photo_sent_to_x', 'Photo sent to {room}', 'फोटो "{room}" मध्ये पाठवला', {'room': 'w'}, 'the “Needs a room” page · after Which room? on a photo this phone held'),
 ('phase.scope_copy_from_x', 'Copy the ticks from {room}', '{room} च्या टिक कॉपी करा', {'room': 'w'}, 'Approve · a room’s sixteen ticks: a chip for each other room that has ticks'),
]

# review 1: THE "Marks:" LINE. The project screen's line of mark counts by state ("Marks: 2 existing · 3 scope") is the wish
# round's (port 62), and Version 87's dictionary had only its "Marks: " and the states' names -- so under मराठी the line stayed
# English wherever a mark was past Existing. The demo's surveyed rooms now put it on Demo bathroom door's project screen, so
# its eight counts are added, in the states' own words (life.exist, life.scope, life.*.short).
MARKS_T = [
 ('life.x_exist', '{n} existing', '{n} सध्याचे', {'n': 'd'}, 'the “Marks:” line: marks as surveyed'),
 ('life.x_wish', '{n} wish', '{n} इच्छा', {'n': 'd'}, 'the “Marks:” line: Client wish'),
 ('life.x_vrev', '{n} V review', '{n} व्हें. तपासणी', {'n': 'd'}, 'the “Marks:” line: Vendor review'),
 ('life.x_vok', '{n} V approved', '{n} व्हें. मंजूर', {'n': 'd'}, 'the “Marks:” line: Vendor approved'),
 ('life.x_vadd', '{n} V added', '{n} व्हें. जोडले', {'n': 'd'}, 'the “Marks:” line: Vendor added'),
 ('life.x_cok', '{n} C approved', '{n} ग्रा. मंजूर', {'n': 'd'}, 'the “Marks:” line: Client approved'),
 ('life.x_scope', '{n} scope', '{n} स्कोप', {'n': 'd'}, 'the “Marks:” line: in Scope — “Marks: 2 existing · 3 scope”'),
 ('life.x_drop', '{n} dropped', '{n} वगळले', {'n': 'd'}, 'the “Marks:” line: Dropped'),
]

def js(s):   # a JS single-quoted string literal
    return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"

# ================================================================ 1. the dictionary (RAW): keys inserted in key order, the rest byte for byte
i0 = src.index('const RAW=') + len('const RAW=')
raw, end = json.JSONDecoder().raw_decode(src[i0:])
assert json.dumps(raw, ensure_ascii=False, separators=(',', ':')) == src[i0:i0 + end], 'RAW does not round-trip'
known = {' '.join(e[1].split()) for sec in 'st' for e in raw[sec]}
def add(sec, row):
    keys = [e[0] for e in raw[sec]]
    assert all(row[0] != e[0] for x in 'stnk' for e in raw[x]), row[0]
    assert ' '.join(row[1].split()) not in known, 'English already a key: ' + row[1]
    known.add(' '.join(row[1].split()))
    raw[sec].insert(bisect.bisect_left(keys, row[0]), row)
for k, en, mr, _ in PHONE_S: add('s', [k, en, mr])
for k, en, mr, ph, _ in PHONE_T: add('t', [k, en, mr, ph])
_life = {e[0]: e[2] for e in raw['s'] if e[0].startswith('life.')}
for k, en, mr, ph, _ in MARKS_T:   # each count is worded as its state already is in the dictionary
    w = {'life.x_exist': 'life.exist', 'life.x_scope': 'life.scope', 'life.x_drop': 'life.drop', 'life.x_vrev': 'life.vrev.short', 'life.x_vok': 'life.vok.short', 'life.x_vadd': 'life.vadd.short', 'life.x_cok': 'life.cok.short'}.get(k)
    assert w is None or mr == '{n} ' + _life[w], (k, mr, _life.get(w))
    add('t', [k, en, mr, ph])
# one tour sentence named the Album; the Album is gone (the key keeps its place, its two texts change)
OLD_T7 = "Estimation is the hand-off: nothing new is marked and the wish list is read-only. Once the client approves the estimate this strip folds into the project's one line, and from then on the Album's works carry the project."
NEW_T7 = "Estimation is the hand-off: nothing new is marked and the wish list is read-only. Once the client approves the estimate this strip folds into the project's one line, and from then on each room's works carry the project."
hit = [e for e in raw['s'] if e[1] == OLD_T7]
assert len(hit) == 1, 'tour phases.7: %d' % len(hit)
T7_MR_OLD = hit[0][2]
assert 'अल्बम' in T7_MR_OLD, T7_MR_OLD
hit[0][1] = NEW_T7
hit[0][2] = None   # set below, once the old Marathi has been read
# review 1, finding 5: two counts the line shows after its first part ("… · 2 on hold", "… · 2 abandoned") were worded for works
# (neuter). The line counts ROOMS now and nothing else on the page shows them, so their Marathi agrees with खोल्या (feminine
# plural); one room has its own text above ("1 on hold", "1 abandoned"), which the dictionary reads before a pattern.
# [key, English, the Marathi it had, the Marathi it has, where]
CHANGED_T = [
 ('app.x_on_hold', '{n} on hold', '{n} थांबवलेले', '{n} थांबवलेल्या', 'the project line, after its first part: rooms on hold (“1 room done · 2 on hold”) · was worded for works'),
 ('app.x_abandoned', '{n} abandoned', '{n} सोडून दिलेले', '{n} सोडून दिलेल्या', 'the project line, after its first part: rooms abandoned (“4 rooms done · 2 abandoned”) · was worded for works'),
]
for k, en, mr_old, mr_new, _ in CHANGED_T:
    e = [x for x in raw['t'] if x[0] == k]
    assert len(e) == 1 and e[0][1] == en and e[0][2] == mr_old, 'changed text %s: %r' % (k, e)
    e[0][2] = mr_new
for sec in 'stnk': assert [e[0] for e in raw[sec]] == sorted(e[0] for e in raw[sec])

# ================================================================ 2. CSS
rep('''.otf.dis input{-webkit-text-fill-color:rgba(29,27,32,.38);opacity:1}
''', '''.otf.dis input{-webkit-text-fill-color:rgba(29,27,32,.38);opacity:1}
/* ================= v62b · the room is the scope of work (mock-up v62b -- to approve, not on the phone). .m3 tokens and the
   album's own rules: a work's row in its room IS the album's chapter (.alb59ch .alb60chh .alb60st .alb60cr), a photo its card
   (.alb59c), the one-line notices its lock banner (.alb60lockbanner), the project screen's two rows its log toggle (.alb59logt)
   -- "Needs a room" in the tone of its "To review" line. What is new is where they sit; no font-family. ================= */
.rm62{flex:1 1 0;min-height:0;min-width:0;display:flex;flex-direction:column;position:relative;background:var(--bgc)}
.rm62caps{font-size:0;line-height:0}                 /* this phone's capture tiles: the rooms grid's own inline flow, as .vs.tiles */
.rm62 .alb59log{max-height:none}                     /* the room is one list: its log does not scroll inside it */
.rm62none{padding:12px 16px}
.alb59ch[data-scope="out"] .alb59cn,.alb59ch[data-scope="out"] .alb59cc{color:var(--onsv)}   /* a work outside the room's scope: greyed, as a chapter that is not the viewer's */
.rm62foldb{background:var(--sc0)}
.scaf.land .rm62 .alb59grid{grid-template-columns:repeat(4,minmax(0,1fr))}
.ov62rows{flex:none;min-width:0}
.ov62row{flex:none;border-bottom:1px solid var(--olv)}
.scaf.land .ov62rows{display:flex}                    /* landscape: the two rows share one line -- the overview's as wide as its words, the other the rest */
.scaf.land .ov62rows .ov62row{flex:0 0 auto;width:auto;gap:16px}
.scaf.land .ov62rows .ov62row:last-child{flex:1 1 0;min-width:0}
.nr62row{background:#FFE3B0;color:#5C3A00}
.nr62row .alb59car{color:#5C3A00}
.ov62{flex:1 1 0;min-height:0;min-width:0;display:flex;flex-direction:column;background:var(--bgc)}
/* THE OVERVIEW (3.5), sketch A: gutter 8 + token 36 + 16 x 17 + count 36 + gutter 8 = 360 dp. Type: the label size (.ls), the
   album's own head lines (.alb60as .alb60rvq .alb60crq .alb59q), a work's row (.alb59ch) and log (.alb59log). The state
   glyphs are drawn, 11 dp, in the tones the state pill uses (.alb60st): Done primary, On hold amber, Abandoned muted. */
.ov62.land[data-ov62page="overview"]{display:grid;grid-template-columns:361px minmax(0,1fr);grid-template-rows:minmax(0,1fr)}   /* 360 dp for the grid + its 1 dp rule; "Needs a room" stays one list */
.ov62a,.ov62b{min-height:0;min-width:0;display:flex;flex-direction:column}
.ov62.land .ov62a{border-right:1px solid var(--olv)}
.ov62h{flex:none;min-width:0}
.ov62 .alb60rvq,.ov62 .alb60crq,.ov62 .alb59q{overflow-wrap:anywhere}
.ov62 .alb59q.ov62sc{padding-top:0}                  /* the approved scope's count: the second line of the head's band, under the project line */
.ov62 .alb59log{max-height:none}
.ov62g{flex:none;width:360px;max-width:100%;margin:0 auto}
.ov62r{display:grid;grid-template-columns:36px repeat(16,17px) 36px;align-items:center;width:100%;height:32px;padding:0 8px;box-sizing:border-box;text-align:left;border-bottom:1px solid var(--olv);color:var(--on);min-width:0}
.ov62r.hd{height:20px;color:var(--onsv);border-bottom-color:var(--ol)}
.ov62r>span{display:flex;align-items:center;justify-content:center;min-width:0;white-space:nowrap}
.ov62r>.ov62tk{display:block;overflow:hidden;text-overflow:ellipsis}
.ov62r>.ov62n{justify-content:flex-end}
.ov62gl{flex:none;display:block}
.ov62c[data-s="ns"]{color:var(--onsv)}
.ov62c[data-s="done"]{color:var(--pri)}
.ov62c[data-s="oh"]{color:#B07A00}
.ov62c[data-s="ab"]{color:var(--onsv);opacity:.75}
.ov62c[data-s="out"]{color:var(--olv)}
.ov62x{font-style:normal;font-weight:700;font-size:11px;line-height:16px;color:#B0740E;margin-left:1px}
.ov62lg{display:flex;flex-wrap:wrap;gap:2px 12px;padding:8px 16px 10px;color:var(--onsv)}
.ov62lg>span{display:inline-flex;align-items:center;gap:4px;white-space:nowrap}
.ov62lg .ov62c{display:inline-flex;align-items:center}
.ov62lg .ov62c[data-s="out"]{color:var(--onsv)}
.ov62w .alb59chh{cursor:default}
.ov62vc{flex:none}
/* the Plan view (question 7, recommended): the arranger's paper, the rooms in place, each in its state's pill tones */
.ov62pw{flex:none;display:flex;justify-content:center;padding:8px 8px 0}
.ov62pl{position:relative;flex:none;background:#F7F6F1}
.ov62pc{position:absolute;box-sizing:border-box;display:flex;flex-direction:column;justify-content:center;align-items:flex-start;gap:0;padding:0 6px;min-width:0;overflow:hidden;text-align:left;background:var(--bgc);color:var(--on);box-shadow:inset 0 0 0 1px var(--ol)}
.ov62pc[data-s="ip"]{background:var(--tc);color:var(--ontc)}
.ov62pc[data-s="done"]{background:var(--pri);color:var(--onpri)}
.ov62pc[data-s="oh"]{background:#FFE3B0;color:#5C3A00}
.ov62pc[data-s="ab"]{background:var(--sv);color:var(--onsv)}
.ov62pc[data-s="none"]{background:var(--sc0);color:var(--onsv);box-shadow:inset 0 0 0 1px var(--olv)}
.ov62pt{font-weight:700;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.ov62pv{display:flex;align-items:center;gap:4px;white-space:nowrap;max-width:100%;min-width:0}
.ov62pc .ov62x{color:inherit;margin-left:0}
.ov62pc.sm{padding:0 2px;align-items:center}
.ov62pc.sm .ov62pv{gap:2px}
.ov62pn{padding:8px 16px 0}
/* "Needs a room" (3.7): the album's card and its two-button column, the log's rows; Dismiss is a text button on its own line */
.ov62.land .alb59grid{grid-template-columns:repeat(4,minmax(0,1fr))}
.nr62dl{padding-top:4px}
.nr62da{display:flex;justify-content:flex-end}
.sc62cp{padding:2px 12px 6px}
/* the room tile's own line (3.1): the room's roll-up and "2/8" over a thin bar; "3 wishes" in the wish phases */
.tile .rt62{position:absolute;left:7px;right:7px;bottom:31px;display:flex;align-items:baseline;gap:6px;min-width:0}
.tile .rt62s{flex:1 1 auto;min-width:0}
.tile .rt62n{flex:none}
.tile .rt62b{position:absolute;left:7px;right:7px;bottom:26px;height:3px;border-radius:2px;background:rgba(29,27,32,.12);overflow:hidden}
.tile .rt62b i{display:block;height:100%;border-radius:2px;background:var(--pri)}
.tile.ph62 .rt62b{background:rgba(255,255,255,.35)}
.tile.ph62 .rt62b i{background:#fff}
''')
# the arranger page's own rules go with the page (its tile, its paper, its foot)
cut('''
/* ================= v54 BUILDER D: THE FLOOR-PLAN ARRANGER (arrange54.js). Inside the phone frame only, on the app's light M3''',
    '''.scaf.land .fp54ns.all{-webkit-line-clamp:unset}
''', '')
rep('''#device[lang="mr"] .fp54svg text,#device[lang="mr"] #t55{font-family:Roboto,'Noto Sans Devanagari',system-ui,sans-serif}''',
    '''#device[lang="mr"] #t55{font-family:Roboto,'Noto Sans Devanagari',system-ui,sans-serif}''')
rep('''#device[lang="mr"] .fp54svg text{font-family:Roboto,'Noto Sans Devanagari',system-ui,sans-serif}
#device[lang="mr"] .fp54rot{font-family:Roboto,'Noto Sans Devanagari',system-ui,sans-serif}
#device[lang="mr"] .fp54b{font-family:Roboto,'Noto Sans Devanagari',system-ui,sans-serif}
''', '')

# ================================================================ 3. P54: the reseed, the line, the scope per room
rep('''const lensOn=()=>store.lens==='v53';
function persist(){ try{ localStorage.setItem(LS,JSON.stringify(store)); }catch(_){} }
''', '''const lensOn=()=>store.lens==='v53';
function persist(){ try{ localStorage.setItem(LS,JSON.stringify(store)); }catch(_){} }
// v62b: the demo's S5 data is re-seeded per room -- Demo flat interiors is in execution, Demo bathroom door at Revisions, and
// Sample wardrobe repair approved before the scope was per room. A stage an earlier mock-up kept on this browser for those
// three would put them somewhere their seed does not fit, so it is dropped ONCE (nothing else of the store is touched; a move made from now on is kept as before).
if(store.seed!=='v62b'){ if(store.moves&&typeof store.moves==='object'){ delete store.moves['PROJ-DEMO']; delete store.moves['PROJ-DEMO-2']; delete store.moves['PROJ-DEMO-3']; } store.seed='v62b'; persist(); }
''')
rep('''  const C=window.ALB60, n=st=>C&&C.WORKS&&C.stateOf?C.WORKS.filter(([id])=>C.stateOf(p,id)===st).length:0;
  let parts=[[n('In progress'),'in progress'],[n('Done'),'done'],[n('On hold'),'on hold']].filter(x=>x[0]>0).map(([k,w])=>`${k} ${w}`);
  // §13.4 (Amit, 27 Oct ~10:15 IST): never a bare "Execution". "no work started yet" ONLY while every work is Not started
  // (v61c review 4: a work started and then abandoned WAS started); with nothing else counted, the abandoned ones are named
  if(!parts.length){ const ab=n('Abandoned'); parts=ab?[`${ab} abandoned`]:['no work started yet']; }
  return {kind:'exec',quote:p.quote,text:['Execution'].concat(parts).join(' · '),parts}; }''',
    '''  // v62b (room-centred-album.md §2; question 3 at its recommended option): the line counts ROOMS, each by its own roll-up over
  // the works in its approved scope -- "Execution · 2 rooms in progress · 1 done · 1 on hold" (ALB60.lineParts).
  // §13.4 (Amit, 27 Oct ~10:15 IST) stands: never a bare "Execution". "no work started yet" ONLY while every cell is Not
  // started (v61c review 4: a room started and then abandoned WAS started); with nothing else counted, the abandoned rooms are named
  const C=window.ALB60, parts=C&&C.lineParts?C.lineParts(p):['no work started yet'];
  return {kind:'exec',quote:p.quote,text:['Execution'].concat(parts).join(' · '),parts}; }''')
rep('''// the project line (projectLine) is that state -- the quote stage until Client approved, then the Album's works -- and the''',
    '''// the project line (projectLine) is that state -- the quote stage until Client approved, then the rooms' works (v62b) -- and the''')
rep('''// ONE derived line, never set by hand: 'Quote · <stage>' through the quote journey, then 'Execution' and the Album's
// non-zero counts (in progress · done · on hold), or 'no work started yet' (§13.4). Its four readers, and nothing else
// says where a project is: the project screen's line, the project list row, the album header, the Wish list bar.''',
    '''// ONE derived line, never set by hand: 'Quote · <stage>' through the quote journey, then 'Execution' and the non-zero
// counts of ROOMS by their roll-up (in progress · done · on hold; v62b), or 'no work started yet' (§13.4). Its readers, and
// nothing else says where a project is: the project screen's line, the project list row, the overview's bar, the Wish list bar.''')
# the S5 stand-in's guarded PUT …/stage: scope = [{room, works}]
rep('''      if((Array.isArray(o.scope)&&!o.scope.length)||(o.scope===undefined&&!(window.ALB60&&window.ALB60.scopeOf(p)))) return refuse('scope_missing',SCOPE_MISSING_MSG); }''',
    '''      if((Array.isArray(o.scope)&&!cells62(o.scope).length)||(o.scope===undefined&&!(window.ALB60&&window.ALB60.scopeOf(p)))) return refuse('scope_missing',SCOPE_MISSING_MSG);
      { const bad=unknown62(p,o.scope); if(bad) return refuse('room_unknown',roomUnknownMsg(bad)); } }   // v62b: every room S5 is sent is one it knows''')
rep('''    request:p.serverId?{method:'PUT',path:`/api/asis/${encodeURIComponent(p.serverId)}/stage`,body:Object.assign({stage:q,base},sc?{scope:o.scope.slice(),scope_base:(window.ALB60&&window.ALB60.scopeAt(p))||''}:{})}:null};''',
    '''    request:p.serverId?{method:'PUT',path:`/api/asis/${encodeURIComponent(p.serverId)}/stage`,body:Object.assign({stage:q,base},sc?{scope:cells62(o.scope),scope_base:(window.ALB60&&window.ALB60.scopeAt(p))||''}:{})}:null};''')
rep('''const SCOPE_MISSING_MSG='Tick the works in the approved estimate before moving to Client approved';''',
    '''const SCOPE_MISSING_MSG='Tick the works in the approved estimate before moving to Client approved';
// v62b: the approved scope is PER ROOM -- [{room, works}], rooms as an array (never an object keyed by a free-text name), each
// a room S5 knows (ALB60.known). cells62 keeps the rooms that carry a tick; unknown62 names the first room S5 does not know.
const cells62=sc=>Array.isArray(sc)?sc.filter(c=>c&&typeof c.room==='string'&&c.room&&Array.isArray(c.works)&&c.works.length).map(c=>({room:c.room,works:c.works.slice()})):[];
const unknown62=(p,sc)=>{ const A6=window.ALB60; if(!A6||!A6.known) return null; const c=cells62(sc).find(x=>!A6.known(p,x.room)); return c?c.room:null; };
const roomUnknownMsg=room=>`S5 holds no survey of ${room} yet; not saved`;''')
rep('''  if(!Array.isArray(ticks)||!ticks.length) return refuse('scope_missing',SCOPE_MISSING_MSG);
  if(!isPM()) return refuse('pm_only',PM_SCOPE_MSG);''',
    '''  if(!cells62(ticks).length) return refuse('scope_missing',SCOPE_MISSING_MSG);
  if(!isPM()) return refuse('pm_only',PM_SCOPE_MSG);
  { const bad=unknown62(p,ticks); if(bad) return refuse('room_unknown',roomUnknownMsg(bad)); }''')
rep('''body:{works:ticks.slice(),base:(window.ALB60&&window.ALB60.scopeAt(p))||''}}:null};
  log.push(rec); if(window.ALB60) window.ALB60.recordScope(p,ticks,at); msg=null; return rec; }''',
    '''body:{scope:cells62(ticks),base:(window.ALB60&&window.ALB60.scopeAt(p))||''}}:null};
  log.push(rec); if(window.ALB60) window.ALB60.recordScope(p,cells62(ticks),at); msg=null; return rec; }''')
rep('''  if(sc&&window.ALB60) window.ALB60.recordScope(p,o.scope,at);''', '''  if(sc&&window.ALB60) window.ALB60.recordScope(p,cells62(o.scope),at);''')
# the opened quote view's APPROVED SCOPE row: works and rooms; a tap opens the overview (the one place the scope is read)
rep('''  if(op56&&K&&K.openScope){ const A6=window.ALB60, sc=A6?A6.scopeOf(p):null, at=A6&&sc?A6.scopeAt(p):'';   // v61c: the approved scope
    h+=`<div class="p54x p61sc" data-p61sc="${sc?sc.length:''}"><span class="ls p54xl">APPROVED SCOPE</span>`+
      (sc?`<span class="bs rip p61scv" data-p61scv="1"${A(()=>{ K.openScope(p,'view'); })}>${esc(`${sc.length} of ${A6.WORKS.length} works`+(at?` · since ${A6.whenOf(at)}`:'')+' · tap to view')}</span>`''',
    '''  if(op56&&K&&K.openScope){ const A6=window.ALB60, sc=A6?A6.scopeCount(p):null, at=A6&&sc?A6.scopeAt(p):'';   // v61c: the approved scope · v62b: per room -- its cells and its rooms; the tap opens the overview, the one place the scope is read
    h+=`<div class="p54x p61sc" data-p61sc="${sc?sc.works:''}"${sc?` data-p62rooms="${sc.rooms}"`:''}><span class="ls p54xl">APPROVED SCOPE</span>`+
      (sc?`<span class="bs rip p61scv" data-p61scv="1"${A(()=>{ K.openScope(p,'view'); })}>${esc(`${plural(sc.works,'work')} · ${plural(sc.rooms,'room')}`+(at?` · since ${A6.whenOf(at)}`:'')+' · tap to view')}</span>`''')

# ================================================================ 4. the arranger page leaves the phone mock-up (3.1); FP54, the pure geometry, stays
rep('''// ================================================================ THE FLOOR-PLAN ARRANGER, STAGE 1 (v54, BUILDER D)
''', '''// ================================================================ THE FLOOR-PLAN ARRANGER, STAGE 1 (v54, BUILDER D)
// v62b (room-centred-album.md §3.1): THE PAGE AND ITS TILE ARE REMOVED FROM THE PHONE MOCK-UP. Arranging the rooms into the
// flat's plan is S5 web's (Amit, 11 Nov 2026: "arranging a floor plan will be done in s5 rather than apk"), and a Floor plan
// tile beside the room tiles is the grid he called unrelated. What stays is window.FP54, the pure geometry below, as the
// source S5 web's arranger is ported from; where the phone later shows the finished plan, read-only, is his question 7.
// What follows describes the page as v54 built it (it is in Version 87 and before).
''')
cut('''  // ---------------------------------------------------------------- THE PAGE (a factory, built by app.js with its kit)
  window.__mcftPlan54=function(K){''', '''  if(window.__appPlanReady) window.__appPlanReady();
''', '')

# ================================================================ 5. the two modules: the core by cell, the room's home
cut('''// ================================================================ v60: THE ALBUM BY WORKS -- window.ALB60 (the pure core)''',
    '''window.__alb60Test=window.ALB60.test;
''', open(SRC + 'core62b.js', encoding='utf-8').read())
cut('''// ================================================================ v60: THE ALBUM BY WORKS -- the page, window.__mcftAlbum60(K)''',
    '''  return {tile,screen,ov,open,leave,back};
};
''', open(SRC + 'room62b.js', encoding='utf-8').read())

exec(open(S + '/proto88/work/build88_app.py', encoding='utf-8').read())
exec(open(S + '/proto88/work/build88_clean.py', encoding='utf-8').read())   # before the notes card, which counts what it dropped
exec(open(S + '/proto88/work/build88_page.py', encoding='utf-8').read())

# ================================================================ the dictionary goes back, with the tour sentence's Marathi
T7_MR_NEW = T7_MR_OLD.replace(T7_FROM, T7_TO)
assert T7_MR_NEW != T7_MR_OLD, 'the tour sentence: its Marathi did not change: ' + T7_MR_OLD
hit[0][2] = T7_MR_NEW
i0 = src.index('const RAW=') + len('const RAW=')
_, end = json.JSONDecoder().raw_decode(src[i0:])
src = src[:i0] + json.dumps(raw, ensure_ascii=False, separators=(',', ':')) + src[i0 + end:]
rep(js(OLD_T7), js(NEW_T7))
# no step of the six flows that remain names the Album or the Floor plan (the one that did, phases.7, is re-worded just
# above; the Floor plan flow is cut in build88_page.py), and the dictionary keeps none of the cut flow's texts
_a = src.index("{id:'phases', name:'The phases'"); _b = src.index('// (v62b: the sixth flow, Floor plan, went with the Floor plan page')
_flows = src[_a:_b]
assert _flows.count("{id:'") >= 6 and not re.search(r'[Aa]lbum|[Ff]loor plan', _flows), 'a tour step still names the Album or the Floor plan: ' + str(re.findall(r'.{60}(?:[Aa]lbum|[Ff]loor plan).{40}', _flows)[:3])

# ================================================================ the header invariants
assert src.count(EYE4) == 1, 'the 4th eyebrow line moved'
assert src.count('0.3.318') == 2, 'the build is named %d times' % src.count('0.3.318')
assert len(re.findall(r'<span class="eyeln">\d{1,2} [A-Z][a-z]{2} 2026, \d\d:\d\d IST</span>', src)) == 1
assert src.count('<b>mock-up v62b</b>') == 1 and src.count('mock-up v62a</b>') == 0
open(OUT, 'w', encoding='utf-8').write(src)
print('built', OUT, hashlib.md5(src.encode('utf-8')).hexdigest(), len(src.splitlines()), 'lines · stamp', STAMP)
