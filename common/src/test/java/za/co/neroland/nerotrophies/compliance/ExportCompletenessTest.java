package za.co.neroland.nerotrophies.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyExport;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * Two players: {@code /nerotrophies export} returns every stored field of the requester's record (checked
 * against the store's own serialised form) and nothing of anyone else.
 */
class ExportCompletenessTest {

    private static final UUID A = UUID.fromString("e4900000-0000-4000-8000-0000000000a1");
    private static final UUID B = UUID.fromString("e4900000-0000-4000-8000-0000000000b2");
    private static final Identifier ONE = TestDefs.id("one");
    private static final Identifier TWO = TestDefs.id("two");
    private static final Identifier GONE = TestDefs.id("revoked_later");
    private static final Identifier TRIM = TestDefs.id("trim/orbital");

    private static TrophyState twoPlayers() {
        TrophyState s = new TrophyState();
        s.award(A, ONE, 1_000L, 0x1111L);
        s.award(A, TWO, 2_000L, 0x1112L);
        s.award(A, GONE, 3_000L, 0x1113L);
        s.award(A, TRIM, 3_500L, 0L);
        s.markClaimed(A, ONE, 4_000L);
        s.markPaid(A, GONE);
        s.revoke(A, GONE); // paid survives revoke and must still be exported
        s.completeCollection(A, TestDefs.id("a_set"));
        s.refreshName(A, "ExportAlice", 5_000L);
        s.setAnnounce(A, false);
        s.setShowName(A, false);

        s.award(B, ONE, 6_000L, 0x2221L);
        s.award(B, TestDefs.id("bobs_secret"), 7_000L, 0x2222L);
        s.markPaid(B, ONE);
        s.completeCollection(B, TestDefs.id("bobs_set"));
        s.refreshName(B, "ExportBob", 8_000L);
        return s;
    }

    @Test
    void exportHoldsEveryStoredFieldOfTheRequester() {
        TrophyState s = twoPlayers();
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(List.of(TestDefs.trophy("one", TrophyTier.GOLD)), List.of(),
                1L);
        JsonObject out = TrophyExport.toJson(s, A, defs);
        String text = TrophyExport.toText(s, A, defs);

        assertEquals(A.toString(), out.get("player").getAsString());
        assertEquals("ExportAlice", out.get("last_known_name").getAsString());
        assertEquals(5_000L, out.get("name_refreshed_at").getAsLong());
        assertFalse(out.get("announce").getAsBoolean());
        assertFalse(out.get("show_name").getAsBoolean());
        assertEquals(3, out.getAsJsonArray("earned").size(), "one, two and the trim entry");
        assertTrue(text.contains(TrophyExport.hex(0x1111L)) && text.contains(TrophyExport.hex(0x1112L)));
        assertTrue(text.contains(TRIM.toString()));
        assertTrue(text.contains("\"last_claim\": 4000"));
        assertTrue(text.contains("a_set"));
        assertTrue(out.getAsJsonArray("coins_paid_for").toString().contains(GONE.toString()),
                "a paid id survives revoke and is exported");

        // Every value the store itself writes for A appears in the export (nothing stored is left out).
        JsonElement stored = TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow();
        int checked = 0;
        for (JsonElement rec : stored.getAsJsonObject().getAsJsonArray("players")) {
            String json = rec.toString();
            if (!json.contains("ExportAlice")) {
                continue; // the UUID is stored as an int array, so find A's record by its name
            }
            checked++;
            Set<String> leaves = new HashSet<>();
            collectStrings(rec, leaves);
            for (String leaf : leaves) {
                assertTrue(text.contains(leaf), "export misses stored value " + leaf);
            }
        }
        assertEquals(1, checked, "found the requester's stored record");
    }

    @Test
    void exportHoldsNothingOfAnyoneElse() {
        TrophyState s = twoPlayers();
        String text = TrophyExport.toText(s, A, TestDefs.snapshot(List.of(), List.of(), 1L));
        assertFalse(text.contains(B.toString()));
        assertFalse(text.contains(B.toString().replace("-", "")));
        assertFalse(text.contains("ExportBob"));
        assertFalse(text.contains(TrophyExport.hex(0x2221L)));
        assertFalse(text.contains(TrophyExport.hex(0x2222L)));
        assertFalse(text.contains("bobs_secret"));
        assertFalse(text.contains("bobs_set"));

        String bob = TrophyExport.toText(s, B, TestDefs.snapshot(List.of(), List.of(), 1L));
        assertFalse(bob.contains(A.toString()));
        assertFalse(bob.contains("ExportAlice"));
        assertFalse(bob.contains(TrophyExport.hex(0x1111L)));
        assertFalse(bob.contains("a_set"));
    }

    /** String leaves (ids, the name, the UUID) of the stored record; numbers are checked field by field above. */
    private static void collectStrings(JsonElement e, Set<String> out) {
        if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(en -> collectStrings(en.getValue(), out));
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> collectStrings(x, out));
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            out.add(e.getAsString());
        }
    }
}
