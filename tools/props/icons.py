"""Pixel-art inventory icons (16x16) for the stage and site props, in the flat front-view style of the tent items.

Layer 0 holds the fixed colours; layer 1 holds the dyed part in greys, tinted in game with the block's colour.
"""
import json
import os

from PIL import Image

N = 16
FIXED = {
    "K": (38, 38, 42), "k": (68, 68, 74), "g": (104, 108, 114), "s": (154, 158, 166), "S": (198, 200, 206),
    "C": (234, 236, 240), "W": (242, 240, 234), "w": (206, 204, 198), "B": (120, 86, 58), "b": (90, 64, 44),
    "R": (194, 46, 38), "G": (80, 200, 110), "L": (58, 100, 210), "Y": (242, 198, 46),
}
TINT = {"1": 246, "2": 212, "3": 172, "4": 128}

ICONS = {
    "stage_deck": [
        "................",
        "................",
        "................",
        "................",
        "..BBbBBBBbBBBB..",
        ".BBBBBBbBBBBbBB.",
        "SSSSSSSSSSSSSSSS",
        "1111111111111111",
        "1213121312131213",
        "1213121312131213",
        "1213121312131213",
        "1213121312131213",
        "1213121312131213",
        "1213121312131213",
        "1213121312131213",
        "2324232423242324",
    ],
    "cyclorama": [
        "KKKKKKKKKKKKKKKK",
        "K.k..k..k..k..kK",
        "K11111111111111K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K11111111111112K",
        "K22222222222223K",
        "K33333333333334K",
        "K44444444444444K",
        "KKK..........KKK",
    ],
    "flight_case": [
        "................",
        "................",
        "................",
        "..CSSSSSSSSSSC..",
        "..SKKKKKKKKKKS..",
        "..S1111111111S..",
        "..SKKKKKKKKKKS..",
        "..SSCSSSSSSCSS..",
        "..SKCKKKKKKCKS..",
        "..SKKKKKKKKKKS..",
        "..SKKKWWWWKKKS..",
        "..SKKKwwwwKKKS..",
        "..SKKKKKKKKKKS..",
        "..CSSSSSSSSSSC..",
        "...KK......KK...",
        "...kk......kk...",
    ],
    "flight_case_trunk": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "CSSSSSSSSSSSSSSC",
        "SKKKKKKKKKKKKKKS",
        "S11111111111111S",
        "SSSCSSCSSCSSCSSS",
        "SKKCKKCKKCKKCKKS",
        "SKKKKKWWWWKKKKKS",
        "SKKKKKwwwwKKKKKS",
        "SKKKKKKKKKKKKKKS",
        "CSSSSSSSSSSSSSSC",
        ".KK..........KK.",
        ".kk..........kk.",
    ],
    "flight_case_tall": [
        "....CSSSSSSC....",
        "....SKKKKKKS....",
        "....S111111S....",
        "....SKKKKKKS....",
        "....SSCSSCSS....",
        "....SKCKKCKS....",
        "....SKKKKKKS....",
        "....SKWWWWKS....",
        "....SKwwwwKS....",
        "....SKKKKKKS....",
        "....SKKKKKKS....",
        "....SSCSSCSS....",
        "....SKCKKCKS....",
        "....CSSSSSSC....",
        ".....KK..KK.....",
        ".....kk..kk.....",
    ],
    "flight_case_xl": [
        "CSSSSSSSSSSSSSSC",
        "SKKKKKKKKKKKKKKS",
        "S11111111111111S",
        "SKKKKKKKKKKKKKKS",
        "SSSCSSCSSCSSCSSS",
        "SKKCKKCKKCKKCKKS",
        "SKKKKKKKKKKKKKKS",
        "SKKKKKWWWWKKKKKS",
        "SKKKKKwwwwKKKKKS",
        "SKKKKKKKKKKKKKKS",
        "SKKKKKKKKKKKKKKS",
        "SSSCSSCSSCSSCSSS",
        "SKKCKKCKKCKKCKKS",
        "CSSSSSSSSSSSSSSC",
        ".KK..........KK.",
        ".kk..........kk.",
    ],
    "generator": [
        "................",
        "................",
        "..KKK...........",
        "...s......CC....",
        "...s.....C..C...",
        ".11111111111111.",
        "3222242222422223",
        "3gggg4gggg4KKKK3",
        "32222422224KGGK3",
        "3gggg4gggg4KkkK3",
        "32222422224KKKK3",
        "3gggg4gggg42222R",
        "322C2422C24R2LL3",
        "3333343333433333",
        "KKKKKKKKKKKKKKKK",
        "KkkkKKKKKKKKkkkK",
    ],
    "site_toilet": [
        ".....WWWWWW...K.",
        "....WWWWWWWW..K.",
        "..wwwwwwwwwwwwK.",
        "...3222222223.K.",
        "...3211111123.K.",
        "...3212WW2123.K.",
        "...3212112123.K.",
        "...3212112123KK.",
        "...3212112R23.K.",
        "...3212112K23.K.",
        "...3212112123.K.",
        "...3212112123...",
        "...3212112123...",
        "...3211111123...",
        "...gggggggggg...",
        "..KKKKKKKKKKKK..",
    ],
    "site_fence": [
        "................",
        "................",
        ".SSSSSSSSSSSSSS.",
        "S11C111C111C111S",
        "S11111211111121S",
        "S12111111211111S",
        "S11112111111211S",
        "S11111111121111S",
        "S12111211111111S",
        "S11111111211121S",
        "S11121111111111S",
        "S11111121111211S",
        "S11C111C111C111S",
        ".SSSSSSSSSSSSSS.",
        "KKKK........KKKK",
        "KKKKK......KKKKK",
    ],
    "oriflamme": [
        "........333.....",
        ".......31113....",
        ".......311113...",
        ".......3111123..",
        ".......3111122..",
        ".......3111122..",
        ".......3111122..",
        ".......3111122..",
        ".......311112...",
        ".......31112....",
        ".......3112.....",
        ".......K.k......",
        ".......Kk.......",
        ".......K........",
        "......kKk.......",
        "....KK.K.KK.....",
    ],
}


def _stairs():
    g = [["."] * N for _ in range(N)]
    for i in range(4):
        x0, top = 1 + 4 * i, 12 - 3 * i
        for x in range(x0, min(N, x0 + 4)):
            g[top][x] = "1" if x - x0 < 2 else "S"
            for y in range(top + 1, 15):
                g[y][x] = "k" if x == x0 else "s"
    for x in (1, 14):
        g[15][x] = "K"
    for x in range(1, 13):
        y = round(8 - 0.75 * (x - 1))
        if 0 <= y < N:
            g[y][x] = "C"
    for y in range(8, 12):
        g[y][2] = "S"
    for y in range(1, 6):
        g[y][12] = "S"
    return ["".join(r) for r in g]


def _ramp():
    g = [["."] * N for _ in range(N)]
    for x in range(1, N):
        t = round(14 - (x - 1) * 10 / 14)
        g[t][x] = "S"
        if t - 1 >= 0:
            g[t - 1][x] = "1"
        for y in range(t + 1, 15):
            g[y][x] = "s"
        if x % 4 == 2:
            g[t][x] = "K"
    for x in range(2, 13):
        y = round(14 - (x - 1) * 10 / 14) - 6
        if 0 <= y < N:
            g[y][x] = "C"
    for (x, y0) in ((2, 8), (12, 1)):
        t = round(14 - (x - 1) * 10 / 14)
        for y in range(y0 + 1, t - 1):
            g[y][x] = "S"
    return ["".join(r) for r in g]


ICONS["stage_stairs"] = _stairs()
ICONS["stage_ramp"] = _ramp()
ICONS["frise"] = [
    "................",
    "..kkkkkkkkkkkk..",
    "..kSSSSSSSSSSk..",
    "..S1212121212S..",
    ".S121212121212S.",
    "S12121212121212S",
    "1212121212121212",
    "2121212121212121",
    "1212121212121212",
    ".2..2..2..2..2..",
    "2..2..2..2..2..2",
    "................",
    "................",
    "................",
    "................",
    "................",
]
ICONS["pendrillon"] = [
    "......SSSS......",
    ".....S1111S.....",
    "....S121212S....",
    "...S12121212S...",
    "..S1212121212S..",
    "..212121212121..",
    "..121212121212..",
    "..212121212121..",
    "..121212121212..",
    "..212121212121..",
    "..121212121212..",
    "..212121212121..",
    "..121212121212..",
    "..212121212121..",
    "..323232323232..",
    "..434343434343..",
]


def write_all(assets):
    folder = os.path.join(assets, "textures", "item")
    os.makedirs(folder, exist_ok=True)
    for name, rows in ICONS.items():
        assert len(rows) == N and all(len(r) == N for r in rows), name
        base = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        dyed = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in FIXED:
                    base.putpixel((x, y), (*FIXED[ch], 255))
                elif ch in TINT:
                    v = TINT[ch]
                    dyed.putpixel((x, y), (v, v, v, 255))
        base.save(os.path.join(folder, name + ".png"))
        layers = {"layer0": f"stagetents:item/{name}"}
        if dyed.getbbox():
            dyed.save(os.path.join(folder, name + "_dye.png"))
            layers["layer1"] = f"stagetents:item/{name}_dye"
        with open(os.path.join(assets, "models", "item", name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"parent": "item/generated", "textures": layers}, f, indent=2)
            f.write("\n")
    return sorted(ICONS)
