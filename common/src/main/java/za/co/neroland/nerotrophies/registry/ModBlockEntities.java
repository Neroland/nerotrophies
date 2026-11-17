package za.co.neroland.nerotrophies.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PlaqueBlockEntity;
import za.co.neroland.nerotrophies.blockentity.TrophyBlockEntity;

/**
 * NeroTrophies' block-entity types, one per trophy-bearing block, under the block ids. Every one stores
 * {@link za.co.neroland.nerotrophies.component.TrophyStamp}s at most (plus a finish, and for a cabinet a collection
 * id and trim), never a name or a UUID.
 */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NeroTrophiesCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<TrophyBlockEntity>> TROPHY = BLOCK_ENTITIES.register(
            TrophyIds.TROPHY, key -> BlockEntityTypeCompat.create(TrophyBlockEntity::new, ModBlocks.TROPHY.get()));
    public static final RegistryEntry<BlockEntityType<PedestalBlockEntity>> DISPLAY_PEDESTAL =
            BLOCK_ENTITIES.register(TrophyIds.DISPLAY_PEDESTAL, key -> BlockEntityTypeCompat.create(
                    PedestalBlockEntity::new, ModBlocks.DISPLAY_PEDESTAL.get()));
    public static final RegistryEntry<BlockEntityType<CabinetBlockEntity>> TROPHY_CABINET =
            BLOCK_ENTITIES.register(TrophyIds.TROPHY_CABINET, key -> BlockEntityTypeCompat.create(
                    CabinetBlockEntity::new, ModBlocks.TROPHY_CABINET.get()));
    public static final RegistryEntry<BlockEntityType<CabinetBlockEntity>> WALL_CABINET =
            BLOCK_ENTITIES.register(TrophyIds.WALL_CABINET, key -> BlockEntityTypeCompat.create(
                    CabinetBlockEntity::wall, ModBlocks.WALL_CABINET.get()));
    public static final RegistryEntry<BlockEntityType<PlaqueBlockEntity>> TROPHY_PLAQUE =
            BLOCK_ENTITIES.register(TrophyIds.TROPHY_PLAQUE, key -> BlockEntityTypeCompat.create(
                    PlaqueBlockEntity::new, ModBlocks.TROPHY_PLAQUE.get()));

    private ModBlockEntities() {
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
