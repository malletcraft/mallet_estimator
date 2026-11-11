# proto88 · build88.py's clean-up part: what v62b made dead goes. exec'd by build88.py: `src`, rep() and `raw` are its.

# ================================================================ 17. the Album page's own CSS (its tile, its two-column page, its head, the
# quote-stage groups of its chapter 0, its end line, its "Progress shot" button, its "Open:" block). Every rule below was
# used by v62a's Album page and by nothing in v62b; the rules the room and the overview still use (.alb59ch, .alb59c,
# .alb59log, .alb60st, .alb60cr, the viewer, the camera ...) stay. Each line is cut by exact match, once.
DEAD_CSS = [
    '.tile.alb59t{background:var(--tc);color:var(--ontc)}',
    '.alb59t .alb59g{position:absolute;left:8px;top:8px;color:var(--ontc)}',
    '.alb59t .alb59tl{position:absolute;left:7px;bottom:7px;width:72%;color:var(--ontc)}',
    '.alb59t .alb59tn{position:absolute;right:7px;bottom:7px;color:var(--ontc)}',
    '.alb59{flex:1 1 0;min-height:0;min-width:0;display:flex;flex-direction:column;position:relative}',
    '.alb59.land{display:grid;grid-template-columns:248px minmax(0,1fr);grid-template-rows:minmax(0,1fr)}',
    '.alb59h{flex:none;background:var(--bgc);min-width:0}',
    '.alb59.land .alb59h{overflow-y:auto;overflow-x:hidden;scrollbar-width:none;border-right:1px solid var(--olv);min-height:0}',
    '.alb59main{flex:1 1 0;min-height:0;min-width:0;display:flex;flex-direction:column;position:relative}',
    '.alb59.land .alb59log{max-height:none}',
    '.alb59empty{padding:16px}',
    '.alb59sg{padding:0 0 8px}',
    '.alb59sgh{display:flex;align-items:baseline;gap:8px;padding:6px 12px 4px 46px;min-width:0}',
    '.alb59sgh .ll{flex:0 1 auto;min-width:0}',
    '.alb59gc{margin-left:auto;flex:none;color:var(--onsv)}',
    '.alb59now{flex:none;border-radius:8px;padding:0 6px;background:var(--pri);color:var(--onpri);font-size:11px;line-height:16px;font-weight:500}',
    '.alb59es{display:flex;align-items:center;gap:8px;min-height:28px;padding:0 12px 0 46px;color:var(--onsv);min-width:0}',
    '.alb59es .ell{flex:0 1 auto;min-width:0}',
    '.alb59.land .alb59grid{grid-template-columns:repeat(3,minmax(0,1fr))}',
    '.alb59end{display:flex;align-items:center;justify-content:center;gap:4px;min-height:48px;margin:8px 12px 0;border-top:2px solid var(--ol);color:var(--onsv)}',
    '.alb59end[data-alb59end="closed"]{color:var(--pri);border-top-color:var(--pri)}',
    '.alb59fab{position:absolute;right:16px;bottom:16px;height:56px;border-radius:16px;background:var(--pc);color:var(--onpc);display:flex;align-items:center;gap:12px;padding:0 20px 0 16px;box-shadow:0 1px 3px rgba(0,0,0,.3),0 4px 8px 3px rgba(0,0,0,.15);z-index:2}',
    '.alb59.land .alb59fab{right:12px;bottom:12px}',
    '.alb60open{background:var(--pc);color:var(--onpc);padding:10px 16px;min-width:0}',
    '.alb60open .ts{overflow-wrap:anywhere}',
]
for ln in DEAD_CSS: rep(ln + '\n', '')

# ================================================================ 18. the dictionary: the texts of the removed pages and of the removed tour
# (checked one by one: none is drawn by anything left on the page, and no code names a key). 'Floor plan' itself stays
# (view.floor_plan, survey prep's own tab, with the same Marathi as tour.plan.name had).
DROP_S = ['plan.no_room_size', 'plan.reset_layout', 'plan.stage_1_hand_door', 'plan.back_start_row_every', 'refuse.plan.no_room_has_survey', 'refuse.plan.nothing_arrange_yet',
          'album.aria.take_progress', 'album.closed_cleaning', 'album.closes_cleaning', 'album.empty', 'album.no_open_work', 'album.not_reached',
          'tour.plan.1.body', 'tour.plan.1.title', 'tour.plan.2.body', 'tour.plan.2.title', 'tour.plan.3.body', 'tour.plan.3.title', 'tour.plan.4.body', 'tour.plan.4.title',
          'tour.plan.5.body', 'tour.plan.5.title', 'tour.plan.6.body', 'tour.plan.6.title', 'tour.plan.line', 'tour.plan.name']
DROP_T = ['album.aria.tile_x', 'album.sub_x', 'album.title_x', 'phase.scope_since_x', 'plan.aria.floor_plan_arrange_x', 'plan.aria.floor_plan_x_drag', 'plan.floor_plan_x',
          'plan.not_surveyed_yet_x', 'plan.turn_x_quarter_turn', 'plan.turned_x', 'plan.x_drag_move_pinch', 'plan.x_overlap', 'plan.x_surveyed_x']
_fp = [e for e in raw['s'] if e[0] in ('tour.plan.name', 'view.floor_plan')]
assert len(_fp) == 2 and _fp[0][1] == _fp[1][1] and _fp[0][2] == _fp[1][2], 'Floor plan: the two keys differ'
for sec, keys in (('s', DROP_S), ('t', DROP_T)):
    have = [e[0] for e in raw[sec]]
    for k in keys: assert have.count(k) == 1, 'dictionary key ' + k
    raw[sec] = [e for e in raw[sec] if e[0] not in keys]
assert not [e[0] for e in raw['s'] if e[0].startswith('tour.plan.')]
