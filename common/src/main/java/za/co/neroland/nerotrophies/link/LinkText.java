package za.co.neroland.nerotrophies.link;

import java.util.Locale;

import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;

/**
 * Resolves a lang key to the server's (English) text for link clients, which ship no mod lang files. A missing key
 * falls back to a humanised id path, never to the raw key.
 */
@FunctionalInterface
public interface LinkText {

    /** {@code key} as text, or {@code fallback} when the server's language does not hold it. */
    String resolve(String key, String fallback);

    /** The server's loaded language (the loaders add mod {@code en_us} files to it on a dedicated server). */
    LinkText SERVER = (key, fallback) -> {
        try {
            Language language = Language.getInstance();
            return language.has(key) ? language.getOrDefault(key) : fallback;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    };

    /** The fallback for a missing title: the id's last path segment, {@code _} to spaces, first letter upper. */
    static String humanise(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String last = (slash >= 0 ? path.substring(slash + 1) : path).replace('_', ' ').trim();
        if (last.isEmpty()) {
            return id.toString();
        }
        return last.substring(0, 1).toUpperCase(Locale.ROOT) + last.substring(1);
    }
}
