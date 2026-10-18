"""Export only the terrace parasol, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

n = model(props.parasol(), "parasol", tinted=("cloth",), gui_scale=0.22, gui_offset=(0, -1.4, 0))
blockstate("parasol", facing_parts("parasol"))
model(props.picnic_table(), "picnic_table", gui_scale=0.22, gui_offset=(-0.4, 0, 0))
model(props.picnic_table_parasol(), "picnic_table_parasol", tinted=("cloth",), gui_scale=0.16, gui_offset=(-0.4, -0.6, 0))
picnic = []
for flag, name in (("false", "picnic_table"), ("true", "picnic_table_parasol")):
    picnic += facing_parts(name, {"parasol": flag})
blockstate("picnic_table", picnic)

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["parasol"]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
dyed = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            v = icons.TINT[ch]
            dyed.putpixel((x, y), (v, v, v, 255))
base.save(os.path.join(folder, "parasol.png"))
dyed.save(os.path.join(folder, "parasol_dye.png"))
extra = os.path.join(ASSETS, "models", "item", "picnic_table_parasol.json")
if os.path.exists(extra):
    os.remove(extra)
print("parasol", n, "triangles")
