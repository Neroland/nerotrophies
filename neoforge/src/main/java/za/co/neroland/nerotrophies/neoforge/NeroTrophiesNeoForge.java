package za.co.neroland.nerotrophies.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.client.TrophyClient;

/** NeoForge entry point for NeroTrophies. */
@Mod(NeroTrophiesCommon.MOD_ID)
public final class NeroTrophiesNeoForge {

    public NeroTrophiesNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] NeoForge bootstrap");
        NeroTrophiesCommon.init();
        // Common init created NeroTrophies' deferred registers through Core's registration seam; this attaches them
        // to our mod event bus.
        RegistrationProvider.attach(modEventBus);
        NeoForgeTrophyNetwork.register(modEventBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                        NeroTrophiesCommon.onServerTick(event.getServer()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.RegisterCommandsEvent event) ->
                        NeroTrophiesCommon.registerCommands(event.getDispatcher()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> NeroTrophiesCommon.onServerStopped());
        // Client-only wiring; never loaded on a dedicated server.
        //? if >=1.21.9 {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
        //?} else {
        /*if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
        *///?}
            za.co.neroland.nerotrophies.neoforge.network.NeoForgeTrophyClientEvents.register();
            NeoForgeTrophyClientContent.init(modEventBus);
            TrophyClient.init();
        }
    }
}
