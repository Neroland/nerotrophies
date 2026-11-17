package za.co.neroland.nerotrophies.gallery;

import java.util.Optional;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.command.TrophyCommands;

/**
 * {@code /nerotrophies gallery} and {@code /nerotrophies gallery clear} (Stage 8, ART-PLAN section 9): a walkable
 * trophy hall built in front of the player that shows every loaded trophy, collection, finish, trim and display block,
 * and its exact removal. Operators only, players only, creative only. One hall per server at a time; building and
 * clearing run a budgeted slice per tick ({@link GalleryService}).
 *
 * <p>Every trophy in the hall is a display copy with a reserved token ({@code -1} placeholder name, {@code -2}
 * nameless, {@code 0} replica): it is never indexed, awarded, counted outside the hall's own display-only cabinets,
 * sent with a name, or reported to the link module. The hall never touches the trophy store.
 */
public final class TrophyGallery {

    private TrophyGallery() {
    }

    /** Common init step 7. */
    public static void init() {
        TrophyCommands.extend(root -> root.then(Commands.literal("gallery")
                .requires(TrophyGallery::allowed)
                .executes(TrophyGallery::build)
                .then(Commands.literal("clear").executes(TrophyGallery::clear))));
    }

    /** End of every server tick (from {@code NeroTrophiesCommon.onServerTick}): the build / clear slice and plates. */
    public static void tick(MinecraftServer server) {
        GalleryService.attach(server).tick();
    }

    /** Operator check. */
    private static boolean allowed(CommandSourceStack source) {
        //? if >=1.21.11 {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        //?} else {
        /*return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?}
    }

    /** The player who may use the gallery, or null after telling the source why not. */
    private static ServerPlayer user(CommandSourceStack source) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        Optional<String> refusal = GalleryAccess.refusal(player != null, player != null && player.isCreative());
        if (refusal.isPresent()) {
            source.sendFailure(Component.translatable(refusal.get()));
            return null;
        }
        return player;
    }

    private static int build(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        ServerPlayer player = user(source);
        if (player == null) {
            return 0;
        }
        MinecraftServer server = source.getServer();
        GalleryService service = GalleryService.attach(server);
        if (service.busy()) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.busy"));
            return 0;
        }
        if (service.store().snapshot().isPresent()) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.exists"));
            return 0;
        }
        GalleryCatalogue catalogue = GalleryCatalogue.of(TrophyApi.definitions(server));
        if (catalogue.isEmpty()) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.empty_catalogue"));
            return 0;
        }
        GalleryLayout.Result result = GalleryLayout.plan(catalogue);
        if (result.tooLarge()) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.too_large", GalleryFrame.WIDTH,
                    result.length(), GalleryLayout.MAX_WIDTH, GalleryLayout.MAX_LENGTH));
            return 0;
        }
        GalleryPlan plan = result.plan().get();
        ServerLevel level = player.level();
        GalleryFrame frame = GalleryFrame.inFrontOf(player.blockPosition(), player.getDirection(), plan.length());
        int floor = frame.origin().getY();
        if (level.isOutsideBuildHeight(floor) || level.isOutsideBuildHeight(floor + GalleryFrame.HEIGHT - 1)) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.no_room"));
            return 0;
        }
        service.start(GalleryJob.build(level.dimension().identifier(), frame, plan), level, player.getUUID());
        source.sendSuccess(() -> Component.translatable("nerotrophies.gallery.started", GalleryFrame.WIDTH,
                plan.length()), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        ServerPlayer player = user(source);
        if (player == null) {
            return 0;
        }
        MinecraftServer server = source.getServer();
        GalleryService service = GalleryService.attach(server);
        if (service.busy()) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.busy"));
            return 0;
        }
        Optional<GallerySnapshot> snapshot = service.store().snapshot();
        ServerLevel level = snapshot.map(s -> server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, s.dimension()))).orElse(null);
        if (snapshot.isEmpty() || level == null) {
            source.sendFailure(Component.translatable("nerotrophies.gallery.none"));
            return 0;
        }
        service.start(GalleryJob.clear(snapshot.get()), level, player.getUUID());
        source.sendSuccess(() -> Component.translatable("nerotrophies.gallery.clearing"), false);
        return 1;
    }
}
