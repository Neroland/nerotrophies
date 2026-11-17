"""Texel rectangles (x, y, w, h) on the 32x32 trophy tier sheets.

Shared by the texture generator and the model writer so UVs can never drift from the art.
"""

# common to every tier
STAGE = (0, 0, 12, 12)        # name block top: the stage floor
FOOT_BOTTOM = (12, 0, 12, 12)
SPARE = (24, 0, 8, 12)
PLATE = (0, 12, 12, 3)        # nameplate: front of the name block
FRONT_FOOT = (0, 15, 12, 1)
SIDE = (12, 12, 12, 3)        # east; west is the mirror
SIDE_FOOT = (12, 15, 12, 1)
BACK = (0, 16, 12, 3)
BACK_FOOT = (0, 19, 12, 1)

BRONZE = {
    "pad_top": (0, 20, 6, 6),
    "pad_side": (12, 16, 6, 1),
    "clip": (18, 16, 2, 1),
}

SILVER = {
    "step_top": (0, 20, 10, 10),
    "step_side": (12, 16, 10, 2),
    "pad_top": (10, 20, 6, 6),
    "pad_side": (22, 16, 6, 1),
    "up_front": (16, 20, 1, 8),
    "up_outer": (17, 20, 2, 8),
    "up_inner": (19, 20, 2, 8),
    "up_back": (21, 20, 1, 8),
    "up_top": (22, 20, 1, 2),
    "glow_tips": (22, 22, 10, 2),   # glow sheet only
}

GOLD = {
    "step1_top": (0, 20, 10, 10),
    "step1_side": (12, 16, 10, 1),
    "step2_top": (10, 20, 8, 8),
    "step2_side": (12, 17, 8, 1),
    "step2_side_e": (20, 17, 8, 1),  # east riser; west mirrors it (glow: front six texels)
    "root": (18, 20, 2, 2),
    "riser_front": (20, 20, 1, 5),
    "riser_outer": (21, 20, 2, 5),
    "riser_inner": (23, 20, 2, 5),
    "riser_back": (25, 20, 1, 5),
    "tip_front": (26, 20, 1, 3),
    "tip_outer": (27, 20, 2, 3),
    "tip_inner": (29, 20, 2, 3),
    "tip_top": (31, 20, 1, 2),
    "tip_back": (26, 23, 1, 3),
}

NERO = {
    "plate_top": (0, 20, 8, 8),     # plan of x4-12, z4-12; each slab maps its own sub-rect
    "plate_side": (12, 16, 8, 1),
    "node": (8, 20, 1, 1),
    "lens_top": (10, 20, 2, 2),     # glow sheet only
    "lens_side": (10, 22, 2, 1),    # glow sheet only
}

TIER_LAYOUT = {"bronze": BRONZE, "silver": SILVER, "gold": GOLD, "nero": NERO}

# pip stamps on the 12-wide side and back strips: (column, row, w, h) squares, or the hex outline
PIPS = {
    "bronze": [(5, 0)],
    "silver": [(3, 0), (7, 0)],
    "gold": [(2, 0), (5, 0), (8, 0)],
}
HEX_PIP = [".XX.", "X..X", ".XX."]   # nero, columns 4-7, rows 0-2
HEX_PIP_COL = 4

# footprint of the nero hex plate inside the 12x12 stage (stage texel = block x-2, z-2)
def nero_footprint():
    cells = set()
    for (x1, x2, z1, z2) in ((4, 12, 6, 10), (5, 11, 5, 11), (6, 10, 4, 12)):
        for x in range(x1, x2):
            for z in range(z1, z2):
                cells.add((x - 2, z - 2))
    return cells
