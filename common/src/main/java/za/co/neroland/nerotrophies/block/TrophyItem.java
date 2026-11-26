package za.co.neroland.nerotrophies.block;

//? if >=1.21.5 {
import java.util.function.Consumer;
//?} else {
/*import java.util.List;
*///?}

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModDataComponents;

/**
 * The one trophy item. Its name is the trophy's title; its tooltip (client) adds the tier, "Earned by" only when the
 * server pushed a name for this exact token, and "Genuine trophy" or "Replica". An anvil name is vanilla behaviour and
 * is never shown as the earner.
 */
public class TrophyItem extends BlockItem {

    public TrophyItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        if (stamp == null) {
            return super.getName(stack);
        }
        return Component.translatable(TrophyItemHooks.trophyTitle.apply(stamp.trophy()));
    }

    //? if >=1.21.5 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        TrophyItemHooks.trophyTooltip.accept(stack, out);
    }
    //?} else if >=1.20.5 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        TrophyItemHooks.trophyTooltip.accept(stack, lines::add);
    }
    *///?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        TrophyItemHooks.trophyTooltip.accept(stack, lines::add);
    }
    *///?}
}
