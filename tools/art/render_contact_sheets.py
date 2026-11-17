#!/usr/bin/env python3
"""Proof sheets for the NeroTrophies art. Nothing written here ships.

    python3 tools/art/render_contact_sheets.py <output folder>

Parts the game draws in code (the icon miniature, the nero ring, nameplate text, tier-coloured
rings) are approximated here so the sheets show the finished look.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import iso  # noqa: E402
import tex_entity  # noqa: E402
import tex_gui  # noqa: E402
import tex_items  # noqa: E402
import tex_trophy  # noqa: E402
from palette import (DEFAULT_FINISH, GLASS, PANEL, PANEL_EDGE, PRISM_A, PRISM_B, PRISM_MID, RAMPS, REPLICA,  # noqa: E402
                     STEEL_DK, TEXT, TEXT_2, TIERS, TRIMS, TROPHY_ACCENT, TROPHY_ACCENT_BRIGHT, rgba, scale as dim)
from px import Canvas, pixels  # noqa: E402

BG = (58, 62, 72, 255)
BG_LIGHT = (198, 198, 198, 255)
LABEL = (232, 236, 244, 255)


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


class Sheet:
    def __init__(self, w, h, bg=BG):
        self.img = Image.new("RGBA", (w, h), bg)
        self.d = ImageDraw.Draw(self.img)

    def put(self, im, x, y):
        self.img.alpha_composite(im, (int(x), int(y)))

    def text(self, x, y, s, fill=LABEL):
        self.d.text((x, y), s, fill=fill)

    def save(self, folder, name):
        path = os.path.join(folder, name)
        self.img.convert("RGB").save(path)
        print("wrote", path, self.img.size)


def up(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


# ---------------------------------------------------------------- stand-ins for BER content
def sample_icon():
    """A neutral 16x16 flat item for the miniature."""
    c = Canvas(16, 16)
    art = ["................", ".......XX.......", "......XooX......", ".....XooooX.....",
           "....XoooHooX....", "...XoooHHoooX...", "..XoooHHooooxX..", ".XooooHoooooxxX.",
           ".XoooooooooxxxX.", "..XoooooooxxxX..", "...XoooooxxxX...", "....XoooxxxX....",
           ".....XoxxxX.....", "......XxxX......", ".......XX.......", "................"]
    c.art(0, 0, art, {"X": 0x1B3A4A, "o": 0x4FC3E8, "H": 0xD8F6FF, "x": 0x2A86B0})
    return c.img


def sample_block():
    c = Canvas(16, 16, 0x7A5A3A)
    c.frame(0, 0, 16, 16, 0x5A4028)
    c.r(3, 3, 10, 10, 0x9A7448)
    c.r(6, 6, 4, 4, 0xE8C070)
    return c.img


def ring_texture():
    c = Canvas(16, 16)
    rows = ["....XXXXXXXX....", "...X........X...", "..X..........X..", ".X............X."]
    for j, row in enumerate(rows):
        c.art(0, 2 + j, [row], {"X": 0xFFFFFF})
        c.art(0, 13 - j, [row], {"X": 0xFFFFFF})
    for y in range(6, 10):
        c.p(0, y, 0xFFFFFF)
        c.p(15, y, 0xFFFFFF)
    return c.img


STAGE_Y = {"bronze": 5, "silver": 7, "gold": 6, "nero": 8}


def miniature(tier, replica, block=False):
    """Extra quads: the icon standing on (or floating over) the stage."""
    y = STAGE_Y[tier]
    if replica and tier == "nero":
        y = 6
    extra = []
    if block:
        t = {"b": sample_block()}
        face = {"texture": "#b", "uv": [0, 0, 16, 16]}
        els = [{"from": [4.8, y, 4.8], "to": [11.2, y + 6.4, 11.2],
                "faces": {k: face for k in ("north", "south", "east", "west", "up")}}]
    else:
        t = {"i": sample_icon()}
        face = {"texture": "#i", "uv": [0, 0, 16, 16]}
        els = [{"from": [4, y, 8], "to": [12, y + 8, 8], "shade": False, "faces": {"north": face, "south": face}}]
    kw = {}
    if replica:
        kw = {"tints": {None: (150, 150, 150)}}
    extra.append((t, els, kw))
    if tier == "nero" and not replica:
        ring = {"texture": "#r", "uv": [0, 0, 16, 16], "tintindex": 1}
        els = [{"from": [2, 12, 2], "to": [14, 12, 14], "shade": False, "faces": {"up": ring, "down": ring},
                "rotation": {"origin": [8, 12, 8], "axis": "x", "angle": 15}}]
        extra.append(({"r": ring_texture()}, els, {"tints": {1: rgb(PRISM_MID)}}))
    return extra


# ---------------------------------------------------------------- tiers
def cvd(im, kind):
    """Simulate colour-vision deficiency (Machado-style matrices) or greyscale."""
    m = {
        "protanopia": (0.152, 1.053, -0.205, 0.115, 0.786, 0.099, -0.004, -0.048, 1.052),
        "deuteranopia": (0.367, 0.861, -0.228, 0.280, 0.673, 0.047, -0.012, 0.043, 0.969),
        "tritanopia": (1.256, -0.077, -0.179, -0.078, 0.931, 0.148, 0.005, 0.691, 0.304),
        "greyscale": (0.299, 0.587, 0.114) * 3,
    }[kind]
    rgbim = im.convert("RGB")
    out = rgbim.convert("RGB", (m[0], m[1], m[2], 0, m[3], m[4], m[5], 0, m[6], m[7], m[8], 0))
    return out.convert("RGBA")


def tier_strip(view, k, replica, block=False):
    cells = []
    for t in TIERS:
        name = "trophy_%s%s" % (t, "_replica" if replica else "")
        cells.append(iso.render(name, view, k, extra=miniature(t, replica, block),
                                bounds=iso_bounds(view)))
    w = sum(c.width for c in cells) + 12 * (len(cells) - 1)
    out = Image.new("RGBA", (w, max(c.height for c in cells)), (0, 0, 0, 0))
    x = 0
    for c in cells:
        out.alpha_composite(c, (x, out.height - c.height))
        x += c.width + 12
    return out


def iso_bounds(view, x=(0, 16), y=(0, 18), z=(0, 16)):
    pts = [view.project((a, b, c)) for a in x for b in y for c in z]
    return (min(p[0] for p in pts), min(p[1] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts))


def sheet_tiers(folder):
    rows = [("genuine, iso", iso.ISO, False, False), ("genuine, front", iso.FRONT, False, False),
            ("genuine, block miniature", iso.ISO, False, True),
            ("replica, iso", iso.ISO, True, False), ("replica, front", iso.FRONT, True, False)]
    strips = [(label, tier_strip(v, 10, rep, blk)) for (label, v, rep, blk) in rows]
    small = tier_strip(iso.FRONT, 1, False)
    small_r = tier_strip(iso.FRONT, 1, True)
    w = max(s.width for _, s in strips) + 40
    h = sum(s.height + 26 for _, s in strips) + 80
    sh = Sheet(w, h)
    y = 10
    sh.text(10, y, "TIERS  bronze / silver / gold / nero   (miniature, ring and text are code-drawn in game)")
    y += 18
    for label, s in strips:
        sh.text(10, y, label)
        sh.put(s, 20, y + 14)
        y += s.height + 26
    sh.text(10, y, "1x front (true texel size): genuine, replica")
    sh.put(small, 20, y + 14)
    sh.put(small_r, 40 + small.width, y + 14)
    sh.save(folder, "tiers.png")

    front = tier_strip(iso.FRONT, 7, False)
    isov = tier_strip(iso.ISO, 7, False)
    kinds = ("protanopia", "deuteranopia", "tritanopia", "greyscale")
    sh = Sheet(front.width + isov.width + 60, (front.height + 24) * len(kinds) + 20)
    y = 8
    for kind in kinds:
        sh.text(10, y, kind)
        for x, strip in ((20, front), (40 + front.width, isov)):
            base = Image.new("RGBA", strip.size, BG)
            base.alpha_composite(strip)
            sh.put(cvd(base, kind), x, y + 14)
        y += front.height + 24
    sh.save(folder, "tiers_cvd.png")


# ---------------------------------------------------------------- nameplates
FONT = dict(zip("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_", [
    "7E1111117E", "7F49494936", "3E41414122", "7F4141221C", "7F49494941", "7F09090901", "3E4149497A",
    "7F0808087F", "00417F4100", "2040413F01", "7F08142241", "7F40404040", "7F020C027F", "7F0408107F",
    "3E4141413E", "7F09090906", "3E4151215E", "7F09192946", "4649494931", "01017F0101", "3F4040403F",
    "1F2040201F", "3F4038403F", "6314081463", "0708700807", "6151494543", "3E5149453E", "00427F4000",
    "4261514946", "2141454B31", "1814127F10", "2745454539", "3C4A494930", "0171090503", "3649494936",
    "064949291E", "4040404040"]))


def text_bitmap(s):
    """Stand-in for the vanilla font: 5x7 glyphs, 6 px advance (16 W = 95 px, as in game)."""
    w = len(s) * 6 - 1
    im = Image.new("L", (w, 8), 0)
    for n, ch in enumerate(s):
        cols = bytes.fromhex(FONT[ch])
        for i, col in enumerate(cols):
            for j in range(7):
                if col >> j & 1:
                    im.putpixel((n * 6 + i, j), 255)
    return im


def plate(tier, state, name=None, res=96):
    """The 12x3 nameplate at res px per texel. state: name | blank | replica."""
    r = RAMPS[tier]
    c = Canvas(12, 3)
    if state == "replica":
        c.blit(tex_trophy.replica_cover(), 0, 0)
    else:
        c.r(0, 0, 12, 3, r.FRAME)
        c.r(1, 1, 10, 2, GLASS)
        if state == "blank":
            ink = dim(r.INK, 0.55)
            c.r(1, 2, 3, 1, ink)
            c.r(8, 2, 3, 1, ink)
            c.r(5, 1, 2, 2, ink)
    im = up(c.img, res)
    if state == "name":
        bm = text_bitmap(name)
        s = max(min(1 / 96, 0.625 / bm.width), 1 / 160)      # block per font px
        px = s * 16 * res                                    # screen px per font px
        tw, th = max(1, round(bm.width * px)), max(1, round(8 * px))
        glyphs = bm.resize((tw, th), Image.NEAREST)
        ink = Image.new("RGBA", (tw, th), rgba(r.INK))
        im.paste(ink, (round(6 * res - tw / 2), round(2 * res - th / 2)), glyphs)
    return im


def sheet_nameplates(folder):
    names = ["ACE", "STARFORGER", "NEBULA_WALKER_26", "WWWWWWWWWWWWWWWW"]
    states = [("name", n) for n in names] + [("blank", None), ("replica", None)]
    dist = [(2, 289), (6, 96), (12, 48)]                   # plate width in px at 1080p, FOV 70
    colw = [300, 300, 200]
    roww = sum(colw) + 20
    sh = Sheet(110 + roww * 2, 40 + len(states) * 2 * 92)
    sh.text(10, 8, "NAMEPLATES  at 2 / 6 / 12 blocks (1080p, FOV 70). 6 and 12 shown x3 beside true size. "
                   "Font is a 5x7 stand-in with the vanilla advance.")
    y = 30
    for half, tiers in enumerate((("bronze", "silver"), ("gold", "nero"))):
        for (state, name) in states:
            sh.text(8, y + 20, (name or state)[:16], fill=(151, 166, 188, 255))
            for ti, tier in enumerate(tiers):
                big = plate(tier, state, name)
                x = 110 + ti * roww
                for (blocks, wpx), cw in zip(dist, colw):
                    small = big.resize((wpx, max(1, round(wpx / 4))), Image.LANCZOS)
                    sh.put(small, x, y)
                    if blocks != 2:
                        sh.put(up(small, 3) if blocks == 12 else up(small, 2), x, y + small.height + 6)
                    x += cw
            y += 92
    sh.save(folder, "nameplates.png")


# ---------------------------------------------------------------- blocks
def joined(models, view, k, tints, n=1, pitch=16, bounds=None):
    quads = []
    for i in range(n):
        for name in models(i, n):
            tx, els = iso.load_model(name)
            quads += iso.model_quads(tx, els, view, tints, offset=(-pitch * i, 0, 0))
    return iso.draw(quads, k, bounds)


def cabinet_parts(block, cab, style):
    def parts(i, n):
        out = [block]
        left, right = i == 0, i == n - 1          # model left is x = 0; offsets run toward -x
        fr = ["edge_top", "edge_bottom", "crown"]
        if right:
            out.append(block + "_side_left")
            fr += ["edge_left", "corner_tl", "corner_bl"]
        if left:
            out.append(block + "_side_right")
            fr += ["edge_right", "corner_tr", "corner_br"]
        return out + ["frame/%s_%s_%s" % (cab, p, style) for p in fr]
    return parts


def sheet_blocks(folder):
    fin = {"default": rgb(DEFAULT_FINISH), "white": (249, 255, 254), "black": (29, 29, 33), "void": (155, 107, 255)}
    sh = Sheet(1500, 1600)
    sh.text(10, 8, "DISPLAY BLOCKS")
    x, y = 20, 30
    for label, finish in fin.items():
        for ring_label, ring in (("empty", rgb(STEEL_DK)), ("gold", rgb(RAMPS["gold"].GLOW))):
            im = iso.render("display_pedestal", iso.ISO, 9, {0: finish, 1: ring})
            sh.put(im, x, y)
            sh.text(x, y + im.height + 2, "pedestal %s / %s" % (label, ring_label))
            x += im.width + 14
    y += 210
    x = 20
    for label, finish in (("default", fin["default"]), ("white", fin["white"]), ("black", fin["black"])):
        im = joined(cabinet_parts("trophy_cabinet", "cabinet", "plate"), iso.ISO, 9, {0: finish, 2: (255, 255, 255)})
        sh.put(im, x, y)
        sh.text(x, y + im.height + 2, "trophy cabinet, %s finish" % label)
        x += im.width + 14
    im = joined(cabinet_parts("trophy_cabinet", "cabinet", "plate"), iso.View(150, 24), 9,
                {0: fin["default"], 2: (255, 255, 255)})
    sh.put(im, x, y)
    sh.text(x, y + im.height + 2, "from behind")
    im = iso.render("trophy_cabinet_inventory", iso.View(45, 30), 6, {0: fin["default"]})
    sh.put(im, x + 230, y + 40)
    sh.text(x + 230, y + 170, "inventory model")
    y += 250
    im = joined(cabinet_parts("trophy_cabinet", "cabinet", "laurel"), iso.ISO, 9,
                {0: fin["default"], 2: (255, 213, 74)}, n=3)
    sh.put(im, 20, y)
    sh.text(20, y + im.height + 2, "three joined (laurel_gold): one frame, one pool")
    im = joined(cabinet_parts("wall_cabinet", "wall_cabinet", "plate"), iso.ISO, 9, {0: fin["default"]}, n=2)
    sh.put(im, 560, y)
    sh.text(560, y + im.height + 2, "wall cabinet x2 joined")
    im = joined(cabinet_parts("wall_cabinet", "wall_cabinet", "orbital"), iso.ISO, 9,
                {0: fin["white"], 2: (96, 212, 232)})
    sh.put(im, 960, y)
    sh.text(960, y + im.height + 2, "wall cabinet, white, orbital_plasma")
    y += 290
    x = 20
    for label, ring in (("empty", rgb(STEEL_DK)), ("bronze", rgb(RAMPS["bronze"].BASE)),
                        ("silver", rgb(RAMPS["silver"].BASE)), ("gold", rgb(RAMPS["gold"].BASE)),
                        ("replica", rgb(REPLICA))):
        im = iso.render("trophy_plaque", iso.View(10, 5), 9, {0: fin["default"], 1: ring})
        sh.put(im, x, y)
        sh.text(x, y + im.height + 2, "plaque " + label)
        x += im.width + 20
    for name in ("trophy_spotlight_ceiling", "trophy_spotlight_wall"):
        base = Image.new("RGBA", (190, 190), (24, 26, 32, 255))
        im = iso.render(name, iso.View(30, -8) if "ceiling" in name else iso.View(40, 10), 9)
        base.alpha_composite(im, (10, 10))
        sh.put(base, x, y)
        sh.text(x, y + 194, name.replace("trophy_", ""))
        x += 210
    y += 240
    x = 20
    for label, tint in (("ghost bronze", RAMPS["bronze"].BASE), ("ghost gold", RAMPS["gold"].BASE),
                        ("ghost hidden", 0x97A6BC)):
        tx, els = iso.load_model("trophy_ghost")
        quads = iso.model_quads(tx, els, iso.ISO, {None: rgb(tint)}, alpha=0.35)
        base = Image.new("RGBA", (190, 150), (27, 20, 40, 255))
        base.alpha_composite(iso.draw(quads, 9), (10, 10))
        sh.put(base, x, y)
        sh.text(x, y + 154, label)
        x += 210
    sh.img = sh.img.crop((0, 0, 1500, y + 180))
    sh.save(folder, "blocks.png")


def sheet_trims(folder):
    sh = Sheet(1560, 1010)
    sh.text(10, 8, "TRIMS  twelve ids on six styles (front and iso); accent is tint index 2")
    x, y = 20, 30
    n = 0
    for style, trims in TRIMS.items():
        for trim, accent in trims:
            tints = {0: rgb(DEFAULT_FINISH), 2: rgb(accent) if accent else (255, 255, 255)}
            parts = cabinet_parts("trophy_cabinet", "cabinet", style)
            a = joined(parts, iso.FRONT, 8, tints)
            b = joined(parts, iso.ISO, 5, tints)
            sh.put(a, x, y)
            sh.put(b, x + a.width + 6, y + 20)
            sh.text(x, y + a.height + 2, trim)
            n += 1
            x += 258
            if n % 6 == 0:
                x, y = 20, y + 200
    for i, (style, accent) in enumerate((("laurel", 0xD6424A), ("neon", 0xFF3FA4), ("prism", None))):
        tints = {0: rgb(DEFAULT_FINISH), 2: rgb(accent) if accent else (255, 255, 255)}
        im = joined(cabinet_parts("trophy_cabinet", "cabinet", style), iso.FRONT, 8, tints, n=3)
        sh.put(im, 20 + i * 420, y + 10)
        sh.text(20 + i * 420, y + im.height + 14, "joined x3, " + style)
    for i, (style, accent) in enumerate((("wild", 0x78D678), ("orbital", 0xFFB347))):
        tints = {0: rgb(DEFAULT_FINISH), 2: rgb(accent)}
        im = joined(cabinet_parts("wall_cabinet", "wall_cabinet", style), iso.FRONT, 8, tints, n=3)
        sh.put(im, 20 + i * 420, y + 220)
        sh.text(20 + i * 420, y + im.height + 224, "wall cabinet joined x3, " + style)
    sh.save(folder, "trims.png")


# ---------------------------------------------------------------- items and gui
def sheet_items(folder):
    sprites = tex_items.build()
    order = ["trophy_bronze", "trophy_silver", "trophy_gold", "trophy_nero", "trophy_bronze_replica",
             "trophy_silver_replica", "trophy_gold_replica", "trophy_nero_replica", "trophy_unknown",
             "trophy_book", "trophy_plaque", "trophy_spotlight"]
    sh = Sheet(len(order) * 84 + 20, 420)
    sh.text(10, 6, "ITEMS  1x, 2x, 4x on a dark and a light slot")
    for half, bg in enumerate(((20, 28, 38, 255), (139, 139, 139, 255))):
        y = 26 + half * 190
        sh.d.rectangle([8, y - 4, sh.img.width - 8, y + 180], fill=bg)
        for i, name in enumerate(order):
            im = sprites[name].img
            x = 16 + i * 84
            sh.put(up(im, 4), x, y)
            sh.put(up(im, 2), x, y + 72)
            sh.put(im, x + 40, y + 80)
            sh.put(up(im, 4).resize((48, 48), Image.NEAREST), x, y + 112)
    sh.save(folder, "items.png")


def toast(tier):
    """Mock of the 160x32 award toast from the toast sheet."""
    r = RAMPS[tier]
    c = Canvas(160, 32, PANEL)
    c.frame(0, 0, 160, 32, PRISM_MID if tier == "nero" else r.BASE)
    for y in range(1, 31):
        colour = r.LT if tier != "nero" else (PRISM_B if y < 16 else PRISM_A)
        c.r(1, y, 3, 1, colour)
    c.paste(tex_gui.medallion(tier), 6, 4)
    c.img.alpha_composite(sample_icon(), (10, 6))
    line = text_bitmap(tier.upper() + "_TROPHY_EARNED".replace("_", " ").replace(" ", "_"))
    c.img.paste(Image.new("RGBA", line.size, rgba(r.INK)), (36, 7), line)
    title = text_bitmap("FIRST_ORBIT")
    c.img.paste(Image.new("RGBA", title.size, rgba(TEXT)), (36, 18), title)
    n = {"bronze": 1, "silver": 2, "gold": 3, "nero": 1}[tier]
    for i in range(n):
        x = 154 - 3 - i * 4
        if tier == "nero":
            c.art(x, 4, [".X.", "X.X", ".X."], {"X": r.INK})
        else:
            c.r(x, 4, 3, 3, r.INK)
    return c.img


def sheet_gui(folder):
    g = tex_gui.build()
    sh = Sheet(1500, 1560)
    sh.text(10, 6, "GUI SHEETS  toast x4, emblems x4, book x3, cabinet x3; toast mock-ups x3 and in greyscale")
    back = lambda im: Image.alpha_composite(Image.new("RGBA", im.size, (40, 30, 52, 255)), im)  # noqa: E731
    sh.put(up(back(g["trophy_toast"].img), 4), 20, 26)
    sh.put(up(back(g["mod_emblems"].img), 4), 560, 26)
    y = 300
    for i, t in enumerate(TIERS):
        im = toast(t)
        sh.put(up(im, 3), 20 + (i % 2) * 500, y + (i // 2) * 106)
        sh.put(up(cvd(im, "greyscale"), 2), 1020, y + i * 70 - 60)
    y += 230
    book = back(g["trophy_book"].img.crop((0, 0, 128, 124)))
    sh.put(up(book, 4), 20, y)
    cab = back(g["trophy_cabinet"].img)
    sh.put(up(cab, 3), 560, y)
    sh.save(folder, "gui.png")


# ---------------------------------------------------------------- entity sheets
def tinted(im, colour):
    """A white mask multiplied by one colour, as the renderer's vertex colour does."""
    r, g, b = rgb(colour)
    out = im.copy()
    out.putdata([(p[0] * r // 255, p[1] * g // 255, p[2] * b // 255, p[3]) for p in pixels(im)])
    return out


def sheet_entity(folder):
    tex = {k: v.img for k, v in tex_entity.build().items()}
    k = 16
    sh = Sheet(1180, 640)
    sh.text(10, 8, "ENTITY SHEETS  raw at x16 on dark and light grounds (white = mask tinted at draw time), then in use")
    y = 30
    for name in ("nameplate_blank", "nameplate_replica", "plate_cover_pedestal", "plate_cover_plaque", "tier_pips"):
        im = up(tex[name], k)
        sh.text(10, y, "%s.png  %dx%d" % (name, tex[name].width, tex[name].height), fill=(151, 166, 188, 255))
        y += 14
        if name == "tier_pips":
            sh.put(im, 10, y)
            for i in range(1, 5):                                   # cell boundaries
                sh.d.line([(10 + i * 8 * k, y - 2), (10 + i * 8 * k, y + im.height + 1)], fill=(120, 60, 60, 255))
        else:
            sh.put(im, 10, y)
            sh.d.rectangle([300, y - 2, 300 + im.width + 3, y + im.height + 1], fill=BG_LIGHT)
            sh.put(im, 302, y)
        y += im.height + 12
    ring = up(tex["trophy_ring"], k)
    sh.text(660, 30, "trophy_ring.png  16x16", fill=(151, 166, 188, 255))
    sh.d.rectangle([660, 44, 660 + 255, 44 + 255], fill=rgb(GLASS))
    sh.put(ring, 660, 44)
    # ring as drawn: near (lower) half PRISM_A, far (upper) half PRISM_B
    a, b = tinted(tex["trophy_ring"], PRISM_A), tinted(tex["trophy_ring"], PRISM_B)
    mix = b.copy()
    mix.paste(a.crop((0, 8, 16, 16)), (0, 8))
    sh.d.rectangle([930, 44, 930 + 191, 44 + 191], fill=rgb(GLASS))
    sh.put(up(mix, 12), 930, 44)
    sh.text(930, 240, "tinted: far half PRISM_B, near half PRISM_A")
    # in use
    sh.text(10, y, "IN USE (x8): blank ornament on each tier plate at INK 55 %  |  pips in tier INK on velvet  |  "
                   "ghost marker cells", fill=LABEL)
    y += 16
    for i, tier in enumerate(TIERS):
        r = RAMPS[tier]
        c = Canvas(12, 3)
        c.r(0, 0, 12, 3, r.FRAME)
        c.r(1, 1, 10, 2, GLASS)
        c.img.alpha_composite(tinted(tex["nameplate_blank"], dim(r.INK, 0.55)), (1, 1))
        sh.put(up(c.img, 8), 10 + i * 110, y)
        v = Canvas(12, 6, 0x1B1428)
        cell = tex["tier_pips"].crop((i * 8, 0, i * 8 + 8, 4))
        v.img.alpha_composite(tinted(cell, r.INK), (2, 1))
        sh.put(up(v.img, 8), 10 + i * 110, y + 34)
        sh.text(10 + i * 110, y + 86, tier, fill=(151, 166, 188, 255))
    q = Canvas(12, 6, 0x1B1428)
    q.img.alpha_composite(tinted(tex["tier_pips"].crop((32, 0, 40, 4)), 0x97A6BC), (2, 1))
    sh.put(up(q.img, 8), 10 + 4 * 110, y + 34)
    sh.text(10 + 4 * 110, y + 86, "hidden", fill=(151, 166, 188, 255))
    # covers in place
    x = 600
    ped = Canvas(14, 4, 0x7A8492)
    ped.blit(tex["plate_cover_pedestal"], 0, 0)
    sh.put(up(ped.img, 8), x, y)
    sh.text(x, y + 36, "pedestal plaque, covered")
    wall = Canvas(14, 5, 0x1B1428)
    wall.r(0, 0, 14, 2, GLASS)
    wall.r(0, 0, 1, 2, 0x7A8492)
    wall.r(13, 0, 1, 2, 0x7A8492)
    wall.blit(tex["plate_cover_plaque"], 0, 3)
    sh.put(up(wall.img, 8), x + 150, y)
    sh.text(x + 150, y + 44, "wall plaque: title strip, covered name strip")
    tro = Canvas(12, 3)
    tro.blit(tex["nameplate_replica"], 0, 0)
    sh.put(up(tro.img, 8), x + 440, y)
    sh.text(x + 440, y + 28, "trophy plate, covered")
    # true size strip
    sh.text(10, y + 110, "true size (x1) and x4:")
    xx = 10
    for name in sorted(tex):
        sh.put(tex[name], xx, y + 128)
        sh.put(up(tex[name], 4), xx, y + 150)
        xx += tex[name].width * 4 + 20
    sh.save(folder, "entity.png")


def main(argv):
    if not argv:
        print(__doc__)
        return 2
    folder = argv[0]
    os.makedirs(folder, exist_ok=True)
    only = set(argv[1:])
    for name, fn in (("tiers", sheet_tiers), ("nameplates", sheet_nameplates), ("blocks", sheet_blocks),
                     ("trims", sheet_trims), ("items", sheet_items), ("gui", sheet_gui),
                     ("entity", sheet_entity)):
        if not only or name in only:
            fn(folder)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
