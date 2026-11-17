#!/usr/bin/env python3
"""Generates the wiki's trophy catalogue pages from the shipped trophy JSON and the English lang file.

Writes ``wiki/Catalogue-<Group>.md`` (one page per mod group) and the generated block of
``wiki/Collections.md`` (between the ``BEGIN GENERATED`` / ``END GENERATED`` markers), so the wiki
never drifts from the data. Hidden trophies are listed as "???" (hidden until earned) with no tier,
icon or description, exactly as the Trophy Book shows them before they are earned.

Usage::

    python3 tools/wiki/gen_wiki_catalogue.py           # rewrite the pages
    python3 tools/wiki/gen_wiki_catalogue.py --check   # exit 1 if a page is out of date

Reads data files only; no network, no personal data.
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "common", "src", "main", "resources")
DATA = os.path.join(RES, "data")
LANG = os.path.join(RES, "assets", "nerotrophies", "lang", "en_us.json")
PENDING = os.path.join(ROOT, "tools", "lang", "pending")
WIKI = os.path.join(ROOT, "wiki")

BEGIN = "<!-- BEGIN GENERATED: tools/wiki/gen_wiki_catalogue.py -->"
END = "<!-- END GENERATED -->"

TIERS = ["bronze", "silver", "gold", "nero"]
TIER_NAMES = {"bronze": "Bronze", "silver": "Silver", "gold": "Gold", "nero": "Nero"}

# Page slug, page title, intro, Trophy Book categories in page order.
GROUPS = [
    ("Catalogue-Core-and-Vanilla", "Catalogue: Core and Vanilla",
     "Trophies that need nothing but Neroland Core and NeroTrophies: vanilla milestones, Core milestones and "
     "NeroTrophies' own. A Core-only server can earn the vanilla and NeroTrophies ones.",
     ["minecraft", "nerolandcore", "nerotrophies"]),
    ("Catalogue-Space-and-Tech", "Catalogue: Space and Tech",
     "Trophies for Nerospace, Nerotech, NeroPower and NeroLogistics. Each one appears only when its mod is installed.",
     ["nerospace", "nerotech", "neropower", "nerologistics"]),
    ("Catalogue-Society", "Catalogue: Society",
     "Trophies for NeroEconomy, NeroFactions, NeroColonies, NeroSecurity, NeroEvents and NeroQuests. Each one appears "
     "only when its mod is installed.",
     ["neroeconomy", "nerofactions", "nerocolonies", "nerosecurity", "neroevents", "neroquests"]),
    ("Catalogue-Life-and-World", "Catalogue: Life and World",
     "Trophies for NeroCreatures, NeroAgriculture, NeroCompanion and NeroRuins. Each one appears only when its mod is "
     "installed.",
     ["nerocreatures", "neroagriculture", "nerocompanion", "neroruins"]),
    ("Catalogue-Play-and-Craft", "Catalogue: Play and Craft",
     "Trophies for NeroDecor, NeroNotes, NeroArcade, NeroMaps and NeroLink. Each one appears only when its mod is "
     "installed.",
     ["nerodecor", "neronotes", "neroarcade", "neromaps", "nerolink"]),
]

# Player-facing notes per trophy id (kept in step with the lang file's descriptions).
NOTES = {
    "nerotrophies:neroquests/arcade_apprentice":
        "Not awarded retroactively: the quest's gate closes after its reward, so it only counts if NeroTrophies "
        "was installed when you finished it.",
    "nerotrophies:neronotes/into_the_soundforge":
        "Not awarded retroactively: only a visit made while NeroTrophies is installed counts.",
    "nerotrophies:neromaps/bound_atlas":
        "Have the item in your inventory (an Atlas comes from using a Blank Atlas after exploring).",
    "nerotrophies:neronotes/pressed_vinyl":
        "Have the item in your inventory (a Resonant Disk comes from the Disk Press).",
    "nerotrophies:neroevents/stargazer":
        "Mine meteor ore: the ore drops dust, not itself, so mining it is what counts.",
}
BOSS_NOTE = "Only the player who lands the killing blow earns it."
BOSS_IDS = {"nerotrophies:nerocreatures/" + b for b in
            ("cinder_tyrant", "rime_matriarch", "bloom_sovereign", "facet_colossus")}

GROUP_FOOTNOTES = {
    "Catalogue-Life-and-World": [
        "The four NeroCreatures boss trophies credit only the player who lands the killing blow. That is by design, "
        "not a bug.",
    ],
    "Catalogue-Play-and-Craft": [
        "Catalogue scope: NeroLink has no trophies yet, NeroMaps 4, NeroArcade 5 and NeroNotes 5. More arrive when "
        "those mods expose the events.",
    ],
    "Catalogue-Society": [
        "Catalogue scope: NeroQuests has 3 trophies for now. More arrive when it exposes the events.",
    ],
}


def load_lang() -> dict:
    lang = {}
    for path in sorted(glob.glob(os.path.join(PENDING, "*.json"))):
        with open(path, encoding="utf-8") as f:
            lang.update(json.load(f))
    with open(LANG, encoding="utf-8") as f:
        lang.update(json.load(f))
    return lang


def load(kind: str) -> dict:
    out = {}
    for ns_dir in sorted(glob.glob(os.path.join(DATA, "*"))):
        ns = os.path.basename(ns_dir)
        base = os.path.join(ns_dir, kind)
        for path in sorted(glob.glob(os.path.join(base, "**", "*.json"), recursive=True)):
            rel = os.path.relpath(path, base)[:-5].replace(os.sep, "/")
            with open(path, encoding="utf-8") as f:
                out[f"{ns}:{rel}"] = json.load(f)
    return out


def default_key(prefix: str, ident: str, suffix: str) -> str:
    ns, path = ident.split(":", 1)
    return f"{prefix}.{ns}.{path.replace('/', '.')}.{suffix}"


def requires(d: dict) -> list:
    r = d.get("requires_mod")
    if r is None:
        return []
    return [r] if isinstance(r, str) else list(r)


def category(ident: str, d: dict) -> str:
    if d.get("category"):
        return d["category"]
    req = requires(d)
    return req[0] if req else ident.split(":", 1)[0]


def how(d: dict, collections_by_trophy: dict, ident: str) -> str:
    t = d.get("trigger")
    if not t:
        coll = collections_by_trophy.get(ident)
        return "Complete a collection" if coll else "Awarded by a mod"
    kind = t.get("type")
    if kind == "advancement":
        return "Advancement"
    if kind == "stat":
        return "Statistic"
    if kind == "item_obtained":
        return "Have the item"
    if kind == "block_placed":
        return "Place the block"
    if kind == "dimension_visited":
        return "Visit"
    if kind == "nero_event":
        return {"gate": "Milestone", "material_milestone": "Material milestone",
                "reputation": "Reputation", "currency": "Credits"}.get(t.get("event"), "Event")
    return "Event"


def cell(text: str) -> str:
    return text.replace("|", "\\|").replace("\n", " ").strip()


def mod_name(lang: dict, cat: str) -> str:
    return lang.get(f"nerotrophies.category.{cat}", cat)


def trophy_rows(lang, trophies, collections_by_trophy, cat):
    items = [(i, d) for i, d in trophies.items() if category(i, d) == cat]
    shown = sorted((x for x in items if not x[1].get("hidden")),
                   key=lambda x: (TIERS.index(x[1]["tier"]), x[1].get("order", 0), x[0]))
    hidden = [x for x in items if x[1].get("hidden")]
    rows = []
    for ident, d in shown:
        title = lang.get(d.get("title") or default_key("trophy", ident, "title"), ident)
        desc = lang.get(d.get("description") or default_key("trophy", ident, "desc"), "")
        notes = []
        req = requires(d)
        if len(req) > 1:
            notes.append("Needs " + " and ".join(mod_name(lang, r) for r in req) + ".")
        if ident in BOSS_IDS:
            notes.append(BOSS_NOTE)
        if ident in NOTES:
            notes.append(NOTES[ident])
        coll = collections_by_trophy.get(ident)
        if coll:
            notes.append(f"Awarded for completing the {coll} collection.")
        rows.append(f"| {cell(title)} | {TIER_NAMES[d['tier']]} | {cell(desc)} | "
                    f"{how(d, collections_by_trophy, ident)} | {cell(' '.join(notes))} |")
    for _ in hidden:
        rows.append("| ??? | ??? | Hidden until earned. | ??? | |")
    return items, rows


def render_group(lang, trophies, collections_by_trophy, slug, title, intro, cats) -> str:
    out = [f"# {title}", "", intro, ""]
    out += ["Tables are generated from the trophy files the mod ships, so they always match the game. Hidden "
            "trophies are listed as **???**: they stay hidden until earned, in the Trophy Book and here. "
            "\"How\" says what kind of event earns the trophy; see [Tiers](Tiers) for what the tiers mean.", ""]
    for cat in cats:
        items, rows = trophy_rows(lang, trophies, collections_by_trophy, cat)
        name = mod_name(lang, cat)
        out.append(f"## {name}")
        out.append("")
        if not items:
            out.append(f"No {name} trophies yet. They arrive when {name} exposes the events they need.")
            out.append("")
            continue
        counts = {t: sum(1 for _, d in items if d["tier"] == t) for t in TIERS}
        hidden = sum(1 for _, d in items if d.get("hidden"))
        line = f"{len(items)} trophies: " + ", ".join(f"{counts[t]} {TIER_NAMES[t].lower()}" for t in TIERS)
        if hidden:
            line += f" ({hidden} hidden)"
        out += [line + ".", "", "| Trophy | Tier | How to earn it | How | Notes |",
                "|--------|------|----------------|-----|-------|"]
        out += rows
        out.append("")
    notes = GROUP_FOOTNOTES.get(slug)
    if notes:
        out += ["## Notes", ""] + [f"- {n}" for n in notes] + [""]
    out += ["Trophies are never awarded for work that NeroTrophies cannot see, and they never gate gameplay. "
            "Most trophies are also awarded **retroactively**: when you log in, or after a reload, work you already "
            "did counts (advancements, statistics, milestones and items you hold). Trophies marked \"Not awarded "
            "retroactively\" are the exceptions.", ""]
    return "\n".join(out)


def render_collections(lang, trophies, collections) -> str:
    out = [BEGIN, "", "| Collection | Members | Cabinet trim | Completion trophy | Needs |",
           "|------------|---------|--------------|-------------------|-------|"]
    for ident in sorted(collections):
        c = collections[ident]
        title = lang.get(c.get("title") or default_key("collection", ident, "title"), ident)
        desc = lang.get(c.get("description") or default_key("collection", ident, "desc"), "")
        members = []
        for m in c.get("members", []):
            md = trophies.get(m)
            if md is None:
                continue
            members.append(lang.get(md.get("title") or default_key("trophy", m, "title"), m))
        trim = lang.get(f"trim.nerotrophies.{c.get('trim', 'standard')}", c.get("trim", ""))
        comp = c.get("trophy")
        comp_title = ""
        if comp and comp in trophies:
            comp_title = lang.get(trophies[comp].get("title") or default_key("trophy", comp, "title"), comp)
        need = sorted({r for m in c.get("members", []) if m in trophies for r in requires(trophies[m])}
                      | set(requires(c)))
        need_txt = ", ".join(mod_name(lang, r) for r in need) or "Nothing extra"
        head = f"**{cell(title)}**" + (f": {cell(desc)}" if desc else "")
        out.append(f"| {head} | {cell(', '.join(members))} | {cell(trim)} | {cell(comp_title) or '-'} | "
                   f"{cell(need_txt)} |")
    out += ["", END]
    return "\n".join(out)


def build() -> dict:
    lang = load_lang()
    trophies = load("neroland_trophies")
    collections = load("neroland_trophy_collections")
    collections_by_trophy = {}
    for ident, c in collections.items():
        if c.get("trophy"):
            collections_by_trophy[c["trophy"]] = lang.get(
                c.get("title") or default_key("collection", ident, "title"), ident)
    pages = {}
    for slug, title, intro, cats in GROUPS:
        pages[os.path.join(WIKI, slug + ".md")] = render_group(
            lang, trophies, collections_by_trophy, slug, title, intro, cats)
    coll_path = os.path.join(WIKI, "Collections.md")
    if os.path.exists(coll_path):
        with open(coll_path, encoding="utf-8") as f:
            text = f.read()
        if BEGIN in text and END in text:
            pre = text.split(BEGIN, 1)[0]
            post = text.split(END, 1)[1]
            pages[coll_path] = pre + render_collections(lang, trophies, collections) + post
    known = {c for _, _, _, cats in GROUPS for c in cats}
    stray = sorted({category(i, d) for i, d in trophies.items()} - known)
    if stray:
        raise SystemExit(f"categories not on any catalogue page: {', '.join(stray)}")
    return pages


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="exit 1 if a page is out of date")
    args = ap.parse_args()
    stale = []
    for path, text in build().items():
        old = None
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as f:
                old = f.read().replace("\r\n", "\n")
        if old != text:
            stale.append(os.path.relpath(path, ROOT))
            if not args.check:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(text)
    if args.check:
        if stale:
            print("Out of date (run tools/wiki/gen_wiki_catalogue.py): " + ", ".join(stale))
            return 1
        print("Wiki catalogue is up to date.")
        return 0
    print(f"Wrote {len(stale)} page(s)." if stale else "Nothing to change.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
