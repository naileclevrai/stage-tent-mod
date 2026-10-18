"""Export only the traffic cone, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import preview_mesh  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

mesh = props.traffic_cone()
n = model(mesh, "traffic_cone", gui_scale=0.9)
blockstate("traffic_cone", facing_parts("traffic_cone"))

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["traffic_cone"]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    if len(row) != icons.N:
        raise SystemExit(f"row {y} is {len(row)}")
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            raise SystemExit("traffic cone is not dyeable")
base.save(os.path.join(folder, "traffic_cone.png"))
preview_mesh.render(mesh, "traffic_cone")
print("traffic_cone", n)
