package za.co.neroland.nerotrophies.menu;

import java.util.OptionalInt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.telemetry.NeroTrophiesTelemetry;

/**
 * The single door every NeroTrophies menu goes through: {@code openMenu}, but it cannot take the server thread with
 * it when the host platform misbehaves (a Bukkit/Paper hybrid's inventory bridge choking on a modded menu). Catches
 * {@link RuntimeException} / {@link LinkageError} only, closes cleanly, tells the player, logs once by menu key and
 * reports it anonymously. Hybrid servers are not supported. The log and report never carry a player or a position.
 */
public final class MenuOpener {

    private MenuOpener() {
    }

    /** Opens {@code provider}; empty when the platform (or vanilla) refused. */
    public static OptionalInt open(ServerPlayer player, MenuProvider provider) {
        try {
            return player.openMenu(provider);
        } catch (RuntimeException | LinkageError failure) {
            recover(player, provider, failure);
            return OptionalInt.empty();
        }
    }

    private static void recover(ServerPlayer player, MenuProvider provider, Throwable cause) {
        String menu = describe(provider);
        NeroTrophiesCommon.LOGGER.warn("[NeroTrophies] The server platform refused to open menu '{}' ({}); the "
                + "interaction was cancelled. Hybrid (Bukkit/Paper) servers are not supported.", menu,
                cause.getClass().getName());
        try {
            player.closeContainer();
        } catch (RuntimeException | LinkageError ignored) {
            // The client resyncs on its own.
        }
        try {
            player.sendSystemMessage(Component.translatable("nerotrophies.menu.open_failed"));
        } catch (RuntimeException | LinkageError ignored) {
            // The player simply does not get the notice.
        }
        NeroTrophiesTelemetry.captureHandledException(cause, "menu_open", menu);
    }

    /** A label safe to log: our translation key, else the provider class. */
    private static String describe(MenuProvider provider) {
        try {
            Component title = provider.getDisplayName();
            if (title != null && title.getContents() instanceof TranslatableContents translatable) {
                return translatable.getKey();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through.
        }
        return provider.getClass().getName();
    }
}
