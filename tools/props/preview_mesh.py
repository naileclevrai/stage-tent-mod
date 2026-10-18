"""Orthographic mockups of a prop mesh: front, side and three-quarter.

Front looks along +Z at the -Z face. Images land in tools/props/preview/.
"""
import math
import os

from PIL import Image, ImageDraw

COLORS = {
    "paint": (214, 206, 196),
    "black": (36, 36, 40),
    "white": (242, 240, 232),
    "chrome": (214, 218, 224),
    "steel": (196, 200, 206),
    "metal": (176, 180, 186),
    "galvanised": (158, 164, 158),
    "rubber": (28, 28, 32),
    "wood": (186, 140, 86),
    "wood_dark": (118, 78, 48),
    "red": (176, 36, 34),
    "orange": (226, 112, 28),
    "yellow": (232, 186, 36),
    "green": (46, 150, 72),
    "grey": (92, 96, 102),
    "navy": (28, 40, 82),
    "gold": (206, 164, 72),
    "bulb": (255, 214, 120),
    "blue": (40, 96, 190),
    "velour": (140, 36, 48),
    "white_band": (236, 238, 242),
    "bin_plastic": (78, 168, 64),
    "bin_divers": (58, 140, 70),
    "bin_lid_plastic": (236, 196, 36),
    "bin_lid_divers": (28, 30, 34),
}


def render(mesh, name, folder=None, tint=None):
    """Write {name}_front.png, {name}_side.png, {name}_three.png. Returns the three paths."""
    folder = folder or os.path.join(os.path.dirname(__file__), "preview")
    os.makedirs(folder, exist_ok=True)
    tris = []
    for mat, group in mesh.groups.items():
        rgb = COLORS.get(mat, (180, 80, 180))
        if tint and mat in tint:
            rgb = tint[mat]
        for (a, na, _), (b, nb, _), (c, nc, _) in group:
            n = (na[0] + nb[0] + nc[0], na[1] + nb[1] + nc[1], na[2] + nb[2] + nc[2])
            tris.append((a, b, c, _unit(n), rgb))
    views = {
        "front": _view_front,
        "side": _view_side,
        "three": _view_three,
    }
    paths = []
    for label, project in views.items():
        path = os.path.join(folder, f"{name}_{label}.png")
        _draw(tris, project, path, label)
        paths.append(path)
    return paths


def _unit(v):
    l = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return (v[0] / l, v[1] / l, v[2] / l) if l > 1e-8 else (0.0, 1.0, 0.0)


def _view_front(p):
    """Camera on -Z, X to the right, Y up. Depth grows toward the camera."""
    return (p[0], p[1], -p[2])


def _view_side(p):
    """Camera on +X, looking toward -X. +Z (the wall) is to the right."""
    return (p[2], p[1], p[0])


def _view_three(p):
    """Front (-Z), a little of the left side, and the top."""
    x, y, z = p[0] - 0.5, p[1] - 0.45, 0.5 - p[2]
    yaw = math.radians(34)
    c, s = math.cos(yaw), math.sin(yaw)
    x, z = c * x + s * z, -s * x + c * z
    pitch = math.radians(24)
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = cp * y - sp * z, sp * y + cp * z
    return (x + 0.5, y + 0.45, z)


def _draw(tris, project, path, label):
    W, H = 640, 720
    projected = []
    for a, b, c, n, rgb in tris:
        pa, pb, pc = project(a), project(b), project(c)
        projected.append((pa, pb, pc, n, rgb))
    xs, ys = [], []
    for pa, pb, pc, _, _ in projected:
        for p in (pa, pb, pc):
            xs.append(p[0])
            ys.append(p[1])
    # Keep the unit block in frame too, so a small prop does not fill the picture.
    for x in (0.0, 1.0):
        for y in (0.0, 1.0):
            q = project((x, y, 0.5))
            xs.append(q[0])
            ys.append(q[1])
    minx, maxx = min(xs), max(xs)
    miny, maxy = min(ys), max(ys)
    span = max(maxx - minx, maxy - miny, 0.2)
    scale = 560 / span
    cx = (minx + maxx) / 2
    cy = (miny + maxy) / 2
    ox, oy = W / 2 - cx * scale, H / 2 + cy * scale

    def pix(p):
        return (p[0] * scale + ox, oy - p[1] * scale)

    light = _unit((0.35, 0.75, 0.55))
    order = sorted(projected, key=lambda t: (t[0][2] + t[1][2] + t[2][2]) / 3)
    im = Image.new("RGB", (W, H), (248, 246, 242))
    dr = ImageDraw.Draw(im)
    _block_hint(dr, project, pix)
    for pa, pb, pc, n, rgb in order:
        lam = 0.28 + 0.72 * max(0.0, n[0] * light[0] + n[1] * light[1] + n[2] * light[2])
        col = tuple(max(0, min(255, int(ch * lam))) for ch in rgb)
        dr.polygon([pix(pa), pix(pb), pix(pc)], fill=col)
    dr.text((12, 12), label, fill=(40, 40, 44))
    im.save(path)


def _block_hint(dr, project, pix):
    """Faint unit-block edges and the floor, so scale and the wall plane stay readable."""
    corners = [(x, y, z) for x in (0, 1) for y in (0, 1) for z in (0, 1)]
    edges = []
    for i, a in enumerate(corners):
        for b in corners[i + 1:]:
            if sum(abs(a[k] - b[k]) for k in range(3)) == 1:
                edges.append((a, b))
    col = (214, 210, 204)
    for a, b in edges:
        dr.line([pix(project(a)), pix(project(b))], fill=col, width=1)
    floor = [project((x, 0, z)) for x, z in ((0, 0), (1, 0), (1, 1), (0, 1))]
    dr.polygon([pix(p) for p in floor], outline=(190, 186, 178))
