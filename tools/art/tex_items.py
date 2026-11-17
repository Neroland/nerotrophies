"""Item sprites, 16x16, art inside 14x14, 1-texel G_DARK outline. Each tier sprite is the tier's
front elevation at half scale so the tier reads in a slot with no highlight border."""
from palette import (G_DARK, GRAPHITE, GRAPHITE_4, TRIM, STEEL, STEEL_DK, G_LIGHT, GLASS, TEXT, TEXT_2,
                     TEXT_FAINT, TROPHY_ACCENT, TROPHY_ACCENT_BRIGHT, TROPHY_ACCENT_DIM, VELVET, VELVET_LT,
                     PRISM_A, PRISM_B, PRISM_MID, RAMPS, REPLICA_RAMP, REPLICA, REPLICA_DK, REPLICA_LT, TIERS)
from px import Canvas

HEX4 = [".XX.", "XXXX", "XXXX", ".XX."]
RING8 = ["..XXXX..", ".X....X.", "X......X", "X......X", "X......X", "X......X", ".X....X.", "..XXXX.."]


def _base(c, ramp, top=None, plate=None, ink=None, checker=None):
    """12x4 slab at the bottom of the art box, with the nameplate line in it."""
    c.r(2, 11, 12, 4, ramp.BASE)
    c.r(2, 11, 12, 1, ramp.LT if top is None else top)
    c.r(2, 14, 12, 1, ramp.DK)
    c.r(13, 12, 1, 2, ramp.DK)
    if checker is not None:
        for x in range(3, 13):
            c.p(x, 12, checker if x % 2 else ramp.BASE)
    else:
        c.r(3, 12, 10, 1, plate)
        c.p(7, 12, ink)
        c.p(8, 12, ink)


def _exhibit(c, x, y, body, lit, shade):
    c.art(x, y, HEX4, {"X": body})
    c.p(x + 1, y, lit)
    c.p(x, y + 1, lit)
    c.p(x + 2, y + 3, shade)
    c.p(x + 3, y + 2, shade)


def trophy_sprite(tier, replica=False):
    r = REPLICA_RAMP if replica else RAMPS[tier]
    c = Canvas(16, 16)
    if replica:
        ex = (REPLICA_LT, REPLICA_LT, REPLICA)
        base_kw = dict(checker=REPLICA_DK)
    else:
        ex = (TEXT_2, TEXT, TEXT_2)
        base_kw = dict(plate=GLASS, ink=r.INK)
    if tier == "bronze":
        _base(c, r, **base_kw)
        c.r(5, 10, 6, 1, r.DK)                                   # the inset pad
        _exhibit(c, 6, 6, *ex)
    elif tier == "silver":
        _base(c, r, **base_kw)
        c.r(4, 10, 8, 1, r.BASE)                                 # one step
        c.r(4, 10, 8, 1, r.LT) if False else None
        for x in (3, 12):                                        # two straight uprights
            c.r(x, 4, 1, 6, r.LT)
            c.p(x, 4, r.LT if replica else r.GLOW)
        c.p(12, 9, r.BASE)
        c.p(3, 9, r.BASE)
        _exhibit(c, 6, 6, *ex)
    elif tier == "gold":
        _base(c, r, **base_kw)
        c.r(3, 10, 10, 1, r.BASE)                                # two steps
        c.r(4, 9, 8, 1, r.LT)
        for (x, y) in ((3, 8), (2, 7), (1, 6), (1, 5), (2, 4)):  # arms curving out, then in
            c.p(x, y, r.LT)
            c.p(15 - x, y, r.LT)
        c.p(2, 4, r.HI)
        c.p(13, 4, r.HI)
        c.p(3, 8, r.DK)
        c.p(12, 8, r.DK)
        _exhibit(c, 6, 5, *ex)
    else:  # nero
        if replica:
            _base(c, r, **base_kw)
        else:
            _base(c, r, top=r.LT, plate=PRISM_MID, ink=PRISM_MID)
        c.r(5, 10, 6, 1, r.LT)                                   # hex plate
        lower = r.BASE if replica else PRISM_A
        upper = r.LT if replica else PRISM_B
        for j, row in enumerate(RING8):                          # ring: teal below, violet above
            for i, ch in enumerate(row):
                if ch == "X":
                    c.p(4 + i, 1 + j, upper if j < 4 else lower)
        _exhibit(c, 6, 3, *ex)                                   # floats two texels clear
    c.outline(G_DARK)
    return c


def trophy_unknown():
    c = Canvas(16, 16)
    c.r(2, 11, 12, 4, GRAPHITE)
    c.r(2, 11, 12, 1, GRAPHITE_4)
    c.r(3, 12, 10, 1, G_DARK)
    c.r(5, 10, 6, 1, GRAPHITE_4)
    c.art(6, 3, [".XX.", "X..X", "..X.", "..X.", "....", "..X."], {"X": TEXT_FAINT})
    c.outline(G_DARK)
    return c


def trophy_book():
    s, g, b = RAMPS["silver"], RAMPS["gold"], RAMPS["bronze"]
    c = Canvas(16, 16)
    c.r(3, 2, 10, 12, TROPHY_ACCENT_DIM)                         # cover
    c.r(3, 2, 10, 1, TROPHY_ACCENT)
    c.r(3, 2, 1, 12, TROPHY_ACCENT)
    c.r(1, 2, 2, 12, GRAPHITE_4)                                 # spine with the four tier studs
    for y, colour in ((4, b.LT), (6, s.LT), (8, g.LT), (10, PRISM_A)):
        c.p(2, y, colour)
    c.r(3, 14, 11, 1, s.LT)                                      # page edges
    c.r(13, 3, 1, 12, s.LT)
    c.p(13, 14, GRAPHITE_4)
    c.art(6, 5, HEX4, {"X": TROPHY_ACCENT_BRIGHT})               # the family hex
    c.p(7, 6, s.LT)
    c.r(12, 7, 2, 2, GRAPHITE_4)                                 # clasp
    c.outline(G_DARK)
    return c


def trophy_plaque():
    c = Canvas(16, 16)
    c.r(2, 1, 12, 14, VELVET)
    for (x, y) in ((4, 3), (10, 3), (7, 6), (4, 8), (11, 8)):
        c.p(x, y, VELVET_LT)
    c.frame(2, 1, 12, 14, STEEL_DK)
    ring = ["..XX..", ".X..X.", "X....X", "X....X", ".X..X.", "..XX.."]
    c.art(5, 2, ring, {"X": TROPHY_ACCENT})
    c.p(7, 2, TROPHY_ACCENT_BRIGHT); c.p(6, 3, TROPHY_ACCENT_BRIGHT); c.p(5, 4, TROPHY_ACCENT_BRIGHT)
    c.r(7, 4, 2, 2, TEXT_2)                                      # the exhibit
    for y in (9, 12):
        c.r(3, y, 10, 2, GLASS)
    c.r(5, 9, 6, 1, TEXT_2)                                      # title line
    c.r(6, 12, 4, 1, TROPHY_ACCENT_BRIGHT)                       # name line
    c.outline(G_DARK)
    return c


def trophy_spotlight():
    s = RAMPS["silver"]
    c = Canvas(16, 16)
    c.r(6, 1, 4, 1, TRIM)                                        # mount
    c.r(7, 2, 2, 2, STEEL_DK)                                    # arm
    c.r(4, 4, 8, 5, GRAPHITE)                                    # housing, three-quarter view
    c.r(4, 4, 8, 1, GRAPHITE_4)
    c.r(4, 4, 1, 5, GRAPHITE_4)
    c.r(5, 6, 6, 1, STEEL_DK)
    c.p(10, 5, G_LIGHT); c.p(10, 7, G_LIGHT)
    c.r(5, 9, 6, 1, STEEL_DK)                                    # rim
    c.r(7, 9, 2, 1, s.HI)                                        # lens
    c.outline(G_DARK)
    for (x, y) in ((7, 11), (8, 11), (6, 13), (9, 13)):          # cone
        c.p(x, y, s.GLOW)
    c.p(7, 11, s.GLOW)
    return c


def build():
    out = {}
    for t in TIERS:
        out["trophy_%s" % t] = trophy_sprite(t)
        out["trophy_%s_replica" % t] = trophy_sprite(t, replica=True)
    out["trophy_unknown"] = trophy_unknown()
    out["trophy_book"] = trophy_book()
    out["trophy_plaque"] = trophy_plaque()
    out["trophy_spotlight"] = trophy_spotlight()
    return out
