"""Generate the DJ arch inventory silhouette and its small configuration plate texture."""
from pathlib import Path
from PIL import Image, ImageDraw
import math

root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/stagetents/textures"

icon = Image.new("RGBA", (32, 32))
d = ImageDraw.Draw(icon)
curve = []
inner = []
for i in range(33):
    angle = math.pi * i / 32
    curve.append((round(16 + 14 * math.cos(angle)), round(29 - 26 * math.sin(angle))))
    inner.append((round(16 + 11 * math.cos(angle)), round(29 - 21 * math.sin(angle))))
d.polygon(curve + [(30, 30), (2, 30)], fill=(165, 205, 220, 72))
d.line(curve, fill=(16, 21, 27), width=2, joint="curve")
d.line(inner, fill=(16, 21, 27), width=2, joint="curve")
for i in (4, 10, 16, 22, 28):
    d.line((curve[i], inner[i]), fill=(108, 122, 134))
d.rectangle((0, 28, 5, 31), fill=(70, 80, 90))
d.rectangle((27, 28, 31, 31), fill=(70, 80, 90))
icon.save(root / "item/dj_arch.png")

plate = Image.new("RGBA", (16, 16), (29, 35, 42, 255))
p = ImageDraw.Draw(plate)
p.rectangle((0, 0, 15, 15), outline=(95, 107, 116))
p.line((2, 3, 13, 3), fill=(78, 92, 103), width=1)
p.arc((2, 4, 13, 14), 180, 360, fill=(130, 151, 163), width=2)
p.rectangle((3, 12, 4, 13), fill=(180, 193, 201))
p.rectangle((11, 12, 12, 13), fill=(180, 193, 201))
p.point((8, 8), fill=(105, 180, 210))
plate.save(root / "block/dj_arch_plate.png")
