package za.co.neroland.nerotrophies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** Core export (Core 1.20.1): the player's own record, no UUID, no raw claim token, nothing for no record. */
class TrophyCoreExportTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void noRecordExportsNothing() {
        assertNull(TrophyExport.coreExport(new TrophyState(), A, TestDefs.snapshot(List.of(), List.of(), 0L)));
    }

    @Test
    void recordHasNoUuidNoRawTokenAndNobodyElse() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1L, 0x1111L);
        s.refreshName(A, "Alice", 2L);
        s.award(B, TestDefs.id("one"), 3L, 0x2222L);
        s.refreshName(B, "Bob", 4L);
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(List.of(TestDefs.trophy("one", TrophyTier.GOLD)),
                List.of(), 1L);

        JsonObject json = TrophyExport.coreExport(s, A, defs);
        String text = json.toString();
        assertFalse(json.has("player"));
        assertFalse(text.contains(A.toString()));
        assertFalse(text.contains(TrophyExport.hex(0x1111L)), "the claim token is never exported raw");
        assertTrue(text.contains("Alice"), "the last-known gamertag is the player's own");
        assertFalse(text.contains("Bob"));
        assertFalse(text.contains(TrophyExport.hex(0x2222L)));
        assertEquals(1, json.getAsJsonArray("earned").size());
        JsonObject row = json.getAsJsonArray("earned").get(0).getAsJsonObject();
        assertFalse(row.has("token"));
        assertTrue(row.get("has_claim_token").getAsBoolean());
    }
}
