package za.co.neroland.nerotrophies.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies' block-entity types. <b>Empty in Stage 2A.</b> The blocks work package registers the trophy, pedestal,
 * cabinet (floor and wall) and plaque types here, under the block ids in {@link TrophyIds}, with their classes in
 * {@code blockentity/}. Every one stores an {@link za.co.neroland.nerotrophies.component.TrophyStamp} at most, never
 * a name or a UUID.
 */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NeroTrophiesCommon.MOD_ID);

    private ModBlockEntities() {
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
