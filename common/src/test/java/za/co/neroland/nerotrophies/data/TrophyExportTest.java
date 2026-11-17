package za.co.neroland.nerotrophies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** {@code /nerotrophies export}: everything about one player, nothing about anyone else. */
class TrophyExportTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void exportReturnsOnePlayerOnly() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1L, 0x1111L);
        s.refreshName(A, "Alice", 2L);
        s.award(B, TestDefs.id("one"), 3L, 0x2222L);
        s.award(B, TestDefs.id("secret"), 4L, 0x3333L);
        s.refreshName(B, "Bob", 5L);
        s.completeCollection(B, TestDefs.id("bobs_set"));
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(List.of(TestDefs.trophy("one", TrophyTier.GOLD)),
                List.of(), 1L);

        String a = TrophyExport.toText(s, A, defs);
        assertTrue(a.contains(A.toString()));
        assertTrue(a.contains("Alice"));
        assertTrue(a.contains(TrophyExport.hex(0x1111L)));
        assertTrue(a.contains("trophy.nerotrophies.one.title"));
        assertFalse(a.contains(B.toString()));
        assertFalse(a.contains("Bob"));
        assertFalse(a.contains(TrophyExport.hex(0x2222L)));
        assertFalse(a.contains("secret"));
        assertFalse(a.contains("bobs_set"));

        JsonObject json = TrophyExport.toJson(s, A, defs);
        assertEquals(1, json.getAsJsonArray("earned").size());
    }

    @Test
    void exportOfSomeoneWithoutARecordSaysSo() {
        JsonObject json = TrophyExport.toJson(new TrophyState(), A, TestDefs.snapshot(List.of(), List.of(), 0L));
        assertFalse(json.get("has_record").getAsBoolean());
        assertFalse(json.has("earned"));
    }

    @Test
    void largeExportsAreChunked() {
        List<String> parts = TrophyExport.chunks("x".repeat(10), 4);
        assertEquals(List.of("xxxx", "xxxx", "xx"), parts);
        assertEquals(1, TrophyExport.chunks("", 4).size());
    }
}
