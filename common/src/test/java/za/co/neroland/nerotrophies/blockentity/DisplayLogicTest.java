package za.co.neroland.nerotrophies.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/** Breaking a display returns everything inside it exactly once; the pedestal's comparator output by tier. */
class DisplayLogicTest {

    @Test
    void breakingReturnsEveryItemOnceAndLeavesTheDisplayEmpty() {
        List<String> slots = new ArrayList<>(Arrays.asList("a", "", "b", "", "", "c"));
        List<String> dropped = DisplayContents.drain(slots, String::isEmpty, () -> "");
        assertEquals(List.of("a", "b", "c"), dropped);
        assertTrue(slots.stream().allMatch(String::isEmpty));
        assertEquals(List.of(), DisplayContents.drain(slots, String::isEmpty, () -> ""), "nothing twice");
    }

    @Test
    void pedestalComparatorFollowsTheTier() {
        assertEquals(0, PedestalSignal.of(false, Optional.empty(), false));
        assertEquals(4, PedestalSignal.of(true, Optional.of(TrophyTier.BRONZE), false));
        assertEquals(8, PedestalSignal.of(true, Optional.of(TrophyTier.SILVER), false));
        assertEquals(12, PedestalSignal.of(true, Optional.of(TrophyTier.GOLD), false));
        assertEquals(15, PedestalSignal.of(true, Optional.of(TrophyTier.NERO), false));
        assertEquals(1, PedestalSignal.of(true, Optional.of(TrophyTier.NERO), true), "replica");
        assertEquals(1, PedestalSignal.of(true, Optional.empty(), false), "other item");
    }
}
