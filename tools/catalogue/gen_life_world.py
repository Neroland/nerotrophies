#!/usr/bin/env python3
"""Writes the life-world catalogue group: NeroCreatures, NeroAgriculture, NeroCompanion, NeroRuins.

Outputs (paths relative to the repo root):
  common/src/main/resources/data/nerotrophies/neroland_trophies/<mod>/<slug>.json
  tools/lang/pending/catalogue-life-world.json
  tools/catalogue/citations/life-world.csv

Every trigger id and icon was checked against the sibling's source. Re-running is idempotent.
"""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
LANG = ROOT / "tools/lang/pending/catalogue-life-world.json"
CITES = ROOT / "tools/catalogue/citations/life-world.csv"


def gate(gid):
    return {"type": "nero_event", "event": "gate", "gate": gid}


def milestone(mid):
    return {"type": "nero_event", "event": "material_milestone", "milestone": mid}


def item(iid, count=1):
    t = {"type": "item_obtained", "item": iid}
    if count != 1:
        t["count"] = count
    return t


def killed(entity):
    return {"type": "stat", "stat_type": "minecraft:killed", "stat": entity, "at_least": 1}


# (mod, slug, tier, hidden, icon, trigger, title, description, survey_row, notes)
T = [
    # --- NeroCreatures -------------------------------------------------------------------------
    ("nerocreatures", "new_best_friend", "bronze", False, "nerocreatures:creature_saddle",
     gate("nerocreatures:first_tame"), "New Best Friend", "Tame your first creature.", "CREA-GATE-01", ""),
    ("nerocreatures", "matchmaker", "bronze", False, "nerocreatures:incubator",
     gate("nerocreatures:first_breed"), "Matchmaker", "Breed two creatures.", "CREA-GATE-03", ""),
    ("nerocreatures", "first_deploy", "bronze", False, "nerocreatures:drone_shell",
     gate("nerocreatures:first_deploy"), "Ground Crew", "Deploy a Terraforming Drone.", "CREA-GATE-02", ""),
    ("nerocreatures", "field_naturalist", "bronze", False, "nerocreatures:field_scanner",
     gate("nerocreatures:bestiary/scanned/mossback"), "Field Naturalist",
     "Scan a Mossback with the Field Scanner.", "CREA-GATE-04", ""),
    ("nerocreatures", "mimic_unmasked", "silver", True, "nerocreatures:cache_mimic_spawn_egg",
     gate("nerocreatures:bestiary/scanned/cache_mimic"), "Mimic Unmasked",
     "Scan a Cache Mimic and see it for what it is.", "CREA-GATE-04", "hidden; never a collection member"),
    ("nerocreatures", "leviathan_studied", "silver", False, "nerocreatures:leviathan_barnacle",
     gate("nerocreatures:bestiary/studied/drift_leviathan"), "Leviathan Studied",
     "Complete the Bestiary study of the Drift Leviathan.", "CREA-GATE-05", ""),
    ("nerocreatures", "cinder_tyrant", "gold", False, "nerocreatures:tyrant_ember",
     gate("nerocreatures:bestiary/studied/cinder_tyrant"), "Tyrant Slayer",
     "Scan the Cinder Tyrant, then land the killing blow.", "CREA-GATE-05",
     "re-triggered from the kill stat (credits the last hit only); Boss Hunter member"),
    ("nerocreatures", "rime_matriarch", "gold", False, "nerocreatures:matriarch_frostcore",
     gate("nerocreatures:bestiary/studied/rime_matriarch"), "Thaw the Matriarch",
     "Scan the Rime Matriarch, then land the killing blow.", "CREA-GATE-05", "Boss Hunter member"),
    ("nerocreatures", "bloom_sovereign", "gold", False, "nerocreatures:sovereign_seed",
     gate("nerocreatures:bestiary/studied/bloom_sovereign"), "Uprooted",
     "Scan the Bloom Sovereign, then land the killing blow.", "CREA-GATE-05", "Boss Hunter member"),
    ("nerocreatures", "facet_colossus", "gold", False, "nerocreatures:colossus_facet",
     gate("nerocreatures:bestiary/studied/facet_colossus"), "Shattered Facets",
     "Scan the Facet Colossus, then land the killing blow.", "CREA-GATE-05", "Boss Hunter member"),

    # --- NeroAgriculture -----------------------------------------------------------------------
    ("neroagriculture", "first_harvest", "bronze", False, "neroagriculture:resource_seed",
     gate("neroagriculture:first_harvest"), "First Harvest", "Harvest a mature crop.", "AGRI-GATE-02", ""),
    ("neroagriculture", "seed_scientist", "bronze", False, "neroagriculture:seed_research_bench",
     gate("neroagriculture:first_research"), "Seed Scientist",
     "Complete a research at the Seed Research Bench.", "AGRI-GATE-01", ""),
    ("neroagriculture", "greenhouse_keeper", "silver", False, "neroagriculture:greenhouse_controller",
     gate("neroagriculture:greenhouse_keeper"), "Greenhouse Keeper",
     "Open a formed, running Greenhouse.", "AGRI-GATE-03", ""),
    ("neroagriculture", "tower_grower", "silver", False, "neroagriculture:crop_tower_controller",
     gate("neroagriculture:tower_grower"), "Tower Grower", "Open a formed Crop Tower.", "AGRI-GATE-04", ""),
    ("neroagriculture", "xenobotanist", "silver", False, "neroagriculture:alien_seed",
     milestone("neroagriculture:alien_seed_researched"), "Xenobotanist",
     "Research an alien seed.", "AGRI-MILE-03", "player-scope milestone, any material"),
    ("neroagriculture", "transmuter", "gold", False, "neroagriculture:orbite_fragment",
     gate("neroagriculture:transmutation"), "Transmuter",
     "Produce an Orbite fragment and unlock Transmutation.", "AGRI-GATE-07", ""),
    ("neroagriculture", "void_harvest", "gold", True, "neroagriculture:voidite_fragment",
     item("neroagriculture:voidite_fragment"), "Void Harvest",
     "Get a Voidite fragment, the tier Ascension unlocks.", "AGRI-ITEM-01", "hidden; never a collection member"),
    ("neroagriculture", "ascension", "nero", False, "neroagriculture:colonite_fragment",
     gate("neroagriculture:ascension"), "Ascension",
     "Produce a Colonite fragment and unlock Ascension, the last gate of the ladder.", "AGRI-GATE-08",
     "nero confirmed: ascension is the last gate (it guards the Voidite tier); owner tracking is on by default"),

    # --- NeroCompanion -------------------------------------------------------------------------
    ("nerocompanion", "boot_sequence", "bronze", False, "nerocompanion:capsule",
     gate("nerocompanion:first_deploy"), "Boot Sequence", "Deploy your first companion.", "COMP-GATE-01", ""),
    ("nerocompanion", "chipped_in", "bronze", False, "nerocompanion:blank_chip",
     gate("nerocompanion:first_chip"), "Chipped In", "Fit an upgrade chip to a companion.", "COMP-GATE-02", ""),
    ("nerocompanion", "lost_and_found", "bronze", False, "nerocompanion:companion_bay",
     gate("nerocompanion:first_recovery"), "Lost and Found", "Reclaim a lost companion.", "COMP-GATE-12", ""),
    ("nerocompanion", "first_mine", "bronze", False, "nerocompanion:ore_filter_chip",
     gate("nerocompanion:first_mine"), "Rock and Roll", "Have a Mining Bot finish a job.", "COMP-GATE-06",
     "Working Crew member"),
    ("nerocompanion", "first_carry", "bronze", False, "nerocompanion:cargo_rack_chip",
     gate("nerocompanion:first_carry"), "Special Delivery", "Have a carrier deliver its cargo.", "COMP-GATE-07",
     "Working Crew member"),
    ("nerocompanion", "first_repair", "bronze", False, "nerocompanion:salvage_chip",
     gate("nerocompanion:first_repair"), "Good as New", "Have a Repair Bot finish a repair.", "COMP-GATE-08",
     "Working Crew member"),
    ("nerocompanion", "first_guard", "bronze", False, "nerocompanion:guard_protocol_chip",
     gate("nerocompanion:first_guard"), "On Guard", "Have a Combat Bot down a target while guarding.",
     "COMP-GATE-09", "Working Crew member"),
    ("nerocompanion", "its_alive", "silver", True, "nerocompanion:scrap_egg",
     gate("nerocompanion:first_hatch"), "It's Alive!", "Hatch a Scrap Egg into a Scrapling.", "COMP-GATE-10",
     "hidden; never a collection member"),
    ("nerocompanion", "best_buddies", "silver", False, "nerocompanion:fetch_ball",
     gate("nerocompanion:bond_three"), "Best Buddies", "Raise a companion's bond to level 3.", "COMP-GATE-11", ""),
    ("nerocompanion", "full_squad", "gold", False, "nerocompanion:remote",
     gate("nerocompanion:full_squad"), "Full Squad", "Have a full squad of companions active at once.",
     "COMP-GATE-04", ""),

    # --- NeroRuins -----------------------------------------------------------------------------
    ("neroruins", "archivist", "bronze", False, "neroruins:data_shard",
     gate("neroruins:first_log"), "Archivist", "Read a data log.", "RUIN-GATE-01", ""),
    ("neroruins", "safecracker", "bronze", False, "neroruins:vault_cache",
     gate("neroruins:first_vault"), "Safecracker", "Open a locked vault.", "RUIN-GATE-02", ""),
    ("neroruins", "puzzle_solver", "silver", False, "neroruins:glyph_lock",
     gate("neroruins:puzzle_solver"), "Puzzle Solver", "Solve a puzzle door.", "RUIN-GATE-03", ""),
    ("neroruins", "codex_earth", "bronze", False, "neroruins:surveyors_compass",
     gate("neroruins:codex/earth"), "Earth Codex", "Archive every log of the Earth chapter.", "RUIN-GATE-04",
     "Codex member"),
    ("neroruins", "codex_luna", "bronze", False, "neroruins:pale_survey_lens",
     gate("neroruins:codex/luna"), "Luna Codex", "Archive every log of the Luna chapter.", "RUIN-GATE-04",
     "Codex member"),
    ("neroruins", "codex_ark", "gold", False, "neroruins:directors_seal",
     gate("neroruins:codex/ark"), "The Ark Remembers", "Archive every log of the Ark chapter.", "RUIN-GATE-04",
     "Codex member; not hidden"),
    ("neroruins", "codex_mars", "bronze", False, "neroruins:field_recorder",
     gate("neroruins:codex/mars"), "Mars Codex", "Archive every log of the Mars chapter.", "RUIN-GATE-04",
     "Codex member"),
    ("neroruins", "codex_cindara", "silver", False, "neroruins:meridian_keystone",
     gate("neroruins:codex/cindara"), "Cindara Codex", "Archive every log of the Cindara chapter.",
     "RUIN-GATE-04", "Codex member"),
    ("neroruins", "codex_glacira", "silver", False, "neroruins:reach_thermal_core",
     gate("neroruins:codex/glacira"), "Glacira Codex", "Archive every log of the Glacira chapter.",
     "RUIN-GATE-04", "Codex member"),
    ("neroruins", "codex_greenxertz", "silver", False, "neroruins:rootglass_vessel",
     gate("neroruins:codex/greenxertz"), "Greenxertz Codex", "Archive every log of the Greenxertz chapter.",
     "RUIN-GATE-04", "Codex member"),
    ("neroruins", "codex_nyxara", "silver", False, "neroruins:twilight_prism",
     gate("neroruins:codex/nyxara"), "Nyxara Codex", "Archive every log of the Nyxara chapter.",
     "RUIN-GATE-04", "Codex member"),
    ("neroruins", "codex_lattice", "nero", False, "neroruins:resonance_pylon",
     gate("neroruins:codex/lattice"), "Lattice Codex",
     "Archive every log of the Lattice chapter, the last of the Codex.", "RUIN-GATE-04",
     "Codex member; the mod's nero"),
    ("neroruins", "sentinel_breaker", "gold", False, "neroruins:sentinel_mk3_spawn_egg",
     killed("neroruins:sentinel_mk3"), "Sentinel Breaker", "Land the final blow on a Mark III Sentinel.",
     "RUIN-STAT-01", "kill stat credits the last hit only"),
    ("neroruins", "heart_of_resonance", "silver", True, "neroruins:resonance_heart",
     item("neroruins:resonance_heart"), "Heart of Resonance", "Get the Resonance Heart.", "RUIN-ITEM-01",
     "hidden; never a collection member"),
]

# Collection-completion trophies are hand-written under neroland_trophies/collection/; cite them here.
EXTRA_ROWS = [
    ["nerotrophies:collection/boss_hunter", "nero", "false", "collection", "boss_hunter",
     "awarded on completing the boss_hunter collection (no trigger)"],
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
            "requires_mod": mod,
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
        w.writerows(EXTRA_ROWS)
    print(f"{len(T)} trophies written")


if __name__ == "__main__":
    main()
