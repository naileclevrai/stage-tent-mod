"""Small mesh toolkit for building prop models and exporting them as Forge OBJ block models.

Coordinates are in blocks, with the origin at the block corner (a single block spans 0..1 on every axis). Models are
built facing north (front towards -z), which is how Minecraft blockstates expect them before rotation.
"""
import math

TAU = math.pi * 2


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _norm(v):
    l = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return (v[0] / l, v[1] / l, v[2] / l) if l > 1e-12 else (0.0, 1.0, 0.0)


class Mesh:
    """Triangles grouped by material, each vertex with a position, a normal and a uv."""

    def __init__(self):
        self.groups = {}  # material -> list of triangles [(p, n, uv) * 3]

    # ------------------------------------------------------------------ raw faces

    def tri(self, mat, a, b, c, na=None, nb=None, nc=None, uva=(0, 0), uvb=(1, 0), uvc=(1, 1)):
        if na is None:
            n = _norm(_cross(_sub(b, a), _sub(c, a)))
            na = nb = nc = n
        self.groups.setdefault(mat, []).append(((a, na, uva), (b, nb, uvb), (c, nc, uvc)))

    def quad(self, mat, a, b, c, d, n=None, uv=((0, 0), (1, 0), (1, 1), (0, 1)), normals=None):
        """Counter-clockwise quad (seen from the front)."""
        if normals is None:
            if n is None:
                n = _norm(_cross(_sub(c, a), _sub(d, b)))
            normals = (n, n, n, n)
        self.tri(mat, a, b, c, normals[0], normals[1], normals[2], uv[0], uv[1], uv[2])
        self.tri(mat, a, c, d, normals[0], normals[2], normals[3], uv[0], uv[2], uv[3])

    # ------------------------------------------------------------------ solids

    def box(self, mat, x0, y0, z0, x1, y1, z1, uv_scale=1.0, faces="nsewud"):
        """Axis-aligned box; uvs follow world size so textures keep their pixel density."""
        s = uv_scale
        if "u" in faces:
            self.quad(mat, (x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (0, 1, 0),
                      ((x0 * s, z0 * s), (x0 * s, z1 * s), (x1 * s, z1 * s), (x1 * s, z0 * s)))
        if "d" in faces:
            self.quad(mat, (x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (0, -1, 0),
                      ((x0 * s, z1 * s), (x0 * s, z0 * s), (x1 * s, z0 * s), (x1 * s, z1 * s)))
        if "n" in faces:
            self.quad(mat, (x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (0, 0, -1),
                      ((x1 * s, y0 * s), (x0 * s, y0 * s), (x0 * s, y1 * s), (x1 * s, y1 * s)))
        if "s" in faces:
            self.quad(mat, (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (0, 0, 1),
                      ((x0 * s, y0 * s), (x1 * s, y0 * s), (x1 * s, y1 * s), (x0 * s, y1 * s)))
        if "w" in faces:
            self.quad(mat, (x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (-1, 0, 0),
                      ((z0 * s, y0 * s), (z1 * s, y0 * s), (z1 * s, y1 * s), (z0 * s, y1 * s)))
        if "e" in faces:
            self.quad(mat, (x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (1, 0, 0),
                      ((z1 * s, y0 * s), (z0 * s, y0 * s), (z0 * s, y1 * s), (z1 * s, y1 * s)))

    def bevel_box(self, mat, x0, y0, z0, x1, y1, z1, b=0.01):
        """Box with chamfered edges (rounded-looking corners for planks and tops)."""
        b = min(b, (x1 - x0) / 3, (y1 - y0) / 3, (z1 - z0) / 3)
        self.box(mat, x0 + b, y0, z0 + b, x1 - b, y1, z1 - b, faces="ud")
        self.box(mat, x0, y0 + b, z0 + b, x1, y1 - b, z1 - b, faces="ew")
        self.box(mat, x0 + b, y0 + b, z0, x1 - b, y1 - b, z1, faces="ns")
        # Chamfers along the 12 edges.
        k = 0.7071
        for (xa, xb, sx) in ((x0, x0 + b, -1), (x1, x1 - b, 1)):
            for (ya, yb, sy) in ((y0, y0 + b, -1), (y1, y1 - b, 1)):
                self.quad(mat, (xb, ya, z0 + b), (xb, ya, z1 - b), (xa, yb, z1 - b), (xa, yb, z0 + b), _norm((sx * k, sy * k, 0)))
        for (xa, xb, sx) in ((x0, x0 + b, -1), (x1, x1 - b, 1)):
            for (za, zb, sz) in ((z0, z0 + b, -1), (z1, z1 - b, 1)):
                self.quad(mat, (xb, y0 + b, za), (xb, y1 - b, za), (xa, y1 - b, zb), (xa, y0 + b, zb), _norm((sx * k, 0, sz * k)))
        for (ya, yb, sy) in ((y0, y0 + b, -1), (y1, y1 - b, 1)):
            for (za, zb, sz) in ((z0, z0 + b, -1), (z1, z1 - b, 1)):
                self.quad(mat, (x0 + b, ya, zb), (x1 - b, ya, zb), (x1 - b, yb, za), (x0 + b, yb, za), _norm((0, sy * k, sz * k)))
        self.fix_winding_outward(((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2), mat)

    def lathe(self, mat, profile, cx=0.5, cz=0.5, segs=24, uv_v=None, a0=0.0, a1=TAU, radius_fn=None, smooth=True):
        """Surface of revolution around the vertical axis. profile: [(radius, y), ...] from bottom to top.
        radius_fn(angle, ring_index) can modulate the radius (folds in cloth)."""
        rings = []
        for i, (r, y) in enumerate(profile):
            ring = []
            for k in range(segs + 1):
                a = a0 + (a1 - a0) * k / segs
                rr = r * (radius_fn(a, i) if radius_fn else 1.0)
                ring.append((cx + rr * math.cos(a), y, cz + rr * math.sin(a), a))
            rings.append(ring)
        # Accumulated profile length for v.
        vs = [0.0]
        for i in range(1, len(profile)):
            dr = profile[i][0] - profile[i - 1][0]
            dy = profile[i][1] - profile[i - 1][1]
            vs.append(vs[-1] + math.hypot(dr, dy))
        for i in range(len(rings) - 1):
            for k in range(segs):
                p00, p01 = rings[i][k], rings[i][k + 1]
                p10, p11 = rings[i + 1][k], rings[i + 1][k + 1]
                pts = [p00, p01, p11, p10]
                ns = []
                for (pt, ii, kk) in ((p00, i, k), (p01, i, k + 1), (p11, i + 1, k + 1), (p10, i + 1, k)):
                    ns.append(self._lathe_normal(rings, ii, kk, cx, cz) if smooth else None)
                u0, u1 = k / segs, (k + 1) / segs
                uv = ((u0 * 2, vs[i]), (u1 * 2, vs[i]), (u1 * 2, vs[i + 1]), (u0 * 2, vs[i + 1]))
                a, b, c, d = [(p[0], p[1], p[2]) for p in pts]
                if smooth:
                    self.quad(mat, a, d, c, b, uv=(uv[0], uv[3], uv[2], uv[1]), normals=(ns[0], ns[3], ns[2], ns[1]))
                else:
                    self.quad(mat, a, d, c, b, uv=(uv[0], uv[3], uv[2], uv[1]))

    @staticmethod
    def _lathe_normal(rings, i, k, cx, cz):
        n_r = len(rings)
        segs = len(rings[0]) - 1
        a = rings[min(i + 1, n_r - 1)][k]
        b = rings[max(i - 1, 0)][k]
        t_up = _sub(a, b)
        kp = k + 1 if k < segs else 1
        km = k - 1 if k > 0 else segs - 1
        t_side = _sub(rings[i][kp], rings[i][km])
        n = _norm(_cross(t_up[:3], t_side[:3]))
        p = rings[i][k]
        out = (p[0] - cx, 0, p[2] - cz)
        if n[0] * out[0] + n[2] * out[2] < 0 and abs(out[0]) + abs(out[2]) > 1e-6:
            n = (-n[0], -n[1], -n[2])
        elif abs(out[0]) + abs(out[2]) <= 1e-6 and n[1] < 0:
            n = (-n[0], -n[1], -n[2])
        return n

    def disc(self, mat, r, y, cx=0.5, cz=0.5, segs=24, up=True):
        for k in range(segs):
            a0, a1 = TAU * k / segs, TAU * (k + 1) / segs
            p0 = (cx + r * math.cos(a0), y, cz + r * math.sin(a0))
            p1 = (cx + r * math.cos(a1), y, cz + r * math.sin(a1))
            c = (cx, y, cz)
            uv0, uv1, uvc = (0.5 + 0.5 * math.cos(a0), 0.5 + 0.5 * math.sin(a0)), (0.5 + 0.5 * math.cos(a1), 0.5 + 0.5 * math.sin(a1)), (0.5, 0.5)
            if up:
                self.tri(mat, c, p1, p0, (0, 1, 0), (0, 1, 0), (0, 1, 0), uvc, uv1, uv0)
            else:
                self.tri(mat, c, p0, p1, (0, -1, 0), (0, -1, 0), (0, -1, 0), uvc, uv0, uv1)

    def cylinder(self, mat, r, y0, y1, cx=0.5, cz=0.5, segs=16, caps=True):
        self.lathe(mat, [(r, y0), (r, y1)], cx, cz, segs)
        if caps:
            self.disc(mat, r, y1, cx, cz, segs, True)
            self.disc(mat, r, y0, cx, cz, segs, False)

    def tube(self, mat, path, r, segs=8, caps=True, up_hint=(0, 1, 0)):
        """Round tube along a polyline [(x, y, z), ...]."""
        frames = []
        n = len(path)
        for i in range(n):
            a = path[max(0, i - 1)]
            b = path[min(n - 1, i + 1)]
            t = _norm(_sub(b, a))
            ref = up_hint if abs(t[0] * up_hint[0] + t[1] * up_hint[1] + t[2] * up_hint[2]) < 0.95 else (1, 0, 0)
            u = _norm(_cross(t, ref))
            w = _cross(t, u)
            frames.append((t, u, w))
        rings = []
        for i, p in enumerate(path):
            t, u, w = frames[i]
            ring = []
            for k in range(segs + 1):
                a = TAU * k / segs
                nrm = (u[0] * math.cos(a) + w[0] * math.sin(a), u[1] * math.cos(a) + w[1] * math.sin(a), u[2] * math.cos(a) + w[2] * math.sin(a))
                ring.append(((p[0] + nrm[0] * r, p[1] + nrm[1] * r, p[2] + nrm[2] * r), nrm))
            rings.append(ring)
        length = 0.0
        for i in range(n - 1):
            seg_len = math.dist(path[i], path[i + 1])
            for k in range(segs):
                (a, na), (b, nb) = rings[i][k], rings[i][k + 1]
                (c, nc), (d, nd) = rings[i + 1][k + 1], rings[i + 1][k]
                u0, u1 = k / segs, (k + 1) / segs
                self.quad(mat, a, d, c, b, uv=((u0, length), (u0, length + seg_len), (u1, length + seg_len), (u1, length)),
                          normals=(na, nd, nc, nb))
            length += seg_len
        if caps:
            for end, sign in ((0, -1), (n - 1, 1)):
                t = frames[end][0]
                cen = path[end]
                cn = (t[0] * sign, t[1] * sign, t[2] * sign)
                for k in range(segs):
                    a, b = rings[end][k][0], rings[end][k + 1][0]
                    if sign > 0:
                        self.tri(mat, cen, a, b, cn, cn, cn)
                    else:
                        self.tri(mat, cen, b, a, cn, cn, cn)

    def sphere(self, mat, r, cx, cy, cz, segs=12, rings=8, sy=1.0):
        prof = []
        for i in range(rings + 1):
            t = math.pi * i / rings - math.pi / 2
            prof.append((max(1e-4, r * math.cos(t)), cy + r * sy * math.sin(t)))
        self.lathe(mat, prof, cx, cz, segs)

    def extrude(self, mat, poly, z0, z1, side_mat=None):
        """Prism from a 2D polygon in the x/y plane (counter-clockwise), from z0 to z1 (z0 = front)."""
        side_mat = side_mat or mat
        n = len(poly)
        # Fan triangulation (polygon must be star-shaped from its centroid).
        cx = sum(p[0] for p in poly) / n
        cy = sum(p[1] for p in poly) / n
        for i in range(n):
            a, b = poly[i], poly[(i + 1) % n]
            self.tri(mat, (cx, cy, z0), (b[0], b[1], z0), (a[0], a[1], z0), (0, 0, -1), (0, 0, -1), (0, 0, -1),
                     (cx, cy), (b[0], b[1]), (a[0], a[1]))
            self.tri(mat, (cx, cy, z1), (a[0], a[1], z1), (b[0], b[1], z1), (0, 0, 1), (0, 0, 1), (0, 0, 1),
                     (cx, cy), (a[0], a[1]), (b[0], b[1]))
            nrm = _norm((b[1] - a[1], -(b[0] - a[0]), 0))
            l = math.dist(a, b)
            self.quad(side_mat, (a[0], a[1], z0), (a[0], a[1], z1), (b[0], b[1], z1), (b[0], b[1], z0), nrm,
                      ((0, 0), (z1 - z0, 0), (z1 - z0, l), (0, l)))

    # ------------------------------------------------------------------ transforms

    def merge(self, other, transform=None):
        for mat, tris in other.groups.items():
            for t in tris:
                if transform:
                    t = tuple((transform(p), _rot_only(transform, n), uv) for (p, n, uv) in t)
                self.groups.setdefault(mat, []).append(t)

    def transformed(self, fn):
        out = Mesh()
        out.merge(self, fn)
        return out

    def fix_winding_outward(self, center, mat):
        """Flips triangles of {mat} whose winding disagrees with their normal (used by bevel_box)."""
        fixed = []
        for t in self.groups.get(mat, []):
            (a, na, ua), (b, nb, ub), (c, nc, uc) = t
            gn = _cross(_sub(b, a), _sub(c, a))
            if gn[0] * na[0] + gn[1] * na[1] + gn[2] * na[2] < 0:
                t = ((a, na, ua), (c, nc, uc), (b, nb, ub))
            fixed.append(t)
        self.groups[mat] = fixed

    def tri_count(self):
        return sum(len(v) for v in self.groups.values())


def _rot_only(fn, n):
    o = fn((0, 0, 0))
    p = fn(n)
    return _norm((p[0] - o[0], p[1] - o[1], p[2] - o[2]))


def rot_y(deg, cx=0.5, cz=0.5):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return lambda p: (cx + (p[0] - cx) * c - (p[2] - cz) * s, p[1], cz + (p[0] - cx) * s + (p[2] - cz) * c)


def rot_x(deg, cy, cz):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return lambda p: (p[0], cy + (p[1] - cy) * c - (p[2] - cz) * s, cz + (p[1] - cy) * s + (p[2] - cz) * c)


def rot_z(deg, cx, cy):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return lambda p: (cx + (p[0] - cx) * c - (p[1] - cy) * s, cy + (p[0] - cx) * s + (p[1] - cy) * c, p[2])


def translate(dx, dy, dz):
    return lambda p: (p[0] + dx, p[1] + dy, p[2] + dz)


def chain(*fns):
    def f(p):
        for fn in fns:
            p = fn(p)
        return p
    return f
