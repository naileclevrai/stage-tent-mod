"""Export only the backstage wash station."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

n = model(props.wash_station(), "wash_station", gui_scale=0.45, gui_offset=(0, -0.5, 0))
blockstate("wash_station", facing_parts("wash_station"))

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["wash_station"]
img = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    if len(row) != icons.N:
        raise SystemExit(f"row {y} is {len(row)}")
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            img.putpixel((x, y), (*icons.FIXED[ch], 255))
img.save(os.path.join(folder, "wash_station.png"))
item = {"parent": "minecraft:item/generated", "textures": {"layer0": "stagetents:item/wash_station"}}
with open(os.path.join(ASSETS, "models", "item", "wash_station.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump(item, f, indent=2)
    f.write("\n")
print("wash_station", n, "triangles")
