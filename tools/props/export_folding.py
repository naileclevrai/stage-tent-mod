"""Export only the folding table (block model + blockstate + pixel icon)."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
from build import ASSETS, model, blockstate, facing_parts  # noqa: E402

mesh = props.folding_table()
n = model(mesh, "folding_table", gui_scale=0.26, gui_offset=(-0.7, -0.15, 0))
blockstate("folding_table", facing_parts("folding_table"))
rows = icons.ICONS["folding_table"]
assert len(rows) == icons.N and all(len(r) == icons.N for r in rows)
# Only this icon. write_all would repaint every other menu icon.
from PIL import Image  # noqa: E402
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
base.save(os.path.join(ASSETS, "textures", "item", "folding_table.png"))
item = {
    "parent": "item/generated",
    "textures": {"layer0": "stagetents:item/folding_table"},
}
with open(os.path.join(ASSETS, "models", "item", "folding_table.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump(item, f, indent=2)
    f.write("\n")
xs, ys, zs = [], [], []
for tris in mesh.groups.values():
    for t in tris:
        for p, _n, _uv in t:
            xs.append(p[0]); ys.append(p[1]); zs.append(p[2])
print("folding_table", n, "bbox",
      round(min(xs), 3), round(min(ys), 3), round(min(zs), 3),
      round(max(xs), 3), round(max(ys), 3), round(max(zs), 3))
