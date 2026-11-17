package za.co.neroland.nerotrophies.link;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import net.minecraft.resources.Identifier;

/**
 * Deep-link-safe handles for trophies and collections. A trophy id holds {@code :} and {@code /}, which the bridge's
 * deep-link pattern forbids in an entity segment, so the link module names things by {@code t_}/{@code c_} plus the
 * first 16 hex characters of the SHA-256 of the id. Stable across restarts and reloads; the app maps them back through
 * the snapshot.
 */
public final class TrophyLinkKeys {

    /** The app's and the bridge's entity-segment rule. */
    public static final Pattern KEY = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    public static final String TROPHY_PREFIX = "t_";
    public static final String COLLECTION_PREFIX = "c_";
    /** Hidden unearned trophies get a positional handle that identifies nothing. */
    public static final String HIDDEN_PREFIX = "hidden_";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private TrophyLinkKeys() {
    }

    public static String trophy(Identifier id) {
        return TROPHY_PREFIX + hex16(id.toString());
    }

    public static String collection(Identifier id) {
        return COLLECTION_PREFIX + hex16(id.toString());
    }

    static String hex16(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                out.append(HEX[(digest[i] >> 4) & 0xF]).append(HEX[digest[i] & 0xF]);
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
