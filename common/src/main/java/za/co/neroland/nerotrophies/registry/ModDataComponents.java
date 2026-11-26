package za.co.neroland.nerotrophies.registry;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentType;
//?}
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * NeroTrophies' item data components. None of them carries personal data: a trophy is an id and a random token,
 * a finish is a palette id, a Book selection is a collection id.
 */
public final class ModDataComponents {

    //? if >=1.20.5 {
    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NeroTrophiesCommon.MOD_ID);

    /** {@code nerotrophies:trophy}: trophy id + claim token, nothing else. */
    public static final RegistryEntry<DataComponentType<TrophyStamp>> TROPHY =
            COMPONENTS.register(TrophyIds.COMPONENT_TROPHY, key -> DataComponentType.<TrophyStamp>builder()
                    .persistent(TrophyStamp.CODEC)
                    .networkSynchronized(TrophyStamp.STREAM_CODEC)
                    .build());

    /** {@code nerotrophies:finish}: a Core palette finish id kept by a broken display block. */
    public static final RegistryEntry<DataComponentType<Identifier>> FINISH =
            COMPONENTS.register(TrophyIds.COMPONENT_FINISH, key -> DataComponentType.<Identifier>builder()
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC)
                    .build());

    /** {@code nerotrophies:book_selection}: the collection a Trophy Book binds cabinets to. */
    public static final RegistryEntry<DataComponentType<Identifier>> BOOK_SELECTION =
            COMPONENTS.register(TrophyIds.COMPONENT_BOOK_SELECTION, key -> DataComponentType.<Identifier>builder()
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC)
                    .build());
    //?} else {
    /*public static final LegacyComponent<TrophyStamp> TROPHY = new LegacyComponent<>(
            "nerotrophies:trophy", TrophyStamp.CODEC);
    public static final LegacyComponent<Identifier> FINISH = new LegacyComponent<>(
            "nerotrophies:finish", Identifier.CODEC);
    public static final LegacyComponent<Identifier> BOOK_SELECTION = new LegacyComponent<>(
            "nerotrophies:book_selection", Identifier.CODEC);

    public record LegacyComponent<T>(String key, com.mojang.serialization.Codec<T> codec) {
        public T read(net.minecraft.world.item.ItemStack stack) {
            var tag = stack.getTag();
            var value = tag == null ? null : tag.get(key);
            return value == null ? null : codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, value).result().orElse(null);
        }

        public void write(net.minecraft.world.item.ItemStack stack, T value) {
            var tag = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, value).result()
                    .orElseThrow(() -> new IllegalArgumentException("Cannot write item key " + key));
            stack.getOrCreateTag().put(key, tag);
        }

        public void clear(net.minecraft.world.item.ItemStack stack) {
            stack.removeTagKey(key);
        }
    }
    *///?}

    private ModDataComponents() {
    }

    /** Classload-forcing no-op (common init step 3, first). */
    public static void init() {
    }
}
