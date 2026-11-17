package za.co.neroland.nerotrophies.gallery;

import java.util.Optional;

/**
 * Who may build or clear the gallery: a player (never the console, a command block or a function, so the hall is
 * always placed in front of someone and nothing about it reaches the server log) in creative mode. Operator level is
 * checked by the command tree. Pure; tested.
 */
public final class GalleryAccess {

    public static final String PLAYERS_ONLY = "nerotrophies.gallery.players_only";
    public static final String CREATIVE_ONLY = "nerotrophies.gallery.creative_only";

    private GalleryAccess() {
    }

    /** The lang key of the refusal, or empty when the source may use the gallery. */
    public static Optional<String> refusal(boolean player, boolean creative) {
        if (!player) {
            return Optional.of(PLAYERS_ONLY);
        }
        if (!creative) {
            return Optional.of(CREATIVE_ONLY);
        }
        return Optional.empty();
    }
}
