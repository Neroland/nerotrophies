#!/usr/bin/env python3
"""Write the NeroTrophies block models, item models, item-model definitions and blockstates.

The element lists below are authored by hand; this tool only spares typing the UV arithmetic and
keeps the UVs tied to the texture layout in layout.py.

    python3 tools/art/gen_trophy_models.py            write the JSON
    python3 tools/art/gen_trophy_models.py --check    compare with the files on disk

Conventions: a model's front is north; blockstates rotate it. Tint index 0 = finish, 1 = tier,
2 = trim accent. Glow parts are separate elements with shade off and light emission 15.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import layout as L  # noqa: E402
from palette import DEFAULT_FINISH, STEEL_DK, TIERS, TRIM_STYLES  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "nerotrophies")
NS = "nerotrophies"
E = 0.01          # inflation of overlay and glow elements
FACING_Y = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def tex(name):
    return "%s:block/%s" % (NS, name)


def uv32(rect, flip_u=False, flip_v=False):
    """Texel rect on a 32x32 sheet -> model UV."""
    x, y, w, h = rect
    u1, v1, u2, v2 = x / 2, y / 2, (x + w) / 2, (y + h) / 2
    if flip_u:
        u1, u2 = u2, u1
    if flip_v:
        v1, v2 = v2, v1
    return [u1, v1, u2, v2]


def sub(rect, dx, dy, w, h):
    return (rect[0] + dx, rect[1] + dy, w, h)


def f(texture, uv, tint=None, cull=None):
    out = {"texture": texture, "uv": [round(v, 3) for v in uv]}
    if tint is not None:
        out["tintindex"] = tint
    if cull is not None:
        out["cullface"] = cull
    return out


def el(frm, to, faces, glow=False, shade=True, rotation=None, name=None):
    out = {}
    if name:
        out["name"] = name
    out["from"] = frm
    out["to"] = to
    if rotation:
        out["rotation"] = rotation
    if glow:
        out["shade"] = False
        out["light_emission"] = 15
    elif not shade:
        out["shade"] = False
    out["faces"] = faces
    return out


def model(textures, elements, render_type=None, parent="minecraft:block/block"):
    out = {"parent": parent}
    if render_type:
        out["render_type"] = "minecraft:" + render_type
    out["textures"] = textures
    out["elements"] = elements
    return out


def mirror_x(elements):
    """Mirror a list of elements across x = 8 (left part -> right part)."""
    out = []
    swap = {"east": "west", "west": "east"}
    for e in elements:
        m = json.loads(json.dumps(e))
        (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
        m["from"], m["to"] = [16 - x2, y1, z1], [16 - x1, y2, z2]
        faces = {}
        for side, face in m["faces"].items():
            if side in ("north", "south", "up", "down"):
                u1, v1, u2, v2 = face["uv"]
                face["uv"] = [u2, v1, u1, v2]
            faces[swap.get(side, side)] = face
        m["faces"] = faces
        out.append(m)
    return out


# ------------------------------------------------------------------ trophies
def trophy_base(s):
    return [
        el([2, 0, 2], [14, 1, 14], {
            "north": f(s, uv32(L.FRONT_FOOT)), "south": f(s, uv32(L.BACK_FOOT)),
            "east": f(s, uv32(L.SIDE_FOOT)), "west": f(s, uv32(L.SIDE_FOOT, flip_u=True)),
            "down": f(s, uv32(L.FOOT_BOTTOM), cull="down")}, name="foot"),
        el([2, 1, 2], [14, 4, 14], {
            "north": f(s, uv32(L.PLATE)), "south": f(s, uv32(L.BACK)),
            "east": f(s, uv32(L.SIDE)), "west": f(s, uv32(L.SIDE, flip_u=True)),
            "up": f(s, uv32(L.STAGE))}, name="name block"),
    ]


def ring_faces(s, side_rect, top_rect=None):
    side = f(s, uv32(side_rect))
    faces = {"north": side, "south": side, "east": side, "west": side}
    if top_rect:
        faces["up"] = f(s, uv32(top_rect))
    return faces


def trophy_elements(tier, replica):
    s, g = "#sheet", "#glow"
    T = L.TIER_LAYOUT[tier]
    els = trophy_base(s)
    if tier == "bronze":
        els.append(el([5, 4, 5], [11, 5, 11], ring_faces(s, T["pad_side"], T["pad_top"]), name="inset pad"))
        clip, clip1 = uv32(T["clip"]), uv32(sub(T["clip"], 0, 0, 1, 1))
        els.append(el([7, 5, 9], [9, 6, 10], {
            "north": f(s, clip), "south": f(s, clip), "up": f(s, clip),
            "east": f(s, clip1), "west": f(s, clip1)}, name="mount clip"))
    elif tier == "silver":
        els.append(el([3, 4, 3], [13, 6, 13], ring_faces(s, T["step_side"], T["step_top"]), name="step"))
        els.append(el([5, 6, 5], [11, 7, 11], ring_faces(s, T["pad_side"], T["pad_top"]), name="pad"))
        els.append(el([3, 6, 7], [4, 14, 9], {
            "north": f(s, uv32(T["up_front"])), "south": f(s, uv32(T["up_back"])),
            "west": f(s, uv32(T["up_outer"], flip_u=True)), "east": f(s, uv32(T["up_inner"])),
            "up": f(s, uv32(T["up_top"]))}, name="upright west"))
        els.append(el([12, 6, 7], [13, 14, 9], {
            "north": f(s, uv32(T["up_front"])), "south": f(s, uv32(T["up_back"])),
            "east": f(s, uv32(T["up_outer"])), "west": f(s, uv32(T["up_inner"])),
            "up": f(s, uv32(T["up_top"]))}, name="upright east"))
        if not replica:
            els.append(el([3 - E, 13, 7 - E], [13 + E, 14 + E, 9 + E], {
                "up": f(g, uv32(T["glow_tips"])),
                "north": f(g, uv32(sub(T["glow_tips"], 0, 0, 10, 1)))}, glow=True, name="tip glow"))
    elif tier == "gold":
        els.append(el([3, 4, 3], [13, 5, 13], ring_faces(s, T["step1_side"], T["step1_top"]), name="step 1"))
        els.append(el([4, 5, 4], [12, 6, 12], {
            "north": f(s, uv32(T["step2_side"])), "south": f(s, uv32(T["step2_side"])),
            "east": f(s, uv32(T["step2_side_e"])), "west": f(s, uv32(T["step2_side_e"], flip_u=True)),
            "up": f(s, uv32(T["step2_top"]))}, name="step 2"))
        root, root_s = uv32(T["root"]), uv32(sub(T["root"], 0, 0, 2, 1))
        for (x1, x2, outer, inner, angle, pivot, tag) in ((2, 4, "west", "east", -22.5, 2, "west"),
                                                         (12, 14, "east", "west", 22.5, 14, "east")):
            rx1, rx2 = (1, 2) if tag == "west" else (14, 15)
            flip = tag == "west"
            els.append(el([x1, 6, 7], [x2, 7, 9], {
                "north": f(s, root_s), "south": f(s, root_s), "east": f(s, root_s), "west": f(s, root_s),
                "up": f(s, root), "down": f(s, root)}, name="arm root " + tag))
            els.append(el([rx1, 7, 7], [rx2, 12, 9], {
                "north": f(s, uv32(T["riser_front"])), "south": f(s, uv32(T["riser_back"])),
                outer: f(s, uv32(T["riser_outer"], flip_u=flip)),
                inner: f(s, uv32(T["riser_inner"], flip_u=not flip)),
                "down": f(s, uv32(sub(T["root"], 0, 0, 1, 2)))}, name="arm riser " + tag))
            els.append(el([rx1, 12, 7], [rx2, 15, 9], {
                "north": f(s, uv32(T["tip_front"])), "south": f(s, uv32(T["tip_back"])),
                outer: f(s, uv32(T["tip_outer"], flip_u=flip)),
                inner: f(s, uv32(T["tip_inner"], flip_u=not flip)),
                "up": f(s, uv32(T["tip_top"]))},
                rotation={"origin": [pivot, 12, 8], "axis": "z", "angle": angle}, name="arm tip " + tag))
        if not replica:
            els.append(el([4 - E, 5, 4 - E], [12 + E, 6, 12 + E], {
                "north": f(g, uv32(T["step2_side"])),
                "east": f(g, uv32(T["step2_side_e"])),
                "west": f(g, uv32(T["step2_side_e"], flip_u=True))}, glow=True, name="step-edge glow"))
    else:
        side = T["plate_side"]
        for (x1, x2, z1, z2, tag) in ((4, 12, 6, 10, "a"), (5, 11, 5, 11, "b"), (6, 10, 4, 12, "c")):
            ns = f(s, uv32(sub(side, 0, 0, x2 - x1, 1)))
            ew = f(s, uv32(sub(side, 0, 0, z2 - z1, 1)))
            top = f(s, uv32(sub(T["plate_top"], x1 - 4, z1 - 4, x2 - x1, z2 - z1)))
            els.append(el([x1, 4, z1], [x2, 5, z2],
                          {"north": ns, "south": ns, "east": ew, "west": ew, "up": top}, name="hex plate " + tag))
        node = f(s, uv32(T["node"]))
        for (x1, z1, x2, z2) in ((7, 4, 9, 5), (4, 9, 5, 10), (11, 9, 12, 10)):
            els.append(el([x1, 5, z1], [x2, 6, z2],
                          {k: node for k in ("north", "south", "east", "west", "up")}, name="emitter node"))
        if not replica:
            lens = f(g, uv32(T["lens_side"]))
            els.append(el([7, 5, 7], [9, 6, 9], {
                "north": lens, "south": lens, "east": lens, "west": lens,
                "up": f(g, uv32(T["lens_top"]))}, glow=True, name="lens"))
            els.append(el([2 - E, 1, 2 - E], [14 + E, 4 + E, 14 + E], {
                "east": f(g, uv32(L.SIDE)), "west": f(g, uv32(L.SIDE, flip_u=True)),
                "south": f(g, uv32(L.BACK)), "up": f(g, uv32(L.STAGE))}, glow=True, name="seam glow"))
    return els


def trophy_model(tier, replica):
    sheet = tex("trophy_%s%s" % (tier, "_replica" if replica else ""))
    textures = {"particle": sheet, "sheet": sheet}
    if not replica and tier != "bronze":
        textures["glow"] = tex("trophy_%s_glow" % tier)
    return model(textures, trophy_elements(tier, replica), "cutout")


def ghost_model():
    t = "#ghost"
    return model({"particle": tex("trophy_ghost"), "ghost": tex("trophy_ghost")}, [
        el([2, 0, 2], [14, 4, 14], {k: f(t, [0, 0, 12, 4]) for k in ("north", "south", "east", "west")}
           | {"up": f(t, [0, 0, 12, 12])}, shade=False, name="base"),
        el([5, 4, 5], [11, 5, 11], {k: f(t, [0, 4, 6, 5]) for k in ("north", "south", "east", "west")}
           | {"up": f(t, [0, 0, 6, 6])}, shade=False, name="stage"),
        el([4, 9, 8], [12, 13, 8], {"north": f(t, [0, 0, 8, 4]), "south": f(t, [0, 0, 8, 4])},
           shade=False, name="marker"),
    ], "translucent")


# ------------------------------------------------------------------ pedestal
def elev(x1, y1, x2, y2):
    """UV of a side face on an elevation texture (texel row = 16 - y)."""
    return [x1, 16 - y2, x2, 16 - y1]


def sides(t, x1, y1, x2, y2, tint=None):
    face = f(t, elev(x1, y1, x2, y2), tint)
    return {"north": face, "south": face, "east": face, "west": face}


def pedestal_model():
    s, top, bot = "#side", "#top", "#bottom"
    plan = lambda a, b: [a, a, b, b]  # noqa: E731
    tint_side = f("#tint", [4, 6, 12, 12], 0)
    ring_side = f("#ring", [3, 1, 13, 2], 1)
    pl = "#plaque"
    return model({
        "particle": tex("display_pedestal_side"), "side": tex("display_pedestal_side"),
        "top": tex("display_pedestal_top"), "bottom": tex("display_pedestal_bottom"),
        "tint": tex("display_pedestal_tint"), "ring": tex("display_pedestal_ring"),
        "plaque": tex("display_pedestal_plaque")}, [
        el([2, 0, 2], [14, 2, 14], sides(s, 2, 0, 14, 2)
           | {"up": f(top, plan(2, 14)), "down": f(bot, plan(2, 14), cull="down")}, name="foot"),
        el([3, 2, 3], [13, 3, 13], sides(s, 3, 2, 13, 3), name="base collar"),
        el([4, 3, 4], [12, 11, 12], sides(s, 4, 3, 12, 11), name="shaft"),
        el([4 - E, 4, 4 - E], [12 + E, 10, 12 + E], {
            "north": f("#tint", [4, 0, 12, 6], 0), "south": tint_side, "east": tint_side, "west": tint_side},
           name="finish insets"),
        el([3, 11, 3], [13, 12, 13], sides(s, 3, 11, 13, 12) | {"down": f(top, plan(3, 13))}, name="neck collar"),
        el([2, 12, 2], [14, 14, 14], sides(s, 2, 12, 14, 14)
           | {"up": f(top, plan(2, 14)), "down": f(bot, plan(2, 14))}, name="capital"),
        el([4, 14, 4], [12, 15, 12], sides(s, 4, 14, 12, 15) | {"up": f(top, plan(4, 12))}, name="turntable"),
        el([1, 7, 3], [15, 11, 4], {
            "north": f(pl, [1, 5, 15, 9]), "south": f(pl, [1, 10, 15, 14]),
            "up": f(pl, [1, 4, 15, 5]), "down": f(pl, [1, 9, 15, 10]),
            "east": f(pl, [0, 5, 1, 9]), "west": f(pl, [0, 5, 1, 9])}, name="plaque"),
        el([3, 14, 3], [13, 15, 13], {
            "north": ring_side, "south": ring_side, "east": ring_side, "west": ring_side,
            "up": f("#ring", plan(3, 13), 1)}, shade=False, name="tier ring"),
    ], "cutout")


# ------------------------------------------------------------------ cabinets
def cabinet_textures(prefix):
    return {
        "particle": tex(prefix + "_outer"), "outer": tex(prefix + "_outer"), "tint": tex(prefix + "_outer_tint"),
        "velvet": tex("trophy_cabinet_velvet"), "shelf": tex("trophy_cabinet_shelf"),
        "glass": tex("trophy_cabinet_glass")}


def cabinet_body():
    o, v, sh = "#outer", "#velvet", "#shelf"
    edge = f(o, [0, 0, 16, 1])
    return [
        el([0, 0, 15], [16, 16, 16], {
            "north": f(v, [0, 0, 16, 16]), "south": f(o, [0, 0, 16, 16], cull="south"),
            "up": edge, "down": edge, "east": f(o, [0, 0, 1, 16]), "west": f(o, [0, 0, 1, 16])}, name="back"),
        el([0, 0, 15.5], [16, 16, 16 + E], {"south": f("#tint", [0, 0, 16, 16], 0)}, name="back finish"),
        el([0, 0, 0], [16, 1, 15], {
            "up": f(v, [0, 0, 16, 15]), "down": f(sh, [0, 14, 16, 15], cull="down"),
            "north": f(o, [0, 15, 16, 16]), "east": f(o, [0, 15, 15, 16]), "west": f(o, [0, 15, 15, 16])},
           name="floor"),
        el([0, 15, 0], [16, 16, 15], {
            "up": f(o, [0, 0, 16, 15], cull="up"), "down": f(o, [0, 1, 16, 16]),
            "north": edge, "east": f(o, [0, 0, 15, 1]), "west": f(o, [0, 0, 15, 1])}, name="roof"),
        el([0, 15.5, 0], [16, 16 + E, 15], {"up": f("#tint", [0, 0, 16, 15], 0)}, name="roof finish"),
        el([0, 1, 2], [16, 2, 15], {"up": f(sh, [0, 1, 16, 14]), "north": f(sh, [0, 0, 16, 1])}, name="lower deck"),
        el([0, 8, 2], [16, 9, 15], {
            "up": f(sh, [0, 1, 16, 14]), "down": f(sh, [0, 1, 16, 14]), "north": f(sh, [0, 0, 16, 1])},
           name="upper shelf"),
        el([2, 14.5, 2], [14, 15, 3], {
            "north": f(sh, [2, 15, 14, 16]), "south": f(sh, [2, 15, 14, 16]), "down": f(sh, [2, 15, 14, 16])},
           shade=False, name="light strip"),
        el([0, 1, 1], [16, 15, 1], {"north": f("#glass", [0, 1, 16, 15])}, name="front glass"),
    ]


def cabinet_side_left():
    o = "#outer"
    return [
        el([0, 1, 1], [1, 4, 15], {
            "west": f(o, [1, 12, 15, 15], cull="west"), "east": f(o, [1, 12, 15, 15]),
            "up": f(o, [1, 0, 15, 1]), "north": f(o, [0, 12, 1, 15])}, name="dado"),
        el([0 - E, 1, 1], [0.5, 4, 15], {"west": f("#tint", [1, 10, 15, 13], 0)}, name="dado finish"),
        el([0.05, 4, 1], [0.05, 15, 15], {
            "west": f("#glass", [1, 1, 15, 12]), "east": f("#glass", [1, 1, 15, 12])}, name="side glass"),
    ]


def wall_cabinet_body():
    o, v, sh = "#outer", "#velvet", "#shelf"
    return [
        el([0, 2, 15], [16, 14, 16], {
            "north": f(v, [0, 2, 16, 14]), "south": f(o, [0, 4, 16, 16], cull="south"),
            "up": f(o, [0, 5, 16, 6]), "down": f(o, [0, 5, 16, 6]),
            "east": f(o, [15, 4, 16, 16]), "west": f(o, [15, 4, 16, 16])}, name="back"),
        el([0, 2, 10], [16, 3, 15], {
            "down": f(o, [0, 0, 16, 5]), "up": f(v, [0, 10, 16, 15]),
            "north": f(o, [0, 4, 16, 5]), "east": f(o, [0, 4, 5, 5]), "west": f(o, [0, 4, 5, 5])}, name="floor"),
        el([0, 2 - E, 10], [16, 2.5, 15], {"down": f("#tint", [0, 0, 16, 5], 0)}, name="floor finish"),
        el([0, 13, 10], [16, 14, 15], {
            "up": f(o, [0, 0, 16, 5]), "down": f(o, [0, 5, 16, 10]),
            "north": f(o, [0, 0, 16, 1]), "east": f(o, [0, 0, 5, 1]), "west": f(o, [0, 0, 5, 1])}, name="roof"),
        el([0, 13.5, 10], [16, 14 + E, 15], {"up": f("#tint", [0, 0, 16, 5], 0)}, name="roof finish"),
        el([0, 3, 10], [16, 4, 11], {
            "north": f(sh, [0, 0, 16, 1]), "up": f(sh, [0, 0, 16, 1]), "south": f(sh, [0, 1, 16, 2])},
           name="shelf lip"),
        el([0, 4, 10], [16, 13, 10], {"north": f("#glass", [0, 4, 16, 13])}, name="front glass"),
    ]


def wall_cabinet_side_left():
    return [
        el([0.05, 3, 10], [0.05, 13, 15], {
            "west": f("#glass", [10, 3, 15, 13]), "east": f("#glass", [10, 3, 15, 13])}, name="side glass"),
    ]


# frame parts: (from, to, north uv on the trim sheet, flips)
def frame_geometry(wall):
    z1, z2 = (9, 10) if wall else (0, 1)
    zs, zc = z1 - 0.5, z1 - 0.25          # studs stand 0.5 proud, the crown 0.25
    if wall:
        yb, yt, post = 2, 14, (4, 12)
    else:
        yb, yt, post = 0, 16, (2, 14)
    pr = [0, post[0] - yb + 1, 2, post[1] - yb + 1] if not wall else [0, 4, 2, 12]
    stud = [12, 12, 16, 16]
    return {
        "edge_top": ([0, yt - 2, z1], [16, yt, z2], [0, 0, 16, 2]),
        "edge_bottom": ([0, yb, z1], [16, yb + 2, z2], [0, 2, 16, 0]),
        "edge_left": ([0, post[0], z1], [2, post[1], z2], [pr[2], pr[1], pr[0], pr[3]]),
        "edge_right": ([14, post[0], z1], [16, post[1], z2], pr),
        "corner_tl": ([0, yt - 4, zs], [4, yt, z2], [16, 12, 12, 16]),
        "corner_tr": ([12, yt - 4, zs], [16, yt, z2], stud),
        "corner_bl": ([0, yb, zs], [4, yb + 4, z2], [16, 16, 12, 12]),
        "corner_br": ([12, yb, zs], [16, yb + 4, z2], [12, 16, 16, 12]),
        "crown": ([1, yt - 2, zc], [15, yt + 2, z2], [2, 2, 16, 6]),
    }


PARTS = ("edge_top", "edge_bottom", "edge_left", "edge_right",
         "corner_tl", "corner_tr", "corner_bl", "corner_br", "crown")


def frame_part(wall, part, lit=False, tinted=True):
    frm, to, uv = frame_geometry(wall)[part]
    t = "#trim"
    u1, v1, u2, v2 = uv
    lo_u, lo_v = min(u1, u2), min(v1, v2)
    hi_u, hi_v = max(u1, u2), max(v1, v2)
    faces = {"north": f(t, uv)}
    if part.startswith("edge_top") or part.startswith("edge_bottom"):
        outer, inner = [lo_u, 0, hi_u, 1], [lo_u, 1, hi_u, 2]
        faces["up"] = f(t, outer if part == "edge_top" else inner)
        faces["down"] = f(t, inner if part == "edge_top" else outer)
    elif part.startswith("edge_"):
        faces["east"] = f(t, [0, lo_v, 1, hi_v])
        faces["west"] = f(t, [0, lo_v, 1, hi_v])
    else:
        faces["south"] = f(t, [u2, v1, u1, v2])
        faces["up"] = f(t, [lo_u, lo_v, hi_u, lo_v + 1])
        faces["down"] = f(t, [lo_u, hi_v - 1, hi_u, hi_v])
        faces["east"] = f(t, [lo_u, lo_v, lo_u + 1, hi_v])
        faces["west"] = f(t, [hi_u - 1, lo_v, hi_u, hi_v])
    tint = 2 if tinted else None
    acc = {"north": f("#accent", uv, tint)}
    if part == "crown":
        acc["south"] = f("#accent", [u2, v1, u1, v2], tint)
    return [
        el(frm, to, faces, name=part),
        el([frm[0] - E, frm[1] - E, frm[2] - E], [to[0] + E, to[1] + E, to[2] + E], acc, glow=lit,
           name=part + " accent"),
    ]


def trim_textures(style):
    return {"trim": tex("trim_" + style), "accent": tex("trim_%s_accent" % style)}


def frame_models(out):
    for wall, cab in ((False, "cabinet"), (True, "wall_cabinet")):
        for part in PARTS:
            base = "frame/%s_%s" % (cab, part)
            out["models/block/%s.json" % base] = {
                "parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                "textures": {"particle": "#trim"}, "elements": frame_part(wall, part)}
            for style in TRIM_STYLES:
                path = "models/block/%s_%s.json" % (base, style)
                if style in ("neon", "prism"):
                    # lit accents (and prism's untinted animated strip) need their own elements
                    out[path] = model({"particle": tex("trim_" + style), **trim_textures(style)},
                                      frame_part(wall, part, lit=True, tinted=style != "prism"), "translucent")
                else:
                    out[path] = {"parent": "%s:block/%s" % (NS, base),
                                 "textures": {"particle": tex("trim_" + style), **trim_textures(style)}}


def inventory_model(wall):
    prefix = "wall_cabinet" if wall else "trophy_cabinet"
    left = wall_cabinet_side_left() if wall else cabinet_side_left()
    els = (wall_cabinet_body() if wall else cabinet_body()) + left + mirror_x(left)
    for part in PARTS:
        els += frame_part(wall, part)
    return model({**cabinet_textures(prefix), **trim_textures("plate")}, els, "translucent")


# ------------------------------------------------------------------ plaque, spotlight
def plaque_model():
    b, s = "#back", "#strip"
    return model({
        "particle": tex("trophy_plaque_back"), "back": tex("trophy_plaque_back"),
        "tint": tex("trophy_plaque_back_tint"), "medallion": tex("trophy_plaque_medallion"),
        "strip": tex("trophy_plaque_strip")}, [
        el([2, 0, 15], [14, 16, 16], {
            "north": f(b, [2, 0, 14, 16]), "south": f(b, [2, 0, 14, 16], cull="south"),
            "east": f(b, [0, 0, 1, 16]), "west": f(b, [0, 0, 1, 16]),
            "up": f(b, [2, 0, 14, 1]), "down": f(b, [2, 15, 14, 16])}, name="backplate"),
        el([2, 0, 15 - E], [14, 16, 15.5], {"north": f("#tint", [2, 0, 14, 16], 0)}, name="finish frame"),
        el([4, 8, 14], [12, 16, 15], {"north": f("#medallion", [4, 0, 12, 8], 1)}, name="medallion ring"),
        el([1, 3, 14], [15, 5, 15], {
            "north": f(s, [1, 11, 15, 13]), "up": f(s, [1, 10, 15, 11]), "down": f(s, [1, 13, 15, 14]),
            "east": f(s, [1, 11, 2, 13]), "west": f(s, [1, 11, 2, 13])}, name="title strip"),
        el([1, 0, 14], [15, 2, 15], {
            "north": f(s, [1, 14, 15, 16]), "up": f(s, [1, 13, 15, 14]), "down": f(s, [1, 13, 15, 14]),
            "east": f(s, [1, 14, 2, 16]), "west": f(s, [1, 14, 2, 16])}, name="name strip"),
    ], "cutout")


def spotlight_model(wall):
    h, lens, cone = "#housing", "#lens", "#cone"
    side = f(h, [0, 0, 6, 4])
    lens_side = f(lens, [0, 4, 4, 5])
    plate, plate_edge = f(h, [12, 0, 16, 4]), f(h, [12, 4, 16, 5])
    if not wall:
        arm = f(h, [12, 6, 14, 9])
        els = [
            el([6, 15, 6], [10, 16, 10], {
                "up": f(h, [12, 0, 16, 4], cull="up"), "down": plate,
                "north": plate_edge, "south": plate_edge, "east": plate_edge, "west": plate_edge}, name="mount"),
            el([7, 12, 7], [9, 15, 9], {k: arm for k in ("north", "south", "east", "west")}, name="arm"),
            el([5, 8, 5], [11, 12, 11], {
                "north": side, "south": side, "east": side, "west": side,
                "up": f(h, [6, 0, 12, 6]), "down": f(h, [0, 6, 6, 12])}, name="housing"),
            el([6, 7, 6], [10, 8, 10], {
                "down": f(lens, [0, 0, 4, 4]), "north": lens_side, "south": lens_side,
                "east": lens_side, "west": lens_side}, glow=True, name="lens"),
            el([0, 0, 8], [16, 7, 8], {"north": f(cone, [0, 0, 16, 7]), "south": f(cone, [0, 0, 16, 7])},
               shade=False, name="cone a"),
            el([8, 0, 0], [8, 7, 16], {"east": f(cone, [0, 0, 16, 7]), "west": f(cone, [0, 0, 16, 7])},
               shade=False, name="cone b"),
            el([5, 6.9, 5.5], [11, 6.9, 10.5], {"down": f(cone, [0, 11, 6, 16]), "up": f(cone, [0, 11, 6, 16])},
               shade=False, name="halo cap"),
        ]
    else:
        arm_side, arm_top = f(h, [6, 8, 11, 10]), f(h, [12, 6, 14, 11])
        els = [
            el([6, 6, 15], [10, 10, 16], {
                "south": f(h, [12, 0, 16, 4], cull="south"), "north": plate,
                "up": plate_edge, "down": plate_edge, "east": f(h, [12, 0, 13, 4]), "west": f(h, [12, 0, 13, 4])},
               name="mount"),
            el([7, 7, 10], [9, 9, 15], {"east": arm_side, "west": arm_side, "up": arm_top, "down": arm_top},
               name="arm"),
            el([5, 5, 4], [11, 9, 10], {
                "north": side, "south": side, "east": side, "west": side,
                "up": f(h, [6, 0, 12, 6]), "down": f(h, [0, 6, 6, 12])}, name="housing"),
            el([6, 4, 5], [10, 5, 9], {
                "down": f(lens, [0, 0, 4, 4]), "north": lens_side, "south": lens_side,
                "east": lens_side, "west": lens_side}, glow=True, name="lens"),
            el([0, 0, 7], [16, 4, 7], {"north": f(cone, [0, 7, 16, 11]), "south": f(cone, [0, 7, 16, 11])},
               shade=False, name="cone a"),
            el([8, 0, -1], [8, 4, 15], {"east": f(cone, [0, 7, 16, 11]), "west": f(cone, [0, 7, 16, 11])},
               shade=False, name="cone b"),
            el([5, 3.9, 4.5], [11, 3.9, 9.5], {"down": f(cone, [0, 11, 6, 16]), "up": f(cone, [0, 11, 6, 16])},
               shade=False, name="halo cap"),
        ]
    return model({
        "particle": tex("trophy_spotlight"), "housing": tex("trophy_spotlight"),
        "lens": tex("trophy_spotlight_lens_glow"), "cone": tex("trophy_spotlight_cone")}, els, "translucent")


# ------------------------------------------------------------------ items
def generated(name):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (NS, name)}}


TROPHY_CASES = [t for t in TIERS] + [t + "_replica" for t in TIERS]


def item_models(out):
    for name in TROPHY_CASES + ["trophy_unknown", "trophy_book", "trophy_plaque", "trophy_spotlight"]:
        key = name if name.startswith("trophy_") else "trophy_" + name
        out["models/item/%s.json" % key] = generated(key)
    out["models/item/display_pedestal.json"] = {"parent": NS + ":block/display_pedestal"}
    out["models/item/trophy_cabinet.json"] = {"parent": NS + ":block/trophy_cabinet_inventory"}
    out["models/item/wall_cabinet.json"] = {"parent": NS + ":block/wall_cabinet_inventory"}
    # 1.21.1 only (item id -> models/item/<id>.json): the tier dispatch as predicate overrides.
    # 1.21.4 and later read items/trophy.json instead and ignore this file.
    trophy = generated("trophy_unknown")
    trophy["overrides"] = [
        {"predicate": {NS + ":trophy_tier": (i + 1) / 10}, "model": "%s:item/trophy_%s" % (NS, case)}
        for i, case in enumerate(TROPHY_CASES)]
    out["models/item/trophy.json"] = trophy


def plain(model_id, tints=None):
    m = {"type": "minecraft:model", "model": model_id}
    if tints:
        m["tints"] = [{"type": "minecraft:constant", "value": t} for t in tints]
    return {"model": m}


def item_definitions(out):
    """assets/<ns>/items/*.json, read by 1.21.4 and later (26.x included); ignored by 1.21.1."""
    out["items/trophy.json"] = {"model": {
        "type": "minecraft:select", "property": NS + ":trophy_tier",
        "cases": [{"when": case, "model": {"type": "minecraft:model", "model": "%s:item/trophy_%s" % (NS, case)}}
                  for case in TROPHY_CASES],
        "fallback": {"type": "minecraft:model", "model": NS + ":item/trophy_unknown"}}}
    out["items/trophy_book.json"] = plain(NS + ":item/trophy_book")
    out["items/trophy_plaque.json"] = plain(NS + ":item/trophy_plaque")
    out["items/trophy_spotlight.json"] = plain(NS + ":item/trophy_spotlight")
    out["items/display_pedestal.json"] = plain(NS + ":item/display_pedestal", [DEFAULT_FINISH, STEEL_DK])
    out["items/trophy_cabinet.json"] = plain(NS + ":item/trophy_cabinet", [DEFAULT_FINISH])
    out["items/wall_cabinet.json"] = plain(NS + ":item/wall_cabinet", [DEFAULT_FINISH])


# ------------------------------------------------------------------ blockstates
def variant(model_name, y):
    v = {"model": "%s:block/%s" % (NS, model_name)}
    if y:
        v["y"] = y
    return v


def blockstates(out):
    variants = {}
    for facing, y in FACING_Y:
        for tier in TIERS:
            for replica in (False, True):
                key = "facing=%s,replica=%s,tier=%s" % (facing, str(replica).lower(), tier)
                variants[key] = variant("trophy_%s%s" % (tier, "_replica" if replica else ""), y)
    out["blockstates/trophy.json"] = {"variants": variants}
    for block in ("display_pedestal", "trophy_plaque"):
        out["blockstates/%s.json" % block] = {
            "variants": {"facing=%s" % facing: variant(block, y) for facing, y in FACING_Y}}
    spot = {"facing=down": variant("trophy_spotlight_ceiling", 0)}
    for facing, y in FACING_Y:
        spot["facing=%s" % facing] = variant("trophy_spotlight_wall", y)
    out["blockstates/trophy_spotlight.json"] = {"variants": spot}
    for block, cab in (("trophy_cabinet", "cabinet"), ("wall_cabinet", "wall_cabinet")):
        parts = []
        for facing, y in FACING_Y:
            parts.append({"when": {"facing": facing}, "apply": variant(block, y)})
            for side in ("left", "right"):
                parts.append({"when": {"facing": facing, side: "false"},
                              "apply": variant("%s_side_%s" % (block, side), y)})
            for style in TRIM_STYLES:
                for part in ("edge_top", "edge_bottom", "crown"):
                    parts.append({"when": {"facing": facing, "trim": style},
                                  "apply": variant("frame/%s_%s_%s" % (cab, part, style), y)})
                for side, tag in (("left", "l"), ("right", "r")):
                    for part in ("edge_" + side, "corner_t" + tag, "corner_b" + tag):
                        parts.append({"when": {"facing": facing, "trim": style, side: "false"},
                                      "apply": variant("frame/%s_%s_%s" % (cab, part, style), y)})
        out["blockstates/%s.json" % block] = {"multipart": parts}


def collect():
    out = {}
    for tier in TIERS:
        out["models/block/trophy_%s.json" % tier] = trophy_model(tier, False)
        out["models/block/trophy_%s_replica.json" % tier] = trophy_model(tier, True)
    out["models/block/trophy_ghost.json"] = ghost_model()
    out["models/block/display_pedestal.json"] = pedestal_model()
    out["models/block/trophy_cabinet.json"] = model(cabinet_textures("trophy_cabinet"), cabinet_body(), "translucent")
    left = cabinet_side_left()
    out["models/block/trophy_cabinet_side_left.json"] = model(cabinet_textures("trophy_cabinet"), left, "translucent")
    out["models/block/trophy_cabinet_side_right.json"] = model(
        cabinet_textures("trophy_cabinet"), mirror_x(left), "translucent")
    out["models/block/trophy_cabinet_inventory.json"] = inventory_model(False)
    out["models/block/wall_cabinet.json"] = model(cabinet_textures("wall_cabinet"), wall_cabinet_body(), "translucent")
    wleft = wall_cabinet_side_left()
    out["models/block/wall_cabinet_side_left.json"] = model(cabinet_textures("wall_cabinet"), wleft, "translucent")
    out["models/block/wall_cabinet_side_right.json"] = model(
        cabinet_textures("wall_cabinet"), mirror_x(wleft), "translucent")
    out["models/block/wall_cabinet_inventory.json"] = inventory_model(True)
    frame_models(out)
    out["models/block/trophy_plaque.json"] = plaque_model()
    out["models/block/trophy_spotlight_ceiling.json"] = spotlight_model(False)
    out["models/block/trophy_spotlight_wall.json"] = spotlight_model(True)
    item_models(out)
    item_definitions(out)
    blockstates(out)
    return {k: json.dumps(v, indent=2) + "\n" for k, v in out.items()}


def main(argv):
    files = collect()
    if "--check" in argv:
        bad = 0
        for rel, text in sorted(files.items()):
            path = os.path.join(ASSETS, rel)
            if not os.path.isfile(path) or open(path, encoding="utf-8").read() != text:
                bad += 1
                print("STALE " + rel)
        print("%d files checked, %d problems" % (len(files), bad))
        return 1 if bad else 0
    for rel, text in sorted(files.items()):
        path = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    print("wrote %d files" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
