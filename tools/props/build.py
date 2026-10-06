"""Generates the prop textures, OBJ models, item models and blockstates into the mod resources.

Run from anywhere: python tools/props/build.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import icons  # noqa: E402
import props  # noqa: E402
import textures  # noqa: E402
from export import write_model  # noqa: E402
from mesh import chain  # noqa: E402

# Real-world sizes read small next to a 1.8 block player: seats are scaled up around the block centre, more in
# width than in height so the seat stays at sitting height.
CHAIR_SCALE = (1.45, 1.2)
STOOL_SCALE = (1.2, 1.1)


def scaled(mesh, k):
    kh, kv = k
    return mesh.transformed(lambda p: (0.5 + (p[0] - 0.5) * kh, p[1] * kv, 0.5 + (p[2] - 0.5) * kh))

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "stagetents")
TEX = "stagetents:block/props/"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def tex_map(*names):
    return {n: TEX + n for n in names}


ALL_TEX = None


def model(mesh, name, tinted=(), **kw):
    mats = list(mesh.groups)
    write_model(mesh, name, ASSETS, {m: TEX + m for m in mats}, tinted=tinted, **kw)
    return mesh.tri_count()


def blockstate(name, parts):
    path = os.path.join(ASSETS, "blockstates", name + ".json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"multipart": parts}, f, indent=2)
        f.write("\n")


def facing_parts(model_name, extra_when=None):
    parts = []
    for f, y in FACINGS.items():
        when = {"facing": f}
        if extra_when:
            when.update(extra_when)
        apply = {"model": "stagetents:block/props/" + model_name}
        if y:
            apply["y"] = y
        parts.append({"when": when, "apply": apply})
    return parts


def main():
    names = textures.make_all(os.path.join(ASSETS, "textures", "block", "props"))
    print("textures:", ", ".join(names))
    report = {}
    report["round_table"] = model(props.round_table(), "round_table", tinted=("cloth",))
    report["standing_table"] = model(props.standing_table(True), "standing_table", tinted=("cloth",))
    report["standing_table_bare"] = model(props.standing_table(False), "standing_table_bare")
    report["banquet_chair"] = model(scaled(props.banquet_chair(), CHAIR_SCALE), "banquet_chair", tinted=("cloth",))
    report["folding_chair"] = model(scaled(props.folding_chair(), CHAIR_SCALE), "folding_chair", tinted=("seat",))
    report["bar_stool"] = model(scaled(props.bar_stool(), STOOL_SCALE), "bar_stool", tinted=("cloth",))
    report["bar_counter"] = model(props.bar_counter_base(), "bar_counter", tinted=("cloth",))
    report["bar_counter_end_left"] = model(props.bar_counter_end(-1), "bar_counter_end_left")
    report["bar_counter_end_right"] = model(props.bar_counter_end(1), "bar_counter_end_right")
    report["bleacher"] = model(props.bleacher_base(), "bleacher", tinted=("seat",))
    report["bleacher_end_right"] = model(props.bleacher_end_right(), "bleacher_end_right")
    report["bleacher_end_left"] = model(props.bleacher_end_left(), "bleacher_end_left")
    report["bleacher_aisle"] = model(props.bleacher_aisle(), "bleacher_aisle")
    report["bleacher_support"] = model(props.bleacher_support(), "bleacher_support")
    report["crowd_barrier"] = model(props.crowd_barrier(), "crowd_barrier", gui_scale=0.3)
    report["stanchion"] = model(props.stanchion_base(), "stanchion")
    report["stanchion_rope_1"] = model(props.stanchion_rope(1), "stanchion_rope_1", tinted=("velvet",))
    report["stanchion_rope_2"] = model(props.stanchion_rope(2), "stanchion_rope_2", tinted=("velvet",))
    report["turnstile"] = model(props.turnstile_cabinet(), "turnstile", gui_scale=0.5)
    report["guide_rail"] = model(props.guide_rail(), "guide_rail")
    report["guide_rail_corner"] = model(props.guide_rail_corner(), "guide_rail_corner")
    report["shooting_gallery"] = model(props.shooting_gallery(), "shooting_gallery", gui_scale=0.22, gui_offset=(0, -1.5, 0))
    for level in range(1, 5):
        report[f"stage_deck_{level}"] = model(props.stage_deck(level), f"stage_deck_{level}", tinted=("velour",))
        report[f"stage_deck_{level}_end_left"] = model(props.stage_deck_end(level, -1), f"stage_deck_{level}_end_left", tinted=("velour",))
        report[f"stage_deck_{level}_end_right"] = model(props.stage_deck_end(level, 1), f"stage_deck_{level}_end_right", tinted=("velour",))
    report["stage_stairs"] = model(props.stage_stairs(), "stage_stairs", tinted=("paint",))
    report["stage_ramp"] = model(props.stage_ramp(), "stage_ramp", tinted=("paint",))
    for n, fn in (("stage_stairs", props.stage_stairs_rail), ("stage_ramp", props.stage_ramp_rail)):
        report[n + "_end_left"] = model(fn(-1), n + "_end_left")
        report[n + "_end_right"] = model(fn(1), n + "_end_right")
    report["cyclorama"] = model(props.cyclorama(), "cyclorama", tinted=("muslin",), gui_scale=0.28, gui_offset=(0, -1.2, 0))
    report["cyclorama_end_left"] = model(props.cyclorama_end(-1), "cyclorama_end_left", tinted=("muslin",))
    report["cyclorama_end_right"] = model(props.cyclorama_end(1), "cyclorama_end_right", tinted=("muslin",))
    for n, w, top in (("flight_case", 1, 0.80), ("flight_case_trunk", 2, 0.76), ("flight_case_tall", 1, 1.85),
                      ("flight_case_xl", 2, 1.62)):
        report[n] = model(props.flight_case(w, top), n, tinted=("paint",))
    report["generator"] = model(props.generator(), "generator", tinted=("paint",))
    report["site_toilet"] = model(props.site_toilet(False), "site_toilet", tinted=("paint",), gui_scale=0.4, gui_offset=(0, -0.4, 0))
    report["site_toilet_open"] = model(props.site_toilet(True), "site_toilet_open", tinted=("paint",))
    report["site_fence"] = model(props.site_fence(), "site_fence", tinted=("scrim", "hem"))
    report["oriflamme"] = model(props.oriflamme(), "oriflamme", tinted=("cloth", "hem"))

    # Models of the earlier fence ends are gone.
    for n in ("site_fence_end_left", "site_fence_end_right", "site_fence_joint", "site_fence_end"):
        for ext in (".obj", ".mtl", ".json"):
            p = os.path.join(ASSETS, "models", "block", "props", n + ext)
            if os.path.exists(p):
                os.remove(p)

    # The connecting parts are not items.
    extra_items = ["guide_rail_corner", "site_toilet_open", "site_fence_end_left", "site_fence_end_right", "site_fence_joint", "site_fence_end",
                   "cyclorama_end_left", "cyclorama_end_right", "stage_stairs_end_left", "stage_stairs_end_right",
                   "stage_ramp_end_left", "stage_ramp_end_right"]
    for level in range(1, 5):
        extra_items += [f"stage_deck_{level}_end_left", f"stage_deck_{level}_end_right"]
        if level < 4:
            extra_items.append(f"stage_deck_{level}")
    for n in ("bar_counter_end_left", "bar_counter_end_right", "bleacher_end_right", "bleacher_end_left", "stanchion_rope_1",
              "stanchion_rope_2", "standing_table_bare", *extra_items):
        p = os.path.join(ASSETS, "models", "item", n + ".json")
        if os.path.exists(p):
            os.remove(p)

    one = lambda m: [{"apply": {"model": "stagetents:block/props/" + m}}]
    blockstate("round_table", one("round_table"))
    blockstate("standing_table", [
        {"when": {"cover": "true"}, "apply": {"model": "stagetents:block/props/standing_table"}},
        {"when": {"cover": "false"}, "apply": {"model": "stagetents:block/props/standing_table_bare"}}])
    blockstate("banquet_chair", facing_parts("banquet_chair"))
    blockstate("folding_chair", facing_parts("folding_chair"))
    blockstate("bar_stool", one("bar_stool"))
    blockstate("bar_counter", facing_parts("bar_counter") + facing_parts("bar_counter_end_left", {"left": "false"})
               + facing_parts("bar_counter_end_right", {"right": "false"}))
    blockstate("bleacher", facing_parts("bleacher") + facing_parts("bleacher_end_right", {"right": "false"})
               + facing_parts("bleacher_end_left", {"left": "false"}))
    blockstate("bleacher_aisle", facing_parts("bleacher_aisle") + facing_parts("bleacher_end_right", {"right": "false"})
               + facing_parts("bleacher_end_left", {"left": "false"}))
    blockstate("bleacher_support", facing_parts("bleacher_support"))
    blockstate("crowd_barrier", facing_parts("crowd_barrier"))
    blockstate("stanchion", one("stanchion") + [
        {"when": {"east": "1"}, "apply": {"model": "stagetents:block/props/stanchion_rope_1"}},
        {"when": {"east": "2"}, "apply": {"model": "stagetents:block/props/stanchion_rope_2"}},
        {"when": {"south": "1"}, "apply": {"model": "stagetents:block/props/stanchion_rope_1", "y": 90}},
        {"when": {"south": "2"}, "apply": {"model": "stagetents:block/props/stanchion_rope_2", "y": 90}}])
    blockstate("shooting_gallery", facing_parts("shooting_gallery"))
    blockstate("turnstile", facing_parts("turnstile"))
    blockstate("guide_rail", facing_parts("guide_rail", {"corner": "false"})
               + facing_parts("guide_rail_corner", {"corner": "true"}))
    deck_parts = []
    for level in range(1, 5):
        deck_parts += facing_parts(f"stage_deck_{level}", {"height": str(level)})
        deck_parts += facing_parts(f"stage_deck_{level}_end_left", {"height": str(level), "left": "false"})
        deck_parts += facing_parts(f"stage_deck_{level}_end_right", {"height": str(level), "right": "false"})
    blockstate("stage_deck", deck_parts)
    for n in ("stage_stairs", "stage_ramp"):
        blockstate(n, facing_parts(n) + facing_parts(n + "_end_left", {"left": "false"})
                   + facing_parts(n + "_end_right", {"right": "false"}))
    blockstate("cyclorama", facing_parts("cyclorama")
               + facing_parts("cyclorama_end_left", {"left": "false"})
               + facing_parts("cyclorama_end_right", {"right": "false"}))
    for n in ("flight_case", "flight_case_trunk", "flight_case_tall", "flight_case_xl"):
        blockstate(n, facing_parts(n))
    blockstate("generator", facing_parts("generator"))
    blockstate("site_toilet", facing_parts("site_toilet", {"open": "false"}) + facing_parts("site_toilet_open", {"open": "true"}))
    blockstate("site_fence", facing_parts("site_fence"))
    blockstate("oriflamme", facing_parts("oriflamme"))
    # The inventory icon is the full-height deck; the height variants are not items.
    deck_item = os.path.join(ASSETS, "models", "item", "stage_deck_4.json")
    deck_icon = os.path.join(ASSETS, "models", "item", "stage_deck.json")
    if os.path.exists(deck_item):
        os.replace(deck_item, deck_icon)
    # Stage and site props show a pixel-art icon in the inventory instead of the 3D model.
    print("icons:", ", ".join(icons.write_all(ASSETS)))
    # Invisible collision cells of multi-block props.
    forms = ["full", "panel_x", "panel_z", "wall_x", "wall_z", "pole"]
    with open(os.path.join(ASSETS, "blockstates", "prop_part.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"variants": {f"form={form}": {"model": "stagetents:block/prop_part"} for form in forms}}, f, indent=2)
        f.write("\n")
    with open(os.path.join(ASSETS, "models", "block", "prop_part.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"textures": {"particle": TEX + "red"}}, f, indent=2)
        f.write("\n")

    # Old box-built furniture models are replaced by the OBJ ones.
    for n in ("round_table", "standing_table", "banquet_chair", "bar_counter", "bleacher"):
        p = os.path.join(ASSETS, "models", "block", n + ".json")
        if os.path.exists(p):
            os.remove(p)

    for k, v in report.items():
        print(f"{k:24s} {v:6d} triangles")


if __name__ == "__main__":
    main()
