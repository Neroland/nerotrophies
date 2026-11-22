package za.co.neroland.nerotrophies.data;

import za.co.neroland.nerolandcore.data.PlayerDataErasure;
import za.co.neroland.nerolandcore.data.PlayerDataExport;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerotrophies.NeroTrophiesCommon;

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
            // The read-side twin: Core's /neroland data exportme carries the /nerotrophies export document.
            PlayerDataExport.register(NeroTrophiesCommon.MOD_ID,
                    (server, player) -> TrophyExport.coreExport(TrophyState.get(server), player,
                            TrophyApi.definitions(server)));
        }
    }

    /** The registered eraser (null before init). */
    public static synchronized TrophyEraser eraser() {
        return eraser;
    }
}
