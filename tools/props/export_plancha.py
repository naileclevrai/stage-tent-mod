"""Export only the plancha, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
import textures  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

textures.make_plancha(os.path.join(ASSETS, "textures", "block", "props"))
off = model(props.plancha(False), "plancha", tinted=("paint",), gui_scale=0.55, gui_offset=(0, -0.15, 0))
on = model(props.plancha(True), "plancha_lit", tinted=("paint",), gui_scale=0.55, gui_offset=(0, -0.15, 0))
parts = []
for flag, name in (("false", "plancha"), ("true", "plancha_lit")):
    parts += facing_parts(name, {"lit": flag})
blockstate("plancha", parts)

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["plancha"]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
dyed = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    if len(row) != icons.N:
        raise SystemExit(f"row {y} is {len(row)}")
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            v = icons.TINT[ch]
            dyed.putpixel((x, y), (v, v, v, 255))
base.save(os.path.join(folder, "plancha.png"))
dyed.save(os.path.join(folder, "plancha_dye.png"))
lit_item = os.path.join(ASSETS, "models", "item", "plancha_lit.json")
if os.path.exists(lit_item):
    os.remove(lit_item)


def bounds(mesh):
    xs, ys, zs = [], [], []
    for tris in mesh.groups.values():
        for tri in tris:
            for p, _, _ in tri:
                xs.append(p[0])
                ys.append(p[1])
                zs.append(p[2])
    return min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)


print("plancha", off, "lit", on, "bounds", bounds(props.plancha(True)))
