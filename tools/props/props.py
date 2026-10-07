"""Prop models. Every builder returns a Mesh in block units, facing north (front towards -z)."""
import math

from mesh import Mesh, TAU, rot_y, rot_x, rot_z, translate, chain, _cross, _norm, _sub

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
    # Back, open on the bartender side: a work shelf and an under-counter shelf, set in from the block edges so the
    # end panels and the next module never share a plane with them.
    m.box("black", 0.0, 0.07, 0.60, 1.0, 0.16, 0.64, faces="su")
    m.bevel_box("white", 0.002, 0.70, 0.46, 0.998, 0.73, 0.635, 0.004)
    m.bevel_box("white", 0.002, 0.30, 0.30, 0.998, 0.33, 0.635, 0.004)
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
    # Stands 3 mm proud of the module's own end faces (carcass, kick plate, shelves) and of the frame front.
    x0 = -0.003 if side < 0 else 0.97
    m.box("black", x0, 0.0, 0.006, x0 + 0.033, 1.018, 0.645)
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


# ---------------------------------------------------------------------------------------------------------------- access

# Turnstile geometry, shared with the Java renderer of the rotor (TurnstileRenderer):
# hub centre in block space and arm length. The passage runs along z, the cabinet stands on the -x side.
TURNSTILE_HUB = (0.385, 0.92, 0.5)
TURNSTILE_ARM = 0.60


def turnstile_cabinet():
    """Tripod turnstile cabinet in brushed stainless steel: column, wide head with a sloped top carrying two badge
    readers and the arrow display, end displays, hub boss on the passage side, base plate with anchor bolts.
    The rotor and the lit displays are drawn by the block entity renderer."""
    m = Mesh()
    S = "steel"
    # Base plate with bolts.
    m.bevel_box(S, 0.01, 0.0, 0.10, 0.36, 0.012, 0.90, 0.003)
    for (x, z) in ((0.05, 0.14), (0.31, 0.14), (0.05, 0.86), (0.31, 0.86)):
        m.cylinder("chrome", 0.012, 0.012, 0.02, cx=x, cz=z, segs=8)
    # Column with an access door outline and a key lock.
    m.bevel_box(S, 0.04, 0.012, 0.16, 0.29, 0.84, 0.84, 0.006)
    m.box("black", 0.288, 0.20, 0.24, 0.292, 0.76, 0.245, faces="e")
    m.box("black", 0.288, 0.20, 0.755, 0.292, 0.76, 0.76, faces="e")
    m.box("black", 0.288, 0.755, 0.24, 0.292, 0.76, 0.76, faces="e")
    m.box("black", 0.288, 0.20, 0.24, 0.292, 0.205, 0.76, faces="e")
    m.box("chrome", 0.289, 0.62, 0.70, 0.296, 0.65, 0.73, faces="eud")
    # Head: wider housing whose top slopes down towards the passage.
    head = [(0.0, 0.84), (0.34, 0.84), (0.34, 1.00), (0.0, 1.07)]
    m.extrude(S, [(p[0], p[1]) for p in head], 0.06, 0.94)
    m.box("black", 0.0, 0.838, 0.06, 0.34, 0.842, 0.94, faces="d")
    # Recesses on the sloped top: two readers and the display window.
    slope = math.atan2(0.07, 0.34)
    def on_top(x0, x1, z0, z1, mat, lift):
        y0 = 1.07 - 0.07 * x0 / 0.34 + lift
        y1 = 1.07 - 0.07 * x1 / 0.34 + lift
        m.quad(mat, (x0, y0, z1), (x1, y1, z1), (x1, y1, z0), (x0, y0, z0))
    for (z0, z1) in ((0.12, 0.30), (0.70, 0.88)):
        on_top(0.07, 0.27, z0, z1, "reader", 0.003)
    on_top(0.10, 0.25, 0.37, 0.63, "black", 0.002)
    # End display windows.
    m.box("black", 0.06, 0.88, 0.055, 0.28, 1.00, 0.06, faces="n")
    m.box("black", 0.06, 0.88, 0.94, 0.28, 1.00, 0.945, faces="s")
    # Hub boss on the passage side.
    boss = Mesh()
    boss.lathe("steel", [(0.075, 0.0), (0.075, 0.03), (0.06, 0.045)], cx=0.0, cz=0.0, segs=20)
    boss.disc("steel", 0.06, 0.045, cx=0.0, cz=0.0, segs=20)
    hx, hy, hz = TURNSTILE_HUB
    m.merge(boss.transformed(lambda p: (0.34 + p[1], hy + p[0], hz + p[2])))
    return m


def _guide_post(m, x, z):
    m.lathe("steel", [(0.07, 0.0), (0.07, 0.012), (0.03, 0.03)], cx=x, cz=z, segs=16)
    m.cylinder("steel", 0.025, 0.03, 1.02, cx=x, cz=z, segs=14)
    m.sphere("chrome", 0.03, x, 1.03, z, segs=12, rings=6)


def guide_rail():
    """Stainless steel queue rail: two posts with flanged feet and three rails along the passage."""
    m = Mesh()
    x = 0.5
    for z in (0.06, 0.94):
        _guide_post(m, x, z)
    for y in (0.30, 0.62, 0.98):
        m.tube("steel", [(x, y, 0.06), (x, y, 0.94)], 0.018, segs=10, caps=False)
    return m


def guide_rail_corner():
    """90° queue-rail corner. Arms run toward -z (the block facing) and +x (clockwise of it).

    Blockstate y-rotation and FurnitureBlock.rotated() swing those two arms together, and the Java
    corner picks its facing so the arms land on the two neighbouring rails. The short arc is the
    outside of that angle. Do not mirror it onto -x unless GuideRailBlock stops using getClockWise().
    """
    m = Mesh()
    n = 14
    arc = []
    for i in range(n + 1):
        a = -math.pi / 2 + (math.pi / 2) * i / n
        arc.append((0.5 + 0.44 * math.cos(a), 0.5 + 0.44 * math.sin(a)))
    _guide_post(m, arc[0][0], arc[0][1])
    _guide_post(m, arc[-1][0], arc[-1][1])
    for y in (0.30, 0.62, 0.98):
        m.tube("steel", [(x, y, z) for x, z in arc], 0.018, segs=10, caps=False)
    return m


# ---------------------------------------------------------------------------------------------------------------- helpers

def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _side_plate(m, mat, poly, x0, x1):
    """Prism from a polygon in the z/y plane ([(z, y), ...] counter-clockwise seen from -x), between x0 and x1."""
    p = Mesh()
    p.extrude(mat, poly, x0, x1)
    m.merge(p.transformed(lambda q: (q[2], q[1], q[0])))


def _front_panel(m, mat, x0, y0, x1, y1, z):
    """Quad facing -z carrying a whole texture, upright and read the right way round from the front."""
    m.quad(mat, (x1, y0, z), (x0, y0, z), (x0, y1, z), (x1, y1, z), (0, 0, -1), ((0, 0), (1, 0), (1, 1), (0, 1)))


def _sheet(m, mat, grid, hint, uvs=None, back_mat=None, gap=0.003):
    """Smooth-shaded cloth from a grid of points (rows x cols). The front faces {hint}; a back face sits {gap}
    behind it so the cloth reads from both sides."""
    rows, cols = len(grid), len(grid[0])
    nrm = [[None] * cols for _ in range(rows)]
    for i in range(rows):
        for j in range(cols):
            du = _sub(grid[i][min(j + 1, cols - 1)], grid[i][max(j - 1, 0)])
            dv = _sub(grid[min(i + 1, rows - 1)][j], grid[max(i - 1, 0)][j])
            n = _norm(_cross(du, dv))
            if _dot(n, hint) < 0:
                n = (-n[0], -n[1], -n[2])
            nrm[i][j] = n
    if uvs is None:
        uvs = [[(p[0], p[1]) for p in row] for row in grid]
    for i in range(rows - 1):
        for j in range(cols - 1):
            q = ((i, j), (i, j + 1), (i + 1, j + 1), (i + 1, j))
            pts = [grid[a][b] for a, b in q]
            ns = [nrm[a][b] for a, b in q]
            uv = [uvs[a][b] for a, b in q]
            m.quad(mat, *pts, uv=uv, normals=ns)
            if back_mat:
                bp = [(p[0] - n[0] * gap, p[1] - n[1] * gap, p[2] - n[2] * gap) for p, n in zip(pts, ns)]
                m.quad(back_mat, *bp, uv=uv, normals=[(-n[0], -n[1], -n[2]) for n in ns])


def _ring(m, mat, c, r, t, plane="xy", segs=12, tsegs=5):
    """Torus-like ring of tube radius t around c, in the given plane."""
    pts = []
    for k in range(segs + 1):
        a = TAU * k / segs
        if plane == "xy":
            pts.append((c[0] + r * math.cos(a), c[1] + r * math.sin(a), c[2]))
        elif plane == "yz":
            pts.append((c[0], c[1] + r * math.sin(a), c[2] + r * math.cos(a)))
        else:
            pts.append((c[0] + r * math.cos(a), c[1], c[2] + r * math.sin(a)))
    m.tube(mat, pts, t, segs=tsegs, caps=False)


# ---------------------------------------------------------------------------------------------------------------- stage

def _pleats(m, mat, xa, xb, y0, y1, z, depth=0.010, per=8):
    """Pleated drape hanging in front of the plane z (towards -z), along x. Pleats are locked to whole blocks so a
    run of modules reads as one skirt. Lined on the back."""
    n = max(2, int(round((xb - xa) * per * 4)))
    col = []
    for k in range(n + 1):
        x = xa + (xb - xa) * k / n
        a = TAU * per * x
        f = z - depth * (0.5 - 0.5 * math.cos(a))
        df = -depth * 0.5 * math.sin(a) * TAU * per
        col.append((x, f, _norm((df, 0, -1))))
    for k in range(n):
        (xa_, fa, na), (xb_, fb, nb) = col[k], col[k + 1]
        m.quad(mat, (xb_, y0, fb), (xa_, y0, fa), (xa_, y1, fa), (xb_, y1, fb),
               uv=((xb_, y0), (xa_, y0), (xa_, y1), (xb_, y1)), normals=(nb, na, na, nb))
        ba, bb = (-na[0], 0, -na[2]), (-nb[0], 0, -nb[2])
        m.quad(mat, (xa_, y0, fa + 0.002), (xb_, y0, fb + 0.002), (xb_, y1, fb + 0.002), (xa_, y1, fa + 0.002),
               uv=((xa_, y0), (xb_, y0), (xb_, y1), (xa_, y1)), normals=(ba, bb, bb, ba))


def _deck_skirt(m, h, xa, xb):
    """Velour skirt on the front of a deck: velcro header over the frame, pleats down to the floor."""
    m.box("velour", xa, h - 0.056, -0.022, xb, h - 0.012, -0.004, faces="nud")
    _pleats(m, "velour", xa, xb, 0.006, h - 0.056, -0.004, depth=0.016)


def stage_deck(level):
    """One metre of stage deck (praticable). level 1..4 is the height in quarter-blocks.
    Aluminium edge frame with chamfered joints, phenolic grip top, corner castings on round legs with screw jacks,
    ledgers and diagonal braces on the taller settings, dyed velour skirt on the audience side."""
    h = level * 0.25
    m = Mesh()
    t = 0.06
    # Frame: four chamfered extrusions, so a run of decks shows a fine joint line between modules.
    m.bevel_box("metal", 0.0, h - t, 0.0, 1.0, h, 0.03, 0.004)
    m.bevel_box("metal", 0.0, h - t, 0.97, 1.0, h, 1.0, 0.004)
    m.bevel_box("metal", 0.0, h - t, 0.03, 0.03, h, 0.97, 0.004)
    m.bevel_box("metal", 0.97, h - t, 0.03, 1.0, h, 0.97, 0.004)
    # Film-faced ply top, 3 mm under the frame edge, and its underside.
    m.box("deck", 0.03, h - 0.03, 0.03, 0.97, h - 0.003, 0.97, faces="u")
    m.box("wood_dark", 0.03, h - 0.03, 0.03, 0.97, h - 0.003, 0.97, faces="d")
    # Two joists under the ply.
    for z in (0.33, 0.64):
        m.box("metal", 0.03, h - t, z, 0.97, h - 0.03, z + 0.03, faces="nsd")
    # Corner castings with the leg sockets.
    legs = ((0.045, 0.045), (0.955, 0.045), (0.045, 0.955), (0.955, 0.955))
    for (x, z) in legs:
        m.bevel_box("metal", x - 0.045, h - t - 0.035, z - 0.045, x + 0.045, h - t, z + 0.045, 0.006)
        top = h - t - 0.035
        # Round leg, screw jack and foot plate.
        if top > 0.06:
            m.cylinder("metal", 0.021, 0.05, top, cx=x, cz=z, segs=12, caps=False)
            m.lathe("black", [(0.026, 0.040), (0.030, 0.045), (0.030, 0.055), (0.024, 0.06)], cx=x, cz=z, segs=6)
        m.cylinder("chrome", 0.011, 0.012, min(0.05, top), cx=x, cz=z, segs=8, caps=False)
        m.bevel_box("black", x - 0.04, 0.0, z - 0.04, x + 0.04, 0.012, z + 0.04, 0.003)
    # Ledgers and braces on the back and the two sides (the skirt hides the front).
    if level >= 2:
        y = 0.10
        m.tube("metal", [(0.045, y, 0.955), (0.955, y, 0.955)], 0.012, segs=8, caps=False)
        for x in (0.045, 0.955):
            m.tube("metal", [(x, y, 0.045), (x, y, 0.955)], 0.012, segs=8, caps=False)
    if level >= 3:
        top = h - t - 0.05
        m.tube("metal", [(0.06, 0.12, 0.955), (0.94, top, 0.955)], 0.010, segs=8)
        for x in (0.045, 0.955):
            m.tube("metal", [(x, 0.12, 0.06), (x, top, 0.94)], 0.010, segs=8)
        for (x, y, z) in ((0.06, 0.12, 0.955), (0.94, top, 0.955), (0.045, 0.12, 0.06), (0.045, top, 0.94),
                          (0.955, 0.12, 0.06), (0.955, top, 0.94)):
            m.sphere("black", 0.018, x, y, z, segs=6, rings=4)
    _deck_skirt(m, h, 0.0, 1.0)
    return m


def stage_deck_end(level, side):
    """Velour skirt returning along the open end of a deck run (side -1 = left, 1 = right)."""
    h = level * 0.25
    s = Mesh()
    if side > 0:
        _deck_skirt(s, h, -0.022, 1.0)
    else:
        _deck_skirt(s, h, 0.0, 1.022)
    return s.transformed(rot_y(90 if side > 0 else -90))


STRINGER = [(0.0, 0.012), (0.10, 0.012), (1.0, 0.90), (1.0, 1.0), (0.83, 1.0), (0.0, 0.17)]


def stage_stairs():
    """Stage stairs, four steps up to one block, bottom step towards the player (-z).
    Aluminium stringers and rear legs, chequer-plate treads with dyed nosings, black risers, hooks over the deck."""
    m = Mesh()
    for (x0, x1) in ((0.02, 0.06), (0.94, 0.98)):
        _side_plate(m, "metal", STRINGER, x0, x1)
        m.box("rubber", x0, 0.0, 0.0, x1, 0.012, 0.10)
        # Rear leg down to the floor.
        m.box("metal", x0 + 0.004, 0.012, 0.93, x1 - 0.004, 0.92, 0.97, faces="nsewd")
        m.box("rubber", x0, 0.0, 0.925, x1, 0.012, 0.975)
    m.tube("metal", [(0.06, 0.35, 0.95), (0.94, 0.35, 0.95)], 0.012, segs=8, caps=False)
    for i in range(4):
        zf, top = 0.25 * i, 0.25 * (i + 1)
        m.box("tread", 0.06, top - 0.035, zf, 0.94, top, zf + 0.25, faces="u")
        m.box("metal", 0.06, top - 0.035, zf, 0.94, top, zf + 0.25, faces="nd")
        m.box("paint", 0.06, top - 0.03, zf - 0.005, 0.94, top + 0.002, zf + 0.045, faces="nud")
        m.box("black", 0.06, 0.25 * i, zf + 0.03, 0.94, top - 0.035, zf + 0.045, faces="ns")
        # Brackets under the tread, bolted to the stringers.
        for x0 in (0.06, 0.90):
            m.box("metal", x0, top - 0.075, zf + 0.06, x0 + 0.04, top - 0.035, zf + 0.20, faces="nsud")
    # Hooks that rest on the deck the stairs lead up to.
    for x in (0.16, 0.84):
        m.bevel_box("metal", x - 0.06, 1.0, 0.95, x + 0.06, 1.012, 1.06, 0.003)
    return m


def _handrail(side, line, base):
    """Handrail on one side of a stair or ramp: two posts, a sloped top rail with returns and a knee rail.
    line(z) is the walking line, base(z) the top of the stringer the posts stand on."""
    m = Mesh()
    x = 0.04 if side < 0 else 0.96
    top = lambda z: line(z) + 0.90
    for z in (0.12, 0.88):
        m.tube("metal", [(x, base(z), z), (x, top(z), z)], 0.018, segs=10)
        m.bevel_box("metal", x - 0.03, base(z), z - 0.03, x + 0.03, base(z) + 0.012, z + 0.03, 0.003)
    rail = [(x, top(0.04) - 0.13, 0.0), (x, top(0.04) - 0.035, 0.012), (x, top(0.04), 0.04),
            (x, top(0.96), 0.96), (x, top(0.96) - 0.035, 0.988), (x, top(0.96) - 0.13, 1.0)]
    m.tube("metal", rail, 0.019, segs=10)
    m.tube("metal", [(x, line(0.12) + 0.48, 0.12), (x, line(0.88) + 0.48, 0.88)], 0.013, segs=8, caps=False)
    for z in (0.12, 0.88):
        m.sphere("black", 0.02, x, line(z) + 0.48, z, segs=6, rings=4)
    return m


def stage_stairs_rail(side):
    return _handrail(side, lambda z: min(1.0, z + 0.25), lambda z: min(1.0, z + 0.17))


RAMP_Y0 = 0.03


def _ramp_y(z):
    return RAMP_Y0 + (1.0 - RAMP_Y0) * z


def stage_ramp():
    """Access ramp up to one block: chequer-plate deck with rubber grip strips, side cheeks with dyed kerbs,
    closed back, hooks over the deck."""
    m = Mesh()
    k = 1.0 - RAMP_Y0
    n = _norm((0, 1, -k))
    seg = 4
    for i in range(seg):
        za, zb = i / seg, (i + 1) / seg
        ya, yb = _ramp_y(za), _ramp_y(zb)
        va, vb = za * math.hypot(1, k), zb * math.hypot(1, k)
        m.quad("tread", (0.06, ya, za), (0.94, ya, za), (0.94, yb, zb), (0.06, yb, zb), n,
               ((0.06, va), (0.94, va), (0.94, vb), (0.06, vb)))
    m.box("metal", 0.06, 0.0, 0.0, 0.94, RAMP_Y0, 0.012, faces="nu")
    for z in (0.12, 0.32, 0.52, 0.72, 0.90):
        za, zb = z - 0.022, z + 0.022
        lift = 0.004
        m.quad("rubber", (0.08, _ramp_y(za) + lift, za), (0.92, _ramp_y(za) + lift, za),
               (0.92, _ramp_y(zb) + lift, zb), (0.08, _ramp_y(zb) + lift, zb), n)
        m.quad("rubber", (0.08, _ramp_y(za), za - 0.0001), (0.92, _ramp_y(za), za - 0.0001),
               (0.92, _ramp_y(za) + lift, za), (0.08, _ramp_y(za) + lift, za), (0, 0, -1))
    for (x0, x1) in ((0.02, 0.06), (0.94, 0.98)):
        _side_plate(m, "metal", [(0.0, 0.0), (1.0, 0.0), (1.0, _ramp_y(1.0) + 0.06), (0.0, RAMP_Y0 + 0.06)], x0, x1)
        _side_plate(m, "paint", [(0.0, RAMP_Y0 + 0.06), (1.0, _ramp_y(1.0) + 0.06), (1.0, _ramp_y(1.0) + 0.085),
                                 (0.0, RAMP_Y0 + 0.085)], x0 - 0.002, x1 + 0.002)
    m.box("black", 0.06, 0.0, 0.985, 0.94, 0.99, 1.0, faces="s")
    for x in (0.16, 0.84):
        m.bevel_box("metal", x - 0.06, 1.0, 0.95, x + 0.06, 1.012, 1.06, 0.003)
    return m


def stage_ramp_rail(side):
    return _handrail(side, _ramp_y, lambda z: _ramp_y(z) + 0.085)


# ---------------------------------------------------------------------------------------------------------------- cyclorama

CYC_Z = 0.46
CYC_TOP = 2.86
CYC_PIPE = 2.94


def _cyc_profile():
    """Side profile of the cloth [(z, y)]: floor apron, radius cove, then straight up to the top hem."""
    pts = [(0.05, 0.006)]
    r = 0.34
    cz, cy = CYC_Z - r, 0.006 + r
    for k in range(11):
        a = -math.pi / 2 + (math.pi / 2) * k / 10
        pts.append((cz + r * math.cos(a), cy + r * math.sin(a)))
    for y in (1.2, 2.0, CYC_TOP):
        pts.append((CYC_Z, y))
    return pts


def _sandbag(m, x, z, y=0.012):
    m.sphere("canvas", 0.085, x, y + 0.035, z, segs=10, rings=6, sy=0.42)
    m.tube("canvas", [(x - 0.03, y + 0.07, z), (x, y + 0.095, z), (x + 0.03, y + 0.07, z)], 0.008, segs=5)


def cyclorama():
    """One metre of cyclorama: seamless dyed muslin sweeping into a floor cove, laced to a black top batten,
    a weighted bottom hem. From behind: an upright every metre, a rear brace and a sandbagged foot."""
    m = Mesh()
    prof = _cyc_profile()
    xs = (0.0, 0.5, 1.0)
    grid = [[(x, y, z) for x in xs] for (z, y) in prof]
    vs = [0.0]
    for i in range(1, len(prof)):
        vs.append(vs[-1] + math.dist(prof[i], prof[i - 1]))
    uvs = [[(x, vs[i]) for x in xs] for i in range(len(prof))]
    _sheet(m, "muslin", grid, (0, 0.7, -1), uvs, back_mat="muslin", gap=0.004)
    # Top hem laced to the batten, weighted hem along the front of the apron.
    m.tube("muslin", [(0.0, CYC_TOP, CYC_Z + 0.002), (1.0, CYC_TOP, CYC_Z + 0.002)], 0.012, segs=8, caps=False)
    m.tube("black", [(0.0, CYC_PIPE, CYC_Z + 0.002), (1.0, CYC_PIPE, CYC_Z + 0.002)], 0.022, segs=10, caps=False)
    for k in range(8):
        x = (k + 0.5) / 8
        m.tube("black", [(x - 0.035, CYC_TOP + 0.008, CYC_Z - 0.012), (x, CYC_PIPE - 0.018, CYC_Z - 0.02),
                         (x + 0.035, CYC_TOP + 0.008, CYC_Z - 0.012)], 0.0035, segs=4)
        m.sphere("chrome", 0.007, x - 0.035, CYC_TOP + 0.004, CYC_Z - 0.010, segs=6, rings=3)
    m.tube("muslin", [(0.0, 0.016, 0.05), (1.0, 0.016, 0.05)], 0.011, segs=8, caps=False)
    # Rear support.
    zb = 0.53
    m.tube("black", [(0.5, 0.012, zb), (0.5, CYC_PIPE + 0.01, zb)], 0.02, segs=8)
    m.bevel_box("black", 0.47, CYC_PIPE - 0.03, CYC_Z - 0.02, 0.53, CYC_PIPE + 0.03, zb + 0.03, 0.006)
    m.tube("black", [(0.5, 1.6, zb + 0.01), (0.5, 0.03, 0.95)], 0.014, segs=6)
    m.bevel_box("black", 0.42, 0.0, 0.48, 0.58, 0.012, 0.99, 0.003)
    _sandbag(m, 0.5, 0.78)
    return m


def cyclorama_end(side):
    """Upright closing a cyclorama run, the cloth edge laced to it."""
    m = Mesh()
    edge = 0.0 if side < 0 else 1.0
    xu = -0.07 if side < 0 else 1.07
    zu = CYC_Z + 0.002
    m.tube("black", [(xu, 0.012, zu), (xu, CYC_PIPE + 0.03, zu)], 0.024, segs=10)
    m.sphere("black", 0.03, xu, CYC_PIPE + 0.03, zu, segs=8, rings=4)
    m.tube("black", [(edge, CYC_PIPE, zu), (xu, CYC_PIPE, zu)], 0.022, segs=10, caps=False)
    m.bevel_box("black", xu - 0.035, CYC_PIPE - 0.035, zu - 0.035, xu + 0.035, CYC_PIPE + 0.035, zu + 0.035, 0.006)
    m.tube("black", [(xu, 1.5, zu + 0.02), (xu, 0.03, 0.93)], 0.014, segs=6)
    m.bevel_box("black", xu - 0.06, 0.0, 0.08, xu + 0.06, 0.012, 0.98, 0.003)
    _sandbag(m, xu, 0.80)
    # Hemmed edge of the cloth following the cove, laced to the upright.
    m.tube("muslin", [(edge, y, z) for (z, y) in _cyc_profile()[1:]], 0.008, segs=6)
    y = 0.55
    while y < CYC_TOP - 0.1:
        m.tube("black", [(edge, y, CYC_Z - 0.004), (xu, y + 0.09, zu - 0.02), (edge, y + 0.18, CYC_Z - 0.004)], 0.0035, segs=4)
        y += 0.18
    return m


# ---------------------------------------------------------------------------------------------------------------- road and site

def _butterfly_latch(m, x, y, z):
    """Recessed butterfly latch on a face looking at -z (z = face plane)."""
    m.bevel_box("chrome", x - 0.048, y - 0.048, z - 0.006, x + 0.048, y + 0.048, z + 0.002, 0.004)
    m.box("black", x - 0.036, y - 0.03, z - 0.0065, x + 0.036, y + 0.03, z - 0.006, faces="n")
    bow = [(-0.032, -0.018), (0.0, -0.007), (0.032, -0.018), (0.032, 0.018), (0.0, 0.007), (-0.032, 0.018)]
    m.extrude("chrome", [(x + px, y + py) for (px, py) in bow], z - 0.013, z - 0.0065)
    m.box("chrome", x - 0.022, y + 0.052, z - 0.008, x + 0.022, y + 0.075, z + 0.002, faces="nuew")


def _recessed_handle(m, x, y, z):
    m.bevel_box("black", x - 0.08, y - 0.04, z - 0.004, x + 0.08, y + 0.04, z + 0.002, 0.004)
    m.tube("chrome", [(x - 0.062, y + 0.02, z - 0.004), (x - 0.062, y, z - 0.016), (x + 0.062, y, z - 0.016),
                      (x + 0.062, y + 0.02, z - 0.004)], 0.007, segs=6)


def _caster(m, x, z, brake=False):
    m.bevel_box("metal", x - 0.045, 0.116, z - 0.045, x + 0.045, 0.13, z + 0.045, 0.003)
    m.cylinder("chrome", 0.028, 0.104, 0.116, cx=x, cz=z, segs=10)
    for dx in (-0.024, 0.017):
        m.box("metal", x + dx, 0.035, z - 0.02, x + dx + 0.007, 0.104, z + 0.035)
    m.tube("rubber", [(x - 0.016, 0.045, z + 0.012), (x + 0.016, 0.045, z + 0.012)], 0.045, segs=14)
    m.tube("chrome", [(x - 0.019, 0.045, z + 0.012), (x + 0.019, 0.045, z + 0.012)], 0.014, segs=8)
    if brake:
        m.box("red", x - 0.014, 0.075, z - 0.065, x + 0.014, 0.088, z - 0.02)


def _tiled_box(m, mat, x0, y0, z0, x1, y1, z1):
    """Box split into block-sized tiles so its texture keeps the same pixel size on big faces."""
    xs = [x0 + (x1 - x0) * k / max(1, math.ceil(x1 - x0 - 1e-6)) for k in range(max(1, math.ceil(x1 - x0 - 1e-6)) + 1)]
    ys = [y0 + (y1 - y0) * k / max(1, math.ceil(y1 - y0 - 1e-6)) for k in range(max(1, math.ceil(y1 - y0 - 1e-6)) + 1)]
    for i in range(len(xs) - 1):
        for j in range(len(ys) - 1):
            faces = "ns" + ("w" if i == 0 else "") + ("e" if i == len(xs) - 2 else "") + ("d" if j == 0 else "") + ("u" if j == len(ys) - 2 else "")
            m.box(mat, xs[i], ys[j], z0, xs[i + 1], ys[j + 1], z1, faces=faces)


def flight_case(w=1, top=0.80):
    """Road case {w} blocks wide (x from 0 to w) whose lid stands at {top}: black laminated ply between aluminium
    extrusions, ball corners, a hybrid lid valance, recessed butterfly latches, spring handles on both ends, a dyed
    stencil band, label holder, castors with brakes and stacking cups on the lid."""
    m = Mesh()
    x0, x1, y0, y1, z0, z1 = 0.08, w - 0.08, 0.13, top, 0.14, 0.86
    xc = (x0 + x1) / 2
    e = 0.026
    _tiled_box(m, "case", x0 + 0.004, y0 + 0.004, z0 + 0.004, x1 - 0.004, y1 - 0.004, z1 - 0.004)
    for y in (y0, y1 - e):
        for z in (z0, z1 - e):
            m.bevel_box("metal", x0 + e, y, z, x1 - e, y + e, z + e, 0.004)
    for x in (x0, x1 - e):
        for z in (z0, z1 - e):
            m.bevel_box("metal", x, y0 + e, z, x + e, y1 - e, z + e, 0.004)
    for x in (x0, x1 - e):
        for y in (y0, y1 - e):
            m.bevel_box("metal", x, y, z0 + e, x + e, y + e, z1 - e, 0.004)
    for x in (x0 + 0.012, x1 - 0.012):
        for y in (y0 + 0.012, y1 - 0.012):
            for z in (z0 + 0.012, z1 - 0.012):
                m.sphere("chrome", 0.03, x, y, z, segs=10, rings=6)
    # Lid line, and on tall cases a second valance where the front door meets the base.
    yl = y1 - 0.20
    lines = [yl] + ([y0 + 0.30] if top > 1.2 else [])
    for y in lines:
        m.bevel_box("metal", x0 + e, y - 0.012, z0 - 0.003, x1 - e, y + 0.012, z1 + 0.003, 0.003)
        m.bevel_box("metal", x0 - 0.003, y - 0.012, z0 + e, x1 + 0.003, y + 0.012, z1 - e, 0.003)
    # Dyed stencil band round the lid.
    m.box("paint", x0 + e, yl + 0.065, z0 + 0.001, x1 - e, yl + 0.135, z1 - 0.001, faces="ns")
    m.box("paint", x0 + 0.001, yl + 0.065, z0 + e, x1 - 0.001, yl + 0.135, z1 - e, faces="ew")
    # Front: latches along each valance, the label holder in the middle of the front.
    n = max(2, round(w * 2))
    for y in lines:
        for k in range(n):
            _butterfly_latch(m, x0 + (x1 - x0) * (k + 0.5) / n, y, z0)
    ly = (y0 + yl) / 2 if top < 1.2 else (lines[1] + yl) / 2
    m.bevel_box("chrome", xc - 0.13, ly - 0.065, z0 - 0.004, xc + 0.13, ly + 0.065, z0 + 0.002, 0.003)
    _front_panel(m, "label", xc - 0.115, ly - 0.053, xc + 0.115, ly + 0.053, z0 - 0.0045)
    # Ends: a latch per valance and recessed handles, built on a face looking at -z then turned round.
    handles = [y0 + (yl - y0) * 0.55] if top < 1.2 else [y0 + 0.55, yl - 0.35]
    for deg in (90, -90):
        s = Mesh()
        zf = 0.5 - (x1 - xc)
        for y in lines:
            _butterfly_latch(s, xc, y, zf)
        for y in handles:
            _recessed_handle(s, xc, y, zf)
        m.merge(s.transformed(rot_y(deg, xc, 0.5)))
    # Stacking cups on the lid, castors underneath.
    pos = ((x0 + 0.12, 0.26), (x1 - 0.12, 0.26), (x0 + 0.12, 0.74), (x1 - 0.12, 0.74))
    for (x, z) in pos:
        m.lathe("black", [(0.058, y1), (0.058, y1 + 0.006), (0.048, y1 + 0.006), (0.040, y1 + 0.001)], cx=x, cz=z, segs=14)
        m.disc("rubber", 0.040, y1 + 0.001, cx=x, cz=z, segs=14)
    for k, (x, z) in enumerate(pos):
        _caster(m, x, z, brake=k < 2)
    return m


def _louvre(m, x0, x1, y0, y1, z, mat="paint"):
    """Grille of horizontal slats over a dark opening on a face looking at -z."""
    m.box("black", x0, y0, z - 0.002, x1, y1, z, faces="n")
    n = max(2, int((y1 - y0) / 0.034))
    for k in range(n):
        y = y0 + 0.008 + (y1 - y0 - 0.016) * k / max(1, n - 1)
        m.quad(mat, (x1, y - 0.006, z - 0.016), (x0, y - 0.006, z - 0.016), (x0, y + 0.012, z - 0.002), (x1, y + 0.012, z - 0.002))
        m.quad(mat, (x0, y - 0.006, z - 0.016), (x1, y - 0.006, z - 0.016), (x1, y - 0.006, z - 0.002), (x0, y - 0.006, z - 0.002), (0, -1, 0))
    m.box(mat, x0 - 0.012, y0 - 0.012, z - 0.018, x1 + 0.012, y0, z, faces="nuewd")
    m.box(mat, x0 - 0.012, y1, z - 0.018, x1 + 0.012, y1 + 0.012, z, faces="nuewd")
    m.box(mat, x0 - 0.012, y0, z - 0.018, x0, y1, z, faces="new")
    m.box(mat, x1, y0, z - 0.018, x1 + 0.012, y1, z, faces="new")


# Generator canopy, three blocks long (x from -1 to 2) and two high. The exhaust position is mirrored in GeneratorBlock.
GEN_X0, GEN_X1, GEN_Z0, GEN_Z1, GEN_TOP = -0.95, 1.95, 0.05, 0.95, 1.78
GEN_EXHAUST = (-0.55, 0.65)


def _door(s, xa, xb, ya, yb, zf, louvre=True, handle=True):
    """Canopy door outlined by its seams, on a face looking at -z: louvred upper half, recessed handle."""
    for (a, b, c, d) in ((xa, ya, xb, ya + 0.006), (xa, yb - 0.006, xb, yb), (xa, ya, xa + 0.006, yb), (xb - 0.006, ya, xb, yb)):
        s.box("black", a, b, zf - 0.001, c, d, zf, faces="n")
    if louvre:
        _louvre(s, xa + 0.10, xb - 0.10, ya + (yb - ya) * 0.52, yb - 0.10, zf)
    if handle:
        hx = xb - 0.07
        hy = ya + (yb - ya) * 0.42
        s.bevel_box("black", hx - 0.022, hy - 0.07, zf - 0.006, hx + 0.022, hy + 0.07, zf + 0.002, 0.004)
        s.box("chrome", hx - 0.008, hy - 0.05, zf - 0.02, hx + 0.008, hy + 0.05, zf - 0.006)


def _socket(m, x, y, z, mat, r=0.045):
    m.tube(mat, [(x, y, z), (x, y, z - 0.045)], r, segs=14)
    m.tube("black", [(x, y, z - 0.045), (x, y, z - 0.047)], r * 0.62, segs=12)
    lid = Mesh()
    lid.bevel_box(mat, x - r - 0.004, 0.0, -0.05, x + r + 0.004, 0.01, 0.0, 0.003)
    m.merge(lid.transformed(chain(rot_x(35, 0.0, 0.0), translate(0, y + r + 0.006, z))))


def generator():
    """Silent canopied event generator (around 100 kVA), three blocks long: bunded base tank with fork pockets,
    dyed canopy with seamed doors and louvres, a control section with the panel under a hood, CEE sockets with flip
    lids and an emergency stop, radiator outlet and intake grilles on the ends, fuel filler, rating plate, lifting
    eye, exhaust stack with a rain flap and an earth spike."""
    m = Mesh()
    X0, X1, Z0, Z1, top = GEN_X0, GEN_X1, GEN_Z0, GEN_Z1, GEN_TOP
    # Base tank.
    m.bevel_box("black", X0 - 0.01, 0.0, Z0 - 0.01, X1 + 0.01, 0.20, Z1 + 0.01, 0.01)
    m.bevel_box("black", X0 - 0.02, 0.17, Z0 - 0.02, X1 + 0.02, 0.20, Z1 + 0.02, 0.006)
    for xa in (-0.55, 1.20):
        m.box("rubber", xa, 0.03, Z0 - 0.012, xa + 0.34, 0.14, Z0 - 0.01, faces="n")
        m.box("rubber", xa, 0.03, Z1 + 0.01, xa + 0.34, 0.14, Z1 + 0.012, faces="s")
    m.tube("chrome", [(1.82, 0.08, Z0 - 0.012), (1.82, 0.08, Z0 - 0.03)], 0.016, segs=8)
    # Canopy and roof cap.
    m.bevel_box("paint", X0, 0.20, Z0, X1, top, Z1, 0.03)
    m.bevel_box("paint", X0 + 0.02, top - 0.006, Z0 + 0.02, X1 - 0.02, top + 0.035, Z1 - 0.02, 0.014)
    roof = top + 0.035
    # Front long side: two service doors, then the control section behind its own seam.
    s = Mesh()
    zf = Z0
    _door(s, -0.86, -0.01, 0.26, 1.70, zf)
    _door(s, 0.01, 0.86, 0.26, 1.70, zf)
    _door(s, 0.94, 1.88, 0.26, 1.70, zf, louvre=False, handle=False)
    m.merge(s)
    m.bevel_box("black", 1.02, 1.08, zf - 0.010, 1.80, 1.56, zf + 0.002, 0.006)
    _front_panel(m, "gen_panel", 1.05, 1.11, 1.77, 1.53, zf - 0.0105)
    m.bevel_box("paint", 1.00, 1.56, zf - 0.09, 1.82, 1.585, zf + 0.002, 0.006)
    m.bevel_box("grey", 1.02, 0.52, zf - 0.008, 1.80, 0.96, zf + 0.002, 0.006)
    for (x, y, mat, r) in ((1.14, 0.66, "blue", 0.04), (1.30, 0.66, "blue", 0.04), (1.46, 0.66, "blue", 0.04),
                           (1.66, 0.68, "red", 0.06)):
        _socket(m, x, y, zf - 0.008, mat, r)
    for k in range(6):
        x = 1.08 + k * 0.11
        m.box("black", x, 0.85, zf - 0.016, x + 0.07, 0.92, zf - 0.008, faces="nudew")
        m.box("white", x + 0.02, 0.875, zf - 0.024, x + 0.05, 0.895, zf - 0.016, faces="nudew")
    m.bevel_box("yellow", 1.82, 1.30, zf - 0.008, 1.92, 1.40, zf + 0.002, 0.004)
    m.tube("red", [(1.87, 1.35, zf - 0.008), (1.87, 1.35, zf - 0.03)], 0.016, segs=10, caps=False)
    m.tube("red", [(1.87, 1.35, zf - 0.03), (1.87, 1.35, zf - 0.044)], 0.032, segs=12)
    # Back long side: three louvred doors.
    s = Mesh()
    for (xa, xb) in ((-0.86, 0.10), (0.12, 1.08), (1.10, 1.86)):
        _door(s, xa, xb, 0.26, 1.70, Z0)
    m.merge(s.transformed(rot_y(180)))
    # Ends: radiator outlet on -x, intake, fuel filler and rating plate on +x.
    for deg in (90, -90):
        s = Mesh()
        zf = 0.5 - (X1 - 0.5)
        if deg < 0:
            _louvre(s, 0.16, 0.84, 0.42, 1.60, zf)
        else:
            _louvre(s, 0.18, 0.82, 1.00, 1.60, zf)
            s.tube("black", [(0.30, 0.50, zf), (0.30, 0.50, zf - 0.03)], 0.05, segs=14, caps=False)
            s.tube("chrome", [(0.30, 0.50, zf - 0.03), (0.30, 0.50, zf - 0.045)], 0.045, segs=14)
            s.bevel_box("chrome", 0.52, 0.44, zf - 0.004, 0.80, 0.66, zf + 0.002, 0.003)
            _front_panel(s, "label", 0.535, 0.455, 0.785, 0.645, zf - 0.0045)
        m.merge(s.transformed(rot_y(deg)))
    # Roof: lifting eye in a recess, radiator cap, exhaust stack with a rain flap.
    m.box("black", 0.38, roof, 0.38, 0.62, roof + 0.002, 0.62, faces="u")
    m.tube("chrome", [(0.44, roof, 0.5), (0.44, roof + 0.07, 0.5), (0.5, roof + 0.12, 0.5), (0.56, roof + 0.07, 0.5),
                      (0.56, roof, 0.5)], 0.016, segs=8)
    m.cylinder("black", 0.05, roof, roof + 0.03, cx=1.55, cz=0.5, segs=12)
    m.cylinder("chrome", 0.035, roof + 0.03, roof + 0.045, cx=1.55, cz=0.5, segs=12)
    ex, ez = GEN_EXHAUST
    m.lathe("black", [(0.10, roof), (0.10, roof + 0.02), (0.065, roof + 0.03)], cx=ex, cz=ez, segs=14)
    m.cylinder("metal", 0.06, roof + 0.02, roof + 0.17, cx=ex, cz=ez, segs=14, caps=False)
    m.lathe("metal", [(0.06, roof + 0.17), (0.066, roof + 0.172), (0.066, roof + 0.18), (0.054, roof + 0.18)], cx=ex, cz=ez, segs=14)
    flap = Mesh()
    flap.bevel_box("metal", ex - 0.07, 0.0, ez - 0.07, ex + 0.07, 0.008, ez + 0.07, 0.003)
    m.merge(flap.transformed(chain(rot_x(-24, 0.0, ez + 0.07), translate(0, roof + 0.18, 0))))
    m.box("metal", ex - 0.02, roof + 0.16, ez + 0.058, ex + 0.02, roof + 0.19, ez + 0.075)
    # Earth spike with its green/yellow lead.
    m.tube("chrome", [(1.90, 0.0, -0.03), (1.90, 0.16, -0.03)], 0.008, segs=6)
    m.tube("yellow", [(1.90, 0.15, -0.03), (1.88, 0.15, 0.0), (1.85, 0.12, Z0 - 0.012)], 0.007, segs=5)
    return m


# Site toilet cabin: footprint and wall height.
WC0, WC1, WC_TOP = 0.09, 0.91, 2.05


def _wc_wall(s, z, ribs=True, vent=True):
    """One cabin wall on the plane z, looking at -z, between the corner posts."""
    a, b = WC0 + 0.035, WC1 - 0.035
    s.box("paint", a, 0.08, z, b, WC_TOP, z + 0.025, faces="ns")
    if ribs:
        for k in range(5):
            x = a + (b - a) * (k + 0.5) / 5
            s.bevel_box("paint", x - 0.026, 0.12, z - 0.016, x + 0.026, 1.76, z + 0.002, 0.008)
    if vent:
        _louvre(s, 0.30, 0.70, 1.84, 1.96, z)


def _wc_door(s, z, opened):
    """Door filling the opening, on the plane z, hinged on the -x jamb."""
    d = Mesh()
    d.box("paint", 0.165, 0.105, z - 0.006, 0.835, 1.895, z + 0.018)
    for x in (0.33, 0.50, 0.67):
        d.bevel_box("paint", x - 0.026, 0.20, z - 0.020, x + 0.026, 1.58, z - 0.004, 0.008)
    d.bevel_box("black", 0.75, 0.94, z - 0.012, 0.80, 1.08, z - 0.004, 0.004)
    d.tube("chrome", [(0.775, 1.0, z - 0.012), (0.775, 1.0, z - 0.03), (0.72, 1.0, z - 0.03)], 0.009, segs=6)
    d.box("green" if opened else "red", 0.755, 1.10, z - 0.012, 0.795, 1.13, z - 0.006, faces="nuewd")
    _front_panel(d, "wc_sign", 0.42, 1.66, 0.58, 1.82, z - 0.007)
    for y in (0.30, 1.00, 1.70):
        d.cylinder("black", 0.012, y - 0.05, y + 0.05, cx=0.17, cz=z - 0.008, segs=8)
    if opened:
        d = d.transformed(rot_y(-105, 0.165, z - 0.008))
    s.merge(d)


def site_toilet(opened):
    """Portable site toilet, two blocks tall: dyed ribbed shell on a dark base, corner posts, translucent domed
    roof, vent stack, louvred vents, door with a vacant/occupied indicator. Open, it shows the seat, the paper
    roll, a hand sanitiser and a coat hook."""
    m = Mesh()
    m.bevel_box("grey", WC0 - 0.01, 0.0, WC0 - 0.01, WC1 + 0.01, 0.08, WC1 + 0.01, 0.01)
    for deg in (90, -90, 180):
        s = Mesh()
        _wc_wall(s, WC0)
        m.merge(s.transformed(rot_y(deg)))
    for x in (WC0 + 0.035, WC1 - 0.035):
        for z in (WC0 + 0.035, WC1 - 0.035):
            m.cylinder("paint", 0.04, 0.08, WC_TOP, cx=x, cz=z, segs=12, caps=False)
    # Front: jambs, lintel and threshold around the door.
    m.box("paint", WC0 + 0.035, 0.08, WC0, 0.165, WC_TOP, WC0 + 0.025, faces="nsew")
    m.box("paint", 0.835, 0.08, WC0, WC1 - 0.035, WC_TOP, WC0 + 0.025, faces="nsew")
    m.box("paint", 0.165, 1.895, WC0, 0.835, WC_TOP, WC0 + 0.025, faces="nsd")
    m.bevel_box("grey", 0.14, 0.08, WC0 - 0.012, 0.86, 0.105, WC0 + 0.03, 0.004)
    _wc_door(m, WC0 + 0.006, opened)
    # Roof.
    m.bevel_box("white", 0.06, WC_TOP, 0.06, 0.94, WC_TOP + 0.05, 0.94, 0.02)
    _cushion(m, "white", 0.11, 0.89, 0.11, 0.89, WC_TOP + 0.05, 0.07)
    # Vent stack on the back.
    m.cylinder("black", 0.03, 0.70, 2.30, cx=0.78, cz=WC1 + 0.035, segs=10)
    m.lathe("black", [(0.0, 2.33), (0.05, 2.33), (0.05, 2.36), (0.0, 2.37)], cx=0.78, cz=WC1 + 0.035, segs=10)
    for y in (1.0, 1.9):
        m.box("black", 0.76, y, WC1, 0.80, y + 0.03, WC1 + 0.035)
    if opened:
        m.box("grey", WC0 + 0.03, 0.08, WC0 + 0.03, WC1 - 0.03, 0.09, WC1 - 0.03, faces="u")
        m.bevel_box("paint", WC0 + 0.03, 0.09, 0.52, WC1 - 0.03, 0.48, WC1 - 0.022, 0.01)
        _cushion(m, "white", 0.36, 0.64, 0.56, 0.84, 0.48, 0.022)
        m.disc("rubber", 0.075, 0.5025, cx=0.5, cz=0.70, segs=16)
        m.bevel_box("white", 0.37, 0.50, 0.855, 0.63, 0.80, 0.875, 0.006)
        m.box("chrome", WC0 + 0.025, 0.86, 0.395, WC0 + 0.05, 0.875, 0.405)
        m.tube("white", [(WC0 + 0.05, 0.85, 0.40), (WC0 + 0.13, 0.85, 0.40)], 0.045, segs=12)
        m.bevel_box("white", WC1 - 0.07, 1.20, 0.34, WC1 - 0.025, 1.38, 0.46, 0.006)
        m.box("black", WC1 - 0.08, 1.21, 0.385, WC1 - 0.07, 1.23, 0.415)
        m.tube("chrome", [(0.5, 1.50, WC1 - 0.025), (0.5, 1.50, WC1 - 0.07), (0.5, 1.53, WC1 - 0.08)], 0.008, segs=5)
    return m


FENCE_TOP = 1.96
FENCE_X0, FENCE_X1 = -0.95, 1.95


def site_fence():
    """Site fence panel, three blocks wide (x from -1 to 2): heras-style galvanised tube frame and welded mesh,
    a dyed privacy scrim with webbing hems, eyelets and cable ties, a rubber foot under each upright and couplers
    on the right end to clamp the next panel."""
    m = Mesh()
    G = "galvanised"
    z = 0.5
    a, b = FENCE_X0, FENCE_X1
    m.tube(G, [(a, 0.02, z), (a, FENCE_TOP - 0.035, z), (a + 0.015, FENCE_TOP - 0.01, z), (a + 0.035, FENCE_TOP, z),
               (b - 0.035, FENCE_TOP, z), (b - 0.015, FENCE_TOP - 0.01, z), (b, FENCE_TOP - 0.035, z), (b, 0.02, z)], 0.021, segs=10)
    m.tube(G, [(a, 0.17, z), (b, 0.17, z)], 0.015, segs=8, caps=False)
    for x in (0.0, 1.0):
        m.tube(G, [(x, 0.17, z), (x, FENCE_TOP, z)], 0.011, segs=6, caps=False)
    cols = [a + (b - a) * k / 3 for k in range(4)]
    for i in range(3):
        xa, xb = cols[i], cols[i + 1]
        for (ya, yb) in ((0.17, 1.065), (1.065, FENCE_TOP)):
            m.quad("mesh", (xb, ya, z), (xa, ya, z), (xa, yb, z), (xb, yb, z), (0, 0, -1),
                   ((xb, ya), (xa, ya), (xa, yb), (xb, yb)))
            m.quad("mesh", (xa, ya, z + 0.001), (xb, ya, z + 0.001), (xb, yb, z + 0.001), (xa, yb, z + 0.001), (0, 0, 1),
                   ((xa, ya), (xb, ya), (xb, yb), (xa, yb)))
    # Scrim on the public side, lined on the back.
    zs = 0.472
    sa, sb = a + 0.02, b - 0.02
    cols = [sa + (sb - sa) * k / 3 for k in range(4)]
    for i in range(3):
        xa, xb = cols[i], cols[i + 1]
        for (ya, yb) in ((0.14, 1.045), (1.045, 1.95)):
            m.quad("scrim", (xb, ya, zs), (xa, ya, zs), (xa, yb, zs), (xb, yb, zs), (0, 0, -1),
                   ((xb, ya), (xa, ya), (xa, yb), (xb, yb)))
            m.quad("scrim", (xa, ya, zs + 0.002), (xb, ya, zs + 0.002), (xb, yb, zs + 0.002), (xa, yb, zs + 0.002),
                   (0, 0, 1), ((xa, ya), (xb, ya), (xb, yb), (xa, yb)))
    for (ya, yb) in ((0.14, 0.19), (1.90, 1.95)):
        for i in range(3):
            m.box("hem", cols[i], ya, zs - 0.004, cols[i + 1], yb, zs,
                  faces="nud" + ("w" if i == 0 else "") + ("e" if i == 2 else ""))
    x = a + 0.125
    while x < b - 0.05:
        for (y, ry) in ((1.925, FENCE_TOP), (0.165, 0.17)):
            _ring(m, "chrome", (x, y, zs - 0.0045), 0.009, 0.003, "xy", segs=8, tsegs=4)
            _ring(m, "black", (x, ry, z), 0.032, 0.0035, "yz", segs=10, tsegs=4)
        x += 0.25
    _fence_foot(m, a)
    _fence_foot(m, b)
    # Couplers on the right end, reaching over to the next panel's upright.
    for y in (0.42, 1.62):
        m.bevel_box(G, b - 0.04, y - 0.028, 0.476, b + 0.13, y + 0.028, 0.524, 0.006)
        m.tube("chrome", [(b + 0.05, y, 0.468), (b + 0.05, y, 0.532)], 0.009, segs=6)
        m.box("chrome", b + 0.036, y - 0.014, 0.524, b + 0.064, y + 0.014, 0.536)
    return m


def _fence_foot(m, xu):
    """Recycled rubber foot under an upright at x = xu, kept inside the panel's own blocks."""
    x0, x1 = (xu - 0.05, xu + 0.17) if xu < 0.5 else (xu - 0.17, xu + 0.05)
    _side_plate(m, "rubber", [(0.28, 0.0), (0.72, 0.0), (0.68, 0.115), (0.32, 0.115)], x0, x1)
    for (za, zb) in ((0.30, 0.38), (0.62, 0.70)):
        m.bevel_box("rubber", x0 + 0.01, 0.0, za, x1 - 0.01, 0.03, zb, 0.004)
    for dx in (0.0, 0.12 if xu < 0.5 else -0.12):
        m.disc("black", 0.026, 0.1155, cx=xu + dx, cz=0.5, segs=10)


# Feather flag geometry: the pole bends from FLAG_BEND over an arc towards +x.
FLAG_BEND, FLAG_R, FLAG_SWEEP = 2.0, 0.78, math.radians(68)
FLAG_FOOT = 0.82
FLAG_CORNER = (1.02, 0.92)
# The whole flag is drawn at this scale: about 4.6 blocks to the tip, five blocks of pole collision.
FLAG_SCALE = 1.7


def _flag_pole(n_arc=16):
    pts = [(0.5, FLAG_FOOT + (FLAG_BEND - FLAG_FOOT) * k / 8) for k in range(9)]
    for k in range(1, n_arc + 1):
        a = FLAG_SWEEP * k / n_arc
        pts.append((0.5 + FLAG_R * (1 - math.cos(a)), FLAG_BEND + FLAG_R * math.sin(a)))
    return pts


def _along(path, s):
    """Point at fraction s of the length of a 2D polyline."""
    lens = [math.dist(path[i], path[i + 1]) for i in range(len(path) - 1)]
    d = s * sum(lens)
    for i, l in enumerate(lens):
        if d <= l or i == len(lens) - 1:
            t = 0 if l == 0 else min(1.0, d / l)
            return (path[i][0] + (path[i + 1][0] - path[i][0]) * t, path[i][1] + (path[i + 1][1] - path[i][1]) * t)
        d -= l


def _flag_wave(u, s):
    return 0.5 + 0.045 * u * math.sin(2.4 * u + 1.6 * s + 0.4)


def oriflamme():
    """Feather flag: cross base with rubber feet and a spigot, sectioned pole whose top bends over, a dyed
    feather-shaped flag in a sleeve, hemmed edges and a bungee from the bottom corner to the pole."""
    m = Mesh()
    # Base.
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        ex, ez = 0.5 + 0.26 * math.cos(a), 0.5 + 0.26 * math.sin(a)
        m.tube("black", [(0.5, 0.035, 0.5), (ex, 0.02, ez)], 0.016, segs=6)
        m.cylinder("rubber", 0.028, 0.0, 0.02, cx=ex, cz=ez, segs=10)
    m.lathe("black", [(0.045, 0.0), (0.045, 0.07), (0.03, 0.09)], segs=14)
    m.cylinder("chrome", 0.02, 0.09, 0.15, segs=10)
    # Pole: lower section and ferrule, the rest disappears into the sleeve.
    m.cylinder("black", 0.016, 0.15, FLAG_FOOT + 0.02, segs=10, caps=False)
    m.cylinder("chrome", 0.021, 0.62, 0.66, segs=10)
    # Flag: the pole side follows the bent pole, the free edge bulges out and climbs to the tip.
    pole = _flag_pole()
    tip = pole[-1]
    bx, by = FLAG_CORNER

    def free(t):
        return (bx + (tip[0] - bx) * t + 0.075 * math.sin(math.pi * t), by + (tip[1] - by) * t)

    rows, cols = 26, 7
    grid = []
    for i in range(rows + 1):
        s = i / rows
        L = _along(pole, s)
        R = free(s)
        row = []
        for j in range(cols + 1):
            u = j / cols
            row.append((L[0] + (R[0] - L[0]) * u, L[1] + (R[1] - L[1]) * u, _flag_wave(u, s)))
        grid.append(row)
    _sheet(m, "cloth", grid, (0, 0, -1), back_mat="cloth", gap=0.003)
    # Sleeve over the pole, hems, tip cap and bungee.
    m.tube("hem", [(p[0], p[1], 0.5) for p in pole], 0.021, segs=10)
    m.tube("hem", [(*free(i / 12), _flag_wave(1, i / 12)) for i in range(13)], 0.006, segs=5, caps=False)
    m.tube("hem", [grid[0][j] for j in range(cols + 1)], 0.006, segs=5, caps=False)
    m.sphere("black", 0.026, tip[0], tip[1], 0.5, segs=8, rings=4)
    c = grid[0][-1]
    _ring(m, "chrome", (c[0], c[1] - 0.012, c[2]), 0.01, 0.003, "xy", segs=8, tsegs=4)
    m.tube("black", [(c[0], c[1] - 0.02, c[2]), (0.75, 0.70, 0.5), (0.52, 0.64, 0.5)], 0.004, segs=4)
    k = FLAG_SCALE
    return m.transformed(lambda p: (0.5 + (p[0] - 0.5) * k, p[1] * k, 0.5 + (p[2] - 0.5) * k))


def _yz_board(m, mat, x, z0, y0, z1, y1, thick, wide):
    """A plank standing in the YZ plane: `thick` along the table, `wide` across its face."""
    dz, dy = z1 - z0, y1 - y0
    length = math.hypot(dz, dy) or 1.0
    pz, py = -dy / length, dz / length
    board = Mesh()
    board.box(mat, x - thick / 2, -wide / 2, 0.0, x + thick / 2, wide / 2, length)

    def xf(p):
        return (p[0], y0 + p[2] * dy / length + p[1] * py, z0 + p[2] * dz / length + p[1] * pz)

    m.merge(board, xf)


def picnic_table():
    """Two-metre pine picnic table: plank top, two plank benches, A-frames and the tie beams of the photos."""
    m = Mesh()
    top, frame = "wood", "wood_dark"
    # Tabletop: five boards, a little longer than the frames, with a small gap so the planks read separately.
    z0, z1, gap, n = 0.22, 0.78, 0.008, 5
    w = (z1 - z0 - gap * (n - 1)) / n
    for i in range(n):
        a = z0 + i * (w + gap)
        m.bevel_box(top, 0.05, 0.714, a, 1.95, 0.752, a + w, 0.007)
    # Benches are shorter than the top and sit outside it, one on each long side.
    benches = ((-0.10, 0.14), (0.86, 1.10))
    for a, b in benches:
        bw = (b - a - gap * 2) / 3
        for i in range(3):
            u = a + i * (bw + gap)
            m.bevel_box(top, 0.16, 0.418, u, 1.84, 0.450, u + bw, 0.006)
    for x in (0.28, 1.72):
        # Splayed legs, a low tie and the seat rail that carries the benches.
        _yz_board(m, frame, x, -0.02, 0.02, 0.46, 0.70, 0.045, 0.09)
        _yz_board(m, frame, x, 1.02, 0.02, 0.54, 0.70, 0.045, 0.09)
        m.bevel_box(frame, x - 0.022, 0.11, 0.10, x + 0.022, 0.17, 0.90, 0.004)
        m.bevel_box(frame, x - 0.022, 0.36, -0.02, x + 0.022, 0.418, 1.02, 0.004)
        for z in (0.02, 0.98):
            m.box("black", x - 0.012, 0.40, z - 0.012, x + 0.012, 0.424, z + 0.012)
    # Beams running the length: under the top, and under each bench.
    m.bevel_box(frame, 0.28, 0.64, 0.46, 1.72, 0.71, 0.54, 0.006)
    m.bevel_box(frame, 0.28, 0.385, -0.02, 1.72, 0.418, 0.06, 0.004)
    m.bevel_box(frame, 0.28, 0.385, 0.94, 1.72, 0.418, 1.02, 0.004)
    # Half again as long, wide and tall: about three metres, benches included.
    return m.transformed(lambda p: (p[0] * 1.5, p[1] * 1.5, 0.5 + (p[2] - 0.5) * 1.5))


# Water cannon, facing north (the jet leaves towards -z). The barrel is built on Y, then pitched up.
# Pivot sits in the main block so the whole mouth stays in that column and the block in front of it.
CANNON_PIVOT = (0.5, 1.08, 0.37)
CANNON_PITCH = -36.0
CANNON_NOZZLE = 0.84


def _fold_frame(m, hinge_x, foot_x):
    """One U-shaped folding leg and its diagonal stay. The foot sits toward the end of the top."""
    z_top, z_bot = (0.32, 0.68), (0.22, 0.78)
    for zt, zb in zip(z_top, z_bot):
        m.tube("black", [(hinge_x, 0.68, zt), (foot_x, 0.015, zb)], 0.016, segs=8)
        m.box("grey", hinge_x - 0.022, 0.652, zt - 0.022, hinge_x + 0.022, 0.700, zt + 0.022)
    m.tube("black", [(foot_x, 0.015, z_bot[0]), (foot_x, 0.015, z_bot[1])], 0.014, segs=8)
    # Stay from under the middle of the leaf out to a slider on the leg.
    stay_x = hinge_x + (1.0 - hinge_x) * 0.85
    slide_x = foot_x + (hinge_x - foot_x) * 0.42
    m.tube("black", [(stay_x, 0.655, 0.50), (slide_x, 0.30, 0.50)], 0.011, segs=6)
    m.box("grey", slide_x - 0.018, 0.282, 0.482, slide_x + 0.018, 0.318, 0.518)


def folding_table():
    """180 × 70 × 74 cm white plastic folding table, shown open. Length runs along +x across two blocks."""
    m = Mesh()
    # Two moulded leaves and the hairline seam between them.
    m.bevel_box("white", 0.10, 0.702, 0.15, 0.993, 0.740, 0.85, 0.006)
    m.bevel_box("white", 1.007, 0.702, 0.15, 1.90, 0.740, 0.85, 0.006)
    m.box("grey", 0.14, 0.668, 0.18, 1.86, 0.702, 0.82)
    for x in (0.16, 1.84):
        for z in (0.20, 0.80):
            m.box("grey", x - 0.012, 0.734, z - 0.012, x + 0.012, 0.746, z + 0.012)
    # Carry handle under one long edge, at the seam.
    m.tube("black", [(0.93, 0.64, 0.82), (0.93, 0.58, 0.90), (1.07, 0.58, 0.90), (1.07, 0.64, 0.82)], 0.011, segs=6)
    _fold_frame(m, 0.42, 0.14)
    _fold_frame(m, 1.58, 1.86)
    # One and a half times the 180 cm table: about 2.7 m long and 1.11 m tall.
    return m.transformed(lambda p: (p[0] * 1.5, p[1] * 1.5, 0.5 + (p[2] - 0.5) * 1.5))


# Water cannon, facing north (the jet leaves towards -z). The barrel is built on Y, then pitched up.
# Pivot sits in the main block so the whole mouth stays in that column and the block in front of it.
CANNON_PIVOT = (0.5, 1.08, 0.37)
CANNON_PITCH = -36.0
CANNON_NOZZLE = 0.84


def _inward(mesh, mat):
    """Flip one material so it is seen from inside (the bore of the barrel)."""
    flipped = []
    for (a, na, ua), (b, nb, ub), (c, nc, uc) in mesh.groups.get(mat, []):
        flipped.append((
            (a, (-na[0], -na[1], -na[2]), ua),
            (c, (-nc[0], -nc[1], -nc[2]), uc),
            (b, (-nb[0], -nb[1], -nb[2]), ub),
        ))
    mesh.groups[mat] = flipped


def _cannon_wheel(m, x, z, axle_to):
    """Solid road wheel: closed sidewalls and a rim, axle along X."""
    w = Mesh()
    w.lathe("rubber", [
        (0.055, -0.064), (0.13, -0.060), (0.175, -0.040), (0.192, -0.012),
        (0.192, 0.012), (0.175, 0.040), (0.13, 0.060), (0.055, 0.064),
    ], cx=0, cz=0, segs=20)
    w.disc("rubber", 0.13, -0.058, cx=0, cz=0, segs=20, up=False)
    w.disc("rubber", 0.13, 0.058, cx=0, cz=0, segs=20, up=True)
    w.lathe("galvanised", [
        (0.040, -0.042), (0.118, -0.034), (0.130, -0.010),
        (0.130, 0.010), (0.118, 0.034), (0.040, 0.042),
    ], cx=0, cz=0, segs=16)
    w.disc("galvanised", 0.115, -0.028, cx=0, cz=0, segs=16, up=False)
    w.disc("galvanised", 0.115, 0.028, cx=0, cz=0, segs=16, up=True)
    w.cylinder("black", 0.034, -0.048, 0.048, cx=0, cz=0, segs=10)
    m.merge(w.transformed(chain(rot_z(90, 0, 0), translate(x, 0.192, z))))
    m.tube("galvanised", [(x, 0.192, z), (axle_to, 0.192, z)], 0.022, segs=8, caps=False)


def _cannon_barrel():
    """Barrel along +Y: fan at the back (negative Y), mouth and spray ring at the front."""
    b = Mesh()
    b.lathe("paint", [
        (0.30, -0.58), (0.36, -0.54), (0.40, -0.48), (0.405, -0.40),
        (0.348, -0.355), (0.322, -0.28), (0.312, -0.02), (0.318, 0.22),
        (0.340, 0.42), (0.390, 0.58), (0.440, 0.70), (0.470, 0.78),
        (0.485, 0.82), (0.440, 0.855), (0.390, 0.835),
    ], cx=0, cz=0, segs=32)
    # Bands and the stainless lip the nozzles sit on.
    b.lathe("chrome", [(0.334, -0.01), (0.352, 0.012), (0.352, 0.09), (0.334, 0.112)], cx=0, cz=0, segs=32)
    b.lathe("white", [(0.328, 0.16), (0.338, 0.175), (0.338, 0.235), (0.328, 0.25)], cx=0, cz=0, segs=32)
    b.lathe("chrome", [
        (0.400, 0.74), (0.470, 0.765), (0.510, 0.805), (0.518, 0.835),
        (0.490, 0.868), (0.415, 0.875),
    ], cx=0, cz=0, segs=32)
    # Intake: grille, hub, blades.
    b.disc("fan", 0.29, -0.575, cx=0, cz=0, segs=28, up=False)
    b.lathe("chrome", [(0.30, -0.60), (0.345, -0.575), (0.345, -0.545), (0.30, -0.53)], cx=0, cz=0, segs=28)
    b.cylinder("galvanised", 0.055, -0.595, -0.545, cx=0, cz=0, segs=12)
    for k in range(7):
        blade = Mesh()
        blade.box("black", 0.06, -0.568, -0.006, 0.27, -0.552, 0.006)
        b.merge(blade.transformed(rot_y(k * (360 / 7), 0, 0)))
    # Bore closed onto the grille: the liner flares out until it meets the fan, so no ring of sky between them.
    b.disc("fan", 0.40, -0.47, cx=0, cz=0, segs=32, up=True)
    liner = Mesh()
    liner.lathe("white", [
        (0.39, -0.455), (0.36, -0.20), (0.30, 0.15), (0.32, 0.48), (0.40, 0.74), (0.44, 0.84),
    ], cx=0, cz=0, segs=32)
    _inward(liner, "white")
    b.merge(liner)
    for k in range(6):
        vane = Mesh()
        vane.box("white", 0.04, 0.20, -0.012, 0.20, 0.72, 0.012)
        b.merge(vane.transformed(rot_y(k * 60 + 8, 0, 0)))
    return b


def _cannon_base():
    """Skid, wheels, pedestal and control box. The barrel is a separate mesh so its tilt can change."""
    m = Mesh()
    # Skid: galvanised skirt, diamond-plate tank, filler, feet and the two rear wheels.
    m.box("galvanised", 0.22, 0.05, 0.04, 1.58, 0.14, 0.92)
    m.box("tread", 0.18, 0.14, 0.02, 1.62, 0.36, 0.94)
    m.cylinder("black", 0.040, 0.36, 0.42, cx=1.28, cz=0.28, segs=12)
    m.cylinder("chrome", 0.026, 0.42, 0.455, cx=1.28, cz=0.28, segs=12)
    for x in (0.34, 1.42):
        m.box("galvanised", x - 0.05, 0.0, 0.10, x + 0.05, 0.07, 0.22)
        m.box("rubber", x - 0.07, 0.0, 0.08, x + 0.07, 0.025, 0.24)
    _cannon_wheel(m, 0.10, 0.74, 0.36)
    _cannon_wheel(m, 1.70, 0.74, 1.40)
    # Pedestal and the yoke the barrel pivots in. The barrel is centred on the main block.
    m.cylinder("galvanised", 0.20, 0.36, 0.43, cx=0.5, cz=0.38, segs=20)
    m.cylinder("galvanised", 0.100, 0.43, 0.98, cx=0.5, cz=0.38, segs=16)
    m.box("galvanised", 0.10, 0.94, 0.24, 0.90, 1.02, 0.52)
    for x in (0.08, 0.86):
        m.box("galvanised", x, 0.90, 0.20, x + 0.06, 1.26, 0.52)
    px, py, pz = CANNON_PIVOT
    m.tube("chrome", [(0.06, py, pz), (0.94, py, pz)], 0.038, segs=12)
    m.cylinder("chrome", 0.038, 0.36, 0.45, cx=1.05, cz=0.55, segs=10)
    # Control box on the right of the deck, buttons facing out.
    m.bevel_box("grey", 1.18, 0.36, 0.48, 1.56, 0.76, 0.86, 0.008)
    m.box("black", 1.56, 0.46, 0.54, 1.572, 0.70, 0.80, faces="e")
    m.box("green", 1.56, 0.60, 0.72, 1.586, 0.66, 0.78, faces="e")
    m.box("red", 1.56, 0.50, 0.58, 1.592, 0.56, 0.64, faces="e")
    m.tube("yellow", [(1.38, 0.76, 0.66), (1.38, 0.82, 0.66)], 0.012, segs=6)
    return m


def water_cannon_aimed(elevation):
    """Barrel, ram and feed hose. ``elevation`` is degrees above the horizontal (0 lies flat, 90 points up)."""
    tilt = elevation - 90.0
    xf = chain(rot_x(tilt, 0, 0), translate(*CANNON_PIVOT))
    m = _cannon_barrel().transformed(xf)
    lug = xf((0.0, 0.18, -0.36))
    foot = (0.5, 0.46, 0.10)
    mid = tuple((a + b) * 0.5 for a, b in zip(foot, lug))
    m.tube("grey", [foot, mid], 0.032, segs=8)
    m.tube("chrome", [mid, lug], 0.016, segs=8)
    m.tube("chrome", [(0.42, 0.46, 0.10), (0.58, 0.46, 0.10)], 0.012, segs=6)
    inlet = xf((-0.34, -0.08, 0.0))
    m.tube("black", [(1.05, 0.45, 0.55), (1.05, 0.62, 0.48), (0.78, 0.88, 0.42), inlet], 0.018, segs=7)
    return m


def water_cannon_base():
    return _cannon_base()


def water_cannon():
    """Whole cannon at the default elevation, used for the item."""
    m = _cannon_base()
    m.merge(water_cannon_aimed(55))
    return m


def _distro_outlet(m, x, y, z, mat, r):
    """CEE outlet on a face looking toward -z: square flange, closed lid, blank label. No brand."""
    flange = r + 0.014
    m.box(mat, x - flange, y - flange, z - 0.010, x + flange, y + flange, z, faces="nsewd")
    cap = Mesh()
    cap.cylinder(mat, r * 0.82, -0.016, 0.0, cx=0, cz=0, segs=12, caps=False)
    cap.disc(mat, r * 0.82, 0.0, cx=0, cz=0, segs=12, up=False)
    cap.disc(mat, r * 0.78, -0.016, cx=0, cz=0, segs=12, up=True)
    cap.box("white", -r * 0.32, -0.022, r * 0.08, r * 0.32, -0.012, r * 0.48, faces="u")
    cap.box(mat, -r * 0.55, -0.006, r * 0.72, r * 0.55, 0.012, r * 1.05, faces="nsewud")
    m.merge(cap.transformed(chain(rot_x(-90, 0, 0), translate(x, y, z - 0.010))))
    for sx in (-1, 1):
        for sy in (-1, 1):
            m.box("grey", x + sx * (flange - 0.008) - 0.004, y + sy * (flange - 0.008) - 0.004, z - 0.014,
                  x + sx * (flange - 0.008) + 0.004, y + sy * (flange - 0.008) + 0.004, z - 0.010, faces="n")


def _distro_plug(m, x, y, z):
    """5-pin inlet plug lying on the coiled lead. Red body, grey collar, plain pins."""
    p = Mesh()
    p.cylinder("grey", 0.026, 0.0, 0.04, cx=0, cz=0, segs=12)
    p.cylinder("red", 0.042, 0.035, 0.095, cx=0, cz=0, segs=14)
    p.lathe("red", [(0.040, 0.095), (0.044, 0.10), (0.044, 0.112), (0.030, 0.122)], cx=0, cz=0, segs=14)
    p.disc("red", 0.028, 0.122, cx=0, cz=0, segs=14, up=True)
    for k in range(5):
        a = k * math.tau / 5 - 0.4
        p.cylinder("chrome", 0.0045, 0.122, 0.145, cx=math.cos(a) * 0.016, cz=math.sin(a) * 0.016, segs=5)
    m.merge(p.transformed(chain(rot_x(-58, 0, 0), rot_y(-25, 0, 0), translate(x, y, z))))


def power_distro():
    """Portable stage power distro. Black moulded case, two large outlets and twelve small ones in the front bay,
    breakers on the back, input lead coiled on the lid. Deliberately unbranded."""
    m = Mesh()
    x0, x1, z0, z1, top = 0.06, 0.94, 0.12, 0.90, 0.70
    # Body, then a shallow lip. The outlets sit just behind that lip, not at the back of a deep bay.
    m.bevel_box("black", x0, 0.02, 0.28, x1, top, z1, 0.014)
    m.bevel_box("black", x0, 0.08, z0, x0 + 0.07, top, 0.30, 0.008)
    m.bevel_box("black", x1 - 0.07, 0.08, z0, x1, top, 0.30, 0.008)
    m.bevel_box("black", x0, top - 0.07, z0, x1, top, 0.30, 0.008)
    m.bevel_box("black", x0, 0.02, z0, x1, 0.09, z1, 0.008)
    for fx, fz in ((0.16, 0.22), (0.84, 0.22), (0.16, 0.80), (0.84, 0.80)):
        m.box("rubber", fx - 0.045, 0.0, fz - 0.04, fx + 0.045, 0.028, fz + 0.04)
    pz = 0.22
    m.box("black", 0.15, 0.14, pz, 0.85, 0.60, 0.30)
    _distro_outlet(m, 0.26, 0.47, pz, "red", 0.055)
    _distro_outlet(m, 0.26, 0.28, pz, "red", 0.055)
    for y in (0.50, 0.38, 0.26):
        for col in range(4):
            _distro_outlet(m, 0.42 + col * 0.105, y, pz, "blue", 0.036)
    # Grip slots on the sides of the body.
    m.box("grey", x0 - 0.001, 0.40, 0.46, x0 + 0.018, 0.56, 0.70, faces="w")
    m.box("black", x0 + 0.008, 0.44, 0.50, x0 + 0.026, 0.52, 0.66, faces="w")
    m.box("grey", x1 - 0.018, 0.40, 0.46, x1 + 0.001, 0.56, 0.70, faces="e")
    m.box("black", x1 - 0.026, 0.44, 0.50, x1 - 0.008, 0.52, 0.66, faces="e")
    # Breakers on the back: one long row, a test button, a shorter row, the cable gland.
    m.box("grey", 0.18, 0.40, z1, 0.70, 0.58, z1 + 0.008)
    m.box("white", 0.70, 0.40, z1, 0.82, 0.58, z1 + 0.008)
    for i in range(9):
        x = 0.20 + i * 0.054
        m.box("white", x, 0.48, z1 + 0.008, x + 0.040, 0.55, z1 + 0.016)
        m.box("grey", x + 0.012, 0.50, z1 + 0.016, x + 0.028, 0.545, z1 + 0.022)
        m.box("red", x + 0.014, 0.44, z1 + 0.010, x + 0.026, 0.455, z1 + 0.016)
    m.tube("yellow", [(0.76, 0.50, z1 + 0.008), (0.76, 0.50, z1 + 0.026)], 0.016, segs=8)
    m.box("black", 0.735, 0.445, z1 + 0.010, 0.785, 0.458, z1 + 0.018)
    m.box("grey", 0.22, 0.22, z1, 0.52, 0.36, z1 + 0.008)
    for i in range(4):
        x = 0.24 + i * 0.065
        m.box("white", x, 0.28, z1 + 0.008, x + 0.046, 0.34, z1 + 0.016)
        m.box("grey", x + 0.014, 0.30, z1 + 0.016, x + 0.032, 0.335, z1 + 0.022)
    m.tube("black", [(0.70, 0.28, z1 - 0.01), (0.70, 0.28, z1 + 0.04)], 0.026, segs=10, caps=False)
    m.tube("grey", [(0.70, 0.28, z1 + 0.028), (0.70, 0.28, z1 + 0.05)], 0.016, segs=8)
    # Input lead: out of the gland, up the back, coiled on the lid, ending at the plug.
    cx, cz, cy = 0.50, 0.52, 0.735
    pts = [(0.70, 0.28, z1 + 0.045), (0.70, 0.52, z1 + 0.07), (0.70, top + 0.02, z1 + 0.02), (0.62, cy, 0.70)]
    steps = 40
    for i in range(steps + 1):
        t = i / steps
        a = t * math.tau * 2.15 + 0.6
        rr = 0.20 * (1.0 - 0.62 * t)
        pts.append((cx + math.cos(a) * rr, cy + 0.006 * math.sin(a * 3), cz + math.sin(a) * rr * 0.75))
    pts += [(0.38, 0.76, 0.40), (0.34, 0.78, 0.36)]
    m.tube("black", pts, 0.011, segs=6, caps=False)
    m.box("red", 0.55, 0.74, 0.60, 0.64, 0.755, 0.615)
    m.box("red", 0.55, 0.755, 0.58, 0.64, 0.77, 0.64)
    _distro_plug(m, 0.34, 0.78, 0.36)
    return m


def _rack_case(m, x0, x1, y0, y1, z0, z1):
    """Flight-case shell with no grabs and no latches. The front and back stay open for the fascias."""
    e = 0.030
    m.box("case", x0 + e, y0 + e, z0 + e, x1 - e, y1 - e, z1 - e)
    for y in (y0, y1 - e):
        for z in (z0, z1 - e):
            m.bevel_box("metal", x0 + e, y, z, x1 - e, y + e, z + e, 0.003)
    for x in (x0, x1 - e):
        for z in (z0, z1 - e):
            m.bevel_box("metal", x, y0 + e, z, x + e, y1 - e, z + e, 0.003)
    for x in (x0, x1 - e):
        for y in (y0, y1 - e):
            m.bevel_box("metal", x, y, z0 + e, x + e, y + e, z1 - e, 0.003)
    for x in (x0 + 0.014, x1 - 0.014):
        for y in (y0 + 0.014, y1 - 0.014):
            for z in (z0 + 0.014, z1 - 0.014):
                m.sphere("chrome", 0.030, x, y, z, segs=10, rings=6)
    # Dyeable band on the cheeks and the lid only, so it never crosses a fascia.
    m.box("paint", x0 + 0.001, y1 - 0.090, z0 + e + 0.01, x0 + e - 0.001, y1 - e - 0.012, z1 - e - 0.01, faces="w")
    m.box("paint", x1 - e + 0.001, y1 - 0.090, z0 + e + 0.01, x1 - 0.001, y1 - e - 0.012, z1 - e - 0.01, faces="e")
    m.box("paint", x0 + 0.10, y1 - 0.001, z0 + 0.10, x1 - 0.10, y1 + 0.004, z0 + 0.16, faces="u")
    # Rivets on the vertical corner extrusions, front and back.
    for x in (x0 + e * 0.42, x1 - e * 0.42):
        for z, face in ((z0 - 0.001, "n"), (z1 + 0.001, "s")):
            for i in range(5):
                y = y0 + 0.08 + (y1 - y0 - 0.16) * i / 4
                dz = -0.005 if face == "n" else 0.005
                m.box("chrome", x - 0.0035, y - 0.0035, min(z, z + dz), x + 0.0035, y + 0.0035, max(z, z + dz), faces=face)


def _out_front(part, x, y, z):
    """Local +Y sticks out of the front face, toward -z. Origin sits on the panel."""
    return part.transformed(chain(rot_x(-90, 0, 0), translate(x, y, z)))


def _cee_cap(m, x, y, z, color, r, tag=False, mark=False):
    """Closed CEE cap proud of a front panel. Optional blank yellow plate above it."""
    cap = Mesh()
    cap.cylinder(color, r * 0.96, 0.0, 0.008, cx=0, cz=0, segs=12, caps=False)
    cap.cylinder("black", r * 0.99, -0.003, 0.002, cx=0, cz=0, segs=12, caps=False)
    cap.cylinder(color, r * 0.78, 0.006, 0.018, cx=0, cz=0, segs=12, caps=False)
    cap.disc(color, r * 0.78, 0.018, cx=0, cz=0, segs=12, up=True)
    cap.cylinder("grey", r * 0.11, 0.008, 0.016, cx=0, cz=r * 0.70, segs=6)
    cap.box(color, -r * 0.20, 0.010, -r * 1.05, r * 0.20, 0.020, -r * 0.68)
    cap.box("grey", -r * 0.09, 0.014, -r * 1.16, r * 0.09, 0.022, -r * 0.96)
    if mark:
        cap.box("white", -r * 0.26, 0.017, -r * 0.22, r * 0.26, 0.023, r * 0.22)
    m.merge(_out_front(cap, x, y, z))
    if tag:
        tw, th = r * 0.92, max(0.012, r * 0.36)
        top = y + r * 0.92
        m.box("yellow", x - tw, top, z - 0.014, x + tw, top + th, z - 0.005)


def _earth_pip(m, x, y, z):
    """Small green/yellow earth sticker, no symbol drawn on it."""
    p = Mesh()
    p.cylinder("green", 0.013, 0.0, 0.005, cx=0, cz=0, segs=10)
    p.box("yellow", -0.004, 0.003, -0.009, 0.004, 0.008, 0.009)
    m.merge(_out_front(p, x, y, z))


def _inlet(m, x, y, z):
    """63 A 5-pin inlet: grey flange, red bowl, lid flipped up, pins toward the viewer."""
    fl = 0.056
    m.box("grey", x - fl, y - fl, z - 0.016, x + fl, y + fl, z - 0.003)
    m.box("black", x - fl + 0.007, y - fl + 0.007, z - 0.018, x + fl - 0.007, y + fl - 0.007, z - 0.014, faces="n")
    for sx in (-1, 1):
        for sy in (-1, 1):
            bx, by = x + sx * (fl - 0.012), y + sy * (fl - 0.012)
            m.box("chrome", bx - 0.0045, by - 0.0045, z - 0.022, bx + 0.0045, by + 0.0045, z - 0.015, faces="nsew")
    bowl = Mesh()
    bowl.cylinder("red", 0.040, 0.0, 0.014, cx=0, cz=0, segs=14, caps=False)
    bowl.disc("red", 0.040, 0.0, cx=0, cz=0, segs=14, up=False)
    bowl.cylinder("black", 0.028, 0.008, 0.018, cx=0, cz=0, segs=14, caps=False)
    bowl.disc("black", 0.026, 0.018, cx=0, cz=0, segs=14, up=True)
    # Earth at the bottom, thicker and longer. Four smaller pins around the clock.
    for ang, rad, pr, plen in (
        (-math.pi / 2, 0.015, 0.0070, 0.024),
        (math.radians(-18), 0.014, 0.0044, 0.017),
        (math.radians(54), 0.014, 0.0044, 0.017),
        (math.radians(126), 0.014, 0.0044, 0.017),
        (math.radians(198), 0.014, 0.0044, 0.017),
    ):
        bowl.cylinder("chrome", pr, 0.016, 0.016 + plen, cx=math.cos(ang) * rad, cz=math.sin(ang) * rad, segs=6)
    m.merge(_out_front(bowl, x, y, z - 0.014))
    m.box("grey", x - 0.030, y + fl - 0.012, z - 0.018, x + 0.030, y + fl + 0.004, z - 0.006)
    lid = Mesh()
    lid.box("red", -0.044, 0.004, -0.020, 0.044, 0.070, -0.009)
    lid.box("black", -0.028, 0.012, -0.023, 0.028, 0.058, -0.020)
    lid.box("grey", -0.010, 0.052, -0.026, 0.010, 0.068, -0.020)
    m.merge(lid.transformed(chain(rot_x(18, 0, 0), translate(x, y + fl - 0.006, z - 0.006))))
    _earth_pip(m, x + fl + 0.022, y + 0.016, z)


def _mcb_row(m, x0, x1, y, z, groups=(4, 4, 4)):
    """One DIN strip on the back face: a ticked legend, then grouped white breakers."""
    h = 0.062
    gap, split = 0.0032, 0.014
    n = sum(groups)
    inner = (x1 - x0) - split * (len(groups) - 1)
    w = (inner - gap * (n - len(groups))) / n
    m.box("grey", x0 - 0.006, y - 0.004, z, x1 + 0.006, y + h + 0.002, z + 0.006)
    m.box("white", x0, y + h + 0.006, z + 0.005, x1, y + h + 0.020, z + 0.012)
    x = x0
    i = 0
    for gi, count in enumerate(groups):
        if gi:
            x += split
        for k in range(count):
            on = (i % 5) != 3
            m.box("white", x, y, z + 0.006, x + w, y + h, z + 0.016)
            m.box("grey", x + 0.001, y + h - 0.008, z + 0.015, x + w - 0.001, y + h - 0.005, z + 0.018, faces="s")
            m.box("black", x + w * 0.18, y + 0.016, z + 0.015, x + w * 0.82, y + h - 0.014, z + 0.018, faces="s")
            ly0, ly1 = (y + 0.034, y + 0.052) if on else (y + 0.012, y + 0.030)
            m.box("grey", x + w * 0.22, ly0, z + 0.017, x + w * 0.78, ly1, z + 0.026)
            m.box("red", x + w * 0.28, ly1 - 0.007, z + 0.024, x + w * 0.72, ly1, z + 0.030, faces="s")
            # A tick on the legend, lined up with this module. No writing.
            tx = x + w * 0.35
            m.box("grey", tx, y + h + 0.009, z + 0.012, tx + w * 0.30, y + h + 0.013, z + 0.015, faces="s")
            x += w
            if k != count - 1:
                x += gap
            i += 1


def _green_button(m, x, y, z):
    b = Mesh()
    b.cylinder("chrome", 0.030, 0.0, 0.004, cx=0, cz=0, segs=14)
    b.cylinder("grey", 0.024, 0.002, 0.007, cx=0, cz=0, segs=14)
    b.cylinder("green", 0.018, 0.005, 0.014, cx=0, cz=0, segs=14)
    b.disc("green", 0.012, 0.015, cx=0, cz=0, segs=12, up=True)
    b.cylinder("white", 0.004, 0.014, 0.017, cx=0, cz=0, segs=8)
    m.merge(b.transformed(chain(rot_x(90, 0, 0), translate(x, y, z))))


def _rating_plate(m, x, y, z):
    """Blank white plate with ruling lines and four screws. No logo, no words."""
    w, h = 0.155, 0.175
    m.box("grey", x, y, z, x + w, y + h, z + 0.005)
    m.box("white", x + 0.008, y + 0.008, z + 0.005, x + w - 0.008, y + h - 0.008, z + 0.011)
    for i in range(6):
        yy = y + 0.026 + i * 0.022
        m.box("grey", x + 0.020, yy, z + 0.011, x + w - 0.020, yy + 0.0035, z + 0.014, faces="s")
    for sx in (x + 0.012, x + w - 0.012):
        for sy in (y + 0.012, y + h - 0.012):
            m.box("chrome", sx - 0.004, sy - 0.004, z + 0.010, sx + 0.004, sy + 0.004, z + 0.016, faces="s")


def _blue_caster(m, x, z):
    """Swivel caster parked so the blue hub faces the front, grey tyre around it."""
    m.box("metal", x - 0.022, 0.116, z - 0.020, x + 0.022, 0.150, z + 0.020)
    m.box("metal", x - 0.034, 0.100, z - 0.012, x + 0.034, 0.118, z + 0.012)
    w = Mesh()
    w.cylinder("blue", 0.048, -0.007, 0.007, cx=0, cz=0, segs=16, caps=True)
    w.lathe("grey", [(0.046, -0.009), (0.057, -0.003), (0.057, 0.003), (0.046, 0.009)], cx=0, cz=0, segs=16)
    w.cylinder("chrome", 0.014, -0.010, 0.010, cx=0, cz=0, segs=8)
    m.merge(w.transformed(chain(rot_x(90, 0, 0), translate(x, 0.060, z))))


def _fascia(m, x0, x1, y0, y1, z, sign):
    """Black instrument panel sitting on the case face, not down a hole. sign -1 = front."""
    if sign < 0:
        m.box("black", x0, y0, z - 0.007, x1, y1, z + 0.012)
        t = 0.008
        m.box("metal", x0 - t, y0 - t, z - 0.003, x1 + t, y0, z + 0.006)
        m.box("metal", x0 - t, y1, z - 0.003, x1 + t, y1 + t, z + 0.006)
        m.box("metal", x0 - t, y0, z - 0.003, x0, y1, z + 0.006)
        m.box("metal", x1, y0, z - 0.003, x1 + t, y1, z + 0.006)
    else:
        m.box("black", x0, y0, z - 0.012, x1, y1, z + 0.007)
        t = 0.008
        m.box("metal", x0 - t, y0 - t, z - 0.006, x1 + t, y0, z + 0.003)
        m.box("metal", x0 - t, y1, z - 0.006, x1 + t, y1 + t, z + 0.003)
        m.box("metal", x0 - t, y0, z - 0.006, x0, y1, z + 0.003)
        m.box("metal", x1, y0, z - 0.006, x1 + t, y1, z + 0.003)
    for sx in (x0 + 0.012, x1 - 0.012):
        for sy in (y0 + 0.012, y1 - 0.012):
            if sign < 0:
                m.box("chrome", sx - 0.004, sy - 0.004, z - 0.012, sx + 0.004, sy + 0.004, z - 0.006, faces="n")
            else:
                m.box("chrome", sx - 0.004, sy - 0.004, z + 0.006, sx + 0.004, sy + 0.004, z + 0.012, faces="s")


def power_rack():
    """One flight case on four blue casters. Sockets on -z, breakers on +z. No handles, no latches, no logo."""
    m = Mesh()
    x0, x1, y0, y1, z0, z1 = 0.045, 0.955, 0.158, 0.972, 0.22, 0.78
    _rack_case(m, x0, x1, y0, y1, z0, z1)
    # Thin galvanised dolly, four wheels. The cheeks stay bare.
    m.box("metal", 0.10, 0.136, 0.28, 0.90, 0.154, 0.72)
    m.box("metal", 0.08, 0.124, 0.26, 0.92, 0.140, 0.30)
    m.box("metal", 0.08, 0.124, 0.70, 0.92, 0.140, 0.74)
    m.box("metal", 0.08, 0.124, 0.26, 0.14, 0.140, 0.74)
    m.box("metal", 0.86, 0.124, 0.26, 0.92, 0.140, 0.74)
    for x, z in ((0.16, 0.30), (0.84, 0.30), (0.16, 0.70), (0.84, 0.70)):
        _blue_caster(m, x, z)

    # Front fascia fills the frame. Blues and the open inlet on the left,
    # reds with blank yellow plates in three rows (4 / 4 / 3) on the right.
    px0, px1, py0, py1 = 0.090, 0.910, 0.204, 0.926
    _fascia(m, px0, px1, py0, py1, z0 + 0.010, -1)
    zf = z0 + 0.003
    for y, r in ((0.800, 0.032), (0.690, 0.026), (0.590, 0.026)):
        _cee_cap(m, 0.210, y, zf, "blue", r)
    _inlet(m, 0.210, 0.345, zf)
    for x in (0.430, 0.545, 0.660, 0.775):
        for y in (0.790, 0.630):
            _cee_cap(m, x, y, zf, "red", 0.040, tag=True)
    _cee_cap(m, 0.455, 0.450, zf, "red", 0.050, tag=True, mark=True)
    _cee_cap(m, 0.640, 0.458, zf, "red", 0.034, tag=True)
    _cee_cap(m, 0.760, 0.458, zf, "red", 0.034, tag=True)

    # Back fascia. Facing this side, the strips sit on the left and the button
    # plus the ruled plate sit on the right.
    _fascia(m, px0, px1, py0, py1, z1 - 0.010, +1)
    zb = z1 - 0.003
    for y in (0.640, 0.470, 0.300):
        _mcb_row(m, 0.115, 0.690, y, zb)
    _green_button(m, 0.800, 0.760, zb)
    _rating_plate(m, 0.722, 0.300, zb)
    return m


# Cable ramp. One block is one module: channels run along Z (the facing axis).
# The profile below is the five-channel ramp, which fills the block. Fewer channels
# keep the same groove width and scale that profile about x=0.5 (see cable_ramp).
# Closed lid sits on the bay. Open lid swings up around the hinge on +X.
# These numbers are the full-width layout. CableRampBlock scales them the same way.
CABLE_LID = (0.264, 0.156, 0.010, 0.742, 0.222, 0.990)  # x0 y0 z0 x1 y1 z1
CABLE_HINGE = (0.742, 0.189)
CABLE_OPEN_DEG = -70.0
# Black plastic repeat, in blocks. Block textures do not wrap, so those faces are cut on this grid.
PLATE_TILE = 0.25
# One molded "CABLE" along the lid. The word runs with the ramp, not across it as a single stretched glyph.
LABEL_PITCH = 0.5


def _solid_prism(m, mat, poly, z0, z1, skip=()):
    """Convex counter-clockwise polygon in the XY plane, extruded from z0 to z1."""
    n = len(poly)
    cx = sum(p[0] for p in poly) / n
    cy = sum(p[1] for p in poly) / n
    for i in range(n):
        a, b = poly[i], poly[(i + 1) % n]
        m.tri(mat, (cx, cy, z0), (b[0], b[1], z0), (a[0], a[1], z0), (0, 0, -1), (0, 0, -1), (0, 0, -1))
        m.tri(mat, (cx, cy, z1), (a[0], a[1], z1), (b[0], b[1], z1), (0, 0, 1), (0, 0, 1), (0, 0, 1))
        if i in skip:
            continue
        nrm = _norm((b[1] - a[1], -(b[0] - a[0]), 0.0))
        m.quad(mat, (a[0], a[1], z0), (a[0], a[1], z1), (b[0], b[1], z1), (b[0], b[1], z0), nrm)


def _tile_splits(a, b, tile):
    """a..b split on the texture grid, so each piece spans at most one repeat."""
    lo, hi = (a, b) if a <= b else (b, a)
    pts = [lo]
    k = math.floor(lo / tile) + 1
    while k * tile < hi - 1e-6:
        pts.append(k * tile)
        k += 1
    if hi - pts[-1] > 1e-6:
        pts.append(hi)
    return pts


def _plate_flat(m, mat, x0, x1, z0, z1, y, nrm=(0.0, 1.0, 0.0)):
    """Horizontal plastic. UVs follow world X/Z, one tile at a time."""
    xs = _tile_splits(x0, x1, PLATE_TILE)
    zs = _tile_splits(z0, z1, PLATE_TILE)
    for i in range(len(xs) - 1):
        for j in range(len(zs) - 1):
            xa, xb = xs[i], xs[i + 1]
            za, zb = zs[j], zs[j + 1]
            m.quad(mat, (xa, y, za), (xa, y, zb), (xb, y, zb), (xb, y, za), nrm,
                   ((xa / PLATE_TILE, za / PLATE_TILE), (xa / PLATE_TILE, zb / PLATE_TILE),
                    (xb / PLATE_TILE, zb / PLATE_TILE), (xb / PLATE_TILE, za / PLATE_TILE)))


def _plate_slope(m, mat, p0, p1, z0, z1, nrm):
    """Plastic on a straight slope from p0 to p1 (x, y). Tiled along the slope and along Z."""
    length = math.hypot(p1[0] - p0[0], p1[1] - p0[1])
    if length < 1e-6:
        return
    steps = max(1, math.ceil(length / PLATE_TILE - 1e-9))
    zs = _tile_splits(z0, z1, PLATE_TILE)
    for i in range(steps):
        ta, tb = i / steps, (i + 1) / steps
        ax = p0[0] + (p1[0] - p0[0]) * ta
        ay = p0[1] + (p1[1] - p0[1]) * ta
        bx = p0[0] + (p1[0] - p0[0]) * tb
        by = p0[1] + (p1[1] - p0[1]) * tb
        ua, ub = length * ta / PLATE_TILE, length * tb / PLATE_TILE
        for j in range(len(zs) - 1):
            za, zb = zs[j], zs[j + 1]
            m.quad(mat, (ax, ay, za), (ax, ay, zb), (bx, by, zb), (bx, by, za), nrm,
                   ((ua, za / PLATE_TILE), (ua, zb / PLATE_TILE),
                    (ub, zb / PLATE_TILE), (ub, za / PLATE_TILE)))


def _plate_label(m, mat, x0, x1, z0, z1, y, nrm=(0.0, 1.0, 0.0)):
    """Lid top. The molded word repeats along Z and spans the lid, so it is not one giant letter."""
    zs = _tile_splits(z0, z1, LABEL_PITCH)
    for j in range(len(zs) - 1):
        za, zb = zs[j], zs[j + 1]
        ua, ub = za / LABEL_PITCH, zb / LABEL_PITCH
        m.quad(mat, (x0, y, za), (x0, y, zb), (x1, y, zb), (x1, y, za), nrm,
               ((ua, 0.0), (ub, 0.0), (ub, 1.0), (ua, 1.0)))


def _cyl_z(m, mat, r, z0, z1, x, y, segs=8):
    part = Mesh()
    part.cylinder(mat, r, 0.0, max(1e-4, z1 - z0), cx=0.0, cz=0.0, segs=segs, caps=True)
    m.merge(part.transformed(chain(rot_x(90, 0, 0), translate(x, y, z0))))


def cable_ramp(channels, open_lid=False):
    """Rubber cable ramp. ``channels`` is 1..5 grooves under the yellow lid.

    Five grooves fill the block. Fewer grooves keep about the same groove width, so the
    whole ramp (wings, lid, dogs) gets narrower and stays centred. No brand.
    """
    m = Mesh()
    n = max(1, min(5, int(channels)))
    s = n / 5.0

    def X(x):
        return 0.5 + (x - 0.5) * s

    # Left slope, outer low to inner high. Right is the mirror. X() is the centred width.
    lx0, ly0, lx1, ly1 = X(0.010), 0.012, X(0.198), 0.148
    rx0, ry0, rx1, ry1 = X(0.802), 0.148, X(0.990), 0.012
    sh_y = 0.148
    # Shoulders: flat black bands the lid rests on. Inner edge is the channel wall.
    sh_l0, sh_l1 = X(0.198), X(0.270)
    sh_r0, sh_r1 = X(0.730), X(0.802)
    bay0, bay1 = sh_l1, sh_r0
    floor0, floor1 = 0.022, 0.044
    rib_top = 0.132
    # Body stops a hair inside the block so two modules don't flicker on the shared face.
    bz0, bz1 = 0.002, 0.998

    # Bottom and top edges stay off the prism: the ground would flicker, and the top is smooth plastic.
    _solid_prism(m, "black", [(lx0, 0.0), (lx1, 0.0), (lx1, ly1), (lx0, ly0)], bz0, bz1, skip=(0, 1, 2))
    _solid_prism(m, "black", [(rx0, 0.0), (rx1, 0.0), (rx1, ry1), (rx0, ry0)], bz0, bz1, skip=(0, 2, 3))
    _plate_slope(m, "black_plate", (lx1, ly1), (lx0, ly0), bz0, bz1, _norm((ly0 - ly1, lx1 - lx0, 0.0)))
    _plate_slope(m, "black_plate", (rx1, ry1), (rx0, ry0), bz0, bz1, _norm((ry0 - ry1, rx1 - rx0, 0.0)))
    # Flat shoulders. The face against the slope stays open; the top is plate.
    m.box("black", sh_l0, 0.030, bz0, sh_l1, sh_y, bz1, faces="nse")
    m.box("black", sh_r0, 0.030, bz0, sh_r1, sh_y, bz1, faces="nsw")
    _plate_flat(m, "black_plate", sh_l0, sh_l1, bz0, bz1, sh_y)
    _plate_flat(m, "black_plate", sh_r0, sh_r1, bz0, bz1, sh_y)
    # Sill under the grooves. The north end is a socket; the south end carries the dogs.
    m.box("black", bay0, 0.001, 0.082, bay1, floor0, 0.948, faces="nsew")
    m.box("rubber", bay0, floor0, 0.078, bay1, floor1, bz1, faces="nsewu")
    # Ribs scale with the ramp so each groove stays about the same width.
    rib_w = (0.016 if n >= 4 else 0.022) * s
    if n > 1 and rib_w > 1e-4:
        span = bay1 - bay0
        ch_w = (span - rib_w * (n - 1)) / n
        x = bay0 + ch_w
        for _ in range(n - 1):
            m.box("black", x, floor1 - 0.002, bz0, x + rib_w, rib_top, bz1)
            x += rib_w + ch_w
    # Three dogs on the south end. They sit in the next module's north socket.
    for x0p, x1p in ((0.292, 0.392), (0.442, 0.558), (0.608, 0.708)):
        m.box("black", X(x0p), 0.0, 0.952, X(x1p), 0.020, 1.060, faces="nsewu")

    # Bolt holes on the black wings. Radius follows the wing so a single groove stays readable.
    for sx in (X(0.232), X(0.768)):
        for sz in (0.24, 0.50, 0.76):
            m.cylinder("grey", 0.018 * s, sh_y, sh_y + 0.004, cx=sx, cz=sz, segs=8, caps=False)
            m.cylinder("rubber", 0.010 * s, sh_y + 0.002, sh_y + 0.008, cx=sx, cz=sz, segs=8)

    lid = Mesh()
    x0, y0, lz0, x1, y1, lz1 = CABLE_LID
    x0, x1 = X(x0), X(x1)
    lid.box("yellow", x0, y0, lz0, x1, y1, lz1, faces="nsewd")
    _plate_label(lid, "yellow_plate", x0, x1, lz0, lz1, y1)
    hx, hy = X(CABLE_HINGE[0]), CABLE_HINGE[1]
    knuckle = 0.55 + 0.45 * s
    # Hinge barrel: black knuckles stay on the base, yellow ones travel with the lid.
    for i, hz in enumerate([0.08 + k * 0.052 for k in range(17)]):
        if i % 2 == 0:
            _cyl_z(m, "black", 0.020 * knuckle, hz, hz + 0.046, hx, hy, segs=8)
        else:
            _cyl_z(lid, "yellow", 0.024 * knuckle, hz, hz + 0.046, hx, hy, segs=8)
    if open_lid:
        m.merge(lid.transformed(rot_z(CABLE_OPEN_DEG, hx, hy)))
    else:
        m.merge(lid)
    return m
