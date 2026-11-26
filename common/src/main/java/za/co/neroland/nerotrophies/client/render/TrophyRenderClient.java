package za.co.neroland.nerotrophies.client.render;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.block.TrophyItemHooks;
import za.co.neroland.nerotrophies.blockentity.PlateState;
import za.co.neroland.nerotrophies.client.screen.TrophyBookScreen;
import za.co.neroland.nerotrophies.client.screen.TrophyKeys;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModDataComponents;

/**
 * The blocks and screens' client side, shared by every loader: the item hooks (tooltip, titles, open the Book), the
 * item-model property, and the key mapping's tick. Each loader's {@code *TrophyClientContent} registers the
 * renderers, screens, tints, layers and keys and calls {@link #init} and {@link #tick}. Client only.
 */
public final class TrophyRenderClient {

    private TrophyRenderClient() {
    }

    /** Physical client, once, before resources load. */
    public static void init() {
        TrophyItemHooks.trophyTooltip = TrophyRenderClient::tooltip;
        TrophyItemHooks.trophyTitle = TrophyLook::titleKey;
        TrophyItemHooks.collectionTitle = TrophyLook::collectionTitleKey;
        TrophyItemHooks.openBook = TrophyRenderClient::openBook;
        TrophyItemProperties.register();
    }

    /** End of every client tick: the Book key. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (TrophyKeys.OPEN_BOOK.consumeClick()) {
            //? if >=26.2 {
            boolean free = mc.gui.screen() == null;
            //?} else {
            /*boolean free = mc.screen == null;
            *///?}
            if (mc.player != null && free) {
                openBook();
            }
        }
    }

    public static void openBook() {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        mc.gui.setScreen(new TrophyBookScreen());
        //?} else {
        /*mc.setScreen(new TrophyBookScreen());
        *///?}
    }

    /** Tooltip lines of a trophy stack: tier, earner (only when named), genuine or replica. */
    static void tooltip(ItemStack stack, Consumer<Component> out) {
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        if (stamp == null) {
            out.accept(Component.translatable("tooltip.nerotrophies.replica").withStyle(style -> style.withColor((TrophyColours.REPLICA_TEXT) & 0xFFFFFF)));
            return;
        }
        TrophyLook.Look look = TrophyLook.of(stamp, Optional.empty());
        if (look.tier() != null) {
            out.accept(Component.translatable("tooltip.nerotrophies.tier",
                    Component.translatable("nerotrophies.tier." + look.tier().getSerializedName()))
                    .withStyle(style -> style.withColor((TrophyColours.TEXT_2) & 0xFFFFFF)));
        }
        if (look.replica()) {
            out.accept(Component.translatable("tooltip.nerotrophies.replica").withStyle(style -> style.withColor((TrophyColours.REPLICA_TEXT) & 0xFFFFFF)));
            return;
        }
        if ((look.plate() == PlateState.NAMED || look.plate() == PlateState.GALLERY) && look.name() != null) {
            out.accept(Component.translatable("tooltip.nerotrophies.earned_by", look.name())
                    .withStyle(style -> style.withColor((TrophyColours.TEXT) & 0xFFFFFF)));
        }
        out.accept(Component.translatable("tooltip.nerotrophies.genuine")
                .withStyle(style -> style.withColor(TrophyColours.ink(look.tier()) & 0xFFFFFF)));
    }
}
