package za.co.neroland.nerotrophies.block;

import net.minecraft.util.StringRepresentable;

/**
 * The six cabinet frame styles the art draws (the {@code trim} blockstate property of both cabinets). A trim id picks
 * a style plus an accent colour ({@link Trims}); the style is all the baked model needs.
 */
public enum TrimStyle implements StringRepresentable {
    PLATE("plate"),
    ORBITAL("orbital"),
    LAUREL("laurel"),
    WILD("wild"),
    NEON("neon"),
    PRISM("prism");

    private final String name;

    TrimStyle(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
