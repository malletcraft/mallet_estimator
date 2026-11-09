# refresh.py RUN "HH:MM" "HH:MM IST" [BULLET_EXTRA]   (env HDR_DATE, e.g. "9 Nov 2026": the build date, default the page's; env HDR_SUBS: a JSON list of [old, new] pairs, each matched once — a changed clause or a changed phrase of the bullet) — the header refresh by DIRECT SUBSTITUTION on the recovered Version 75 page
# (the one-constant build system was lost with the scratchpad on 3 Nov 2026). Exactly three substitutions, each matched once:
#   1. the build-time span   <span class="eyeln">D Mon 2026, HH:MM IST</span>
#   2. the eyebrow's 4th line phone APK <b>0.3.NNN</b>
#   3. the #ch61c bullet      APK 0.3.NNN, from Google Play internal testing  (+ an optional sentence appended before "Claude updates")
# Writes v61cbuild/index61c2.html from v61cbuild/tmp/hdr6/base.html (the page as last published), prints the proof, refuses if any
# spot is not matched exactly once. Nothing else on the page is touched — the clauses stay as they are.
import sys, re, hashlib, os, json
H = '/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/v61cbuild/'
run = int(sys.argv[1]); at = sys.argv[2]; play = sys.argv[3]; extra = sys.argv[4] if len(sys.argv) > 4 else ''
ver = f'0.3.{run}'
old = open(H + 'tmp/hdr6/base.html', encoding='utf-8').read()
t = old
m = re.findall(r'<span class="eyeln">(\d{1,2} [A-Z][a-z]{2} 20\d\d), (\d\d:\d\d) IST</span>', t); assert len(m) == 1, ('build time', m)
old_date, old_at = m[0]
new_date = os.environ.get('HDR_DATE', old_date)
assert re.fullmatch(r'\d{1,2} [A-Z][a-z]{2} 20\d\d', new_date), ('HDR_DATE', new_date)
m = re.findall(r'phone APK <b>(0\.3\.\d+)</b>', t); assert len(m) == 1, ('4th line', m)
old_ver = m[0]
assert t.count(f'APK {old_ver}, from Google Play internal testing') == 1, 'bullet'
assert t.count(old_ver) == 2, ('the old build is named exactly twice', t.count(old_ver))
subs = [(f'<span class="eyeln">{old_date}, {old_at} IST</span>', f'<span class="eyeln">{new_date}, {at} IST</span>', 'the build time'),
        (f'phone APK <b>{old_ver}</b>', f'phone APK <b>{ver}</b>', "the eyebrow's 4th line"),
        (f'APK {old_ver}, from Google Play internal testing', f'APK {ver}, from Google Play internal testing', '#ch61c phone-line bullet')]
if extra:
    a = ' Claude updates this line with every Play publish.'
    assert t.count(a) == 1, 'bullet tail'
    subs.append((a, ' ' + extra.strip() + a, 'the bullet sentence added'))
for i, (a2, b2) in enumerate(json.loads(os.environ.get('HDR_SUBS', '[]'))):   # extra exact pairs (a changed clause, a changed phrase)
    assert a2 and a2 != b2 and old_ver not in a2 and ver not in b2, ('HDR_SUBS pair', i)
    subs.append((a2, b2, f'HDR_SUBS pair {i + 1}'))
for a, b, w in subs:
    n = t.count(a); assert n == 1, (w, n); t = t.replace(a, b); print(f'1 substitution: {w}: -{len(a.encode())} +{len(b.encode())} bytes')
assert t.count(ver) == 2 and old_ver not in t, 'the new build is named exactly twice and the old one nowhere'
open(H + 'index61c2.html', 'w', encoding='utf-8').write(t)
print('old md5', hashlib.md5(old.encode()).hexdigest(), '->', 'new md5', hashlib.md5(t.encode()).hexdigest(), '| bytes', len(old.encode()), '->', len(t.encode()), f'({len(t.encode()) - len(old.encode()):+d})', '| play:', play)
