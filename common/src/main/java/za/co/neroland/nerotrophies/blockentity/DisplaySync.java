package za.co.neroland.nerotrophies.blockentity;

/**
 * The client-sync byte budget for the ordinary (non-trophy) items a display holds. A display takes any item, and an
 * item can carry a great deal of component data (a full shulker box, a long written book), so the update tag a
 * display sends with its chunk is capped: an item whose encoded data is larger than {@link #ITEM_MAX_BYTES}, or that
 * would take the display past {@link #DISPLAY_MAX_BYTES}, is sent by item id only. Clients then draw the item's plain
 * model; the server still holds and saves the full stack, and breaking the display returns it intact. Pure.
 */
public final class DisplaySync {

    /** Most encoded bytes one item may send with all its components. */
    public static final int ITEM_MAX_BYTES = 8 * 1024;
    /** Most encoded bytes all of one display's full items may send together (six cabinet slots at most). */
    public static final int DISPLAY_MAX_BYTES = 32 * 1024;

    private DisplaySync() {
    }

    /**
     * For each item size in slot order, whether it is sent in full ({@code true}) or by id only. Items are admitted
     * first come, first served while the display budget lasts; an oversized one never spends the budget.
     */
    public static boolean[] plan(int[] sizes) {
        boolean[] full = new boolean[sizes.length];
        long left = DISPLAY_MAX_BYTES;
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            if (size >= 0 && size <= ITEM_MAX_BYTES && size <= left) {
                full[i] = true;
                left -= size;
            }
        }
        return full;
    }
}
