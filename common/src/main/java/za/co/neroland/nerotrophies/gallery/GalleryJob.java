package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import net.minecraft.resources.Identifier;

/**
 * Builds or clears one hall a slice at a time, so a large catalogue never stalls the server: {@link #step} does at
 * most {@code maxOps} cells and stops once {@code maxNanos} have passed, then picks up where it left off next tick.
 *
 * <p><b>Build:</b> capture every cell first (nothing in the box changes before the whole snapshot exists), then place
 * every block, then fill the block entities (so cabinet pools see their neighbours). <b>Clear:</b> empty every cell's
 * container or display (so nothing drops or duplicates), then restore every cell from the snapshot. Clearing is
 * idempotent, so an interrupted clear can simply be run again.
 */
public final class GalleryJob {

    public enum Kind { BUILD, CLEAR }

    public enum Phase { CAPTURE, PLACE, FILL, EMPTY, RESTORE, DONE }

    private final Kind kind;
    private final GalleryFrame frame;
    private final GalleryPlan plan;
    private final GallerySnapshot.Builder capture;
    private GallerySnapshot snapshot;
    private Phase phase;
    private int cursor;
    private int ticks;

    private GalleryJob(Kind kind, GalleryFrame frame, GalleryPlan plan, GallerySnapshot.Builder capture,
            GallerySnapshot snapshot) {
        this.kind = kind;
        this.frame = frame;
        this.plan = plan;
        this.capture = capture;
        this.snapshot = snapshot;
        this.phase = kind == Kind.BUILD ? Phase.CAPTURE : Phase.EMPTY;
    }

    public static GalleryJob build(Identifier dimension, GalleryFrame frame, GalleryPlan plan) {
        if (plan.length() != frame.length()) {
            throw new IllegalArgumentException("plan and frame lengths differ");
        }
        return new GalleryJob(Kind.BUILD, frame, plan, new GallerySnapshot.Builder(dimension, frame), null);
    }

    public static GalleryJob clear(GallerySnapshot snapshot) {
        return new GalleryJob(Kind.CLEAR, snapshot.frame(), null, null, snapshot);
    }

    public Kind kind() {
        return this.kind;
    }

    public Phase phase() {
        return this.phase;
    }

    public boolean done() {
        return this.phase == Phase.DONE;
    }

    /** Ticks this job has run for. */
    public int ticks() {
        return this.ticks;
    }

    public GalleryFrame frame() {
        return this.frame;
    }

    /** The snapshot: complete once a build has passed {@link Phase#CAPTURE}, or the one being cleared. */
    public GallerySnapshot snapshot() {
        return this.snapshot;
    }

    /**
     * Runs one slice. Returns true when the build's capture just completed in this slice (the caller then persists
     * {@link #snapshot()} before any block changes), false otherwise.
     */
    public boolean step(GalleryWorld world, int maxOps, long maxNanos, LongSupplier clock) {
        this.ticks++;
        long start = clock.getAsLong();
        int ops = 0;
        int cells = this.frame.cells();
        while (this.phase != Phase.DONE && ops < maxOps) {
            if (ops > 0 && clock.getAsLong() - start >= maxNanos) {
                break;
            }
            switch (this.phase) {
                case CAPTURE -> {
                    this.capture.put(this.cursor, world.capture(this.frame.pos(this.cursor)));
                    ops++;
                    if (++this.cursor >= cells) {
                        this.snapshot = this.capture.build(this.plates());
                        this.next(Phase.PLACE);
                        // Hand back now: the caller persists the snapshot before the first block changes.
                        return true;
                    }
                }
                case PLACE -> {
                    var pos = this.frame.pos(this.cursor);
                    world.empty(pos);
                    world.placeBlock(pos, this.plan.cells()[this.cursor], this.frame);
                    ops++;
                    if (++this.cursor >= cells) {
                        this.next(Phase.FILL);
                    }
                }
                case FILL -> {
                    GalleryPiece piece = this.plan.cells()[this.cursor];
                    if (piece.hasContents()) {
                        world.fill(this.frame.pos(this.cursor), piece, this.frame);
                        ops++;
                    }
                    if (++this.cursor >= cells) {
                        this.next(Phase.DONE);
                    }
                }
                case EMPTY -> {
                    world.empty(this.frame.pos(this.cursor));
                    ops++;
                    if (++this.cursor >= cells) {
                        this.next(Phase.RESTORE);
                    }
                }
                case RESTORE -> {
                    world.restore(this.frame.pos(this.cursor), this.snapshot.prior(this.cursor));
                    ops++;
                    if (++this.cursor >= cells) {
                        this.next(Phase.DONE);
                    }
                }
                default -> {
                }
            }
        }
        return false;
    }

    private void next(Phase phase) {
        this.phase = phase;
        this.cursor = 0;
    }

    private List<GallerySnapshot.Plate> plates() {
        List<GallerySnapshot.Plate> out = new ArrayList<>();
        for (GalleryPlan.PlateSpot spot : this.plan.plates()) {
            out.add(new GallerySnapshot.Plate(this.frame.pos(spot.local()), spot.demo()));
        }
        return out;
    }
}
