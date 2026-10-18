"""Export only the sorting bins, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import preview_mesh  # noqa: E402
import props  # noqa: E402
import textures  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

textures.make_bins(os.path.join(ASSETS, "textures", "block", "props"))
mesh = props.sorting_bins()
n = model(mesh, "sorting_bins", gui_scale=0.7)
blockstate("sorting_bins", facing_parts("sorting_bins"))

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["sorting_bins"]
base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
for y, row in enumerate(rows):
    if len(row) != icons.N:
        raise SystemExit(f"row {y} is {len(row)}: {row}")
    for x, ch in enumerate(row):
        if ch in icons.FIXED:
            base.putpixel((x, y), (*icons.FIXED[ch], 255))
        elif ch in icons.TINT:
            raise SystemExit("sorting bins are not dyeable")
base.save(os.path.join(folder, "sorting_bins.png"))
preview_mesh.render(mesh, "sorting_bins")
print("sorting_bins", n)
