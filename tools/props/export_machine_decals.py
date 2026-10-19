"""Export only the machine decal sheet."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import machine_decals  # noqa: E402
from build import ASSETS  # noqa: E402

print(machine_decals.make_machine_decals(os.path.join(ASSETS, "textures", "block", "props")))
