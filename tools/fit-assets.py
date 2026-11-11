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
"""
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'assets', 'capri', 'assets', 'capri')
WARDROBE = os.path.join(ROOT, 'wardrobe')
POSES = os.path.join(ROOT, 'poses')
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


def outfit(im):
    body = im.convert('RGBA').resize((220, 420), Image.LANCZOS)
    out = Image.new('RGBA', (240, 420))
    out.paste(body, (0, 0))
    out = darken(out)
    arr = np.array(out)
    arr[..., 3][ARM] = 0
    return Image.fromarray(arr)


def trim(im, most):
    im = im.convert('RGBA')
    box = im.getchannel('A').point(lambda a: 255 if a > 12 else 0).getbbox()
    im = im.crop(box)
    im.thumbnail((most, most), Image.LANCZOS)
    return im


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
            out, path = outfit(im), os.path.join(WARDROBE, f'outfit-{rest}.png')
        elif kind == 'acc':
            out, path = trim(im, 256), os.path.join(WARDROBE, f'acc-{ACC_IDS.get(rest, rest)}.png')
        elif kind == 'face':
            out, path = im.convert('RGB').resize((256, 256), Image.LANCZOS), os.path.join(ROOT, f'face-{rest}.png')
        elif kind == 'cutin':
            out, path = im.convert('RGB').resize((360, 240), Image.LANCZOS), os.path.join(ROOT, f'cutin-{rest}.png')
        elif kind == 'pose':
            out, path = trim(im, 640), os.path.join(POSES, f'pose-{rest}.png')
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


if __name__ == '__main__':
    main()
