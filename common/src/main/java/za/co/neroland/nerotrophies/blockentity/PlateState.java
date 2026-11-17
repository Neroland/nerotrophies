package za.co.neroland.nerotrophies.blockentity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * The three nameplate states a renderer may draw, plus the gallery placeholder. Decided from the stamp's
 * token and what name sync told this client; a replica never carries a name, whatever else is known.
 */
public enum PlateState {
    /** Lit gamertag. */
    NAMED,
    /** The lit ornament: genuine with no name to show, erased, unknown, or not resolved yet. */
    BLANK,
    /** The pewter cover: no name, ever. */
    REPLICA,
    /** A gallery display copy: the client-local placeholder name. */
    GALLERY;

    /** Which plate to draw. {@code replica} / {@code name} come from name sync for this token (null = unknown). */
    public static PlateState of(long token, boolean replica, @Nullable String name) {
        if (token == TrophyStamp.UNSTAMPED || replica) {
            return REPLICA;
        }
        if (token == TrophyStamp.GALLERY_TOKEN) {
            return GALLERY;
        }
        if (TrophyStamp.isReserved(token)) {
            return BLANK;
        }
        return name != null && !name.isEmpty() ? NAMED : BLANK;
    }

    /** The name a plate may show: only {@link #NAMED} ever returns one. */
    @Nullable
    public static String shownName(long token, boolean replica, @Nullable String name) {
        return of(token, replica, name) == NAMED ? name : null;
    }
}
