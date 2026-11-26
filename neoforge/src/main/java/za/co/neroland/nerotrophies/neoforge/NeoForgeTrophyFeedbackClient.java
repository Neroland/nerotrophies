package za.co.neroland.nerotrophies.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
//? if >=1.20.5 {
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

import za.co.neroland.nerotrophies.client.particle.TrophyParticles;
import za.co.neroland.nerotrophies.client.toast.AwardFeedback;

/**
 * NeoForge client wiring for award feedback: particle providers on the mod bus, the award-toast listener, and the
 * client tick for delayed, placement, completion and idle particles. Called from the client dist only.
 */
public final class NeoForgeTrophyFeedbackClient {

    private NeoForgeTrophyFeedbackClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener((RegisterParticleProvidersEvent event) ->
                TrophyParticles.registerAll((type, factory) -> event.registerSpriteSet(type, factory::apply)));
        AwardFeedback.init();
        //? if >=1.20.5 {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AwardFeedback.tick());
        //?} else {
        /*NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TickEvent.ClientTickEvent event) -> {
            if (event.phase == net.neoforged.neoforge.event.TickEvent.Phase.END) AwardFeedback.tick();
        });
        *///?}
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> TrophyParticles.clear());
    }
}
