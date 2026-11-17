"""Unit tests for check_assets.py. Run from tools/: python3 -m unittest test_check_assets -v"""

import contextlib
import io
import json
import os
import shutil
import struct
import tempfile
import unittest

import check_assets as c

A = c.ASSETS
REG = c.REGISTRY

TROPHY_IDS = """
public final class TrophyIds {
    public static final String LAMP = "lamp";
    public static final String BOOK = "book";
    public static final String STANDARD_TRIM = "standard";
}
"""
MOD_BLOCKS = """
public final class ModBlocks {
    // BLOCKS.register("commented_out", key -> null);
    public static final RegistryEntry<Block> LAMP = BLOCKS.register(TrophyIds.LAMP, key -> new Block(p));
}
"""
MOD_ITEMS = """
public final class ModItems {
    public static final RegistryEntry<Item> LAMP = block(TrophyIds.LAMP, ModBlocks.LAMP);
    public static final RegistryEntry<Item> BOOK = ITEMS.register(TrophyIds.BOOK, key -> new Item(p));
    private static RegistryEntry<Item> block(String id, Supplier<Block> block) { return null; }
}
"""
MENUS = """
public final class ModMenuTypes {
    public static final RegistryEntry<MenuType<M>> LAMP = MENUS.register(TrophyIds.LAMP, key -> null);
}
"""
SOUNDS_JAVA = """
public final class Sounds {
    public static final SoundEvent CLICK = event("block.lamp.click");
    private static SoundEvent event(String path) { return SoundEvent.createVariableRangeEvent(TrophyIds.id(path)); }
    void tip() {
        Component.translatable("screen.nerotrophies.title");
        Component.translatable(on ? "screen.nerotrophies.on" : "screen.nerotrophies.off", "not.a.key");
        Component.translatable("nerotrophies.tier." + tier);
        Component.translatable("gui.done");
    }
}
"""
PARTICLES_JAVA = """
public final class ModParticles {
    public static final RegistryEntry<ParticleType<?>> SPARK = Registries.PARTICLE_TYPE.register("spark", x);
}
"""
TRIMS_JAVA = """
public final class Trims {
    public static final List<Trim> ALL = List.of(new Trim(TrophyIds.STANDARD_TRIM, S, 0), new Trim("gold", S, 1));
}
"""


def png(w, h):
    return b"\x89PNG\r\n\x1a\n" + struct.pack(">I", 13) + b"IHDR" + struct.pack(">II", w, h) + b"\x08\x06\x00\x00\x00"


def ogg(channels, rate):
    return b"OggS" + b"\x00" * 24 + b"\x01vorbis" + struct.pack("<I", 0) + bytes([channels]) + struct.pack("<I", rate) + b"\x00" * 16


class Fixture:
    """A throwaway repository with one block (+ its item), one plain item, a menu, a sound and a particle."""

    def __init__(self, tmp):
        self.parent = tmp
        self.repo = os.path.join(tmp, "nerotrophies")
        self.java(REG + "/TrophyIds.java", TROPHY_IDS)
        self.java(REG + "/ModBlocks.java", MOD_BLOCKS)
        self.java(REG + "/ModItems.java", MOD_ITEMS)
        self.java(REG + "/ModMenuTypes.java", MENUS)
        self.java("common/src/main/java/za/co/neroland/nerotrophies/block/Sounds.java", SOUNDS_JAVA)
        self.java("common/src/main/java/za/co/neroland/nerotrophies/client/ModParticles.java", PARTICLES_JAVA)
        self.java(c.TRIMS, TRIMS_JAVA)
        os.makedirs(os.path.join(self.repo, "neoforge/versions/1.21.1"))
        os.makedirs(os.path.join(self.repo, "neoforge/versions/26.3"))
        self.json(A + "/blockstates/lamp.json", {"variants": {"": [{"model": "nerotrophies:block/lamp"}]}})
        self.json(A + "/models/block/lamp_base.json", {
            "parent": "minecraft:block/block",
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#side"}}}]})
        self.json(A + "/models/block/lamp.json", {
            "parent": "nerotrophies:block/lamp_base",
            "textures": {"particle": "#side", "side": "nerotrophies:block/lamp"}})
        self.json(A + "/models/item/lamp.json", {"parent": "nerotrophies:block/lamp"})
        self.json(A + "/models/item/book.json", {"parent": "minecraft:item/generated",
                                                  "textures": {"layer0": "nerotrophies:item/book"}})
        self.json(A + "/items/lamp.json", {"model": {"type": "minecraft:model", "model": "nerotrophies:item/lamp"}})
        self.json(A + "/items/book.json", {"model": {"type": "minecraft:model", "model": "nerotrophies:item/book"}})
        self.raw(A + "/textures/block/lamp.png", png(16, 16))
        self.raw(A + "/textures/item/book.png", png(16, 16))
        self.raw(A + "/textures/particle/spark.png", png(8, 8))
        self.json(A + "/particles/spark.json", {"textures": ["nerotrophies:spark"]})
        self.json(A + "/sounds.json", {"block.lamp.click": {
            "subtitle": "subtitles.nerotrophies.block.lamp.click", "sounds": ["nerotrophies:block/click"]}})
        self.raw(A + "/sounds/block/click.ogg", ogg(1, 32000))
        self.lang = {
            "block.nerotrophies.lamp": "Lamp", "item.nerotrophies.book": "Book", "itemGroup.nerotrophies": "NT",
            "container.nerotrophies.lamp": "Lamp", "trim.nerotrophies.standard": "Standard",
            "trim.nerotrophies.gold": "Gold", "subtitles.nerotrophies.block.lamp.click": "Click",
            "screen.nerotrophies.title": "T", "screen.nerotrophies.on": "On", "screen.nerotrophies.off": "Off"}
        self.write_lang()
        self.checklist = [("A/textures/block/lamp.png", (16, 16)), ("A/sounds/block/click.ogg", "ogg"),
                          ("A/particles/spark.json", None)]

    def path(self, rel):
        p = os.path.join(self.repo, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        return p

    def java(self, rel, src):
        with open(self.path(rel), "w", encoding="utf-8") as f:
            f.write(src)

    def json(self, rel, data):
        with open(self.path(rel), "w", encoding="utf-8") as f:
            json.dump(data, f)

    def raw(self, rel, data):
        with open(self.path(rel), "wb") as f:
            f.write(data)

    def remove(self, rel):
        os.remove(os.path.join(self.repo, rel))

    def write_lang(self):
        self.json(A + "/lang/en_us.json", self.lang)

    def run(self, siblings=False, app_dir=None):
        return c.Checker(self.repo, siblings=siblings, checklist=self.checklist, app_dir=app_dir).run()


class CheckAssetsTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.fx = Fixture(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def assertError(self, rep, fragment):
        self.assertTrue(any(fragment in e for e in rep.errors), f"no error with {fragment!r} in {rep.errors}")

    def assertWarning(self, rep, fragment):
        self.assertTrue(any(fragment in w for w in rep.warnings), f"no warning with {fragment!r} in {rep.warnings}")

    def test_clean_fixture_passes(self):
        rep = self.fx.run()
        self.assertEqual(rep.errors, [])
        self.assertEqual(rep.warnings, [])

    def test_registry_parsing(self):
        k = c.Checker(self.fx.repo, siblings=False, checklist=[])
        k.read_registries()
        self.assertEqual(k.blocks, ["lamp"])
        self.assertEqual(k.items, ["lamp", "book"])
        self.assertEqual(k.menus, ["lamp"])
        self.assertEqual(k.sounds, ["block.lamp.click"])
        self.assertEqual(k.particles, ["spark"])
        self.assertEqual(k.trims, ["standard", "gold"])

    def test_missing_blockstate(self):
        self.fx.remove(A + "/blockstates/lamp.json")
        self.assertError(self.fx.run(), "blockstate missing: " + A + "/blockstates/lamp.json")

    def test_missing_model_and_parent(self):
        self.fx.remove(A + "/models/block/lamp_base.json")
        self.assertError(self.fx.run(), "model nerotrophies:block/lamp_base missing")

    def test_missing_texture(self):
        self.fx.remove(A + "/textures/block/lamp.png")
        self.assertError(self.fx.run(), "texture nerotrophies:block/lamp missing")

    def test_unresolved_face_variable(self):
        self.fx.json(A + "/models/block/lamp.json", {"parent": "nerotrophies:block/lamp_base",
                                                    "textures": {"top": "nerotrophies:block/lamp"}})
        self.assertError(self.fx.run(), "face texture #side")

    def test_vanilla_parent_and_texture_allowed(self):
        self.fx.json(A + "/models/block/lamp.json", {"parent": "minecraft:block/cube_all",
                                                    "textures": {"all": "minecraft:block/stone"}})
        rep = self.fx.run()
        self.assertEqual(rep.errors, [])

    def test_missing_item_definition(self):
        self.fx.remove(A + "/items/book.json")
        self.assertError(self.fx.run(), "item-model definition missing: " + A + "/items/book.json")

    def test_legacy_item_model_required_only_with_old_nodes(self):
        self.fx.remove(A + "/models/item/book.json")
        self.assertError(self.fx.run(), "legacy item model")
        os.rmdir(os.path.join(self.fx.repo, "neoforge/versions/1.21.1"))
        rep = self.fx.run()
        self.assertFalse(any("legacy item model" in e for e in rep.errors))

    def test_missing_lang_keys(self):
        for key in ("block.nerotrophies.lamp", "item.nerotrophies.book", "itemGroup.nerotrophies",
                    "subtitles.nerotrophies.block.lamp.click", "screen.nerotrophies.off", "trim.nerotrophies.gold"):
            del self.fx.lang[key]
        self.fx.write_lang()
        rep = self.fx.run()
        for key in ("block.nerotrophies.lamp", "item.nerotrophies.book", "itemGroup.nerotrophies",
                    "subtitles.nerotrophies.block.lamp.click", "screen.nerotrophies.off", "trim.nerotrophies.gold"):
            self.assertError(rep, "missing " + key + " ")
        self.assertFalse(any("item.nerotrophies.lamp" in e for e in rep.errors))
        self.assertFalse(any("gui.done" in e or "not.a.key" in e or "tier." in e for e in rep.errors))

    def test_pending_lang_key_is_a_warning(self):
        del self.fx.lang["screen.nerotrophies.title"]
        self.fx.write_lang()
        self.fx.json("tools/lang/pending/screen.json", {"screen.nerotrophies.title": "T"})
        rep = self.fx.run()
        self.assertEqual(rep.errors, [])
        self.assertWarning(rep, "screen.nerotrophies.title")

    def test_missing_sound_file_and_undefined_event(self):
        self.fx.remove(A + "/sounds/block/click.ogg")
        self.fx.json(A + "/sounds.json", {"block.lamp.click": {
            "subtitle": "subtitles.nerotrophies.block.lamp.click", "sounds": [{"name": "nerotrophies:block/clack"}]}})
        self.assertError(self.fx.run(), A + "/sounds/block/clack.ogg")
        self.fx.json(A + "/sounds.json", {})
        self.assertError(self.fx.run(), "created in Java but not defined")

    def test_particle_texture_and_registration(self):
        self.fx.remove(A + "/textures/particle/spark.png")
        self.assertError(self.fx.run(), A + "/textures/particle/spark.png")
        self.fx.json(A + "/particles/glow.json", {"textures": ["nerotrophies:spark"]})
        self.assertWarning(self.fx.run(), "nerotrophies:glow is registered")
        self.fx.remove(A + "/particles/spark.json")
        self.assertError(self.fx.run(), "definition missing: " + A + "/particles/spark.json")

    def test_checklist_missing_and_wrong_size(self):
        self.fx.checklist.append(("A/textures/gui/book.png", (256, 256)))
        self.assertError(self.fx.run(), A + "/textures/gui/book.png: checklist file missing")
        self.fx.raw(A + "/textures/gui/book.png", png(128, 128))
        self.assertError(self.fx.run(), "is 128x128, checklist wants 256x256")

    def test_checklist_ogg_format(self):
        self.fx.raw(A + "/sounds/block/click.ogg", ogg(2, 44100))
        self.assertError(self.fx.run(), "checklist wants mono 32 kHz")

    def test_app_copy_checked_only_with_an_app_dir(self):
        self.fx.checklist.append(("APP/assets/trophies/book.png", (16, 16)))
        app = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, app, True)
        self.assertEqual(self.fx.run(siblings=False, app_dir=app).errors, [])
        self.assertEqual(self.fx.run(siblings=True).errors, [], "no app dir given: the app check is skipped")
        self.assertError(self.fx.run(siblings=True, app_dir=app), "<app>/assets/trophies/book.png")
        os.makedirs(os.path.join(app, "assets", "trophies"))
        with open(os.path.join(app, "assets", "trophies", "book.png"), "wb") as f:
            f.write(png(16, 16))
        self.assertEqual(self.fx.run(siblings=True, app_dir=app).errors, [])

    def test_orphans_warn(self):
        self.fx.raw(A + "/textures/block/unused.png", png(16, 16))
        self.fx.json(A + "/models/block/unused.json", {"parent": "minecraft:block/block"})
        rep = self.fx.run()
        self.assertEqual(rep.errors, [])
        self.assertWarning(rep, A + "/textures/block/unused.png")
        self.assertWarning(rep, A + "/models/block/unused.json")

    def test_java_texture_reference_is_not_orphan(self):
        self.fx.raw(A + "/textures/gui/hud.png", png(16, 16))
        self.fx.java("common/src/main/java/za/co/neroland/nerotrophies/client/Hud.java",
                     'class Hud { static final Object T = TrophyIds.id("textures/gui/hud.png"); }')
        self.assertEqual(self.fx.run().warnings, [])

    def test_animated_mcmeta(self):
        self.fx.raw(A + "/textures/block/lamp.png", png(16, 40))
        self.fx.json(A + "/textures/block/lamp.png.mcmeta", {"animation": {"frametime": 5}})
        self.fx.checklist = []
        self.assertError(self.fx.run(), "height is not a multiple of width")

    def test_main_exit_codes(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            self.assertEqual(c.main(["--repo", self.fx.repo, "--no-siblings"]), 1)  # real checklist files absent
        self.assertIn("checklist file missing", out.getvalue())


if __name__ == "__main__":
    unittest.main()
