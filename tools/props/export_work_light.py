"""Export only the site beacon, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import preview_mesh  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

off = model(props.work_light(False), "work_light", gui_scale=0.8)
on = model(props.work_light(True), "work_light_lit", gui_scale=0.8)
parts = []
for flag, name in (("false", "work_light"), ("true", "work_light_lit")):
    parts += facing_parts(name, {"lit": flag})
blockstate("work_light", parts)

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["work_light"]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    if len(row) != icons.N:
        raise SystemExit(f"row {y} is {len(row)}")
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            raise SystemExit("work light is not dyeable")
base.save(os.path.join(folder, "work_light.png"))
lit_item = os.path.join(ASSETS, "models", "item", "work_light_lit.json")
if os.path.exists(lit_item):
    os.remove(lit_item)
preview_mesh.render(props.work_light(True), "work_light")
print("work_light", off, "lit", on)
