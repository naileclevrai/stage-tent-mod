"""Export only the light tower: trailer body, mast section, yoke, lamp, blockstate and pixel icon."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, model, blockstate, facing_parts  # noqa: E402
from PIL import Image  # noqa: E402

body = props.light_tower()
print("body", model(body, "light_tower", tinted=("paint",), gui_scale=0.34, gui_offset=(-0.35, -0.2, 0)))
for name, mesh in (
    ("light_tower_section", props.light_tower_section()),
    ("light_tower_yoke", props.light_tower_yoke()),
    ("light_tower_lamp", props.light_tower_lamp()),
):
    print(name, model(mesh, name))
    item = os.path.join(ASSETS, "models", "item", name + ".json")
    if os.path.exists(item):
        os.remove(item)

blockstate("light_tower", facing_parts("light_tower"))

rows = icons.ICONS["light_tower"]
assert len(rows) == icons.N and all(len(r) == icons.N for r in rows), [len(r) for r in rows]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
dyed = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            v = icons.TINT[ch]
            dyed.putpixel((x, y), (v, v, v, 255))
base.save(os.path.join(ASSETS, "textures", "item", "light_tower.png"))
dyed.save(os.path.join(ASSETS, "textures", "item", "light_tower_dye.png"))
item = {
    "parent": "item/generated",
    "textures": {
        "layer0": "stagetents:item/light_tower",
        "layer1": "stagetents:item/light_tower_dye",
    },
}
with open(os.path.join(ASSETS, "models", "item", "light_tower.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump(item, f, indent=2)
    f.write("\n")

xs, ys, zs = [], [], []
for tris in body.groups.values():
    for t in tris:
        for p, _n, _uv in t:
            xs.append(p[0])
            ys.append(p[1])
            zs.append(p[2])
print("bbox", round(min(xs), 3), round(min(ys), 3), round(min(zs), 3),
      round(max(xs), 3), round(max(ys), 3), round(max(zs), 3))
