"""GUI sheets: toast, Trophy Book widgets, mod emblems and the cabinet screen. 1 GUI px per texel."""
from palette import (G_DARK, GRAPHITE, GLASS, PANEL, PANEL_EDGE, TEXT, TEXT_2, TEXT_FAINT, TRACK, STEEL_DK,
                     TROPHY_ACCENT, TROPHY_ACCENT_BRIGHT, TROPHY_ACCENT_DIM, VELVET, VELVET_LT, PRISM_A, PRISM_B,
                     PRISM_MID, RAMPS, TIERS, rgba, scale)
from px import Canvas
from emblems import EMBLEMS

WHITE = 0xFFFFFF
WELL = (4, 2, 16, 16)   # clear well for the 16x16 icon item inside a 24x24 medallion


def _plinth(c, r, plate=GLASS, ink=None, top=None):
    """20x4 slab on rows 19-22, the nameplate line inside it."""
    c.r(2, 19, 20, 4, r.BASE)
    c.r(2, 19, 20, 1, r.LT if top is None else top)
    c.r(2, 22, 20, 1, r.DK)
    c.r(21, 20, 1, 2, r.DK)
    c.r(4, 21, 16, 1, plate)
    c.r(10, 21, 4, 1, r.INK if ink is None else ink)


def _outline_outside_well(c):
    before = {(x, y) for y in range(24) for x in range(24) if c.get(x, y)[3]}
    c.outline(G_DARK)
    wx, wy, ww, wh = WELL
    for y in range(wy, wy + wh):
        for x in range(wx, wx + ww):
            if (x, y) not in before:
                c.p(x, y, None)


NERO_RING = [(0, 8, 15), (1, 6, 17), (2, 4, 19), (3, 3, 20), (4, 2, 21)]   # (row, left x, right x)


def medallion(tier):
    r = RAMPS[tier]
    c = Canvas(24, 24)
    if tier == "bronze":
        _plinth(c, r)
        c.r(6, 18, 12, 1, r.DK)                      # the inset pad
    elif tier == "silver":
        _plinth(c, r)
        c.r(4, 18, 16, 1, r.BASE)                    # one step
        for x in (2, 20):                            # uprights
            c.r(x, 6, 2, 13, r.LT)
            c.r(x + (1 if x == 2 else 0), 6, 1, 13, r.BASE)
            c.r(x, 6, 2, 1, r.GLOW)
    elif tier == "gold":
        _plinth(c, r)
        c.r(3, 18, 18, 1, r.BASE)                    # two steps
        c.r(5, 17, 14, 1, r.LT)
        arm = [(3, 16), (2, 15), (2, 14), (1, 13), (1, 12), (1, 11), (1, 10), (1, 9), (2, 8), (2, 7), (3, 6)]
        for i, (x, y) in enumerate(arm):             # laurel arms, alternating leaves
            for xx in (x, 23 - x):
                c.p(xx, y, r.LT if i % 2 == 0 else r.BASE)
        for xx in (3, 20):
            c.p(xx, 5, r.HI)
            c.p(xx, 6, r.HI)
        for (x, y) in ((2, 12), (2, 10), (3, 14)):   # leaf barbs
            c.p(x, y, r.DK)
            c.p(23 - x, y, r.DK)
    else:
        _plinth(c, r, plate=PRISM_MID, ink=PRISM_MID, top=r.LT)
        c.r(8, 18, 8, 1, r.LT)                       # hex emitter plate
        for (row, xl, xr) in NERO_RING:              # hex ring, violet above, teal below
            n = 8 if row == 0 else (2 if row in (1, 2) else 1)
            for (y, colour) in ((row, PRISM_B), (17 - row, PRISM_A)):
                c.r(xl, y, n, 1, colour)
                c.r(xr - n + 1, y, n, 1, colour)
        for y in range(5, 13):
            colour = PRISM_B if y < 9 else PRISM_A
            c.p(1, y, colour)
            c.p(22, y, colour)
    _outline_outside_well(c)
    return c


def collection_medallion():
    c = Canvas(24, 24)
    for y in range(24):
        for x in range(24):
            if (x - 11.5) ** 2 + (y - 11.5) ** 2 <= 10.6 ** 2:
                c.p(x, y, TROPHY_ACCENT_DIM)
    crown = ["........XX........",
             ".XX....X..X....XX.",
             "X..X..X....X..X..X",
             "X..X..X....X..X..X",
             ".XX...X....X...XX.",
             ".......X..X.......",
             "........XX........",
             "..................",
             "XXXXXXXXXXXXXXXXXX"]
    c.art(3, 7, crown, {"X": TROPHY_ACCENT_BRIGHT})
    c.outline(G_DARK)
    return c


def summary_medallion():
    c = Canvas(24, 24)
    plinth = ["..XXXXXX..", ".X......X.", "XXXXXXXXXX", "X........X", "X........X", "XXXXXXXXXX"]
    for i, (x, y) in enumerate(((12, 5), (7, 10), (2, 15))):   # stepped back to the right
        for j, row in enumerate(plinth):
            for k, ch in enumerate(row):
                c.p(x + k, y + j, TROPHY_ACCENT_BRIGHT if ch == "X" else PANEL)
    c.outline(G_DARK)
    return c


def toast_sheet():
    c = Canvas(128, 64)
    for i, t in enumerate(TIERS):
        c.blit(medallion(t), i * 24, 0)
    c.blit(collection_medallion(), 96, 0)
    c.r(0, 24, 3, 3, WHITE)                                   # pip dot
    c.art(4, 24, [".X.", "X.X", ".X."], {"X": WHITE})         # pip hex
    for y in range(32):                                       # sheen: a slanted band
        s = (31 - y) // 8
        for i, a in enumerate((51, 102, 102, 102, 102, 51)):  # 20 % edges, 40 % centre
            c.p(s + i, 32 + y, rgba(WHITE, a))
    c.blit(summary_medallion(), 16, 32)
    return c


# ------------------------------------------------------------------ book
def _hull(c, x, y, w, h):
    c.framed(x, y, w, h, PANEL, PANEL_EDGE)
    for (cx, cy, dx, dy) in ((x, y, 1, 1), (x + w - 1, y, -1, 1), (x, y + h - 1, 1, -1), (x + w - 1, y + h - 1, -1, -1)):
        for i in range(3):                                    # 3-texel accent bracket
            c.p(cx + dx * i, cy, TROPHY_ACCENT)
            c.p(cx, cy + dy * i, TROPHY_ACCENT)


def _button(c, x, y, w, h, fill, frame, arrow=None, arrow_colour=TEXT):
    c.framed(x, y, w, h, fill, frame)
    if arrow == "left":
        c.art(x + 4, y + 5, ["..X", ".XX", "XXX", "XXX", ".XX", "..X"], {"X": arrow_colour})
    elif arrow == "right":
        c.art(x + 5, y + 5, ["X..", "XX.", "XXX", "XXX", "XX.", "X.."], {"X": arrow_colour})


CHECK7 = [".......", "......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."]
LOCK7 = ["..XXX..", ".X...X.", ".X...X.", "XXXXXXX", "XXX.XXX", "XXX.XXX", "XXXXXXX"]
RECLAIM7 = ["..XXX.X", ".X...XX", "X...XXX", "X......", "X.....X", ".X...X.", "..XXX.."]
QUESTION = [".XXX.", "X...X", "....X", "...X.", "..X..", ".....", "..X.."]


def book_sheet():
    c = Canvas(256, 256)
    _hull(c, 0, 0, 24, 24)
    # card frames, 12x12, nine-slice border 3
    c.frame(24, 0, 12, 12, PANEL_EDGE)                        # locked
    for i in range(12):                                       # hidden: dashed 2 on, 2 off
        if i % 4 < 2:
            for (x, y) in ((36 + i, 0), (36 + i, 11), (36, i), (47, i)):
                c.p(x, y, TEXT_FAINT)
    c.frame(48, 0, 12, 12, TROPHY_ACCENT)                     # hover
    c.frame(60, 0, 12, 12, TROPHY_ACCENT_BRIGHT)              # selected ring
    c.frame(72, 0, 12, 12, WHITE)                             # earned: mask, tinted to the tier
    # buttons 12x16, nine-slice border 3
    _button(c, 0, 24, 12, 16, PANEL, PANEL_EDGE)
    _button(c, 12, 24, 12, 16, PANEL, TROPHY_ACCENT)
    _button(c, 24, 24, 12, 16, TROPHY_ACCENT_DIM, TROPHY_ACCENT)
    _button(c, 36, 24, 12, 16, PANEL, TRACK)
    # checkboxes 8x8: off, on, off-hover, on-hover
    tick = ["......", ".....X", "....X.", "X..X..", ".XX...", "......"]
    for i, (frame, on) in enumerate(((PANEL_EDGE, False), (PANEL_EDGE, True), (TROPHY_ACCENT, False), (TROPHY_ACCENT, True))):
        x = 48 + i * 8
        c.framed(x, 24, 8, 8, GLASS, frame)
        if on:
            c.art(x + 1, 25, tick, {"X": TROPHY_ACCENT_BRIGHT})
    # chips 12x14: off, and a white mask for on
    c.frame(0, 40, 12, 14, PANEL_EDGE)
    c.frame(12, 40, 12, 14, WHITE)
    # glyphs 7x7, white, tinted
    c.art(0, 56, CHECK7, {"X": WHITE})
    c.art(8, 56, LOCK7, {"X": WHITE})
    c.art(16, 56, RECLAIM7, {"X": WHITE})
    c.r(32, 56, 3, 3, WHITE)
    c.art(36, 56, [".X.", "X.X", ".X."], {"X": WHITE})
    # icon wells 20x20: earned, locked, hidden
    c.framed(0, 64, 20, 20, GLASS, PANEL_EDGE)
    c.framed(20, 64, 20, 20, GLASS, TRACK)
    c.framed(40, 64, 20, 20, GLASS, TRACK)
    c.art(47, 70, QUESTION, {"X": TEXT_FAINT})
    # the five medallions, identical to the toast sheet
    for i, t in enumerate(TIERS):
        c.blit(medallion(t), i * 24, 88)
    c.blit(collection_medallion(), 96, 88)
    # scrollbar thumb and track 4x8, nine-slice border 1
    c.framed(0, 112, 4, 8, TEXT_FAINT, PANEL_EDGE)
    c.framed(4, 112, 4, 8, GLASS, TRACK)
    return c


def emblem_sheet():
    c = Canvas(128, 48)
    for i, (name, accent, art) in enumerate(EMBLEMS):
        cell = Canvas(16, 16)
        cell.art(2, 2, art.strip("\n").split("\n"), {"X": TEXT, "s": TEXT_2, "a": accent})
        cell.outline(G_DARK)
        c.blit(cell, (i % 8) * 16, (i // 8) * 16)
    return c


# ------------------------------------------------------------------ cabinet screen
def _well(c, x, y):
    """18x18 inset slot well."""
    c.r(x, y, 18, 18, GLASS)
    c.r(x, y, 18, 1, G_DARK)
    c.r(x, y, 1, 18, G_DARK)
    c.r(x, y + 17, 18, 1, PANEL_EDGE)
    c.r(x + 17, y, 1, 18, PANEL_EDGE)


def _velvet(c, x, y, w, h):
    c.r(x, y, w, h, VELVET)
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            i, j = (xx - x) // 4, (yy - y) // 4
            if (xx - x) % 4 == (2 if j % 2 else 0) and (yy - y) % 4 == 1:
                c.p(xx, yy, VELVET_LT)


def _velvet_well(c, x, y):
    c.frame(x, y, 18, 18, TROPHY_ACCENT_DIM)


def cabinet_sheet():
    c = Canvas(256, 256)
    _hull(c, 0, 0, 176, 186)
    _velvet(c, 8, 20, 54, 36)                                 # floor cabinet: 3 x 2 wells
    for row in range(2):
        for i in range(3):
            _velvet_well(c, 8 + i * 18, 20 + row * 18)
    c.frame(7, 19, 56, 38, PANEL_EDGE)
    c.framed(84, 72, 70, 16, GLASS, PANEL_EDGE)               # trim preview well
    c.r(8, 98, 160, 1, PANEL_EDGE)                            # rule above the inventory
    for row in range(3):                                      # player inventory, vanilla positions
        for i in range(9):
            _well(c, 7 + i * 18, 103 + row * 18)
    for i in range(9):
        _well(c, 7 + i * 18, 161)
    # wall cabinet slot area, drawn over (8, 20)
    _velvet(c, 176, 0, 54, 36)
    for x in (9, 27):
        _velvet_well(c, 176 + x, 9)
    # previous / next: idle, hover, disabled
    for i, (frame, colour) in enumerate(((PANEL_EDGE, TEXT), (TROPHY_ACCENT, TEXT), (TRACK, TEXT_FAINT))):
        _button(c, 176 + i * 12, 40, 12, 16, PANEL, frame, "left", colour)
        _button(c, 176 + i * 12, 56, 12, 16, PANEL, frame, "right", colour)
    # collection field: idle, hover, open
    c.framed(0, 190, 98, 16, GLASS, PANEL_EDGE)
    c.framed(0, 206, 98, 16, GLASS, TROPHY_ACCENT)
    c.framed(0, 222, 98, 16, TRACK, TROPHY_ACCENT_BRIGHT)
    # list row: plain, hover
    c.r(100, 190, 98, 12, PANEL)
    c.r(100, 202, 98, 12, TRACK)
    c.r(100, 190, 1, 24, PANEL_EDGE)
    c.r(197, 190, 1, 24, PANEL_EDGE)
    c.frame(200, 190, 18, 18, WHITE)                          # slot border mask, tinted to the tier
    c.art(220, 190, ["XXXXX", ".XXX.", "..X.."], {"X": TEXT_2})
    return c


def build():
    return {
        "trophy_toast": toast_sheet(),
        "trophy_book": book_sheet(),
        "mod_emblems": emblem_sheet(),
        "trophy_cabinet": cabinet_sheet(),
    }
