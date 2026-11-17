package za.co.neroland.nerotrophies.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;
import za.co.neroland.nerotrophies.network.RecordBuilder;

/** The link module reads the requester's own record only and never leaks a hidden trophy it has not earned. */
class TrophyLinkSnapshotsTest {

    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final LinkText TEXT = (key, fallback) -> fallback;

    static TrophyDefinition hidden(String path) {
        return TrophyDefinition.of(TestDefs.id(path), new TrophyDefinition.Data(
                Identifier.withDefaultNamespace("dragon_egg"), TrophyTier.NERO, Optional.empty(), true,
                Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), 0, 1));
    }

    static final TrophyDefinitions.Snapshot DEFS = TestDefs.snapshot(
            List.of(TestDefs.trophy("one", TrophyTier.GOLD), TestDefs.trophy("two", TrophyTier.BRONZE),
                    hidden("secret_vault")),
            List.of(TestDefs.collection("pair", "laurel", TestDefs.id("one"), TestDefs.id("two"))), 1L);

    private static TrophyState state() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1_000L, 101L);
        s.award(A, TestDefs.id("two"), 2_000L, 102L);
        s.award(A, TestDefs.id("secret_vault"), 3_000L, 103L);
        s.completeCollection(A, TestDefs.id("pair"));
        s.refreshName(A, "Alice", 4_000L);
        return s;
    }

    private static TrophyLinkSnapshots provider(TrophyState s) {
        TrophyLinkSnapshots.View view = new TrophyLinkSnapshots.View() {
            @Override
            public TrophyState state() {
                return s;
            }

            @Override
            public TrophyDefinitions.Snapshot definitions() {
                return DEFS;
            }

            @Override
            public RecordBuilder.ProgressSource progress(UUID player) {
                return d -> Optional.of(new TriggerEvaluator.Progress(1, 4));
            }

            @Override
            public LinkText text() {
                return TEXT;
            }

            @Override
            public long nowMillis() {
                return 9_999L;
            }
        };
        return new TrophyLinkSnapshots(() -> view);
    }

    @Test
    void aSecondPlayerReadsNothingOfTheFirsts() {
        TrophyLinkSnapshots p = provider(state());
        for (String section : TrophyLinkModule.SECTIONS) {
            String text = p.snapshot(B, section, Map.of()).toString();
            assertFalse(text.contains("\"earned\":true"), section + ": B earned nothing");
            assertFalse(text.contains("\"completed\":true"), section);
            assertFalse(text.contains("Alice") || text.contains(A.toString()) || text.contains(B.toString()),
                    section + ": no names or UUIDs");
            assertFalse(java.util.regex.Pattern.compile("[:\\[,][123]000[,}\\]]").matcher(text).find(),
                    section + ": none of A's award times");
            assertFalse(text.contains("secret_vault"), section + ": A earned the hidden one, B did not");
        }
        JsonObject summary = p.snapshot(B, TrophyLinkModule.SECTION_SUMMARY, Map.of());
        assertEquals(0, summary.get("earned").getAsInt());
        assertEquals(3, summary.get("total").getAsInt());
        assertFalse(summary.has("lastEarnedAt"));
    }

    @Test
    void theOwnerSeesTheirOwnRecordWithoutNameOrTokens() {
        TrophyLinkSnapshots p = provider(state());
        JsonObject trophies = p.snapshot(A, TrophyLinkModule.SECTION_TROPHIES, Map.of());
        JsonArray list = trophies.getAsJsonArray("trophies");
        assertEquals(3, list.size());
        for (JsonElement e : list) {
            assertTrue(e.getAsJsonObject().get("earned").getAsBoolean());
            assertNotNull(e.getAsJsonObject().get("at"));
            assertTrue(TrophyLinkKeys.KEY.matcher(e.getAsJsonObject().get("key").getAsString()).matches());
        }
        JsonObject summary = p.snapshot(A, TrophyLinkModule.SECTION_SUMMARY, Map.of());
        assertEquals(3, summary.get("earned").getAsInt());
        assertEquals(1, summary.getAsJsonObject("collections").get("completed").getAsInt());
        assertEquals(1, summary.getAsJsonObject("tiers").getAsJsonObject("gold").get("earned").getAsInt());
        assertEquals(3_000L, summary.get("lastEarnedAt").getAsLong());
        for (String section : TrophyLinkModule.SECTIONS) {
            String text = p.snapshot(A, section, Map.of()).toString();
            assertFalse(text.contains("Alice") || text.contains(A.toString()), section + ": no gamertag, no UUID");
            assertFalse(java.util.regex.Pattern.compile("[:\\[,]10[123][,}\\]]").matcher(text).find(),
                    section + ": no tokens");
        }
    }

    @Test
    void hiddenUnearnedNeverLeaksDetails() {
        TrophyState s = new TrophyState();
        s.award(B, TestDefs.id("one"), 5_000L, 201L);
        JsonObject trophies = TrophyLinkSnapshots.trophies(s.record(B).orElse(null), DEFS, TEXT,
                d -> Optional.of(new TriggerEvaluator.Progress(2, 9)), 1L);
        JsonObject masked = null;
        for (JsonElement e : trophies.getAsJsonArray("trophies")) {
            if (e.getAsJsonObject().get("hidden").getAsBoolean()) {
                masked = e.getAsJsonObject();
            }
        }
        assertNotNull(masked);
        assertEquals("???", masked.get("title").getAsString());
        assertEquals(java.util.Set.of("key", "mod", "hidden", "earned", "title"), masked.keySet());
        assertTrue(masked.get("key").getAsString().startsWith(TrophyLinkKeys.HIDDEN_PREFIX));
        String text = trophies.toString();
        assertFalse(text.contains("secret") || text.contains("dragon_egg") || text.contains("nero\""),
                "no id, icon or tier of the hidden trophy");
        assertFalse(text.contains(TrophyLinkKeys.trophy(TestDefs.id("secret_vault"))), "no hash handle either");
        // unearned visible trophies get progress, earned ones do not
        for (JsonElement e : trophies.getAsJsonArray("trophies")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("id") && o.get("id").getAsString().endsWith("two")) {
                assertEquals(2, o.getAsJsonObject("progress").get("current").getAsInt());
            }
            if (o.has("id") && o.get("id").getAsString().endsWith("one")) {
                assertFalse(o.has("progress"));
            }
        }
    }

    @Test
    void hiddenCollectionMemberIsMasked() {
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(
                List.of(TestDefs.trophy("one", TrophyTier.GOLD), hidden("secret_vault")),
                List.of(TestDefs.collection("odd", "standard", TestDefs.id("one"), TestDefs.id("secret_vault"))), 1L);
        String text = TrophyLinkSnapshots.collections(null, defs, TEXT, 1L).toString();
        assertFalse(text.contains(TrophyLinkKeys.trophy(TestDefs.id("secret_vault"))));
        assertTrue(text.contains("\"memberCount\":2"));
    }

    @Test
    void unknownSectionAndNoServerAreErrors() {
        assertEquals("UNKNOWN_SECTION",
                provider(state()).snapshot(A, "leaderboard", Map.of()).get("error").getAsString());
        assertEquals("UNAVAILABLE",
                new TrophyLinkSnapshots(() -> null).snapshot(A, "trophies", Map.of()).get("error").getAsString());
    }
}
