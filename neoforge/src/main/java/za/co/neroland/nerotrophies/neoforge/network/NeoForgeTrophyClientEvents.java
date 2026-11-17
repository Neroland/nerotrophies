package za.co.neroland.nerotrophies.neoforge.network;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;

/** NeoForge client glue for name sync: the in-memory name cache and own record are dropped on disconnect (009). */
public final class NeoForgeTrophyClientEvents {

    private NeoForgeTrophyClientEvents() {
    }

    /** Physical client only. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientTrophyNet.disconnected());
    }
}
