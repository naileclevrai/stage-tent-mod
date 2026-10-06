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

    # Brushed stainless steel: brighter than aluminium, long vertical grain.
    cols = [random.Random(17).uniform(-9, 9) for _ in range(N)]
    tex["steel"] = _img(lambda x, y, r: (_clamp(206 + cols[x] + r.uniform(-3, 3)), _clamp(208 + cols[x] + r.uniform(-3, 3)),
                                        _clamp(212 + cols[x] + r.uniform(-3, 3)), 255))

    # Badge reader: black bezel, dark glass, a card symbol.
    def reader(x, y, r):
        if x < 2 or y < 2 or x > 29 or y > 29:
            return (18, 18, 20, 255)
        if 9 <= x <= 22 and 11 <= y <= 20 and (x in (9, 22) or y in (11, 20)):
            return (140, 146, 156, 255)
        if 11 <= x <= 14 and 13 <= y <= 15:
            return (190, 160, 70, 255)
        return (_clamp(34 + r.uniform(-3, 3)), _clamp(36 + r.uniform(-3, 3)), _clamp(42 + r.uniform(-3, 3)), 255)
    tex["reader"] = _img(reader)

    # LED matrix displays: a green arrow pointing up (the direction of passage) and a red cross, as dots.
    arrow = {(x, y) for y in range(4, 28) for x in range(32) if
             (y < 14 and abs(x - 15.5) <= (y - 4) * 1.0 + 0.5) or (y >= 14 and 11 <= x <= 20)}
    cross = {(x, y) for y in range(5, 27) for x in range(5, 27) if abs(x - y) <= 2 or abs(x + y - 31) <= 2}

    def led(cells, on):
        def f(x, y, r):
            if (x % 2 == 0) and (y % 2 == 0) and (x, y) in cells:
                return (*on, 255)
            if (x % 2 == 0) and (y % 2 == 0):
                return (on[0] // 7, on[1] // 7, on[2] // 7, 255)
            return (8, 8, 9, 255)
        return f
    tex["led_arrow"] = _img(led(arrow, (70, 255, 110)))
    tex["led_cross"] = _img(led(cross, (255, 60, 50)))

    # Stage deck top: dark brown film-faced ply with an embossed wire-mesh grip.
    def deck(x, y, r):
        v = 1.0 + r.uniform(-0.04, 0.04)
        if (x + y) % 4 == 0 or (x - y) % 4 == 0:
            v *= 1.22
        elif (x + y) % 4 == 1:
            v *= 0.86
        return (_clamp(70 * v), _clamp(50 * v), _clamp(38 * v), 255)
    tex["deck"] = _img(deck, 21)

    # Aluminium chequer plate: alternating diagonal lugs with a highlight and a shadow.
    def tread(x, y, r):
        cx, cy, lx, ly = x // 4, y // 4, x % 4, y % 4
        v = 168 + r.uniform(-5, 5)
        d = (lx - ly) if (cx + cy) % 2 == 0 else (lx + ly - 3)
        if d == 0 and 0 < lx < 3:
            v = 214
        elif d == 1 and 0 < lx < 3:
            v = 138
        return _grey(v)
    tex["tread"] = _img(tread, 22)

    # Painted steel / moulded plastic (tinted): smooth with a faint orange peel.
    tex["paint"] = _img(lambda x, y, r: _grey(232 + r.uniform(-3, 3)), 23)
    # Velour stage skirt (tinted): soft pile, slightly streaked along the drop.
    pile = [random.Random(24).uniform(-6, 6) for _ in range(N)]
    tex["velour"] = _img(lambda x, y, r: _grey(206 + pile[x] + r.uniform(-5, 5)), 25)
    # Seamless cyclorama muslin (tinted): almost flat.
    tex["muslin"] = _img(lambda x, y, r: _grey(246 + r.uniform(-2, 2)), 26)
    # Knitted privacy scrim (tinted) and its webbing hem.
    tex["scrim"] = _img(lambda x, y, r: _grey(214 + (10 if (x + (y // 2) * 1) % 3 == 0 else 0) - (8 if y % 2 else 0) + r.uniform(-3, 3)), 27)
    tex["hem"] = _img(lambda x, y, r: _grey(186 + (14 if y % 8 in (2, 5) else 0) + r.uniform(-4, 4)), 28)

    # Welded wire mesh of a site fence panel: galvanised wires, holes cut out.
    def wire(x, y, r):
        if x % 4 == 0 or y % 8 == 0:
            return _grey(176 + r.uniform(-14, 14))
        return (0, 0, 0, 0)
    tex["mesh"] = _img(wire, 29)

    # Flight case laminate: black with a fine dimpled pattern.
    tex["case"] = _img(lambda x, y, r: _grey(32 + (4 if (x % 3 == 0 and y % 3 == 1) else 0) + r.uniform(-2, 2)), 30)
    tex["grey"] = _img(lambda x, y, r: (_clamp(76 + r.uniform(-4, 4)), _clamp(79 + r.uniform(-4, 4)), _clamp(84 + r.uniform(-4, 4)), 255), 31)
    tex["green"] = _img(lambda x, y, r: (_clamp(52 + r.uniform(-5, 5)), _clamp(178 + r.uniform(-6, 6)), _clamp(78 + r.uniform(-5, 5)), 255), 32)
    tex["blue"] = _img(lambda x, y, r: (_clamp(36 + r.uniform(-4, 4)), _clamp(92 + r.uniform(-5, 5)), _clamp(196 + r.uniform(-6, 6)), 255), 33)
    # Sandbag canvas.
    tex["canvas"] = _img(lambda x, y, r: (_clamp(150 + (8 if (x + y) % 2 else -6) + r.uniform(-6, 6)),
                                          _clamp(128 + (8 if (x + y) % 2 else -6) + r.uniform(-6, 6)), _clamp(92 + r.uniform(-6, 6)), 255), 34)

    # Generator control panel: LCD, buttons, key switch and a row of breakers.
    def gen_panel(x, y, r):
        if x < 1 or y < 1 or x > 30 or y > 30:
            return (20, 20, 22, 255)
        if 3 <= x <= 18 and 3 <= y <= 11:
            if x in (3, 18) or y in (3, 11):
                return (14, 14, 16, 255)
            lit = (5 <= y <= 9) and (x % 3 != 1) and (x < 13 or y in (5, 9))
            return (120, 230, 200, 255) if lit else (38, 92, 84, 255)
        if (x - 25) ** 2 + (y - 7) ** 2 <= 9:
            return (190, 190, 196, 255) if abs(x - 25) <= 0 or (x - 25) ** 2 + (y - 7) ** 2 >= 7 else (60, 60, 64, 255)
        for (bx, c) in ((5, (60, 200, 90)), (11, (220, 60, 50)), (17, (200, 200, 205))):
            if bx <= x <= bx + 3 and 15 <= y <= 18:
                return (*c, 255)
        if 3 <= x <= 28 and 22 <= y <= 28:
            if (x - 3) % 4 == 3:
                return (14, 14, 16, 255)
            return (235, 235, 235, 255) if y in (24, 25) else (36, 36, 40, 255)
        return (_clamp(58 + r.uniform(-3, 3)), _clamp(60 + r.uniform(-3, 3)), _clamp(66 + r.uniform(-3, 3)), 255)
    tex["gen_panel"] = _img(gen_panel, 35)

    # Card in a label holder / rating plate.
    def label(x, y, r):
        if x < 1 or y < 1 or x > 30 or y > 30:
            return (60, 60, 64, 255)
        if y in (6, 7) and 3 <= x <= 24:
            return (30, 30, 30, 255)
        if y in (13, 19, 25) and 3 <= x <= (28 if y != 25 else 16):
            return (110, 110, 110, 255)
        return _grey(238 + r.uniform(-3, 3))
    tex["label"] = _img(label, 36)

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

    # Door plate of the site toilet.
    wc = Image.new("RGBA", (N, N), (240, 240, 236, 255))
    d = ImageDraw.Draw(wc)
    d.rectangle([0, 0, N - 1, N - 1], outline=(40, 40, 44, 255), width=2)
    try:
        font = ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 15)
    except OSError:
        font = ImageFont.load_default()
    w = d.textlength("WC", font=font)
    d.text(((N - w) / 2, 7), "WC", font=font, fill=(30, 30, 34, 255))
    wc.save(os.path.join(folder, "wc_sign.png"))
    return sorted(list(tex) + ["booth_sign", "wc_sign"])
