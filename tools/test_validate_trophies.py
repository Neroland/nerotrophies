import json
import os
import tempfile
import unittest

import validate_trophies as v

RES = "common/src/main/resources"
DATA = RES + "/data/nerotrophies"


def write(root, rel, data):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f)


def trophy(icon="minecraft:stone", tier="bronze", trigger=None, **extra):
    data = {"schema": 1, "icon": icon, "tier": tier}
    if trigger is not None:
        data["trigger"] = trigger
    data.update(extra)
    return data


def adv(path):
    return {"type": "advancement", "advancement": "minecraft:" + path}


class Fixture:
    """A throwaway repository (and sibling folder) holding a tiny valid catalogue."""

    def __init__(self, tmp):
        self.parent = tmp
        self.repo = os.path.join(tmp, "nerotrophies")
        self.lang = {}
        self.citations = {}
        self.add_trophy("minecraft/a", trophy(trigger=adv("story/a")))
        self.add_trophy("minecraft/b", trophy(trigger=adv("story/b")))
        self.add_collection("pair", {"icon": "minecraft:stone", "trim": "laurel_gold",
                                     "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/b"]})

    def add_trophy(self, path, data):
        write(self.repo, f"{DATA}/neroland_trophies/{path}.json", data)
        ident = "nerotrophies:" + path
        self.lang[v.default_key("trophy", ident, "title")] = "T"
        self.lang[v.default_key("trophy", ident, "desc")] = "D"
        self.citations[ident] = "TEST-ROW-01"

    def add_collection(self, path, data):
        write(self.repo, f"{DATA}/neroland_trophy_collections/{path}.json", data)
        ident = "nerotrophies:" + path
        self.lang[v.default_key("collection", ident, "title")] = "T"
        self.lang[v.default_key("collection", ident, "desc")] = "D"

    def run(self, siblings=False):
        write(self.repo, "tools/lang/pending/test.json", self.lang)
        path = os.path.join(self.repo, "tools/catalogue/citations/test.csv")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write("id,tier,hidden,trigger_type,survey_row,notes\n")
            for ident, row in self.citations.items():
                f.write(f"{ident},bronze,false,x,{row},\n")
        rep, *_ = v.validate(self.repo, self.parent if siblings else None, [])
        return rep


class ValidatorTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.fx = Fixture(self._tmp.name)

    def tearDown(self):
        self._tmp.cleanup()

    def assertErrorContains(self, rep, text):
        self.assertTrue(any(text in e for e in rep.errors), f"no error containing {text!r}: {rep.errors}")

    def test_fixture_is_clean(self):
        rep = self.fx.run()
        self.assertEqual(rep.errors, [])

    def test_shipped_catalogue_is_clean_without_siblings(self):
        repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        rep, trophies, collections, _, _ = v.validate(repo, None, [])
        self.assertEqual(rep.errors, [])
        self.assertGreater(len(trophies), 0)
        self.assertGreater(len(collections), 0)

    def test_schema_errors(self):
        self.fx.add_trophy("minecraft/bad_tier", trophy(tier="platinum", trigger=adv("story/c")))
        self.fx.add_trophy("minecraft/threshold", trophy(trigger={"type": "nero_event", "event": "threshold"}))
        self.fx.add_trophy("minecraft/typo", trophy(trigger=adv("story/d"), requires_mods="x"))
        self.fx.add_trophy("minecraft/coins", trophy(trigger=adv("story/e"), reward={"coins": 10001}))
        self.fx.add_trophy("minecraft/fraction", trophy(trigger={"type": "stat", "stat": "minecraft:jump",
                                                                 "at_least": 1.5}))
        rep = self.fx.run()
        self.assertErrorContains(rep, "'tier' must be one of")
        self.assertErrorContains(rep, "'threshold' is reserved")
        self.assertErrorContains(rep, "unknown key 'requires_mods'")
        self.assertErrorContains(rep, "'reward' must be")
        self.assertErrorContains(rep, "stat 'at_least' must be a whole number")

    def test_missing_lang_key(self):
        self.fx.add_trophy("minecraft/c", trophy(trigger=adv("story/c")))
        del self.fx.lang["trophy.nerotrophies.minecraft.c.desc"]
        self.assertErrorContains(self.fx.run(), "trophy.nerotrophies.minecraft.c.desc is missing")

    def test_hidden_member(self):
        self.fx.add_trophy("minecraft/secret", trophy(trigger=adv("story/s"), hidden=True))
        self.fx.add_collection("leaky", {"icon": "minecraft:stone", "trim": "prism",
                                         "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/secret"]})
        self.assertErrorContains(self.fx.run(), "hidden trophies are never collection members")

    def test_collection_size_and_members(self):
        members = []
        for i in range(25):
            self.fx.add_trophy(f"minecraft/m{i}", trophy(trigger=adv(f"story/m{i}")))
            members.append(f"nerotrophies:minecraft/m{i}")
        self.fx.add_collection("big", {"icon": "minecraft:stone", "trim": "prism", "members": members})
        self.fx.add_collection("ghost", {"icon": "minecraft:stone", "trim": "nope",
                                         "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/zz"]})
        rep = self.fx.run()
        self.assertErrorContains(rep, "limit is 24")
        self.assertErrorContains(rep, "member nerotrophies:minecraft/zz is not a trophy")
        self.assertErrorContains(rep, "unknown trim 'nope'")

    def test_nero_rules(self):
        self.fx.add_trophy("minecraft/n1", trophy(tier="nero", trigger={"type": "item_obtained",
                                                                       "item": "minecraft:dragon_egg"}))
        self.fx.add_trophy("minecraft/n2", trophy(tier="nero", trigger=adv("end/n2")))
        rep = self.fx.run()
        self.assertErrorContains(rep, "nero tier on a polled 'item_obtained' trigger")
        self.assertErrorContains(rep, "second nero trophy in category 'nerotrophies'")

    def test_duplicate_trigger_and_grind(self):
        self.fx.add_trophy("minecraft/a_again", trophy(trigger=adv("story/a")))
        self.fx.add_trophy("minecraft/grind", trophy(trigger={"type": "block_placed", "block": "minecraft:stone",
                                                              "count": 10000}))
        self.fx.add_trophy("minecraft/afk", trophy(trigger={"type": "stat", "stat": "minecraft:play_time",
                                                            "at_least": 360000}))
        rep = self.fx.run()
        self.assertErrorContains(rep, "same trigger as nerotrophies:minecraft/a")
        self.assertErrorContains(rep, "repeated-use grind")
        self.assertErrorContains(rep, "rewards idle time")

    def test_completion_trophy_rules(self):
        self.fx.add_trophy("collection/crown", trophy(tier="nero", trigger=adv("story/crown"), requires_mod="other"))
        self.fx.add_collection("crowned", {"icon": "minecraft:stone", "trim": "prism", "requires_mod": "x",
                                           "trophy": "nerotrophies:collection/crown",
                                           "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/b"]})
        rep = self.fx.run()
        self.assertErrorContains(rep, "must have no trigger")
        self.assertErrorContains(rep, "requires_mod 'x' is not required by every member")
        self.assertErrorContains(rep, "completion trophy needs 'other'")

    def test_trigger_less_completion_trophy_is_fine(self):
        self.fx.add_trophy("collection/crown", trophy(tier="gold"))
        self.fx.add_collection("crowned", {"icon": "minecraft:stone", "trim": "prism",
                                           "trophy": "nerotrophies:collection/crown",
                                           "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/b"]})
        self.assertEqual(self.fx.run().errors, [])

    def test_id_clash(self):
        self.fx.add_trophy("pair", trophy(trigger=adv("story/pair")))
        self.assertErrorContains(self.fx.run(), "also a trophy id")

    def test_sibling_checks_only_when_present(self):
        gate = {"type": "nero_event", "event": "gate", "gate": "somemod:team_gate"}
        self.fx.add_trophy("somemod/g", trophy(icon="somemod:widget", trigger=gate, requires_mod="somemod"))
        self.fx.add_trophy("somemod/hook", trophy(icon="somemod:widget", requires_mod="somemod",
                                                  trigger={"type": "nero_event", "event": "gate",
                                                           "gate": "somemod:hook/free_stuff"}))
        rep = self.fx.run(siblings=True)
        self.assertErrorContains(rep, "only allow-listed hooks")
        self.assertNotIn("not PLAYER scope", " ".join(rep.errors))
        self.assertTrue(any("somemod not checked out" in n for n in rep.notes))

        sib = os.path.join(self.fx.parent, "somemod", RES)
        write(sib, "assets/somemod/lang/en_us.json", {"item.somemod.widget": "Widget"})
        write(sib, "data/somemod/neroland_gates/team_gate.json", {"scope": "team"})
        write(sib, "data/somemod/advancement/root.json", {})
        rep = self.fx.run(siblings=True)
        self.assertErrorContains(rep, "is not PLAYER scope")
        self.assertNotIn("is not an item", " ".join(rep.errors))

    def test_sibling_icon_and_overlay_advancement(self):
        self.fx.add_trophy("somemod/new", trophy(icon="somemod:missing", requires_mod="somemod",
                                                 trigger={"type": "advancement", "advancement": "somemod:new"}))
        sib = os.path.join(self.fx.parent, "somemod", "common/src/main")
        write(sib, "resources-26.3/data/somemod/advancement/new.json", {})
        write(sib, "resources/assets/somemod/lang/en_us.json", {})
        rep = self.fx.run(siblings=True)
        self.assertErrorContains(rep, "is not an item in ../somemod")
        self.assertTrue(any("only exists on MC >= 26.3" in w for w in rep.warnings))

    def test_unobtainable_block_warns(self):
        placed = {"type": "block_placed", "block": "somemod:ore"}
        self.fx.add_trophy("somemod/ore", trophy(icon="somemod:ore", trigger=placed, requires_mod="somemod"))
        sib = os.path.join(self.fx.parent, "somemod", RES)
        write(sib, "assets/somemod/lang/en_us.json", {"block.somemod.ore": "Ore"})
        write(sib, "data/somemod/loot_table/blocks/ore.json", {"pools": [{"entries": [
            {"type": "minecraft:item", "name": "somemod:dust"}]}]})
        rep = self.fx.run(siblings=True)
        self.assertEqual(rep.errors, [])
        self.assertTrue(any("may never reach a survival inventory" in w for w in rep.warnings), rep.warnings)

        write(sib, "data/somemod/loot_table/blocks/ore.json", {"pools": [{"entries": [{"type": "minecraft:alternatives",
            "children": [{"type": "minecraft:item", "name": "somemod:ore"}]}]}]})
        rep = self.fx.run(siblings=True)
        self.assertFalse(any("survival inventory" in w for w in rep.warnings), rep.warnings)

    def test_recipe_makes_item_obtainable(self):
        got = {"type": "item_obtained", "item": "somemod:gizmo"}
        self.fx.add_trophy("somemod/gizmo", trophy(icon="somemod:gizmo", trigger=got, requires_mod="somemod"))
        sib = os.path.join(self.fx.parent, "somemod", RES)
        write(sib, "assets/somemod/lang/en_us.json", {"item.somemod.gizmo": "Gizmo"})
        rep = self.fx.run(siblings=True)
        self.assertTrue(any("may never reach a survival inventory" in w for w in rep.warnings))
        write(sib, "data/somemod/recipe/gizmo.json", {"type": "minecraft:crafting_shapeless",
                                                      "result": {"id": "somemod:gizmo", "count": 1}})
        rep = self.fx.run(siblings=True)
        self.assertFalse(any("survival inventory" in w for w in rep.warnings), rep.warnings)

    def test_citation_rows(self):
        self.fx.add_trophy("minecraft/c", trophy(trigger=adv("story/c")))
        del self.fx.citations["nerotrophies:minecraft/c"]
        self.fx.citations["nerotrophies:minecraft/gone"] = "TEST-ROW-02"
        rep = self.fx.run()
        self.assertErrorContains(rep, "nerotrophies:minecraft/c: no citation row")
        self.assertErrorContains(rep, "names no existing trophy: nerotrophies:minecraft/gone")

    def test_citation_cites_a_survey_row(self):
        self.fx.citations["nerotrophies:minecraft/a"] = "see notes"
        self.fx.add_trophy("collection/crown", trophy(tier="gold"))
        self.fx.citations["nerotrophies:collection/crown"] = "crowned"
        self.fx.add_collection("crowned", {"icon": "minecraft:stone", "trim": "prism",
                                           "trophy": "nerotrophies:collection/crown",
                                           "members": ["nerotrophies:minecraft/a", "nerotrophies:minecraft/b"]})
        rep = self.fx.run()
        self.assertErrorContains(rep, "does not cite a survey row id (got 'see notes')")
        self.assertNotIn("collection/crown", " ".join(rep.errors))

    def test_quest_pack_keys(self):
        write(self.fx.repo, f"{DATA}/neroquests/chapters/hall.json",
              {"title": "chapter.nerotrophies.hall.title", "quests": [{"quest": "nerotrophies:hall/q"}]})
        rep = self.fx.run()
        self.assertErrorContains(rep, "chapter.nerotrophies.hall.title is missing")
        self.assertErrorContains(rep, "entry nerotrophies:hall/q is not a quest")


if __name__ == "__main__":
    unittest.main()
