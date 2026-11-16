package za.co.neroland.nerotrophies.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/** NeoForge entry point for NeroTrophies. */
@Mod(NeroTrophiesCommon.MOD_ID)
public final class NeroTrophiesNeoForge {

    public NeroTrophiesNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] NeoForge bootstrap");
        NeroTrophiesCommon.init();
        // Common init created NeroTrophies' deferred registers through Core's registration seam; this attaches them
        // to our mod event bus.
        RegistrationProvider.attach(modEventBus);
    }
}
