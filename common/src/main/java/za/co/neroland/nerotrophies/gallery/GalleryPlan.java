package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.List;

import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * A laid-out hall: one {@link GalleryPiece} for every cell of the {@code length} x {@link GalleryFrame#WIDTH} x
 * {@link GalleryFrame#HEIGHT} box, indexed like {@link GalleryFrame#index}, plus the demo plates. Independent of where
 * it is built.
 */
public record GalleryPlan(int length, GalleryPiece[] cells, List<PlateSpot> plates) {

    /** A demo plate's cell and what it plays. */
    public record PlateSpot(GalleryFrame.Local local, GalleryPiece.Demo demo) {
    }

    public GalleryPlan {
        if (cells.length != length * GalleryFrame.WIDTH * GalleryFrame.HEIGHT) {
            throw new IllegalArgumentException("cells");
        }
        plates = List.copyOf(plates);
    }

    public GalleryPiece at(GalleryFrame.Local local) {
        return this.cells[index(local)];
    }

    public static int index(GalleryFrame.Local local) {
        return (local.l() * GalleryFrame.WIDTH + (local.w() - GalleryFrame.W_MIN)) * GalleryFrame.HEIGHT + local.y();
    }

    public static GalleryFrame.Local local(int index) {
        int y = index % GalleryFrame.HEIGHT;
        int rest = index / GalleryFrame.HEIGHT;
        return new GalleryFrame.Local(rest / GalleryFrame.WIDTH, rest % GalleryFrame.WIDTH + GalleryFrame.W_MIN, y);
    }

    /** Every trophy stamp the hall places (all of them reserved gallery tokens). */
    public List<TrophyStamp> stamps() {
        List<TrophyStamp> out = new ArrayList<>();
        for (GalleryPiece piece : this.cells) {
            out.addAll(piece.stamps());
        }
        return out;
    }
}
