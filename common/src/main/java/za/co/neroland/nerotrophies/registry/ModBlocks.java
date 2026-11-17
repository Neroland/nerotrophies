package za.co.neroland.nerotrophies.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import za.co.neroland.nerolandcore.registry.PropertiesCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies' six blocks (ids fixed by the owner on 2026-11-17). <b>Stage 2A placeholders:</b> plain blocks so the
 * ids, items, models and loot paths exist; the blocks work package replaces each factory with its own class from
 * {@code block/} (and owns this file from then on).
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NeroTrophiesCommon.MOD_ID);

    public static final RegistryEntry<Block> TROPHY = placeholder(TrophyIds.TROPHY);
    public static final RegistryEntry<Block> DISPLAY_PEDESTAL = placeholder(TrophyIds.DISPLAY_PEDESTAL);
    public static final RegistryEntry<Block> TROPHY_CABINET = placeholder(TrophyIds.TROPHY_CABINET);
    public static final RegistryEntry<Block> WALL_CABINET = placeholder(TrophyIds.WALL_CABINET);
    public static final RegistryEntry<Block> TROPHY_PLAQUE = placeholder(TrophyIds.TROPHY_PLAQUE);
    public static final RegistryEntry<Block> TROPHY_SPOTLIGHT = placeholder(TrophyIds.TROPHY_SPOTLIGHT);

    private ModBlocks() {
    }

    private static RegistryEntry<Block> placeholder(String id) {
        return BLOCKS.register(id, key -> new Block(PropertiesCompat.block(key)
                .strength(1.5F).sound(SoundType.METAL).noOcclusion()));
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
