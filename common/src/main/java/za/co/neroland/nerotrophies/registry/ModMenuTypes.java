package za.co.neroland.nerotrophies.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies' menu types. <b>Empty in Stage 2A.</b> The UI work package registers the cabinet menus here (classes in
 * {@code menu/}, opened only through one {@code menu.MenuOpener}).
 */
public final class ModMenuTypes {

    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NeroTrophiesCommon.MOD_ID);

    private ModMenuTypes() {
    }

    /** Classload-forcing no-op (common init step 3). */
    public static void init() {
    }
}
