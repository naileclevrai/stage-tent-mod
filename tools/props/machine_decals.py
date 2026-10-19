"""Decal sheet for the machines (forklift and scissor lifts): stickers, plates, lenses, tyres and printed panels.

Drawn at twice the size and scaled down so the lettering stays clean. Rects are in sheet pixels and must match
MachineDecals.java.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

SIZE = 512
S = 2

RECTS = {
    "hazard": (0, 0, 256, 32),
    "hazard_rw": (256, 0, 256, 32),
    "brand": (0, 32, 256, 64),
    "model_sl08": (256, 32, 128, 32),
    "model_sl1418": (384, 32, 128, 32),
    "model_fd25": (256, 64, 128, 32),
    "text_4x2": (384, 64, 128, 32),
    "warn": (0, 96, 64, 64),
    "crush": (64, 96, 64, 64),
    "tie_down": (128, 96, 64, 64),
    "lift_point": (192, 96, 64, 64),
    "capacity": (256, 96, 128, 64),
    "load_chart": (384, 96, 128, 64),
    "platform_panel": (0, 160, 128, 64),
    "ground_panel": (128, 160, 128, 64),
    "dash": (256, 160, 128, 64),
    "grille": (384, 160, 128, 64),
    "headlight": (0, 224, 64, 64),
    "taillight": (64, 224, 64, 64),
    "beacon": (128, 224, 64, 64),
    "gauge": (192, 224, 64, 64),
    "battery": (256, 224, 128, 32),
    "lowering": (256, 256, 128, 32),
    "manual": (384, 224, 128, 64),
    "wheel": (0, 288, 128, 128),
    "wheel_rt": (128, 288, 128, 128),
    "checker": (256, 288, 128, 64),
    "mesh": (384, 288, 128, 64),
    "pallet": (256, 352, 128, 64),
    "carton": (384, 352, 128, 64),
    "deck": (0, 416, 128, 64),
    "case": (128, 416, 128, 64),
    "arm_tag": (256, 416, 128, 32),
    "no_step": (256, 448, 128, 32),
    "serial": (0, 480, 128, 32),
    "outrigger": (128, 480, 128, 32),
    "chevron": (384, 416, 128, 32),
    "mast_tag": (384, 448, 128, 32),
    "mirror": (384, 480, 64, 32),
    "plug": (448, 480, 64, 32),
}

YELLOW = (246, 196, 20)
BLACK = (24, 24, 26)
WHITE = (246, 246, 242)
RED = (206, 34, 30)
BLUE = (24, 86, 168)


def _font(size, bold=True):
    name = "arialbd.ttf" if bold else "arial.ttf"
    for path in (os.path.join(r"C:\Windows\Fonts", name), os.path.join(r"C:\Windows\Fonts", "arial.ttf")):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def _narrow(size):
    for name in ("arialnb.ttf", "ARIALNB.TTF", "bahnschrift.ttf", "arialbd.ttf"):
        try:
            return ImageFont.truetype(os.path.join(r"C:\Windows\Fonts", name), size)
        except OSError:
            continue
    return ImageFont.load_default()


def _text(d, box, text, font, fill, anchor="mm"):
    x0, y0, x1, y1 = box
    d.text(((x0 + x1) / 2, (y0 + y1) / 2), text, font=font, fill=fill, anchor=anchor)


def _fit(d, box, text, size, fill, narrow=False, pad=0.9):
    """Largest font that fits the box."""
    x0, y0, x1, y1 = box
    w, h = (x1 - x0) * pad, (y1 - y0) * pad
    while size > 6:
        f = _narrow(size) if narrow else _font(size)
        l, t, r, b = d.textbbox((0, 0), text, font=f)
        if r - l <= w and b - t <= h:
            break
        size -= 2
    _text(d, box, text, f, fill)


def _stripes(img, box, a, b, width, slope=1.0):
    """Diagonal bands, clipped to the box."""
    x0, y0, x1, y1 = box
    tile = Image.new("RGBA", (x1 - x0 + 1, y1 - y0 + 1), a + (255,))
    td = ImageDraw.Draw(tile)
    h = tile.height
    x = -h * 2
    while x < tile.width + h:
        td.polygon([(x, h), (x + width, h), (x + width + h * slope, 0), (x + h * slope, 0)], fill=b)
        x += width * 2
    img.paste(tile, (x0, y0))


def _triangle_warn(d, box, symbol):
    x0, y0, x1, y1 = box
    m = (x1 - x0) * 0.06
    d.rectangle(box, fill=(255, 255, 255, 0))
    pts = [((x0 + x1) / 2, y0 + m), (x1 - m, y1 - m * 1.4), (x0 + m, y1 - m * 1.4)]
    d.polygon(pts, fill=BLACK)
    inner = [((x0 + x1) / 2, y0 + m * 4.2), (x1 - m * 3.2, y1 - m * 3.0), (x0 + m * 3.2, y1 - m * 3.0)]
    d.polygon(inner, fill=YELLOW)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2 + (y1 - y0) * 0.1
    s = (x1 - x0)
    if symbol == "!":
        d.rectangle((cx - s * 0.04, cy - s * 0.2, cx + s * 0.04, cy + s * 0.06), fill=BLACK)
        d.ellipse((cx - s * 0.05, cy + s * 0.11, cx + s * 0.05, cy + s * 0.21), fill=BLACK)


def _square_picto(d, box, kind):
    x0, y0, x1, y1 = box
    s = x1 - x0
    d.rounded_rectangle((x0 + 2 * S, y0 + 2 * S, x1 - 2 * S, y1 - 2 * S), radius=6 * S, fill=YELLOW, outline=BLACK, width=3 * S)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    if kind == "crush":
        # Two jaws closing on a hand.
        d.polygon([(x0 + s * 0.18, y0 + s * 0.22), (x1 - s * 0.18, y0 + s * 0.22), (cx, cy - s * 0.02)], fill=BLACK)
        d.polygon([(x0 + s * 0.18, y1 - s * 0.22), (x1 - s * 0.18, y1 - s * 0.22), (cx, cy + s * 0.02)], fill=BLACK)
        d.rounded_rectangle((x0 + s * 0.1, cy - s * 0.06, cx + s * 0.05, cy + s * 0.06), radius=s * 0.05, fill=BLACK)
        for k in range(3):
            d.rounded_rectangle((cx - s * 0.02, cy - s * 0.06 + k * s * 0.04, cx + s * 0.12, cy - s * 0.03 + k * s * 0.04),
                                radius=s * 0.015, fill=BLACK)
    elif kind == "tie":
        d.ellipse((cx - s * 0.14, y0 + s * 0.18, cx + s * 0.14, y0 + s * 0.42), outline=BLACK, width=5 * S)
        d.line((cx, y0 + s * 0.42, cx, cy + s * 0.12), fill=BLACK, width=6 * S)
        d.arc((cx - s * 0.18, cy - s * 0.04, cx + s * 0.18, y1 - s * 0.16), 0, 200, fill=BLACK, width=6 * S)
    elif kind == "lift":
        # Hook over a box: lifting point.
        d.rectangle((x0 + s * 0.22, cy + s * 0.06, x1 - s * 0.22, y1 - s * 0.18), fill=BLACK)
        d.line((cx, y0 + s * 0.16, cx, cy + s * 0.06), fill=BLACK, width=5 * S)
        d.arc((cx - s * 0.1, y0 + s * 0.12, cx + s * 0.1, y0 + s * 0.3), 180, 360, fill=BLACK, width=4 * S)


def _lens(img, box, rim, glass, hot, rings):
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = box
    d.ellipse(box, fill=rim)
    m = (x1 - x0) * 0.1
    d.ellipse((x0 + m, y0 + m, x1 - m, y1 - m), fill=glass)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    r = (x1 - x0) / 2 - m
    for k in range(rings):
        rr = r * (1 - k / rings)
        t = k / max(1, rings - 1)
        col = tuple(int(glass[i] * (1 - t) + hot[i] * t) for i in range(3))
        d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), outline=col, width=max(1, int(r / rings * 0.6)))
    d.ellipse((cx - r * 0.35, cy - r * 0.35, cx + r * 0.35, cy + r * 0.35), fill=hot)


def _wheel(d, box, tyre, rim, knobby):
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    r = (x1 - x0) / 2
    d.ellipse(box, fill=(255, 255, 255, 0))
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=tyre)
    if knobby:
        for k in range(28):
            a = k / 28 * math.tau
            p = [(cx + math.cos(a + o) * rr, cy + math.sin(a + o) * rr) for o, rr in
                 ((-0.05, r * 0.99), (0.05, r * 0.99), (0.04, r * 0.86), (-0.04, r * 0.86))]
            d.polygon(p, fill=tuple(max(0, c - 14) for c in tyre))
    # Sidewall rings and lettering band.
    d.ellipse((cx - r * 0.86, cy - r * 0.86, cx + r * 0.86, cy + r * 0.86), outline=tuple(c + 10 for c in tyre), width=2 * S)
    d.ellipse((cx - r * 0.74, cy - r * 0.74, cx + r * 0.74, cy + r * 0.74), outline=tuple(max(0, c - 8) for c in tyre), width=1 * S)
    # Rim.
    rr = r * 0.62
    d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), fill=rim)
    d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), outline=tuple(max(0, c - 50) for c in rim), width=2 * S)
    d.ellipse((cx - rr * 0.84, cy - rr * 0.84, cx + rr * 0.84, cy + rr * 0.84), outline=tuple(max(0, c - 30) for c in rim), width=2 * S)
    # Dish holes.
    for k in range(6):
        a = k / 6 * math.tau + 0.3
        hx, hy = cx + math.cos(a) * rr * 0.6, cy + math.sin(a) * rr * 0.6
        d.ellipse((hx - rr * 0.12, hy - rr * 0.12, hx + rr * 0.12, hy + rr * 0.12), fill=(40, 40, 42))
    # Hub and studs.
    hr = rr * 0.34
    d.ellipse((cx - hr, cy - hr, cx + hr, cy + hr), fill=tuple(max(0, c - 25) for c in rim))
    for k in range(5):
        a = k / 5 * math.tau
        hx, hy = cx + math.cos(a) * hr * 0.66, cy + math.sin(a) * hr * 0.66
        d.ellipse((hx - hr * 0.13, hy - hr * 0.13, hx + hr * 0.13, hy + hr * 0.13), fill=(200, 200, 196))
    d.ellipse((cx - hr * 0.3, cy - hr * 0.3, cx + hr * 0.3, cy + hr * 0.3), fill=(70, 70, 72))


def make_machine_decals(folder):
    os.makedirs(folder, exist_ok=True)
    big = SIZE * S
    img = Image.new("RGBA", (big, big), (255, 255, 255, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(7)

    def box(name):
        x, y, w, h = RECTS[name]
        return (x * S, y * S, (x + w) * S - 1, (y + h) * S - 1)

    _stripes(img, box("hazard"), BLACK, YELLOW, 16 * S)
    _stripes(img, box("hazard_rw"), WHITE, RED, 16 * S)

    # Brand wordmark, white on clear, with a swoosh under it.
    b = box("brand")
    _fit(d, (b[0], b[1], b[2], b[3] - 14 * S), "STAGELIFT", 64 * S, WHITE, narrow=True, pad=0.96)
    d.polygon([(b[0] + 8 * S, b[3] - 10 * S), (b[2] - 8 * S, b[3] - 16 * S), (b[2] - 8 * S, b[3] - 11 * S),
               (b[0] + 8 * S, b[3] - 6 * S)], fill=WHITE)

    _fit(d, box("model_sl08"), "SL08E", 30 * S, WHITE, narrow=True)
    m = box("model_sl1418")
    d.rounded_rectangle(m, radius=4 * S, fill=BLUE)
    d.rounded_rectangle((m[0] + 2 * S, m[1] + 2 * S, m[2] - 2 * S, m[3] - 2 * S), radius=3 * S, outline=WHITE, width=1 * S)
    _fit(d, m, "SL1418 RT", 24 * S, WHITE, narrow=True)
    _fit(d, box("model_fd25"), "FD25", 30 * S, BLACK, narrow=True)
    _fit(d, box("text_4x2"), "4X2", 30 * S, WHITE, narrow=True)

    _triangle_warn(d, box("warn"), "!")
    _square_picto(d, box("crush"), "crush")
    _square_picto(d, box("tie_down"), "tie")
    _square_picto(d, box("lift_point"), "lift")

    # Capacity plate: white, red border, kg and persons.
    c = box("capacity")
    d.rectangle(c, fill=WHITE)
    d.rectangle(c, outline=RED, width=3 * S)
    _fit(d, (c[0], c[1] + 4 * S, c[2], c[1] + 30 * S), "MAX 230 kg", 22 * S, BLACK)
    # Two persons.
    for k in range(2):
        px = c[0] + 36 * S + k * 22 * S
        py = c[1] + 36 * S
        d.ellipse((px - 4 * S, py, px + 4 * S, py + 8 * S), fill=BLACK)
        d.rectangle((px - 5 * S, py + 9 * S, px + 5 * S, py + 22 * S), fill=BLACK)
    _fit(d, (c[0] + 70 * S, c[1] + 34 * S, c[2] - 4 * S, c[3] - 4 * S), "2", 22 * S, BLACK)

    # Forklift load chart: a small graph on a silver plate.
    lc = box("load_chart")
    d.rectangle(lc, fill=(206, 208, 210))
    d.rectangle(lc, outline=(90, 92, 96), width=2 * S)
    gx0, gy0, gx1, gy1 = lc[0] + 14 * S, lc[1] + 10 * S, lc[2] - 10 * S, lc[3] - 12 * S
    d.line((gx0, gy0, gx0, gy1, gx1, gy1), fill=BLACK, width=1 * S)
    d.line([(gx0, gy0 + 6 * S), (gx0 + (gx1 - gx0) * 0.45, gy0 + 6 * S), (gx1, gy1 - 6 * S)], fill=RED, width=2 * S)
    for k in range(4):
        y = gy0 + (gy1 - gy0) * k / 4
        d.line((gx0 - 3 * S, y, gx0, y), fill=BLACK, width=1 * S)
    _fit(d, (lc[0], lc[3] - 11 * S, lc[2], lc[3]), "2500 kg  500 mm  3000 mm", 9 * S, BLACK)

    # Platform control box face: e-stop, mode switch, horn, LEDs.
    p = box("platform_panel")
    d.rectangle(p, fill=(36, 38, 42))
    d.rectangle(p, outline=(90, 92, 96), width=2 * S)
    cx = p[0] + 24 * S
    cy = p[1] + 28 * S
    d.ellipse((cx - 16 * S, cy - 16 * S, cx + 16 * S, cy + 16 * S), fill=YELLOW)
    d.ellipse((cx - 11 * S, cy - 11 * S, cx + 11 * S, cy + 11 * S), fill=RED)
    d.ellipse((cx - 7 * S, cy - 7 * S, cx + 3 * S, cy + 1 * S), fill=(236, 90, 80))
    for k, (lab, col) in enumerate((("LIFT", (230, 230, 230)), ("DRIVE", (230, 230, 230)), ("SPEED", (230, 230, 230)))):
        bx = p[0] + 54 * S + k * 24 * S
        d.ellipse((bx - 7 * S, cy - 7 * S, bx + 7 * S, cy + 7 * S), fill=(70, 72, 78), outline=(150, 150, 150), width=1 * S)
        _fit(d, (bx - 12 * S, cy + 9 * S, bx + 12 * S, cy + 18 * S), lab, 7 * S, col)
    for k, col in enumerate(((60, 220, 70), (250, 190, 40), (230, 50, 40))):
        lx = p[0] + 54 * S + k * 10 * S
        d.ellipse((lx - 3 * S, p[1] + 6 * S, lx + 3 * S, p[1] + 12 * S), fill=col)
    _fit(d, (p[0], p[3] - 12 * S, p[2], p[3] - 2 * S), "PLATFORM CONTROL", 8 * S, (200, 200, 200))

    # Ground control panel on the chassis: key switch, e-stop, hour meter, battery gauge.
    g = box("ground_panel")
    d.rectangle(g, fill=(46, 48, 52))
    d.rectangle(g, outline=(120, 122, 126), width=2 * S)
    ex, ey = g[0] + 22 * S, g[1] + 30 * S
    d.ellipse((ex - 15 * S, ey - 15 * S, ex + 15 * S, ey + 15 * S), fill=YELLOW)
    d.ellipse((ex - 10 * S, ey - 10 * S, ex + 10 * S, ey + 10 * S), fill=RED)
    kx = g[0] + 56 * S
    d.ellipse((kx - 9 * S, ey - 9 * S, kx + 9 * S, ey + 9 * S), fill=(170, 172, 176))
    d.rectangle((kx - 2 * S, ey - 7 * S, kx + 2 * S, ey + 7 * S), fill=(40, 40, 40))
    _fit(d, (kx - 18 * S, ey + 10 * S, kx + 18 * S, ey + 18 * S), "OFF  PLAT  GND", 6 * S, WHITE)
    d.rectangle((g[0] + 80 * S, g[1] + 12 * S, g[2] - 8 * S, g[1] + 26 * S), fill=(150, 170, 140))
    _fit(d, (g[0] + 80 * S, g[1] + 12 * S, g[2] - 8 * S, g[1] + 26 * S), "01284.6", 10 * S, BLACK)
    for k in range(5):
        col = (60, 220, 70) if k < 4 else (80, 80, 80)
        d.rectangle((g[0] + 82 * S + k * 7 * S, g[1] + 32 * S, g[0] + 86 * S + k * 7 * S, g[1] + 40 * S), fill=col)
    _fit(d, (g[0], g[3] - 12 * S, g[2], g[3] - 2 * S), "GROUND CONTROL", 8 * S, (210, 210, 210))

    # Forklift dashboard: LCD with fuel, hours, warning lamps.
    da = box("dash")
    d.rectangle(da, fill=(30, 31, 34))
    d.rounded_rectangle((da[0] + 30 * S, da[1] + 8 * S, da[2] - 30 * S, da[1] + 40 * S), radius=4 * S, fill=(120, 170, 190))
    _fit(d, (da[0] + 30 * S, da[1] + 8 * S, da[2] - 30 * S, da[1] + 26 * S), "1284 h", 14 * S, (14, 30, 40))
    for k in range(6):
        d.rectangle((da[0] + 36 * S + k * 9 * S, da[1] + 30 * S, da[0] + 42 * S + k * 9 * S, da[1] + 36 * S),
                    fill=(14, 30, 40) if k < 5 else (120, 170, 190))
    for k, col in enumerate(((230, 60, 40), (250, 180, 30), (60, 200, 80), (60, 130, 240))):
        lx = da[0] + 10 * S + (k % 2) * 108 * S
        ly = da[1] + 14 * S + (k // 2) * 18 * S
        d.ellipse((lx - 5 * S, ly - 5 * S, lx + 5 * S, ly + 5 * S), fill=col)
    _fit(d, (da[0], da[3] - 18 * S, da[2], da[3] - 4 * S), "P  N  F  R", 10 * S, (210, 210, 210))

    # Vent grille: dark louvres.
    gr = box("grille")
    d.rectangle(gr, fill=(255, 255, 255, 0))
    for k in range(7):
        y = gr[1] + 6 * S + k * 8 * S
        d.rounded_rectangle((gr[0] + 6 * S, y, gr[2] - 6 * S, y + 4 * S), radius=2 * S, fill=(20, 20, 22))

    _lens(img, box("headlight"), (70, 70, 74), (230, 236, 240), (255, 255, 250), 5)
    _lens(img, box("taillight"), (60, 60, 64), (170, 20, 18), (255, 90, 70), 4)
    _lens(img, box("beacon"), (40, 40, 40), (240, 130, 10), (255, 220, 90), 4)
    ga = box("gauge")
    d.ellipse(ga, fill=(50, 50, 52))
    d.ellipse((ga[0] + 6 * S, ga[1] + 6 * S, ga[2] - 6 * S, ga[3] - 6 * S), fill=WHITE)
    gcx, gcy = (ga[0] + ga[2]) / 2, (ga[1] + ga[3]) / 2
    for k in range(9):
        a = math.radians(135 + k * 33.75)
        r0, r1 = 18 * S, 23 * S
        d.line((gcx + math.cos(a) * r0, gcy + math.sin(a) * r0, gcx + math.cos(a) * r1, gcy + math.sin(a) * r1),
               fill=RED if k > 6 else BLACK, width=2 * S)
    a = math.radians(200)
    d.line((gcx, gcy, gcx + math.cos(a) * 20 * S, gcy + math.sin(a) * 20 * S), fill=RED, width=2 * S)

    for name, text, bg, fg in (("battery", "24V DC  BATTERY", BLACK, YELLOW),
                               ("lowering", "EMERGENCY LOWERING", RED, WHITE),
                               ("no_step", "NO STEP", YELLOW, BLACK),
                               ("serial", "S/N 1418-06-2214", (200, 200, 196), BLACK),
                               ("outrigger", "OUTRIGGER", YELLOW, BLACK),
                               ("mast_tag", "KEEP CLEAR OF MAST", YELLOW, BLACK)):
        bb = box(name)
        d.rectangle(bb, fill=bg)
        d.rectangle(bb, outline=fg, width=1 * S)
        _fit(d, bb, text, 20 * S, fg, narrow=True)

    ma = box("manual")
    d.rectangle(ma, fill=(30, 30, 32))
    d.rectangle((ma[0] + 6 * S, ma[1] + 6 * S, ma[2] - 6 * S, ma[3] - 6 * S), fill=YELLOW)
    _fit(d, (ma[0] + 6 * S, ma[1] + 6 * S, ma[2] - 6 * S, ma[1] + 36 * S), "MANUAL", 22 * S, BLACK)
    _fit(d, (ma[0] + 6 * S, ma[1] + 34 * S, ma[2] - 6 * S, ma[3] - 8 * S), "READ BEFORE USE", 10 * S, BLACK)

    _wheel(d, box("wheel"), (34, 34, 36), (196, 198, 200), False)
    _wheel(d, box("wheel_rt"), (30, 30, 32), (240, 186, 26), True)

    # Diamond checker plate.
    ch = box("checker")
    d.rectangle(ch, fill=(150, 154, 158))
    for yy in range(ch[1], ch[3], 12 * S):
        for xx in range(ch[0] + ((yy - ch[1]) // (12 * S)) % 2 * 6 * S, ch[2], 12 * S):
            d.line((xx, yy + 8 * S, xx + 6 * S, yy + 2 * S), fill=(206, 210, 214), width=2 * S)
            d.line((xx + 1 * S, yy + 9 * S, xx + 7 * S, yy + 3 * S), fill=(96, 98, 102), width=1 * S)
    # Expanded metal grating.
    me = box("mesh")
    tile = Image.new("RGBA", (me[2] - me[0] + 1, me[3] - me[1] + 1), (255, 255, 255, 0))
    td = ImageDraw.Draw(tile)
    for yy in range(0, tile.height + 8 * S, 8 * S):
        for xx in range(-16 * S, tile.width + 16 * S, 16 * S):
            x = xx + (8 * S if (yy // (8 * S)) % 2 else 0)
            td.polygon([(x, yy), (x + 8 * S, yy - 4 * S), (x + 16 * S, yy), (x + 8 * S, yy + 4 * S)], outline=(70, 72, 76), width=2 * S)
    img.paste(tile, (me[0], me[1]))

    # Pallet board, cardboard, deck top, flight case face.
    pa = box("pallet")
    d.rectangle(pa, fill=(196, 160, 112))
    for k in range(30):
        y = rnd.randint(pa[1], pa[3])
        d.line((pa[0], y, pa[2], y + rnd.randint(-3, 3)), fill=(176, 140, 94), width=1 * S)
    d.rectangle((pa[0] + 8 * S, pa[1] + 22 * S, pa[0] + 38 * S, pa[1] + 42 * S), outline=(60, 40, 24), width=2 * S)
    _fit(d, (pa[0] + 8 * S, pa[1] + 22 * S, pa[0] + 38 * S, pa[1] + 42 * S), "EPAL", 10 * S, (60, 40, 24))
    for k in range(3):
        nx = pa[0] + 60 * S + k * 20 * S
        for yy in (pa[1] + 10 * S, pa[3] - 10 * S):
            d.ellipse((nx - 2 * S, yy - 2 * S, nx + 2 * S, yy + 2 * S), fill=(80, 70, 60))
    ca = box("carton")
    d.rectangle(ca, fill=(196, 150, 96))
    d.rectangle((ca[0], ca[1] + 26 * S, ca[2], ca[1] + 38 * S), fill=(214, 186, 140))
    d.rectangle((ca[0] + 70 * S, ca[1] + 6 * S, ca[2] - 8 * S, ca[1] + 22 * S), fill=WHITE)
    _fit(d, (ca[0] + 70 * S, ca[1] + 6 * S, ca[2] - 8 * S, ca[1] + 22 * S), "STAGE 2", 9 * S, BLACK)
    _fit(d, (ca[0] + 4 * S, ca[1] + 42 * S, ca[0] + 64 * S, ca[3] - 4 * S), "FRAGILE", 14 * S, RED)
    for k in range(2):
        ux = ca[0] + 14 * S + k * 18 * S
        d.polygon([(ux, ca[1] + 20 * S), (ux - 5 * S, ca[1] + 12 * S), (ux + 5 * S, ca[1] + 12 * S)], fill=BLACK)
        d.rectangle((ux - 1 * S, ca[1] + 16 * S, ux + 1 * S, ca[1] + 24 * S), fill=BLACK)
    de = box("deck")
    d.rectangle(de, fill=(40, 34, 30))
    for k in range(400):
        x, y = rnd.randint(de[0], de[2]), rnd.randint(de[1], de[3])
        d.point((x, y), fill=(56, 48, 42))
    d.rectangle(de, outline=(170, 172, 176), width=3 * S)
    cs = box("case")
    d.rectangle(cs, fill=(28, 28, 30))
    d.rectangle(cs, outline=(196, 198, 202), width=4 * S)
    d.rectangle((cs[0] + 40 * S, cs[1] + 18 * S, cs[2] - 40 * S, cs[1] + 40 * S), fill=WHITE)
    _fit(d, (cs[0] + 40 * S, cs[1] + 18 * S, cs[2] - 40 * S, cs[1] + 40 * S), "FOH  #2", 11 * S, BLACK)

    at = box("arm_tag")
    d.rectangle(at, fill=(255, 255, 255, 0))
    d.rectangle((at[0] + 4 * S, at[1] + 4 * S, at[0] + 56 * S, at[3] - 4 * S), fill=YELLOW)
    _fit(d, (at[0] + 4 * S, at[1] + 4 * S, at[0] + 56 * S, at[3] - 4 * S), "SL", 14 * S, BLACK)
    d.rectangle((at[0] + 70 * S, at[1] + 4 * S, at[2] - 4 * S, at[3] - 4 * S), fill=YELLOW)
    _fit(d, (at[0] + 70 * S, at[1] + 4 * S, at[2] - 4 * S, at[3] - 4 * S), "12", 14 * S, BLACK)

    cv = box("chevron")
    tile = Image.new("RGBA", (cv[2] - cv[0] + 1, cv[3] - cv[1] + 1), YELLOW + (255,))
    td = ImageDraw.Draw(tile)
    hh = tile.height
    for k in range(-2, 12):
        x = k * 16 * S
        td.polygon([(x, 0), (x + 8 * S, 0), (x + 16 * S, hh / 2), (x + 8 * S, hh), (x, hh), (x + 8 * S, hh / 2)], fill=BLACK)
    td.rectangle((0, 0, tile.width, 2 * S), fill=BLACK)
    td.rectangle((0, hh - 2 * S, tile.width, hh), fill=BLACK)
    img.paste(tile, (cv[0], cv[1]))

    mi = box("mirror")
    d.rounded_rectangle(mi, radius=6 * S, fill=(30, 30, 32))
    d.rounded_rectangle((mi[0] + 4 * S, mi[1] + 4 * S, mi[2] - 4 * S, mi[3] - 4 * S), radius=4 * S, fill=(150, 176, 196))
    d.line((mi[0] + 14 * S, mi[3] - 8 * S, mi[0] + 30 * S, mi[1] + 8 * S), fill=(220, 236, 246), width=3 * S)
    pl = box("plug")
    d.rectangle(pl, fill=(30, 30, 32))
    d.ellipse((pl[0] + 6 * S, pl[1] + 4 * S, pl[0] + 30 * S, pl[3] - 4 * S), fill=(40, 90, 200))
    for k in range(3):
        a = math.radians(90 + k * 120)
        hx, hy = pl[0] + 18 * S + math.cos(a) * 6 * S, (pl[1] + pl[3]) / 2 + math.sin(a) * 6 * S
        d.ellipse((hx - 2 * S, hy - 2 * S, hx + 2 * S, hy + 2 * S), fill=(10, 10, 10))
    _fit(d, (pl[0] + 32 * S, pl[1], pl[2], pl[3]), "230V", 12 * S, WHITE)

    out = img.resize((SIZE, SIZE), Image.LANCZOS)
    # Hard alpha: the sheet is drawn with a cutout render type.
    px = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b_, a = px[x, y]
            px[x, y] = (r, g, b_, 255 if a >= 128 else 0)
    path = os.path.join(folder, "machine_decals.png")
    out.save(path)
    return path
