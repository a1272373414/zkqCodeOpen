import sys, os, cv2
from game_state import parse_file, find_first, OUT

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
feats = parse_file(KT)
print('features:', len(feats))
for n in sys.argv[1:]:
    img = cv2.imread(os.path.join(OUT, n))
    hits = []
    for f in feats:
        h = find_first(img, [f])
        if h:
            hits.append('%s@%d,%d' % (h[0], h[1], h[2]))
    print('== %s: %d/%d hits' % (n, len(hits), len(feats)))
    print('   ' + ' '.join(hits))
