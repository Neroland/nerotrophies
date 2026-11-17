package za.co.neroland.nerotrophies.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PlaqueBlockEntity;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/** The Trophy Plaque: a wall plaque showing one trophy's medallion, title and earner. */
public class TrophyPlaqueBlock extends SingleDisplayBlock {

    public static final MapCodec<TrophyPlaqueBlock> CODEC = BlockCodecs.simple(TrophyPlaqueBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.horizontal(1, 0, 14, 15, 16, 16);

    public TrophyPlaqueBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 removed Block.codec().
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState defaults(BlockState state) {
        return super.defaults(state).setValue(DisplayProps.GLOW, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DisplayProps.GLOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(DisplayProps.FACING, facing);
    }

    @Override
    protected BlockEntityType<? extends DisplayBlockEntity> displayType() {
        return ModBlockEntities.TROPHY_PLAQUE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlaqueBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(DisplayProps.FACING));
    }

    @Override
    protected Use useWith(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand) {
        return this.insert(stack, level, pos, player);
    }
}
