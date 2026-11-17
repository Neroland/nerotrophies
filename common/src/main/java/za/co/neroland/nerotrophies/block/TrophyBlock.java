package za.co.neroland.nerotrophies.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.TrophyBlockEntity;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/**
 * The one trophy block: any trophy, by the stamp its block entity carries. {@code tier} and {@code replica} pick the
 * baked model and are display only (set by the server's token check). Not tintable: earned colour is not paintable.
 */
public class TrophyBlock extends DisplayBlock {

    public static final MapCodec<TrophyBlock> CODEC = BlockCodecs.simple(TrophyBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D);

    public TrophyBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 removed Block.codec().
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState defaults(BlockState state) {
        return super.defaults(state).setValue(DisplayProps.TIER, TrophyTier.BRONZE)
                .setValue(DisplayProps.REPLICA, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DisplayProps.TIER, DisplayProps.REPLICA);
    }

    @Override
    protected boolean tintable() {
        return false;
    }

    @Override
    protected BlockEntityType<? extends DisplayBlockEntity> displayType() {
        return ModBlockEntities.TROPHY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy) {
            trophy.contentsChanged();
        }
    }
}
