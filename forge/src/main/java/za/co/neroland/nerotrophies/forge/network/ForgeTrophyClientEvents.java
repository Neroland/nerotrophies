package za.co.neroland.nerotrophies.forge.network;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;

/** Forge client glue for name sync: the in-memory name cache and own record are dropped on disconnect (009). */
public final class ForgeTrophyClientEvents {

    private ForgeTrophyClientEvents() {
    }

    /** Physical client only. */
    public static void register() {
        //? if >=1.21.6 {
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientTrophyNet.disconnected());
        //?} else {
        /*net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> ClientTrophyNet.disconnected());
        *///?}
    }
}
