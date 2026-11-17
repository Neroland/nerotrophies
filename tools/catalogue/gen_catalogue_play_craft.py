#!/usr/bin/env python3
"""Generates the play-craft catalogue group: NeroDecor, NeroNotes, NeroArcade and NeroMaps trophies.

Writes, relative to the repo root:
  common/src/main/resources/data/nerotrophies/neroland_trophies/<mod>/<slug>.json
  tools/lang/pending/catalogue-play-craft.json
  tools/catalogue/citations/play-craft.csv

Every trigger id and icon was checked against the sibling's source. NeroLink ships nothing: the
bridge registers no items, blocks, gates or stats. Run from anywhere: python gen_catalogue_play_craft.py
"""
import csv
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
LANG = ROOT / "tools/lang/pending/catalogue-play-craft.json"
CSV = ROOT / "tools/catalogue/citations/play-craft.csv"


def block(b):
    return {"type": "block_placed", "block": b}


def item(i):
    return {"type": "item_obtained", "item": i}


def gate(g):
    return {"type": "nero_event", "event": "gate", "gate": g}


def dim(d):
    return {"type": "dimension_visited", "dimension": d}


def stat(stat_type, s, n):
    return {"type": "stat", "stat_type": stat_type, "stat": s, "at_least": n}


# (mod, slug, tier, hidden, icon, trigger, survey row, title, description, notes)
TROPHIES = [
    # NeroDecor
    ("nerodecor", "interior_designer", "bronze", False, "nerodecor:chroma_hub",
     block("nerodecor:chroma_hub"), "DECO-BLOCK-01",
     "Interior Designer", "Place a Chroma Hub.", ""),
    ("nerodecor", "hologram_hobbyist", "bronze", False, "nerodecor:holo_projector",
     block("nerodecor:holo_projector"), "DECO-BLOCK-02",
     "Hologram Hobbyist", "Place a Holo Projector.", ""),
    ("nerodecor", "blast_proof", "bronze", False, "nerodecor:blast_door",
     block("nerodecor:blast_door"), "DECO-BLOCK-03",
     "Blast Proof", "Install a Blast Door.", ""),
    ("nerodecor", "command_deck", "bronze", False, "nerodecor:command_console",
     block("nerodecor:command_console"), "DECO-BLOCK-03",
     "Command Deck", "Place a Command Console.", ""),
    ("nerodecor", "void_architect", "silver", True, "nerodecor:hull_void_crystal",
     block("nerodecor:hull_void_crystal"), "DECO-BLOCK-04",
     "Void Architect", "Build with Void Crystal hull plating.", ""),
    ("nerodecor", "painter", "silver", False, "nerodecor:paint_gun",
     stat("minecraft:used", "nerodecor:paint_gun", 100), "DECO-STAT-01",
     "Painter", "Use the Paint Gun on paintable blocks 100 times.",
     "PaintGunItem.useOn returns SUCCESS on a paintable block, so vanilla ItemStack.useOn counts minecraft:used"),
    # NeroNotes
    ("neronotes", "pressed_vinyl", "bronze", False, "neronotes:custom_disk",
     item("neronotes:custom_disk"), "NOTE-ITEM-01",
     "Pressed Vinyl", "Press a composition onto a Resonant Disk.", ""),
    ("neronotes", "composer", "silver", False, "neronotes:publish_lectern",
     gate("neronotes:first_publish"), "NOTE-GATE-01",
     "Composer", "Publish a composition to the shared library.", "player-scope gate"),
    ("neronotes", "into_the_soundforge", "silver", False, "neronotes:resonant_block_deep_bass",
     dim("neronotes:soundforge"), "NOTE-DIM-01",
     "Into the Soundforge", "Step through a Harmonic Gate into the Soundforge.",
     "not retroactive for past visits"),
    ("neronotes", "gatekeeper", "bronze", False, "neronotes:harmonic_gate",
     block("neronotes:harmonic_gate"), "NOTE-BLOCK-01",
     "Gatekeeper", "Place a Harmonic Gate.", ""),
    ("neronotes", "swap_meet", "bronze", False, "neronotes:disk_exchanger",
     block("neronotes:disk_exchanger"), "NOTE-BLOCK-01",
     "Swap Meet", "Place a Disk Exchanger.", "craftable; replaces a pedestal trophy with no survival source"),
    # NeroArcade
    ("neroarcade", "insert_coin", "bronze", False, "neroarcade:arcade_cabinet",
     block("neroarcade:arcade_cabinet"), "ARCA-BLOCK-01",
     "Insert Coin", "Set up an Arcade Cabinet.", ""),
    ("neroarcade", "game_designer", "bronze", False, "neroarcade:cartridge",
     gate("neroarcade:first_cartridge"), "ARCA-GATE-01",
     "Game Designer", "Build your first cartridge at the Game Bench.", "player-scope gate"),
    ("neroarcade", "published", "silver", False, "neroarcade:arcade_exchange",
     gate("neroarcade:first_published"), "ARCA-GATE-02",
     "Published", "Publish a cartridge on the Arcade Exchange.", "player-scope gate"),
    ("neroarcade", "versus_victor", "silver", False, "neroarcade:link_bus",
     gate("neroarcade:first_versus_win"), "ARCA-GATE-03",
     "Versus Victor", "Win a versus match against another player.", "player-scope gate"),
    ("neroarcade", "up_in_lights", "bronze", False, "neroarcade:marquee_screen",
     block("neroarcade:marquee_screen"), "ARCA-BLOCK-01",
     "Up in Lights", "Place a Marquee Screen.", ""),
    # NeroMaps (bonus)
    ("neromaps", "cartographer", "bronze", False, "neromaps:cartography_desk",
     block("neromaps:cartography_desk"), "MAPS-BLOCK-01",
     "Cartographer", "Place a Cartography Desk.", ""),
    ("neromaps", "bound_atlas", "bronze", False, "neromaps:atlas",
     item("neromaps:atlas"), "MAPS-ITEM-01",
     "Bound Atlas", "Record your first map in an Atlas.", ""),
    ("neromaps", "signposted", "bronze", False, "neromaps:signpost",
     block("neromaps:signpost"), "MAPS-BLOCK-01",
     "Signposted", "Place a Signpost.", ""),
    ("neromaps", "war_room", "silver", False, "neromaps:map_table",
     block("neromaps:map_table"), "MAPS-BLOCK-01",
     "War Room", "Place a Map Table.", ""),
]


def main():
    lang = {}
    rows = []
    order = {}
    for mod, slug, tier, hidden, icon, trigger, row, title, desc, notes in TROPHIES:
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
            "requires_mod": mod,
            "order": order[mod],
        }
        out = DATA / mod / f"{slug}.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8", newline="\n")
        lang[f"{key}.title"] = title
        lang[f"{key}.description"] = desc
        target = trigger.get("block") or trigger.get("item") or trigger.get("gate") \
            or trigger.get("dimension") or f"{trigger.get('stat_type')}:{trigger.get('stat')}"
        rows.append([f"nerotrophies:{mod}/{slug}", tier, str(hidden).lower(), trigger["type"], row,
                     "; ".join(x for x in (target, notes) if x)])
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8", newline="\n")
    CSV.parent.mkdir(parents=True, exist_ok=True)
    with CSV.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "tier", "hidden", "trigger_type", "survey_row", "notes"])
        w.writerows(rows)
    print(f"{len(TROPHIES)} trophies written")


if __name__ == "__main__":
    main()
