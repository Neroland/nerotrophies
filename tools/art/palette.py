"""NeroTrophies art palette. One source of truth for every generator in tools/art.

Values are ARGB-less 0xRRGGBB ints; helpers turn them into RGBA tuples. The client colour class
mirrors these byte for byte.
"""


def rgba(hex_rgb, alpha=255):
    return ((hex_rgb >> 16) & 255, (hex_rgb >> 8) & 255, hex_rgb & 255, alpha)


def lerp(a, b, t):
    ar, ag, ab = (a >> 16) & 255, (a >> 8) & 255, a & 255
    br, bg, bb = (b >> 16) & 255, (b >> 8) & 255, b & 255
    r = round(ar + (br - ar) * t)
    g = round(ag + (bg - ag) * t)
    bl = round(ab + (bb - ab) * t)
    return (r << 16) | (g << 8) | bl


def scale(c, f):
    return lerp(0x000000, c, f)


CLEAR = (0, 0, 0, 0)

# family metals and chrome
G_DARK = 0x1A1D22
GRAPHITE = 0x2C3137
GRAPHITE_4 = 0x3A4047
TRIM = 0x5B6670
STEEL = 0x7A8492
STEEL_DK = 0x424A56
G_LIGHT = 0x8E98A4
GLASS = 0x0B1119
NERO_ALLOY = 0x26A69A
PLASMA = 0x60D4E8
PANEL = 0x141C26
PANEL_EDGE = 0x2A3A4D
TEXT = 0xE8ECF4
TEXT_2 = 0x97A6BC
TEXT_FAINT = 0x5C6B82
TRACK = 0x1E2A3A

# NeroTrophies line
TROPHY_ACCENT = 0xA77BFF
TROPHY_ACCENT_BRIGHT = 0xD3BEFF
TROPHY_ACCENT_DIM = 0x4A3878
VELVET = 0x1B1428
VELVET_LT = 0x2A2040

DEFAULT_FINISH = 0x4A525C

PRISM_A = 0x2FE6C8
PRISM_MID = 0x60D4E8
PRISM_B = 0x9B6BFF

TIERS = ("bronze", "silver", "gold", "nero")


class Ramp:
    def __init__(self, deep, dk, base, lt, hi, ink=None, glow=None, hilite=None, frame=None):
        self.DEEP, self.DK, self.BASE, self.LT, self.HI = deep, dk, base, lt, hi
        self.INK, self.GLOW, self.HILITE = ink, glow, hilite
        self.FRAME = lt if frame is None else frame


RAMPS = {
    "bronze": Ramp(0x3A2218, 0x6B3F26, 0xA5673A, 0xD08A52, 0xF0B98A, 0xF0C9A4, 0xE8915A, 0xD9824A),
    "silver": Ramp(0x2E3640, 0x5E6B7A, 0x9AA7B6, 0xC9D3DF, 0xF2F6FA, 0xE8ECF4, 0xCFE2F5, 0xC9D3DF),
    "gold": Ramp(0x4A3208, 0x8F6614, 0xD9A520, 0xFFD54A, 0xFFF3B0, 0xFFE9A0, 0xFFDF6B, 0xFFDF3A),
    "nero": Ramp(0x0B0E13, 0x161B22, 0x232A33, 0x3A4654, 0x7FE9DC, 0xA8F0EA, PRISM_A, PRISM_A,
                 frame=PRISM_MID),
}

REPLICA_DEEP = 0x23262B
REPLICA_DK = 0x3F444B
REPLICA = 0x5E646C
REPLICA_LT = 0x7E858E
REPLICA_TEXT = 0x8A919A
# four steps only: HI collapses onto LT, there is no ink and no glow
REPLICA_RAMP = Ramp(REPLICA_DEEP, REPLICA_DK, REPLICA, REPLICA_LT, REPLICA_LT, REPLICA_DK, None, None,
                    frame=REPLICA)

GHOST_UNKNOWN = 0x97A6BC

# trim family: style -> list of (trim id, accent or None)
TRIMS = {
    "plate": [("standard", None)],
    "orbital": [("orbital_plasma", 0x60D4E8), ("orbital_amber", 0xFFB347)],
    "laurel": [("laurel_gold", 0xFFD54A), ("laurel_crimson", 0xD6424A), ("laurel_copper", 0xE48C42)],
    "wild": [("wild_green", 0x78D678), ("wild_sand", 0xC89858), ("wild_teal", 0x56CCC4)],
    "neon": [("neon_pink", 0xFF3FA4), ("neon_rose", 0xE8788E)],
    "prism": [("prism", None)],
}
TRIM_STYLES = tuple(TRIMS)


def prism_frames():
    """Eight colours of the 2 s teal <-> violet cycle (frame 0 teal, frame 4 violet)."""
    up = [PRISM_A, lerp(PRISM_A, PRISM_MID, 0.5), PRISM_MID, lerp(PRISM_MID, PRISM_B, 0.5), PRISM_B]
    return up + [up[3], up[2], up[1]]
