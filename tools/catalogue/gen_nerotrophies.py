#!/usr/bin/env python3
"""Generates NeroTrophies' own trophies: bronzes for placing the mod's own display blocks.

Writes, relative to the repo root:
  common/src/main/resources/data/nerotrophies/neroland_trophies/nerotrophies/<slug>.json
  tools/lang/pending/catalogue-nerotrophies.json
  tools/catalogue/citations/nerotrophies.csv

No requires_mod: these load wherever NeroTrophies does, so a Core-only server still has something to earn.
Block ids come from registry/TrophyIds.java; both blocks have survival recipes in this repo.
"""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
LANG = ROOT / "tools/lang/pending/catalogue-nerotrophies.json"
CITES = ROOT / "tools/catalogue/citations/nerotrophies.csv"


def placed(bid):
    return {"type": "block_placed", "block": bid}


# mod, slug, tier, hidden, icon, trigger, title, description, row, notes
T = [
    ("nerotrophies", "curator", "bronze", False, "nerotrophies:trophy_cabinet",
     placed("nerotrophies:trophy_cabinet"), "Curator", "Place a Trophy Cabinet.", "OWN-BLOCK-01",
     "own block; craftable"),
    ("nerotrophies", "on_display", "bronze", False, "nerotrophies:display_pedestal",
     placed("nerotrophies:display_pedestal"), "On Display", "Place a Display Pedestal.", "OWN-BLOCK-01",
     "own block; craftable"),
]


def main():
    lang = {}
    rows = []
    order = {}
    for mod, slug, tier, hidden, icon, trigger, title, desc, row, notes in T:
        order[mod] = order.get(mod, 0) + 10
        key = f"trophy.nerotrophies.{mod}.{slug}"
        body = {
            "schema": 1,
            "icon": icon,
            "tier": tier,
            "trigger": trigger,
            "hidden": hidden,
            "title": f"{key}.title",
            "description": f"{key}.description",
            "category": mod,
            "order": order[mod],
        }
        out = DATA / mod / f"{slug}.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8", newline="\n")
        lang[f"{key}.title"] = title
        lang[f"{key}.description"] = desc
        rows.append([f"nerotrophies:{mod}/{slug}", tier, str(hidden).lower(), trigger["type"], row, notes])
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    CITES.parent.mkdir(parents=True, exist_ok=True)
    with CITES.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "tier", "hidden", "trigger_type", "survey_row", "notes"])
        w.writerows(rows)
    print(f"{len(T)} trophies written")


if __name__ == "__main__":
    main()
