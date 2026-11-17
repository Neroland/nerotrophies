package za.co.neroland.nerotrophies.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.menu.CabinetMenu;

/**
 * NeroTrophies' menu types: the two cabinet screens. Every menu opens through {@code menu.MenuOpener}. Plain vanilla
 * menu types (no extra open data): the client learns the cabinet's state through data slots only.
 */
public final class ModMenuTypes {

    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NeroTrophiesCommon.MOD_ID);

    public static final RegistryEntry<MenuType<CabinetMenu>> TROPHY_CABINET = MENUS.register(TrophyIds.TROPHY_CABINET,
            key -> new MenuType<>(CabinetMenu::floor, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<CabinetMenu>> WALL_CABINET = MENUS.register(TrophyIds.WALL_CABINET,
            key -> new MenuType<>(CabinetMenu::wall, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
