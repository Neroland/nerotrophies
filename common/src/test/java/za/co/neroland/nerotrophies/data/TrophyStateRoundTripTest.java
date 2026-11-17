package za.co.neroland.nerotrophies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/** The store survives a save and load unchanged, writes schema 1, rebuilds its token index and tolerates new keys. */
class TrophyStateRoundTripTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static TrophyState seeded() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("first"), 1_000L, 111L);
        s.award(A, TestDefs.id("second"), 2_000L, 222L);
        s.refreshName(A, "Alice", 3_000L);
        s.setShowName(A, false);
        s.award(A, TestDefs.id("trim/laurel_gold"), 4_000L, TrophyStamp.UNSTAMPED);
        s.completeCollection(A, TestDefs.id("all"));
        s.markPaid(A, TestDefs.id("first"));
        s.markClaimed(A, TestDefs.id("first"), 5_000L);
        s.replaceToken(A, TestDefs.id("first"), 333L, 6_000L);
        s.award(B, TestDefs.id("first"), 7_000L, 444L);
        s.setAnnounce(B, false);
        return s;
    }

    @Test
    void jsonRoundTripKeepsEverything() {
        TrophyState original = seeded();
        JsonElement json = TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(1, json.getAsJsonObject().get("schema").getAsInt());
        TrophyState back = TrophyState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(json, TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, back).getOrThrow());

        TrophyRecord a = back.record(A).orElseThrow();
        assertEquals("Alice", a.name().orElseThrow());
        assertFalse(a.showName());
        assertTrue(a.announce());
        assertEquals(333L, a.earned(TestDefs.id("first")).orElseThrow().token());
        assertTrue(a.earned(TestDefs.id("first")).orElseThrow().claimed());
        assertTrue(a.has(TestDefs.id("trim/laurel_gold")));
        assertTrue(a.completedCollections().contains(TestDefs.id("all")));
        assertTrue(a.paid(TestDefs.id("first")));
        assertFalse(back.record(B).orElseThrow().announce());
        assertEquals(1, back.retiredCount());
        assertEquals(TestDefs.id("first"), back.retiredTrophy(111L).orElseThrow());
    }

    @Test
    void nbtRoundTripKeepsEverything() {
        TrophyState original = seeded();
        Tag tag = TrophyState.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        TrophyState back = TrophyState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        assertEquals(tag, TrophyState.CODEC.encodeStart(NbtOps.INSTANCE, back).getOrThrow());
    }

    @Test
    void tokenIndexIsRebuiltOnLoad() {
        TrophyState back = TrophyState.CODEC.parse(JsonOps.INSTANCE,
                TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, seeded()).getOrThrow()).getOrThrow();
        assertEquals(A, back.owner(333L).orElseThrow().player());
        assertEquals(B, back.owner(444L).orElseThrow().player());
        assertTrue(back.owner(111L).isEmpty(), "a retired token is not live");
        assertTrue(back.owner(TrophyStamp.UNSTAMPED).isEmpty(), "trims carry no live token");
        assertEquals(3, back.indexedTokenCount());
    }

    @Test
    void missingSchemaDefaultsAndUnknownKeysAreIgnored() {
        String json = "{\"future_field\":{\"x\":1},\"players\":[{\"id\":\"" + A + "\",\"later\":true,"
                + "\"earned\":[{\"trophy\":\"nerotrophies:first\",\"at\":5,\"token\":77,\"extra\":\"y\"}]}]}";
        TrophyState back = TrophyState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(TrophyState.SCHEMA, back.loadedSchema());
        assertEquals(A, back.owner(77L).orElseThrow().player());
        assertTrue(back.record(A).orElseThrow().showName(), "toggles default on");
    }

    @Test
    void duplicateTokensKeepTheNewestAndRetireTheRest() {
        String json = "{\"schema\":1,\"players\":["
                + "{\"id\":\"" + A + "\",\"earned\":[{\"trophy\":\"nerotrophies:x\",\"at\":1,\"token\":9}]},"
                + "{\"id\":\"" + B + "\",\"earned\":[{\"trophy\":\"nerotrophies:x\",\"at\":2,\"token\":9}]}]}";
        TrophyState back = TrophyState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(B, back.owner(9L).orElseThrow().player());
        assertEquals(1, back.duplicatesRetiredOnLoad());
        assertEquals(TrophyStamp.UNSTAMPED, back.record(A).orElseThrow().earned(TestDefs.id("x")).orElseThrow().token());
    }

    @Test
    void namesAreSanitisedAndKeptOnlyForPlayersWithTrophies() {
        TrophyState s = new TrophyState();
        s.setAnnounce(A, false);
        assertFalse(s.refreshName(A, "Alice", 1L), "no name for a toggle-only record");
        s.award(A, TestDefs.id("t"), 1L, 5L);
        assertTrue(s.refreshName(A, "Al\u00a7ice\u0000", 2L));
        assertEquals("Alice", s.record(A).orElseThrow().name().orElseThrow());
        assertEquals(TrophyRecord.NAME_MAX, TrophyRecord.sanitiseName("x".repeat(80)).length());
    }
}
