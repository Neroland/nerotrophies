package za.co.neroland.nerotrophies.blockentity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The client-sync budget for plain items: small items go whole, an item over the per-item cap or past the
 * per-display budget goes by id only, and an oversized item never spends the budget of the items after it.
 */
class DisplaySyncTest {

    private static final int ITEM = DisplaySync.ITEM_MAX_BYTES;

    @Test
    void smallItemsAreSentWhole() {
        assertArrayEquals(new boolean[] {true, true, true}, DisplaySync.plan(new int[] {40, 300, 2_000}));
        assertArrayEquals(new boolean[0], DisplaySync.plan(new int[0]));
    }

    @Test
    void anOversizedItemGoesByIdOnlyAndSpendsNothing() {
        assertArrayEquals(new boolean[] {false, true}, DisplaySync.plan(new int[] {ITEM + 1, ITEM}));
        assertArrayEquals(new boolean[] {true}, DisplaySync.plan(new int[] {ITEM}));
    }

    @Test
    void anItemThatCouldNotBeEncodedGoesByIdOnly() {
        assertArrayEquals(new boolean[] {false, true}, DisplaySync.plan(new int[] {-1, 10}));
    }

    @Test
    void aFullCabinetOfLargeItemsStaysWithinTheDisplayBudget() {
        int[] sizes = {ITEM, ITEM, ITEM, ITEM, ITEM, ITEM};
        boolean[] plan = DisplaySync.plan(sizes);
        long sent = 0;
        int whole = 0;
        for (int i = 0; i < sizes.length; i++) {
            if (plan[i]) {
                sent += sizes[i];
                whole++;
            }
        }
        assertEquals(DisplaySync.DISPLAY_MAX_BYTES / ITEM, whole);
        assertTrue(sent <= DisplaySync.DISPLAY_MAX_BYTES);
        assertArrayEquals(new boolean[] {true, true, true, true, false, false}, plan);
    }
}
