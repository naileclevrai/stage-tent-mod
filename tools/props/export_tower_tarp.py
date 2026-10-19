"""Export only the control tower PVC texture."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import textures  # noqa: E402
from build import ASSETS  # noqa: E402

textures.make_tower_tarp(os.path.join(ASSETS, "textures", "block", "props"))
print("tarp_pvc")
