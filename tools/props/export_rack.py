"""Export only the single-block distro rack (block model + blockstate).

Does not rebuild other props and does not rewrite inventory icons.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import props  # noqa: E402
from build import ASSETS, model, blockstate, facing_parts  # noqa: E402

mesh = props.power_rack()
n = model(mesh, "power_rack", tinted=("paint",), gui_scale=0.42, gui_offset=(0, -0.12, 0))
blockstate("power_rack", facing_parts("power_rack"))
xs, ys, zs = [], [], []
for tris in mesh.groups.values():
    for t in tris:
        for p, _n, _uv in t:
            xs.append(p[0]); ys.append(p[1]); zs.append(p[2])
print("power_rack", n, "bbox",
      round(min(xs), 3), round(min(ys), 3), round(min(zs), 3),
      round(max(xs), 3), round(max(ys), 3), round(max(zs), 3))
