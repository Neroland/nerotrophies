package za.co.neroland.nerotrophies.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/** Fabric entry point for NeroTrophies: common init, the payload channel, the server tick, commands and shutdown. */
public final class NeroTrophiesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] Fabric bootstrap");
        NeroTrophiesCommon.init();
        FabricTrophyNetwork.registerCommon();
        ServerTickEvents.END_SERVER_TICK.register(NeroTrophiesCommon::onServerTick);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> NeroTrophiesCommon.registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NeroTrophiesCommon.onServerStopped());
    }
}
