"""A small software renderer for Minecraft block-model JSON, used only for proof sheets.

Painter's algorithm over per-texel quads: enough to judge silhouettes, UVs, tints and layering.
It follows the game's default face UV orientation and face shading (up 1.0, north/south 0.8,
east/west 0.6, down 0.5; none on shade-off elements).
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "nerotrophies")
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1),
          "east": (1, 0, 0), "west": (-1, 0, 0)}
_tex_cache = {}


def load_model(name):
    """Resolve a nerotrophies block model with its parents: (textures, elements)."""
    with open(os.path.join(ASSETS, "models", "block", name + ".json"), encoding="utf-8") as fh:
        data = json.load(fh)
    textures, elements = {}, None
    parent = data.get("parent", "")
    if parent.startswith("nerotrophies:block/"):
        textures, elements = load_model(parent.split("block/", 1)[1])
        textures = dict(textures)
    textures.update(data.get("textures", {}))
    if "elements" in data:
        elements = data["elements"]
    return textures, elements


def texture(ref, textures, frame=0):
    seen = 0
    while isinstance(ref, str) and ref.startswith("#") and seen < 8:
        ref = textures.get(ref[1:])
        seen += 1
    if ref is None:
        return None
    if isinstance(ref, Image.Image):
        return ref
    key = (ref, frame)
    if key not in _tex_cache:
        path = os.path.join(ASSETS, "textures", ref.split(":", 1)[1] + ".png")
        img = Image.open(path).convert("RGBA")
        if img.height > img.width:                     # animated strip
            n = img.height // img.width
            img = img.crop((0, (frame % n) * img.width, img.width, (frame % n + 1) * img.width))
        _tex_cache[key] = img
    return _tex_cache[key]


def face_point(side, frm, to, s, t):
    (x1, y1, z1), (x2, y2, z2) = frm, to
    if side == "north":
        return (x2 + (x1 - x2) * s, y2 + (y1 - y2) * t, z1)
    if side == "south":
        return (x1 + (x2 - x1) * s, y2 + (y1 - y2) * t, z2)
    if side == "east":
        return (x2, y2 + (y1 - y2) * t, z2 + (z1 - z2) * s)
    if side == "west":
        return (x1, y2 + (y1 - y2) * t, z1 + (z2 - z1) * s)
    if side == "up":
        return (x1 + (x2 - x1) * s, y2, z1 + (z2 - z1) * t)
    return (x1 + (x2 - x1) * s, y1, z2 + (z1 - z2) * t)


def rotate(p, rot):
    if not rot:
        return p
    ox, oy, oz = rot["origin"]
    a = math.radians(rot["angle"])
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
    if rot["axis"] == "z":
        x, y = x * c - y * s, x * s + y * c
    elif rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    else:
        x, z = x * c + z * s, -x * s + z * c
    return (x + ox, y + oy, z + oz)


class View:
    def __init__(self, yaw=28, pitch=24):
        a, p = math.radians(yaw), math.radians(pitch)
        self.f = (math.sin(a) * math.cos(p), -math.sin(p), math.cos(a) * math.cos(p))
        self.r = (-math.cos(a), 0.0, math.sin(a))
        fx, fy, fz = self.f
        rx, ry, rz = self.r
        u = (fy * rz - fz * ry, fz * rx - fx * rz, fx * ry - fy * rx)
        self.u = u if u[1] > 0 else (-u[0], -u[1], -u[2])

    def project(self, p):
        dot = lambda a, b: a[0] * b[0] + a[1] * b[1] + a[2] * b[2]  # noqa: E731
        return dot(p, self.r), -dot(p, self.u), dot(p, self.f)


FRONT = View(0, 0)
ISO = View(28, 24)


def model_quads(textures, elements, view, tints=None, frame=0, offset=(0, 0, 0), alpha=1.0, emissive_tint=None):
    """Yield (depth, [4 projected points], rgba)."""
    tints = tints or {}
    out = []
    for e in elements:
        frm, to, rot = e["from"], e["to"], e.get("rotation")
        shade = e.get("shade", True)
        for side, face in e["faces"].items():
            n = NORMAL[side]
            if rot:
                o = rotate((0, 0, 0), rot)
                nn = rotate(n, rot)
                n = (nn[0] - o[0], nn[1] - o[1], nn[2] - o[2])
            if n[0] * view.f[0] + n[1] * view.f[1] + n[2] * view.f[2] >= -1e-6:
                continue
            img = texture(face["texture"], textures, frame)
            if img is None:
                continue
            u1, v1, u2, v2 = face["uv"]
            w, h = img.size
            nu = max(1, round(abs(u2 - u1) * w / 16))
            nv = max(1, round(abs(v2 - v1) * h / 16))
            tint = tints.get(face.get("tintindex"))
            k = SHADE[side] if shade else 1.0
            for j in range(nv):
                for i in range(nu):
                    tu = (u1 + (u2 - u1) * (i + 0.5) / nu) * w / 16
                    tv = (v1 + (v2 - v1) * (j + 0.5) / nv) * h / 16
                    r, g, b, a = img.getpixel((min(w - 1, max(0, int(tu))), min(h - 1, max(0, int(tv)))))
                    if a == 0:
                        continue
                    if tint is not None:
                        r, g, b = r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255
                    r, g, b = int(r * k), int(g * k), int(b * k)
                    pts, depth = [], 0.0
                    for (ds, dt) in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        p = rotate(face_point(side, frm, to, (i + ds) / nu, (j + dt) / nv), rot)
                        p = (p[0] + offset[0], p[1] + offset[1], p[2] + offset[2])
                        sx, sy, d = view.project(p)
                        pts.append((sx, sy))
                        depth += d / 4
                    out.append((depth, pts, (r, g, b, int(a * alpha))))
    return out


def draw(quads, scale, bounds=None, pad=2):
    """Rasterise quads, far to near. bounds = (minx, miny, maxx, maxy) in projected units."""
    if bounds is None:
        xs = [p[0] for q in quads for p in q[1]]
        ys = [p[1] for q in quads for p in q[1]]
        bounds = (min(xs), min(ys), max(xs), max(ys))
    x0, y0, x1, y1 = bounds
    W, H = int((x1 - x0) * scale) + pad * 2 + 1, int((y1 - y0) * scale) + pad * 2 + 1
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for depth, pts, colour in sorted(quads, key=lambda q: -q[0]):
        poly = [((x - x0) * scale + pad, (y - y0) * scale + pad) for x, y in pts]
        if colour[3] == 255:
            d.polygon(poly, fill=colour)
        else:
            bx = max(0, int(min(p[0] for p in poly)) - 1)
            by = max(0, int(min(p[1] for p in poly)) - 1)
            bw = min(W, int(max(p[0] for p in poly)) + 2) - bx
            bh = min(H, int(max(p[1] for p in poly)) + 2) - by
            if bw <= 0 or bh <= 0:
                continue
            layer = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
            ImageDraw.Draw(layer).polygon([(x - bx, y - by) for x, y in poly], fill=colour)
            img.alpha_composite(layer, (bx, by))
            d = ImageDraw.Draw(img)
    return img


def render(name, view=ISO, scale=8, tints=None, frame=0, extra=None, bounds=None):
    textures, elements = load_model(name)
    quads = model_quads(textures, elements, view, tints, frame)
    for (tx, els, kw) in (extra or []):
        quads += model_quads(tx, els, view, **kw)
    return draw(quads, scale, bounds)
