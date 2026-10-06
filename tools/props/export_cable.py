"""Export only the cable ramp (closed and open, one mesh per channel count).

Does not rebuild other props. The inventory icon stays a flat generated item.
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(__file__))
import props  # noqa: E402
from build import ASSETS, model, blockstate, facing_parts  # noqa: E402


def _clamp(v):
    return max(0, min(255, int(v)))


def _font(size):
    for path in (r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\arial.ttf"):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def _plastic(size, color_at):
    """Flat colour with an optional soft shade. No noise."""
    w, h = size
    im = Image.new("RGBA", size)
    px = im.load()
    for y in range(h):
        for x in range(w):
            px[x, y] = color_at(x, y) + (255,)
    return im


def _mold(im, text, font, light, dark, reach=2):
    """Letters fused into the plastic: same fill as the surface, only a light rim and a dark rim."""
    w, h = im.size
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(((w - tw) / 2 - bbox[0], (h - th) / 2 - bbox[1]), text, font=font, fill=255)
    mask = mask.point(lambda p: 255 if p >= 128 else 0)
    m = mask.load()
    px = im.load()

    def on(x, y):
        return 0 <= x < w and 0 <= y < h and m[x, y] >= 128

    for y in range(h):
        for x in range(w):
            if not on(x, y):
                continue
            lit = not on(x - reach, y - reach)
            shade = not on(x + reach, y + reach)
            if lit and not shade:
                px[x, y] = light + (255,)
            elif shade and not lit:
                px[x, y] = dark + (255,)
    return im


def write_plates():
    """Smooth plastic. Yellow lid carries a faint molded CABLE; the black wings carry a smaller one."""
    folder = os.path.join(ASSETS, "textures", "block", "props")
    os.makedirs(folder, exist_ok=True)

    def yellow_at(x, y):
        # One soft crown across the lid. Constant along x so the repeat along the ramp stays seamless.
        t = y / 127
        crown = math.exp(-((t - 0.36) ** 2) / 0.10)
        edge = abs(t - 0.5) * 2
        return (
            _clamp(242 + 10 * crown - 8 * edge),
            _clamp(196 + 8 * crown - 10 * edge),
            _clamp(32 + 4 * crown - 6 * edge),
        )

    yellow = _plastic((128, 128), yellow_at)
    # Same family as the plastic. A thin rim only, so the word fades as soon as you step back.
    _mold(yellow, "CABLE", _font(22), (252, 210, 46), (228, 180, 16), reach=1)
    yellow.save(os.path.join(folder, "yellow_plate.png"))

    black = _plastic((64, 64), lambda x, y: (20, 20, 22))
    _mold(black, "CABLE", _font(11), (26, 26, 28), (15, 15, 17), reach=1)
    black.save(os.path.join(folder, "black_plate.png"))


write_plates()

parts = []
report = []
for n in range(1, 6):
    for open_lid, suffix in ((False, ""), (True, "_open")):
        name = f"cable_ramp_{n}{suffix}"
        mesh = props.cable_ramp(n, open_lid)
        tris = model(mesh, name, gui_scale=0.7, gui_offset=(0, 0.15, 0))
        xs, ys, zs = [], [], []
        for group in mesh.groups.values():
            for t in group:
                for p, _n, _uv in t:
                    xs.append(p[0])
                    ys.append(p[1])
                    zs.append(p[2])
        span = 0.0
        for group in mesh.groups.values():
            for t in group:
                us = [v[2][0] for v in t]
                vs = [v[2][1] for v in t]
                span = max(span, max(us) - min(us), max(vs) - min(vs))
        report.append((name, tris, min(xs), min(ys), min(zs), max(xs), max(ys), max(zs), span))
        when = {"channels": str(n), "open": "true" if open_lid else "false"}
        parts += facing_parts(name, when)

blockstate("cable_ramp", parts)
# model() also writes a 3D item json per variant. Only the flat icon is an item.
for n in range(1, 6):
    for suffix in ("", "_open"):
        extra = os.path.join(ASSETS, "models", "item", f"cable_ramp_{n}{suffix}.json")
        if os.path.exists(extra):
            os.remove(extra)
item = {
    "parent": "item/generated",
    "textures": {"layer0": "stagetents:item/cable_ramp"},
}
with open(os.path.join(ASSETS, "models", "item", "cable_ramp.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump(item, f, indent=2)
    f.write("\n")
for name, tris, x0, y0, z0, x1, y1, z1, span in report:
    print(f"{name:22} {tris:5d}  bbox {x0:.3f} {y0:.3f} {z0:.3f}  {x1:.3f} {y1:.3f} {z1:.3f}  uv {span:.3f}  w {x1 - x0:.3f}")
