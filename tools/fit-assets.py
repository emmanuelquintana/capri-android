#!/usr/bin/env python3
"""Fits the drawings made with GPT (Capri-<version>-assets.zip, unpacked) to the app.

    python3 tools/fit-assets.py <unpacked folder> [only-these-names...]

- outfit-<id>.png  (her whole body, drawn over capri-cutout.png): to her 240x420 picture, with her
  red and green as dark as in capri-full.png (her shader brightens them) and without the hanging
  right arm (her right arm is her left one mirrored, sprite.js).
- acc-<id>.png     trimmed, at most 256 px.
- face-<name>.png  256x256, like her other faces.        cutin-<name>.png 360x240.
- pose-<name>.png  trimmed, at most 640 px (shown as they are, not through her shader).
- wedding-*.png    at most 1000 px wide, as .jpg.
- anim-<name>.png  a sheet of cels (capri-prompts-1.8.0-parte3-animaciones.zip, grid in ANIMS): cut, all cut
  to the same box (so she doesn't jump between cels), into one strip, anims/<name>.png, and its line
  in anims/anims.json (cels, fps, loops, stand / sit / lie / curl).
"""
import json
import os
import sys
from collections import deque

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'assets', 'capri', 'assets', 'capri')
WARDROBE = os.path.join(ROOT, 'wardrobe')
POSES = os.path.join(ROOT, 'poses')
ANIMS_DIR = os.path.join(ROOT, 'anims')
# name: (columns, rows, cels per second, loops, how she is in it: stand / sit / lie)
ANIMS = {
    'gamer-watch': (3, 2, 6, True, 'lie'),
    'gamer-play': (2, 2, 6, True, 'sit'),
    'sleep': (2, 2, 3, True, 'curl'),
    'wake': (4, 1, 6, False, 'stand'),
    'wave': (4, 1, 8, True, 'stand'),
    'dance': (4, 2, 8, True, 'stand'),
    'laugh': (4, 1, 8, True, 'stand'),
    'cry': (4, 1, 6, True, 'sit'),
    'angry': (4, 1, 8, True, 'stand'),
    'love': (4, 1, 6, True, 'stand'),
    'shy': (4, 1, 6, True, 'stand'),
    'think': (4, 1, 4, True, 'stand'),
    'bored-tap': (2, 2, 4, True, 'lie'),
    'hug-rock': (4, 1, 4, True, 'sit'),
    'lean-nails': (4, 1, 4, True, 'stand'),
    'phone': (2, 2, 4, True, 'lie'),
    'charge': (4, 1, 6, True, 'stand'),
    'drained': (4, 1, 6, False, 'stand'),
    'eat': (4, 1, 6, True, 'sit'),
    'sandevistan': (3, 2, 10, False, 'stand'),
    # part 5
    'scan': (4, 1, 6, True, 'stand'),
    'bye': (4, 1, 8, False, 'stand'),
    'dizzy': (4, 1, 6, True, 'sit'),
    'music': (2, 2, 6, True, 'sit'),  # (redone like gamer-play: 2x2)
    'stretch': (4, 1, 4, True, 'stand'),
    'yawn': (4, 1, 6, False, 'stand'),
    'hack': (4, 1, 8, True, 'stand'),
    'surprised': (4, 1, 8, False, 'stand'),
    'selfie': (4, 1, 4, True, 'stand'),
    'guitar': (4, 2, 8, True, 'stand'),
    # 1.9.0
    'listen': (4, 1, 5, True, 'stand'),
    'stream': (4, 1, 5, True, 'stand'),
    'birthday': (4, 1, 5, True, 'stand'),
    'party': (4, 2, 8, True, 'stand'),
}
# the cel where she just stands there, for the ones she jumps or reaches up in: her real height is that cel's,
# not the whole box's (else she looks smaller in them than in the rest)
REST = {'surprised': 3, 'stretch': 3}
# sitting ones GPT drew a little higher in some rows: each cel's seat put on the same line
SEAT = {'music'}
ACC_IDS = {'cat-ears': 'catears', 'santa-hat': 'santahat', 'hat-cowboy': 'cowboyhat', 'heart-balloon': 'balloon'}

FULL = Image.open(os.path.join(ROOT, 'capri-full.png')).convert('RGBA')
CUT = Image.open(os.path.join(ROOT, 'capri-cutout.png')).convert('RGBA')


def hsv(im):
    return np.array(im.convert('RGB').convert('HSV')).astype(float) / 255


def tones(im):
    """mean saturation and value of her red and her green"""
    a = np.array(im)[..., 3] > 128
    h = hsv(im)
    red = a & ((h[..., 0] > 0.85) | (h[..., 0] < 0.05)) & (h[..., 1] > 0.3)
    green = a & (h[..., 0] > 0.22) & (h[..., 0] < 0.5) & (h[..., 1] > 0.3)
    return {k: (h[..., 1][m].mean(), h[..., 2][m].mean()) for k, m in (('red', red), ('green', green)) if m.any()}


BASE = tones(FULL)
# the hanging right arm, taken out of capri-full.png (a bit wider: GPT's sleeve is never exactly there)
_removed = (np.array(CUT)[..., 3] > 60) & (np.array(FULL)[..., 3][:, :220] < 30)
ARM = np.zeros((420, 240), bool)
ARM[:, :220] = _removed
for _ in range(3):
    ARM = ARM | np.roll(ARM, 1, 0) | np.roll(ARM, -1, 0) | np.roll(ARM, 1, 1) | np.roll(ARM, -1, 1)
ARM[:, :128] = False  # never into her body


def darken(im):
    """her red and her green as dark as capri-full.png's"""
    t = tones(im)
    arr = np.array(im).astype(float)
    h = hsv(im)
    for k, lo, hi in (('red', 0.85, 0.05), ('green', 0.22, 0.5)):
        if k not in t:
            continue
        ks, kv = BASE[k][0] / t[k][0], BASE[k][1] / t[k][1]
        if k == 'red':
            m = ((h[..., 0] > lo) | (h[..., 0] < hi)) & (h[..., 1] > 0.25)
        else:
            m = (h[..., 0] > lo) & (h[..., 0] < hi) & (h[..., 1] > 0.25)
        h[..., 1][m] = np.clip(h[..., 1][m] * ks, 0, 1)
        h[..., 2][m] = np.clip(h[..., 2][m] * kv, 0, 1)
    rgb = np.array(Image.fromarray((h * 255).astype('uint8'), 'HSV').convert('RGB'))
    arr[..., :3] = rgb
    return Image.fromarray(arr.astype('uint8'), 'RGBA')


# a hanging hand GPT drew lower than her own, still touching her (x0, y0, x1, y1 and the hue range of that hand)
HANDS = {'rebecca': ((135, 240, 200, 315), (0.55, 0.72))}


def pieces(mask):
    """each connected piece of the picture, by number"""
    h, w = mask.shape
    lab = np.zeros(mask.shape, int)
    n = 0
    for y, x in zip(*np.nonzero(mask)):
        if lab[y, x]:
            continue
        n += 1
        lab[y, x] = n
        q = deque([(y, x)])
        while q:
            cy, cx = q.popleft()
            for ny in (cy - 1, cy, cy + 1):
                for nx in (cx - 1, cx, cx + 1):
                    if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not lab[ny, nx]:
                        lab[ny, nx] = n
                        q.append((ny, nx))
    return lab, n


FULL_A = np.array(FULL)[..., 3] > 30


def leftovers(arr):
    """what's left of GPT's hanging right arm: loose bits beside her, where her own picture has nothing"""
    lab, n = pieces(arr[..., 3] > 40)
    sizes = np.bincount(lab.ravel())
    sizes[0] = 0
    main = sizes.argmax()
    kill = np.zeros(lab.shape, bool)
    for i in range(1, n + 1):
        bit = lab == i
        ys, xs = np.nonzero(bit)
        if i == main or xs.mean() < 128 or not 140 < ys.mean() < 345 or (bit & FULL_A).sum() > bit.sum() / 2:
            continue
        kill |= bit
    return kill


def outfit(im, name=''):
    body = im.convert('RGBA').resize((220, 420), Image.LANCZOS)
    out = Image.new('RGBA', (240, 420))
    out.paste(body, (0, 0))
    out = darken(out)
    arr = np.array(out)
    arr[..., 3][ARM] = 0
    if name in HANDS:
        (x0, y0, x1, y1), (lo, hi) = HANDS[name]
        h = hsv(Image.fromarray(arr))
        hand = np.zeros(ARM.shape, bool)
        hand[y0:y1, x0:x1] = True
        hand &= (h[..., 0] > lo) & (h[..., 0] < hi) & (h[..., 1] > 0.35)
        for _ in range(2):  # and its outline
            hand = hand | np.roll(hand, 1, 0) | np.roll(hand, -1, 0) | np.roll(hand, 1, 1) | np.roll(hand, -1, 1)
        hand[:, :x0] = False
        hand[y1:, :] = False
        arr[..., 3][hand & ~FULL_A] = 0
    arr[..., 3][leftovers(arr)] = 0
    return Image.fromarray(arr)


def trim(im, most):
    im = im.convert('RGBA')
    box = im.getchannel('A').point(lambda a: 255 if a > 12 else 0).getbbox()
    im = im.crop(box)
    im.thumbnail((most, most), Image.LANCZOS)
    return im


def unkey(im):
    """a cyan #00FFFF background (when GPT can't do transparency) made transparent"""
    arr = np.array(im.convert('RGBA')).astype(int)
    if arr[..., 3].min() < 250:
        return Image.fromarray(arr.astype('uint8'), 'RGBA')
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    key = (g > 200) & (b > 200) & (r < 90)
    arr[..., 3][key] = 0
    return Image.fromarray(arr.astype('uint8'), 'RGBA')


def anim(name, im):
    cols, rows, fps, loop, kind = ANIMS[name]
    im = unkey(im)
    cw, ch = im.width // cols, im.height // rows
    cels = [im.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)) for r in range(rows) for c in range(cols)]
    # one box for all (the union of where she is in each cel)
    boxes = [c.getchannel('A').point(lambda a: 255 if a > 24 else 0).getbbox() for c in cels]
    if name in SEAT and all(boxes):
        low = max(b[3] for b in boxes)
        moved = []
        for c, b in zip(cels, boxes):
            m = Image.new('RGBA', c.size)
            m.paste(c, (0, low - b[3]))
            moved.append(m)
        cels = moved
        boxes = [c.getchannel('A').point(lambda a: 255 if a > 24 else 0).getbbox() for c in cels]
    boxes = [b for b in boxes if b]
    box = (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes))
    rest = boxes[REST[name]] if name in REST and len(boxes) > REST[name] else None
    cels = [c.crop(box) for c in cels]
    w, h = cels[0].size
    k = min(1, (360 / h) if kind != 'lie' else (440 / w))
    w, h = max(1, round(w * k)), max(1, round(h * k))
    strip = Image.new('RGBA', (w * len(cels), h))
    for i, c in enumerate(cels):
        strip.paste(c.resize((w, h), Image.LANCZOS), (i * w, 0))
    os.makedirs(ANIMS_DIR, exist_ok=True)
    path = os.path.join(ANIMS_DIR, f'{name}.png')
    strip.save(path, optimize=True)
    listing = os.path.join(ANIMS_DIR, 'anims.json')
    known = json.load(open(listing)) if os.path.exists(listing) else {}
    known[name] = {'n': len(cels), 'w': w, 'h': h, 'fps': fps, 'loop': loop, 'kind': kind}
    if rest:
        known[name]['body'] = round((rest[3] - rest[1]) / (box[3] - box[1]), 3)
    json.dump(dict(sorted(known.items())), open(listing, 'w'), indent=1)
    return strip, path


def main():
    src = sys.argv[1]
    only = set(sys.argv[2:])
    os.makedirs(WARDROBE, exist_ok=True)
    os.makedirs(POSES, exist_ok=True)
    for f in sorted(os.listdir(src)):
        name, ext = os.path.splitext(f)
        if ext.lower() != '.png' or (only and name not in only):
            continue
        im = Image.open(os.path.join(src, f))
        kind, _, rest = name.partition('-')
        if kind == 'outfit':
            out, path = outfit(im, rest), os.path.join(WARDROBE, f'outfit-{rest}.png')
        elif kind == 'acc':
            out, path = trim(im, 256), os.path.join(WARDROBE, f'acc-{ACC_IDS.get(rest, rest)}.png')
        elif kind == 'face':
            out, path = im.convert('RGB').resize((256, 256), Image.LANCZOS), os.path.join(ROOT, f'face-{rest}.png')
        elif kind == 'cutin':
            out, path = im.convert('RGB').resize((360, 240), Image.LANCZOS), os.path.join(ROOT, f'cutin-{rest}.png')
        elif kind == 'pose':
            out, path = trim(im, 640), os.path.join(POSES, f'pose-{rest}.png')
        elif kind == 'anim':
            if rest not in ANIMS:
                print('? (no está en ANIMS)', f)
                continue
            out, path = anim(rest, im)
        elif kind == 'wedding':
            out = im.convert('RGB')
            out.thumbnail((1000, 1000), Image.LANCZOS)
            path = os.path.join(WARDROBE, f'{name}.jpg')
            out.save(path, quality=86)
            print(f'{f} -> {os.path.relpath(path, ROOT)} {out.size} {os.path.getsize(path) // 1024} KB')
            continue
        else:
            print('?', f)
            continue
        out.save(path, optimize=True)
        print(f'{f} -> {os.path.relpath(path, ROOT)} {out.size} {os.path.getsize(path) // 1024} KB')
    index_wardrobe()


def index_wardrobe():
    # which drawings are in (wardrobe.js reads this instead of loading every picture to find out)
    names = sorted(os.path.splitext(f)[0] for f in os.listdir(WARDROBE) if f.endswith('.png'))
    json.dump(names, open(os.path.join(WARDROBE, 'index.json'), 'w'), indent=0)


if __name__ == '__main__':
    main()
