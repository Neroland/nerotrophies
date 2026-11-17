package za.co.neroland.nerotrophies.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/**
 * The Trophy Plaque: one item. A trophy is shown as a medallion over its title and earner; any other item sits in
 * the medallion under its own name, with no earner line. Breaking it returns what it holds.
 */
public class PlaqueBlockEntity extends DisplayBlockEntity {

    public PlaqueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY_PLAQUE.get(), pos, state, 1);
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return anyItem(stack);
    }
}
