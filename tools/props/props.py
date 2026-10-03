"""Prop models. Every builder returns a Mesh in block units, facing north (front towards -z)."""
import math

from mesh import Mesh, TAU, rot_y, rot_x, rot_z, translate, chain

PX = 1 / 16


# ---------------------------------------------------------------------------------------------------------------- tables

def round_table():
    """Banquet table with a cloth falling to the floor in soft folds."""
    m = Mesh()
    prof = [(0.505, 0.0), (0.502, 0.025), (0.497, 0.2), (0.492, 0.45), (0.489, 0.66), (0.487, 0.71),
            (0.481, 0.738), (0.468, 0.752)]
    amp = [0.034, 0.033, 0.028, 0.017, 0.006, 0.002, 0.0, 0.0]
    m.lathe("cloth", prof, segs=56, radius_fn=lambda a, i: 1 + amp[i] * math.sin(a * 13 + 0.6 * math.sin(a * 3)))
    m.disc("cloth", 0.468, 0.752, segs=56)
    return m


def _stretch_cover(m, mat):
    """Stretch cover of a cocktail table: hugs the foot, pinched at the waist, tight over the top."""
    prof = [(0.300, 0.015), (0.300, 0.045), (0.255, 0.12), (0.17, 0.30), (0.105, 0.48), (0.088, 0.56),
            (0.10, 0.64), (0.16, 0.80), (0.27, 0.95), (0.37, 1.04), (0.405, 1.075), (0.405, 1.090),
            (0.395, 1.100)]
    # Faint vertical tension lines in the fabric.
    m.lathe(mat, prof, segs=48, radius_fn=lambda a, i: 1 + 0.012 * math.sin(a * 8) * math.sin(math.pi * i / (len(prof) - 1)))
    m.disc(mat, 0.395, 1.100, segs=48)
    # Bow tied at the front of the waist: a knot, two flat loops against the cover and two tails.
    m.lathe(mat, [(0.094, 0.535), (0.103, 0.545), (0.103, 0.575), (0.094, 0.585)], segs=32)
    zf = 0.5 - 0.105
    m.sphere(mat, 0.022, 0.5, 0.56, zf, segs=10, rings=6, sy=0.8)
    for side in (1, -1):
        loop = []
        for k in range(17):
            t = k / 16 * TAU
            x = 0.5 + side * (0.018 + 0.05 * (1 - math.cos(t)) / 2)
            loop.append((x, 0.56 + 0.026 * math.sin(t), zf + 0.004 + 0.006 * (1 - math.cos(t))))
        m.tube(mat, loop, 0.008, segs=6, caps=False)
        m.tube(mat, [(0.5 + side * 0.01, 0.55, zf), (0.5 + side * 0.035, 0.49, zf + 0.004), (0.5 + side * 0.045, 0.44, zf + 0.012)], 0.0085, segs=6)


def standing_table(covered=True):
    """Cocktail table (1.10 m): chrome column on a weighted disc, round top; optionally under a stretch cover."""
    m = Mesh()
    if covered:
        _stretch_cover(m, "cloth")
        return m
    # Base: weighted disc with a bevelled edge and rubber ring.
    m.lathe("chrome", [(0.30, 0.004), (0.30, 0.018), (0.29, 0.028), (0.06, 0.04)], segs=40)
    m.lathe("rubber", [(0.302, 0.0), (0.302, 0.006)], segs=40)
    m.disc("rubber", 0.302, 0.0, segs=40, up=False)
    m.cylinder("chrome", 0.032, 0.04, 1.03, segs=20)
    # Top bracket and top with a chrome edge band.
    m.cylinder("chrome", 0.12, 1.03, 1.045, segs=24)
    m.lathe("white", [(0.40, 1.045), (0.40, 1.07)], segs=48)
    m.disc("white", 0.40, 1.07, segs=48)
    m.disc("white", 0.40, 1.045, segs=48, up=False)
    m.lathe("chrome", [(0.403, 1.044), (0.403, 1.071)], segs=48)
    return m


# ---------------------------------------------------------------------------------------------------------------- seats

def _leg(m, mat, x, z, y0, y1, r=0.018, lean=(0, 0)):
    m.tube(mat, [(x, y0, z), (x + lean[0], y1, z + lean[1])], r, segs=8)


def banquet_chair():
    """Chiavari chair: turned legs, double stretchers, spindle back, dyed seat cushion."""
    m = Mesh()
    F = "white"
    x0, x1, zf, zb = 0.30, 0.70, 0.30, 0.68
    seat = 0.44
    # Front legs (slight taper to the floor), back legs continue into the back posts with a backwards rake.
    for x in (x0, x1):
        m.lathe(F, [(0.014, 0.0), (0.017, 0.10), (0.019, seat)], cx=x, cz=zf, segs=10)
        m.tube(F, [(x, 0.0, zb + 0.015), (x, seat, zb), (x, 0.70, zb + 0.03), (x, 0.92, zb + 0.06)], 0.017, segs=10)
        m.sphere("gold", 0.02, x, 0.935, zb + 0.062, segs=10, rings=6)
        m.cylinder("rubber", 0.016, 0.0, 0.008, cx=x, cz=zf, segs=10)
    # Seat frame.
    for (a, b) in (((x0, seat, zf), (x1, seat, zf)), ((x0, seat, zb), (x1, seat, zb)), ((x0, seat, zf), (x0, seat, zb)), ((x1, seat, zf), (x1, seat, zb))):
        m.tube(F, [a, b], 0.016, segs=8)
    # Stretchers: a low front rail and doubled side rails, the hallmark of a chiavari.
    for y in (0.13, 0.25):
        for x in (x0, x1):
            m.tube(F, [(x, y, zf), (x, y, zb + 0.008)], 0.009, segs=6)
    m.tube(F, [(x0, 0.16, zf), (x1, 0.16, zf)], 0.009, segs=6)
    # Back: three rails and a row of spindles.
    for y, z in ((0.58, zb + 0.022), (0.75, zb + 0.035), (0.90, zb + 0.058)):
        m.tube(F, [(x0, y, z), (x1, y, z)], 0.012, segs=8)
    for k in range(1, 6):
        x = x0 + (x1 - x0) * k / 6
        m.tube(F, [(x, 0.58, zb + 0.022), (x, 0.75, zb + 0.035)], 0.006, segs=6)
    # Seat cushion: rounded edges, piped seam.
    _cushion(m, "cloth", x0 + 0.005, x1 - 0.005, zf + 0.005, zb - 0.005, seat + 0.012, 0.045)
    return m


def _cushion(m, mat, x0, x1, z0, z1, y, t):
    """Rounded square cushion (superellipse lathe squashed into a rectangle)."""
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    hx, hz = (x1 - x0) / 2, (z1 - z0) / 2
    segs = 32
    prof = [(0.82, y), (0.97, y + t * 0.25), (1.0, y + t * 0.55), (0.96, y + t * 0.85), (0.80, y + t)]
    rings = []
    for (s, yy) in prof:
        ring = []
        for k in range(segs + 1):
            a = TAU * k / segs
            c, sn = math.cos(a), math.sin(a)
            # Superellipse radius so the outline is a rounded square.
            e = 4
            r = 1 / (abs(c) ** e + abs(sn) ** e) ** (1 / e)
            ring.append((cx + c * r * hx * s, yy, cz + sn * r * hz * s))
        rings.append(ring)
    for i in range(len(rings) - 1):
        for k in range(segs):
            a, b, c2, d = rings[i][k], rings[i][k + 1], rings[i + 1][k + 1], rings[i + 1][k]
            m.quad(mat, a, d, c2, b)
    # Top and bottom caps (fans).
    for ring, up in ((rings[-1], True), (rings[0], False)):
        cen = (cx, ring[0][1], cz)
        for k in range(segs):
            p, q = ring[k], ring[k + 1]
            n = (0, 1, 0) if up else (0, -1, 0)
            if up:
                m.tri(mat, cen, q, p, n, n, n, (0.5, 0.5), (q[0], q[2]), (p[0], p[2]))
            else:
                m.tri(mat, cen, p, q, n, n, n, (0.5, 0.5), (p[0], p[2]), (q[0], q[2]))


def folding_chair():
    """Steel folding chair: X side frames, dyed plastic seat and curved back."""
    m = Mesh()
    F = "black"
    x0, x1 = 0.29, 0.71
    for x in (x0, x1):
        # Front leg runs up into the back post; the rear leg crosses it under the seat.
        m.tube(F, [(x, 0.0, 0.33), (x, 0.44, 0.40), (x, 0.82, 0.70)], 0.016, segs=8)
        m.tube(F, [(x, 0.0, 0.72), (x, 0.43, 0.42)], 0.015, segs=8)
        m.cylinder("rubber", 0.018, 0.0, 0.012, cx=x, cz=0.33, segs=8)
        m.cylinder("rubber", 0.018, 0.0, 0.012, cx=x, cz=0.72, segs=8)
        m.sphere("chrome", 0.012, x, 0.44, 0.40, segs=8, rings=4)
    for (y, z) in ((0.06, 0.34), (0.06, 0.715), (0.43, 0.42)):
        m.tube(F, [(x0, y, z), (x1, y, z)], 0.011, segs=6)
    # Seat pan, slightly dished.
    seat = Mesh()
    _cushion(seat, "seat", x0 + 0.01, x1 - 0.01, 0.33, 0.72, 0.445, 0.03)
    m.merge(seat)
    # Curved backrest panel between the back posts.
    pts = []
    for k in range(9):
        t = k / 8
        x = x0 + 0.015 + (x1 - x0 - 0.03) * t
        bow = 0.02 * math.sin(math.pi * t)
        pts.append((x, bow))
    for (ya, yb, za, zb) in ((0.66, 0.80, 0.585, 0.675),):
        for k in range(8):
            (xa, ba), (xb, bb) = pts[k], pts[k + 1]
            m.quad("seat", (xa, ya, za + ba), (xb, ya, za + bb), (xb, yb, zb + bb), (xa, yb, zb + ba))
            m.quad("seat", (xb, ya, za + bb + 0.02), (xa, ya, za + ba + 0.02), (xa, yb, zb + ba + 0.02), (xb, yb, zb + bb + 0.02))
    return m


def bar_stool():
    """High bar stool: chrome base and column, foot ring, dyed round cushion."""
    m = Mesh()
    m.lathe("chrome", [(0.20, 0.004), (0.20, 0.016), (0.17, 0.03), (0.05, 0.05)], segs=36)
    m.lathe("rubber", [(0.202, 0.0), (0.202, 0.006)], segs=36)
    m.disc("rubber", 0.202, 0.0, segs=36, up=False)
    m.cylinder("chrome", 0.028, 0.05, 0.70, segs=16)
    # Foot ring on three spokes.
    ring = [(0.5 + 0.16 * math.cos(TAU * k / 32), 0.30, 0.5 + 0.16 * math.sin(TAU * k / 32)) for k in range(33)]
    m.tube("chrome", ring, 0.011, segs=8, caps=False)
    for k in range(3):
        a = TAU * k / 3 + 0.5
        m.tube("chrome", [(0.5, 0.30, 0.5), (0.5 + 0.16 * math.cos(a), 0.30, 0.5 + 0.16 * math.sin(a))], 0.008, segs=6)
    m.cylinder("black", 0.10, 0.69, 0.71, segs=24)
    # Cushion with a rolled edge.
    m.lathe("cloth", [(0.15, 0.705), (0.185, 0.715), (0.195, 0.74), (0.19, 0.765), (0.17, 0.778), (0.0, 0.782)], segs=40)
    m.disc("cloth", 0.15, 0.705, segs=40, up=False)
    return m


# ---------------------------------------------------------------------------------------------------------------- bar

def bar_counter_base():
    """One metre of bar: dyed front panel in a black frame, dark wood top with a lip, warm LED strip, chrome foot rail."""
    m = Mesh()
    # Carcass and kick plate.
    m.box("black", 0.0, 0.0, 0.05, 1.0, 0.07, 0.64)
    m.box("black", 0.0, 0.07, 0.03, 1.0, 1.02, 0.07)
    # Recessed dyed panel in a frame.
    m.box("cloth", 0.06, 0.13, 0.015, 0.94, 0.95, 0.03, faces="n")
    for (xa, xb, ya, yb) in ((0.0, 0.06, 0.07, 1.02), (0.94, 1.0, 0.07, 1.02), (0.0, 1.0, 0.07, 0.13), (0.0, 1.0, 0.95, 1.02)):
        m.box("black", xa, ya, 0.01, xb, yb, 0.03, faces="nud")
    # Back: shelf for the bartender and an under-counter shelf.
    m.box("black", 0.0, 0.07, 0.60, 1.0, 1.02, 0.64, faces="s")
    m.bevel_box("white", 0.0, 0.70, 0.46, 1.0, 0.73, 0.64, 0.004)
    m.bevel_box("white", 0.0, 0.30, 0.30, 1.0, 0.33, 0.64, 0.004)
    # Top with an overhang towards the guests.
    m.bevel_box("wood", 0.0, 1.02, -0.10, 1.0, 1.075, 0.66, 0.008)
    # LED strip under the lip.
    m.box("bulb", 0.0, 1.005, -0.075, 1.0, 1.02, -0.06, faces="dn")
    # Foot rail on two brackets.
    m.tube("chrome", [(0.0, 0.17, -0.10), (1.0, 0.17, -0.10)], 0.022, segs=10, caps=False)
    for x in (0.25, 0.75):
        m.tube("chrome", [(x, 0.17, -0.10), (x, 0.17, 0.01)], 0.012, segs=6)
        m.cylinder("chrome", 0.03, 0.15, 0.19, cx=x, cz=0.012, segs=10)
    return m


def bar_counter_end(side):
    """Black side panel closing the end of a bar run (side -1 = left, 1 = right)."""
    m = Mesh()
    x0 = 0.0 if side < 0 else 0.97
    m.box("black", x0, 0.0, 0.01, x0 + 0.03, 1.02, 0.64)
    return m


# ---------------------------------------------------------------------------------------------------------------- bleachers

def _bucket_seat(m, cx):
    """Moulded stadium seat (dyed plastic) centred on x = cx, sitting on the riser."""
    w = 0.74
    # Side profile of the shell in the y/z plane: seat pan curving up into the backrest.
    prof = [(0.54, 0.50), (0.60, 0.485), (0.70, 0.48), (0.80, 0.49), (0.86, 0.52), (0.90, 0.60),
            (0.92, 0.70), (0.93, 0.80)]
    n = 10
    for i in range(len(prof) - 1):
        (za, ya), (zb, yb) = prof[i], prof[i + 1]
        for k in range(n):
            ua, ub = k / n, (k + 1) / n
            xa = cx - w / 2 + w * ua
            xb = cx - w / 2 + w * ub
            # Shell is dished across: edges sit a little higher than the middle.
            da = 0.018 * (1 - math.sin(math.pi * ua)) * (1 if i < 4 else 0.3)
            db = 0.018 * (1 - math.sin(math.pi * ub)) * (1 if i < 4 else 0.3)
            m.quad("seat", (xa, ya + da, za), (xb, ya + db, za), (xb, yb + db, zb), (xa, yb + da, zb))
            # Underside, slightly offset.
            m.quad("seat", (xb, ya + db - 0.012, za), (xa, ya + da - 0.012, za), (xa, yb + da - 0.012, zb), (xb, yb + db - 0.012, zb))
    # Rolled front lip and the edges of the shell.
    m.tube("seat", [(cx - w / 2, 0.494, 0.54), (cx + w / 2, 0.494, 0.54)], 0.008, segs=6)
    for x in (cx - w / 2, cx + w / 2):
        m.tube("seat", [(x, p[1] + 0.012, p[0]) for p in prof], 0.007, segs=5)
    # Mounting bracket.
    for dx in (-0.22, 0.22):
        m.box("metal", cx + dx - 0.03, 0.445, 0.62, cx + dx + 0.03, 0.462, 0.80)


def bleacher_base():
    """Grandstand row (1 block wide): treaded walkway at the front, riser and two moulded seats at the back,
    aluminium frame on the left side (the right side frame is added on the last module)."""
    m = Mesh()
    # Walkway: three anti-slip boards with gaps, yellow nosing on the front edge.
    for k in range(3):
        z0 = 0.02 + k * 0.165
        m.bevel_box("wood", 0.0, 0.035, z0, 1.0, 0.07, z0 + 0.15, 0.004)
    m.box("yellow", 0.0, 0.04, 0.0, 1.0, 0.072, 0.025)
    # Riser and seat deck.
    m.box("metal", 0.0, 0.0, 0.50, 1.0, 0.44, 0.52)
    m.bevel_box("metal", 0.0, 0.42, 0.50, 1.0, 0.445, 1.0, 0.003)
    _bucket_seat(m, 0.5)
    # Seat numbers would go here; a small reflector stud instead.
    m.box("yellow", 0.48, 0.36, 0.495, 0.52, 0.40, 0.50, faces="n")
    _bleacher_frame(m, 0.02)
    return m


def _bleacher_frame(m, x):
    """Aluminium side frame: a stringer under the walkway, a post, a diagonal brace and the joist under the seats."""
    F = "metal"
    m.box(F, x - 0.02, 0.0, 0.0, x + 0.02, 0.035, 0.52)
    m.box(F, x - 0.02, 0.0, 0.48, x + 0.02, 0.42, 0.52)
    m.box(F, x - 0.02, 0.38, 0.52, x + 0.02, 0.42, 1.0)
    m.tube(F, [(x, 0.03, 0.06), (x, 0.38, 0.48)], 0.012, segs=6)
    m.tube(F, [(x, 0.40, 0.96), (x, 0.04, 0.96)], 0.016, segs=8)
    m.cylinder("rubber", 0.03, 0.0, 0.01, cx=x, cz=0.96, segs=8)


def _guard_rail(m, x):
    for z in (0.10, 0.90):
        m.tube("galvanised", [(x, 0.07, z), (x, 1.05, z)], 0.018, segs=8)
    m.tube("galvanised", [(x, 1.05, 0.10), (x, 1.05, 0.90)], 0.02, segs=8, caps=False)
    m.tube("galvanised", [(x, 0.60, 0.10), (x, 0.60, 0.90)], 0.013, segs=6, caps=False)


def bleacher_end_left():
    m = Mesh()
    _guard_rail(m, 0.015)
    return m


def bleacher_aisle():
    """Aisle of a grandstand: two steps per row with yellow nosings, centre handrail climbing to the next row."""
    m = Mesh()
    for k in range(3):
        z0 = 0.02 + k * 0.165
        m.bevel_box("wood", 0.0, 0.035, z0, 1.0, 0.07, z0 + 0.15, 0.004)
    m.box("yellow", 0.0, 0.04, 0.0, 1.0, 0.072, 0.025)
    # Half-height step on the back half.
    m.box("metal", 0.0, 0.0, 0.50, 1.0, 0.47, 0.52)
    for k in range(3):
        z0 = 0.52 + k * 0.16
        m.bevel_box("wood", 0.0, 0.47, z0, 1.0, 0.50, z0 + 0.145, 0.004)
    m.box("yellow", 0.0, 0.475, 0.50, 1.0, 0.503, 0.525)
    m.box("metal", 0.0, 0.0, 0.98, 1.0, 0.47, 1.0, faces="s")
    _bleacher_frame(m, 0.02)
    # Centre handrail on two posts, sloping like the steps.
    for (z, y) in ((0.12, 0.07), (0.62, 0.50)):
        m.tube("galvanised", [(0.5, y, z), (0.5, y + 0.95, z)], 0.016, segs=8)
        m.cylinder("galvanised", 0.035, y, y + 0.01, cx=0.5, cz=z, segs=10)
    m.tube("galvanised", [(0.5, 1.02, 0.0), (0.5, 1.45, 0.75), (0.5, 1.53, 1.0)], 0.02, segs=8, caps=False)
    return m


def bleacher_support():
    """Grandstand substructure: aluminium posts, ledgers and diagonal braces under the raised rows."""
    m = Mesh()
    F = "metal"
    for x in (0.03, 0.97):
        for z in (0.03, 0.97):
            m.tube(F, [(x, 0.0, z), (x, 1.0, z)], 0.024, segs=8, caps=False)
        m.tube(F, [(x, 0.08, 0.03), (x, 0.92, 0.97)], 0.014, segs=6)
    for y in (0.06, 0.96):
        for (a, b) in (((0.03, y, 0.03), (0.97, y, 0.03)), ((0.03, y, 0.97), (0.97, y, 0.97)),
                       ((0.03, y, 0.03), (0.03, y, 0.97)), ((0.97, y, 0.03), (0.97, y, 0.97))):
            m.tube(F, [a, b], 0.016, segs=6, caps=False)
    m.tube(F, [(0.03, 0.10, 0.97), (0.97, 0.90, 0.97)], 0.013, segs=6)
    for x in (0.03, 0.97):
        for z in (0.03, 0.97):
            m.box("black", x - 0.05, 0.0, z - 0.05, x + 0.05, 0.012, z + 0.05)
    return m


def bleacher_end_right():
    m = Mesh()
    _bleacher_frame(m, 0.98)
    _guard_rail(m, 0.985)
    return m


# ---------------------------------------------------------------------------------------------------------------- crowd control

def crowd_barrier():
    """Galvanised steel crowd barrier (Vauban type), three blocks wide (x from -1 to 2): tube frame, vertical bars,
    three flat feet, link hooks on one end and eyes on the other."""
    m = Mesh()
    G = "galvanised"
    zc = 0.5
    x0, x1, yb, yt = -0.97, 1.97, 0.09, 1.10
    frame = [(x0, yb, zc), (x0, yt - 0.04, zc), (x0 + 0.04, yt, zc), (x1 - 0.04, yt, zc), (x1, yt - 0.04, zc), (x1, yb, zc)]
    m.tube(G, frame, 0.021, segs=10)
    m.tube(G, [(x0, yb, zc), (x1, yb, zc)], 0.018, segs=8)
    m.tube(G, [(x0, yb + 0.11, zc), (x1, yb + 0.11, zc)], 0.013, segs=8)
    bars = 30
    for k in range(1, bars):
        x = x0 + (x1 - x0) * k / bars
        m.tube(G, [(x, yb + 0.11, zc), (x, yt, zc)], 0.009, segs=6)
    # Feet: flat bars across, on short legs, with rubber pads.
    for x in (-0.80, 0.50, 1.80):
        m.tube(G, [(x, yb, zc), (x, 0.03, zc)], 0.015, segs=8)
        m.bevel_box(G, x - 0.03, 0.0, zc - 0.32, x + 0.03, 0.03, zc + 0.32, 0.004)
        m.box("rubber", x - 0.031, 0.0, zc - 0.32, x + 0.031, 0.008, zc - 0.27)
        m.box("rubber", x - 0.031, 0.0, zc + 0.27, x + 0.031, 0.008, zc + 0.32)
    # Link hooks on the right end, eyes on the left end.
    for y in (0.32, 0.98):
        m.tube(G, [(x1, y, zc), (x1 + 0.035, y, zc), (x1 + 0.035, y - 0.05, zc)], 0.008, segs=5)
        ring = [(x0 - 0.018 + 0.016 * math.cos(TAU * k / 12), y + 0.016 * math.sin(TAU * k / 12), zc) for k in range(13)]
        m.tube(G, ring, 0.006, segs=5, caps=False)
    return m


def stanchion_base():
    """Queue post: weighted chrome base, column, ball top with a rope hook."""
    m = Mesh()
    m.lathe("chrome", [(0.15, 0.004), (0.15, 0.02), (0.13, 0.04), (0.06, 0.07), (0.035, 0.09)], segs=32)
    m.lathe("rubber", [(0.152, 0.0), (0.152, 0.006)], segs=32)
    m.disc("rubber", 0.152, 0.0, segs=32, up=False)
    m.cylinder("chrome", 0.025, 0.09, 0.90, segs=16, caps=False)
    m.lathe("chrome", [(0.025, 0.90), (0.035, 0.905), (0.04, 0.925)], segs=16)
    m.sphere("chrome", 0.04, 0.5, 0.955, 0.5, segs=16, rings=10)
    for (dx, dz) in ((1, 0), (0, 1), (-1, 0), (0, -1)):
        ring = [(0.5 + dx * 0.045 + dx * 0.012 * math.cos(TAU * k / 10), 0.88 + 0.012 * math.sin(TAU * k / 10), 0.5 + dz * 0.045 + dz * 0.012 * math.cos(TAU * k / 10)) for k in range(11)]
        m.tube("chrome", ring, 0.004, segs=4, caps=False)
    return m


def stanchion_rope(length):
    """Velvet rope hanging east to a post {length} blocks away, with brass end clips."""
    m = Mesh()
    xa, xb = 0.5 + 0.057, 0.5 + length - 0.057
    y = 0.872
    sag = 0.16 * length
    pts = []
    n = 14 * length
    for k in range(n + 1):
        t = k / n
        pts.append((xa + (xb - xa) * t, y - sag * 4 * t * (1 - t), 0.5))
    m.tube("velvet", pts, 0.02, segs=10)
    for x in (xa + 0.012, xb - 0.012):
        m.cylinder("gold", 0.024, y - 0.012, y + 0.012, cx=x, cz=0.5, segs=10)
    return m


# ---------------------------------------------------------------------------------------------------------------- fairground

def _duck(m, x, y, z, scale=1.0, flip=False):
    """Tin duck target: extruded silhouette, orange beak, black eye, on a little stand."""
    s = 0.13 * scale
    poly = [(-0.9, 0.0), (0.7, 0.0), (0.95, 0.25), (0.75, 0.42), (0.35, 0.40), (0.40, 0.62), (0.62, 0.85),
            (0.55, 1.08), (0.25, 1.16), (0.0, 1.0), (-0.05, 0.62), (-0.55, 0.55), (-1.0, 0.75), (-0.95, 0.25)]
    if flip:
        poly = [(-p[0], p[1]) for p in reversed(poly)]
    pts = [(x + p[0] * s, y + p[1] * s) for p in poly]
    m.extrude("yellow", pts, z - 0.012, z + 0.012, side_mat="yellow")
    bx = 0.62 if not flip else -0.62
    beak = [(x + bx * s, y + 0.92 * s), (x + (bx + (0.38 if not flip else -0.38)) * s, y + 0.88 * s), (x + bx * s, y + 0.80 * s)]
    if flip:
        beak = list(reversed(beak))
    m.extrude("orange", beak, z - 0.008, z + 0.008)
    ex = 0.30 if not flip else -0.30
    m.box("black", x + ex * s - 0.008, y + 0.98 * s - 0.008, z - 0.016, x + ex * s + 0.008, y + 0.98 * s + 0.008, z - 0.011)
    m.box("metal", x - 0.012, y - 0.05, z - 0.006, x + 0.012, y, z + 0.006)


def _plush(m, mat, x, y, z, s=1.0):
    """Teddy bear prize: body, head, ears, paws, muzzle, eyes."""
    m.sphere(mat, 0.07 * s, x, y + 0.07 * s, z, segs=10, rings=6, sy=1.15)
    m.sphere(mat, 0.055 * s, x, y + 0.19 * s, z - 0.005, segs=10, rings=6)
    for dx in (-0.042, 0.042):
        m.sphere(mat, 0.022 * s, x + dx * s, y + 0.235 * s, z, segs=5, rings=3)
        m.sphere(mat, 0.025 * s, x + dx * 1.3 * s, y + 0.04 * s, z - 0.03 * s, segs=5, rings=3)
    m.sphere("white", 0.022 * s, x, y + 0.18 * s, z - 0.05 * s, segs=5, rings=3)
    for dx in (-0.02, 0.02):
        m.box("black", x + dx * s - 0.007, y + 0.198 * s, z - 0.058 * s, x + dx * s + 0.007, y + 0.212 * s, z - 0.05 * s)


def _rifle(m, x, z, yaw):
    """Air rifle lying on the counter, chained to it."""
    r = Mesh()
    y = 1.085
    r.extrude("wood_dark", [(-0.25, y - 0.01), (-0.06, y - 0.004), (-0.04, y + 0.03), (-0.25, y + 0.045)], z - 0.012, z + 0.012)
    r.tube("black", [(-0.06, y + 0.02, z), (0.27, y + 0.02, z)], 0.009, segs=8)
    r.box("black", -0.07, y + 0.0, z - 0.013, -0.02, y + 0.032, z + 0.013)
    r.tube("metal", [(-0.20, y + 0.0, z), (-0.21, y - 0.03, z - 0.04), (-0.22, y - 0.06, z - 0.08)], 0.004, segs=4)
    m.merge(r.transformed(chain(rot_y(yaw, 0, z), translate(x, 0, 0))))


def shooting_gallery():
    """Fairground shooting booth, 3 blocks wide (x from -1 to 2) and about 2.6 high.
    Counter with striped front and rifles, gallery of tin ducks on two rails, bullseyes, prize shelves,
    striped canopy with a scalloped valance and a light-bulb sign."""
    m = Mesh()
    X0, X1 = -1.0, 2.0
    # ---- counter: striped front panel, black plinth, wooden top, gold trim
    IX0, IX1 = X0 + 0.06, X1 - 0.06  # inside faces of the side walls: nothing shares a face with them
    m.box("black", IX0, 0.0, 0.025, IX1, 0.10, 0.45, faces="nsu")
    stripes = 12
    for k in range(stripes):
        xa = IX0 + (IX1 - IX0) * k / stripes
        xb = IX0 + (IX1 - IX0) * (k + 1) / stripes
        m.box("red" if k % 2 == 0 else "white", xa, 0.10, 0.005, xb, 0.98, 0.025, faces="n")
    m.box("gold", IX0, 0.95, -0.004, IX1, 0.98, 0.005, faces="nud")
    m.box("gold", IX0, 0.10, -0.004, IX1, 0.13, 0.005, faces="nud")
    m.box("black", IX0, 0.10, 0.025, IX1, 0.98, 0.45, faces="su")
    m.bevel_box("wood", X0 - 0.02, 0.98, -0.10, X1 + 0.02, 1.06, 0.48, 0.012)
    for (x, yaw) in ((-0.45, 8), (0.50, -5), (1.45, 4)):
        _rifle(m, x, 0.18, yaw)
    # ---- side walls and back wall
    for x0 in (X0, X1 - 0.06):
        m.box("red", x0, 0.0, 0.0, x0 + 0.06, 2.20, 1.0)
        m.box("gold", x0 - 0.004, 0.0, -0.004, x0 + 0.064, 2.20, 0.03, faces="nwe")
    m.box("booth_back", X0 + 0.06, 0.0, 0.94, X1 - 0.06, 2.195, 1.0, faces="n")
    m.box("navy", X0 + 0.06, 0.0, 0.94, X1 - 0.06, 2.195, 1.0, faces="s")
    # ---- gallery: two rails of ducks swimming in opposite directions, bullseyes above
    for (y, n, flip, phase) in ((1.30, 8, False, 0.0), (1.66, 7, True, 0.17)):
        m.tube("chrome", [(X0 + 0.08, y, 0.86), (X1 - 0.08, y, 0.86)], 0.012, segs=8)
        m.box("black", X0 + 0.06, y - 0.07, 0.88, X1 - 0.06, y - 0.03, 0.94, faces="nu")
        for k in range(n):
            x = X0 + 0.25 + phase + (X1 - X0 - 0.5) * k / (n - 1 if n > 1 else 1) * 0.96
            _duck(m, x, y + 0.012, 0.86, 1.0, flip)
    # Bullseyes under the lower rail, visible over the counter.
    for k in range(6):
        x = X0 + 0.35 + (X1 - X0 - 0.7) * k / 5
        m.merge(_vertical_disc(x, 1.155, 0.925, 0.075))
    # ---- prize shelves on both side walls
    for (x0, x1, sign) in ((X0 + 0.06, X0 + 0.34, 1), (X1 - 0.34, X1 - 0.06, -1)):
        for y in (1.18, 1.62):
            m.bevel_box("white", x0, y - 0.025, 0.25, x1, y, 0.92, 0.004)
        mats = ("plush_pink", "plush_blue", "plush_mint")
        for row, y in enumerate((1.18, 1.62)):
            for k in range(3):
                _plush(m, mats[(k + row) % 3], (x0 + x1) / 2, y, 0.38 + k * 0.2, 0.9 + 0.1 * ((k + row) % 2))
    # ---- canopy: striped slope from the back wall to the front, scalloped valance
    yb, yf, zf = 2.20, 2.02, -0.30
    strips = 12
    for k in range(strips):
        xa = X0 - 0.02 + (X1 - X0 + 0.04) * k / strips
        xb = X0 - 0.02 + (X1 - X0 + 0.04) * (k + 1) / strips
        mat = "red" if k % 2 == 0 else "white"
        m.quad(mat, (xa, yf, zf), (xb, yf, zf), (xb, yb, 1.0), (xa, yb, 1.0))
        m.quad(mat, (xb, yf - 0.01, zf), (xa, yf - 0.01, zf), (xa, yb - 0.01, 1.0), (xb, yb - 0.01, 1.0))
        # Scallop hanging from the front edge.
        cx = (xa + xb) / 2
        half = (xb - xa) / 2
        pts = [(xa, yf)]
        for j in range(9):
            a = math.pi * j / 8
            pts.append((cx - half * math.cos(a), yf - 0.13 - 0.07 * math.sin(a)))
        pts.append((xb, yf))
        pts_ccw = list(reversed(pts))
        m.extrude("white" if k % 2 == 0 else "red", pts_ccw, zf - 0.006, zf + 0.002)
    # Canopy supports.
    for x in (X0 + 0.03, X1 - 0.03):
        m.tube("gold", [(x, 1.06, 0.0), (x, 2.0, 0.0), (x, yf - 0.02, zf + 0.02)], 0.022, segs=8)
    # ---- sign with a bulb border on top of the canopy
    sx0, sx1, sy0, sy1, sz = -0.25, 1.25, 2.12, 2.62, -0.24
    m.box("booth_sign", sx0, sy0, sz - 0.03, sx1, sy1, sz, faces="n")
    m.box("red", sx0, sy0, sz - 0.03, sx1, sy1, sz, faces="sweud")
    m.box("gold", sx0 - 0.03, sy0 - 0.03, sz - 0.035, sx1 + 0.03, sy0, sz + 0.005)
    m.box("gold", sx0 - 0.03, sy1, sz - 0.035, sx1 + 0.03, sy1 + 0.03, sz + 0.005)
    m.box("gold", sx0 - 0.03, sy0, sz - 0.035, sx0, sy1, sz + 0.005)
    m.box("gold", sx1, sy0, sz - 0.035, sx1 + 0.03, sy1, sz + 0.005)
    for k in range(11):
        x = sx0 + (sx1 - sx0) * k / 10
        m.sphere("bulb", 0.022, x, sy1 + 0.015, sz - 0.045, segs=5, rings=3)
        m.sphere("bulb", 0.022, x, sy0 - 0.015, sz - 0.045, segs=5, rings=3)
    for y in (sy0 + 0.12, sy0 + 0.25, sy0 + 0.38):
        m.sphere("bulb", 0.022, sx0 - 0.015, y, sz - 0.045, segs=5, rings=3)
        m.sphere("bulb", 0.022, sx1 + 0.015, y, sz - 0.045, segs=5, rings=3)
    # Bulbs under the canopy edge, lighting the counter.
    for k in range(13):
        x = X0 + 0.1 + (X1 - X0 - 0.2) * k / 12
        m.sphere("bulb", 0.028, x, yf - 0.04, zf + 0.10, segs=5, rings=3)
        m.tube("black", [(x, yf - 0.012, zf + 0.10), (x, yf - 0.03, zf + 0.10)], 0.004, segs=4, caps=False)
    return m


def _vertical_disc(x, y, z, r, segs=24):
    """Bullseye facing the shooter (-z)."""
    m = Mesh()
    for k in range(segs):
        a0, a1 = TAU * k / segs, TAU * (k + 1) / segs
        p0 = (x + r * math.cos(a0), y + r * math.sin(a0), z)
        p1 = (x + r * math.cos(a1), y + r * math.sin(a1), z)
        n = (0, 0, -1)
        m.tri("target", (x, y, z), p0, p1, n, n, n, (0.5, 0.5), (0.5 + 0.5 * math.cos(a0), 0.5 - 0.5 * math.sin(a0)), (0.5 + 0.5 * math.cos(a1), 0.5 - 0.5 * math.sin(a1)))
    m.tube("black", [(x, y, z + 0.003), (x, y, z + 0.015)], r + 0.006, segs=segs, caps=False)
    return m
