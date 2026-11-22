#!/usr/bin/env python3
"""Generates the sibling-gate catalogue group: trophies on the record gates and stats the sibling mods
opened for NeroTrophies after 0.1.0 (each lights up from that sibling's next release).

Writes, relative to the repo root:
  common/src/main/resources/data/nerotrophies/neroland_trophies/<mod>/<slug>.json
  common/src/main/resources/data/nerotrophies/neroland_trophy_collections/ruin_survey.json
  tools/lang/pending/catalogue-sibling-gates.json
  tools/catalogue/citations/sibling-gates.csv

Every gate id was checked against the sibling's data/<mod>/neroland_gates/ folder (PLAYER scope) and every
stat against its stat registration. Orders continue after the existing trophies in each category. Retargets of
existing trophies live in the group generators that own them, not here. Run from anywhere:
python gen_sibling_gates.py
"""
import csv
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
COLLECTIONS = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophy_collections"
LANG = ROOT / "tools/lang/pending/catalogue-sibling-gates.json"
CSV = ROOT / "tools/catalogue/citations/sibling-gates.csv"


def gate(g):
    return {"type": "nero_event", "event": "gate", "gate": g}


def custom_stat(s, n):
    return {"type": "stat", "stat_type": "minecraft:custom", "stat": s, "at_least": n}


FACTIONS = [
    ("free_colonists", "Free Colonists", "nerofactions:colonist_field_gear_helmet", "Colonist Field Gear"),
    ("miner_union", "Miner Union", "nerofactions:union_exosuit_helmet", "Union Exosuit"),
    ("nero_corporation", "Nero Corporation", "nerofactions:corporate_power_suit_helmet", "Corporate Power Suit"),
    ("salvagers", "Salvagers", "nerofactions:salvager_patchwork_helmet", "Salvager Patchwork"),
    ("space_guild", "Space Guild", "nerofactions:guild_flight_suit_helmet", "Guild Flight Suit"),
    ("terraforming_authority", "Terraforming Authority", "nerofactions:authority_environment_suit_helmet",
     "Authority Environment Suit"),
    ("void_cult", "Void Cult", "nerofactions:void_vestments_helmet", "Void Vestments"),
]

RUIN_FAMILIES = [
    ("bunker", "Bunker Delver", "bronze", "Step inside a sealed bunker.", "neroruins:security_keycard"),
    ("survey_lab", "Lab Visitor", "bronze", "Step inside an abandoned survey lab.", "neroruins:survey_scanner"),
    ("crash_site", "Wreck Diver", "bronze", "Step inside a crash site.", "neroruins:rusted_survey_hull"),
    ("colony_field", "Lost Colony", "bronze", "Walk the ruins of a failed colony field.", "neroruins:log_terminal"),
    ("lattice_temple", "Temple Threshold", "silver", "Step inside a Lattice Temple.", "neroruins:latticework"),
    ("halcyon_ark", "Ark Boarder", "silver", "Step aboard the Halcyon Ark.", "neroruins:meridian_keystone"),
]

# (mod, slug, tier, hidden, icon, trigger, title, description, survey row, notes)
T = [
    # NeroPower
    ("neropower", "criticality", "nero", False, "neropower:fission_core", gate("neropower:criticality"),
     "Criticality", "Bring a formed fission core to criticality: a reactor actually burning fuel.",
     "POWR-GATE-02", "operator credited in the moment; owner record kept"),
    ("neropower", "deep_reserves", "silver", False, "neropower:battery_bank_controller",
     gate("neropower:bank_capacity"), "Deep Reserves", "Grow a battery bank to its milestone capacity.",
     "POWR-GATE-03", "operator credited in the moment; owner record kept"),
    ("neropower", "breeder", "silver", False, "neropower:breeder_blanket", gate("neropower:first_breed"),
     "Breeder", "Have a reactor's Breeder Blankets kindle their first pellet.", "POWR-GATE-04",
     "operator credited in the moment; nothing persisted by NeroPower"),
    ("neropower", "spin_up", "bronze", False, "neropower:turbine_spindle", gate("neropower:turbine_spinup"),
     "Spin-Up", "Bring a Steam Turbine up to rated speed and deliver power.", "POWR-GATE-05",
     "operator credited in the moment; nothing persisted by NeroPower"),
    ("neropower", "full_ring", "gold", False, "neropower:smes_controller", gate("neropower:smes_charged"),
     "Full Ring", "Charge a formed Storage Ring all the way to full.", "POWR-GATE-06",
     "operator credited in the moment; nothing persisted by NeroPower"),
    ("neropower", "glass_tomb", "silver", False, "neropower:vitrified_waste", gate("neropower:first_vitrified"),
     "Glass Tomb", "Finish your first Vitrified Waste log in a Vitrifier.", "POWR-GATE-07",
     "set through BackEndHooks.setVitrified; nothing persisted by NeroPower"),
    # NeroSecurity
    ("nerosecurity", "fortress", "nero", False, "nerosecurity:security_hub", gate("nerosecurity:fortress"),
     "Fortress", "Link four cameras, a force field emitter and a blast door to one security hub.",
     "SECU-GATE-04", "pure predicate SecurityMilestones.isFortress"),
    # NeroRuins
    ("neroruins", "temple_cleared", "gold", False, "neroruins:resonance_pylon", gate("neroruins:temple_cleared"),
     "Temple Cleared", "Clear a Lattice Temple: every spawner dormant and its last Sentinel down.",
     "RUIN-GATE-05", "killer plus survival/adventure players inside the temple"),
] + [
    ("neroruins", f"discovered_{fam}", tier, False, icon, gate(f"neroruins:discovered/{fam}"), title, desc,
     "RUIN-GATE-06", "Ruin Survey member; retroactive on next visit")
    for fam, title, tier, desc, icon in RUIN_FAMILIES
] + [
    # Nerospace
    ("nerospace", "liftoff", "bronze", False, "nerospace:rocket_tier_1", gate("nerospace:first_launch"),
     "Liftoff", "Pilot a rocket off the launch pad.", "SPACE-GATE-03", "pilot only"),
    ("nerospace", "crewed_launch", "silver", False, "nerospace:rocket_tier_2", gate("nerospace:crew_launch"),
     "Crewed Launch", "Launch with at least two people aboard.", "SPACE-GATE-04", "everyone seated"),
    ("nerospace", "rover_driver", "bronze", False, "nerospace:rover", gate("nerospace:rover_driven"),
     "Rover Driver", "Drive a powered rover.", "SPACE-GATE-05", ""),
    ("nerospace", "station_builder", "silver", False, "nerospace:station_compass",
     gate("nerospace:station_module"), "Station Builder", "Pay for a new module on your space station.",
     "SPACE-GATE-06", ""),
    # NeroFactions
    ("nerofactions", "outpost_finder", "silver", False, "nerofactions:faction_registry",
     gate("nerofactions:outpost_found"), "Outpost Finder", "Find a faction outpost and step inside it.",
     "FACT-GATE-03", "retroactive on the next visit"),
    ("nerofactions", "contractor", "silver", False, "nerofactions:contract_board",
     gate("nerofactions:contract_completed_10"), "Contractor", "Complete 10 faction contracts.",
     "FACT-GATE-04", "from the existing per-player contract count"),
] + [
    ("nerofactions", f"regalia_{fid}", "silver", False, icon, gate(f"nerofactions:full_set_{fid}"),
     f"{name} Regalia", f"Wear the full {gear} set.", "FACT-GATE-05", "opens for whoever wears the set")
    for fid, name, icon, gear in FACTIONS
] + [
    # NeroCreatures
    ("nerocreatures", "tamer", "silver", False, "nerocreatures:creature_saddle", gate("nerocreatures:tamed/10"),
     "Tamer", "Tame 10 creatures by hand.", "CREA-GATE-06", "lifetime tame count, capped at 25, erasable"),
    ("nerocreatures", "beastmaster", "gold", False, "nerocreatures:capture_pod", gate("nerocreatures:tamed/25"),
     "Beastmaster", "Tame 25 creatures by hand.", "CREA-GATE-06", "lifetime tame count, capped at 25, erasable"),
    ("nerocreatures", "complete_bestiary", "gold", False, "nerocreatures:field_scanner",
     gate("nerocreatures:bestiary/complete"), "Complete Bestiary", "Study every species in the Bestiary.",
     "CREA-GATE-07", "gold: Boss Hunter already holds the category's nero slot"),
    # NeroAgriculture
    ("neroagriculture", "bumper_crop", "silver", False, "neroagriculture:resource_seed",
     custom_stat("neroagriculture:crops_harvested", 100), "Bumper Crop", "Harvest 100 Nero crops.",
     "AGRI-STAT-01", "vanilla custom stat, no mod store"),
    ("neroagriculture", "breadbasket", "gold", False, "neroagriculture:crop_tower_controller",
     custom_stat("neroagriculture:crops_harvested", 1000), "Breadbasket", "Harvest 1,000 Nero crops.",
     "AGRI-STAT-01", "vanilla custom stat, no mod store"),
    ("neroagriculture", "worldshaper", "gold", False, "neroagriculture:alien_seed",
     gate("neroagriculture:terraform_complete"), "Worldshaper", "Complete a terraforming project.",
     "AGRI-GATE-07", "stored controller owner, offline-capable"),
    # NeroArcade
    ("neroarcade", "high_score", "gold", False, "neroarcade:marquee_screen", gate("neroarcade:first_record"),
     "High Score", "Set a new top score on a cartridge's board.", "ARCD-GATE-04", "ties credit nobody; never hot-seat"),
    ("neroarcade", "arcade_legend", "gold", False, "neroarcade:arcade_cabinet",
     custom_stat("neroarcade:matches_won", 100), "Arcade Legend", "Win 100 credited arcade matches.",
     "ARCD-STAT-01", "gold: a stat trigger cannot carry nero"),
    # NeroColonies
    ("nerocolonies", "first_export", "bronze", False, "nerocolonies:colony_planner",
     gate("nerocolonies:first_export"), "First Export", "Ship your colony's first export.", "COLO-GATE-09", ""),
    ("nerocolonies", "population_25", "silver", False, "nerocolonies:colony_beacon",
     gate("nerocolonies:population_25"), "Bustling", "Grow a colony you own to 25 Nerans.", "COLO-GATE-10",
     "credited to the colony owner"),
    ("nerocolonies", "population_50", "gold", False, "nerocolonies:capacity_module",
     gate("nerocolonies:population_50"), "Boomtown", "Grow a colony you own to 50 Nerans.", "COLO-GATE-10",
     "credited to the colony owner"),
    # NeroCompanion
    ("nerocompanion", "soulbound", "nero", False, "nerocompanion:capsule", gate("nerocompanion:bond_max"),
     "Soulbound", "Raise a companion's bond to the highest level.", "COMP-GATE-04", ""),
    # NeroDecor
    ("nerodecor", "light_show", "bronze", False, "nerodecor:scene_chip", gate("nerodecor:scene_authored"),
     "Light Show", "Save a Chroma scene.", "DECO-GATE-01", ""),
    ("nerodecor", "wired_up", "silver", False, "nerodecor:chroma_linker", gate("nerodecor:hub_linked/16"),
     "Wired Up", "Link 16 blocks to one Chroma Hub network.", "DECO-GATE-02", "credited to the network owner"),
    ("nerodecor", "master_electrician", "gold", False, "nerodecor:chroma_cube", gate("nerodecor:hub_linked/64"),
     "Master Electrician", "Link 64 blocks to one Chroma Hub network.", "DECO-GATE-02",
     "credited to the network owner"),
    # NeroEvents
    ("neroevents", "storm_survivor", "bronze", False, "neroevents:event_console",
     gate("neroevents:storm_survived"), "Storm Survivor", "Ride out a storm event to its end.", "EVNT-GATE-05",
     "only storms that ran their course"),
    ("neroevents", "war_victor", "gold", False, "neroevents:war_beacon", gate("neroevents:war_victor"),
     "War Victor", "Fight on the winning side of a faction war.", "EVNT-GATE-06", "a draw credits nobody"),
    # NeroLink (no items: vanilla icons)
    ("nerolink", "linked_up", "bronze", False, "minecraft:recovery_compass", gate("nerolink:first_paired"),
     "Linked Up", "Pair a device with NeroLink.", "LINK-GATE-01", "UUID only, no device data"),
    ("nerolink", "second_screen", "silver", False, "minecraft:spyglass", gate("nerolink:two_devices"),
     "Second Screen", "Pair a second device with NeroLink.", "LINK-GATE-02", "counted from stored device rows"),
    ("nerolink", "remote_operator", "silver", False, "minecraft:lever", gate("nerolink:first_action"),
     "Remote Operator", "Run an action on your base from the NeroLink app.", "LINK-GATE-03", ""),
    ("nerolink", "read_the_manual", "bronze", True, "minecraft:writable_book", gate("nerolink:wiki_reader"),
     "Read the Manual", "Open a wiki page in the NeroLink app.", "LINK-GATE-04", ""),
    ("nerolink", "always_connected", "bronze", False, "minecraft:bell", gate("nerolink:alerts_enabled"),
     "Always Connected", "Turn on NeroLink alerts.", "LINK-GATE-05", ""),
    # NeroMaps
    ("neromaps", "shared_bearings", "bronze", False, "neromaps:waypoint_compass",
     gate("neromaps:first_shared_waypoint"), "Shared Bearings", "Have a waypoint you shared accepted.",
     "MAPS-GATE-02", "credits the sharer"),
    ("neromaps", "surveyor", "silver", False, "neromaps:survey_kit", gate("neromaps:explored/1000"),
     "Surveyor", "Explore 1,000 chunks.", "MAPS-GATE-01", "counted from the explored bitsets"),
    ("neromaps", "explorer", "gold", False, "neromaps:wall_map", gate("neromaps:explored/10000"),
     "Explorer", "Explore 10,000 chunks.", "MAPS-GATE-01", "counted from the explored bitsets"),
    ("neromaps", "world_charted", "nero", False, "neromaps:atlas_upgrade_pages", gate("neromaps:explored/50000"),
     "World Charted", "Explore 50,000 chunks.", "MAPS-GATE-01", "counted from the explored bitsets"),
    # NeroNotes
    ("neronotes", "on_the_charts", "silver", False, "neronotes:resonator", gate("neronotes:listeners/10"),
     "On the Charts", "Have 10 listeners tuned in to your channel at once.", "NOTE-GATE-02", "live subscriber set"),
    ("neronotes", "chart_topper", "nero", False, "neronotes:voice_pedestal", gate("neronotes:listeners/25"),
     "Chart Topper", "Have 25 listeners tuned in to your channel at once.", "NOTE-GATE-02", "live subscriber set"),
    ("neronotes", "prolific", "silver", False, "neronotes:disk_press", gate("neronotes:published/10"),
     "Prolific", "Have 10 of your compositions in the shared library.", "NOTE-GATE-03",
     "counted from visible library rows"),
    ("neronotes", "hit_factory", "gold", False, "neronotes:pattern_wall", gate("neronotes:published/25"),
     "Hit Factory", "Have 25 of your compositions in the shared library.", "NOTE-GATE-03",
     "25 is the default per-player quota"),
    # NeroEconomy
    ("neroeconomy", "self_made", "nero", False, "neroeconomy:credit_chip", gate("neroeconomy:earned_100k"),
     "Self-Made", "Earn 100,000 credits paid out by the economy itself.", "ECON-GATE-09",
     "lifetime earnings, not balance; Tycoon stays on balance"),
    ("neroeconomy", "fine_print", "bronze", False, "neroeconomy:traders_handbook",
     gate("neroeconomy:handbook_read"), "Read the Fine Print", "Read the Trader's Handbook through.",
     "ECON-GATE-10", ""),
    # NeroQuests (chapter gates of NeroQuests' own spine)
    ("neroquests", "daily_grind", "bronze", False, "minecraft:paper", gate("neroquests:first_contract"),
     "Daily Grind", "Complete your first quest contract.", "QUES-GATE-01", ""),
    ("neroquests", "groundwork_done", "bronze", False, "minecraft:crafting_table",
     gate("neroquests:chapter/neroquests/groundwork"), "Groundwork Done", "Complete the Groundwork chapter.",
     "QUES-GATE-02", ""),
    ("neroquests", "industrialist", "silver", False, "minecraft:blast_furnace",
     gate("neroquests:chapter/neroquests/industrial"), "Industrialist", "Complete the Industrial chapter.",
     "QUES-GATE-02", ""),
    ("neroquests", "space_racer", "gold", False, "minecraft:firework_rocket",
     gate("neroquests:chapter/neroquests/space_race"), "Space Racer", "Complete the Space Race chapter.",
     "QUES-GATE-02", ""),
    ("neroquests", "quest_veteran", "silver", False, "minecraft:writable_book", gate("neroquests:completed/25"),
     "Quest Veteran", "Complete 25 quests.", "QUES-GATE-03", "from NeroQuests' own stored progress"),
    ("neroquests", "quest_master", "gold", False, "minecraft:enchanted_book", gate("neroquests:completed/100"),
     "Quest Master", "Complete 100 quests.", "QUES-GATE-03", "from NeroQuests' own stored progress"),
    ("neroquests", "the_whole_book", "nero", False, "neroquests:quest_book", gate("neroquests:all_chapters"),
     "The Whole Book", "Complete every quest chapter.", "QUES-GATE-04", ""),
]

COLLECTION = {
    "schema": 1,
    "icon": "neroruins:surveyors_compass",
    "title": "collection.nerotrophies.ruin_survey.title",
    "description": "collection.nerotrophies.ruin_survey.description",
    "trim": "wild_sand",
    "members": [f"nerotrophies:neroruins/discovered_{fam}" for fam, *_ in RUIN_FAMILIES],
    "requires_mod": "neroruins",
}
COLLECTION_LANG = {
    "collection.nerotrophies.ruin_survey.title": "Ruin Survey",
    "collection.nerotrophies.ruin_survey.description": "Set foot in every family of ruin NeroRuins scatters "
                                                      "across the worlds.",
}


def main():
    mine = {(mod, slug) for mod, slug, *_ in T}
    orders = {}
    for f in DATA.glob("*/*.json"):
        if (f.parent.name, f.stem) in mine:
            continue
        o = json.loads(f.read_text(encoding="utf-8")).get("order", 0)
        orders[f.parent.name] = max(orders.get(f.parent.name, 0), o)
    lang = {}
    rows = []
    for mod, slug, tier, hidden, icon, trigger, title, desc, row, notes in T:
        orders[mod] = orders.get(mod, 0) + 10
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
            "order": orders[mod],
        }
        out = DATA / mod / f"{slug}.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8", newline="\n")
        lang[f"{key}.title"] = title
        lang[f"{key}.description"] = desc
        rows.append([f"nerotrophies:{mod}/{slug}", tier, str(hidden).lower(), trigger["type"], row, notes])
    (COLLECTIONS / "ruin_survey.json").write_text(json.dumps(COLLECTION, indent=2) + "\n", encoding="utf-8",
                                                  newline="\n")
    lang.update(COLLECTION_LANG)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    with CSV.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "tier", "hidden", "trigger_type", "survey_row", "notes"])
        w.writerows(rows)
    print(f"{len(T)} trophies and 1 collection written")


if __name__ == "__main__":
    main()
