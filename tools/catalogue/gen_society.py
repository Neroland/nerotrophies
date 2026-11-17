#!/usr/bin/env python3
"""Writes the society catalogue group: NeroEconomy, NeroFactions, NeroColonies, NeroSecurity,
NeroEvents and NeroQuests.

Outputs (paths relative to the repo root):
  common/src/main/resources/data/nerotrophies/neroland_trophies/<mod>/<slug>.json
  tools/lang/pending/catalogue-society.json
  tools/catalogue/citations/society.csv

Every trigger id and icon was checked against the sibling's source. Re-running is idempotent.
"""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "common/src/main/resources/data/nerotrophies/neroland_trophies"
LANG = ROOT / "tools/lang/pending/catalogue-society.json"
CITES = ROOT / "tools/catalogue/citations/society.csv"


def gate(gid):
    return {"type": "nero_event", "event": "gate", "gate": gid}


def rep(faction, at_least):
    return {"type": "nero_event", "event": "reputation", "faction": faction, "at_least": at_least}


def credits(at_least):
    return {"type": "nero_event", "event": "currency", "currency": "nerolandcore:credits", "at_least": at_least}


def item(iid, count=1):
    t = {"type": "item_obtained", "item": iid}
    if count != 1:
        t["count"] = count
    return t


def stat(stat_type, sid, at_least):
    return {"type": "stat", "stat_type": stat_type, "stat": sid, "at_least": at_least}


def placed(bid, count=1):
    t = {"type": "block_placed", "block": bid}
    if count != 1:
        t["count"] = count
    return t


# Inner circle is the top reputation tier (2500) in every NeroFactions faction file.
FACTIONS = [
    ("free_colonists", "Free Colonists"),
    ("miner_union", "Miner Union"),
    ("nero_corporation", "Nero Corporation"),
    ("salvagers", "Salvagers"),
    ("space_guild", "Space Guild"),
    ("terraforming_authority", "Terraforming Authority"),
    ("void_cult", "Void Cult"),
]

# (mod, slug, tier, hidden, icon, trigger, title, description, survey_row, notes[, requires])
T = [
    # --- NeroEconomy ---------------------------------------------------------------------------
    ("neroeconomy", "open_for_business", "bronze", False, "neroeconomy:merchant_stall",
     gate("neroeconomy:first_sale"), "Open for Business", "Make your first sale.", "ECON-GATE-01", ""),
    ("neroeconomy", "first_purchase", "bronze", False, "neroeconomy:market_board",
     gate("neroeconomy:first_purchase"), "First Purchase", "Buy something on the market.", "ECON-GATE-02", ""),
    ("neroeconomy", "shopkeeper", "bronze", False, "neroeconomy:player_shop",
     placed("neroeconomy:player_shop"), "Shopkeeper", "Place a Player Shop.", "ECON-ITEM-01", ""),
    ("neroeconomy", "shareholder", "silver", False, "neroeconomy:exchange_terminal",
     gate("neroeconomy:first_dividend"), "Shareholder", "Receive your first dividend.", "ECON-GATE-06", ""),
    ("neroeconomy", "gavel", "silver", True, "neroeconomy:trading_terminal",
     gate("neroeconomy:first_auction_win"), "Gavel", "Win an auction.", "ECON-GATE-08",
     "hidden; never a collection member"),
    ("neroeconomy", "five_figures", "silver", False, "neroeconomy:credit_chip",
     credits(10_000), "Five Figures", "Hold 10,000 credits.", "ECON-CUR-01",
     "threshold set from the default money supply: 100 starting balance; Reserve 250000 plus 5000 a day"),
    ("neroeconomy", "seasoned_trader", "gold", False, "neroeconomy:traders_handbook",
     gate("neroeconomy:trader_100"), "Seasoned Trader", "Make 100 sales.", "ECON-GATE-03", ""),
    ("neroeconomy", "tycoon", "gold", False, "neroeconomy:bank_terminal",
     credits(100_000), "Tycoon", "Hold 100,000 credits.", "ECON-CUR-01",
     "gold not nero: 40 percent of the default starting money supply; a balance can be gifted"),
    # --- NeroFactions --------------------------------------------------------------------------
    ("nerofactions", "sworn_in", "bronze", False, "nerofactions:faction_registry",
     gate("nerofactions:sworn_in"), "Sworn In", "Join a faction.", "FACT-GATE-01", ""),
    ("nerofactions", "terminal_customer", "bronze", False, "nerofactions:trade_terminal",
     gate("nerofactions:first_terminal_trade"), "Terminal Customer", "Trade at a faction terminal.",
     "FACT-GATE-04", ""),
] + [
    ("nerofactions", f"inner_circle_{fid}", "gold", False,
     f"nerofactions:{fid}_armor_trim_smithing_template",
     rep(f"nerofactions:{fid}", 2500), f"Inner Circle: {name}",
     f"Reach the inner circle of the {name}.", "FACT-REP-01", "Seven Banners member")
    for fid, name in FACTIONS
] + [
    ("nerofactions", "voidtouched", "silver", True, "nerofactions:void_altar",
     placed("nerofactions:void_altar"), "Voidtouched", "Raise a Void Altar.", "FACT-ITEM-02",
     "hidden; never a collection member"),
    ("nerofactions", "rift_walker", "silver", True, "nerofactions:rift_blade",
     item("nerofactions:rift_blade"), "Rift Walker", "Get a Rift Blade.", "FACT-ITEM-01",
     "hidden; re-tiered gold to silver; never a collection member"),
    # --- NeroColonies --------------------------------------------------------------------------
    ("nerocolonies", "founder", "bronze", False, "nerocolonies:colony_beacon",
     gate("nerocolonies:established"), "Founder", "Found a colony.", "COL-GATE-01", ""),
    ("nerocolonies", "settled", "bronze", False, "nerocolonies:habitat_pod",
     gate("nerocolonies:stage_settled"), "Settled", "Grow a colony to Settled.", "COL-GATE-02", ""),
    ("nerocolonies", "breathing_room", "bronze", False, "nerocolonies:oxygen_generator",
     gate("nerocolonies:life_support_online"), "Breathing Room", "Bring a colony's life support online.",
     "COL-GATE-07", ""),
    ("nerocolonies", "first_research", "bronze", False, "nerocolonies:research_station",
     gate("nerocolonies:first_research"), "Lab Notes", "Unlock a colony research node.",
     "COL-GATE-09", ""),
    ("nerocolonies", "thank_you", "bronze", True, "nerocolonies:thank_you_note",
     item("nerocolonies:thank_you_note"), "Thank You!", "Receive a thank-you note from a Neran.",
     "COL-ITEM-02", "hidden; never a collection member"),
    ("nerocolonies", "thriving", "silver", False, "nerocolonies:farm_station",
     gate("nerocolonies:stage_thriving"), "Thriving", "Grow a colony to Thriving.", "COL-GATE-04", ""),
    ("nerocolonies", "frontier", "silver", False, "nerocolonies:outpost_beacon",
     gate("nerocolonies:first_outpost"), "Frontier", "Found an outpost.", "COL-GATE-11", ""),
    ("nerocolonies", "self_sufficient", "gold", False, "nerocolonies:hydroponics_station",
     gate("nerocolonies:self_sufficient"), "Self-Sufficient",
     "Keep a colony housed, fed, breathing and working all at once.", "COL-GATE-06", ""),
    ("nerocolonies", "metropolis", "nero", False, "nerocolonies:colony_planner",
     gate("nerocolonies:stage_metropolis"), "Metropolis", "Grow a colony to Metropolis, its final stage.",
     "COL-GATE-05", "nero: the final colony stage"),
    # --- NeroSecurity --------------------------------------------------------------------------
    ("nerosecurity", "access_granted", "bronze", False, "nerosecurity:keycard",
     gate("nerosecurity:first_keycard"), "Access Granted", "Program your first Keycard.", "SEC-GATE-01", ""),
    ("nerosecurity", "eyes_everywhere", "bronze", False, "nerosecurity:security_camera",
     placed("nerosecurity:security_camera"), "Eyes Everywhere", "Place a Security Camera.", "SEC-ITEM-01", ""),
    ("nerosecurity", "shields_up", "silver", False, "nerosecurity:force_field_emitter",
     placed("nerosecurity:force_field_emitter"), "Shields Up", "Place a Force Field Emitter.",
     "SEC-ITEM-01", ""),
    ("nerosecurity", "encrypted", "silver", True, "nerosecurity:encryption_module",
     item("nerosecurity:encryption_module"), "Encrypted",
     "Get an Encryption Module, the key to cards nobody else can copy.", "SEC-ITEM-02",
     "hidden; re-pointed to the real item (an encrypted keycard is a component, not an item id); "
     "never a collection member"),
    ("nerosecurity", "installation_online", "silver", False, "nerosecurity:security_hub",
     gate("nerosecurity:installation_online"), "Installation Online", "Bring a security installation online.",
     "SEC-GATE-02", ""),
    ("nerosecurity", "drill_sergeant", "silver", False, "nerosecurity:alarm",
     gate("nerosecurity:lockdown_drill"), "Drill Sergeant", "Run a lockdown.", "SEC-GATE-03",
     "re-tiered gold to silver"),
    # --- NeroEvents ----------------------------------------------------------------------------
    ("neroevents", "first_responder", "bronze", False, "neroevents:event_console",
     gate("neroevents:first_participation"), "First Responder", "Take part in a world event.",
     "EVT-GATE-01", ""),
    ("neroevents", "care_package", "bronze", False, "neroevents:supply_beacon",
     gate("neroevents:supply_claimed"), "Care Package", "Claim a supply drop.", "EVT-GATE-02", ""),
    ("neroevents", "stargazer", "bronze", False, "neroevents:embedded_meteor_ore",
     stat("minecraft:mined", "neroevents:embedded_meteor_ore", 1), "Stargazer", "Mine ore from a fallen meteor.",
     "EVT-ITEM-01", "mined stat; the ore drops dust, never itself"),
    ("neroevents", "salvage_rights", "silver", True, "neroevents:salvage_core_fragment",
     item("neroevents:salvage_core_fragment"), "Salvage Rights", "Recover a Salvage Core Fragment from a crash.",
     "EVT-ITEM-01", "hidden; never a collection member"),
    ("neroevents", "raid_breaker", "silver", False, "neroevents:war_beacon",
     gate("neroevents:raid_repelled"), "Raid Breaker", "Help repel a raid.", "EVT-GATE-03", ""),
    ("neroevents", "giant_slayer", "gold", False, "neroevents:reward_crate_token",
     gate("neroevents:boss_contributor"), "Giant Slayer", "Pull your weight in a world boss fight.",
     "EVT-GATE-04", "re-tiered nero to gold"),
    # --- NeroQuests (thin: floor waived) -------------------------------------------------------
    ("neroquests", "bookworm", "bronze", False, "neroquests:quest_book",
     item("neroquests:quest_book"), "Bookworm", "Get a Quest Book.", "QUEST-ITEM-01", ""),
    ("neroquests", "arcade_apprentice", "bronze", False, "neroarcade:blank_cartridge",
     gate("neroarcade:hook/blank_cartridges"), "Arcade Apprentice", "Finish the arcade's opening quest.",
     "ARCA-GATE-04", "allow-listed hook gate; Quests tab; needs NeroArcade; live only (the gate closes on delivery)",
     ["neroquests", "neroarcade"]),
    ("neroquests", "companion_keeper", "silver", False, "nerocompanion:companion_bay",
     gate("nerocompanion:hook/skin/aurora"), "Companion Keeper", "Finish the Companion Log quest chapter.",
     "COMP-GATE-13", "allow-listed hook gate; Quests tab; needs NeroCompanion; re-tiered gold to silver",
     ["neroquests", "nerocompanion"]),
]


def main():
    lang = {}
    rows = []
    order = {}
    for entry in T:
        mod, slug, tier, hidden, icon, trigger, title, desc, row, notes = entry[:10]
        requires = entry[10] if len(entry) > 10 else mod
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
            "requires_mod": requires,
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
