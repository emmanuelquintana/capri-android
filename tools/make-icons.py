#!/usr/bin/env python3
"""(1.11.5, Emma: "que en la app puedan elegir el icono y qué tan redondeado") Her launcher icons, made from the
pictures in app/icons/ (capri.png = the classic one, icono-01.png … icono-20.png = Emma's), each in 4 shapes:

  sys     the phone's own shape (an adaptive icon: the picture fills it, the launcher cuts it)
  round   a circle, drawn by us        rounded  a square with round corners      soft  almost square

Our shapes sit inside the adaptive icon's safe zone on a transparent background, so the phone's mask never cuts
them: that's how the shape is yours on any phone. Writes res/mipmap-xxxhdpi/icf_*.webp, res/mipmap-anydpi-v26/
ic_*.xml, the <activity-alias> block of AndroidManifest.xml (between ICONS:BEGIN / ICONS:END) and
assets/android/icons.json (what AppIcon.kt offers). Run it again whenever app/icons/ changes:
    python3 tools/make-icons.py
"""
import json, os, re, sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'app', 'icons')
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')
FULL = 432           # 108 dp at xxxhdpi
SIZE = 284           # our shapes: inside the safe zone (~66 % of 108 dp), so no launcher mask touches them
SHAPES = {'sys': None, 'round': 'circle', 'rounded': 0.24, 'soft': 0.10}

def faces():
    names = sorted(f[:-4] for f in os.listdir(SRC) if f.endswith('.png'))
    return ['capri'] + [n for n in names if n != 'capri']

def key(face):  # resource-safe
    return re.sub(r'[^a-z0-9]', '_', face.lower())

def square(im):
    w, h = im.size; s = min(w, h)
    return im.crop(((w - s) // 2, (h - s) // 2, (w - s) // 2 + s, (h - s) // 2 + s))

def main():
    mip = os.path.join(RES, 'mipmap-xxxhdpi'); any26 = os.path.join(RES, 'mipmap-anydpi-v26')
    os.makedirs(any26, exist_ok=True)
    for d in (mip, any26):
        for f in os.listdir(d):
            if f.startswith(('icf_', 'ic_f_')): os.remove(os.path.join(d, f))
    aliases = []; out = []
    for face in faces():
        k = key(face)
        im = square(Image.open(os.path.join(SRC, face + '.png')).convert('RGBA'))
        im.resize((FULL, FULL), Image.LANCZOS).save(os.path.join(mip, f'icf_{k}_full.webp'), quality=88, method=6)
        for shape, r in SHAPES.items():
            name = f'ic_f_{k}_{shape}'
            if r is None:
                xml = f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@mipmap/icf_{k}_full" />\n    <foreground android:drawable="@android:color/transparent" />\n</adaptive-icon>\n'
            else:
                canvas = Image.new('RGBA', (FULL, FULL), (0, 0, 0, 0))
                mask = Image.new('L', (SIZE * 4, SIZE * 4), 0); d = ImageDraw.Draw(mask)
                if r == 'circle': d.ellipse((0, 0, SIZE * 4 - 1, SIZE * 4 - 1), fill=255)
                else: d.rounded_rectangle((0, 0, SIZE * 4 - 1, SIZE * 4 - 1), radius=int(SIZE * 4 * r), fill=255)
                mask = mask.resize((SIZE, SIZE), Image.LANCZOS)
                pic = im.resize((SIZE, SIZE), Image.LANCZOS)
                pic.putalpha(Image.composite(pic.getchannel('A'), Image.new('L', (SIZE, SIZE), 0), mask))
                off = (FULL - SIZE) // 2
                canvas.alpha_composite(pic, (off, off))
                canvas.save(os.path.join(mip, f'icf_{k}_{shape}.webp'), quality=90, method=6)
                xml = f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@android:color/transparent" />\n    <foreground android:drawable="@mipmap/icf_{k}_{shape}" />\n</adaptive-icon>\n'
            open(os.path.join(any26, name + '.xml'), 'w').write(xml)
            aliases.append(f'''        <activity-alias
            android:name=".Icon_{k}_{shape}"
            android:enabled="false"
            android:exported="true"
            android:icon="@mipmap/{name}"
            android:roundIcon="@mipmap/{name}"
            android:label="@string/app_name"
            android:targetActivity=".Home">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>''')
        out.append(k)
    man = os.path.join(ROOT, 'app', 'src', 'main', 'AndroidManifest.xml')
    s = open(man).read()
    block = '        <!-- ICONS:BEGIN (tools/make-icons.py: her icons, one per picture and shape) -->\n' + '\n'.join(aliases) + '\n        <!-- ICONS:END -->'
    s = re.sub(r'        <!-- ICONS:BEGIN.*?<!-- ICONS:END -->', lambda m: block, s, flags=re.S)
    open(man, 'w').write(s)
    json.dump({'faces': out, 'shapes': list(SHAPES)}, open(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'android', 'icons.json'), 'w'), indent=1)
    print(f'{len(out)} faces x {len(SHAPES)} shapes = {len(aliases)} icons')

if __name__ == '__main__':
    main()
