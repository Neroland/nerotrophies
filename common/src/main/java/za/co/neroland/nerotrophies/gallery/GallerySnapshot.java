package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Where a hall stands and everything that was in its box before it was built, so {@code gallery clear} can put the
 * world back exactly. Block states are kept as a palette plus one index per cell (cell order: {@link GalleryPlan#index}),
 * block-entity data per cell index. Holds world data only: no player, no name.
 */
public record GallerySnapshot(Identifier dimension, BlockPos origin, Direction forward, int length,
        List<CompoundTag> palette, int[] states, Map<Integer, CompoundTag> blockEntities, List<Plate> plates) {

    /** A demo plate's world position and what it plays. */
    public record Plate(BlockPos pos, GalleryPiece.Demo demo) {
        static final Codec<Plate> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Plate::pos),
                Codec.STRING.xmap(GalleryPiece.Demo::valueOf, GalleryPiece.Demo::name).fieldOf("demo")
                        .forGetter(Plate::demo)).apply(i, Plate::new));
    }

    private record BlockEntityData(int cell, CompoundTag data) {
        static final Codec<BlockEntityData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("cell").forGetter(BlockEntityData::cell),
                CompoundTag.CODEC.fieldOf("data").forGetter(BlockEntityData::data)).apply(i, BlockEntityData::new));
    }

    private static final Codec<Direction> HORIZONTAL = Codec.STRING.comapFlatMap(name -> {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d.getName().equals(name)) {
                return DataResult.success(d);
            }
        }
        return DataResult.error(() -> "not a horizontal direction: " + name);
    }, Direction::getName);

    public static final Codec<GallerySnapshot> CODEC = RecordCodecBuilder.<GallerySnapshot>create(i -> i.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(GallerySnapshot::dimension),
            BlockPos.CODEC.fieldOf("origin").forGetter(GallerySnapshot::origin),
            HORIZONTAL.fieldOf("forward").forGetter(GallerySnapshot::forward),
            Codec.intRange(1, 4096).fieldOf("length").forGetter(GallerySnapshot::length),
            CompoundTag.CODEC.listOf().fieldOf("palette").forGetter(GallerySnapshot::palette),
            Codec.INT_STREAM.<int[]>xmap(IntStream::toArray, java.util.Arrays::stream).fieldOf("states")
                    .forGetter(GallerySnapshot::states),
            BlockEntityData.CODEC.listOf().xmap(GallerySnapshot::toMap, GallerySnapshot::toList)
                    .fieldOf("block_entities").forGetter(GallerySnapshot::blockEntities),
            Plate.CODEC.listOf().fieldOf("plates").forGetter(GallerySnapshot::plates))
            .apply(i, GallerySnapshot::new)).flatXmap(GallerySnapshot::validate, GallerySnapshot::validate);

    public GallerySnapshot {
        palette = List.copyOf(palette);
        blockEntities = Map.copyOf(blockEntities);
        plates = List.copyOf(plates);
    }

    private static DataResult<GallerySnapshot> validate(GallerySnapshot s) {
        if (s.states.length != s.frame().cells()) {
            return DataResult.error(() -> "gallery snapshot has the wrong number of cells");
        }
        for (int index : s.states) {
            if (index < 0 || index >= s.palette.size()) {
                return DataResult.error(() -> "gallery snapshot palette index out of range");
            }
        }
        return DataResult.success(s);
    }

    private static Map<Integer, CompoundTag> toMap(List<BlockEntityData> list) {
        Map<Integer, CompoundTag> out = new HashMap<>();
        list.forEach(e -> out.put(e.cell(), e.data()));
        return out;
    }

    private static List<BlockEntityData> toList(Map<Integer, CompoundTag> map) {
        List<BlockEntityData> out = new ArrayList<>();
        map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> out.add(new BlockEntityData(e.getKey(), e.getValue())));
        return out;
    }

    public GalleryFrame frame() {
        return new GalleryFrame(this.origin, this.forward, this.length);
    }

    /** What cell {@code index} held. */
    public GalleryWorld.Prior prior(int index) {
        return new GalleryWorld.Prior(this.palette.get(this.states[index]),
                Optional.ofNullable(this.blockEntities.get(index)));
    }

    /** Collects priors cell by cell into a snapshot. */
    public static final class Builder {
        private final GalleryFrame frame;
        private final Identifier dimension;
        private final List<CompoundTag> palette = new ArrayList<>();
        private final Map<CompoundTag, Integer> paletteIndex = new HashMap<>();
        private final int[] states;
        private final Map<Integer, CompoundTag> blockEntities = new HashMap<>();

        public Builder(Identifier dimension, GalleryFrame frame) {
            this.dimension = dimension;
            this.frame = frame;
            this.states = new int[frame.cells()];
        }

        public void put(int index, GalleryWorld.Prior prior) {
            Integer at = this.paletteIndex.get(prior.state());
            if (at == null) {
                at = this.palette.size();
                this.palette.add(prior.state());
                this.paletteIndex.put(prior.state(), at);
            }
            this.states[index] = at;
            prior.blockEntity().ifPresent(tag -> this.blockEntities.put(index, tag));
        }

        public GallerySnapshot build(List<Plate> plates) {
            return new GallerySnapshot(this.dimension, this.frame.origin(), this.frame.forward(), this.frame.length(),
                    this.palette, this.states.clone(), this.blockEntities, plates);
        }
    }
}
