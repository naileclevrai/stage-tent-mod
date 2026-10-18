"""Export only the menu board, without regenerating the other props."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import preview_mesh  # noqa: E402
import props  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from PIL import Image

mesh = props.menu_board()
n = model(mesh, "menu_board", tinted=("paint",), gui_scale=0.72, gui_offset=(0, -0.05, 0))
blockstate("menu_board", facing_parts("menu_board"))

folder = os.path.join(ASSETS, "textures", "item")
os.makedirs(folder, exist_ok=True)
rows = icons.ICONS["menu_board"]
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
base.save(os.path.join(folder, "menu_board.png"))
dyed.save(os.path.join(folder, "menu_board_dye.png"))
preview_mesh.render(mesh, "menu_board", tint={"paint": (150, 96, 54)})
xs, ys, zs = [], [], []
for tris in mesh.groups.values():
    for tri in tris:
        for p, _, _ in tri:
            xs.append(p[0])
            ys.append(p[1])
            zs.append(p[2])
print("menu_board", n, "bounds", round(min(xs), 3), round(min(ys), 3), round(min(zs), 3),
      round(max(xs), 3), round(max(ys), 3), round(max(zs), 3))
