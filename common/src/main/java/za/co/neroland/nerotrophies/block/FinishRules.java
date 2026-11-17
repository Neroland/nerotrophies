package za.co.neroland.nerotrophies.block;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * Setting a finish without NeroDecor: a vanilla dye sets {@code neroland:accent/<colour>}, one of four
 * Core materials sets the matching material finish. The table is explicit (never a material tag) and item ids are
 * matched as strings, so a missing item costs nothing. The item is not consumed.
 */
public final class FinishRules {

    private static final Map<String, String> MATERIALS = Map.of(
            "nerolandcore:nero_alloy_ingot", "neroland:nero_alloy",
            "nerolandcore:starsteel_ingot", "neroland:starsteel",
            "nerolandcore:void_crystal", "neroland:void_crystal",
            "nerolandcore:plasma_glass", "neroland:plasma_glass");

    private FinishRules() {
    }

    /** The finish {@code stack} sets, or empty if it sets none. */
    public static Optional<Identifier> finishFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return finishFor(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** {@link #finishFor(ItemStack)} by item id (pure; tested). */
    public static Optional<Identifier> finishFor(String itemId) {
        String material = MATERIALS.get(itemId);
        if (material != null) {
            return Optional.ofNullable(Identifier.tryParse(material));
        }
        if (itemId.startsWith("minecraft:") && itemId.endsWith("_dye")) {
            String colour = itemId.substring("minecraft:".length(), itemId.length() - "_dye".length());
            for (DyeColor dye : DyeColor.values()) {
                if (dye.getSerializedName().equals(colour)) {
                    return Optional.ofNullable(Identifier.tryParse("neroland:accent/" + colour));
                }
            }
        }
        return Optional.empty();
    }
}
