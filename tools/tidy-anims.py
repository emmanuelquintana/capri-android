#!/usr/bin/env python3
"""(1.10.1) "Las animaciones de la nueva actualización están como con errores": tidies anims/*.png after
fit-assets.py, in place.

    python3 tools/tidy-anims.py

1. Hair cut straight: where a drawing was cut at its cell's edge (her mane past the cell in the sheet), the
   last few pixels fade out, so it's a soft edge instead of a straight line.
2. Sliding cels: each cel of a standing or sitting one is moved sideways onto the first (the best overlap of
   her shape), so she doesn't slide from side to side between cels; the strip gets wider to fit.
   (Not sandevistan: there she dashes on purpose.) Run it once per new drawing: a second time changes nothing.
"""
import json
import os

import numpy as np
from PIL import Image

DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'assets', 'capri', 'assets', 'capri', 'anims')
apply = True

# ---- 1. soft edges
d=json.load(open(f'{DIR}/anims.json'))
for n,m in d.items():
    im=np.array(Image.open(f'{DIR}/{n}.png').convert('RGBA')); w,h=m['w'],m['h']
    F=max(6,round(12*h/360)); touched=[]
    for i in range(m['n']):
        c=im[:,i*w:(i+1)*w]; a=c[:,:,3].astype(float)
        def frac(v): return (v>40).mean()
        sides={'L':frac(np.maximum(a[:,0],a[:,1])),'R':frac(np.maximum(a[:,-1],a[:,-2])),'T':frac(np.maximum(a[0,:],a[1,:]))}
        ramp=(np.arange(F)+0.5)/F
        for k,v in sides.items():
            if v<=0.06: continue
            touched.append(f'f{i}{k}')
            if k=='L': a[:,:F]*=ramp[None,:]
            if k=='R': a[:,-F:]*=ramp[::-1][None,:]
            if k=='T': a[:F,:]*=ramp[:,None]
        c[:,:,3]=a.astype(np.uint8)
    if touched:
        Image.fromarray(im).save(f'{DIR}/{n}.png',optimize=True); print(n,touched)

# ---- 2. cels on one spot
d=json.load(open(f'{DIR}/anims.json'))
def iou(a,b): return (a&b).sum()/max(1,(a|b).sum())
def shift(arr,s):
    o=np.zeros_like(arr)
    if s>0: o[:,s:]=arr[:,:-s]
    elif s<0: o[:,:s]=arr[:,-s:]
    else: o=arr.copy()
    return o
res={}
for n,m in d.items():
    if m['kind'] not in ('stand','sit') or m['n']<2 or n=='sandevistan': continue
    im=np.array(Image.open(f'{DIR}/{n}.png').convert('RGBA')); w,h=m['w'],m['h']
    cels=[im[:,i*w:(i+1)*w] for i in range(m['n'])]
    masks=[c[:,:,3]>60 for c in cels]
    ref=masks[0]; R=int(w*0.25); shifts=[0]
    for mk in masks[1:]:
        best=max(range(-R,R+1),key=lambda s: iou(shift(mk,s),ref))
        base=iou(mk,ref); got=iou(shift(mk,best),ref)
        shifts.append(best if abs(best)>=4 and got-base>0.04 else 0)
    # center the shifts around their median so she stays where she was
    med=int(np.median(shifts)); shifts=[s-med for s in shifts]
    res[n]=shifts
    if any(shifts): print(f'{n:16s} shifts={shifts}')
    if apply and any(shifts):
        pad=max(abs(s) for s in shifts); W=w+2*pad
        out=np.zeros((h,W*m['n'],4),dtype=np.uint8)
        for i,(c,s) in enumerate(zip(cels,shifts)):
            out[:,i*W+pad+s:i*W+pad+s+w]=c
        Image.fromarray(out).save(f'{DIR}/{n}.png',optimize=True)
        m['w']=W
if apply: json.dump(dict(sorted(d.items())),open(f'{DIR}/anims.json','w'),indent=1)
