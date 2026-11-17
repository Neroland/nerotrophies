package za.co.neroland.nerotrophies.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
//? if >=1.21.10 {
import net.minecraft.core.Direction;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/** The Display Pedestal: any one item on a slow turntable; comparator output by trophy tier, 1 for a plain item. */
public class DisplayPedestalBlock extends SingleDisplayBlock {

    public static final MapCodec<DisplayPedestalBlock> CODEC = BlockCodecs.simple(DisplayPedestalBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public DisplayPedestalBlock(Properties properties) {
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
    protected BlockEntityType<? extends DisplayBlockEntity> displayType() {
        return ModBlockEntities.DISPLAY_PEDESTAL.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    //? if >=1.21.10 {
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal ? pedestal.signal() : 0;
    }
    //?} else {
    /*@Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal ? pedestal.signal() : 0;
    }
    *///?}

    @Override
    protected Use useWith(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand) {
        return this.insert(stack, level, pos, player);
    }
}
