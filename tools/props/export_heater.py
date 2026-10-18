"""Export only the terrace heater, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

off = model(props.patio_heater(False), "patio_heater", tinted=("paint",), gui_scale=0.20, gui_offset=(0, -1.6, 0))
on = model(props.patio_heater(True), "patio_heater_lit", tinted=("paint",), gui_scale=0.20, gui_offset=(0, -1.6, 0))
parts = []
for flag, name in (("false", "patio_heater"), ("true", "patio_heater_lit")):
    parts += facing_parts(name, {"lit": flag})
blockstate("patio_heater", parts)

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["patio_heater"]
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
base.save(os.path.join(folder, "patio_heater.png"))
dyed.save(os.path.join(folder, "patio_heater_dye.png"))
lit_item = os.path.join(ASSETS, "models", "item", "patio_heater_lit.json")
if os.path.exists(lit_item):
    os.remove(lit_item)
print("patio_heater", off, "lit", on)
