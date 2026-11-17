package za.co.neroland.nerotrophies.engine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.data.TrophyRecord;

/** Which collections a record has just completed (pure; the engine records and announces them). */
public final class CollectionRules {

    private CollectionRules() {
    }

    /** Collections containing {@code trophy} that the record now holds in full and has not recorded yet. */
    public static List<TrophyCollection> newlyCompletedBy(TrophyRecord record, TrophyDefinitions.Snapshot defs,
            Identifier trophy) {
        List<TrophyCollection> out = new ArrayList<>();
        for (TrophyCollection c : defs.collections().values()) {
            if (c.contains(trophy) && isNewlyComplete(record, c)) {
                out.add(c);
            }
        }
        return out;
    }

    /** Every complete but unrecorded collection (end of a retroactive pass, or after a definition change). */
    public static List<TrophyCollection> allNewlyCompleted(TrophyRecord record, TrophyDefinitions.Snapshot defs) {
        List<TrophyCollection> out = new ArrayList<>();
        for (TrophyCollection c : defs.collections().values()) {
            if (isNewlyComplete(record, c)) {
                out.add(c);
            }
        }
        return out;
    }

    private static boolean isNewlyComplete(TrophyRecord record, TrophyCollection c) {
        if (record.completedCollections().contains(c.id()) || c.members().isEmpty()) {
            return false;
        }
        for (Identifier member : c.members()) {
            if (!record.has(member)) {
                return false;
            }
        }
        return true;
    }
}
