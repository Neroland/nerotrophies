package za.co.neroland.nerotrophies.gallery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The hall's local frame: {@code l} runs away from the entrance wall ({@code l} = 0) along
 * {@link #forward}, {@code w} runs across (negative on the builder's left), {@code y} is height above the floor. Cells
 * are indexed as in {@link GalleryPlan#index}, the order the snapshot is stored in.
 */
public record GalleryFrame(BlockPos origin, Direction forward, int length) {

    public static final int W_MIN = -21;
    public static final int W_MAX = 21;
    public static final int WIDTH = W_MAX - W_MIN + 1;
    public static final int HEIGHT = 8;

    public GalleryFrame {
        if (!forward.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("the hall faces a horizontal direction");
        }
        if (length < 1) {
            throw new IllegalArgumentException("length");
        }
    }

    /** The hall's entrance two blocks in front of a player at {@code feet} looking {@code forward}; floor under them. */
    public static GalleryFrame inFrontOf(BlockPos feet, Direction forward, int length) {
        return new GalleryFrame(feet.relative(forward, 2).below(), forward, length);
    }

    public Direction right() {
        return this.forward.getClockWise();
    }

    public BlockPos pos(int l, int w, int y) {
        Direction right = this.right();
        return this.origin.offset(this.forward.getStepX() * l + right.getStepX() * w, y,
                this.forward.getStepZ() * l + right.getStepZ() * w);
    }

    public BlockPos pos(Local local) {
        return this.pos(local.l(), local.w(), local.y());
    }

    /** A local direction as a world direction ({@link GalleryPiece.Face#DOWN} is down). */
    public Direction world(GalleryPiece.Face face) {
        return switch (face) {
            case TO_ENTRANCE -> this.forward.getOpposite();
            case AWAY -> this.forward;
            case LEFT -> this.right().getOpposite();
            case RIGHT -> this.right();
            case DOWN -> Direction.DOWN;
        };
    }

    public int cells() {
        return this.length * WIDTH * HEIGHT;
    }

    public BlockPos pos(int index) {
        return this.pos(GalleryPlan.local(index));
    }

    /** A cell in the local frame. */
    public record Local(int l, int w, int y) {
    }
}
