#!/usr/bin/env python3
"""Validates NeroTrophies' trophy catalogue, collections and quest pack data.

Checks, all offline:

* Schema: every file under ``data/<ns>/neroland_trophies/`` and
  ``data/<ns>/neroland_trophy_collections/`` decodes the way Neroland Core 1.20.0's
  ``TrophyDefinition`` / ``TrophyCollection`` codecs decode it (same fields, patterns, bounds and
  trigger shapes). Unknown keys are reported too: Core ignores them, but in this catalogue they
  are typos.
* Lang: every title/description key (written or defaulted) exists in
  ``assets/nerotrophies/lang/en_us.json`` or in ``tools/lang/pending/*.json``; so do the quest
  pack's chapter and quest keys.
* Icons: vanilla icons exist in a Minecraft jar found in the Gradle cache; sibling icons exist
  in ``../<repo>`` when that repository is checked out next to this one, else they are skipped.
* Ids: no duplicate trophy ids, no trophy/collection id clash, every collection member and
  completion trophy exists.
* Collections: 2 to 24 members, a known trim, no hidden members, ``requires_mod`` consistent with
  the members, completion trophies have no trigger and are not members of their own collection.
* Catalogue rules: no nero tier on a bare ``item_obtained`` (or any polled trigger), at most one
  nero trophy per Book category, no two trophies on the same trigger, no repeated-use grind
  (huge counts, idle-time stats), PLAYER-scope gates and milestones only, no NeroQuests reward
  hook gates outside the allow-list, and advancement ids that exist on every MC line (an id
  that only exists in a ``resources-<mc>`` overlay is reported as a warning).

Sibling checks are conditional: on CI, where only this repository is checked out, they are
skipped and the run still passes. Exits 1 if any error is found.

Usage: ``python3 tools/validate_trophies.py [--repo DIR] [--siblings DIR | --no-siblings]
[--vanilla-jar JAR ...] [--quiet]``
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys
import zipfile

# ---- Core 1.20.0 codec rules -------------------------------------------------------------------

MAX_FILE_BYTES = 16 * 1024
MAX_ID_LENGTH = 128
MAX_MEMBERS_CORE = 256
MAX_REQUIRED_MODS = 8
INT_MIN, INT_MAX = -(2 ** 31), 2 ** 31 - 1
LANG_KEY = re.compile(r"[a-z0-9_.-]{1,128}")
RESOLVED_KEY = re.compile(r"[a-z0-9_.-]{1,192}")
MOD_ID = re.compile(r"[a-z0-9_.-]{1,64}")
TRIM = re.compile(r"[a-z0-9_]{1,32}")
NAMESPACE = re.compile(r"[a-z0-9_.-]+")
PATH = re.compile(r"[a-z0-9_./-]+")
TIERS = ("bronze", "silver", "gold", "nero")
TROPHY_DIR = "neroland_trophies"
COLLECTION_DIR = "neroland_trophy_collections"

DEFINITION_KEYS = {"schema", "icon", "tier", "trigger", "hidden", "title", "description", "requires_mod",
                   "category", "reward", "order"}
COLLECTION_KEYS = {"schema", "icon", "title", "description", "members", "trim", "trophy", "requires_mod"}
TRIGGER_FIELDS = {
    "advancement": {"advancement"},
    "stat": {"stat_type", "stat", "at_least"},
    "item_obtained": {"item", "count"},
    "block_placed": {"block", "count"},
    "dimension_visited": {"dimension"},
}
NERO_EVENT_FIELDS = {
    "gate": {"gate"},
    "material_milestone": {"milestone", "material"},
    "reputation": {"faction", "at_least"},
    "currency": {"currency", "at_least"},
}

# ---- Catalogue rules ---------------------------------------------------------------------------

#: NeroTrophies' own collections fit one four-cabinet floor pool (4 x 6 slots).
MAX_MEMBERS_CATALOGUE = 24
#: Shipped trims (the cabinet trim table); read from block/Trims.java when present.
DEFAULT_TRIMS = ("standard", "orbital_plasma", "orbital_amber", "laurel_gold", "laurel_crimson",
                 "laurel_copper", "wild_green", "wild_sand", "wild_teal", "neon_pink", "neon_rose", "prism")
#: NeroQuests reward-hook gates a trophy may name (they are opened per player by a quest reward).
HOOK_GATE_ALLOW = {"nerocompanion:hook/skin/aurora", "neroarcade:hook/blank_cartridges"}
#: Trigger types that are polled counts, never a hardest milestone: no nero trophy on them.
NO_NERO_TRIGGERS = {"item_obtained", "block_placed", "stat", "dimension_visited"}
#: Repeated-use grind bounds.
MAX_ITEM_COUNT = 256
MAX_REPEAT_STAT = 500
REPEAT_STAT_TYPES = {"minecraft:used", "minecraft:mined", "minecraft:crafted", "minecraft:picked_up",
                     "minecraft:dropped", "minecraft:broken", "minecraft:killed", "minecraft:killed_by"}
IDLE_STATS = {"minecraft:play_time", "minecraft:play_one_minute", "minecraft:total_world_time",
              "minecraft:time_since_death", "minecraft:time_since_rest", "minecraft:sneak_time"}
#: Mod id -> sibling repository folder, where they differ.
REPO_FOR_MOD = {"nerolandcore": "neroland-core"}


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.notes: list[str] = []

    def error(self, where: str, msg: str) -> None:
        self.errors.append(f"{where}: {msg}")

    def warn(self, where: str, msg: str) -> None:
        self.warnings.append(f"{where}: {msg}")

    def note(self, msg: str) -> None:
        if msg not in self.notes:
            self.notes.append(msg)


# ---- Codec helpers -----------------------------------------------------------------------------

def parse_id(value, default_ns: str = "minecraft"):
    """Identifier.CODEC: ``ns:path`` or a bare path in ``minecraft``. Returns the string or None."""
    if not isinstance(value, str) or len(value) > MAX_ID_LENGTH or value.count(":") > 1:
        return None
    ns, path = value.split(":", 1) if ":" in value else (default_ns, value)
    if not NAMESPACE.fullmatch(ns) or not PATH.fullmatch(path):
        return None
    return f"{ns}:{path}"


def is_whole(value, lo: int, hi: int) -> bool:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return False
    return float(value).is_integer() and lo <= value <= hi


def mod_ids(value):
    """``requires_mod``: one mod id or a list of 1..8. Returns a list or None."""
    mods = [value] if isinstance(value, str) else value
    if not isinstance(mods, list) or not 1 <= len(mods) <= MAX_REQUIRED_MODS:
        return None
    if not all(isinstance(m, str) and MOD_ID.fullmatch(m) for m in mods):
        return None
    return list(mods)


def need_id(rep: Report, where: str, obj: dict, key: str, required: bool = True):
    if key not in obj:
        if required:
            rep.error(where, f"missing '{key}'")
        return None
    parsed = parse_id(obj[key])
    if parsed is None:
        rep.error(where, f"'{key}' is not a valid identifier (<= {MAX_ID_LENGTH} chars)")
    return parsed


def decode_trigger(rep: Report, where: str, trig) -> dict | None:
    if not isinstance(trig, dict):
        rep.error(where, "'trigger' must be an object")
        return None
    ttype = trig.get("type")
    out = {"type": ttype}
    if ttype == "nero_event":
        event = trig.get("event")
        if event == "threshold":
            rep.error(where, "nero_event 'threshold' is reserved and rejected")
            return None
        if event not in NERO_EVENT_FIELDS:
            rep.error(where, f"unknown nero_event '{event}'")
            return None
        out["event"] = event
        allowed = {"type", "event"} | NERO_EVENT_FIELDS[event]
        if event == "gate":
            out["target"] = need_id(rep, where, trig, "gate")
        elif event == "material_milestone":
            out["target"] = need_id(rep, where, trig, "milestone")
            out["material"] = need_id(rep, where, trig, "material", required=False)
        elif event == "reputation":
            out["target"] = need_id(rep, where, trig, "faction")
            if not is_whole(trig.get("at_least"), INT_MIN, INT_MAX):
                rep.error(where, "reputation 'at_least' must be a whole number")
            out["at_least"] = trig.get("at_least")
        else:
            out["target"] = (need_id(rep, where, trig, "currency", required=False)
                             or "nerolandcore:credits")
            if not is_whole(trig.get("at_least"), 1, INT_MAX):
                rep.error(where, "currency 'at_least' must be a whole number >= 1")
            out["at_least"] = trig.get("at_least")
    elif ttype in TRIGGER_FIELDS:
        allowed = {"type"} | TRIGGER_FIELDS[ttype]
        if ttype == "advancement":
            out["target"] = need_id(rep, where, trig, "advancement")
        elif ttype == "stat":
            out["stat_type"] = need_id(rep, where, trig, "stat_type", required=False) or "minecraft:custom"
            out["target"] = need_id(rep, where, trig, "stat")
            if not is_whole(trig.get("at_least"), 1, INT_MAX):
                rep.error(where, "stat 'at_least' must be a whole number >= 1")
            out["at_least"] = trig.get("at_least")
        elif ttype in ("item_obtained", "block_placed"):
            key = "item" if ttype == "item_obtained" else "block"
            out["target"] = need_id(rep, where, trig, key)
            count = trig.get("count", 1)
            if not is_whole(count, 1, INT_MAX):
                rep.error(where, "'count' must be a whole number >= 1")
            out["count"] = count
        else:
            out["target"] = need_id(rep, where, trig, "dimension")
    else:
        rep.error(where, f"unknown trigger type '{ttype}'")
        return None
    for extra in sorted(set(trig) - allowed):
        rep.error(where, f"unknown trigger field '{extra}' (typo?)")
    return out


def common_fields(rep: Report, where: str, data: dict, allowed: set) -> None:
    for extra in sorted(set(data) - allowed):
        rep.error(where, f"unknown key '{extra}' (Core ignores it; here it is a typo)")
    if "schema" in data and not is_whole(data["schema"], 1, INT_MAX):
        rep.error(where, "'schema' must be a whole number >= 1")
    for key in ("title", "description"):
        if key in data and not (isinstance(data[key], str) and LANG_KEY.fullmatch(data[key])):
            rep.error(where, f"'{key}' must be a lang key matching {LANG_KEY.pattern}")


def default_key(prefix: str, ident: str, suffix: str) -> str:
    ns, path = ident.split(":", 1)
    return f"{prefix}.{ns}.{path.replace('/', '.')}.{suffix}"


def read_json(rep: Report, path: str, where: str):
    if os.path.getsize(path) > MAX_FILE_BYTES:
        rep.error(where, f"file larger than {MAX_FILE_BYTES} bytes")
        return None
    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    except (ValueError, UnicodeDecodeError) as exc:
        rep.error(where, f"not valid JSON ({exc.__class__.__name__})")
        return None
    if not isinstance(data, dict):
        rep.error(where, "top level must be an object")
        return None
    return data


def list_data(resource_roots: list, directory: str):
    """Yields (id, file, root) for every JSON under data/<ns>/<directory>/ in each resources root."""
    for root in resource_roots:
        for file in sorted(glob.glob(os.path.join(root, "data", "*", directory, "**", "*.json"), recursive=True)):
            rel = os.path.relpath(file, os.path.join(root, "data")).replace(os.sep, "/")
            ns, _, rest = rel.partition("/")
            path = rest[len(directory) + 1:-len(".json")]
            yield f"{ns}:{path}", file, root


# ---- Load ----------------------------------------------------------------------------------------

def load_definitions(rep: Report, roots: list) -> dict:
    trophies = {}
    for ident, file, _ in list_data(roots, TROPHY_DIR):
        where = ident
        if parse_id(ident) is None:
            rep.error(where, "file path is not a valid identifier")
            continue
        if ident in trophies:
            rep.error(where, "duplicate trophy id (defined in two resource roots)")
            continue
        data = read_json(rep, file, where)
        if data is None:
            continue
        n_err = len(rep.errors)
        common_fields(rep, where, data, DEFINITION_KEYS)
        icon = need_id(rep, where, data, "icon")
        tier = data.get("tier")
        if tier not in TIERS:
            rep.error(where, f"'tier' must be one of {', '.join(TIERS)}")
        trigger = decode_trigger(rep, where, data["trigger"]) if "trigger" in data else None
        hidden = data.get("hidden", False)
        if not isinstance(hidden, bool):
            rep.error(where, "'hidden' must be true or false")
        requires = []
        if "requires_mod" in data:
            requires = mod_ids(data["requires_mod"])
            if requires is None:
                rep.error(where, "'requires_mod' must be a mod id or a list of 1 to 8 mod ids")
                requires = []
        if "category" in data and not (isinstance(data["category"], str) and MOD_ID.fullmatch(data["category"])):
            rep.error(where, "'category' must be a mod id")
        if "reward" in data:
            reward = data["reward"]
            if not isinstance(reward, dict) or not is_whole(reward.get("coins"), 0, 10_000):
                rep.error(where, "'reward' must be {\"coins\": 0..10000}")
        if "order" in data and not is_whole(data["order"], INT_MIN, INT_MAX):
            rep.error(where, "'order' must be a whole number")
        title = data.get("title") or default_key("trophy", ident, "title")
        desc = data.get("description") or default_key("trophy", ident, "desc")
        for key in (title, desc):
            if not RESOLVED_KEY.fullmatch(key):
                rep.error(where, f"lang key '{key}' is longer than 192 characters")
        category = data.get("category") or (requires[0] if requires else ident.split(":")[0])
        if len(rep.errors) != n_err and (icon is None or tier not in TIERS):
            continue
        trophies[ident] = {"icon": icon, "tier": tier, "trigger": trigger, "hidden": hidden is True,
                           "requires": requires, "title": title, "description": desc, "category": category,
                           "has_trigger": "trigger" in data}
    return trophies


def load_collections(rep: Report, roots: list) -> dict:
    collections = {}
    for ident, file, _ in list_data(roots, COLLECTION_DIR):
        where = ident
        if parse_id(ident) is None:
            rep.error(where, "file path is not a valid identifier")
            continue
        if ident in collections:
            rep.error(where, "duplicate collection id (defined in two resource roots)")
            continue
        data = read_json(rep, file, where)
        if data is None:
            continue
        common_fields(rep, where, data, COLLECTION_KEYS)
        icon = need_id(rep, where, data, "icon")
        members_raw = data.get("members")
        members = []
        if not isinstance(members_raw, list):
            rep.error(where, "missing 'members' list")
        else:
            for m in members_raw:
                parsed = parse_id(m)
                if parsed is None:
                    rep.error(where, f"member '{m}' is not a valid identifier")
                else:
                    members.append(parsed)
        trim = data.get("trim")
        if not (isinstance(trim, str) and TRIM.fullmatch(trim)):
            rep.error(where, f"'trim' must match {TRIM.pattern}")
        trophy = need_id(rep, where, data, "trophy", required=False)
        requires = []
        if "requires_mod" in data:
            requires = mod_ids(data["requires_mod"])
            if requires is None:
                rep.error(where, "'requires_mod' must be a mod id or a list of 1 to 8 mod ids")
                requires = []
        collections[ident] = {"icon": icon, "members": members, "trim": trim, "trophy": trophy,
                              "requires": requires,
                              "title": data.get("title") or default_key("collection", ident, "title"),
                              "description": data.get("description") or default_key("collection", ident, "desc")}
    return collections


# ---- Environment: lang, trims, vanilla, siblings ---------------------------------------------

def load_lang(rep: Report, repo: str) -> set:
    keys = set()
    files = [os.path.join(repo, "common/src/main/resources/assets/nerotrophies/lang/en_us.json")]
    files += sorted(glob.glob(os.path.join(repo, "tools/lang/pending/*.json")))
    for file in files:
        if not os.path.isfile(file):
            continue
        try:
            with open(file, encoding="utf-8") as f:
                data = json.load(f)
        except ValueError:
            rep.error(os.path.relpath(file, repo), "lang file is not valid JSON")
            continue
        if isinstance(data, dict):
            keys.update(data)
    return keys


def load_trims(repo: str) -> set:
    java = os.path.join(repo, "common/src/main/java/za/co/neroland/nerotrophies/block/Trims.java")
    if os.path.isfile(java):
        with open(java, encoding="utf-8") as f:
            found = set(re.findall(r'new Trim\("([a-z0-9_]+)"', f.read()))
        if found:
            return found | {"standard"}
    return set(DEFAULT_TRIMS)


def mc_version_of(jar: str) -> str:
    match = re.search(r"/(\d+\.\d+(?:\.\d+)?)(?:-loom[^/]*)?/[^/]+\.jar$", jar.replace(os.sep, "/"))
    return match.group(1) if match else os.path.basename(jar)


def version_key(version: str):
    return tuple(int(p) for p in re.findall(r"\d+", version))


class Vanilla:
    """Item and advancement ids per Minecraft jar found offline."""

    def __init__(self, jars: list) -> None:
        self.lines = {}
        for jar in jars:
            try:
                with zipfile.ZipFile(jar) as z:
                    names = z.namelist()
            except (OSError, zipfile.BadZipFile):
                continue
            items = {n[len("assets/minecraft/items/"):-5] for n in names
                     if n.startswith("assets/minecraft/items/") and n.endswith(".json")}
            if not items:
                items = {n[len("assets/minecraft/models/item/"):-5] for n in names
                         if n.startswith("assets/minecraft/models/item/") and n.endswith(".json")}
            advancements = set()
            for prefix in ("data/minecraft/advancement/", "data/minecraft/advancements/"):
                advancements |= {n[len(prefix):-5] for n in names if n.startswith(prefix) and n.endswith(".json")}
            if items:
                self.lines[mc_version_of(jar)] = (items, advancements)

    def present(self) -> bool:
        return bool(self.lines)

    def missing_lines(self, path: str, kind: int) -> list:
        return sorted((v for v, sets in self.lines.items() if path not in sets[kind]), key=version_key)

    def newest(self) -> str:
        return max(self.lines, key=version_key)


def find_vanilla_jars(repo: str) -> list:
    patterns = [os.path.join(repo, ".gradle/loom-cache/minecraftMaven/net/minecraft/**/minecraft-merged-*.jar")]
    found = []
    for pattern in patterns:
        found += [j for j in glob.glob(pattern, recursive=True) if not j.endswith("-sources.jar")]
    return sorted(set(found))


class Sibling:
    """A sibling mod's source checkout: its shared resources plus ``resources-<mc>`` overlays."""

    def __init__(self, mod: str, repo_dir: str) -> None:
        self.mod = mod
        main = os.path.join(repo_dir, "common/src/main")
        self.base = os.path.join(main, "resources")
        self.overlays = sorted((d for d in glob.glob(os.path.join(main, "resources-*")) if os.path.isdir(d)),
                               key=lambda d: version_key(d.rsplit("resources-", 1)[1]))
        self.lang = set()
        for root in [self.base] + self.overlays:
            file = os.path.join(root, "assets", mod, "lang/en_us.json")
            if os.path.isfile(file):
                try:
                    with open(file, encoding="utf-8") as f:
                        self.lang.update(json.load(f))
                except ValueError:
                    pass

    def roots(self):
        yield None, self.base
        for overlay in self.overlays:
            yield overlay.rsplit("resources-", 1)[1], overlay

    def where(self, rel_paths: list):
        """None when absent everywhere, '' when in the shared resources, else the lowest overlay version."""
        for version, root in self.roots():
            if any(os.path.isfile(os.path.join(root, rel)) for rel in rel_paths):
                return version or ""
        return None

    def has_item(self, path: str) -> bool:
        rels = [f"assets/{self.mod}/items/{path}.json", f"assets/{self.mod}/models/item/{path}.json"]
        return (self.where(rels) is not None or f"item.{self.mod}.{path}" in self.lang
                or f"block.{self.mod}.{path}" in self.lang)

    def has_block(self, path: str) -> bool:
        rels = [f"assets/{self.mod}/blockstates/{path}.json"]
        return self.where(rels) is not None or f"block.{self.mod}.{path}" in self.lang

    def data_json(self, directory: str, path: str):
        for _, root in self.roots():
            file = os.path.join(root, "data", self.mod, directory, path + ".json")
            if os.path.isfile(file):
                try:
                    with open(file, encoding="utf-8") as f:
                        return json.load(f)
                except ValueError:
                    return {}
        return None


class Siblings:
    def __init__(self, parent: str | None, own_mod: str, own_repo: str) -> None:
        self.parent = parent
        self.cache = {}
        self.own_mod = own_mod
        self.own_repo = own_repo

    def get(self, mod: str):
        if mod == self.own_mod:
            if mod not in self.cache:
                self.cache[mod] = Sibling(mod, self.own_repo)
            return self.cache[mod]
        if self.parent is None:
            return None
        if mod not in self.cache:
            repo_dir = os.path.join(self.parent, REPO_FOR_MOD.get(mod, mod))
            self.cache[mod] = Sibling(mod, repo_dir) if os.path.isdir(os.path.join(repo_dir, "common")) else None
        return self.cache[mod]


# ---- Checks --------------------------------------------------------------------------------------

def check_icon(rep, where, icon, vanilla, siblings, what="icon"):
    if icon is None:
        return
    ns, path = icon.split(":", 1)
    if ns == "minecraft":
        if not vanilla.present():
            rep.note("vanilla item checks skipped: no Minecraft jar found (pass --vanilla-jar)")
            return
        missing = vanilla.missing_lines(path, 0)
        if vanilla.newest() in missing:
            rep.error(where, f"{what} {icon} is not a vanilla item on {vanilla.newest()}")
        elif missing:
            rep.warn(where, f"{what} {icon} does not exist on MC {', '.join(missing)} (skipped there)")
        return
    sib = siblings.get(ns)
    if sib is None:
        rep.note(f"{ns} not checked out next to this repository: its icons and trigger ids were not checked")
        return
    if not sib.has_item(path):
        rep.error(where, f"{what} {icon} is not an item in ../{REPO_FOR_MOD.get(ns, ns)}")


def check_trigger_target(rep, where, trig, vanilla, siblings):
    target = trig.get("target")
    if target is None:
        return
    ns, path = target.split(":", 1)
    kind = trig["type"]
    if kind == "nero_event" and trig.get("event") == "gate" and path.startswith("hook/"):
        if target not in HOOK_GATE_ALLOW:
            rep.error(where, f"gate {target} is a NeroQuests reward hook; only allow-listed hooks may be trophies")
        return
    if ns == "minecraft":
        if kind == "advancement" and vanilla.present():
            missing = vanilla.missing_lines(path, 1)
            if vanilla.newest() in missing:
                rep.error(where, f"advancement {target} does not exist on {vanilla.newest()}")
            elif missing:
                rep.warn(where, f"advancement {target} does not exist on MC {', '.join(missing)}")
        return
    sib = siblings.get(ns)
    if sib is None:
        rep.note(f"{ns} not checked out next to this repository: its icons and trigger ids were not checked")
        return
    if kind == "advancement":
        found = sib.where([f"data/{ns}/advancement/{path}.json", f"data/{ns}/advancements/{path}.json"])
        if found is None:
            rep.error(where, f"advancement {target} not found in ../{REPO_FOR_MOD.get(ns, ns)}")
        elif found:
            rep.warn(where, f"advancement {target} only exists on MC >= {found}; locked forever on older lines")
    elif kind == "nero_event" and trig["event"] == "gate":
        gate = sib.data_json("neroland_gates", path)
        if gate is None:
            rep.error(where, f"gate {target} not found in ../{REPO_FOR_MOD.get(ns, ns)}")
        elif str(gate.get("scope", "server")).lower() != "player":
            rep.error(where, f"gate {target} is not PLAYER scope: it never awards a player")
    elif kind == "nero_event" and trig["event"] == "material_milestone":
        milestone = sib.data_json("neroland_material_milestones", path)
        if milestone is None:
            rep.warn(where, f"material milestone {target} has no data file (built in?)")
        elif str(milestone.get("scope", "player")).lower() != "player":
            rep.error(where, f"material milestone {target} is not PLAYER scope")
    elif kind == "item_obtained":
        if not sib.has_item(path):
            rep.error(where, f"item {target} not found in ../{REPO_FOR_MOD.get(ns, ns)}")
    elif kind == "block_placed":
        if not sib.has_block(path):
            rep.error(where, f"block {target} not found in ../{REPO_FOR_MOD.get(ns, ns)}")
    elif kind == "dimension_visited":
        if sib.data_json("dimension", path) is None:
            rep.warn(where, f"dimension {target} has no data file in ../{REPO_FOR_MOD.get(ns, ns)}")


def fingerprint(trig: dict):
    return tuple(sorted((k, str(v)) for k, v in trig.items() if v is not None))


def check_catalogue(rep, trophies, collections, lang, trims, vanilla, siblings):
    completion = {}
    for cid, c in collections.items():
        if c["trophy"]:
            completion.setdefault(c["trophy"], []).append(cid)

    seen_triggers = {}
    nero_by_category = {}
    for tid, t in sorted(trophies.items()):
        for key in (t["title"], t["description"]):
            if key not in lang:
                rep.error(tid, f"lang key {key} is missing (en_us.json or tools/lang/pending)")
        check_icon(rep, tid, t["icon"], vanilla, siblings)
        trig = t["trigger"]
        if trig is not None:
            check_trigger_target(rep, tid, trig, vanilla, siblings)
            fp = fingerprint(trig)
            if fp in seen_triggers:
                rep.error(tid, f"same trigger as {seen_triggers[fp]}")
            else:
                seen_triggers[fp] = tid
            count = trig.get("count")
            if trig["type"] in ("item_obtained", "block_placed") and isinstance(count, int) and count > MAX_ITEM_COUNT:
                rep.error(tid, f"count {count} is repeated-use grind (max {MAX_ITEM_COUNT})")
            if trig["type"] == "stat":
                if trig.get("stat_type") == "minecraft:custom" and trig.get("target") in IDLE_STATS:
                    rep.error(tid, f"stat {trig['target']} rewards idle time")
                at_least = trig.get("at_least")
                if trig.get("stat_type") in REPEAT_STAT_TYPES and isinstance(at_least, int) and at_least > MAX_REPEAT_STAT:
                    rep.error(tid, f"at_least {at_least} is repeated-use grind (max {MAX_REPEAT_STAT})")
        elif tid not in completion:
            rep.warn(tid, "no trigger and no collection awards it: needs a TrophyApi.award call site")
        if t["tier"] == "nero":
            if trig is not None and trig["type"] in NO_NERO_TRIGGERS:
                rep.error(tid, f"nero tier on a polled '{trig['type']}' trigger; nero needs a real hardest milestone")
            if trig is None and tid not in completion:
                rep.error(tid, "nero tier with no trigger and no collection")
            other = nero_by_category.get(t["category"])
            if other:
                rep.error(tid, f"second nero trophy in category '{t['category']}' (first: {other})")
            else:
                nero_by_category[t["category"]] = tid

    for tid, cids in completion.items():
        if len(cids) > 1:
            rep.warn(tid, f"awarded by several collections: {', '.join(sorted(cids))}")
        if tid in trophies and trophies[tid]["has_trigger"]:
            rep.error(tid, "a collection's completion trophy must have no trigger (earned only via the collection)")

    for cid, c in sorted(collections.items()):
        where = f"collection {cid}"
        if cid in trophies:
            rep.error(where, "id is also a trophy id")
        for key in (c["title"], c["description"]):
            if key not in lang:
                rep.error(where, f"lang key {key} is missing (en_us.json or tools/lang/pending)")
        check_icon(rep, where, c["icon"], vanilla, siblings)
        if c["trim"] is not None and c["trim"] not in trims:
            rep.error(where, f"unknown trim '{c['trim']}' (known: {', '.join(sorted(trims))})")
        members = c["members"]
        if len(members) != len(set(members)):
            rep.error(where, "duplicate members")
        if len(members) > MAX_MEMBERS_CORE:
            rep.error(where, f"more than {MAX_MEMBERS_CORE} members (Core skips it)")
        if len(members) > MAX_MEMBERS_CATALOGUE:
            rep.error(where, f"{len(members)} members; this catalogue's limit is {MAX_MEMBERS_CATALOGUE}")
        if len(members) < 2:
            rep.error(where, "fewer than 2 members (Core skips it)")
        known = [m for m in members if m in trophies]
        for m in members:
            if m not in trophies:
                rep.error(where, f"member {m} is not a trophy")
            elif trophies[m]["hidden"]:
                rep.error(where, f"member {m} is hidden; hidden trophies are never collection members")
        if c["trophy"]:
            if c["trophy"] not in trophies:
                rep.error(where, f"completion trophy {c['trophy']} is not a trophy")
            elif c["trophy"] in members:
                rep.error(where, "its completion trophy is also one of its members")
        if known:
            shared = set.intersection(*(set(trophies[m]["requires"]) for m in known))
            for mod in c["requires"]:
                if mod not in shared:
                    rep.error(where, f"requires_mod '{mod}' is not required by every member")
            if not c["requires"] and shared:
                rep.warn(where, f"every member requires {', '.join(sorted(shared))}; say so in requires_mod")
            if c["trophy"] in trophies:
                loadable = shared | set(c["requires"])
                for mod in trophies[c["trophy"]]["requires"]:
                    if mod not in loadable:
                        rep.error(where, f"completion trophy needs '{mod}', which the collection does not")


def check_quest_pack(rep, roots, lang):
    """The quest pack's keys and links (NeroQuests validates the codecs itself)."""
    quests, chapters = {}, {}
    for root in roots:
        for ns_dir in sorted(glob.glob(os.path.join(root, "data", "*", "neroquests"))):
            ns = os.path.basename(os.path.dirname(ns_dir))
            for kind, out in (("quests", quests), ("chapters", chapters)):
                for file in sorted(glob.glob(os.path.join(ns_dir, kind, "**", "*.json"), recursive=True)):
                    rel = os.path.relpath(file, os.path.join(ns_dir, kind)).replace(os.sep, "/")[:-5]
                    data = read_json(rep, file, f"{ns}:{rel}")
                    if data is not None:
                        out[f"{ns}:{rel}"] = data
    for qid, q in quests.items():
        for key in (q.get("title"), q.get("description")):
            if key and key not in lang:
                rep.error(f"quest {qid}", f"lang key {key} is missing")
        for pre in q.get("prerequisites", []):
            if pre not in quests and not pre.startswith("neroquests:"):
                rep.error(f"quest {qid}", f"unknown prerequisite {pre}")
    for cid, c in chapters.items():
        if c.get("title") not in lang:
            rep.error(f"chapter {cid}", f"lang key {c.get('title')} is missing")
        for entry in c.get("quests", []):
            if entry.get("quest") not in quests:
                rep.error(f"chapter {cid}", f"entry {entry.get('quest')} is not a quest in this pack")
    return len(quests), len(chapters)


# ---- Main ----------------------------------------------------------------------------------------

def resource_roots(repo: str) -> list:
    main = os.path.join(repo, "common/src/main")
    roots = [os.path.join(main, "resources")]
    roots += sorted(d for d in glob.glob(os.path.join(main, "resources-*")) if os.path.isdir(d))
    return [r for r in roots if os.path.isdir(r)]


def validate(repo: str, siblings_dir: str | None, jars: list) -> tuple:
    rep = Report()
    roots = resource_roots(repo)
    trophies = load_definitions(rep, roots)
    collections = load_collections(rep, roots)
    lang = load_lang(rep, repo)
    vanilla = Vanilla(jars)
    sibs = Siblings(siblings_dir, "nerotrophies", repo)
    check_catalogue(rep, trophies, collections, lang, load_trims(repo), vanilla, sibs)
    quest_counts = check_quest_pack(rep, roots, lang)
    return rep, trophies, collections, quest_counts, vanilla


def main(argv=None) -> int:
    here = os.path.dirname(os.path.abspath(__file__))
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--repo", default=os.path.dirname(here), help="repository root (default: this one)")
    parser.add_argument("--siblings", help="folder holding sibling checkouts (default: the repo's parent)")
    parser.add_argument("--no-siblings", action="store_true", help="skip every sibling check")
    parser.add_argument("--vanilla-jar", action="append", default=[], help="a Minecraft jar (repeatable)")
    parser.add_argument("--quiet", action="store_true", help="print errors and the summary only")
    args = parser.parse_args(argv)
    repo = os.path.abspath(args.repo)
    siblings_dir = None if args.no_siblings else os.path.abspath(args.siblings or os.path.dirname(repo))
    jars = args.vanilla_jar or find_vanilla_jars(repo)
    rep, trophies, collections, (n_quests, n_chapters), vanilla = validate(repo, siblings_dir, jars)

    tiers = {t: sum(1 for d in trophies.values() if d["tier"] == t) for t in TIERS}
    hidden = sum(1 for d in trophies.values() if d["hidden"])
    print(f"Trophies: {len(trophies)} ({', '.join(f'{k} {v}' for k, v in tiers.items())}; hidden {hidden})")
    print(f"Collections: {len(collections)}; quest pack: {n_chapters} chapter(s), {n_quests} quest(s)")
    print(f"Vanilla jars: {', '.join(sorted(vanilla.lines, key=version_key)) or 'none'}")
    for msg in rep.errors:
        print(f"ERROR   {msg}")
    if not args.quiet:
        for msg in rep.warnings:
            print(f"WARNING {msg}")
        for msg in rep.notes:
            print(f"NOTE    {msg}")
    print(f"{len(rep.errors)} error(s), {len(rep.warnings)} warning(s)")
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())
