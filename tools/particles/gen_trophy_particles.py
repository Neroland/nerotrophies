#!/usr/bin/env python3
"""
Particle sprites and definitions for NeroTrophies (greyscale, tinted in code at spawn).

    python3 gen_trophy_particles.py ASSETS_DIR [PROOF_PNG]

ASSETS_DIR is the mod's assets/nerotrophies folder. Writes textures/particle/*.png (15 sprites) and
particles/*.json (5 definitions). Two grey levels only: FF core, B8 arms and lower edges; the last ring
frame is at half alpha. Hand-laid pixel maps, no randomness, so the output is identical on every run.
"""
import json
import os
import sys

from PIL import Image

W, G = 255, 184           # core, arm


def sprite(rows):
    """rows of text: '#' core, '+' arm, anything else transparent."""
    im = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                im.putpixel((x, y), (W, W, W, 255))
            elif ch == "+":
                im.putpixel((x, y), (G, G, G, 255))
    return im


def pad8(rows, ox, oy):
    out = [["."] * 8 for _ in range(8)]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            out[oy + y][ox + x] = ch
    return ["".join(r) for r in out]


def hex_ring(across, alpha=255):
    """A pointy-top hexagon outline, 1 texel thick, `across` texels from its top vertex to its bottom one."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    c = 8.0 if across % 2 == 0 else 7.5
    r = across / 2.0 * 0.8660254        # inradius: the flat sides are left and right
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x + 0.5 - c), abs(y + 0.5 - c)
            norm = max(dx, dx * 0.5 + dy * 0.8660254)
            if r - 0.95 < norm <= r + 0.05:
                v = W if (y + 0.5 < c) else G          # lit from the top: the lower half is one step down
                im.putpixel((x, y), (v, v, v, alpha))
    return im


SPRITES = {
    # spark: a 3x3 plus shrinking to 2x2, a 1x2 streak and a single texel
    "trophy_spark_0": sprite(pad8([".+.", "+#+", ".+."], 2, 2)),
    "trophy_spark_1": sprite(pad8(["#+", "++"], 3, 3)),
    "trophy_spark_2": sprite(pad8(["#", "+"], 3, 3)),
    "trophy_spark_3": sprite(pad8(["#"], 3, 3)),
    # glint: a four-point star that grows and shrinks once
    "trophy_glint_0": sprite(pad8(["#"], 3, 3)),
    "trophy_glint_1": sprite(pad8([".+.", "+#+", ".+."], 2, 2)),
    "trophy_glint_2": sprite(pad8(["..+..", "..#..", "+###+", "..#..", "..+.."], 1, 1)),
    "trophy_glint_3": sprite(pad8([".+.", "+#+", ".+."], 2, 2)),
    # shard: three hex fragments, white with a B8 lower edge
    "trophy_shard_0": sprite(pad8([".#.", "###", "#.#", "+.+"], 2, 2)),          # 3x4 chevron
    "trophy_shard_1": sprite(pad8([".##.", "####", "++++"], 2, 2)),              # 4x3 half hex
    "trophy_shard_2": sprite(pad8(["#.", "##", ".+"], 3, 2)),                    # 2x3 sliver
    # ring: a hex outline 6, 9, 12 then 14 px across; the last frame at half alpha
    "trophy_ring_0": hex_ring(6),
    "trophy_ring_1": hex_ring(9),
    "trophy_ring_2": hex_ring(12),
    "trophy_ring_3": hex_ring(14, alpha=128),
}

DEFINITIONS = {
    "trophy_spark": ["trophy_spark_0", "trophy_spark_1", "trophy_spark_2", "trophy_spark_3"],
    "trophy_glint": ["trophy_glint_0", "trophy_glint_1", "trophy_glint_2", "trophy_glint_3"],
    "trophy_shard": ["trophy_shard_0", "trophy_shard_1", "trophy_shard_2"],
    "trophy_ring": ["trophy_ring_0", "trophy_ring_1", "trophy_ring_2", "trophy_ring_3"],
    "trophy_mote": ["trophy_spark_3"],
}

TINTS = [(232, 145, 90), (207, 226, 245), (255, 223, 107), (47, 230, 200), (155, 107, 255)]


def main():
    assets = sys.argv[1]
    tex, par = os.path.join(assets, "textures", "particle"), os.path.join(assets, "particles")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(par, exist_ok=True)
    for name, im in SPRITES.items():
        greys = {p[0] for p in im.get_flattened_data() if p[3]}
        assert len(greys) <= 3 and im.size in ((8, 8), (16, 16)), name
        im.save(os.path.join(tex, name + ".png"))
    for name, frames in DEFINITIONS.items():
        with open(os.path.join(par, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps({"textures": ["nerotrophies:" + n for n in frames]}, indent=2) + "\n")
    print("wrote %d sprites, %d definitions" % (len(SPRITES), len(DEFINITIONS)))
    if len(sys.argv) > 2:
        s = 12
        names = list(SPRITES)
        sheet = Image.new("RGBA", (8 * s * 11 + 16 * s * 4 + 15 * (s // 2) + 20, (len(TINTS) + 1) * 17 * s + 10), (14, 20, 30, 255))
        for row, tint in enumerate([(255, 255, 255)] + TINTS):
            x = 10
            for nm in names:
                im = SPRITES[nm]
                t = Image.new("RGBA", im.size)
                t.putdata([(p[0] * tint[0] // 255, p[1] * tint[1] // 255, p[2] * tint[2] // 255, p[3]) for p in im.get_flattened_data()])
                big = t.resize((im.width * s, im.height * s), Image.NEAREST)
                sheet.alpha_composite(big, (x, 5 + row * 17 * s + (16 * s - big.height) // 2))
                x += big.width + s // 2
        sheet.save(sys.argv[2])


if __name__ == "__main__":
    main()
