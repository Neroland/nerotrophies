#!/usr/bin/env python3
"""Generate every NeroTrophies block, item, entity and GUI texture from the palette.

    python3 tools/art/gen_trophy_art.py            write the PNGs
    python3 tools/art/gen_trophy_art.py --check    regenerate in memory and compare with the files

Deterministic: no random numbers, no inputs besides palette.py. Rules enforced on write and check:
at most nine colours per block or item texture, and alpha 0 or 255 outside the translucent set.
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import tex_blocks  # noqa: E402
import tex_entity  # noqa: E402
from px import pixels  # noqa: E402
import tex_gui  # noqa: E402
import tex_items  # noqa: E402
import tex_trophy  # noqa: E402

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "nerotrophies")
TRANSLUCENT = {"trophy_cabinet_glass", "trophy_spotlight_cone"}
ANIMATED = {"trophy_nero_glow", "trophy_prism_accent", "trim_prism_accent"}


def collect():
    """(relative path, bytes) for every file this tool owns."""
    files = []
    groups = (("block", {**tex_trophy.build(), **tex_blocks.build()}),
              ("item", tex_items.build()),
              ("entity", tex_entity.build()),
              ("gui", tex_gui.build()))
    problems = []
    for folder, sheets in groups:
        for name, canvas in sorted(sheets.items()):
            if folder != "gui":
                n = len(canvas.colours())
                if n > 9:
                    problems.append("%s/%s: %d colours (limit 9)" % (folder, name, n))
                if name not in TRANSLUCENT:
                    bad = [p for p in pixels(canvas.img) if p[3] not in (0, 255)]
                    if bad:
                        problems.append("%s/%s: partial alpha" % (folder, name))
            buf = io.BytesIO()
            canvas.img.save(buf, "PNG", optimize=False)
            files.append(("textures/%s/%s.png" % (folder, name), buf.getvalue()))
            if name in ANIMATED:
                files.append(("textures/%s/%s.png.mcmeta" % (folder, name), tex_trophy.ANIM_MCMETA.encode()))
    return files, problems


def main(argv):
    files, problems = collect()
    for p in problems:
        print("FAIL " + p)
    if "--check" in argv:
        for rel, data in files:
            path = os.path.join(ASSETS, rel)
            if not os.path.isfile(path):
                problems.append(rel)
                print("MISSING " + rel)
            elif open(path, "rb").read() != data:
                problems.append(rel)
                print("STALE " + rel)
        print("%d files checked, %d problems" % (len(files), len(problems)))
        return 1 if problems else 0
    if problems:
        return 1
    for rel, data in files:
        path = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)
    print("wrote %d files under %s" % (len(files), os.path.relpath(ASSETS, ROOT)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
