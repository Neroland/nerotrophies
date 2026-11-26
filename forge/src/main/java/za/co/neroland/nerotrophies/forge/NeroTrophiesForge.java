package za.co.neroland.nerotrophies.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.client.TrophyClient;

/** MinecraftForge entry point for NeroTrophies. */
@Mod(NeroTrophiesCommon.MOD_ID)
public final class NeroTrophiesForge {

    //? if >=1.20.5 {
    public NeroTrophiesForge(FMLJavaModLoadingContext context) {
    //?} else {
    /*public NeroTrophiesForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
    *///?}
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] Forge bootstrap");
        NeroTrophiesCommon.init();
        // Common init created NeroTrophies' deferred registers through Core's registration seam; this attaches them
        // to our mod bus group.
        //? if >=1.21.6 {
        RegistrationProvider.attach(context.getModBusGroup());
        //?} else {
        /*RegistrationProvider.attach(context.getModEventBus());
        *///?}
        ForgeTrophyNetwork.register();
        //? if >=1.21.9 {
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(
                event -> NeroTrophiesCommon.onServerTick(event.server()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(
                event -> NeroTrophiesCommon.registerCommands(event.getDispatcher()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(
                event -> NeroTrophiesCommon.onServerStopped());
        //?} else if >=1.21.6 {
        /*net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(
                event -> NeroTrophiesCommon.onServerTick(event.getServer()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(
                event -> NeroTrophiesCommon.registerCommands(event.getDispatcher()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(
                event -> NeroTrophiesCommon.onServerStopped());
        *///?} else {
        /*//? if >=1.20.5 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.TickEvent.ServerTickEvent.Post event) ->
                        NeroTrophiesCommon.onServerTick(event.getServer()));
        //?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.TickEvent.ServerTickEvent event) -> {
                    if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                        NeroTrophiesCommon.onServerTick(event.getServer());
                    }
                });
        //?}
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.RegisterCommandsEvent event) ->
                        NeroTrophiesCommon.registerCommands(event.getDispatcher()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.server.ServerStoppedEvent event) -> NeroTrophiesCommon.onServerStopped());
        *///?}
        // Client-only wiring; never loaded on a dedicated server.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            za.co.neroland.nerotrophies.forge.network.ForgeTrophyClientEvents.register();
            //? if >=1.21.6 {
            ForgeTrophyClientContent.init(context.getModBusGroup());
            //?} else {
            /*ForgeTrophyClientContent.init(context.getModEventBus());
            *///?}
            TrophyClient.init();
            ForgeTrophyFeedbackClient.init(context);
        }
    }
}
