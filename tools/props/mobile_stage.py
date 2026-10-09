"""Deployed 8.20 x 6.60 m trailer stage, exported to the cached tent renderer.

Run: python tools/props/mobile_stage.py
Local +x faces the audience; the trailer and its axles run along z.
"""
import json
import math
import os
import random
import struct

from PIL import Image
from mesh import Mesh, _cross, _sub, _norm
from export import _write_obj

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'stagetents')
MATERIALS = ['galvanised', 'metal', 'chrome', 'black', 'rubber', 'stage_canvas',
             'stage_drape', 'stage_deck', 'tread', 'red', 'orange', 'stage_label']
AX, AZ, DECK = 3.30, 4.10, 1.0
SCALE = 2.0  # The in-game festival version is twice the brochure's dimensions.
# Fold groups. The order is the part byte written into the mesh and read by StageFold.
BODY, WING_POS, WING_NEG, ROOF, AWNING_POS, AWNING_NEG = range(6)
FACADE, CURTAIN, STAIRS, JACK, JACK_POS, JACK_NEG, POST, STRUT = range(6, 14)


def roof_part(x):
    """Centre cassette, or the hinged awning on either side of x = ±1.22."""
    if x < -1.30:
        return AWNING_NEG
    if x > 1.30:
        return AWNING_POS
    return ROOF


def bar(m, mat, a, b, r=0.024, segs=8):
    m.tube(mat, [a, b], r, segs=segs, caps=True)


def bolt(m, p, axis=(1, 0, 0), radius=0.014):
    end = tuple(p[i] + axis[i] * 0.014 for i in range(3))
    bar(m, 'chrome', p, end, radius, 6)


def face(m, mat, corners, n=None, double=False, uv=None):
    n = n or _norm(_cross(_sub(corners[1], corners[0]), _sub(corners[2], corners[0])))
    uv = uv or ((0, 1), (1, 1), (1, 0), (0, 0))
    m.quad(mat, *corners, n=n, uv=uv)
    if double:
        # The lining sits just inside the shell. The same plane z-fights, especially on the roof.
        gap = 0.015
        back = tuple(tuple(p[k] - n[k] * gap for k in range(3)) for p in corners)
        m.quad(mat, *back[::-1], n=tuple(-v for v in n), uv=uv[::-1])


def roof_y(x, z=0):
    # Flat central roof cassette and hinged awnings pitched gently down to their edges.
    pitch = max(0, abs(x) - 1.25) * 0.055
    sag = 0.014 * math.sin(z * math.pi / 0.82) ** 2
    return 5.64 - pitch - sag


def wheel(m, cx, cy, cz, radius=0.365, thickness=0.26):
    """Tyre shoulders, tread, inset rim, holes, hub and five separate wheel bolts. Axis is x."""
    segs = 24
    profile = [(-thickness / 2, radius * .82), (-thickness * .43, radius * .95),
               (-thickness * .3, radius), (thickness * .3, radius),
               (thickness * .43, radius * .95), (thickness / 2, radius * .82)]
    for (xa, ra), (xb, rb) in zip(profile, profile[1:]):
        for i in range(segs):
            a, b = math.tau * i / segs, math.tau * (i + 1) / segs
            pts = [(cx + xa, cy + ra * math.sin(a), cz + ra * math.cos(a)),
                   (cx + xb, cy + rb * math.sin(a), cz + rb * math.cos(a)),
                   (cx + xb, cy + rb * math.sin(b), cz + rb * math.cos(b)),
                   (cx + xa, cy + ra * math.sin(b), cz + ra * math.cos(b))]
            face(m, 'rubber', pts, (0, math.sin((a + b) / 2), math.cos((a + b) / 2)))
    sign = 1 if cx > 0 else -1
    side = cx + sign * (thickness / 2 + .008)
    # The old profile only had a tread ring. Close both sidewalls so the world
    # cannot be seen through the tyre between its shoulder and the rim.
    for side_sign in (-1, 1):
        face_x = cx + side_sign * (thickness / 2 + .003)
        n = (side_sign, 0, 0)
        for i in range(segs):
            a, b = math.tau * i / segs, math.tau * (i + 1) / segs
            centre = (face_x, cy, cz)
            p1 = (face_x, cy + radius * .82 * math.sin(a), cz + radius * .82 * math.cos(a))
            p2 = (face_x, cy + radius * .82 * math.sin(b), cz + radius * .82 * math.cos(b))
            uv1 = (.5 + .5 * math.cos(a), .5 + .5 * math.sin(a))
            uv2 = (.5 + .5 * math.cos(b), .5 + .5 * math.sin(b))
            if side_sign > 0:
                m.tri('rubber', centre, p2, p1, n, n, n, (.5, .5), uv2, uv1)
            else:
                m.tri('rubber', centre, p1, p2, n, n, n, (.5, .5), uv1, uv2)
    bar(m, 'metal', (side - sign * .018, cy, cz), (side, cy, cz), radius * .61, 24)
    bar(m, 'black', (side, cy, cz), (side + sign * .006, cy, cz), radius * .48, 20)
    bar(m, 'metal', (side + sign * .009, cy, cz), (side + sign * .02, cy, cz), radius * .37, 20)
    bar(m, 'chrome', (side + sign * .02, cy, cz), (side + sign * .055, cy, cz), radius * .18, 12)
    for i in range(5):
        a = i * math.tau / 5
        bolt(m, (side + sign * .025, cy + radius * .26 * math.sin(a), cz + radius * .26 * math.cos(a)),
             (sign, 0, 0), .012)
        a += math.pi / 5
        bar(m, 'rubber', (side + sign * .022, cy + radius * .50 * math.sin(a), cz + radius * .50 * math.cos(a)),
            (side + sign * .026, cy + radius * .50 * math.sin(a), cz + radius * .50 * math.cos(a)), radius * .065, 8)
    # Shallow tread ribs across the tyre's running surface.
    for i in range(24):
        a = (i + .4) * math.tau / 24
        y, z = cy + (radius + .004) * math.sin(a), cz + (radius + .004) * math.cos(a)
        bar(m, 'black', (cx - thickness * .28, y, z), (cx + thickness * .28, y, z), .008, 4)


def fender(m, cx, cz):
    """Curved wheel guard; there are no rectangular shelves above the wheels."""
    for i in range(16):
        a, b = .08 + (math.pi - .16) * i / 16, .08 + (math.pi - .16) * (i + 1) / 16
        for radius, mat in ((.415, 'black'), (.432, 'galvanised')):
            pts = [(cx - .17, .38 + radius * math.sin(a), cz + radius * math.cos(a)),
                   (cx + .17, .38 + radius * math.sin(a), cz + radius * math.cos(a)),
                   (cx + .17, .38 + radius * math.sin(b), cz + radius * math.cos(b)),
                   (cx - .17, .38 + radius * math.sin(b), cz + radius * math.cos(b))]
            face(m, mat, pts, (0, math.sin((a + b) / 2), math.cos((a + b) / 2)), double=True)


def chassis(m):
    # Two central longitudinal chassis members, crossmembers and torsion axles.
    m.part = BODY
    for x in (-1.06, 1.06):
        m.box('galvanised', x - .055, .43, -4.04, x + .055, .65, 4.04)
        m.box('metal', x - .071, .63, -4.04, x + .071, .67, 4.04)
    for z in (-3.9, -3.08, -2.26, -1.44, -.62, .20, 1.02, 1.84, 2.66, 3.48, 3.9):
        m.box('galvanised', -1.18, .53, z - .04, 1.18, .66, z + .04)
    for z in (-.67, .67):
        bar(m, 'black', (-1.3, .40, z), (1.3, .40, z), .065, 8)
        for x in (-1.32, 1.32):
            wheel(m, x, .38, z)
            fender(m, x, z)
            m.box('black', x - .08, .49, z - .17, x + .08, .60, z + .17)
    # Hinged extension decks: separate diagonal stays and pinned underframe on each wing.
    for side in (-1, 1):
        m.part = WING_POS if side > 0 else WING_NEG
        for z in (-3.92, -2.36, -.80, .80, 2.36, 3.92):
            x0, x1 = side * 1.23, side * 3.18
            bar(m, 'galvanised', (x0, .80, z), (x1, .80, z), .045, 4)
            bar(m, 'metal', (x0, .44, z), (x1, .80, z), .027, 6)
            m.box('metal', x1 - .10, .72, z - .07, x1 + .10, .86, z + .07)
            bolt(m, (x1, .79, z + .076), (0, 0, 1), .023)
        for z in (-3.4, -1.8, 1.8, 3.4):
            m.box('orange', side * 1.135 - .005, .50, z - .045, side * 1.135 + .005, .56, z + .045)
    # Tail lamp carrier, number plate and reflectors, all unlit.
    m.part = BODY
    m.box('galvanised', -1.17, .43, 4.04, 1.17, .61, 4.12)
    for x in (-.93, .93):
        m.bevel_box('black', x - .18, .44, 4.11, x + .18, .62, 4.16, .025)
        m.box('red', x - .15, .465, 4.162, x + .065, .59, 4.17)
        m.box('orange', x + .075, .465, 4.162, x + .15, .59, 4.17)
    m.box('black', -.29, .46, 4.125, .29, .59, 4.135)
    face(m, 'stage_label', [(-.28, .47, 4.136), (.28, .47, 4.136),
                            (.28, .58, 4.136), (-.28, .58, 4.136)], (0, 0, 1))


def drawbar(m):
    m.part = BODY
    for x in (-1.04, 1.04):
        bar(m, 'galvanised', (x, .54, -3.88), (0, .54, -5.63), .067, 4)
    m.box('galvanised', -.075, .455, -5.96, .075, .63, -5.44)
    bar(m, 'black', (0, .54, -5.74), (0, .54, -5.95), .087, 12)
    for i in range(9):
        z = -5.75 - i * .024
        bar(m, 'rubber', (0, .54, z), (0, .54, z - .014), .092, 12)
    m.bevel_box('metal', -.10, .46, -6.12, .10, .62, -5.96, .035)
    m.box('black', -.066, .444, -6.10, .066, .46, -5.98)
    bar(m, 'metal', (0, .64, -6.05), (0, .72, -5.91), .02, 6)
    m.bevel_box('black', -.06, .70, -5.96, .06, .745, -5.86, .008)
    # Jockey wheel, clamp and winding handle.
    bar(m, 'metal', (.30, .19, -5.48), (.30, .83, -5.48), .045, 10)
    bar(m, 'chrome', (.30, .13, -5.48), (.30, .51, -5.48), .028, 8)
    m.box('galvanised', .235, .45, -5.54, .37, .62, -5.42)
    wheel(m, .30, .14, -5.48, .115, .085)
    bar(m, 'metal', (.30, .83, -5.48), (.46, .83, -5.48), .018, 6)
    bar(m, 'black', (.46, .83, -5.48), (.46, .91, -5.48), .025, 8)
    for x in (-.13, .13):
        points = [(x, .47, -5.5), (x * 2, .27, -5.7), (x * 1.6, .28, -5.95), (x, .46, -6.0)]
        m.tube('black', points, .012, segs=6)


def jack(m, x, z):
    m.bevel_box('black', x - .17, .035, z - .17, x + .17, .070, z + .17, .02)
    m.cylinder('chrome', .031, .075, .60, cx=x, cz=z, segs=8)
    m.cylinder('galvanised', .048, .35, .91, cx=x, cz=z, segs=8)
    m.cylinder('metal', .075, .39, .47, cx=x, cz=z, segs=6)
    m.box('metal', x - .10, .73, z - .08, x + .10, .89, z + .08)
    bolt(m, (x + .104, .79, z), radius=.021)
    bar(m, 'chrome', (x, .92, z), (x + .12, .92, z), .015, 6)
    bar(m, 'black', (x + .12, .92, z), (x + .12, 1.02, z), .022, 8)
    for i in range(6):
        m.cylinder('metal', .036, .16 + i * .024, .17 + i * .024, cx=x, cz=z, segs=8)


def hydraulics(m):
    # Deck stabilisation and diagonal telescopic stays.
    for x in (-3.16, 3.16):
        m.part = JACK_POS if x > 0 else JACK_NEG
        for z in (-3.93, 3.93):
            jack(m, x, z)
            m.part = WING_POS if x > 0 else WING_NEG
            bar(m, 'galvanised', (x, .84, z), (math.copysign(1.1, x), .56, z), .035, 4)
            m.part = JACK_POS if x > 0 else JACK_NEG
    m.part = JACK
    for x in (-1.15, 1.15):
        for z in (-3.44, 3.44):
            jack(m, x, z)
    # Hydraulic pump housing with vent slats, reservoir, pump lever and flexible hose runs.
    m.part = BODY
    m.bevel_box('black', -1.75, .49, 2.6, -1.30, .79, 3.13, .025)
    for i in range(7):
        m.box('metal', -1.762, .54 + i * .027, 2.67, -1.748, .549 + i * .027, 3.05)
    bar(m, 'metal', (-1.70, .78, 2.75), (-1.75, .95, 2.87), .014, 6)
    bar(m, 'black', (-1.75, .95, 2.87), (-1.75, .95, 3.03), .023, 8)
    m.part = POST
    for z in (-3.7, 3.7):
        for side in (-1, 1):
            x = side * 1.16
            # Fixed outer sleeve and chrome telescopic inner section of each lifting post.
            m.part = POST
            m.box('galvanised', x - .065, .82, z - .065, x + .065, 3.50, z + .065)
            m.box('chrome', x - .043, 3.38, z - .043, x + .043, 5.43, z + .043)
            m.box('black', x - .080, 3.40, z - .080, x + .080, 3.49, z + .080)
            for y in (1.22, 1.92, 2.62, 3.30):
                bolt(m, (x + side * .069, y, z), (side, 0, 0), .019)
            m.tube('rubber', [(x + .10, .8, z), (x + .12, 1.15, z), (x + .12, 3.35, z), (x + .05, 3.5, z)], .012, segs=6)
            # Gas strut holding the opened roof wing, with a separate exposed piston rod.
            m.part = STRUT
            a, b = (x, 4.88, z), (side * 2.72, roof_y(side * 2.72) - .19, z)
            mid = tuple(a[i] * .5 + b[i] * .5 for i in range(3))
            bar(m, 'black', a, mid, .036, 8)
            bar(m, 'chrome', mid, b, .019, 8)


def deck(m):
    # Individually edged plywood panels. The gaps expose metal, not a continuous black cuboid.
    for xa, xb, part in ((-AX, -1.25, WING_NEG), (-1.25, 1.25, BODY), (1.25, AX, WING_POS)):
        m.part = part
        for j in range(8):
            za, zb = -AZ + j * 1.025, -AZ + (j + 1) * 1.025
            m.box('metal', xa + .009, .868, za + .009, xb - .009, .966, zb - .009)
            m.box('stage_deck', xa + .025, .967, za + .025, xb - .025, 1.0, zb - .025)
            for x in (xa + .07, xb - .07):
                for z in (za + .07, zb - .07):
                    m.cylinder('black', .013, 1.001, 1.006, cx=x, cz=z, segs=6)
    for x, part in ((-AX, WING_NEG), (AX, WING_POS)):
        m.part = part
        m.box('metal', x - .016, .874, -AZ, x + .016, 1.006, AZ)
    for z in (-AZ, AZ):
        for xa, xb, part in ((-AX, -1.25, WING_NEG), (-1.25, 1.25, BODY), (1.25, AX, WING_POS)):
            m.part = part
            m.box('metal', xa, .874, z - .016, xb, 1.006, z + .016)
    # Hinge barrels between central deck and each unfolded wing, with end pins.
    m.part = BODY
    for x in (-1.25, 1.25):
        for j in range(12):
            z = -3.85 + j * .70
            bar(m, 'metal', (x, .92, z), (x, .92, z + .28), .026, 10)
            bolt(m, (x, .92, z + .286), (0, 0, 1), .034)


def truss(m, a, b, width=.28):
    t = _norm(_sub(b, a))
    u = _norm(_cross(t, (0, 1, 0) if abs(t[1]) < .9 else (1, 0, 0)))
    v = _norm(_cross(t, u))
    offsets = [tuple(width * .5 * (u[k] * i + v[k] * j) for k in range(3))
               for i, j in ((1, 1), (-1, 1), (-1, -1), (1, -1))]
    def point(offset, q):
        return tuple(a[k] + (b[k] - a[k]) * q + offset[k] for k in range(3))
    for off in offsets:
        bar(m, 'metal', point(off, 0), point(off, 1), .025, 8)
    count = max(1, round(math.dist(a, b) / .52))
    for i in range(count):
        qa, qb = i / count, (i + 1) / count
        for side in range(4):
            p, q = offsets[side], offsets[(side + 1) % 4]
            bar(m, 'metal', point(p, qa), point(q, qb), .011, 6)
            bar(m, 'metal', point(p, qa), point(q, qa), .012, 6)
    # Short coupling sleeves and locking pins distinguish the assembled sections.
    for q in (0, .25, .5, .75, 1):
        for off in offsets:
            p = point(off, q)
            p0 = tuple(p[k] - t[k] * .028 for k in range(3))
            p1 = tuple(p[k] + t[k] * .028 for k in range(3))
            bar(m, 'chrome', p0, p1, .032, 8)


def frame(m):
    # Two facade towers and a proper three-dimensional four-chord header.
    m.part = FACADE
    for z in (-3.94, 3.94):
        truss(m, (3.10, 1.02, z), (3.10, 5.34, z), .24)
        m.box('metal', 2.87, 1.01, z - .20, 3.32, 1.045, z + .20)
        for x in (2.91, 3.28):
            for zz in (z - .15, z + .15):
                m.cylinder('chrome', .02, 1.046, 1.06, cx=x, cz=zz, segs=6)
    truss(m, (3.10, 5.34, -3.94), (3.10, 5.34, 3.94), .28)
    # Central roof cassette and exposed cross ribs of both awnings.
    for x in (-3.10, -1.22, 1.22, 3.10):
        m.part = roof_part(x)
        bar(m, 'metal', (x, roof_y(x) - .12, -4.04), (x, roof_y(x) - .12, 4.04), .038, 4)
    for i in range(11):
        z = -4.05 + i * .81
        for xa, xb in ((-3.13, -1.22), (-1.22, 1.22), (1.22, 3.13)):
            m.part = roof_part((xa + xb) / 2)
            bar(m, 'metal', (xa, roof_y(xa) - .10, z), (xb, roof_y(xb) - .10, z), .034, 4)
        m.part = ROOF
        for x in (-1.22, 1.22):
            bar(m, 'chrome', (x, 5.45, z - .11), (x, 5.45, z + .11), .034, 10)
    # Four bare projector bars, with saddle clamps; deliberately no fixtures.
    for x in (-2.2, -.70, .70, 2.2):
        m.part = roof_part(x)
        y = roof_y(x) - .25
        bar(m, 'galvanised', (x, y, -3.85), (x, y, 3.85), .024, 8)
        for z in (-3.6, -.8, .8, 3.6):
            m.box('black', x - .034, y - .036, z - .025, x + .034, y + .055, z + .025)
            bolt(m, (x + .04, y + .025, z), radius=.011)
    # Rear safety rails, separated at the access gate. They ride up with the back wing.
    m.part = WING_NEG
    for za, zb in ((-3.90, 2.32), (3.58, 3.90)):
        for y in (1.50, 2.03):
            bar(m, 'galvanised', (-3.05, y, za), (-3.05, y, zb), .019, 8)
        for z in (za, (za + zb) / 2, zb):
            bar(m, 'metal', (-3.05, 1.02, z), (-3.05, 2.03, z), .022, 8)


def roof(m):
    # A taut PVC envelope with visible pitched wings; no single flat slab.
    m.part = ROOF
    xs = [-3.48 + i * 6.98 / 18 for i in range(19)]
    zs = [-4.23 + i * 8.46 / 20 for i in range(21)]
    for xa, xb in zip(xs, xs[1:]):
        for za, zb in zip(zs, zs[1:]):
            pts = [(xa, roof_y(xa, za), za), (xb, roof_y(xb, za), za),
                   (xb, roof_y(xb, zb), zb), (xa, roof_y(xa, zb), zb)]
            m.part = roof_part((xa + xb) / 2)
            face(m, 'stage_canvas', pts, double=True,
                 uv=((xa / 2, za / 2), (xb / 2, za / 2), (xb / 2, zb / 2), (xa / 2, zb / 2)))
    # Front and rear scalloped valances, plus the side returns.
    for x in (-3.48, 3.50):
        m.part = roof_part(x)
        for i in range(64):
            za, zb = -4.23 + 8.46 * i / 64, -4.23 + 8.46 * (i + 1) / 64
            ya, yb = roof_y(x, za), roof_y(x, zb)
            da, db = .15 + .035 * math.sin(i * math.pi / 2) ** 2, .15 + .035 * math.sin((i + 1) * math.pi / 2) ** 2
            face(m, 'stage_canvas', [(x, ya, za), (x, yb, zb), (x, yb - db, zb), (x, ya - da, za)],
                 (1 if x > 0 else -1, 0, 0), double=True)
        bar(m, 'stage_canvas', (x, roof_y(x) - .17, -4.23), (x, roof_y(x) - .17, 4.23), .008, 6)
    for z in (-4.23, 4.23):
        for i in range(48):
            xa, xb = -3.48 + 6.98 * i / 48, -3.48 + 6.98 * (i + 1) / 48
            m.part = roof_part((xa + xb) / 2)
            ya, yb = roof_y(xa, z), roof_y(xb, z)
            da, db = .16 + .04 * math.sin(i * math.pi / 2) ** 2, .16 + .04 * math.sin((i + 1) * math.pi / 2) ** 2
            face(m, 'stage_canvas', [(xa, ya, z), (xb, yb, z), (xb, yb - db, z), (xa, ya - da, z)],
                 (0, 0, 1 if z > 0 else -1), double=True)


def drape(m, start, end, top, outward, apron=False):
    """Curtain strips with vertical pleats, broad tension wrinkles and a weighted lower hem."""
    length = math.dist(start, end)
    cols, rows = max(4, round(length / .06)), 5 if not apron else 3
    dx, dz = (end[0] - start[0]) / length, (end[1] - start[1]) / length
    nx, nz = dz, -dx
    mat = 'stage_canvas' if apron else 'stage_drape'
    bottom = .09 if apron else 1.035
    def pt(i, j):
        u, v = i / cols, j / rows
        y = bottom + (top(u) - bottom) * v
        fold = (.022 if apron else .035) * math.sin(u * length * math.tau / .44)
        wrinkle = .016 * math.sin(u * length * 9 + v * 7) * math.sin(math.pi * v)
        off = fold * (.55 + .45 * (1 - v)) + wrinkle
        return start[0] + dx * length * u + nx * off, y, start[1] + dz * length * u + nz * off
    def normal(i, j):
        a = _sub(pt(i + .01, j), pt(i - .01, j))
        b = _sub(pt(i, j + .01), pt(i, j - .01))
        n = _norm(_cross(a, b))
        return n if sum(n[k] * outward[k] for k in range(3)) > 0 else tuple(-v for v in n)
    for i in range(cols):
        for j in range(rows):
            corners = [pt(i, j), pt(i + 1, j), pt(i + 1, j + 1), pt(i, j + 1)]
            normals = [normal(i, j), normal(i + 1, j), normal(i + 1, j + 1), normal(i, j + 1)]
            uv = [(i / 16, j / 3), ((i + 1) / 16, j / 3), ((i + 1) / 16, (j + 1) / 3), (i / 16, (j + 1) / 3)]
            gn = _cross(_sub(corners[1], corners[0]), _sub(corners[2], corners[0]))
            if sum(gn[k] * outward[k] for k in range(3)) < 0:
                corners.reverse(); normals.reverse(); uv.reverse()
            # White PVC on the exterior, black lining just inside so the two cloths do not z-fight.
            m.quad('stage_canvas', *corners, normals=normals, uv=uv)
            gap = 0.012
            lining = [tuple(c[k] - outward[k] * gap for k in range(3)) for c in corners]
            m.quad('stage_canvas' if apron else 'stage_drape', *lining[::-1],
                   normals=[tuple(-v for v in n) for n in normals[::-1]], uv=uv[::-1])
    # Sewn hem, regular eyelets and ties along the top rail.
    for i in range(0, cols + 1, max(1, round(cols / (length / .55)))):
        x, y, z = pt(i, rows)
        bar(m, 'metal', (x, y - .045, z), (x + nx * .008, y - .045, z + nz * .008), .014, 8)
        if not apron:
            m.tube('black', [(x, y - .04, z), (x - nx * .022, y + .035, z - nz * .022),
                             (x - nx * .04, y - .03, z - nz * .04)], .006, segs=4)
    # Short individual bottom hem segments follow the folds rather than crossing through the cloth.
    for i in range(cols):
        bar(m, mat, pt(i, 0), pt(i + 1, 0), .007, 4)


def curtains(m):
    m.part = CURTAIN
    for z in (-4.025, 4.025):
        drape(m, (-3.17, z), (2.98, z), lambda u: roof_y(-3.17 + u * 6.15, z) - .20,
              (0, 0, math.copysign(1, z)))
    # Back drape split around a usable backstage entrance.
    for za, zb in ((-4.00, 2.3), (3.62, 4.00)):
        drape(m, (-3.17, za), (-3.17, zb), lambda u: 5.19, (-1, 0, 0))
    # Lightly gathered front skirt, hanging below the metal deck nosing.
    drape(m, (3.32, -4.045), (3.32, 4.045), lambda u: .89, (1, 0, 0), apron=True)
    # Tie-back bundles along both sides of the rear access opening.
    for z in (2.32, 3.60):
        for j in range(4):
            x = -3.17 + (j - 1.5) * .023
            bar(m, 'stage_drape', (x, 1.05, z), (x, 5.18, z), .016, 6)
        m.box('black', -3.23, 2.20, z - .065, -3.08, 2.24, z + .065)


def stairs(m):
    m.part = STAIRS
    z = 3.0
    # Four rises of .4 block after export scale: walkable without jumping.
    for xa, xb, y in ((-5.65, -5.15, .20), (-5.15, -4.65, .40),
                      (-4.65, -4.15, .60), (-4.15, -3.35, .80)):
        m.box('metal', xa, y - .065, z - .48, xb, y - .018, z + .48)
        m.box('tread', xa + .016, y - .018, z - .46, xb - .016, y, z + .46)
        m.box('black', xa - .008, y - .052, z - .48, xa + .024, y + .007, z + .48)
        for x in (xa + .065, xb - .065):
            for zz in (z - .40, z + .40):
                m.cylinder('chrome', .012, y + .001, y + .009, cx=x, cz=zz, segs=6)
    for zz in (z - .46, z + .46):
        bar(m, 'galvanised', (-5.63, .08, zz), (-3.23, .88, zz), .032, 4)
        bar(m, 'metal', (-5.57, .20, zz), (-5.57, 1.25, zz), .02, 8)
        bar(m, 'metal', (-3.40, .78, zz), (-3.40, 1.98, zz), .02, 8)
        m.tube('metal', [(-5.7, 1.21, zz), (-5.57, 1.25, zz), (-3.40, 1.98, zz), (-3.17, 1.98, zz)], .023, segs=8)
        m.box('black', -5.67, .03, zz - .08, -5.49, .08, zz + .08)


def build():
    m = Mesh()
    chassis(m); drawbar(m); hydraulics(m); deck(m); frame(m); roof(m); curtains(m); stairs(m)
    return m.transformed(lambda p: tuple(value * SCALE for value in p))


def textures():
    folder = os.path.join(ASSETS, 'textures', 'block', 'props')
    for name, base in (('stage_canvas', 230), ('stage_drape', 219), ('stage_deck', 35)):
        im = Image.new('RGBA', (64, 64)); px = im.load(); rnd = random.Random(4200)
        for y in range(64):
            for x in range(64):
                weave = (2 if (x + y) % 2 else -2) + rnd.uniform(-2, 2)
                seam = -11 if name == 'stage_canvas' and x in (0, 1) else 0
                stitch = 7 if name == 'stage_canvas' and x == 3 and y % 4 < 2 else 0
                grain = rnd.uniform(-5, 5) if name == 'stage_deck' else 0
                v = int(max(0, min(255, base + weave + seam + stitch + grain)))
                px[x, y] = (v, v, v, 255)
        im.save(os.path.join(folder, name + '.png'))
    im = Image.new('RGBA', (64, 32), (209, 213, 207, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(im)
    d.rectangle((2, 2, 61, 29), outline=(70, 77, 78), width=2)
    d.text((9, 10), 'ST 4200', fill=(35, 41, 42))
    im.save(os.path.join(folder, 'stage_label.png'))


def export(m):
    out = os.path.join(ASSETS, 'models', 'structure')
    os.makedirs(out, exist_ok=True)
    triangles = []
    bounds = [[math.inf] * 3, [-math.inf] * 3]
    for mat, tris in m.groups.items():
        assert mat in MATERIALS, mat
        ids = m.part_ids.get(mat, [])
        assert len(ids) == len(tris), (mat, len(ids), len(tris))
        for part, tri in zip(ids, tris):
            # Fix winding to match vertex normals, as in the furniture exporter.
            gn = _cross(_sub(tri[1][0], tri[0][0]), _sub(tri[2][0], tri[0][0]))
            assert sum(n * n for n in gn) > 1e-15, (mat, tri)
            avg = tuple(sum(v[1][k] for v in tri) for k in range(3))
            if sum(gn[k] * avg[k] for k in range(3)) < 0:
                tri = (tri[0], tri[2], tri[1])
            for p, n, uv in tri:
                assert all(math.isfinite(v) for v in (*p, *n, *uv))
                for k in range(3):
                    bounds[0][k] = min(bounds[0][k], p[k]); bounds[1][k] = max(bounds[1][k], p[k])
            triangles.append((MATERIALS.index(mat), part, tri))
    assert len(triangles) < 60000, len(triangles)
    with open(os.path.join(out, 'opus_4200.mesh'), 'wb') as f:
        f.write(struct.pack('>II', 0x53544733, len(triangles)))
        for mat, part, tri in triangles:
            f.write(struct.pack('>BB', mat, part))
            # Degenerate fourth corner lets the existing quad pipeline render a triangle without allocation per frame.
            for p, n, uv in (*tri, tri[-1]):
                f.write(struct.pack('>8f', *p, *uv, *n))
    preview = os.path.join(ROOT, 'build', 'mobile-stage')
    os.makedirs(preview, exist_ok=True)
    _write_obj(m, os.path.join(preview, 'opus_4200.obj'), 'opus_4200')
    counts = {}
    for _mat, part, _tri in triangles:
        counts[part] = counts.get(part, 0) + 1
    report = {'triangles': len(triangles), 'bounds': bounds,
              'materials': {mat: len(tris) for mat, tris in m.groups.items()},
              'parts': counts, 'fixtures': 0}
    with open(os.path.join(preview, 'report.json'), 'w') as f:
        json.dump(report, f, indent=2)
    print(json.dumps(report))


if __name__ == '__main__':
    textures()
    export(build())
