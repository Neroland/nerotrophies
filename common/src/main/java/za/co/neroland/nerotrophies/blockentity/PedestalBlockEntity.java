package za.co.neroland.nerotrophies.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotrophies.registry.ModBlockEntities;
import za.co.neroland.nerotrophies.registry.ModItems;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The Display Pedestal: one trophy, or one item from the optional tag {@code nerotrophies:displayable}. Comparator
 * output by tier ({@link PedestalSignal}). Breaking it returns what it holds.
 */
public class PedestalBlockEntity extends DisplayBlockEntity {

    public static final TagKey<Item> DISPLAYABLE = TagKey.create(Registries.ITEM, TrophyIds.id("displayable"));

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_PEDESTAL.get(), pos, state, 1);
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.TROPHY.get()) || stack.is(DISPLAYABLE);
    }

    /** Comparator signal from the server's own resolution. */
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
