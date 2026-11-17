#!/usr/bin/env python3
"""Proves every registered NeroTrophies thing has its assets, offline.

Checks:

* Registries: block, item, menu, sound-event and particle ids are read from the Java sources
  (``registry/`` plus any class that creates sound events or registers particle types), and the
  cabinet trims from ``block/Trims.java``.
* Blocks: each block has ``blockstates/<id>.json``; every model it references (``variants`` and
  ``multipart``) exists, and so does every model's parent chain and texture. ``minecraft:``
  parents and textures are vanilla and assumed present. Every ``#var`` a face uses must resolve
  through the model's own parent chain.
* Items: each item has an item-model definition ``items/<id>.json`` (Minecraft 1.21.4+) whose
  referenced models resolve, and, while the build still has a node below 1.21.4 (no ``items/``
  folder there and no resource overlay replacing it), a legacy ``models/item/<id>.json`` whose
  overrides resolve.
* Lang: ``block.<mod>.*``, ``item.<mod>.*`` (items without a block), ``itemGroup.<mod>``,
  ``container.<mod>.<menu>``, ``trim.<mod>.<trim>``, every ``sounds.json`` subtitle and every
  literal key passed to ``Component.translatable(..)`` in Java must be in ``en_us.json``. A key
  found only in ``tools/lang/pending/*.json`` (not merged yet) is a warning.
* Sounds: every file ``sounds.json`` names exists; every sound event Java creates is defined there.
* Particles: every particle definition's textures exist; registered types need a definition.
* The asset checklist: every listed file exists, PNGs at the listed size and Ogg files mono 32 kHz;
  the app's emblem atlas copy is byte-identical (skipped with ``--no-siblings``).
* Orphans (warnings): textures, models, blockstates and item definitions nothing references.

Exit status 1 when any error is found (or any warning with ``--strict``).
Privacy (POPIA/GDPR): reads resource and source files only; prints file paths and rule messages.
"""

import argparse
import json
import os
import re
import struct
import sys

MOD_ID = "nerotrophies"
RES = "common/src/main/resources"
ASSETS = RES + "/assets/" + MOD_ID
JAVA_ROOTS = ("common/src/main/java", "fabric/src/main/java", "forge/src/main/java", "neoforge/src/main/java")
REGISTRY = "common/src/main/java/za/co/neroland/nerotrophies/registry"
TRIMS = "common/src/main/java/za/co/neroland/nerotrophies/block/Trims.java"
LOADERS = ("fabric", "forge", "neoforge")
APP_SIBLING = "nerolink-app"

# The asset checklist from the art plan: (path, expected) where expected is (width, height) for a PNG,
# "ogg" for a mono 32 kHz Ogg Vorbis file, or None for "must exist". "A/" is the mod's asset folder,
# "R/" the repository root and "APP/" the NeroLink app repository next to it.
CHECKLIST = [
    ("A/textures/block/trophy_bronze.png", (32, 32)),
    ("A/textures/block/trophy_silver.png", (32, 32)),
    ("A/textures/block/trophy_gold.png", (32, 32)),
    ("A/textures/block/trophy_nero.png", (32, 32)),
    ("A/textures/block/trophy_silver_glow.png", (32, 32)),
    ("A/textures/block/trophy_gold_glow.png", (32, 32)),
    ("A/textures/block/trophy_nero_glow.png", (32, 256)),
    ("A/textures/block/trophy_nero_glow.png.mcmeta", None),
    ("A/textures/block/trophy_bronze_replica.png", (32, 32)),
    ("A/textures/block/trophy_silver_replica.png", (32, 32)),
    ("A/textures/block/trophy_gold_replica.png", (32, 32)),
    ("A/textures/block/trophy_nero_replica.png", (32, 32)),
    ("A/textures/block/trophy_ghost.png", (16, 16)),
    ("A/textures/block/display_pedestal_top.png", (16, 16)),
    ("A/textures/block/display_pedestal_side.png", (16, 16)),
    ("A/textures/block/display_pedestal_bottom.png", (16, 16)),
    ("A/textures/block/display_pedestal_tint.png", (16, 16)),
    ("A/textures/block/display_pedestal_ring.png", (16, 16)),
    ("A/textures/block/display_pedestal_plaque.png", (16, 16)),
    ("A/textures/block/trophy_cabinet_outer.png", (16, 16)),
    ("A/textures/block/trophy_cabinet_outer_tint.png", (16, 16)),
    ("A/textures/block/trophy_cabinet_velvet.png", (16, 16)),
    ("A/textures/block/trophy_cabinet_shelf.png", (16, 16)),
    ("A/textures/block/trophy_cabinet_glass.png", (16, 16)),
    ("A/textures/block/wall_cabinet_outer.png", (16, 16)),
    ("A/textures/block/wall_cabinet_outer_tint.png", (16, 16)),
    ("A/textures/block/trophy_plaque_back.png", (16, 16)),
    ("A/textures/block/trophy_plaque_back_tint.png", (16, 16)),
    ("A/textures/block/trophy_plaque_medallion.png", (16, 16)),
    ("A/textures/block/trophy_plaque_strip.png", (16, 16)),
    ("A/textures/block/trophy_spotlight.png", (16, 16)),
    ("A/textures/block/trophy_spotlight_lens_glow.png", (16, 16)),
    ("A/textures/block/trophy_spotlight_cone.png", (16, 16)),
    ("A/textures/block/trim_plate.png", (16, 16)),
    ("A/textures/block/trim_plate_accent.png", (16, 16)),
    ("A/textures/block/trim_orbital.png", (16, 16)),
    ("A/textures/block/trim_orbital_accent.png", (16, 16)),
    ("A/textures/block/trim_laurel.png", (16, 16)),
    ("A/textures/block/trim_laurel_accent.png", (16, 16)),
    ("A/textures/block/trim_wild.png", (16, 16)),
    ("A/textures/block/trim_wild_accent.png", (16, 16)),
    ("A/textures/block/trim_neon.png", (16, 16)),
    ("A/textures/block/trim_neon_accent.png", (16, 16)),
    ("A/textures/block/trim_prism.png", (16, 16)),
    ("A/textures/block/trim_prism_accent.png", (16, 128)),
    ("A/textures/block/trim_prism_accent.png.mcmeta", None),
    ("A/textures/entity/nameplate_blank.png", (10, 2)),
    ("A/textures/entity/nameplate_replica.png", (12, 3)),
    ("A/textures/entity/plate_cover_pedestal.png", (14, 4)),
    ("A/textures/entity/plate_cover_plaque.png", (14, 2)),
    ("A/textures/entity/tier_pips.png", (40, 4)),
    ("A/textures/entity/trophy_ring.png", (16, 16)),
    ("A/textures/item/trophy_bronze.png", (16, 16)),
    ("A/textures/item/trophy_silver.png", (16, 16)),
    ("A/textures/item/trophy_gold.png", (16, 16)),
    ("A/textures/item/trophy_nero.png", (16, 16)),
    ("A/textures/item/trophy_bronze_replica.png", (16, 16)),
    ("A/textures/item/trophy_silver_replica.png", (16, 16)),
    ("A/textures/item/trophy_gold_replica.png", (16, 16)),
    ("A/textures/item/trophy_nero_replica.png", (16, 16)),
    ("A/textures/item/trophy_unknown.png", (16, 16)),
    ("A/textures/item/trophy_book.png", (16, 16)),
    ("A/textures/item/trophy_plaque.png", (16, 16)),
    ("A/textures/item/trophy_spotlight.png", (16, 16)),
    ("A/textures/gui/trophy_book.png", (256, 256)),
    ("A/textures/gui/mod_emblems.png", (128, 48)),
    ("A/textures/gui/trophy_toast.png", (128, 64)),
    ("A/textures/gui/trophy_cabinet.png", (256, 256)),
    ("A/textures/particle/trophy_spark_0.png", (8, 8)),
    ("A/textures/particle/trophy_spark_1.png", (8, 8)),
    ("A/textures/particle/trophy_spark_2.png", (8, 8)),
    ("A/textures/particle/trophy_spark_3.png", (8, 8)),
    ("A/textures/particle/trophy_glint_0.png", (8, 8)),
    ("A/textures/particle/trophy_glint_1.png", (8, 8)),
    ("A/textures/particle/trophy_glint_2.png", (8, 8)),
    ("A/textures/particle/trophy_glint_3.png", (8, 8)),
    ("A/textures/particle/trophy_shard_0.png", (8, 8)),
    ("A/textures/particle/trophy_shard_1.png", (8, 8)),
    ("A/textures/particle/trophy_shard_2.png", (8, 8)),
    ("A/textures/particle/trophy_ring_0.png", (16, 16)),
    ("A/textures/particle/trophy_ring_1.png", (16, 16)),
    ("A/textures/particle/trophy_ring_2.png", (16, 16)),
    ("A/textures/particle/trophy_ring_3.png", (16, 16)),
    ("A/particles/trophy_spark.json", None),
    ("A/particles/trophy_glint.json", None),
    ("A/particles/trophy_shard.json", None),
    ("A/particles/trophy_ring.json", None),
    ("A/particles/trophy_mote.json", None),
    ("A/models/block/trophy_bronze.json", None),
    ("A/models/block/trophy_silver.json", None),
    ("A/models/block/trophy_gold.json", None),
    ("A/models/block/trophy_nero.json", None),
    ("A/models/block/trophy_bronze_replica.json", None),
    ("A/models/block/trophy_silver_replica.json", None),
    ("A/models/block/trophy_gold_replica.json", None),
    ("A/models/block/trophy_nero_replica.json", None),
    ("A/models/block/trophy_ghost.json", None),
    ("A/models/block/display_pedestal.json", None),
    ("A/models/block/trophy_cabinet.json", None),
    ("A/models/block/trophy_cabinet_side_left.json", None),
    ("A/models/block/trophy_cabinet_side_right.json", None),
    ("A/models/block/trophy_cabinet_inventory.json", None),
    ("A/models/block/frame/cabinet_edge_top.json", None),
    ("A/models/block/frame/cabinet_edge_bottom.json", None),
    ("A/models/block/frame/cabinet_edge_left.json", None),
    ("A/models/block/frame/cabinet_edge_right.json", None),
    ("A/models/block/frame/cabinet_corner_tl.json", None),
    ("A/models/block/frame/cabinet_corner_tr.json", None),
    ("A/models/block/frame/cabinet_corner_bl.json", None),
    ("A/models/block/frame/cabinet_corner_br.json", None),
    ("A/models/block/frame/cabinet_crown.json", None),
    ("A/models/block/wall_cabinet.json", None),
    ("A/models/block/wall_cabinet_side_left.json", None),
    ("A/models/block/wall_cabinet_side_right.json", None),
    ("A/models/block/wall_cabinet_inventory.json", None),
    ("A/models/block/frame/wall_cabinet_edge_top.json", None),
    ("A/models/block/frame/wall_cabinet_edge_bottom.json", None),
    ("A/models/block/frame/wall_cabinet_edge_left.json", None),
    ("A/models/block/frame/wall_cabinet_edge_right.json", None),
    ("A/models/block/frame/wall_cabinet_corner_tl.json", None),
    ("A/models/block/frame/wall_cabinet_corner_tr.json", None),
    ("A/models/block/frame/wall_cabinet_corner_bl.json", None),
    ("A/models/block/frame/wall_cabinet_corner_br.json", None),
    ("A/models/block/frame/wall_cabinet_crown.json", None),
    ("A/models/block/trophy_plaque.json", None),
    ("A/models/block/trophy_spotlight_ceiling.json", None),
    ("A/models/block/trophy_spotlight_wall.json", None),
    ("A/models/item/trophy_bronze.json", None),
    ("A/models/item/trophy_silver.json", None),
    ("A/models/item/trophy_gold.json", None),
    ("A/models/item/trophy_nero.json", None),
    ("A/models/item/trophy_bronze_replica.json", None),
    ("A/models/item/trophy_silver_replica.json", None),
    ("A/models/item/trophy_gold_replica.json", None),
    ("A/models/item/trophy_nero_replica.json", None),
    ("A/models/item/trophy_unknown.json", None),
    ("A/models/item/trophy_book.json", None),
    ("A/models/item/trophy_plaque.json", None),
    ("A/models/item/trophy_spotlight.json", None),
    ("A/models/item/display_pedestal.json", None),
    ("A/models/item/trophy_cabinet.json", None),
    ("A/models/item/wall_cabinet.json", None),
    ("A/sounds/award/bronze.ogg", "ogg"),
    ("A/sounds/award/silver.ogg", "ogg"),
    ("A/sounds/award/gold.ogg", "ogg"),
    ("A/sounds/award/nero.ogg", "ogg"),
    ("A/sounds/award/collection.ogg", "ogg"),
    ("A/sounds/book/open.ogg", "ogg"),
    ("A/sounds/book/page1.ogg", "ogg"),
    ("A/sounds/book/page2.ogg", "ogg"),
    ("A/sounds/book/page3.ogg", "ogg"),
    ("A/sounds/book/reclaim.ogg", "ogg"),
    ("A/sounds/block/trophy_place.ogg", "ogg"),
    ("A/sounds/block/pedestal_set.ogg", "ogg"),
    ("A/sounds/block/cabinet_latch1.ogg", "ogg"),
    ("A/sounds/block/cabinet_latch2.ogg", "ogg"),
    ("A/sounds.json", None),
    ("R/art/logo/nerotrophies_logo.png", (1024, 1024)),
    ("R/art/logo/nerotrophies_logo_400.png", (400, 400)),
    ("R/common/src/main/resources/nerotrophies_logo.png", (256, 256)),
    ("APP/assets/trophies/mod_emblems.png", (128, 48)),
]

# Vanilla parents a mod model may extend. Any other minecraft: parent is still accepted (vanilla ships
# it); these are only the ones known to define no texture variables a child must supply.
LEGACY_ITEM_MODEL_BELOW = (1, 21, 4)


class Report:
    def __init__(self):
        self.errors = []
        self.warnings = []

    def error(self, where, msg):
        self.errors.append(f"{where}: {msg}")

    def warn(self, where, msg):
        self.warnings.append(f"{where}: {msg}")


def parse_version(text):
    parts = []
    for p in text.split("."):
        if not p.isdigit():
            return None
        parts.append(int(p))
    return tuple(parts)


def split_rl(ref, default_ns="minecraft"):
    if ":" in ref:
        ns, path = ref.split(":", 1)
        return ns, path
    return default_ns, ref


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if len(head) < 24 or head[:8] != b"\x89PNG\r\n\x1a\n" or head[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", head[16:24])


def ogg_format(path):
    """(channels, sample_rate) from an Ogg Vorbis identification header, or None."""
    with open(path, "rb") as f:
        head = f.read(4096)
    if not head.startswith(b"OggS"):
        return None
    i = head.find(b"\x01vorbis")
    if i < 0 or len(head) < i + 16:
        return None
    channels = head[i + 11]
    rate = struct.unpack("<I", head[i + 12:i + 16])[0]
    return channels, rate


JAVA_STRING = re.compile(r'"((?:[^"\\\n]|\\.)*)"')


def strip_comments(src):
    out = []
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            out.append(src[i:j + 1])
            i = j + 1
        elif src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


class Checker:
    def __init__(self, repo, siblings=True, checklist=None):
        self.repo = os.path.abspath(repo)
        self.assets = os.path.join(self.repo, ASSETS)
        self.siblings = siblings
        self.checklist = CHECKLIST if checklist is None else checklist
        self.rep = Report()
        self.json_cache = {}
        self.models_checked = {}
        self.models_used = set()
        self.textures_used = set()
        self.java = {}
        for root in JAVA_ROOTS:
            base = os.path.join(self.repo, root)
            for dirpath, _, files in os.walk(base):
                for name in files:
                    if name.endswith(".java"):
                        path = os.path.join(dirpath, name)
                        with open(path, encoding="utf-8", errors="replace") as f:
                            self.java[os.path.relpath(path, self.repo).replace(os.sep, "/")] = strip_comments(f.read())
        self.java_literals = set()
        for src in self.java.values():
            self.java_literals.update(JAVA_STRING.findall(src))

    # ----------------------------------------------------------------- helpers

    def rel(self, path):
        return os.path.relpath(path, self.repo).replace(os.sep, "/")

    def asset(self, *parts):
        return os.path.join(self.assets, *parts)

    def load_json(self, path):
        if path in self.json_cache:
            return self.json_cache[path]
        data = None
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, ValueError) as e:
            self.rep.error(self.rel(path), f"unreadable JSON ({e.__class__.__name__}: {e})")
        self.json_cache[path] = data
        return data

    # ---------------------------------------------------------------- registry

    def constants(self):
        src = self.java.get(REGISTRY + "/TrophyIds.java", "")
        return dict(re.findall(r'static\s+final\s+String\s+(\w+)\s*=\s*"([^"]*)"\s*;', src))

    def resolve_id(self, token, consts, where):
        token = token.strip()
        if token.startswith('"'):
            return token.strip('"')
        name = token.split(".")[-1]
        if name in consts:
            return consts[name]
        self.rep.error(where, f"cannot resolve id constant {token}")
        return None

    def ids(self, rel_path, pattern, consts):
        src = self.java.get(rel_path)
        if src is None:
            self.rep.error(rel_path, "registry source not found")
            return []
        out = []
        for token in re.findall(pattern, src):
            value = self.resolve_id(token, consts, rel_path)
            if value and value not in out:
                out.append(value)
        return out

    def read_registries(self):
        consts = self.constants()
        idtok = r'(TrophyIds\.\w+|"[^"]+")'
        self.blocks = self.ids(REGISTRY + "/ModBlocks.java", r'(?:BLOCKS\.register|\bdisplay)\(\s*' + idtok, consts)
        self.items = self.ids(REGISTRY + "/ModItems.java", r'(?:ITEMS\.register|\bblock)\(\s*' + idtok, consts)
        self.menus = self.ids(REGISTRY + "/ModMenuTypes.java", r'MENUS\.register\(\s*' + idtok, consts)
        self.sounds = []
        self.particles = []
        for path, src in sorted(self.java.items()):
            if "createVariableRangeEvent" in src or "createFixedRangeEvent" in src or "SOUND_EVENT" in src:
                for tok in re.findall(r'\bevent\(\s*' + idtok + r'\s*\)', src) + re.findall(
                        r'create(?:VariableRange|FixedRange)Event\(\s*TrophyIds\.id\(\s*' + idtok, src):
                    v = self.resolve_id(tok, consts, path)
                    if v and v not in self.sounds:
                        self.sounds.append(v)
                if "SOUND_EVENT" in src:
                    for tok in re.findall(r'\.register\(\s*' + idtok, src):
                        v = self.resolve_id(tok, consts, path)
                        if v and v not in self.sounds:
                            self.sounds.append(v)
            if "PARTICLE_TYPE" in src:
                for tok in re.findall(r'\.register\(\s*' + idtok, src):
                    v = self.resolve_id(tok, consts, path)
                    if v and v not in self.particles:
                        self.particles.append(v)
        self.trims = []
        src = self.java.get(TRIMS)
        if src is not None:
            for tok in re.findall(r'new\s+Trim\(\s*' + idtok, src):
                v = self.resolve_id(tok, consts, TRIMS)
                if v and v not in self.trims:
                    self.trims.append(v)
        if not self.blocks:
            self.rep.error(REGISTRY + "/ModBlocks.java", "no block registrations found")
        if not self.items:
            self.rep.error(REGISTRY + "/ModItems.java", "no item registrations found")

    def needs_legacy_item_models(self):
        versions = []
        for loader in LOADERS:
            d = os.path.join(self.repo, loader, "versions")
            if os.path.isdir(d):
                versions += [v for v in (parse_version(n) for n in os.listdir(d)) if v]
        if not versions or min(versions) >= LEGACY_ITEM_MODEL_BELOW:
            return False
        # A resources-<mc> overlay at or below the oldest node would ship its own item models.
        base = os.path.join(self.repo, "common/src/main")
        for name in (os.listdir(base) if os.path.isdir(base) else []):
            if name.startswith("resources-"):
                v = parse_version(name[len("resources-"):])
                if v and v <= min(versions) and os.path.isdir(os.path.join(base, name, "assets", MOD_ID, "models", "item")):
                    return False
        return True

    # ------------------------------------------------------------------ models

    def texture_exists(self, ref, where):
        ns, path = split_rl(ref)
        if ns == "minecraft":
            return
        if ns != MOD_ID:
            self.rep.error(where, f"texture {ref} is in a foreign namespace")
            return
        self.textures_used.add(path)
        if not os.path.isfile(self.asset("textures", path + ".png")):
            self.rep.error(where, f"texture {ref} missing: {ASSETS}/textures/{path}.png")

    def model_chain(self, ref, where):
        """The model and its parents as (ref, json) within the mod; stops at a vanilla parent."""
        chain = []
        seen = set()
        while ref:
            ns, path = split_rl(ref)
            if path.startswith("builtin/"):
                break
            if ns == "minecraft":
                break
            if ns != MOD_ID:
                self.rep.error(where, f"model {ref} is in a foreign namespace")
                break
            key = ns + ":" + path
            if key in seen:
                self.rep.error(where, f"model parent cycle at {key}")
                break
            seen.add(key)
            self.models_used.add(path)
            file = self.asset("models", path + ".json")
            if not os.path.isfile(file):
                self.rep.error(where, f"model {key} missing: {ASSETS}/models/{path}.json")
                break
            data = self.load_json(file)
            if not isinstance(data, dict):
                break
            chain.append((key, data))
            where = f"{ASSETS}/models/{path}.json"
            ref = data.get("parent")
        return chain

    def check_model(self, ref, where):
        ns, path = split_rl(ref)
        key = ns + ":" + path
        if key in self.models_checked:
            return
        self.models_checked[key] = True
        chain = self.model_chain(ref, where)
        if not chain:
            return
        textures = {}
        elements = None
        for mkey, data in reversed(chain):
            mwhere = f"{ASSETS}/models/{split_rl(mkey)[1]}.json"
            tex = data.get("textures") or {}
            if not isinstance(tex, dict):
                self.rep.error(mwhere, "textures is not an object")
                tex = {}
            for var, value in tex.items():
                if isinstance(value, str) and not value.startswith("#"):
                    self.texture_exists(value, mwhere)
                elif isinstance(value, dict) and isinstance(value.get("sprite"), str):
                    self.texture_exists(value["sprite"], mwhere)
            textures.update(tex)
            if "elements" in data:
                elements = (mwhere, data["elements"])
            for override in data.get("overrides") or []:
                if isinstance(override, dict) and isinstance(override.get("model"), str):
                    self.check_model(override["model"], mwhere)
        leaf_where = f"{ASSETS}/models/{path}.json"

        def resolve(var, depth=0):
            value = textures.get(var)
            if isinstance(value, str) and value.startswith("#") and depth < 16:
                return resolve(value[1:], depth + 1)
            return value

        for var, value in textures.items():
            if isinstance(value, str) and value.startswith("#") and resolve(var) is None:
                self.rep.error(leaf_where, f"texture variable #{var} -> {value} never resolves")
        if elements is not None:
            ewhere, els = elements
            used = set()
            for el in els if isinstance(els, list) else []:
                for face in ((el or {}).get("faces") or {}).values():
                    tex = (face or {}).get("texture")
                    if isinstance(tex, str) and tex.startswith("#"):
                        used.add(tex[1:])
            for var in sorted(used):
                if resolve(var) is None:
                    self.rep.error(leaf_where, f"face texture #{var} (from {ewhere}) is not defined in the model chain")

    def models_in(self, node, where):
        """Every model id an item-model definition or blockstate fragment references."""
        if isinstance(node, dict):
            for k, v in node.items():
                if k in ("model", "base") and isinstance(v, str):
                    self.check_model(v, where)
                else:
                    self.models_in(v, where)
        elif isinstance(node, list):
            for v in node:
                self.models_in(v, where)

    # ------------------------------------------------------------------ checks

    def check_blocks(self):
        for block in self.blocks:
            rel = f"{ASSETS}/blockstates/{block}.json"
            path = self.asset("blockstates", block + ".json")
            if not os.path.isfile(path):
                self.rep.error(f"block {MOD_ID}:{block}", f"blockstate missing: {rel}")
                continue
            data = self.load_json(path)
            if not isinstance(data, dict):
                continue
            if "variants" not in data and "multipart" not in data:
                self.rep.error(rel, "neither variants nor multipart")
            for variant in (data.get("variants") or {}).values():
                self.models_in(variant, rel)
            for part in data.get("multipart") or []:
                self.models_in((part or {}).get("apply"), rel)

    def check_items(self):
        legacy = self.needs_legacy_item_models()
        for item in self.items:
            rel = f"{ASSETS}/items/{item}.json"
            path = self.asset("items", item + ".json")
            if not os.path.isfile(path):
                self.rep.error(f"item {MOD_ID}:{item}", f"item-model definition missing: {rel}")
            else:
                data = self.load_json(path)
                if isinstance(data, dict):
                    if not isinstance(data.get("model"), dict):
                        self.rep.error(rel, "no model object")
                    self.models_in(data.get("model"), rel)
            if legacy:
                if not os.path.isfile(self.asset("models", "item", item + ".json")):
                    self.rep.error(f"item {MOD_ID}:{item}",
                                   f"legacy item model (pre-1.21.4 nodes) missing: {ASSETS}/models/item/{item}.json")
                else:
                    self.check_model(f"{MOD_ID}:item/{item}", f"item {MOD_ID}:{item}")

    def check_sounds(self):
        path = self.asset("sounds.json")
        self.sound_subtitles = []
        if not os.path.isfile(path):
            if self.sounds:
                self.rep.error(f"{ASSETS}/sounds.json", "missing, but Java creates sound events")
            return
        data = self.load_json(path)
        if not isinstance(data, dict):
            return
        where = f"{ASSETS}/sounds.json"
        for event, body in data.items():
            body = body or {}
            sub = body.get("subtitle")
            if sub:
                self.sound_subtitles.append((sub, f"{where} {event}"))
            else:
                self.rep.warn(where, f"event {event} has no subtitle")
            for entry in body.get("sounds") or []:
                if isinstance(entry, dict):
                    if entry.get("type") == "event":
                        continue
                    name = entry.get("name")
                else:
                    name = entry
                if not isinstance(name, str):
                    self.rep.error(where, f"event {event} has a sound without a name")
                    continue
                ns, p = split_rl(name)
                if ns == "minecraft":
                    continue
                if not os.path.isfile(self.asset("sounds", p + ".ogg")):
                    self.rep.error(where, f"event {event}: sound file missing: {ASSETS}/sounds/{p}.ogg")
            if event not in self.sounds and event not in self.java_literals and \
                    not any(lit.endswith("." + event.split(".")[-1]) and event.startswith(lit) for lit in self.java_literals):
                self.rep.warn(where, f"event {event} is never created or referenced in Java")
        for event in self.sounds:
            if event not in data:
                self.rep.error(f"sound {MOD_ID}:{event}", f"created in Java but not defined in {where}")

    def check_particles(self):
        pdir = self.asset("particles")
        defined = sorted(n[:-5] for n in os.listdir(pdir) if n.endswith(".json")) if os.path.isdir(pdir) else []
        for name in defined:
            rel = f"{ASSETS}/particles/{name}.json"
            data = self.load_json(os.path.join(pdir, name + ".json"))
            if not isinstance(data, dict):
                continue
            texs = data.get("textures")
            if not isinstance(texs, list) or not texs:
                self.rep.error(rel, "no textures list")
                continue
            for ref in texs:
                ns, p = split_rl(ref)
                if ns == "minecraft":
                    continue
                self.textures_used.add("particle/" + p)
                if not os.path.isfile(self.asset("textures", "particle", p + ".png")):
                    self.rep.error(rel, f"texture {ref} missing: {ASSETS}/textures/particle/{p}.png")
            if name not in self.particles:
                self.rep.warn(rel, f"no particle type {MOD_ID}:{name} is registered in Java, so it never spawns")
        for name in self.particles:
            if name not in defined:
                self.rep.error(f"particle {MOD_ID}:{name}", f"definition missing: {ASSETS}/particles/{name}.json")

    def java_lang_keys(self):
        keys = {}
        for path, src in self.java.items():
            for m in re.finditer(r'\btranslatable(?:WithFallback)?\s*\(', src):
                depth, i = 1, m.end()
                start = i
                while i < len(src) and depth:
                    c = src[i]
                    if c == '"':
                        i += 1
                        while i < len(src) and src[i] != '"':
                            i += 2 if src[i] == "\\" else 1
                    elif c == "(":
                        depth += 1
                    elif c == ")":
                        depth -= 1
                    i += 1
                args = src[start:i - 1]
                # Only the first argument is the key; later ones are format arguments.
                first, d, j = [], 0, 0
                while j < len(args):
                    c = args[j]
                    if c == '"':
                        k = j + 1
                        while k < len(args) and args[k] != '"':
                            k += 2 if args[k] == "\\" else 1
                        first.append(args[j:k + 1])
                        j = k + 1
                        continue
                    if c in "([{":
                        d += 1
                    elif c in ")]}":
                        d -= 1
                    elif c == "," and d == 0:
                        break
                    first.append(c)
                    j += 1
                first = "".join(first)
                for lm in re.finditer(r'"([^"\\]*)"', first):
                    before = first[:lm.start()].rstrip()
                    after = first[lm.end():].lstrip()
                    key = lm.group(1)
                    if before.endswith("+") or after.startswith("+") or after.startswith("."):
                        continue  # a built key: only its stem is literal
                    if not re.fullmatch(r"[A-Za-z0-9_.\-]+", key) or MOD_ID not in key.split("."):
                        continue
                    keys.setdefault(key, path)
        return keys

    def check_lang(self):
        lang_path = self.asset("lang", "en_us.json")
        lang = self.load_json(lang_path) if os.path.isfile(lang_path) else None
        if lang is None:
            self.rep.error(f"{ASSETS}/lang/en_us.json", "missing or unreadable")
            lang = {}
        pending = {}
        pdir = os.path.join(self.repo, "tools", "lang", "pending")
        if os.path.isdir(pdir):
            for name in sorted(os.listdir(pdir)):
                if name.endswith(".json"):
                    data = self.load_json(os.path.join(pdir, name))
                    if isinstance(data, dict):
                        for k in data:
                            pending.setdefault(k, "tools/lang/pending/" + name)
        need = []
        for b in self.blocks:
            need.append((f"block.{MOD_ID}.{b}", f"block {MOD_ID}:{b}"))
        for it in self.items:
            if it not in self.blocks:
                need.append((f"item.{MOD_ID}.{it}", f"item {MOD_ID}:{it}"))
        need.append((f"itemGroup.{MOD_ID}", "creative tab"))
        for m in self.menus:
            need.append((f"container.{MOD_ID}.{m}", f"menu {MOD_ID}:{m}"))
        for t in self.trims:
            need.append((f"trim.{MOD_ID}.{t}", f"trim {t}"))
        need += getattr(self, "sound_subtitles", [])
        need += [(k, p) for k, p in sorted(self.java_lang_keys().items())]
        seen = set()
        for key, why in need:
            if key in seen:
                continue
            seen.add(key)
            if key in lang:
                continue
            if key in pending:
                self.rep.warn(f"{ASSETS}/lang/en_us.json", f"{key} (for {why}) is only in {pending[key]}, not merged yet")
            else:
                self.rep.error(f"{ASSETS}/lang/en_us.json", f"missing {key} (for {why})")

    def check_checklist(self):
        for path, expected in self.checklist:
            if path.startswith("APP/"):
                if not self.siblings:
                    continue
                full = os.path.join(os.path.dirname(self.repo), APP_SIBLING, path[4:])
                shown = f"../{APP_SIBLING}/{path[4:]}"
            elif path.startswith("A/"):
                full = os.path.join(self.assets, path[2:])
                shown = f"{ASSETS}/{path[2:]}"
            elif path.startswith("R/"):
                full = os.path.join(self.repo, path[2:])
                shown = path[2:]
            else:
                full = os.path.join(self.repo, path)
                shown = path
            if not os.path.isfile(full):
                self.rep.error(shown, "checklist file missing")
                continue
            if isinstance(expected, tuple):
                size = png_size(full)
                if size is None:
                    self.rep.error(shown, "not a PNG")
                elif tuple(size) != tuple(expected):
                    self.rep.error(shown, f"is {size[0]}x{size[1]}, checklist wants {expected[0]}x{expected[1]}")
            elif expected == "ogg":
                fmt = ogg_format(full)
                if fmt is None:
                    self.rep.error(shown, "not an Ogg Vorbis file")
                elif fmt != (1, 32000):
                    self.rep.error(shown, f"is {fmt[0]} channel(s) at {fmt[1]} Hz, checklist wants mono 32 kHz")
            if path.startswith("APP/") and path.endswith("mod_emblems.png"):
                atlas = os.path.join(self.assets, "textures", "gui", "mod_emblems.png")
                if os.path.isfile(atlas):
                    with open(full, "rb") as a, open(atlas, "rb") as b:
                        if a.read() != b.read():
                            self.rep.error(shown, f"not byte-identical to {ASSETS}/textures/gui/mod_emblems.png")

    def check_mcmeta(self):
        tdir = self.asset("textures")
        for dirpath, _, files in os.walk(tdir):
            for name in files:
                if name.endswith(".png.mcmeta"):
                    png = os.path.join(dirpath, name[:-7])
                    rel = self.rel(os.path.join(dirpath, name))
                    if not os.path.isfile(png):
                        self.rep.error(rel, "animation metadata without its PNG")
                        continue
                    data = self.load_json(os.path.join(dirpath, name))
                    size = png_size(png)
                    if isinstance(data, dict) and "animation" in data and size and size[1] % size[0]:
                        self.rep.error(rel, f"animated PNG is {size[0]}x{size[1]}: height is not a multiple of width")

    def check_orphans(self):
        literals = self.java_literals

        def referenced(kind, path):
            stem = path.rsplit("/", 1)[-1]
            for lit in literals:
                if lit.endswith(f"{kind}/{path}.png") or lit.endswith(f"{kind}/{path}.json") or \
                        lit in (path, f"{MOD_ID}:{path}") or lit.endswith("/" + path) or lit == stem:
                    return True
            return False

        tdir = self.asset("textures")
        for dirpath, _, files in os.walk(tdir):
            for name in sorted(files):
                if not name.endswith(".png"):
                    continue
                path = os.path.relpath(os.path.join(dirpath, name[:-4]), tdir).replace(os.sep, "/")
                if path not in self.textures_used and not referenced("textures", path):
                    self.rep.warn(f"{ASSETS}/textures/{path}.png", "orphaned texture: no model, particle or Java reference")
        mdir = self.asset("models")
        for dirpath, _, files in os.walk(mdir):
            for name in sorted(files):
                if not name.endswith(".json"):
                    continue
                path = os.path.relpath(os.path.join(dirpath, name[:-5]), mdir).replace(os.sep, "/")
                if path not in self.models_used and not referenced("models", path):
                    self.rep.warn(f"{ASSETS}/models/{path}.json", "orphaned model: no blockstate, item, parent or Java reference")
        for folder, known in (("blockstates", self.blocks), ("items", self.items)):
            d = self.asset(folder)
            for name in sorted(os.listdir(d)) if os.path.isdir(d) else []:
                if name.endswith(".json") and name[:-5] not in known:
                    self.rep.warn(f"{ASSETS}/{folder}/{name}", f"no registered {folder[:-1].replace('blockstate', 'block')} uses it")

    def run(self):
        self.read_registries()
        self.check_blocks()
        self.check_items()
        self.check_sounds()
        self.check_particles()
        self.check_lang()
        self.check_checklist()
        self.check_mcmeta()
        self.check_orphans()
        return self.rep


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--repo", default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    help="repository root (default: the folder above tools/)")
    ap.add_argument("--no-siblings", action="store_true", help="skip checks that need sibling repositories")
    ap.add_argument("--strict", action="store_true", help="treat warnings as errors")
    args = ap.parse_args(argv)
    checker = Checker(args.repo, siblings=not args.no_siblings)
    rep = checker.run()
    for w in rep.warnings:
        print("WARNING " + w)
    for e in rep.errors:
        print("ERROR " + e)
    print(f"check_assets: {len(checker.blocks)} blocks, {len(checker.items)} items, {len(checker.menus)} menus, "
          f"{len(checker.sounds)} Java sound events, {len(checker.particles)} particle types, {len(checker.trims)} trims; "
          f"{len(rep.errors)} error(s), {len(rep.warnings)} warning(s)")
    return 1 if rep.errors or (args.strict and rep.warnings) else 0


if __name__ == "__main__":
    sys.exit(main())
