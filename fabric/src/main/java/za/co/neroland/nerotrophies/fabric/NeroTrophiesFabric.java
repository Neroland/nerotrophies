package za.co.neroland.nerotrophies.fabric;

import net.fabricmc.api.ModInitializer;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/** Fabric entry point for NeroTrophies. */
public final class NeroTrophiesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] Fabric bootstrap");
        NeroTrophiesCommon.init();
    }
}
