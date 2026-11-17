package za.co.neroland.nerotrophies.gallery;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * What the gallery job needs from a world, so the build / clear sequence and its snapshot can be tested without a
 * game. The live implementation is {@link GalleryLevel}.
 */
public interface GalleryWorld {

    /** What was in one cell before the hall: its block state and, if it had one, its block entity's full data. */
    record Prior(CompoundTag state, Optional<CompoundTag> blockEntity) {
        public Prior {
            state = state.copy();
            blockEntity = blockEntity.map(CompoundTag::copy);
        }
    }

    /** Reads a cell as it is now. */
    Prior capture(BlockPos pos);

    /**
     * Takes everything out of a container or display at {@code pos} without dropping it (the snapshot, or the gallery
     * plan, already holds it), so replacing the block can never spill or duplicate items.
     */
    void empty(BlockPos pos);

    /** Sets the block of {@code piece} (no neighbour updates, no drops). */
    void placeBlock(BlockPos pos, GalleryPiece piece, GalleryFrame frame);

    /** Fills the block entity of a piece that {@linkplain GalleryPiece#hasContents() has contents}. */
    void fill(BlockPos pos, GalleryPiece piece, GalleryFrame frame);

    /** Puts {@code prior} back exactly (block state and block-entity data). */
    void restore(BlockPos pos, Prior prior);
}
