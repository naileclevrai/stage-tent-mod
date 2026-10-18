"""Export only the draft-beer props, without regenerating the other models."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
import textures  # noqa: E402
from build import ASSETS, blockstate, facing_parts, model  # noqa: E402
from export import PINT_DISPLAY, set_item_display  # noqa: E402
from PIL import Image, ImageDraw

textures.make_co2(os.path.join(ASSETS, "textures", "block", "props"))
textures.make_pint(os.path.join(ASSETS, "textures", "block", "props"))

print("beer_tap", model(props.beer_tap(), "beer_tap", gui_scale=0.7))
print("beer_tap_bar", model(props.beer_tap(True), "beer_tap_bar", gui_scale=0.7))
os.remove(os.path.join(ASSETS, "models", "item", "beer_tap_bar.json"))
print("beer_keg", model(props.beer_keg(), "beer_keg", gui_scale=0.7))
print("co2_cylinder", model(props.co2_cylinder(), "co2_cylinder", gui_scale=0.38, gui_offset=(0, -0.7, 0)))
blockstate("beer_tap", facing_parts("beer_tap", {"bar": "false"}) + facing_parts("beer_tap_bar", {"bar": "true"}))
blockstate("beer_keg", [{"apply": {"model": "stagetents:block/props/beer_keg"}}])
blockstate("co2_cylinder", [{"apply": {"model": "stagetents:block/props/co2_cylinder"}}])

item_tex = os.path.join(ASSETS, "textures", "item")
os.makedirs(item_tex, exist_ok=True)
for name in ("beer_tap", "beer_keg", "co2_cylinder"):
    rows = icons.ICONS[name]
    if len(rows) != icons.N or any(len(row) != icons.N for row in rows):
        raise SystemExit(f"bad icon {name}")
    base = Image.new("RGBA", (icons.N, icons.N), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in icons.FIXED:
                base.putpixel((x, y), (*icons.FIXED[ch], 255))
    base.save(os.path.join(item_tex, name + ".png"))


def _icon(name, draw):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(ImageDraw.Draw(im))
    im.save(os.path.join(item_tex, name + ".png"))
    with open(os.path.join(ASSETS, "models", "item", name + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "stagetents:item/" + name}}, f, indent=2)
        f.write("\n")


def _hose(d):
    d.ellipse([1, 1, 14, 14], outline=(90, 58, 28, 255), width=2)
    d.ellipse([4, 4, 11, 11], outline=(36, 92, 170, 255), width=2)
    d.ellipse([7, 7, 9, 9], fill=(90, 58, 28, 255))


_icon("beer_hose", _hose)
print("beer_mug", model(props.beer_glass(False), "beer_mug", render_type="minecraft:translucent", gui_scale=0.9))
print("beer", model(props.beer_glass(True), "beer", render_type="minecraft:translucent", gui_scale=0.9))
set_item_display(ASSETS, "beer_mug", PINT_DISPLAY)
set_item_display(ASSETS, "beer", PINT_DISPLAY)
print("draft icons written")
