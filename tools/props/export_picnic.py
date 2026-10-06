"""Export only the picnic table, without regenerating the other props."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import props  # noqa: E402
from build import ASSETS, TEX, blockstate, facing_parts, model  # noqa: E402

n = model(props.picnic_table(), "picnic_table", gui_scale=0.22, gui_offset=(-0.4, 0, 0))
blockstate("picnic_table", facing_parts("picnic_table"))
print("picnic_table", n, "triangles")
print("tex", TEX)
print("assets", ASSETS)
