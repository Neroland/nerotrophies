package za.co.neroland.nerotrophies.client.screen;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
//? if >=1.21.9 {
import net.minecraft.resources.Identifier;
//?}

/** NeroTrophies' key mapping: open the Trophy Book (unbound by default, so it never steals a key). Client only. */
public final class TrophyKeys {

    //? if >=1.21.9 {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nerotrophies", "nerotrophies"));
    //?} else {
    /*public static final String CATEGORY = "key.categories.nerotrophies";
    *///?}

    public static final KeyMapping OPEN_BOOK =
            new KeyMapping("key.nerotrophies.open_book", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private TrophyKeys() {
    }

    public static List<KeyMapping> all() {
        return List.of(OPEN_BOOK);
    }
}
