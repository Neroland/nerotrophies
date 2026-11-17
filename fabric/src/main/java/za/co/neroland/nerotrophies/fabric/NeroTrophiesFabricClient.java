package za.co.neroland.nerotrophies.fabric;

import net.fabricmc.api.ClientModInitializer;

import za.co.neroland.nerotrophies.client.TrophyClient;

/** Fabric client entry point: clientbound payload receivers, then the shared client init. */
public final class NeroTrophiesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricTrophyNetwork.registerClient();
        za.co.neroland.nerotrophies.fabric.network.FabricTrophyClientEvents.register();
        FabricTrophyClientContent.register();
        TrophyClient.init();
    }
}
