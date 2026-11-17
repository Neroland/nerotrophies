package za.co.neroland.nerotrophies.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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
 * Forge client wiring for the blocks and screens (renderers, cabinet screens, tints, the Book key and its tick).
 * Chunk layers come from the models' {@code render_type}. From 1.21.9 each event has its own static bus;
 * 1.21.6-1.21.8 take mod-bus events from the bus group; before that from the mod bus. The entry point calls
 * {@code init} on the client dist.
 */
public final class ForgeTrophyClientContent {

    private ForgeTrophyClientContent() {
    }

    private static void screens() {
        MenuScreens.register(ModMenuTypes.TROPHY_CABINET.get(), TrophyCabinetScreen::new);
        MenuScreens.register(ModMenuTypes.WALL_CABINET.get(), TrophyCabinetScreen::new);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TROPHY.get(), ctx -> new TrophyRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.DISPLAY_PEDESTAL.get(), ctx -> new PedestalRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.TROPHY_CABINET.get(), ctx -> new CabinetRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.WALL_CABINET.get(), ctx -> new CabinetRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.TROPHY_PLAQUE.get(), ctx -> new PlaqueRenderer());
    }

    private static void colours(RegisterColorHandlersEvent.Block event) {
        //? if >=26.1 {
        event.register(java.util.List.copyOf(DisplayTints.sources()), DisplayTints.blocks());
        //?} else {
        /*event.register(DisplayTints.FINISH, DisplayTints.blocks());
        *///?}
    }

    //? if >=1.21.9 {
    public static void init(net.minecraftforge.eventbus.api.bus.BusGroup modBus) {
        TrophyRenderClient.init();
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeTrophyClientContent::renderers);
        FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(ForgeTrophyClientContent::screens));
        RegisterColorHandlersEvent.Block.BUS.addListener(ForgeTrophyClientContent::colours);
        RegisterKeyMappingsEvent.BUS.addListener(event -> TrophyKeys.all().forEach(event::register));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> TrophyRenderClient.tick());
    }
    //?} else if >=1.21.6 {
    /*public static void init(net.minecraftforge.eventbus.api.bus.BusGroup modBus) {
        TrophyRenderClient.init();
        EntityRenderersEvent.RegisterRenderers.getBus(modBus).addListener(ForgeTrophyClientContent::renderers);
        FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(ForgeTrophyClientContent::screens));
        RegisterColorHandlersEvent.Block.getBus(modBus).addListener(ForgeTrophyClientContent::colours);
        RegisterKeyMappingsEvent.getBus(modBus).addListener(event -> TrophyKeys.all().forEach(event::register));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> TrophyRenderClient.tick());
    }
    *///?} else if >=1.21.4 {
    /*public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        TrophyRenderClient.init();
        modBus.addListener(ForgeTrophyClientContent::renderers);
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ForgeTrophyClientContent::screens));
        modBus.addListener(ForgeTrophyClientContent::colours);
        modBus.addListener((RegisterKeyMappingsEvent event) -> TrophyKeys.all().forEach(event::register));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent.Post event) -> TrophyRenderClient.tick());
    }
    *///?} else {
    /*public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        TrophyRenderClient.init();
        modBus.addListener(ForgeTrophyClientContent::renderers);
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ForgeTrophyClientContent::screens));
        modBus.addListener(ForgeTrophyClientContent::colours);
        modBus.addListener((RegisterKeyMappingsEvent event) -> TrophyKeys.all().forEach(event::register));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent.Post event) -> TrophyRenderClient.tick());
        modBus.addListener((RegisterColorHandlersEvent.Item event) ->
                event.register((stack, tintIndex) -> DisplayTints.itemColour(tintIndex),
                        ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_CABINET.get(),
                        ModBlocks.WALL_CABINET.get(), ModBlocks.TROPHY_PLAQUE.get()));
    }
    *///?}
}
