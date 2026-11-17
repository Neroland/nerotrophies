"""Display block textures: pedestal, cabinets, plaque, spotlight, ghost and the six trim styles.

Every sheet is 16x16 and laid out as the elevation or plan of the part it covers, so a model face at
block position (x, y) samples texel (x, 16 - y): the art and the UVs line up by construction.
"""
from palette import (G_DARK, GRAPHITE, GRAPHITE_4, TRIM, STEEL, STEEL_DK, G_LIGHT, GLASS, PLASMA, VELVET,
                     VELVET_LT, RAMPS, PRISM_MID, prism_frames, rgba)
from px import Canvas

WHITE = 0xFFFFFF
T_HI, T_FILL, T_LO = 0xFFFFFF, 0xD8D8D8, 0xB8B8B8    # finish layer greys


def tint_panel(c, x, y, w, h, holes=()):
    """Greyscale finish panel: lit top row and left column, shaded bottom row and right column."""
    c.r(x, y, w, h, T_FILL)
    c.r(x, y, w, 1, T_HI)
    c.r(x, y, 1, h, T_HI)
    c.r(x, y + h - 1, w, 1, T_LO)
    c.r(x + w - 1, y, 1, h, T_LO)
    for (hx, hy) in holes:
        c.p(hx, hy, None)


def velvet():
    c = Canvas(16, 16, VELVET)
    for j in range(4):
        for i in range(4):
            c.p(i * 4 + (2 if j % 2 else 0), j * 4 + 1, VELVET_LT)   # fixed weave
    return c


# ---------------------------------------------------------------- pedestal
def pedestal_side():
    c = Canvas(16, 16, GRAPHITE)
    c.r(4, 1, 8, 1, STEEL)                    # turntable edge
    c.r(2, 2, 12, 2, GRAPHITE_4)              # capital
    c.r(2, 3, 12, 1, GRAPHITE)
    c.r(2, 2, 12, 1, GRAPHITE_4)
    c.p(3, 2, G_LIGHT); c.p(12, 2, G_LIGHT)   # capital rivets
    c.p(3, 3, G_DARK); c.p(12, 3, G_DARK)
    c.r(3, 4, 10, 1, TRIM)                    # neck collar
    c.framed(4, 5, 8, 8, GRAPHITE, G_DARK)    # shaft: framed face
    for (x, y) in ((4, 5), (11, 5), (4, 12), (11, 12)):
        c.p(x, y, STEEL_DK)
    c.r(5, 8, 6, 1, G_DARK)                   # panel line at y 7
    c.r(3, 13, 10, 1, TRIM)                   # base collar
    c.r(2, 14, 12, 1, GRAPHITE)               # foot
    c.r(2, 15, 12, 1, G_DARK)
    c.p(2, 14, GRAPHITE_4); c.p(13, 14, G_DARK)
    return c


def pedestal_top():
    c = Canvas(16, 16, GRAPHITE)
    c.frame(2, 2, 12, 12, STEEL)              # capital rim
    for (x, y) in ((3, 3), (12, 3), (3, 11), (12, 11)):
        c.rivet(x, y, G_LIGHT, G_DARK)
    c.r(4, 4, 8, 8, STEEL)                    # turntable disc
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        c.p(x, y, GRAPHITE)
    c.r(4, 4, 1, 1, GRAPHITE)
    c.r(5, 5, 6, 1, G_LIGHT)                  # lit top-left arc of the disc
    c.r(5, 5, 1, 6, G_LIGHT)
    c.r(7, 7, 2, 1, G_DARK)                   # spindle
    return c


def pedestal_bottom():
    return Canvas(16, 16, G_DARK)


def pedestal_tint():
    c = Canvas(16, 16)
    # side faces: 6x6 panel at x5-10, rows 6-11, split by the panel line on row 8
    tint_panel(c, 5, 6, 6, 2)
    tint_panel(c, 5, 9, 6, 3)
    # front face (rows 0-5 hold its copy): only the three rows below the plaque are painted
    tint_panel(c, 5, 3, 6, 3)
    return c


def pedestal_ring():
    c = Canvas(16, 16)
    c.frame(3, 3, 10, 10, WHITE)              # plan: band round the turntable
    c.r(3, 1, 10, 1, WHITE)                   # elevation: the band's outer edge
    return c


def pedestal_plaque():
    c = Canvas(16, 16, STEEL)
    c.r(1, 5, 14, 4, GLASS)
    c.r(1, 5, 14, 1, STEEL)
    c.r(1, 5, 1, 4, STEEL)
    c.r(14, 5, 1, 4, STEEL)
    c.r(0, 10, 16, 6, STEEL_DK)               # the plate's back and edges
    return c


# ---------------------------------------------------------------- cabinets
def cabinet_outer():
    c = Canvas(16, 16)
    c.framed(0, 0, 16, 16, GRAPHITE, G_DARK)
    c.r(1, 8, 14, 1, STEEL_DK)                # panel line
    for (x, y) in ((1, 1), (14, 1), (1, 13), (14, 13)):
        c.rivet(x, y, G_LIGHT, G_DARK)
    return c


def cabinet_outer_tint():
    c = Canvas(16, 16)
    tint_panel(c, 2, 2, 12, 6)
    tint_panel(c, 2, 9, 12, 5)
    return c


def cabinet_shelf():
    c = Canvas(16, 16, STEEL_DK)
    c.r(0, 0, 16, 1, STEEL)                   # front lip
    c.r(0, 14, 16, 1, G_DARK)                 # rear shadow line
    c.r(2, 15, 12, 1, G_LIGHT)                # the roof's light strip samples this row
    return c


def cabinet_glass():
    c = Canvas(16, 16, rgba(PLASMA, 46))      # 18 %
    c.p(2, 3, rgba(PLASMA, 89))               # 35 % glints
    c.p(3, 2, rgba(PLASMA, 89))
    return c


def wall_cabinet_outer():
    c = Canvas(16, 16)
    c.framed(0, 0, 16, 5, GRAPHITE, G_DARK)   # roof top / floor underside, 16x5
    c.rivet(1, 1, G_LIGHT, G_DARK)
    c.rivet(14, 1, G_LIGHT, G_DARK)
    c.framed(0, 5, 16, 11, GRAPHITE, G_DARK)  # ends and the hidden back
    c.r(1, 10, 14, 1, STEEL_DK)
    return c


def wall_cabinet_outer_tint():
    c = Canvas(16, 16)
    tint_panel(c, 3, 1, 10, 3)
    return c


# ---------------------------------------------------------------- plaque
def plaque_back():
    c = Canvas(16, 16, STEEL_DK)
    v = velvet()
    for y in range(1, 15):
        for x in range(3, 13):
            c.p(x, y, v.get(x, y))
    c.frame(2, 0, 12, 16, STEEL_DK)
    c.r(0, 0, 2, 16, G_DARK)                  # the plate's thin edges
    c.r(14, 0, 2, 16, G_DARK)
    return c


def plaque_back_tint():
    c = Canvas(16, 16)
    c.frame(3, 1, 10, 14, T_FILL)
    c.r(3, 1, 10, 1, T_HI)
    c.r(3, 1, 1, 14, T_HI)
    c.r(3, 14, 10, 1, T_LO)
    c.r(12, 1, 1, 14, T_LO)
    return c


HEX8 = ["..XXXX..",
        ".X....X.",
        "X......X",
        "X......X",
        "X......X",
        "X......X",
        ".X....X.",
        "..XXXX.."]
HEX8_IN = ["........",
           "..XXX...",
           ".X......",
           ".X......",
           ".X......",
           "........",
           "........",
           "........"]


def plaque_medallion():
    c = Canvas(16, 16)
    c.art(4, 0, HEX8, {"X": T_FILL})
    # light catches the top-left edges: brighter, and one texel thicker
    for j, row in enumerate(HEX8):
        for i, ch in enumerate(row):
            if ch == "X" and (j < 2 or i < 2) and i + j < 8:
                c.p(4 + i, j, T_HI)
            elif ch == "X" and (j > 5 or i > 5) and i + j > 7:
                c.p(4 + i, j, T_LO)
    c.art(4, 0, HEX8_IN, {"X": T_HI})
    return c


def plaque_strip():
    c = Canvas(16, 16, STEEL_DK)
    for y in (11, 14):                        # title strip rows 11-12, name strip rows 14-15
        c.r(1, y, 14, 2, GLASS)
        c.r(1, y, 1, 2, STEEL)
        c.r(14, y, 1, 2, STEEL)
    c.r(0, 10, 16, 1, STEEL)                  # strip top and bottom edges
    c.r(0, 13, 16, 1, STEEL)
    return c


# ---------------------------------------------------------------- spotlight
def spotlight():
    c = Canvas(16, 16, GRAPHITE)
    c.framed(0, 0, 6, 4, GRAPHITE, G_DARK)    # housing side 6x4
    c.r(1, 1, 4, 2, GRAPHITE_4)               # band
    c.p(2, 1, G_LIGHT); c.p(3, 1, G_LIGHT)    # vent pair
    c.framed(6, 0, 6, 6, GRAPHITE, G_DARK)    # housing back / top 6x6
    c.r(8, 2, 2, 2, GRAPHITE_4)
    c.framed(0, 6, 6, 6, GRAPHITE, G_DARK)    # housing mouth 6x6
    c.r(1, 7, 4, 4, G_DARK)                   # lens seat
    c.r(12, 0, 4, 4, TRIM)                    # mount plate with four rivets
    for (x, y) in ((12, 0), (15, 0), (12, 3), (15, 3)):
        c.p(x, y, G_LIGHT)
    c.r(12, 4, 4, 1, STEEL_DK)                # mount edge
    c.r(12, 6, 4, 6, STEEL_DK)                # arm
    c.r(12, 6, 1, 6, TRIM)
    c.r(6, 8, 5, 2, STEEL_DK)                 # wall arm, lengthways
    c.r(6, 8, 5, 1, TRIM)
    return c


def spotlight_lens_glow():
    s = RAMPS["silver"]
    c = Canvas(16, 16)
    c.r(0, 0, 4, 4, s.GLOW)
    c.r(1, 1, 2, 2, s.HI)
    c.r(0, 4, 4, 1, s.GLOW)
    return c


def spotlight_cone():
    hi = RAMPS["silver"].HI
    c = Canvas(16, 16)
    a18, a10, a5, a25 = 46, 26, 13, 64
    # ceiling cone: rows 0-6, 4 px wide under the lens to 16 px, three flat bands
    for row, (w, a) in enumerate(((4, a18), (6, a18), (8, a10), (10, a10), (12, a10), (14, a5), (16, a5))):
        c.r(8 - w // 2, row, w, 1, rgba(hi, a))
    # wall-arm cone: rows 7-10
    for row, (w, a) in enumerate(((4, a18), (8, a10), (12, a10), (16, a5))):
        c.r(8 - w // 2, 7 + row, w, 1, rgba(hi, a))
    # halo cap: 6x5 at rows 11-15, corners dropped
    c.r(0, 11, 6, 5, rgba(hi, a25))
    for (x, y) in ((0, 11), (5, 11), (0, 15), (5, 15)):
        c.p(x, y, None)
    return c


# ---------------------------------------------------------------- ghost
def ghost():
    c = Canvas(16, 16)
    for y in range(0, 16, 2):
        c.r(0, y, 16, 1, WHITE)
    return c


# ---------------------------------------------------------------- trims
# regions: edge bar x0-15 y0-1 | post x0-1 y2-15 | crown x2-15 y2-5 | corner stud x12-15 y12-15
def _trim_base(bar0, bar1, post0, post1):
    c = Canvas(16, 16)
    c.r(0, 0, 16, 1, bar0)
    c.r(0, 1, 16, 1, bar1)
    c.r(0, 2, 1, 14, post0)
    c.r(1, 2, 1, 14, post1)
    return c


ARC = ["....XXXXXX....",       # shallow arc, 14x4 (crown region)
       "..XX......XX..",
       ".X..........X.",
       "X............X"]


def trim_plate():
    m = _trim_base(GRAPHITE_4, GRAPHITE, STEEL, STEEL_DK)
    m.r(2, 2, 14, 4, GRAPHITE_4)
    m.frame(2, 2, 14, 4, STEEL)
    m.r(3, 5, 12, 1, STEEL_DK)
    m.r(12, 12, 4, 4, STEEL)
    m.frame(12, 12, 4, 4, STEEL_DK)
    m.rivet(13, 13, G_LIGHT, G_DARK)
    return m, Canvas(16, 16)


def trim_orbital():
    m = _trim_base(STEEL, G_LIGHT, STEEL, G_LIGHT)
    a = Canvas(16, 16)
    a.r(0, 1, 16, 1, WHITE)                   # the rail's centre line
    a.r(1, 2, 1, 14, WHITE)
    m.art(2, 2, ARC, {"X": STEEL})
    m.r(8, 2, 2, 2, G_LIGHT)                  # node at the apex
    a.r(8, 2, 2, 2, WHITE)
    m.r(12, 12, 4, 4, STEEL)                  # ring bracket: a square with a 2x2 hole
    m.r(12, 12, 4, 1, G_LIGHT)
    m.r(13, 13, 2, 2, G_DARK)
    return m, a


def trim_laurel():
    m = _trim_base(GRAPHITE_4, GRAPHITE, GRAPHITE_4, GRAPHITE)
    a = Canvas(16, 16)
    for x in range(1, 16, 4):                 # a band every fourth texel
        m.r(x, 0, 1, 2, TRIM)
        a.r(x, 0, 1, 2, WHITE)
    for y in range(4, 16, 4):
        m.r(0, y, 2, 1, TRIM)
        a.r(0, y, 2, 1, WHITE)
    leaves = [(0, 3), (1, 2), (2, 1), (3, 1), (5, 0), (6, 0), (7, 0), (8, 0), (10, 1), (11, 1), (12, 2), (13, 3)]
    for i, (x, y) in enumerate(leaves):       # wreath arc of alternating leaf texels
        m.p(2 + x, 2 + y, G_LIGHT if i % 2 == 0 else TRIM)
        if i % 2 == 0:
            a.p(2 + x, 2 + y, WHITE)
    for (x, y) in ((1, 2), (2, 3), (4, 2), (9, 2), (11, 3), (12, 2)):
        m.p(2 + x, 2 + y, GRAPHITE_4)         # inner row of leaves, unlit
    m.r(12, 12, 4, 4, GRAPHITE_4)             # leaf stud: three stepped texels
    for i, (x, y) in enumerate(((12, 14), (13, 13), (14, 12))):
        m.p(x, y, G_LIGHT)
        a.p(x, y, WHITE) if i == 1 else None
    m.p(15, 15, GRAPHITE); m.p(14, 15, GRAPHITE); m.p(15, 14, GRAPHITE)
    return m, a


def trim_wild():
    b = RAMPS["bronze"]
    m = _trim_base(b.BASE, b.DK, b.BASE, b.DK)
    a = Canvas(16, 16)
    for (x, y) in ((5, 0), (11, 1), (0, 6), (1, 11)):
        m.p(x, y, b.DEEP)                     # fixed worn texels
    horns = [(0, 3), (1, 2), (2, 2), (3, 1), (4, 1), (5, 0), (6, 1),
             (13, 3), (12, 2), (11, 2), (10, 1), (9, 1), (8, 0), (7, 1)]
    for (x, y) in horns:                      # two horn curves meeting at the centre
        m.p(2 + x, 2 + y, b.BASE)
    for (x, y) in ((1, 3), (2, 3), (3, 2), (12, 3), (11, 3), (10, 2)):
        m.p(2 + x, 2 + y, b.DK)
    for (x, y) in ((5, 0), (8, 0), (0, 3), (13, 3)):
        m.p(2 + x, 2 + y, b.LT)
        a.p(2 + x, 2 + y, WHITE)              # horn tips
    m.r(12, 12, 4, 4, b.DK)                   # claw: an L of three texels hooked inward
    for (x, y) in ((12, 12), (12, 13), (13, 12)):
        m.p(x, y, b.BASE)
    m.p(14, 14, b.LT)
    m.p(13, 13, b.LT)
    a.p(14, 14, WHITE)
    a.p(13, 13, WHITE)
    m.p(15, 15, b.DEEP)
    return m, a


def trim_neon():
    m = _trim_base(G_DARK, GRAPHITE, G_DARK, GRAPHITE)
    a = Canvas(16, 16)
    a.r(0, 1, 16, 1, WHITE)                   # tube inlay
    a.r(1, 2, 1, 14, WHITE)
    m.r(2, 2, 14, 4, G_DARK)                  # marquee bar
    m.frame(2, 2, 14, 4, GRAPHITE)
    for x in (1, 3, 5, 8, 10, 12):            # six lamps
        m.p(2 + x, 4, GRAPHITE_4)
        a.p(2 + x, 4, WHITE)
        m.p(2 + x, 3, GRAPHITE)
    m.r(12, 12, 4, 4, G_DARK)
    m.frame(12, 12, 4, 4, GRAPHITE)
    m.p(13, 13, GRAPHITE_4)
    a.p(13, 13, WHITE)                        # lit centre texel
    return m, a


HEX4 = [".XX.", "XXXX", "XXXX", ".XX."]
PRISM_CROWN = ["......XX......",
               ".XX..X..X..XX.",
               "X..X.X..X.X..X",
               ".XX...XX...XX."]


def trim_prism(colour=None):
    n = RAMPS["nero"]
    m = _trim_base(n.DK, n.LT, n.DK, n.LT)
    a = Canvas(16, 16)
    a.r(0, 0, 16, 1, colour)                  # outer edge row
    a.r(0, 2, 1, 14, colour)
    m.r(2, 2, 14, 4, None)
    for j, row in enumerate(PRISM_CROWN):     # three hex outlines, the centre one taller
        for i, ch in enumerate(row):
            if ch == "X":
                m.p(2 + i, 2 + j, n.LT)
                a.p(2 + i, 2 + j, colour)
    for (x, y) in ((1, 2), (2, 2), (6, 1), (7, 1), (6, 2), (7, 2), (11, 2), (12, 2)):
        m.p(2 + x, 2 + y, n.BASE)             # hex interiors
    m.r(12, 12, 4, 4, n.DK)
    m.art(12, 12, HEX4, {"X": n.BASE})        # hex stud
    m.p(13, 12, n.LT); m.p(12, 13, n.LT)
    return m, a


def trim_prism_strip():
    strip = Canvas(16, 128)
    for i, colour in enumerate(prism_frames()):
        strip.blit(trim_prism(colour)[1], 0, i * 16)
    return strip


def build():
    out = {
        "display_pedestal_top": pedestal_top(),
        "display_pedestal_side": pedestal_side(),
        "display_pedestal_bottom": pedestal_bottom(),
        "display_pedestal_tint": pedestal_tint(),
        "display_pedestal_ring": pedestal_ring(),
        "display_pedestal_plaque": pedestal_plaque(),
        "trophy_cabinet_outer": cabinet_outer(),
        "trophy_cabinet_outer_tint": cabinet_outer_tint(),
        "trophy_cabinet_velvet": velvet(),
        "trophy_cabinet_shelf": cabinet_shelf(),
        "trophy_cabinet_glass": cabinet_glass(),
        "wall_cabinet_outer": wall_cabinet_outer(),
        "wall_cabinet_outer_tint": wall_cabinet_outer_tint(),
        "trophy_plaque_back": plaque_back(),
        "trophy_plaque_back_tint": plaque_back_tint(),
        "trophy_plaque_medallion": plaque_medallion(),
        "trophy_plaque_strip": plaque_strip(),
        "trophy_spotlight": spotlight(),
        "trophy_spotlight_lens_glow": spotlight_lens_glow(),
        "trophy_spotlight_cone": spotlight_cone(),
        "trophy_ghost": ghost(),
    }
    for style, fn in (("plate", trim_plate), ("orbital", trim_orbital), ("laurel", trim_laurel),
                      ("wild", trim_wild), ("neon", trim_neon)):
        metal, accent = fn()
        out["trim_%s" % style] = metal
        out["trim_%s_accent" % style] = accent
    out["trim_prism"] = trim_prism(PRISM_MID)[0]
    out["trim_prism_accent"] = trim_prism_strip()
    return out
