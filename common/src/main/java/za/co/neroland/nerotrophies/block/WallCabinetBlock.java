package za.co.neroland.nerotrophies.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/** The Wall Cabinet: a 6 px deep wall vitrine with two slots; pools sideways like the floor cabinet. */
public class WallCabinetBlock extends TrophyCabinetBlock {

    public static final MapCodec<WallCabinetBlock> WALL_CODEC = BlockCodecs.simple(WallCabinetBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.horizontal(0, 2, 9, 16, 14, 16);

    public WallCabinetBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return WALL_CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(DisplayProps.FACING, facing);
    }

    @Override
    protected BlockEntityType<? extends DisplayBlockEntity> displayType() {
        return ModBlockEntities.WALL_CABINET.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CabinetBlockEntity.wall(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(DisplayProps.FACING));
    }
}
