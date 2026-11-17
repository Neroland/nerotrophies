package za.co.neroland.nerotrophies.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;

/** Fabric client glue for name sync: the in-memory name cache and own record are dropped on disconnect. */
public final class FabricTrophyClientEvents {

    private FabricTrophyClientEvents() {
    }

    /** Client init only. */
    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientTrophyNet.disconnected());
    }
}
