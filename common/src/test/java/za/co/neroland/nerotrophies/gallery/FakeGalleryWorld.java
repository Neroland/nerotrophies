package za.co.neroland.nerotrophies.gallery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * A world of block-state tags and block-entity tags for the gallery job, with no game. Unset cells hold stone. Records
 * every position written, and counts operations, so tests can check the box, the order and the budget.
 */
final class FakeGalleryWorld implements GalleryWorld {

    final Map<BlockPos, GalleryWorld.Prior> cells = new HashMap<>();
    final Set<BlockPos> written = new HashSet<>();
    int captures;
    int ops;
    int placesBeforeCaptureDone;
    int expectedCells = -1;
    int spilled;

    static CompoundTag state(String name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    /** A chest-like block entity holding {@code items} stacks. */
    static CompoundTag container(int items) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:chest");
        ListTag list = new ListTag();
        for (int i = 0; i < items; i++) {
            CompoundTag stack = new CompoundTag();
            stack.putString("id", "minecraft:diamond");
            stack.putInt("Slot", i);
            list.add(stack);
        }
        tag.put("Items", list);
        return tag;
    }

    GalleryWorld.Prior get(BlockPos pos) {
        return this.cells.getOrDefault(pos, new GalleryWorld.Prior(state("minecraft:stone"), Optional.empty()));
    }

    @Override
    public GalleryWorld.Prior capture(BlockPos pos) {
        this.captures++;
        this.ops++;
        return this.get(pos);
    }

    @Override
    public void empty(BlockPos pos) {
        // Not counted: emptying is part of the cell's place or restore step.
        GalleryWorld.Prior prior = this.get(pos);
        if (prior.blockEntity().isPresent() && prior.blockEntity().get().contains("Items")) {
            CompoundTag be = prior.blockEntity().get().copy();
            be.put("Items", new ListTag());
            this.cells.put(pos, new GalleryWorld.Prior(prior.state(), Optional.of(be)));
        }
    }

    @Override
    public void placeBlock(BlockPos pos, GalleryPiece piece, GalleryFrame frame) {
        this.ops++;
        if (this.expectedCells >= 0 && this.captures < this.expectedCells) {
            this.placesBeforeCaptureDone++;
        }
        GalleryWorld.Prior old = this.get(pos);
        if (old.blockEntity().isPresent() && old.blockEntity().get().get("Items") instanceof ListTag items
                && !items.isEmpty()) {
            this.spilled++; // a container replaced while still full would have dropped its items
        }
        this.written.add(pos);
        Optional<CompoundTag> be = piece.hasContents() ? Optional.of(new CompoundTag()) : Optional.empty();
        this.cells.put(pos, new GalleryWorld.Prior(state("gallery:" + piece.getClass().getSimpleName()), be));
    }

    @Override
    public void fill(BlockPos pos, GalleryPiece piece, GalleryFrame frame) {
        this.ops++;
        CompoundTag be = new CompoundTag();
        be.putString("piece", piece.toString());
        this.cells.put(pos, new GalleryWorld.Prior(this.get(pos).state(), Optional.of(be)));
    }

    @Override
    public void restore(BlockPos pos, GalleryWorld.Prior prior) {
        this.ops++;
        this.written.add(pos);
        this.cells.put(pos, prior);
    }

    /** Every cell in the given box, as it is now. */
    Map<BlockPos, GalleryWorld.Prior> snapshot(BlockPos min, BlockPos max) {
        Map<BlockPos, GalleryWorld.Prior> out = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            out.put(pos.immutable(), this.get(pos));
        }
        return out;
    }
}
