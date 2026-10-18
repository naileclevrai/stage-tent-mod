"""Export only the festoon post, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, model  # noqa: E402
from PIL import Image

n = model(props.festoon(), "festoon", gui_scale=0.32, gui_offset=(0, -1.2, 0))
blockstate("festoon", [{"apply": {"model": "stagetents:block/props/festoon"}}])

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["festoon"]
if len(rows) != icons.N:
    raise SystemExit(f"{len(rows)} rows")
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
base.save(os.path.join(folder, "festoon.png"))
dyed.save(os.path.join(folder, "festoon_dye.png"))
print("festoon", n, "triangles")
