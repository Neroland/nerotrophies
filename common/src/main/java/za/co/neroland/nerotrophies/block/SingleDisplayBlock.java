package za.co.neroland.nerotrophies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayContents;

/** A one-slot display (pedestal, plaque): use with an accepted item puts one in; use with an empty hand takes it out. */
public abstract class SingleDisplayBlock extends DisplayBlock {

    protected SingleDisplayBlock(Properties properties) {
        super(properties);
    }

    protected Use insert(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DisplayBlockEntity display) || !display.getItem(0).isEmpty()
                || !display.accepts(0, stack)) {
            return Use.PASS;
        }
        if (!level.isClientSide()) {
            display.setItem(0, DisplayContents.normalise(stack));
            DisplaySounds.play(level, pos, DisplaySounds.PEDESTAL_SET);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return Use.SUCCESS;
    }

    @Override
    protected Use useEmpty(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DisplayBlockEntity display) || display.getItem(0).isEmpty()) {
            return Use.PASS;
        }
        if (!level.isClientSide()) {
            give(player, InteractionHand.MAIN_HAND, display.removeItem(0));
        }
        return Use.SUCCESS;
    }
}
