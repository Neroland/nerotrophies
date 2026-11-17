package za.co.neroland.nerotrophies.block;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client seams for the shared item classes, installed by client init and inert on a dedicated server (which never
 * draws a tooltip or opens a screen). Keeps every client class out of common item code.
 */
public final class TrophyItemHooks {

    /** Adds the client-only tooltip lines of a trophy stack (earner, genuine / replica). */
    public static volatile BiConsumer<ItemStack, Consumer<Component>> trophyTooltip = (stack, out) -> {
    };
    /** The lang key of a trophy's title as this client knows it. */
    public static volatile Function<Identifier, String> trophyTitle = id -> defaultKey("trophy", id, "title");
    /** The lang key of a collection's title as this client knows it. */
    public static volatile Function<Identifier, String> collectionTitle = id -> defaultKey("collection", id, "title");
    /** Opens the Trophy Book screen (client). */
    public static volatile Runnable openBook = () -> {
    };

    private TrophyItemHooks() {
    }

    /** Core's default lang key for a definition: {@code <prefix>.<ns>.<path with / as .>.<suffix>}. */
    public static String defaultKey(String prefix, Identifier id, String suffix) {
        return prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.') + "." + suffix;
    }
}
