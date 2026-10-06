"""Export the power distro block, item and blockstate. Does not rebuild the other props."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import props  # noqa: E402
from build import model, blockstate, facing_parts  # noqa: E402

n = model(props.power_distro(), "power_distro", gui_scale=0.42, gui_offset=(0, -0.15, 0))
blockstate("power_distro", facing_parts("power_distro"))
print("power_distro", n)
