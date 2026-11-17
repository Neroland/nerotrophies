package za.co.neroland.nerotrophies.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.menu.CabinetMenu;
import za.co.neroland.nerotrophies.menu.MenuOpener;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/**
 * The Trophy Cabinet: a floor vitrine with six slots. Use opens its screen (slots, collection binding, trim); a
 * dye or Core material sets its finish; sneak-use with a Trophy Book binds its pool to the Book's collection.
 * Cabinets beside each other that face the same way and share a binding pool their slots, up to four.
 */
public class TrophyCabinetBlock extends DisplayBlock {

    public static final MapCodec<TrophyCabinetBlock> CODEC = BlockCodecs.simple(TrophyCabinetBlock::new);

    public TrophyCabinetBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 removed Block.codec().
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState defaults(BlockState state) {
        return super.defaults(state).setValue(DisplayProps.GLOW, 0).setValue(DisplayProps.LEFT, false)
                .setValue(DisplayProps.RIGHT, false).setValue(DisplayProps.TRIM, TrimStyle.PLATE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DisplayProps.GLOW, DisplayProps.LEFT, DisplayProps.RIGHT, DisplayProps.TRIM);
    }

    @Override
    protected BlockEntityType<? extends DisplayBlockEntity> displayType() {
        return ModBlockEntities.TROPHY_CABINET.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state);
    }

    @Override
    protected Use useEmpty(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof CabinetBlockEntity cabinet) {
            MenuOpener.open(server, CabinetMenu.provider(cabinet));
        }
        return Use.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof CabinetBlockEntity cabinet) {
            cabinet.refreshRow();
            CabinetBlockEntity.refreshAround(server, pos);
        }
    }

    @Override
    protected void afterRemoved(ServerLevel level, BlockPos pos, BlockState oldState) {
        CabinetBlockEntity.refreshAround(level, pos);
    }
}
