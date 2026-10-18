"""Export only the DJ cover foot, without regenerating the other props."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import preview_mesh  # noqa: E402
import props  # noqa: E402
from build import model  # noqa: E402

mesh = props.dj_foot()
n = model(mesh, "dj_foot", gui_scale=1.0)
preview_mesh.render(mesh, "dj_foot")
print("dj_foot", n)
