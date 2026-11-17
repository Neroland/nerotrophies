package za.co.neroland.nerotrophies.gallery;

import java.util.List;
import java.util.Optional;

/**
 * One line of gallery text: a lang key, its arguments and an optional fallback for a key a third-party pack may not
 * ship (a category name). Arguments are nested {@link GalleryText}s or numbers only, so no free text and never a
 * player's name can reach a sign. Turned into a translatable component by the world side; pure here.
 */
public record GalleryText(String key, List<Object> args, Optional<String> fallback) {

    /** An empty sign line. */
    public static final GalleryText BLANK = new GalleryText("", List.of(), Optional.empty());

    public GalleryText {
        args = List.copyOf(args);
        for (Object arg : args) {
            if (!(arg instanceof GalleryText) && !(arg instanceof Integer)) {
                throw new IllegalArgumentException("gallery text arguments are lang keys or numbers only");
            }
        }
    }

    public static GalleryText of(String key, Object... args) {
        return new GalleryText(key, List.of(args), Optional.empty());
    }

    public static GalleryText withFallback(String key, String fallback) {
        return new GalleryText(key, List.of(), Optional.of(fallback));
    }

    /** This key with {@code args} instead of its own arguments. */
    public GalleryText withArgs(Object... args) {
        return new GalleryText(this.key, List.of(args), this.fallback);
    }

    public boolean blank() {
        return this.key.isEmpty();
    }
}
