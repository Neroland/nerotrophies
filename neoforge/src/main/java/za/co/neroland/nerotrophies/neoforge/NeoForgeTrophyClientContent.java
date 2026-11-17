package za.co.neroland.nerotrophies.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

import za.co.neroland.nerotrophies.client.render.CabinetRenderer;
import za.co.neroland.nerotrophies.client.render.DisplayTints;
import za.co.neroland.nerotrophies.client.render.PedestalRenderer;
import za.co.neroland.nerotrophies.client.render.PlaqueRenderer;
import za.co.neroland.nerotrophies.client.render.TrophyRenderClient;
import za.co.neroland.nerotrophies.client.render.TrophyRenderer;
import za.co.neroland.nerotrophies.client.screen.TrophyCabinetScreen;
import za.co.neroland.nerotrophies.client.screen.TrophyKeys;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;
//? if <1.21.4 {
/*import za.co.neroland.nerotrophies.registry.ModBlocks;
*///?}
import za.co.neroland.nerotrophies.registry.ModMenuTypes;

/**
 * NeoForge client wiring for the blocks and screens: renderers, cabinet screens, tints, the Book key and its tick.
 * Chunk layers come from the models' {@code render_type}. The entry point calls {@link #init} on the client dist.
 */
public final class NeoForgeTrophyClientContent {

    private NeoForgeTrophyClientContent() {
    }

    public static void init(IEventBus modBus) {
        TrophyRenderClient.init();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(ModBlockEntities.TROPHY.get(), ctx -> new TrophyRenderer());
            event.registerBlockEntityRenderer(ModBlockEntities.DISPLAY_PEDESTAL.get(), ctx -> new PedestalRenderer());
            event.registerBlockEntityRenderer(ModBlockEntities.TROPHY_CABINET.get(), ctx -> new CabinetRenderer());
            event.registerBlockEntityRenderer(ModBlockEntities.WALL_CABINET.get(), ctx -> new CabinetRenderer());
            event.registerBlockEntityRenderer(ModBlockEntities.TROPHY_PLAQUE.get(), ctx -> new PlaqueRenderer());
        });
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(ModMenuTypes.TROPHY_CABINET.get(), TrophyCabinetScreen::new);
            event.register(ModMenuTypes.WALL_CABINET.get(), TrophyCabinetScreen::new);
        });
        //? if >=26.1 {
        modBus.addListener((RegisterColorHandlersEvent.BlockTintSources event) ->
                event.register(java.util.List.copyOf(DisplayTints.sources()), DisplayTints.blocks()));
        //?} else {
        /*modBus.addListener((RegisterColorHandlersEvent.Block event) ->
                event.register(DisplayTints.FINISH, DisplayTints.blocks()));
        *///?}
        //? if <1.21.4 {
        /*modBus.addListener((RegisterColorHandlersEvent.Item event) ->
                event.register((stack, tintIndex) -> DisplayTints.itemColour(tintIndex),
                        ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_CABINET.get(),
                        ModBlocks.WALL_CABINET.get(), ModBlocks.TROPHY_PLAQUE.get()));
        *///?}
        modBus.addListener((RegisterKeyMappingsEvent event) -> TrophyKeys.all().forEach(event::register));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> TrophyRenderClient.tick());
    }
}
