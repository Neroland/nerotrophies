package za.co.neroland.nerotrophies.forge;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.nerotrophies.client.particle.TrophyParticles;
import za.co.neroland.nerotrophies.client.toast.AwardFeedback;

/**
 * Forge client wiring for award feedback: particle providers, the award-toast listener, and the client tick for
 * delayed, placement, completion and idle particles. From 1.21.9 each event has its own static bus; 1.21.6-1.21.8
 * take mod-bus events from the bus group; before that from the mod bus. Called from the client dist only.
 */
public final class ForgeTrophyFeedbackClient {

    private ForgeTrophyFeedbackClient() {
    }

    public static void init(FMLJavaModLoadingContext context) {
        //? if >=1.21.9 {
        RegisterParticleProvidersEvent.BUS.addListener(ForgeTrophyFeedbackClient::particles);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> AwardFeedback.tick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> TrophyParticles.clear());
        //?} else if >=1.21.6 {
        /*RegisterParticleProvidersEvent.getBus(context.getModBusGroup()).addListener(ForgeTrophyFeedbackClient::particles);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> AwardFeedback.tick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> TrophyParticles.clear());
        *///?} else {
        /*context.getModEventBus().addListener(ForgeTrophyFeedbackClient::particles);
        //? if >=1.20.5 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent.Post event) -> AwardFeedback.tick());
        //?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END) AwardFeedback.tick();
                });
        //?}
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> TrophyParticles.clear());
        *///?}
        AwardFeedback.init();
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        TrophyParticles.registerAll((type, factory) -> event.registerSpriteSet(type, factory::apply));
    }
}
