package za.co.neroland.nerotrophies.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;

/**
 * A placed trophy: one stamp (trophy id + token). The server sets the display-only {@code tier} / {@code replica}
 * blockstate from its own token check on load and on a slow re-check; that state is never read as truth. Its loot
 * table returns the trophy with its stamp, so removal drops nothing extra.
 */
public class TrophyBlockEntity extends DisplayBlockEntity {

    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY.get(), pos, state, 1);
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.TROPHY.get());
    }

    @Override
    public boolean tintable() {
        return false;
    }

    @Override
    protected int refreshPeriod() {
        return 300;
    }

    @Override
    protected boolean dropsContentsOnRemoval() {
        return false;
    }

    /** The trophy's stamp, if it has one. */
    @Nullable
    public TrophyStamp stamp() {
        return stampOf(this.getItem(0));
    }

    @Override
    protected boolean afterRefresh(ServerLevel level, TrophyState state) {
        BlockState current = this.getBlockState();
        if (!current.hasProperty(DisplayProps.TIER)) {
            return false;
        }
        TrophyTier tier = this.view(0).flatMap(SlotView::tier).orElse(TrophyTier.BRONZE);
        boolean replica = this.view(0).map(SlotView::replica).orElse(true);
        BlockState next = current.setValue(DisplayProps.TIER, tier).setValue(DisplayProps.REPLICA, replica);
        if (next != current) {
            level.setBlock(this.worldPosition, next, 3);
            return true;
        }
        return false;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        TrophyStamp stamp = this.stamp();
        if (stamp != null) {
            components.set(ModDataComponents.TROPHY.get(), stamp);
        }
    }

    @Override
    protected void applyItemComponents(@Nullable TrophyStamp stamp) {
        if (stamp != null) {
            this.items.set(0, DisplayContents.stack(stamp));
        }
    }
}
