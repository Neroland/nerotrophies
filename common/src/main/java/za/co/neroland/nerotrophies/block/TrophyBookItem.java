package za.co.neroland.nerotrophies.block;

//? if <1.21.5 {
/*import java.util.List;
*///?}
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.registry.ModDataComponents;

/**
 * The Trophy Book: use it to open the book screen (client); sneak-use it on a cabinet to bind that cabinet's pool to
 * the collection selected in the book. The selection is a collection id on the stack
 * ({@code nerotrophies:book_selection}), nothing more.
 */
public class TrophyBookItem extends Item {

    public TrophyBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || !(level.getBlockEntity(context.getClickedPos()) instanceof CabinetBlockEntity cabinet)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Identifier selected = context.getItemInHand().get(ModDataComponents.BOOK_SELECTION.get());
            if (selected == null || TrophyApi.definitions(server.getServer()).collection(selected).isEmpty()) {
                DisplayBlock.actionBar(player, Component.translatable("nerotrophies.cabinet.choose_first"));
            } else {
                cabinet.bindPool(Optional.of(selected));
                DisplaySounds.play(level, context.getClickedPos(), DisplaySounds.CABINET_LATCH);
                String title = TrophyApi.definitions(server.getServer()).collection(selected).get().title();
                DisplayBlock.actionBar(player, Component.translatable("nerotrophies.cabinet.bound",
                        Component.translatable(title)));
            }
        }
        return InteractionResult.SUCCESS;
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            TrophyItemHooks.openBook.run();
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        if (level.isClientSide()) {
            TrophyItemHooks.openBook.run();
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),
                level.isClientSide());
    }
    *///?}

    private static void selectionLine(ItemStack stack, Consumer<Component> out) {
        Identifier selected = stack.get(ModDataComponents.BOOK_SELECTION.get());
        if (selected != null) {
            out.accept(Component.translatable("tooltip.nerotrophies.book.binds",
                    Component.translatable(TrophyItemHooks.collectionTitle.apply(selected)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    //? if >=1.21.5 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        selectionLine(stack, out);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        selectionLine(stack, lines::add);
    }
    *///?}
}
