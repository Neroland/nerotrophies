"""Tiny pixel-art canvas over Pillow, shared by the NeroTrophies art generators."""
from PIL import Image

from palette import CLEAR, rgba


def col(c, alpha=255):
    if c is None:
        return CLEAR
    if isinstance(c, tuple):
        return c
    return rgba(c, alpha)


def _pixels(img):
    """All pixels as RGBA tuples (Pillow renamed getdata in 12.x)."""
    if hasattr(img, "get_flattened_data"):
        return list(img.get_flattened_data())
    return list(img.getdata())


pixels = _pixels


class Canvas:
    def __init__(self, w, h, fill=None):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), col(fill))

    def p(self, x, y, c, alpha=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), col(c, alpha))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def r(self, x, y, w, h, c, alpha=255):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.p(xx, yy, c, alpha)

    def frame(self, x, y, w, h, c):
        self.r(x, y, w, 1, c)
        self.r(x, y + h - 1, w, 1, c)
        self.r(x, y, 1, h, c)
        self.r(x + w - 1, y, 1, h, c)

    def framed(self, x, y, w, h, fill, border):
        self.r(x, y, w, h, fill)
        self.frame(x, y, w, h, border)

    def bevel(self, x, y, w, h, fill, lt, dk):
        """Top-left light: top row and left column lt, bottom row and right column dk."""
        self.r(x, y, w, h, fill)
        self.r(x, y, w, 1, lt)
        self.r(x, y, 1, h, lt)
        self.r(x, y + h - 1, w, 1, dk)
        self.r(x + w - 1, y, 1, h, dk)

    def rivet(self, x, y, lt, dk):
        self.p(x, y, lt)
        self.p(x, y + 1, dk)

    def art(self, x, y, rows, key):
        """Stamp ASCII art; chars missing from key (and '.') are skipped."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in key and key[ch] is not None:
                    self.p(x + i, y + j, key[ch])

    def outline(self, c, x0=0, y0=0, w=None, h=None):
        """1-texel outline on every transparent texel 4-adjacent to an opaque one."""
        w = self.w if w is None else w
        h = self.h if h is None else h
        todo = []
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if self.get(x, y)[3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if x0 <= xx < x0 + w and y0 <= yy < y0 + h and self.get(xx, yy)[3] != 0:
                        todo.append((x, y))
                        break
        for x, y in todo:
            self.p(x, y, c)

    def paste(self, other, x, y):
        self.img.alpha_composite(other.img if isinstance(other, Canvas) else other, (x, y))

    def blit(self, other, x, y):
        """Copy including alpha (no blending)."""
        src = other.img if isinstance(other, Canvas) else other
        self.img.paste(src, (x, y))

    def colours(self):
        return {px for px in _pixels(self.img) if px[3] != 0}

    def remap(self, table):
        out = Canvas(self.w, self.h)
        data = [table.get(px, px) for px in _pixels(self.img)]
        out.img.putdata(data)
        return out
