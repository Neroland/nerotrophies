package za.co.neroland.nerotrophies.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/**
 * The Display Pedestal: one item of any kind - a trophy or anything else. Comparator output by tier for a trophy,
 * 1 for any other item ({@link PedestalSignal}). Breaking it returns what it holds.
 */
public class PedestalBlockEntity extends DisplayBlockEntity {

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_PEDESTAL.get(), pos, state, 1);
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return anyItem(stack);
    }

    /** Comparator signal from the server's own resolution; a plain (non-trophy) item gives 1. */
    public int signal() {
        ItemStack stack = this.getItem(0);
        if (stack.isEmpty()) {
            return 0;
        }
        if (stampOf(stack) == null) {
            return PedestalSignal.of(true, java.util.Optional.empty(), true);
        }
        return this.view(0).map(v -> PedestalSignal.of(true, v.tier(), v.replica()))
                .orElse(PedestalSignal.of(true, java.util.Optional.empty(), true));
    }

    @Override
    protected boolean afterRefresh(net.minecraft.server.level.ServerLevel level,
            za.co.neroland.nerotrophies.data.TrophyState state) {
        level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        return false;
    }
}
