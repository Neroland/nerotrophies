package za.co.neroland.nerotrophies.gallery;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * What goes into one cell of the hall, in the hall's local frame ({@link GalleryFrame}). Pure data: the world side
 * ({@link GalleryLevel}) turns a piece into blocks and block-entity contents. Every trophy a piece carries is a
 * {@link TrophyStamp} with a reserved token ({@code -1} display copy, {@code -2} nameless copy, {@code 0} replica):
 * the gallery never holds a live claim token, so nothing here can award, count for or name a real player.
 */
public sealed interface GalleryPiece {

    /** A direction in the hall's frame: towards the entrance, away from it, the player's left or right, or down. */
    enum Face { TO_ENTRANCE, AWAY, LEFT, RIGHT, DOWN }

    /** The plain vanilla blocks of the shell. */
    enum Mat { AIR, FLOOR, RUNNER, WALL, CEILING, BAND, BASE }

    /** The six demo plates, in lobby order. */
    enum Demo { BRONZE, SILVER, GOLD, NERO, COLLECTION, SUMMARY }

    /** Whether this piece needs block-entity contents after the blocks are down. */
    default boolean hasContents() {
        return false;
    }

    /** Every trophy stamp this piece places (gallery tokens only). */
    default List<TrophyStamp> stamps() {
        return List.of();
    }

    record Solid(Mat mat) implements GalleryPiece {
    }

    /** A Trophy Spotlight: ceiling-mounted ({@link Face#DOWN}) or a wall arm facing out of its wall. */
    record Spotlight(Face face) implements GalleryPiece {
    }

    record Plate(Demo demo) implements GalleryPiece {
    }

    /** A warped sign: on a wall (facing out of it) or standing (text facing {@code face}); four lines at most. */
    record Sign(Face face, boolean wall, List<GalleryText> lines) implements GalleryPiece {
        public Sign {
            lines = List.copyOf(lines);
            if (lines.size() > 4) {
                throw new IllegalArgumentException("a sign has four lines");
            }
        }

        @Override
        public boolean hasContents() {
            return true;
        }
    }

    /** A trophy block facing the viewer. */
    record Trophy(Face face, TrophyStamp stamp) implements GalleryPiece {
        public Trophy {
            requireGallery(stamp);
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @Override
        public List<TrophyStamp> stamps() {
            return List.of(this.stamp);
        }
    }

    /**
     * A Display Pedestal with an optional finish, holding a trophy, the gallery's showpiece (an ordinary item, to show
     * that a display takes any item), or nothing.
     */
    record Pedestal(Face face, Optional<Identifier> finish, Optional<TrophyStamp> stamp, boolean showpiece)
            implements GalleryPiece {
        public Pedestal {
            stamp.ifPresent(GalleryPiece::requireGallery);
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @Override
        public List<TrophyStamp> stamps() {
            return this.stamp.map(List::of).orElse(List.of());
        }
    }

    /**
     * A Trophy Cabinet ({@code wall} false, six slots) or Wall Cabinet (two slots). {@code displayOnly} is the flag that
     * lets a gallery cabinet count gallery copies; only the gallery ever sets it.
     */
    record Cabinet(Face face, boolean wall, Optional<Identifier> collection, String trim, Optional<Identifier> finish,
            boolean displayOnly, List<TrophyStamp> contents) implements GalleryPiece {
        public Cabinet {
            contents = List.copyOf(contents);
            if (contents.size() > (wall ? 2 : 6)) {
                throw new IllegalArgumentException("too many trophies for one cabinet");
            }
            contents.forEach(GalleryPiece::requireGallery);
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @Override
        public List<TrophyStamp> stamps() {
            return this.contents;
        }
    }

    /** A Trophy Plaque on a wall. */
    record Plaque(Face face, TrophyStamp stamp) implements GalleryPiece {
        public Plaque {
            requireGallery(stamp);
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @Override
        public List<TrophyStamp> stamps() {
            return List.of(this.stamp);
        }
    }

    /** Gallery copies use reserved tokens only: never a token that could belong to a player. */
    static void requireGallery(TrophyStamp stamp) {
        if (!TrophyStamp.isReserved(stamp.token())) {
            throw new IllegalArgumentException("gallery trophies carry reserved tokens only");
        }
    }
}
