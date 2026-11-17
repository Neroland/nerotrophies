package za.co.neroland.nerotrophies.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
//? if >=1.21.2 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
*///?}
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * Blockstate properties shared by the display blocks. {@link #TIER} and {@link #REPLICA} on the trophy are
 * <b>display only</b>: the server sets them from its own token check on load and on a slow re-check, and nothing
 * ever reads them as truth (a cabinet counts by resolving the stamp, never by state).
 */
public final class DisplayProps {

    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The spotlight: down (ceiling) or a horizontal wall arm. */
    public static final EnumProperty<Direction> SPOT_FACING = EnumProperty.create("facing", Direction.class,
            Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final DirectionProperty SPOT_FACING = DirectionProperty.create("facing", Direction.DOWN,
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    *///?}
    public static final EnumProperty<TrophyTier> TIER = EnumProperty.create("tier", TrophyTier.class);
    public static final BooleanProperty REPLICA = BooleanProperty.create("replica");
    /** Light level of an emissive finish; 0 otherwise. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 15);
    /** A cabinet joins its neighbour on that side (same block, same facing, same binding). */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final EnumProperty<TrimStyle> TRIM = EnumProperty.create("trim", TrimStyle.class);

    private DisplayProps() {
    }
}
