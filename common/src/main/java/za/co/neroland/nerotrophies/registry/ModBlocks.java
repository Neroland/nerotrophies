package za.co.neroland.nerotrophies.registry;

import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.PropertiesCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.block.DisplayPedestalBlock;
import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.block.TrophyBlock;
import za.co.neroland.nerotrophies.block.TrophyCabinetBlock;
import za.co.neroland.nerotrophies.block.TrophyPlaqueBlock;
import za.co.neroland.nerotrophies.block.TrophySpotlightBlock;
import za.co.neroland.nerotrophies.block.WallCabinetBlock;

/**
 * NeroTrophies' six blocks (ids fixed by the owner on 2026-11-17): the trophy, the Display Pedestal, the Trophy and
 * Wall Cabinets, the Trophy Plaque and the Trophy Spotlight. The four tintable ones emit their finish's light
 * ({@link DisplayProps#GLOW}); the spotlight emits 14.
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NeroTrophiesCommon.MOD_ID);

    public static final RegistryEntry<Block> TROPHY = BLOCKS.register(TrophyIds.TROPHY,
            key -> new TrophyBlock(PropertiesCompat.block(key).strength(0.8F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<Block> DISPLAY_PEDESTAL = display(TrophyIds.DISPLAY_PEDESTAL,
            DisplayPedestalBlock::new);
    public static final RegistryEntry<Block> TROPHY_CABINET = display(TrophyIds.TROPHY_CABINET,
            TrophyCabinetBlock::new);
    public static final RegistryEntry<Block> WALL_CABINET = display(TrophyIds.WALL_CABINET, WallCabinetBlock::new);
    public static final RegistryEntry<Block> TROPHY_PLAQUE = display(TrophyIds.TROPHY_PLAQUE, TrophyPlaqueBlock::new);
    public static final RegistryEntry<Block> TROPHY_SPOTLIGHT = BLOCKS.register(TrophyIds.TROPHY_SPOTLIGHT,
            key -> new TrophySpotlightBlock(PropertiesCompat.block(key).strength(0.8F).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(state -> 14)));

    private ModBlocks() {
    }

    private static RegistryEntry<Block> display(String id, Function<BlockBehaviour.Properties, Block> factory) {
        return BLOCKS.register(id, key -> factory.apply(PropertiesCompat.block(key).strength(1.5F)
                .sound(SoundType.METAL).noOcclusion().lightLevel(ModBlocks::glow)));
    }

    private static int glow(BlockState state) {
        return state.hasProperty(DisplayProps.GLOW) ? state.getValue(DisplayProps.GLOW) : 0;
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
