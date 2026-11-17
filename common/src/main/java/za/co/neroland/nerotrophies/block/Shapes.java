package za.co.neroland.nerotrophies.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Facing-rotated box shapes, authored for a north-facing model (front on z 0). */
final class Shapes {

    private Shapes() {
    }

    /** One box per horizontal facing, from a north-facing box in pixels. */
    static Map<Direction, VoxelShape> horizontal(double x0, double y0, double z0, double x1, double y1, double z1) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, Block.box(x0, y0, z0, x1, y1, z1));
        map.put(Direction.SOUTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
        map.put(Direction.EAST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
        map.put(Direction.WEST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
        return map;
    }
}
