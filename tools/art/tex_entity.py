"""Sheets the block-entity renderers draw themselves: plate ornaments, covers, tier pips, the nero ring.

Everything white-on-clear here is a mask tinted at draw time; the covers are final colour.
"""
import layout as L
from px import Canvas
from tex_trophy import replica_cover

WHITE = 0xFFFFFF

PIP_CELL = (8, 4)                                   # one cell of tier_pips.png
PIP_CELLS = ("bronze", "silver", "gold", "nero", "unknown")
PIP_X = {"bronze": (3,), "silver": (1, 5), "gold": (0, 3, 6)}   # left texel of each 2x2 pip
QUESTION = ["XXX", ".XX", "...", ".X."]             # hook, gap, dot

# flat-top hexagon outline, 12 texels across the flats (rows 2-13), 14 across the points (columns 1-14)
RING_HALF = [(4, 11), (3, 12), (3, 12), (2, 13), (2, 13), (1, 14)]   # rows 2-7: left and right texel


def nameplate_blank():
    """Dash, stud, dash over the 10x2 text area of a nameplate."""
    c = Canvas(10, 2)
    c.r(0, 1, 3, 1, WHITE)
    c.r(7, 1, 3, 1, WHITE)
    c.r(4, 0, 2, 2, WHITE)
    return c


def tier_pips():
    cw, ch = PIP_CELL
    c = Canvas(cw * len(PIP_CELLS), ch)
    for i, name in enumerate(PIP_CELLS):
        x0 = i * cw
        if name == "nero":
            c.art(x0 + 2, 0, L.HEX_PIP, {"X": WHITE})
        elif name == "unknown":
            c.art(x0 + 3, 0, QUESTION, {"X": WHITE})
        else:
            for px in PIP_X[name]:
                c.r(x0 + px, 1, 2, 2, WHITE)
    return c


def trophy_ring():
    c = Canvas(16, 16)
    for j, (xl, xr) in enumerate(RING_HALF):
        for y in (2 + j, 13 - j):
            if j == 0:
                c.r(xl, y, xr - xl + 1, 1, WHITE)
            else:
                c.p(xl, y, WHITE)
                c.p(xr, y, WHITE)
    return c


def build():
    return {
        "nameplate_blank": nameplate_blank(),
        "nameplate_replica": replica_cover(),
        "plate_cover_pedestal": replica_cover(w=14, h=4, rivets=(1, 12)),
        "plate_cover_plaque": replica_cover(w=14, h=2, rivets=(1, 12)),
        "tier_pips": tier_pips(),
        "trophy_ring": trophy_ring(),
    }
