package za.co.neroland.nerotrophies.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * The Trophy Spotlight: a ceiling lamp or wall arm, light 14, a fixed neutral cone. No block entity, so it is not
 * tintable (decision 006).
 */
public class TrophySpotlightBlock extends Block {

    public static final MapCodec<TrophySpotlightBlock> CODEC = BlockCodecs.simple(TrophySpotlightBlock::new);
    private static final VoxelShape CEILING = Block.box(5.0D, 7.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final Map<Direction, VoxelShape> WALL =
            za.co.neroland.nerotrophies.block.Shapes.horizontal(5, 4, 4, 11, 10, 16);

    @SuppressWarnings("this-escape")
    public TrophySpotlightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DisplayProps.SPOT_FACING, Direction.DOWN));
    }

    // No @Override: 26.3 removed Block.codec().
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DisplayProps.SPOT_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return this.defaultBlockState().setValue(DisplayProps.SPOT_FACING,
                face.getAxis().isHorizontal() ? face : Direction.DOWN);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(DisplayProps.SPOT_FACING);
        return facing == Direction.DOWN ? state : state.setValue(DisplayProps.SPOT_FACING, rotation.rotate(facing));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(DisplayProps.SPOT_FACING);
        return facing == Direction.DOWN ? state : state.rotate(mirror.getRotation(facing));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(DisplayProps.SPOT_FACING);
        return facing == Direction.DOWN ? CEILING : WALL.getOrDefault(facing, Shapes.block());
    }
}
