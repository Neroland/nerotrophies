"""Trophy tier sheets, glow layers and pewter replica twins (32x32 atlases at 16 texels per block)."""
from palette import (GLASS, RAMPS, REPLICA_RAMP, REPLICA_LT, PRISM_MID, TIERS, prism_frames)
from px import Canvas
import layout as L


def _roles(tier, ramp, replica):
    """Which ramp step each surface of the common base uses."""
    if tier == "nero":
        # black metal: sides one step down, lit border on the stage, no streak
        return dict(side_top=ramp.BASE, side=ramp.DK, side_bot=ramp.DEEP, side_edge=ramp.DEEP,
                    foot=ramp.DEEP, stage=ramp.BASE, stage_border=ramp.LT, streak=None)
    streak = None if replica else (ramp.LT if tier == "bronze" else ramp.HI)
    return dict(side_top=ramp.LT, side=ramp.BASE, side_bot=ramp.DK, side_edge=ramp.DK,
                foot=ramp.DK, stage=ramp.LT, stage_border=ramp.BASE, streak=streak)


def _pips(c, x0, y0, tier, ink):
    if tier == "nero":
        c.art(x0 + L.HEX_PIP_COL, y0, L.HEX_PIP, {"X": ink})
    else:
        for (px, py) in L.PIPS[tier]:
            c.r(x0 + px, y0 + py, 2, 2, ink)


def _strip(c, rect, foot_rect, tier, ro, ink, stamp=None):
    x, y, w, h = rect
    c.r(x, y, w, 1, ro["side_top"])
    c.r(x, y + 1, w, 1, ro["side"])
    c.r(x, y + 2, w, 1, ro["side_bot"])
    c.r(x, y, 1, h, ro["side_edge"])
    c.r(x + w - 1, y, 1, h, ro["side_edge"])
    fx, fy, fw, fh = foot_rect
    c.r(fx, fy, fw, fh, ro["foot"])
    _pips(c, x, y, tier, ink)
    if stamp is not None:
        c.r(x + 10, y + 1, 2, 2, stamp)   # family hex stamp, back strip only


def replica_cover(ramp=REPLICA_RAMP, w=12, h=3, rivets=(1, 10)):
    """The dead-metal plate cover: field, checker on the lower rows, two rivets."""
    c = Canvas(w, h, ramp.BASE)
    for yy in range(1, h):
        for xx in range(w):
            if (xx + yy) % 2 == 0:
                c.p(xx, yy, ramp.DK)
    for rx in rivets:
        c.p(rx, h - 2, ramp.LT)
        c.p(rx, h - 1, ramp.DEEP)
    return c


def tier_sheet(tier, replica=False):
    ramp = REPLICA_RAMP if replica else RAMPS[tier]
    ro = _roles(tier, ramp, replica)
    ink = ramp.INK
    if replica and tier == "nero":
        ink = REPLICA_LT   # the sides are already REPLICA_DK
    c = Canvas(32, 32, ramp.DK)

    # stage floor
    x, y, w, h = L.STAGE
    c.framed(x, y, w, h, ro["stage"], ro["stage_border"])
    if ro["streak"] is not None:
        c.p(x + 2, y + 3, ro["streak"])
        c.p(x + 3, y + 2, ro["streak"])
    # foot underside
    c.r(*L.FOOT_BOTTOM, ramp.DEEP)
    # spare column: tier swatch with the pip stamp, reused by particles and the upper parts
    sx, sy, sw, sh = L.SPARE
    c.framed(sx, sy, sw, sh, ramp.BASE, ramp.DK)
    if tier == "nero":
        c.art(sx + 2, sy + 2, L.HEX_PIP, {"X": ink})
    else:
        for i in range(len(L.PIPS[tier])):
            c.r(sx + 3, sy + 2 + i * 3, 2, 2, ink)

    # nameplate
    px_, py_, pw, ph = L.PLATE
    if replica:
        c.blit(replica_cover(), px_, py_)
    else:
        c.r(px_, py_, pw, ph, ramp.FRAME)
        c.r(px_ + 1, py_ + 1, pw - 2, ph - 1, GLASS)
    c.r(*L.FRONT_FOOT, ro["foot"])
    _strip(c, L.SIDE, L.SIDE_FOOT, tier, ro, ink)
    _strip(c, L.BACK, L.BACK_FOOT, tier, ro, ink, stamp=ramp.LT)

    T = L.TIER_LAYOUT[tier]
    if tier == "bronze":
        c.framed(*T["pad_top"], ramp.DEEP, ramp.DK)
        c.r(*T["pad_side"], ramp.DK)
        c.r(*T["clip"], ramp.LT)
    elif tier == "silver":
        c.framed(*T["step_top"], ramp.LT, ramp.BASE)
        x, y, w, h = T["step_side"]
        c.r(x, y, w, 1, ramp.BASE)
        c.r(x, y + 1, w, 1, ramp.DK)
        c.framed(*T["pad_top"], ramp.DEEP, ramp.DK)
        c.r(*T["pad_side"], ramp.DK)
        x, y, w, h = T["up_front"]
        c.r(x, y, w, h, ramp.LT)
        c.p(x, y + 2, ramp.HI)                      # one bright texel two-thirds of the way up
        x, y, w, h = T["up_outer"]
        c.r(x, y, w, h, ramp.LT)
        c.r(x, y, 1, h, ramp.BASE)                  # rear half of the outer face falls away
        c.r(*T["up_inner"], ramp.DK)
        c.r(*T["up_back"], ramp.BASE)
        c.r(*T["up_top"], ramp.LT)
    elif tier == "gold":
        c.framed(*T["step1_top"], ramp.LT, ramp.BASE)
        c.r(*T["step1_side"], ramp.BASE)
        x, y, w, h = T["step2_top"]
        c.framed(x, y, w, h, ramp.LT, ramp.BASE)
        c.r(x + 2, y + 2, 4, 4, ramp.DEEP)          # turntable disc
        for (dx, dy) in ((2, 2), (5, 2), (2, 5), (5, 5)):
            c.p(x + dx, y + dy, ramp.DK)
        c.r(*T["step2_side"], ramp.BASE)
        c.r(*T["step2_side_e"], ramp.BASE)
        c.r(*T["root"], ramp.DK)
        x, y, w, h = T["riser_front"]
        for i in range(h):                          # three stepped leaves, lit from the top
            c.p(x, y + i, ramp.LT if i % 2 == 0 else ramp.DK)
        x, y, w, h = T["riser_outer"]
        c.r(x, y, w, h, ramp.BASE)
        for i in range(0, h, 2):
            c.p(x + 1, y + i, ramp.LT)
        x, y, w, h = T["riser_inner"]
        c.r(x, y, w, h, ramp.DK)
        for i in range(0, h, 2):
            c.p(x, y + i, ramp.BASE)
        c.r(*T["riser_back"], ramp.BASE)
        x, y, w, h = T["tip_front"]
        c.r(x, y, w, h, ramp.LT)
        c.p(x, y, ramp.HI)
        x, y, w, h = T["tip_outer"]
        c.r(x, y, w, h, ramp.LT)
        c.r(x, y, w, 1, ramp.HI)
        c.r(*T["tip_inner"], ramp.BASE)
        c.r(*T["tip_top"], ramp.HI)
        c.r(*T["tip_back"], ramp.BASE)
    else:  # nero
        x, y, w, h = T["plate_top"]
        c.r(x, y, w, h, ramp.DK)
        foot = L.nero_footprint()
        for (fx, fz) in foot:
            c.p(x + fx - 2, y + fz - 2, ramp.LT)
        # one inner hex line and the dark lens seat
        for (fx, fz) in foot:
            n = sum(((fx + dx, fz + dz) in foot) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if n < 4:
                c.p(x + fx - 2, y + fz - 2, ramp.BASE)
        c.r(x + 3, y + 3, 2, 2, ramp.DEEP)
        c.r(*T["plate_side"], ramp.BASE)
        c.r(*T["node"], ramp.LT)
    return c


def glow_layer(tier, colour=None):
    """One 32x32 glow frame (alpha 0 outside the lit texels)."""
    c = Canvas(32, 32)
    ramp = RAMPS[tier]
    T = L.TIER_LAYOUT[tier]
    if tier == "silver":
        x, y, w, h = T["glow_tips"]
        c.p(x, y, ramp.GLOW)
        c.p(x + w - 1, y, ramp.GLOW)
    elif tier == "gold":
        c.r(*T["step2_side"], ramp.GLOW)
        x, y, w, h = T["step2_side_e"]
        c.r(x + 2, y, 6, 1, ramp.GLOW)              # east face: u runs south -> north
    elif tier == "nero":
        # sides: the rear corner seam and the hex pip as the centre seam
        x, y, w, h = L.SIDE
        c.r(x, y, 1, h, colour)
        c.art(x + L.HEX_PIP_COL, y, L.HEX_PIP, {"X": colour})
        x, y, w, h = L.BACK
        c.r(x, y, 1, h, colour)
        c.r(x + w - 1, y, 1, h, colour)
        c.art(x + L.HEX_PIP_COL, y, L.HEX_PIP, {"X": colour})
        # stage floor: the outline of the hex plate
        foot = L.nero_footprint()
        x, y, w, h = L.STAGE
        for fx in range(12):
            for fz in range(12):
                if (fx, fz) in foot:
                    continue
                if any(((fx + dx, fz + dz) in foot) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    c.p(x + fx, y + fz, colour)
        c.r(*T["lens_top"], PRISM_MID)
        c.r(*T["lens_side"], PRISM_MID)
    return c


def nero_glow_strip():
    strip = Canvas(32, 256)
    for i, colour in enumerate(prism_frames()):
        strip.blit(glow_layer("nero", colour), 0, i * 32)
    return strip


def build():
    """name -> Canvas for textures/block."""
    out = {}
    for t in TIERS:
        out["trophy_%s" % t] = tier_sheet(t)
        out["trophy_%s_replica" % t] = tier_sheet(t, replica=True)
    out["trophy_silver_glow"] = glow_layer("silver")
    out["trophy_gold_glow"] = glow_layer("gold")
    out["trophy_nero_glow"] = nero_glow_strip()
    return out


ANIM_MCMETA = '{\n  "animation": {\n    "frametime": 5,\n    "interpolate": true\n  }\n}\n'
