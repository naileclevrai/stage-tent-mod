"""Procedural block textures for the props (32x32, seamless where they tile)."""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

N = 32


def _img(fn, seed=1):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (N, N))
    px = im.load()
    for y in range(N):
        for x in range(N):
            px[x, y] = fn(x, y, rnd)
    return im


def _clamp(v):
    return max(0, min(255, int(v)))


def _grey(v, a=255):
    v = _clamp(v)
    return (v, v, v, a)


def make_all(folder):
    os.makedirs(folder, exist_ok=True)
    tex = {}

    # Brushed aluminium: fine horizontal streaks.
    rows = [random.Random(3).uniform(-7, 7) for _ in range(N)]
    tex["metal"] = _img(lambda x, y, r: _grey(196 + rows[y] + r.uniform(-3, 3)))
    # Chrome: bright with soft dark bands so cylinders read as reflective.
    tex["chrome"] = _img(lambda x, y, r: _grey(170 + 70 * math.sin(x / N * math.tau * 2) ** 2 + r.uniform(-2, 2)))
    # Satin black powder coat.
    tex["black"] = _img(lambda x, y, r: _grey(34 + r.uniform(-4, 4)))
    # White plastic / painted wood.
    tex["white"] = _img(lambda x, y, r: _grey(236 + r.uniform(-3, 3)))
    # Rubber feet and caps.
    tex["rubber"] = _img(lambda x, y, r: _grey(22 + r.uniform(-3, 3)))
    # Gold for trims and chiavari chairs.
    tex["gold"] = _img(lambda x, y, r: (_clamp(214 + 25 * math.sin(x / 5) + r.uniform(-6, 6)),
                                       _clamp(170 + 20 * math.sin(x / 5) + r.uniform(-6, 6)), _clamp(84 + r.uniform(-6, 6)), 255))

    # Wood: long planks with grain.
    def wood(base, dark, seed):
        rr = random.Random(seed)
        offs = [rr.uniform(0, 6.28) for _ in range(4)]

        def f(x, y, r):
            plank = y // 8
            g = math.sin(x * 0.35 + offs[plank] + math.sin(y * 0.7 + plank) * 1.8)
            v = 0.82 + 0.1 * g + r.uniform(-0.03, 0.03)
            if y % 8 == 0:
                v *= 0.72
            if (x + plank * 11) % 32 == 0:
                v *= 0.8
            return (_clamp(base[0] * v), _clamp(base[1] * v), _clamp(base[2] * v), 255)
        return _img(f, seed)

    tex["wood"] = wood((214, 172, 118), None, 5)
    tex["wood_dark"] = wood((118, 78, 48), None, 6)

    # Fabric (near white, takes the block tint): fine weave.
    tex["cloth"] = _img(lambda x, y, r: _grey(238 + (3 if (x // 2 + y // 2) % 2 else -3) + r.uniform(-2, 2)))
    # Plastic stadium seat (tinted): smooth with a soft gloss band.
    tex["seat"] = _img(lambda x, y, r: _grey(212 + 30 * math.exp(-((y - 10) / 5) ** 2) + r.uniform(-2, 2)))
    # Velvet rope (tinted).
    tex["velvet"] = _img(lambda x, y, r: _grey(196 + 40 * math.sin((x + y) / 3) ** 2 + r.uniform(-6, 6)))
    # Galvanised steel for crowd barriers.
    tex["galvanised"] = _img(lambda x, y, r: _grey(170 + r.uniform(-14, 14) + (8 if r.random() < 0.05 else 0)))

    # Light bulbs: warm, almost white in the centre.
    tex["bulb"] = _img(lambda x, y, r: (255, _clamp(232 - 30 * (abs(x - 16) + abs(y - 16)) / 32), _clamp(170 - 60 * (abs(x - 16) + abs(y - 16)) / 32), 255))

    # Shooting gallery paints.
    tex["red"] = _img(lambda x, y, r: (_clamp(186 + r.uniform(-6, 6)), _clamp(32 + r.uniform(-4, 4)), _clamp(30 + r.uniform(-4, 4)), 255))
    tex["yellow"] = _img(lambda x, y, r: (_clamp(242 + r.uniform(-5, 5)), _clamp(196 + r.uniform(-6, 6)), _clamp(36 + r.uniform(-4, 4)), 255))
    tex["navy"] = _img(lambda x, y, r: (_clamp(24 + r.uniform(-3, 3)), _clamp(36 + r.uniform(-3, 3)), _clamp(78 + r.uniform(-5, 5)), 255))
    tex["orange"] = _img(lambda x, y, r: (_clamp(236 + r.uniform(-5, 5)), _clamp(122 + r.uniform(-6, 6)), _clamp(28 + r.uniform(-4, 4)), 255))

    # Bullseye target face.
    def target(x, y, r):
        d = math.hypot(x - 15.5, y - 15.5)
        ring = int(d / 3.2)
        c = (230, 230, 226) if ring % 2 else (196, 30, 28)
        if d > 15.5:
            c = (40, 40, 44)
        return (*c, 255)
    tex["target"] = _img(target)

    # Back wall of the booth: midnight blue with gold stars.
    star = random.Random(9)
    stars = {(star.randrange(N), star.randrange(N)) for _ in range(9)}

    def back(x, y, r):
        if (x, y) in stars or any((x - sx) ** 2 + (y - sy) ** 2 <= 1 for (sx, sy) in stars) and r.random() < 0.6:
            return (236, 196, 90, 255)
        return (_clamp(26 + r.uniform(-3, 3)), _clamp(32 + r.uniform(-3, 3)), _clamp(70 + r.uniform(-4, 4)), 255)
    tex["booth_back"] = _img(back)

    # Prize plush colours.
    for name, c in (("plush_pink", (240, 140, 180)), ("plush_blue", (110, 170, 240)), ("plush_mint", (130, 220, 180))):
        tex[name] = _img(lambda x, y, r, c=c: (_clamp(c[0] + r.uniform(-12, 12)), _clamp(c[1] + r.uniform(-12, 12)), _clamp(c[2] + r.uniform(-12, 12)), 255))

    for name, im in tex.items():
        im.save(os.path.join(folder, name + ".png"))

    # Booth sign: wide lettering texture (64x16) mapped on the pediment.
    sign = Image.new("RGBA", (64, 16), (186, 32, 30, 255))
    d = ImageDraw.Draw(sign)
    d.rectangle([0, 0, 63, 15], outline=(242, 196, 36, 255))
    try:
        font = ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 12)
    except OSError:
        font = ImageFont.load_default()
    text = "TIR"
    w = d.textlength(text, font=font)
    d.text(((64 - w) / 2, 1), text, font=font, fill=(255, 238, 180, 255))
    for x in (5, 58):
        d.ellipse([x - 2, 6, x + 2, 10], fill=(255, 230, 150, 255))
    sign.save(os.path.join(folder, "booth_sign.png"))
    return sorted(list(tex) + ["booth_sign"])
