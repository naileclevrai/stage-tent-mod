"""Export the water cannon: the skid, one barrel mesh per tilt, and the item."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import props  # noqa: E402
from build import ASSETS, model, blockstate, water_cannon_states  # noqa: E402

ELEVATIONS = list(range(20, 75, 5))  # 20° to 70° above the horizontal

n = model(props.water_cannon_base(), "water_cannon_base", tinted=("paint",))
print("base", n)
n = model(props.water_cannon(), "water_cannon", tinted=("paint",), gui_scale=0.28, gui_offset=(-0.35, -0.15, 0))
print("item", n)
for i, elev in enumerate(ELEVATIONS):
    t = model(props.water_cannon_aimed(elev), "water_cannon_barrel_%d" % i, tinted=("paint",))
    print("barrel", elev, t)
blockstate("water_cannon", water_cannon_states())
item_dir = os.path.join(ASSETS, "models", "item")
for name in ["water_cannon_base"] + ["water_cannon_barrel_%d" % i for i in range(len(ELEVATIONS))]:
    p = os.path.join(item_dir, name + ".json")
    if os.path.exists(p):
        os.remove(p)
print("states", len(water_cannon_states()))
