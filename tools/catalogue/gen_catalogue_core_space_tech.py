"""Writes the core-space-tech slice of the starter trophy catalogue.

Covers Neroland Core, vanilla Minecraft, Nerospace, Nerotech, NeroPower and NeroLogistics. Every trigger
id and icon was checked by hand against the sibling's source (and vanilla ids against the 1.21.1 and 26.3
data jars). Re-run after editing the table below:

    python tools/catalogue/gen_catalogue_core_space_tech.py

Outputs (overwritten, LF line endings):
  common/src/main/resources/data/nerotrophies/neroland_trophies/<mod>/<slug>.json
  tools/lang/pending/catalogue-core-space-tech.json
  tools/catalogue/citations/core-space-tech.csv
"""

import csv
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
LANG = ROOT / "tools/lang/pending/catalogue-core-space-tech.json"
CSV = ROOT / "tools/catalogue/citations/core-space-tech.csv"


def adv(i):
    return {"type": "advancement", "advancement": i}


def gate(i):
    return {"type": "nero_event", "event": "gate", "gate": i}


def placed(i):
    return {"type": "block_placed", "block": i}


def obtained(i):
    return {"type": "item_obtained", "item": i}


# mod, slug, tier, hidden, icon, trigger, row, title, description, notes
T = [
    # --- Neroland Core: the arc gates are opened by siblings; the trash can is craftable on Core alone.
    ("nerolandcore", "spark_of_industry", "bronze", False, "nerolandcore:battery",
     gate("nerolandcore:industrial_power"), "CORE-GATE-01",
     "Spark of Industry", "Bring your first industrial machine online.", "arc step 1"),
    ("nerolandcore", "escape_velocity", "silver", False, "nerolandcore:plasma_glass",
     gate("nerolandcore:reached_orbit"), "CORE-GATE-02",
     "Escape Velocity", "Complete a rocket launch and reach orbit.", "arc step 2"),
    ("nerolandcore", "colonist", "silver", False, "nerolandcore:item_store",
     gate("nerolandcore:first_colony"), "CORE-GATE-03",
     "Colonist", "Found your first colony or station.", "arc step 3"),
    ("nerolandcore", "deep_space_pioneer", "gold", False, "nerolandcore:void_crystal",
     gate("nerolandcore:deep_space"), "CORE-GATE-04",
     "Deep Space Pioneer", "Reach a far world, or walk on one brought to life.", "arc step 4"),
    ("nerolandcore", "tidy_engineer", "bronze", True, "nerolandcore:trash_can",
     placed("nerolandcore:trash_can"), "CORE-ITEM-02",
     "Tidy Engineer", "Place a Trash Can. Some things are better gone.", "earnable on Core alone"),
    # --- Vanilla: ids present in both the 1.21.1 and 26.3 data jars.
    ("minecraft", "hellwalker", "bronze", False, "minecraft:obsidian",
     adv("minecraft:story/enter_the_nether"), "VAN-ADV-02",
     "Hellwalker", "Step through a portal into the Nether.", "re-triggered from dimension to advancement"),
    ("minecraft", "withering_heights", "silver", False, "minecraft:wither_skeleton_skull",
     adv("minecraft:nether/summon_wither"), "VAN-ADV-05",
     "Withering Heights", "Summon the Wither.", ""),
    ("minecraft", "dragonslayer", "silver", False, "minecraft:dragon_head",
     adv("minecraft:end/kill_dragon"), "VAN-ADV-04",
     "Dragonslayer", "Defeat the Ender Dragon.", "re-tiered gold to silver"),
    ("minecraft", "beaconator", "gold", False, "minecraft:beacon",
     adv("minecraft:nether/create_full_beacon"), "VAN-ADV-06",
     "Beaconator", "Light a beacon at full power.", ""),
    ("minecraft", "seen_it_all", "gold", False, "minecraft:diamond_boots",
     adv("minecraft:adventure/adventuring_time"), "VAN-ADV-07",
     "Seen It All", "Visit every Overworld biome.", "re-tiered nero to gold"),
    ("minecraft", "how_did_we_get_here", "nero", False, "minecraft:brewing_stand",
     adv("minecraft:nether/all_effects"), "VAN-ADV-10",
     "How Did We Get Here?", "Carry every status effect at once.", "Core and vanilla nero"),
    # --- Nerospace.
    ("nerospace", "luna", "bronze", False, "nerospace:regolith",
     adv("nerospace:luna"), "SPACE-ADV-04",
     "Moonwalker", "Set foot on Luna.", ""),
    ("nerospace", "mars", "bronze", False, "nerospace:rust_pigment",
     adv("nerospace:mars"), "SPACE-ADV-05",
     "Red Dust", "Set foot on Mars.", ""),
    ("nerospace", "cindara", "silver", False, "nerospace:cindrite",
     adv("nerospace:cindara"), "SPACE-ADV-06",
     "Ember World", "Set foot on Cindara.", ""),
    ("nerospace", "glacira", "silver", False, "nerospace:glacite",
     adv("nerospace:glacira"), "SPACE-ADV-07",
     "Frostbound", "Set foot on Glacira.", ""),
    ("nerospace", "greenxertz", "silver", False, "nerospace:nerosteel_ingot",
     adv("nerospace:greenxertz"), "SPACE-ADV-08",
     "Green Horizon", "Set foot on Greenxertz.", ""),
    ("nerospace", "nyxara", "silver", False, "nerospace:nyx_crystal_cluster",
     adv("nerospace:nyxara"), "SPACE-ADV-09",
     "Edge of Night", "Set foot on Nyxara.", ""),
    ("nerospace", "the_big_cheese", "bronze", True, "nerospace:moon_cheese",
     adv("nerospace:luna/the_big_cheese"), "SPACE-ADV-10",
     "The Big Cheese", "Find out what the Moon is really made of.", "re-tiered silver to bronze"),
    ("nerospace", "station_founder", "silver", False, "nerospace:station_charter",
     adv("nerospace:guide/station_charter"), "SPACE-ADV-12",
     "Station Founder", "Found a space station with a Station Charter.", ""),
    ("nerospace", "first_freight", "silver", False, "nerospace:cargo_crate",
     gate("nerospace:first_freight"), "SPACE-GATE-01",
     "First Freight", "Have your first cargo flight delivered.", "offline-capable gate"),
    ("nerospace", "xenoarchaeologist", "gold", True, "nerospace:alien_core",
     adv("nerospace:guide/alien_core"), "SPACE-ADV-18",
     "Xenoarchaeologist", "Recover an intact Alien Core.", ""),
    ("nerospace", "storm_chaser", "gold", False, "nerospace:nyxara_compass",
     gate("nerospace:worlds/nyxara/weather"), "SPACE-GATE-04",
     "Storm Chaser", "Ride out Nyxara's weather in the open.", "single gate; all-six variant needs a hook"),
    ("nerospace", "living_world", "gold", False, "nerospace:terraformer",
     gate("nerospace:living_world"), "SPACE-GATE-02",
     "Living World", "Stand on ground terraformed into a living world.", "replaces Worldmaker; not nero"),
    ("nerospace", "new_life", "nero", False, "nerospace:meadow_loper_spawn_egg",
     adv("nerospace:guide/new_life"), "SPACE-ADV-16",
     "New Life", "Breed native creatures on a world you helped bring to life.", "Nerospace nero"),
    # --- Nerotech.
    ("nerotech", "plugged_in", "bronze", False, "nerotech:nero_generator",
     adv("nerotech:nero_generator"), "TECH-ADV-02",
     "Plugged In", "Get your hands on a Nero Generator.", ""),
    ("nerotech", "clean_hands", "bronze", False, "nerotech:remediator",
     adv("nerotech:remediator"), "TECH-ADV-06",
     "Clean Hands", "Build a Remediator to undo your pollution.", ""),
    ("nerotech", "automaton", "silver", False, "nerotech:auto_crafter",
     adv("nerotech:auto_crafter"), "TECH-ADV-04",
     "Automaton", "Build an Auto Crafter.", ""),
    ("nerotech", "stellar_fuel", "silver", False, "nerotech:stellar_cell",
     adv("nerotech:stellar_fuel"), "TECH-ADV-09",
     "Stellar Fuel", "Produce a Stellar Cell.", "re-tiered gold to silver"),
    ("nerotech", "star_in_a_jar", "gold", False, "nerotech:fusion_reactor",
     adv("nerotech:fusion_reactor"), "TECH-ADV-08",
     "Star in a Jar", "Build a Fusion Reactor and a fusion cell to feed it.", ""),
    ("nerotech", "antimatter", "gold", True, "nerotech:antimatter_cell",
     adv("nerotech:antimatter_cell"), "TECH-ADV-10",
     "Antimatter", "Contain antimatter in a cell.", ""),
    ("nerotech", "event_horizon", "nero", False, "nerotech:singularity_vault",
     adv("nerotech:singularity_vault"), "TECH-ADV-10",
     "Event Horizon", "Build a Singularity Vault.", "Nerotech nero"),
    # --- NeroPower (hard-depends on Nerotech, so requires_mod names NeroPower only).
    ("neropower", "hot_rod", "bronze", False, "neropower:control_rod",
     adv("neropower:root"), "POWER-ADV-01",
     "Hot Rod", "Make your first control rod.", ""),
    ("neropower", "slow_burn", "bronze", False, "neropower:radioisotope_generator",
     adv("neropower:radioisotope_generator"), "POWER-ADV-05",
     "Slow Burn", "Build a Radioisotope Generator.", ""),
    ("neropower", "heat_engine", "bronze", False, "neropower:stirling_generator",
     adv("neropower:stirling_generator"), "POWER-ADV-06",
     "Heat Engine", "Build a Stirling Generator.", ""),
    ("neropower", "line_of_sight", "silver", False, "neropower:beam_transmitter",
     gate("neropower:beam_linked"), "POWER-GATE-02",
     "Line of Sight", "Link a beam transmitter to send power through the air.", "always-on gate (acting player)"),
    ("neropower", "fuel_cycle", "silver", True, "neropower:reprocessed_pellet",
     obtained("neropower:reprocessed_pellet"), "POWER-ITEM-01",
     "Fuel Cycle", "Reprocess spent fuel into a fresh pellet.", ""),
    ("neropower", "orbital_power", "gold", False, "neropower:orbital_receiver",
     obtained("neropower:orbital_receiver"), "POWER-ITEM-01",
     "Orbital Power", "Build an Orbital Receiver.", ""),
    ("neropower", "reactor_core", "gold", False, "neropower:fission_core",
     adv("neropower:fission_core"), "POWER-ADV-02",
     "Reactor Core", "Build a fission core.", "renamed from Criticality; nero waits for a hook"),
    # --- NeroLogistics.
    ("nerologistics", "network_online", "bronze", False, "nerologistics:network_controller",
     placed("nerologistics:network_controller"), "LOGI-ITEM-01",
     "Network Online", "Place a Network Controller.", ""),
    ("nerologistics", "bit_bucket", "bronze", False, "nerologistics:drive_bay",
     placed("nerologistics:drive_bay"), "LOGI-ITEM-01",
     "Bit Bucket", "Place a Drive Bay.", ""),
    ("nerologistics", "air_mail", "silver", False, "nerologistics:drone_hub",
     placed("nerologistics:drone_hub"), "LOGI-ITEM-03",
     "Air Mail", "Place a Drone Hub.", ""),
    ("nerologistics", "all_aboard", "silver", False, "nerologistics:train_station",
     placed("nerologistics:train_station"), "LOGI-ITEM-03",
     "All Aboard", "Place a Train Station.", ""),
    ("nerologistics", "half_a_million", "gold", False, "nerologistics:item_cell_512k",
     obtained("nerologistics:item_cell_512k"), "LOGI-ITEM-02",
     "Half a Million", "Obtain a 512k Item Cell.", ""),
    ("nerologistics", "interplanetary_freight", "gold", False, "nerologistics:rocket_cargo_port",
     gate("nerologistics:first_shipment"), "LOGI-GATE-01",
     "Interplanetary Freight", "Launch your first shipment from a Rocket Cargo Port.", "offline-capable gate"),
]


def dump(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def main():
    lang = {}
    rows = []
    order = {}
    seen_triggers = set()
    for mod, slug, tier, hidden, icon, trigger, row, title, desc, notes in T:
        key = f"trophy.nerotrophies.{mod}.{slug}"
        tkey = json.dumps(trigger, sort_keys=True)
        assert tkey not in seen_triggers, f"duplicate trigger {tkey}"
        seen_triggers.add(tkey)
        assert not (tier == "nero" and trigger["type"] == "item_obtained")
        order[mod] = order.get(mod, 0) + 10
        body = {
            "schema": 1,
            "icon": icon,
            "tier": tier,
            "trigger": trigger,
        }
        if hidden:
            body["hidden"] = True
        body["title"] = key + ".title"
        body["description"] = key + ".description"
        if mod == "minecraft":
            body["category"] = "minecraft"
        else:
            body["requires_mod"] = mod
        body["order"] = order[mod]
        path = DATA / mod / f"{slug}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(dump(body), encoding="utf-8", newline="\n")
        lang[key + ".title"] = title
        lang[key + ".description"] = desc
        ttarget = next(v for k, v in trigger.items() if k not in ("type", "event"))
        ttype = trigger["type"] + (":" + trigger["event"] if "event" in trigger else "")
        rows.append([f"nerotrophies:{mod}/{slug}", tier, str(hidden).lower(), f"{ttype} {ttarget}", row, notes])
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(dump(lang), encoding="utf-8", newline="\n")
    CSV.parent.mkdir(parents=True, exist_ok=True)
    buf = io.StringIO()
    w = csv.writer(buf, lineterminator="\n")
    w.writerow(["id", "tier", "hidden", "trigger_type", "survey_row", "notes"])
    w.writerows(rows)
    CSV.write_text(buf.getvalue(), encoding="utf-8", newline="\n")
    print(f"{len(T)} trophies written")


if __name__ == "__main__":
    main()
