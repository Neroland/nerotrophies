#!/usr/bin/env python3
"""
Generate the NEROTROPHIES mod logo (square), in the shared Neroland family style: a deep-space
starfield with two nebula glows and the family faceted hexagonal core-prism with a glowing centre
node + specular sparkle.

NeroTrophies is the cross-mod trophies mod, so the family core-prism is set into the face of a
gold (#e8b84a) trophy cup with two handles, a stem and a stepped plinth, haloed by a ring of
award stars, all standing on the family perspective grid. The wordmark and tagline are drawn in a built-in 5x7 pixel font (no system font needed, so
the output is identical on every OS). Renders supersampled, then downsamples.

Outputs:
  art/logo/nerotrophies_logo.png       (1024x1024 master)
  art/logo/nerotrophies_logo_400.png   (CurseForge-ready)
  common/src/main/resources/nerotrophies_logo.png  (256x256 in-game mods-list icon)

Usage: python tools/gen_logo.py   (needs Pillow and numpy)
"""
SEED = 61
MOD = 'nerotrophies'
NAME = 'NEROTROPHIES'
TAG = 'EARN · DISPLAY · REMEMBER'
ACCENT = (232, 184, 74)          # family colour #e8b84a (trophy gold)
ACCENT_BRIGHT = (255, 222, 140)
BRIGHT = (255, 248, 230)
NEBULA = (110, 72, 20)
STAR_TINT = (255, 236, 200)
TAG_COL = (255, 230, 180)
GOLD = (255, 214, 90)
GOLD_DARK = (150, 100, 30)

import math, os, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "logo")
ICON = os.path.join(ROOT, "common", "src", "main", "resources")

FINAL = 1024
SS = 2
R = FINAL * SS
rng = random.Random(SEED)

NERO_ALLOY = (38, 166, 154)     # family teal
STARSTEEL  = (140, 178, 208)    # family steel-blue
PLASMA     = (96, 212, 232)     # family cyan
FACET_COLS = [STARSTEEL, NERO_ALLOY, ACCENT, STARSTEEL, NERO_ALLOY, ACCENT]

# ---- 5x7 pixel font ------------------------------------------------------------------------------
GLYPHS = {
    'A': ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    'B': ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    'C': ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    'D': ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    'E': ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    'H': ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    'I': ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    'K': ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    'L': ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    'M': ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    'N': ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    'O': ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    'P': ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    'R': ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    'S': ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    'T': ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    'X': ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    'U': ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    'Y': ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    '·': ["00000", "00000", "00000", "00100", "00000", "00000", "00000"],
    ' ': ["00000"] * 7,
}



def text_cells(text, gap=1):
    """(col, row) of every lit pixel, and the text's width in pixel columns."""
    cells, x = [], 0
    for ch in text:
        g = GLYPHS[ch]
        for r, row in enumerate(g):
            for c, bit in enumerate(row):
                if bit == '1':
                    cells.append((x + c, r))
        x += len(g[0]) + gap
    return cells, x - gap


def sprite_cells(rows):
    return [(c, r) for r, row in enumerate(rows) for c, bit in enumerate(row) if bit == '1']


# ---- background ----------------------------------------------------------------------------------
def background():
    top = np.array([6, 11, 17], float)
    bot = np.array([13, 18, 28], float)
    yy = np.linspace(0, 1, R)[:, None, None]
    img = top[None, None, :] * (1 - yy) + bot[None, None, :] * yy
    img = np.repeat(img, R, axis=1)
    Y, X = np.mgrid[0:R, 0:R].astype(float)

    def glow(cx, cy, rad, color, strength):
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        f = np.clip(1 - d / rad, 0, 1) ** 2 * strength
        for c in range(3):
            img[:, :, c] += color[c] * f

    glow(R * 0.22, R * 0.26, R * 0.55, (20, 80, 84), 0.42)   # family teal nebula
    glow(R * 0.80, R * 0.40, R * 0.55, NEBULA, 0.50)         # mod accent nebula
    glow(R * 0.5, R * 0.36, R * 0.40, (90, 60, 16), 0.30)    # glow behind the trophy

    d = np.sqrt((X - R / 2) ** 2 + (Y - R / 2) ** 2) / (R * 0.72)
    vig = np.clip(1 - (d ** 2) * 0.85, 0.25, 1)
    img *= vig[:, :, None]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def add_stars(base):
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(380):
        x, y = rng.randint(0, R), rng.randint(0, int(R * 0.66))
        s = rng.choice([1, 1, 1, 2, 2, 3]) * SS
        b = rng.randint(120, 255)
        tint = rng.choice([(b, b, b), (b, 255, 255), STAR_TINT, (200, 200, 255)])
        d.rectangle([x, y, x + s, y + s], fill=tint + (rng.randint(120, 255),))  # square: pixel stars
    base.alpha_composite(layer.filter(ImageFilter.GaussianBlur(2 * SS)))
    base.alpha_composite(layer)
    return base


def soft(draw_fn, blur):
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def neon(base, draw_fn, blur=10):
    """A shape drawn three times: wide glow, tight glow, crisp core."""
    base.alpha_composite(soft(draw_fn, blur * SS))
    base.alpha_composite(soft(draw_fn, max(1, blur * SS // 4)))
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    base.alpha_composite(layer)


def grid_floor(base, horizon):
    """A retro perspective grid from the horizon to the bottom edge, fading out with distance."""
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    vx = R / 2
    for i in range(-14, 15):
        x_bottom = vx + i * R * 0.11
        d.line([(vx + i * R * 0.012, horizon), (x_bottom, R)], fill=ACCENT + (120,), width=SS * 2)
    for k in range(1, 14):
        t = (k / 13) ** 2.1
        y = horizon + (R - horizon) * t
        d.line([(0, y), (R, y)], fill=ACCENT + (int(40 + 110 * t),), width=SS * 2)
    # fade toward the horizon
    mask = np.zeros((R, R), np.uint8)
    ys = np.arange(R)
    fade = np.clip((ys - horizon) / (R - horizon), 0, 1) ** 0.8
    mask[:] = (fade * 255).astype(np.uint8)[:, None]
    a = np.array(layer.split()[3]).astype(float) * (mask / 255.0)
    layer.putalpha(Image.fromarray(a.astype(np.uint8)))
    base.alpha_composite(layer.filter(ImageFilter.GaussianBlur(4 * SS)))
    base.alpha_composite(layer)
    # horizon glow line
    neon(base, lambda dr: dr.line([(0, horizon), (R, horizon)], fill=ACCENT_BRIGHT + (200,), width=SS * 2), 12)


# ---- the family core-prism ----------------------------------------------------------------------
def draw_core_prism(base, cx, cy, rad):
    base.alpha_composite(soft(
        lambda dr: dr.ellipse([cx - rad * 1.3, cy - rad * 1.3, cx + rad * 1.3, cy + rad * 1.3],
                              fill=ACCENT + (110,)), 20 * SS))
    hexpts = [(cx + math.cos(math.radians(60 * i - 90)) * rad,
               cy + math.sin(math.radians(60 * i - 90)) * rad) for i in range(6)]
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for i in range(6):
        shade = 0.58 + 0.42 * (i / 5.0)
        col = tuple(int(c * shade) for c in FACET_COLS[i])
        d.polygon([(cx, cy), hexpts[i], hexpts[(i + 1) % 6]], fill=col + (255,))
    ir = rad * 0.36
    d.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=ACCENT_BRIGHT + (255,))
    d.ellipse([cx - ir * 0.5, cy - ir * 0.5, cx + ir * 0.5, cy + ir * 0.5], fill=BRIGHT + (255,))
    for i in range(6):
        d.line([hexpts[i], hexpts[(i + 1) % 6]], fill=(230, 240, 250, 235), width=max(1, SS * 2))
        d.line([(cx, cy), hexpts[i]], fill=(220, 230, 240, 150), width=max(1, SS))
    base.alpha_composite(layer)
    sx, sy = cx - rad * 0.16, cy - rad * 0.46
    base.alpha_composite(soft(lambda dr: dr.ellipse([sx - 7 * SS, sy - 7 * SS, sx + 7 * SS, sy + 7 * SS],
                                                    fill=(255, 255, 255, 255)), 4 * SS))
    dd = ImageDraw.Draw(base)
    L = 13 * SS
    dd.line([sx - L, sy, sx + L, sy], fill=(255, 255, 255, 230), width=SS * 2)
    dd.line([sx, sy - L, sx, sy + L], fill=(255, 255, 255, 230), width=SS * 2)


def pixels(d, cells, ox, oy, px, fill, inset=0.12):
    g = px * inset
    for c, r in cells:
        x, y = ox + c * px, oy + r * px
        d.rectangle([x + g, y + g, x + px - g, y + px - g], fill=fill)


# ---- the trophy ----------------------------------------------------------------------------------
def trophy(base):
    cx, top = R / 2, R * 0.12
    bw = R * 0.19                      # bowl half-width at the rim
    bowl_bot = R * 0.40
    # halo + award stars
    base.alpha_composite(soft(lambda dr: dr.ellipse([cx - R * 0.30, R * 0.06, cx + R * 0.30, R * 0.62],
                                                     fill=ACCENT + (120,)), 50 * SS))
    for k in range(9):
        a = math.radians(-90 + (k - 4) * 22)
        sx, sy = cx + math.cos(a) * R * 0.33, R * 0.36 + math.sin(a) * R * 0.30
        s = R * (0.016 if k % 2 == 0 else 0.010)
        star = [(sx + math.cos(math.radians(36 * n - 90)) * (s if n % 2 == 0 else s * 0.45),
                 sy + math.sin(math.radians(36 * n - 90)) * (s if n % 2 == 0 else s * 0.45)) for n in range(10)]
        neon(base, lambda dr, star=star: dr.polygon(star, fill=ACCENT_BRIGHT + (255,)), 8)

    def shaded_polygon(pts, light, dark):
        """A polygon with a left-to-right metallic gradient."""
        layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
        ImageDraw.Draw(layer).polygon(pts, fill=(255, 255, 255, 255))
        xs = np.linspace(0, 1, R)[None, :]
        band = 0.5 + 0.5 * np.cos((xs - 0.42) * math.pi * 3.2)
        rgb = np.zeros((R, R, 3))
        for c in range(3):
            rgb[:, :, c] = dark[c] + (light[c] - dark[c]) * band
        a = np.array(layer.split()[3])
        out = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), "RGBA")
        base.alpha_composite(out)

    # handles (behind the bowl)
    hd = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    hdr = ImageDraw.Draw(hd)
    for sgn in (-1, 1):
        hx = cx + sgn * bw * 1.02
        box = [hx - bw * 0.42, top + R * 0.04, hx + bw * 0.42, top + R * 0.20]
        hdr.arc(box, 270 if sgn > 0 else 90, 90 if sgn > 0 else 270, fill=GOLD_DARK + (255,), width=int(R * 0.022))
        hdr.arc(box, 270 if sgn > 0 else 90, 90 if sgn > 0 else 270, fill=ACCENT + (255,), width=int(R * 0.012))
    base.alpha_composite(soft(lambda dr: dr.bitmap((0, 0), hd.split()[3], fill=ACCENT + (160,)), 10 * SS))
    base.alpha_composite(hd)

    # bowl: rim to a rounded bottom
    bowl = [(cx - bw, top)]
    for n in range(0, 21):
        a = math.pi * n / 20
        bowl.append((cx - bw * math.cos(a) * (0.98 - 0.25 * math.sin(a) * 0),
                     top + R * 0.10 + (bowl_bot - top - R * 0.10) * math.sin(a)))
    bowl.append((cx + bw, top))
    shaded_polygon(bowl, ACCENT_BRIGHT, GOLD_DARK)
    # rim
    neon(base, lambda dr: dr.ellipse([cx - bw, top - R * 0.018, cx + bw, top + R * 0.018],
                                     fill=(120, 80, 20, 255), outline=ACCENT_BRIGHT + (255,), width=SS * 4), 10)
    # stem + knot
    stem = [(cx - R * 0.022, bowl_bot - R * 0.01), (cx + R * 0.022, bowl_bot - R * 0.01),
            (cx + R * 0.030, R * 0.48), (cx - R * 0.030, R * 0.48)]
    shaded_polygon(stem, ACCENT_BRIGHT, GOLD_DARK)
    shaded_polygon([(cx - R * 0.05, R * 0.43), (cx + R * 0.05, R * 0.43), (cx + R * 0.04, R * 0.455),
                    (cx - R * 0.04, R * 0.455)], ACCENT_BRIGHT, GOLD_DARK)
    # stepped plinth
    shaded_polygon([(cx - R * 0.10, R * 0.48), (cx + R * 0.10, R * 0.48), (cx + R * 0.12, R * 0.52),
                    (cx - R * 0.12, R * 0.52)], ACCENT_BRIGHT, GOLD_DARK)
    d = ImageDraw.Draw(base)
    d.rectangle([cx - R * 0.15, R * 0.52, cx + R * 0.15, R * 0.585], fill=(34, 30, 40, 255))
    d.rectangle([cx - R * 0.15, R * 0.52, cx + R * 0.15, R * 0.528], fill=ACCENT + (255,))
    # name plate on the plinth
    neon(base, lambda dr: dr.rectangle([cx - R * 0.08, R * 0.540, cx + R * 0.08, R * 0.568],
                                       fill=(60, 44, 20, 255), outline=ACCENT_BRIGHT + (255,), width=SS * 2), 6)
    # specular streak on the bowl
    base.alpha_composite(soft(lambda dr: dr.polygon([(cx - bw * 0.62, top + R * 0.03), (cx - bw * 0.48, top + R * 0.03),
                                                     (cx - bw * 0.30, bowl_bot - R * 0.07),
                                                     (cx - bw * 0.40, bowl_bot - R * 0.07)],
                                                    fill=(255, 255, 255, 150)), 6 * SS))
    # the family core-prism set into the cup face
    draw_core_prism(base, cx, top + R * 0.15, int(R * 0.062))
    # floor glow under the plinth
    base.alpha_composite(soft(lambda dr: dr.ellipse([cx - R * 0.24, R * 0.585, cx + R * 0.24, R * 0.635],
                                                     fill=ACCENT + (150,)), 14 * SS))


# ---- pixel wordmark -----------------------------------------------------------------------------
def pixel_text(base, text, cy, max_w, px_max, face_top, face_bot, glow, extrude=0.0, gap=1):
    cells, cols = text_cells(text, gap)
    px = min(px_max, max_w / cols)
    w = cols * px
    ox, oy = (R - w) / 2, cy - 7 * px / 2
    # glow
    gl = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    pixels(ImageDraw.Draw(gl), cells, ox, oy, px, glow + (255,), inset=0.0)
    base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(px * 1.1)))
    # extrusion (retro 3-D drop)
    if extrude > 0:
        ex = Image.new("RGBA", (R, R), (0, 0, 0, 0))
        ed = ImageDraw.Draw(ex)
        steps = int(px * extrude)
        for k in range(steps, 0, -SS):
            shade = 0.25 + 0.35 * (1 - k / steps)
            pixels(ed, cells, ox + k * 0.55, oy + k, px, tuple(int(c * shade) for c in ACCENT) + (255,), 0.0)
        base.alpha_composite(ex)
    # face: vertical gradient, each pixel a bevelled block
    face = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    fd = ImageDraw.Draw(face)
    for c, r in cells:
        t = r / 6.0
        col = tuple(int(face_top[i] * (1 - t) + face_bot[i] * t) for i in range(3))
        x, y = ox + c * px, oy + r * px
        g = px * 0.06
        fd.rectangle([x + g, y + g, x + px - g, y + px - g], fill=col + (255,))
        hi = tuple(min(255, int(v + (255 - v) * 0.55)) for v in col)
        fd.rectangle([x + g, y + g, x + px - g, y + g + px * 0.18], fill=hi + (255,))
        lo = tuple(int(v * 0.72) for v in col)
        fd.rectangle([x + g, y + px - g - px * 0.14, x + px - g, y + px - g], fill=lo + (255,))
    base.alpha_composite(face)
    return oy + 7 * px


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(ICON, exist_ok=True)
    img = background()
    img = add_stars(img)
    horizon = R * 0.60
    grid_floor(img, horizon)
    trophy(img)
    # dark band behind the wordmark so it reads over the grid
    img.alpha_composite(soft(lambda dr: dr.rectangle([0, R * 0.665, R, R * 0.935], fill=(6, 8, 14, 205)), 30 * SS))
    pixel_text(img, NAME, R * 0.755, R * 0.90, R * 0.016, BRIGHT, ACCENT, ACCENT, extrude=0.55)
    pixel_text(img, TAG, R * 0.885, R * 0.62, R * 0.0072, TAG_COL, TAG_COL, ACCENT)

    final = img.convert("RGB").resize((FINAL, FINAL), Image.LANCZOS)
    p1 = os.path.join(OUT, MOD + "_logo.png")
    p2 = os.path.join(OUT, MOD + "_logo_400.png")
    p3 = os.path.join(ICON, MOD + "_logo.png")
    final.save(p1)
    final.resize((400, 400), Image.LANCZOS).save(p2)
    final.resize((256, 256), Image.LANCZOS).save(p3)
    for p in (p1, p2, p3):
        print("wrote", os.path.relpath(p, ROOT))




def generate_gif(source=None, output=None):
    source = source or os.path.join(OUT, "nerotrophies_logo.png")
    output = output or os.path.join(OUT, "nerotrophies_logo.gif")
    with Image.open(source) as image:
        logo = image.convert("RGB")
    logo.thumbnail((400, 400), Image.Resampling.LANCZOS)
    w, h = logo.size
    color = ACCENT
    frames = []
    for index in range(14):
        t = index / 14
        phase = 2 * math.pi * t
        overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
        # a glint sweeping across the trophy
        x = w * (0.25 + 0.5 * t)
        draw.line((x, h * 0.12, x + w * 0.06, h * 0.40), fill=color + (110 + int(60 * math.sin(phase) ** 2),),
                  width=max(1, w // 120))
        overlay = overlay.filter(ImageFilter.MaxFilter(3))
        image = logo.convert("RGBA")
        image.alpha_composite(overlay.filter(ImageFilter.GaussianBlur(max(1, w * 0.008))))
        image.alpha_composite(overlay)
        frames.append(image.convert("RGB"))
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=200, loop=0, optimize=True)
    return output


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Generate the logo or animate its existing master PNG.")
    parser.add_argument("--gif", action="store_true", help="generate the animated logo")
    args = parser.parse_args()
    if args.gif:
        print("wrote", os.path.relpath(generate_gif(), ROOT))
    else:
        main()
