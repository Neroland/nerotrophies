package za.co.neroland.nerotrophies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.Gson;
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
        List<String> parts = TrophyExport.chunks("x".repeat(20), 8);
        assertEquals(List.of("xxxxxxxx", "xxxxxxxx", "xxxx"), parts);
        assertEquals(1, TrophyExport.chunks("", 8).size());
    }

    /** Every limit a click-event value meets on the way: UTF-8, NBT's modified UTF-8, and escaped JSON. */
    private static void assertFitsAClickEvent(String chunk) {
        int utf8 = chunk.getBytes(StandardCharsets.UTF_8).length;
        int modified = 0;
        for (char c : chunk.toCharArray()) {
            modified += c == 0 ? 2 : c < 0x80 ? 1 : c < 0x800 ? 2 : 3;
        }
        int json = new Gson().toJson(chunk).getBytes(StandardCharsets.UTF_8).length;
        assertTrue(utf8 <= TrophyExport.STRING_LIMIT_BYTES, "UTF-8 bytes " + utf8);
        assertTrue(modified <= TrophyExport.STRING_LIMIT_BYTES, "modified UTF-8 bytes " + modified);
        assertTrue(json <= TrophyExport.STRING_LIMIT_BYTES, "escaped JSON bytes " + json);
        assertTrue(TrophyExport.encodedBytes(chunk) <= TrophyExport.CHUNK_BYTES);
        assertFalse(!chunk.isEmpty() && Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)),
                "a chunk never ends inside a surrogate pair");
    }

    @Test
    void aFullRecordWithMultibyteTextExportsInChunksThatFitTheClickEventLimit() {
        TrophyState s = new TrophyState();
        List<za.co.neroland.nerolandcore.trophy.TrophyDefinition> defs = new ArrayList<>();
        long token = 1L;
        for (int i = 0; i < 4096; i++) {
            String path = String.format(Locale.ROOT, "pack/trophy_%04d", i);
            s.award(A, TestDefs.id(path), 1_700_000_000_000L + i, ++token);
            defs.add(TestDefs.trophy(path, TrophyTier.GOLD));
        }
        for (int i = 0; i < 512; i++) {
            s.award(A, TestDefs.id("trim/t" + i), 1_700_000_000_000L + i, 0L);
        }
        s.refreshName(A, "\u00c9l\u00e9a\u6f22\u5b57\ud83c\udfc6\"<&>", 2L); // accents, CJK, an emoji, escapes
        String json = TrophyExport.toText(s, A, TestDefs.snapshot(defs, List.of(), 1L));
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length > 4 * TrophyExport.STRING_LIMIT_BYTES,
                "precondition: the export is far larger than one click event can carry");

        List<String> parts = TrophyExport.chunks(json, TrophyExport.CHUNK_BYTES);
        assertTrue(parts.size() > 4);
        parts.forEach(TrophyExportTest::assertFitsAClickEvent);
        assertEquals(json, String.join("", parts), "nothing is lost or reordered");
    }

    @Test
    void multibyteAndEscapedTextIsChunkedByBytesNotCharacters() {
        String text = "\ud83c\udfc6".repeat(30_000) + "\u6f22".repeat(30_000) + "\"\\\n".repeat(20_000);
        List<String> parts = TrophyExport.chunks(text, TrophyExport.CHUNK_BYTES);
        parts.forEach(TrophyExportTest::assertFitsAClickEvent);
        assertEquals(text, String.join("", parts));
    }
}
