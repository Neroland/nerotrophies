package za.co.neroland.nerotrophies.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotrophies.registry.ModBlockEntities;
import za.co.neroland.nerotrophies.registry.ModItems;

/** The Trophy Plaque: one trophy, shown as a medallion over its title and earner. Breaking it returns the trophy. */
public class PlaqueBlockEntity extends DisplayBlockEntity {

    public PlaqueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY_PLAQUE.get(), pos, state, 1);
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.TROPHY.get());
    }
}
