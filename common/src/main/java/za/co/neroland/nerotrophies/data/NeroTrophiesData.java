package za.co.neroland.nerotrophies.data;

import za.co.neroland.nerolandcore.data.PlayerDataErasure;

/**
 * Registers NeroTrophies' eraser with Core (common init step 4), before any store can exist: registering late is how
 * an erasure request silently misses a store. Retention comes with it, because Core's sweep runs every registered
 * eraser for players inactive longer than {@code dataRetentionDays}.
 */
public final class NeroTrophiesData {

    private static TrophyEraser eraser;

    private NeroTrophiesData() {
    }

    public static synchronized void init() {
        if (eraser == null) {
            eraser = new TrophyEraser();
            PlayerDataErasure.register(eraser);
        }
    }

    /** The registered eraser (null before init). */
    public static synchronized TrophyEraser eraser() {
        return eraser;
    }
}
