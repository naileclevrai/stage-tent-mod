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

    # Rear intake of the water cannon: a ring, a hub and radial blades.
    def fan(x, y, r):
        dx, dy = x - 15.5, y - 15.5
        d = math.hypot(dx, dy)
        if d > 15.2:
            return (28, 30, 34, 255)
        ang = math.atan2(dy, dx)
        blade = (ang / (math.tau / 7)) % 1.0
        if d > 13.5:
            return (176, 180, 186, 255)
        if d < 3.3:
            return (148, 152, 158, 255)
        if blade < 0.20:
            return _grey(78 + 50 * (1 - abs(blade - 0.10) / 0.10))
        if abs(d - 6.6) < 0.55 or abs(d - 10.4) < 0.5:
            return (96, 100, 106, 255)
        return (20, 22, 26, 255)
    tex["fan"] = _img(fan, 37)

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
    make_co2(folder)
    make_pint(folder)
    make_plancha(folder)
    make_bins(folder)
    make_extinguisher(folder)
    make_tower_tarp(folder)
    return sorted(list(tex) + ["booth_sign", "wc_sign"] + [f"co2_{i}" for i in range(12)] + ["co2_blue", "pint_glass", "beer_liquid", "beer_foam", "plancha_hot", "plancha_meat", "plancha_sausage", "bin_plastic", "bin_divers", "bin_plastic_side", "bin_divers_side", "bin_lid_plastic", "bin_lid_divers", "bin_top_plastic", "bin_top_divers", "bin_mustard", "bin_navy", "bin_yellow", "bin_black", "extinguisher_label", "extinguisher_side", "extinguisher_gauge", "extinguisher_check", "tarp_pvc"])


def make_co2(folder):
    """Painted cylinder body: light grey at the shoulder, grimy grey-brown at the foot, plus a blue collar."""
    os.makedirs(folder, exist_ok=True)
    bands = 12
    for i in range(bands):
        t = i / (bands - 1)
        r = 186 + (58 - 186) * t
        g = 188 + (52 - 188) * t
        b = 184 + (46 - 184) * t
        if t > 0.5:
            k = (t - 0.5) / 0.5
            r += 18 * k
            g -= 6 * k
            b -= 14 * k
        rnd = random.Random(80 + i)
        im = Image.new("RGBA", (N, N))
        px = im.load()
        for y in range(N):
            for x in range(N):
                n = rnd.uniform(-7, 7)
                rust = 22 if rnd.random() < 0.015 + t * 0.04 else 0
                px[x, y] = (_clamp(r + n + rust), _clamp(g + n + rust * 0.35), _clamp(b + n), 255)
        im.save(os.path.join(folder, f"co2_{i}.png"))
    blue = Image.new("RGBA", (N, N))
    px = blue.load()
    rnd = random.Random(91)
    for y in range(N):
        lift = 28 * (1 - y / (N - 1))
        for x in range(N):
            n = rnd.uniform(-6, 6)
            px[x, y] = (_clamp(16 + n), _clamp(96 + lift + n), _clamp(186 + n), 255)
    blue.save(os.path.join(folder, "co2_blue.png"))


def make_pint(folder):
    """Clear pint glass, amber beer and a cream foam head."""
    os.makedirs(folder, exist_ok=True)
    glass = Image.new("RGBA", (N, N))
    px = glass.load()
    for y in range(N):
        for x in range(N):
            shine = 70 if 11 <= x <= 15 else 0
            px[x, y] = (_clamp(198 + shine * 0.4), _clamp(214 + shine * 0.3), _clamp(226 + shine * 0.2), 160)
    glass.save(os.path.join(folder, "pint_glass.png"))
    beer = Image.new("RGBA", (N, N))
    px = beer.load()
    rnd = random.Random(12)
    for y in range(N):
        for x in range(N):
            n = rnd.uniform(-8, 8)
            px[x, y] = (_clamp(198 + n), _clamp(118 + n * 0.4), _clamp(36 + n * 0.2), 235)
    beer.save(os.path.join(folder, "beer_liquid.png"))
    foam = Image.new("RGBA", (N, N))
    px = foam.load()
    rnd = random.Random(13)
    for y in range(N):
        for x in range(N):
            n = rnd.uniform(-6, 10)
            px[x, y] = (_clamp(246 + n), _clamp(238 + n), _clamp(214 + n * 0.4), 248)
    foam.save(os.path.join(folder, "beer_foam.png"))


def make_plancha(folder):
    """Seasoned hot plate, a seared steak and a sausage."""
    os.makedirs(folder, exist_ok=True)
    hot = Image.new("RGBA", (N, N))
    px = hot.load()
    rnd = random.Random(21)
    for y in range(N):
        for x in range(N):
            n = rnd.uniform(-10, 8)
            heat = 50 * math.exp(-((x - 16) ** 2 + (y - 18) ** 2) / 180)
            px[x, y] = (_clamp(92 + n + heat), _clamp(48 + n * 0.4 + heat * 0.45), _clamp(28 + n * 0.2), 255)
    hot.save(os.path.join(folder, "plancha_hot.png"))
    meat = Image.new("RGBA", (N, N))
    px = meat.load()
    rnd = random.Random(22)
    for y in range(N):
        stripe = 28 if 6 <= y % 14 <= 8 else 0
        for x in range(N):
            n = rnd.uniform(-12, 10)
            px[x, y] = (_clamp(132 + n - stripe), _clamp(62 + n * 0.4 - stripe * 0.6), _clamp(36 + n * 0.2), 255)
    meat.save(os.path.join(folder, "plancha_meat.png"))
    sausage = Image.new("RGBA", (N, N))
    px = sausage.load()
    rnd = random.Random(23)
    for y in range(N):
        for x in range(N):
            n = rnd.uniform(-8, 8)
            spec = 40 if rnd.random() < 0.08 else 0
            px[x, y] = (_clamp(168 + n + spec), _clamp(72 + n * 0.3 + spec * 0.4), _clamp(42 + n * 0.2), 255)
    sausage.save(os.path.join(folder, "plancha_sausage.png"))


def _font(size):
    try:
        return ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", size)
    except OSError:
        return ImageFont.load_default()


def _centered(draw, text, font, box, fill):
    x0, y0, x1, y1 = box
    bb = draw.textbbox((0, 0), text, font=font)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    draw.text((x0 + (x1 - x0 - tw) / 2 - bb[0], y0 + (y1 - y0 - th) / 2 - bb[1]), text, font=font, fill=fill)


def _bottle(d, x, y, s, fill, line):
    cap = (236, 206, 32, 255)
    d.rounded_rectangle([x + 0.36 * s, y, x + 0.64 * s, y + 0.10 * s], radius=3, fill=cap, outline=line, width=3)
    d.rectangle([x + 0.42 * s, y + 0.09 * s, x + 0.58 * s, y + 0.24 * s], fill=fill, outline=line, width=3)
    d.rounded_rectangle([x + 0.16 * s, y + 0.22 * s, x + 0.84 * s, y + 0.96 * s], radius=max(4, int(0.12 * s)),
                        fill=fill, outline=line, width=4)
    d.line([(x + 0.30 * s, y + 0.40 * s), (x + 0.30 * s, y + 0.78 * s)], fill=(255, 255, 255, 210), width=3)


def _cup(d, x, y, s, fill, line):
    d.rounded_rectangle([x + 0.18 * s, y + 0.10 * s, x + 0.82 * s, y + 0.20 * s], radius=3, fill=fill, outline=line, width=3)
    d.polygon([
        (x + 0.24 * s, y + 0.18 * s), (x + 0.76 * s, y + 0.18 * s),
        (x + 0.66 * s, y + 0.86 * s), (x + 0.34 * s, y + 0.86 * s),
    ], fill=fill, outline=line)
    d.line([(x + 0.24 * s, y + 0.18 * s), (x + 0.34 * s, y + 0.86 * s), (x + 0.66 * s, y + 0.86 * s),
            (x + 0.76 * s, y + 0.18 * s)], fill=line, width=4)
    d.arc([x + 0.68 * s, y + 0.28 * s, x + 1.02 * s, y + 0.68 * s], -80, 80, fill=line, width=4)


def _carton(d, x, y, s, fill, line):
    d.polygon([
        (x + 0.18 * s, y + 0.28 * s), (x + 0.55 * s, y + 0.10 * s), (x + 0.92 * s, y + 0.28 * s),
        (x + 0.92 * s, y + 0.82 * s), (x + 0.55 * s, y + 0.98 * s), (x + 0.18 * s, y + 0.82 * s),
    ], fill=fill, outline=line)
    d.line([(x + 0.18 * s, y + 0.28 * s), (x + 0.55 * s, y + 0.46 * s), (x + 0.92 * s, y + 0.28 * s)], fill=line, width=3)
    d.line([(x + 0.55 * s, y + 0.46 * s), (x + 0.55 * s, y + 0.98 * s)], fill=line, width=3)


def _wave(d, x0, x1, y, amp, period, fill, width):
    pts = [(x, y + amp * math.sin((x - x0) / period * math.tau)) for x in range(int(x0), int(x1) + 1, 2)]
    d.line(pts, fill=fill, width=width, joint="curve")


def _recycle(d, cx, cy, r, fill, width):
    """Three chasing arrows on a triangle."""
    v = [(cx + r * math.cos(math.radians(-90 + 120 * k)), cy + r * math.sin(math.radians(-90 + 120 * k)))
         for k in range(3)]
    for k in range(3):
        (ax, ay), (bx, by) = v[k], v[(k + 1) % 3]
        p = lambda t: (ax + (bx - ax) * t, ay + (by - ay) * t)
        d.line([p(0.12), p(0.62)], fill=fill, width=width)
        dx, dy = bx - ax, by - ay
        ln = math.hypot(dx, dy)
        nx, ny = -dy / ln, dx / ln
        tip = p(0.86)
        base = p(0.60)
        s = width * 1.5
        d.polygon([tip, (base[0] + nx * s, base[1] + ny * s), (base[0] - nx * s, base[1] - ny * s)], fill=fill)


def _bottle_badge(d, cx, cy, r, ring, fill):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ring)
    s = r / 20
    d.rounded_rectangle([cx - 5 * s, cy - 7 * s, cx + 5 * s, cy + 14 * s], radius=3 * s, fill=fill)
    d.polygon([(cx - 5 * s, cy - 6 * s), (cx - 2.5 * s, cy - 11 * s), (cx + 2.5 * s, cy - 11 * s), (cx + 5 * s, cy - 6 * s)],
              fill=fill)
    d.rectangle([cx - 2.5 * s, cy - 15 * s, cx + 2.5 * s, cy - 11 * s], fill=fill)


def _trash_badge(d, cx, cy, r, ring, fill):
    """Person dropping litter into a bin."""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ring)
    s = r / 20
    d.polygon([(cx + 1 * s, cy + 1 * s), (cx + 13 * s, cy + 1 * s), (cx + 11 * s, cy + 14 * s), (cx + 3 * s, cy + 14 * s)],
              fill=fill)
    d.rectangle([cx, cy - 1 * s, cx + 14 * s, cy + 1 * s], fill=fill)
    d.ellipse([cx - 11 * s, cy - 15 * s, cx - 5 * s, cy - 9 * s], fill=fill)
    d.line([(cx - 8 * s, cy - 8 * s), (cx - 9 * s, cy + 5 * s)], fill=fill, width=max(2, int(3 * s)))
    d.line([(cx - 9 * s, cy + 5 * s), (cx - 12 * s, cy + 14 * s)], fill=fill, width=max(2, int(3 * s)))
    d.line([(cx - 9 * s, cy + 5 * s), (cx - 5 * s, cy + 14 * s)], fill=fill, width=max(2, int(3 * s)))
    d.line([(cx - 8 * s, cy - 5 * s), (cx - 1 * s, cy - 3 * s)], fill=fill, width=max(2, int(3 * s)))
    d.rectangle([cx + 4 * s, cy - 7 * s, cx + 7 * s, cy - 4 * s], fill=fill)


def _star(d, cx, cy, r0, r1, fill, points=8):
    pts = []
    for k in range(points * 2):
        a = math.pi * k / points - math.pi / 2
        rr = r1 if k % 2 == 0 else r0
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=fill)


def _arc_text(img, text, cx, cy, radius, font, fill, centre_deg=-90):
    """Letters set on a circle, centred on centre_deg (screen degrees, -90 = top), reading left to right."""
    d = ImageDraw.Draw(img)
    widths = [d.textlength(ch, font=font) for ch in text]
    total = sum(widths) / radius
    a = math.radians(centre_deg) - total / 2
    for ch, w in zip(text, widths):
        mid = a + w / 2 / radius
        tile = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        td = ImageDraw.Draw(tile)
        bb = td.textbbox((0, 0), ch, font=font)
        td.text((32 - (bb[0] + bb[2]) / 2, 32 - (bb[1] + bb[3]) / 2), ch, font=font, fill=fill)
        tile = tile.rotate(-math.degrees(mid) - 90, resample=Image.BICUBIC)
        img.alpha_composite(tile, (int(cx + radius * math.cos(mid) - 32), int(cy + radius * math.sin(mid) - 32)))
        a += w / radius


def make_bins(folder):
    """Prints for the corrugated festival sorting bins. Generic English wording, no brand.

    Body prints are 256 x 512, lid sides 352 x 160, lid tops 256 x 256 with the hole in the middle.
    """
    os.makedirs(folder, exist_ok=True)
    lime = (150, 192, 62, 255)
    mustard = (198, 182, 48, 255)
    yellow = (228, 208, 62, 255)
    navy = (30, 34, 50, 255)
    lid_black = (36, 36, 38, 255)
    white = (250, 250, 246, 255)
    blue = (46, 150, 222, 255)
    red = (222, 54, 44, 255)
    dark = (60, 60, 40, 255)

    for name, colour in (("bin_mustard", mustard), ("bin_navy", navy), ("bin_yellow", yellow),
                         ("bin_black", lid_black)):
        Image.new("RGBA", (16, 16), colour).save(os.path.join(folder, name + ".png"))

    def recycle_block(d, y, ink, plastic):
        _recycle(d, 64, y, 26, ink, 6)
        if plastic:
            _centered(d, "5", _font(16), (52, y - 10, 76, y + 12), ink)
            _centered(d, "PP", _font(13), (44, y + 26, 84, y + 42), ink)
        _centered(d, "100%", _font(22), (104, y - 30, 236, y - 6), ink)
        _centered(d, "RECYCLABLE", _font(12), (104, y - 6, 236, y + 8), ink)
        for x in range(112, 230, 6):
            d.rectangle([x, y + 12, x + 2, y + 14], fill=ink)
        _centered(d, "50%", _font(22), (104, y + 18, 236, y + 42), ink)
        _centered(d, "RECYCLED MATERIAL", _font(10), (104, y + 42, 236, y + 54), ink)

    # ---------------------------------------------------------------- plastics, front
    img = Image.new("RGBA", (256, 512), lime)
    d = ImageDraw.Draw(img)
    d.polygon([(186, 0), (214, 0), (204, 512), (196, 512)], fill=yellow)
    for k, word in enumerate(("DO THE", "RIGHT", "THING")):
        d.text((18, 70 + k * 46), word, font=_font(44), fill=white)
    _star(d, 62, 262, 18, 30, red, 7)
    d.line([(52, 274), (72, 252)], fill=white, width=5)
    d.polygon([(76, 248), (62, 250), (74, 262)], fill=white)
    _wave(d, 196, 256, 330, 7, 22, blue, 8)
    _recycle(d, 104, 430, 34, white, 8)
    img.save(os.path.join(folder, "bin_plastic.png"))

    # ---------------------------------------------------------------- plastics, side and back
    img = Image.new("RGBA", (256, 512), mustard)
    d = ImageDraw.Draw(img)
    _wave(d, 0, 76, 86, 7, 22, blue, 8)
    _centered(d, "PLASTIC", _font(26), (0, 180, 256, 210), white)
    _bottle_badge(d, 128, 252, 30, blue, white)
    _wave(d, 160, 256, 360, 7, 22, blue, 8)
    recycle_block(d, 448, dark, True)
    img.save(os.path.join(folder, "bin_plastic_side.png"))

    # ---------------------------------------------------------------- general waste, front
    img = Image.new("RGBA", (256, 512), lime)
    d = ImageDraw.Draw(img)
    d.polygon([(70, 0), (112, 0), (96, 330), (86, 330)], fill=navy)
    d.polygon([(222, 0), (256, 0), (256, 512), (232, 512), (238, 260)], fill=navy)
    d.ellipse([132, 44, 216, 128], outline=red, width=13)
    _arc_text(img, "REDUCE · REUSE · RECYCLE ·", 174, 86, 35, _font(9), white, centre_deg=-90)
    d = ImageDraw.Draw(img)
    _wave(d, 200, 256, 200, 7, 22, blue, 8)
    _star(d, 46, 432, 14, 24, red, 7)
    d.text((70, 380), "SORT", font=_font(40), fill=white)
    d.text((70, 420), "& RECYCLE", font=_font(24), fill=white)
    img.save(os.path.join(folder, "bin_divers.png"))

    # ---------------------------------------------------------------- general waste, side and back
    img = Image.new("RGBA", (256, 512), navy)
    d = ImageDraw.Draw(img)
    _wave(d, 0, 76, 86, 7, 22, lime, 8)
    _centered(d, "GENERAL WASTE", _font(24), (0, 180, 256, 210), white)
    _trash_badge(d, 128, 252, 30, (150, 150, 150, 255), white)
    _wave(d, 160, 256, 360, 7, 22, lime, 8)
    recycle_block(d, 448, white, False)
    img.save(os.path.join(folder, "bin_divers_side.png"))

    # ---------------------------------------------------------------- lid sides
    for name, bg, word, badge, ring in (("bin_lid_plastic", yellow, "PLASTIC", _bottle_badge, blue),
                                       ("bin_lid_divers", lid_black, "GENERAL WASTE", _trash_badge, (150, 150, 150, 255))):
        img = Image.new("RGBA", (352, 160), bg)
        d = ImageDraw.Draw(img)
        _centered(d, word, _font(30), (0, 20, 352, 56), white)
        badge(d, 176, 104, 30, ring, white)
        img.save(os.path.join(folder, name + ".png"))

    # ---------------------------------------------------------------- lid tops, hole in the middle
    for name, bg, ink in (("bin_top_plastic", yellow, (110, 96, 20, 255)),
                         ("bin_top_divers", lid_black, (170, 170, 170, 255))):
        img = Image.new("RGBA", (256, 256), bg)
        d = ImageDraw.Draw(img)
        d.polygon([(128, 190), (118, 204), (138, 204)], fill=ink)
        d.rectangle([124, 203, 132, 216], fill=ink)
        img.save(os.path.join(folder, name + ".png"))


def make_extinguisher(folder):
    """Prints for the 6 kg powder extinguisher. Generic English wording, no brand.

    extinguisher_label wraps 124 degrees around the front, extinguisher_side is the approval strip next to it,
    extinguisher_gauge is the manometer face and extinguisher_check the inspection sticker on the back.
    """
    os.makedirs(folder, exist_ok=True)
    red = (196, 28, 30, 255)
    white = (250, 250, 248, 255)
    ink = (30, 30, 32, 255)

    def centered(d, y, text, size, fill, x0=0, x1=None):
        font = _font(size)
        x1 = x1 if x1 is not None else d.im.size[0]
        w = d.textlength(text, font=font)
        d.text((x0 + (x1 - x0 - w) / 2, y), text, font=font, fill=fill)

    # ---------------------------------------------------------------- main label, 256 x 512
    W, H = 256, 512
    lab = Image.new("RGBA", (W, H), red)
    d = ImageDraw.Draw(lab)
    d.rectangle([4, 4, W - 5, H - 5], outline=white, width=3)
    # Title band and the white tab with the charge.
    centered(d, 16, "FIRE EXTINGUISHER", 22, white)
    d.rounded_rectangle([14, 56, W - 15, 88], radius=5, fill=white)
    centered(d, 61, "6 kg ABC DRY POWDER", 19, red)
    for i, rating in enumerate(("27A", "183B", "C")):
        centered(d, 96, rating, 17, white, 12 + i * 77, 12 + (i + 1) * 77)
    d.line([(14, 122), (W - 15, 122)], fill=white, width=2)

    # Two instruction rows: number box, small pictogram, text.
    def step(y, num, lines, pin):
        d.rectangle([14, y, 34, y + 20], outline=white, width=2)
        centered(d, y + 1, num, 15, white, 14, 35)
        # Pictogram: the extinguisher head with an arrow (pin out) or a jet towards a flame.
        d.rounded_rectangle([46, y + 16, 64, y + 40], radius=6, outline=white, width=2)
        d.rectangle([52, y + 10, 58, y + 16], fill=white)
        d.line([(57, y + 11), (70, y + 3)], fill=white, width=2)
        d.line([(52, y + 13), (42, y + 18), (41, y + 36)], fill=white, width=2)
        if pin:
            d.ellipse([60, y + 6, 68, y + 14], outline=white, width=2)
            d.line([(70, y + 10), (86, y + 4)], fill=white, width=2)
            d.polygon([(86, y + 1), (91, y + 4), (86, y + 8)], fill=white)
        else:
            d.line([(66, y + 26), (84, y + 34)], fill=white, width=2)
            d.polygon([(86, y + 40), (90, y + 28), (94, y + 34), (97, y + 26), (100, y + 40)], fill=(255, 200, 40, 255))
        for k, text in enumerate(lines):
            d.text((106, y + 2 + k * 15), text, font=_font(13), fill=white)

    step(132, "1", ("Pull the", "safety pin"), True)
    step(184, "2", ("Aim at the base of", "the flames and", "squeeze the lever"), False)
    d.line([(14, 238), (W - 15, 238)], fill=white, width=2)

    # Fire classes: white tiles with the letter in a red corner box and a small pictogram.
    def tile(x, letter, kind):
        d.rectangle([x, 248, x + 66, 314], fill=white)
        d.rectangle([x + 38, 248, x + 66, 276], fill=red)
        d.rectangle([x + 40, 250, x + 64, 274], outline=white, width=2)
        centered(d, 249, letter, 21, white, x + 38, x + 67)
        flame = (220, 60, 30, 255)
        if kind == "wood":
            d.polygon([(x + 6, 304), (x + 34, 292), (x + 36, 298), (x + 8, 310)], fill=(140, 82, 40, 255))
            d.polygon([(x + 8, 292), (x + 36, 304), (x + 34, 310), (x + 6, 298)], fill=(160, 98, 52, 255))
        elif kind == "liquid":
            d.ellipse([x + 4, 302, x + 36, 312], fill=(70, 70, 80, 255))
            d.polygon([(x + 30, 284), (x + 34, 294), (x + 26, 294)], fill=(70, 70, 80, 255))
            d.ellipse([x + 26, 290, x + 34, 298], fill=(70, 70, 80, 255))
        else:
            d.rounded_rectangle([x + 10, 288, x + 28, 312], radius=6, fill=(70, 70, 80, 255))
            d.rectangle([x + 16, 282, x + 22, 289], fill=(70, 70, 80, 255))
        d.polygon([(x + 12, 290), (x + 16, 268), (x + 20, 278), (x + 24, 262), (x + 30, 290)], fill=flame)
        d.polygon([(x + 17, 290), (x + 20, 278), (x + 22, 284), (x + 25, 290)], fill=(255, 200, 40, 255))

    for i, (letter, kind) in enumerate((("A", "wood"), ("B", "liquid"), ("C", "gas"))):
        tile(14 + i * 79, letter, kind)
    d.line([(14, 324), (W - 15, 324)], fill=white, width=2)

    # Electrical warning.
    for k, text in enumerate(("Suitable for use on live electrical",
                              "equipment up to 1000 V.",
                              "Keep the nozzle at least 1 m away.",
                              "Avoid contact with live conductors.")):
        centered(d, 332 + k * 15, text, 12, white)
    d.line([(14, 396), (W - 15, 396)], fill=white, width=2)

    # Technical block in small print.
    for k, text in enumerate(("• Agent: ABC dry powder, 6 kg",
                              "• Propellant: nitrogen, 15 bar",
                              "• Operating range: -20 °C to +60 °C",
                              "• Recharge after every use",
                              "• Service once a year")):
        d.text((16, 403 + k * 13), text, font=_font(10), fill=white)
    d.rectangle([4, 472, W - 5, H - 5], fill=white)
    centered(d, 478, "DRY POWDER EXTINGUISHER", 13, red)
    centered(d, 493, "For fire use only · Do not remove", 10, red)
    lab.save(os.path.join(folder, "extinguisher_label.png"))

    # ---------------------------------------------------------------- approval strip, 48 x 384
    side = Image.new("RGBA", (48, 384), red)
    d = ImageDraw.Draw(side)
    d.rectangle([2, 2, 45, 381], outline=white, width=2)
    d.rectangle([7, 10, 40, 48], outline=white, width=2)
    centered(d, 16, "CE", 20, white)
    d.ellipse([7, 58, 40, 91], outline=white, width=2)
    for k in range(12):
        a = math.tau * k / 12
        d.line([(23.5 + 9 * math.cos(a), 74.5 + 9 * math.sin(a)), (23.5 + 14 * math.cos(a), 74.5 + 14 * math.sin(a))],
               fill=white, width=2)
    for k, text in enumerate(("EN 3-7", "EN 3-8")):
        d.rectangle([7, 102 + k * 30, 40, 126 + k * 30], outline=white, width=2)
        centered(d, 108 + k * 30, text, 9, white)
    # Small print running up the strip.
    run = Image.new("RGBA", (220, 30), (0, 0, 0, 0))
    rd = ImageDraw.Draw(run)
    rd.text((0, 0), "Recharge after any use, even partial.", font=_font(9), fill=white)
    rd.text((0, 14), "Check the extinguisher regularly.", font=_font(9), fill=white)
    run = run.rotate(90, expand=True)
    side.alpha_composite(run, (9, 160))
    side.save(os.path.join(folder, "extinguisher_side.png"))

    # ---------------------------------------------------------------- manometer face, 64 x 64
    g = Image.new("RGBA", (64, 64), white)
    d = ImageDraw.Draw(g)
    box = [6, 6, 57, 57]
    # Scale from 225 degrees (empty, left) to -45 degrees (overfull, right), clockwise on the dial.
    d.arc(box, 135, 232, fill=(206, 34, 34, 255), width=6)
    d.arc(box, 232, 308, fill=(40, 160, 70, 255), width=6)
    d.arc(box, 308, 405, fill=(206, 34, 34, 255), width=6)
    for k in range(11):
        a = math.radians(135 + 27 * k)
        d.line([(32 + 19 * math.cos(a), 32 + 19 * math.sin(a)), (32 + 24 * math.cos(a), 32 + 24 * math.sin(a))],
               fill=ink, width=1)
    # Needle in the green, slightly right of top.
    a = math.radians(-80)
    d.line([(32, 32), (32 + 21 * math.cos(a), 32 + 21 * math.sin(a))], fill=ink, width=2)
    d.ellipse([29, 29, 35, 35], fill=ink)
    centered(d, 40, "bar", 8, ink)
    g.save(os.path.join(folder, "extinguisher_gauge.png"))

    # ---------------------------------------------------------------- inspection sticker, 96 x 64
    c = Image.new("RGBA", (96, 64), white)
    d = ImageDraw.Draw(c)
    d.rectangle([0, 0, 95, 15], fill=(36, 130, 64, 255))
    centered(d, 1, "INSPECTED", 12, white)
    for k, year in enumerate(("25", "26", "27", "28")):
        x = 4 + k * 23
        d.rectangle([x, 19, x + 20, 33], outline=ink, width=1)
        centered(d, 20, year, 10, ink, x, x + 21)
    for k in range(12):
        x = 4 + (k % 6) * 15
        y = 37 + (k // 6) * 13
        d.rectangle([x, y, x + 13, y + 11], outline=ink, width=1)
        centered(d, y, str(k + 1), 8, ink, x, x + 14)
    # Punched month and year.
    d.ellipse([29, 23, 37, 31], fill=ink)
    d.ellipse([68, 51, 76, 59], fill=ink)
    c.save(os.path.join(folder, "extinguisher_check.png"))


def make_tower_tarp(folder):
    """Opaque PVC for the control tower sheets and roof, drawn near white so the dye tints it.

    One tile covers one bay by one lift. Faint woven scrim under the coating, a soft mottle and a few scuffs.
    """
    os.makedirs(folder, exist_ok=True)
    rnd = random.Random(42)
    n = 64
    img = Image.new("RGBA", (n, n))
    px = img.load()
    blobs = [(rnd.uniform(0, n), rnd.uniform(0, n), rnd.uniform(10, 22), rnd.uniform(-6, 6)) for _ in range(9)]
    for y in range(n):
        for x in range(n):
            v = 236.0
            # Polyester scrim showing through the PVC coat.
            if x % 2 == 0:
                v -= 3
            if y % 2 == 0:
                v -= 3
            for bx, by, r, a in blobs:
                dx = min(abs(x - bx), n - abs(x - bx))
                dy = min(abs(y - by), n - abs(y - by))
                d2 = (dx * dx + dy * dy) / (r * r)
                if d2 < 1:
                    v += a * (1 - d2)
            v += rnd.uniform(-2.5, 2.5)
            c = int(max(0, min(255, v)))
            px[x, y] = (c, c, c, 255)
    d = ImageDraw.Draw(img)
    for _ in range(5):
        x0, y0 = rnd.uniform(0, n), rnd.uniform(0, n)
        a = rnd.uniform(0, math.tau)
        ln = rnd.uniform(4, 10)
        d.line([(x0, y0), (x0 + ln * math.cos(a), y0 + ln * math.sin(a))], fill=(222, 222, 222, 255), width=1)
    img.save(os.path.join(folder, "tarp_pvc.png"))
