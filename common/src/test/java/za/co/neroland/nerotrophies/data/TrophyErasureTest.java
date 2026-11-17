package za.co.neroland.nerotrophies.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.ErasureConformance;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;

/**
 * Erasure removes the whole record and its token index; every trophy the player earned then resolves to blank (never
 * to their name, never to a replica). Core's retention sweep reaches the same eraser, so retention needs nothing more.
 */
class TrophyErasureTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static TrophyState twoPlayers() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1L, 101L);
        s.award(A, TestDefs.id("two"), 2L, 102L);
        s.refreshName(A, "Alice", 3L);
        s.completeCollection(A, TestDefs.id("set"));
        s.markPaid(A, TestDefs.id("one"));
        s.award(B, TestDefs.id("one"), 4L, 201L);
        s.refreshName(B, "Bob", 5L);
        return s;
    }

    @Test
    void eraseRemovesTheWholeRecordAndTheTokenIndex() {
        TrophyState s = twoPlayers();
        long[] dropped = s.erase(A);
        assertArrayEquals(new long[] {101L, 102L}, dropped);
        assertFalse(s.hasRecord(A));
        assertFalse(s.anyTokenOwnedBy(A));
        assertTrue(s.owner(101L).isEmpty());
        assertEquals(0, s.retiredCount(), "erased tokens are not retired");
        assertEquals(TrophyStatus.BLANK, TokenResolver.resolve(s, TestDefs.id("one"), 101L).status());
        assertTrue(TokenResolver.resolve(s, TestDefs.id("one"), 101L).name().isEmpty());
        // Bob is untouched.
        assertEquals("Bob", TokenResolver.resolve(s, TestDefs.id("one"), 201L).name().orElseThrow());
    }

    @Test
    void erasureSurvivesASaveAndLoad() {
        TrophyState s = twoPlayers();
        s.erase(A);
        TrophyState back = TrophyState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                TrophyState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, s).getOrThrow()).getOrThrow();
        assertFalse(back.hasRecord(A));
        assertEquals(TrophyStatus.BLANK, TokenResolver.resolve(back, TestDefs.id("one"), 101L).status());
        String json = TrophyState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, back).getOrThrow()
                .toString();
        assertFalse(json.contains(A.toString()));
        assertFalse(json.contains("Alice"));
    }

    @Test
    void coreErasureConformancePasses() {
        TrophyState s = twoPlayers();
        TrophyEraser eraser = TrophyEraser.forState(s);
        PlayerDataErasure.register(eraser);
        try {
            ErasureConformance.create()
                    .probe("nerotrophies", s::hasRecord)
                    .probe("nerotrophies.tokens", s::anyTokenOwnedBy)
                    .verify(null, A);
        } finally {
            PlayerDataErasure.unregister(eraser);
        }
        assertTrue(s.hasRecord(B));
    }

    @Test
    void retentionSweepPathRunsTheSameEraser() {
        // Core's retention sweep calls every registered eraser for each stale player, exactly as erase() does.
        TrophyState s = twoPlayers();
        TrophyEraser eraser = TrophyEraser.forState(s);
        PlayerDataErasure.register(eraser);
        try {
            PlayerDataErasure.erase(null, A);
        } finally {
            PlayerDataErasure.unregister(eraser);
        }
        assertFalse(s.hasRecord(A));
        assertTrue(s.hasRecord(B));
    }

    @Test
    void erasingSomeoneWithoutARecordIsHarmless() {
        TrophyState s = twoPlayers();
        TrophyEraser.forState(s).erase(null, UUID.randomUUID());
        assertEquals(2, s.recordCount());
    }
}
