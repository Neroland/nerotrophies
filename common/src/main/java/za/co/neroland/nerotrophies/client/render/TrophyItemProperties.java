package za.co.neroland.nerotrophies.client.render;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The item-model property the trophy sprites read, {@code nerotrophies:trophy_tier}: {@code <tier>} or
 * {@code <tier>_replica} from what this client knows of the stack's token (name sync, else the player's own record),
 * empty when unknown (the "unknown" sprite). From 1.21.4 a {@code select} property for {@code items/trophy.json}; on
 * 1.21.1 a clamped float for the {@code models/item/trophy.json} overrides (0.1-0.4 tiers, 0.5-0.8 replicas). The trophy
 * item is in no highlight tag: its sprite carries the tier. Client only.
 */
public final class TrophyItemProperties {

    public static final Identifier TROPHY_TIER = TrophyIds.id("trophy_tier");

    private TrophyItemProperties() {
    }

    /** The property value of {@code stack}. */
    public static String tierKey(ItemStack stack) {
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        if (stamp == null) {
            return "";
        }
        TrophyLook.Look look = TrophyLook.of(stamp, Optional.empty());
        if (look.tier() == null) {
            return "";
        }
        return look.tier().getSerializedName() + (look.replica() ? "_replica" : "");
    }

    /** The 1.21.1 override value of {@code stack}. */
    public static float legacyValue(ItemStack stack) {
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        if (stamp == null) {
            return 0.0F;
        }
        TrophyLook.Look look = TrophyLook.of(stamp, Optional.empty());
        if (look.tier() == null) {
            return 0.0F;
        }
        return (look.tier().ordinal() + 1 + (look.replica() ? 4 : 0)) / 10.0F;
    }

    public static void register() {
        //? if >=1.21.4 {
        put(TROPHY_TIER, TrophyTierProperty.TYPE);
        //?} else {
        /*net.minecraft.client.renderer.item.ItemProperties.registerGeneric(TROPHY_TIER,
                (stack, level, entity, seed) -> legacyValue(stack));
        *///?}
    }

    //? if >=1.21.4 {
    /**
     * Adds the select property type to vanilla's id mapper, found by type (the field is private outside Fabric's access
     * widener and Forge has no event for it); failing that the trophy shows its "unknown" sprite.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void put(Identifier id, Object type) {
        try {
            for (java.lang.reflect.Field field : net.minecraft.client.renderer.item.properties.select
                    .SelectItemModelProperties.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && net.minecraft.util.ExtraCodecs.LateBoundIdMapper.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((net.minecraft.util.ExtraCodecs.LateBoundIdMapper) field.get(null)).put(id, type);
                    return;
                }
            }
            NeroTrophiesCommon.LOGGER.warn("[NeroTrophies] item model property {} not registered: no id mapper", id);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NeroTrophiesCommon.LOGGER.warn("[NeroTrophies] item model property {} not registered ({})", id,
                    e.getClass().getSimpleName());
        }
    }

    /** {@code nerotrophies:trophy_tier}. */
    public record TrophyTierProperty()
            implements net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty<String> {
        public static final Type<TrophyTierProperty, String> TYPE = Type.create(
                com.mojang.serialization.MapCodec.unit(new TrophyTierProperty()),
                com.mojang.serialization.Codec.STRING);

        @Override
        public String get(ItemStack stack,
                @org.jetbrains.annotations.Nullable net.minecraft.client.multiplayer.ClientLevel level,
                @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity entity, int seed,
                net.minecraft.world.item.ItemDisplayContext context) {
            return tierKey(stack);
        }

        @Override
        public Type<TrophyTierProperty, String> type() {
            return TYPE;
        }
        //? if >=1.21.5 {

        @Override
        public com.mojang.serialization.Codec<String> valueCodec() {
            return com.mojang.serialization.Codec.STRING;
        }
        //?}
    }
    //?}
}
