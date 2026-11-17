package za.co.neroland.nerotrophies.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.data.TrophyExport;
import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.Announcements;
import za.co.neroland.nerotrophies.engine.EngineServer;
import za.co.neroland.nerotrophies.engine.TrophyEngine;

/**
 * {@code /nerotrophies}: {@code list}, {@code announce on|off}, {@code showname on|off}, {@code export} for every
 * player; {@code list <player>}, {@code award}, {@code revoke}, {@code reload} for operators (level 2) and
 * {@code export <player>} for admins (level 3). Output about a player goes only to the one who asked; nothing here
 * writes a name, UUID or token to the server log.
 *
 * <p><b>Registration point for later work:</b> {@link #extend} adds subcommands (the gallery adds
 * {@code gallery} / {@code gallery clear} here) before the loaders register commands.
 */
public final class TrophyCommands {

    private static final List<Consumer<LiteralArgumentBuilder<CommandSourceStack>>> EXTENSIONS = new ArrayList<>();

    private static final SuggestionProvider<CommandSourceStack> TROPHIES = (c, b) ->
            SharedSuggestionProvider.suggestResource(
                    TrophyApi.definitions(c.getSource().getServer()).trophies().keySet(), b);

    private TrophyCommands() {
    }

    /**
     * Adds subcommands under {@code /nerotrophies}. Call during common init. <b>Gallery:</b> register
     * {@code gallery} and {@code gallery clear} here (player-only, creative-only, op).
     */
    public static synchronized void extend(Consumer<LiteralArgumentBuilder<CommandSourceStack>> extension) {
        EXTENSIONS.add(extension);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(NeroTrophiesCommon.MOD_ID)
                .then(Commands.literal("list")
                        .executes(c -> listOwn(c))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(c -> listOther(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("award")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("trophy", IdentifierArgument.id()).suggests(TROPHIES)
                                        .executes(c -> award(c, EntityArgument.getPlayer(c, "player"),
                                                IdentifierArgument.getId(c, "trophy"))))))
                .then(Commands.literal("revoke")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("trophy", IdentifierArgument.id()).suggests(TROPHIES)
                                        .executes(c -> revoke(c, EntityArgument.getPlayer(c, "player"),
                                                IdentifierArgument.getId(c, "trophy"))))))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(TrophyCommands::reload))
                .then(Commands.literal("announce")
                        .then(Commands.literal("on").executes(c -> toggle(c, true, true)))
                        .then(Commands.literal("off").executes(c -> toggle(c, true, false))))
                .then(Commands.literal("showname")
                        .then(Commands.literal("on").executes(c -> toggle(c, false, true)))
                        .then(Commands.literal("off").executes(c -> toggle(c, false, false))))
                .then(Commands.literal("export")
                        .executes(c -> export(c, c.getSource().getPlayerOrException(), true))
                        .then(Commands.argument("player", EntityArgument.player())
                                //? if >=1.21.11 {
                                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                //?} else {
                                /*.requires(s -> s.hasPermission(Commands.LEVEL_ADMINS))
                                *///?}
                                .executes(c -> export(c, EntityArgument.getPlayer(c, "player"), false))));
        List<Consumer<LiteralArgumentBuilder<CommandSourceStack>>> extensions;
        synchronized (TrophyCommands.class) {
            extensions = List.copyOf(EXTENSIONS);
        }
        extensions.forEach(e -> e.accept(root));
        dispatcher.register(root);
    }

    private static int listOwn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        MinecraftServer server = c.getSource().getServer();
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(server);
        TrophyRecord record = TrophyState.get(server).record(player.getUUID()).orElse(null);
        int shown = 0;
        int total = defs.trophies().size();
        if (record != null) {
            for (TrophyRecord.Earned e : record.earnedEntries()) {
                TrophyDefinition def = defs.trophies().get(e.trophy());
                if (def != null) {
                    shown++;
                }
            }
        }
        final int count = shown;
        c.getSource().sendSuccess(() -> Component.translatable("nerotrophies.command.list.header", count, total), false);
        if (record != null) {
            for (TrophyRecord.Earned e : record.earnedEntries()) {
                TrophyDefinition def = defs.trophies().get(e.trophy());
                if (def != null) {
                    MutableComponent line = Component.translatable("nerotrophies.command.list.entry",
                            Component.translatable("nerotrophies.tier." + def.tier().getSerializedName()),
                            Announcements.title(def));
                    c.getSource().sendSuccess(() -> line, false);
                }
            }
        }
        return count;
    }

    /** Operator view: trophy ids and award times only, shown to the operator who asked. */
    private static int listOther(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        MinecraftServer server = c.getSource().getServer();
        TrophyRecord record = TrophyState.get(server).record(target.getUUID()).orElse(null);
        int n = record == null ? 0 : record.earnedEntries().size();
        c.getSource().sendSuccess(() -> Component.translatable("nerotrophies.command.list.other", n), false);
        if (record != null) {
            for (TrophyRecord.Earned e : record.earnedEntries()) {
                String line = e.trophy() + " @ " + java.time.Instant.ofEpochMilli(e.at());
                c.getSource().sendSuccess(() -> Component.literal(line), false);
            }
        }
        return n;
    }

    private static int award(CommandContext<CommandSourceStack> c, ServerPlayer target, Identifier trophy) {
        MinecraftServer server = c.getSource().getServer();
        if (TrophyApi.definition(server, trophy).isEmpty()) {
            c.getSource().sendFailure(Component.translatable("nerotrophies.command.unknown_trophy", trophy.toString()));
            return 0;
        }
        boolean added = TrophyApi.award(server, target.getUUID(), trophy, AwardSource.COMMAND);
        c.getSource().sendSuccess(() -> Component.translatable(added
                ? "nerotrophies.command.award.done" : "nerotrophies.command.award.already", trophy.toString()), false);
        return added ? 1 : 0;
    }

    private static int revoke(CommandContext<CommandSourceStack> c, ServerPlayer target, Identifier trophy) {
        boolean removed = TrophyApi.revoke(c.getSource().getServer(), target.getUUID(), trophy);
        c.getSource().sendSuccess(() -> Component.translatable(removed
                ? "nerotrophies.command.revoke.done" : "nerotrophies.command.revoke.missing", trophy.toString()), false);
        return removed ? 1 : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        MinecraftServer server = c.getSource().getServer();
        EngineServer.reload(server);
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(server);
        c.getSource().sendSuccess(() -> Component.translatable("nerotrophies.command.reload.done",
                defs.trophies().size(), defs.collections().size()), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> c, boolean announce, boolean value)
            throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        MinecraftServer server = c.getSource().getServer();
        if (announce) {
            TrophyEngine.INSTANCE.setAnnounce(server, player, value);
        } else {
            TrophyEngine.INSTANCE.setShowName(server, player, value);
        }
        String key = "nerotrophies.command." + (announce ? "announce" : "showname") + (value ? ".on" : ".off");
        c.getSource().sendSuccess(() -> Component.translatable(key), false);
        return 1;
    }

    /**
     * Export to the requesting player's chat as click-to-copy chunks; refused from the console so nothing lands in the
     * server log. {@code own} exports the requester's own record; otherwise an admin's export of {@code target}.
     */
    private static int export(CommandContext<CommandSourceStack> c, ServerPlayer target, boolean own)
            throws CommandSyntaxException {
        if (!(c.getSource().getEntity() instanceof ServerPlayer requester)) {
            c.getSource().sendFailure(Component.translatable("nerotrophies.command.export.players_only"));
            return 0;
        }
        MinecraftServer server = c.getSource().getServer();
        UUID subject = own ? requester.getUUID() : target.getUUID();
        String json = TrophyExport.toText(TrophyState.get(server), subject, TrophyApi.definitions(server));
        List<String> parts = TrophyExport.chunks(json, TrophyExport.CHUNK_BYTES);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            MutableComponent line = Component.translatable("nerotrophies.command.export.chunk", i + 1, parts.size())
                    .withStyle(style -> style.withClickEvent(copy(part)).withUnderlined(true));
            requester.sendSystemMessage(line);
        }
        return 1;
    }

    private static ClickEvent copy(String text) {
        //? if >=1.21.5 {
        return new ClickEvent.CopyToClipboard(text);
        //?} else {
        /*return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
        *///?}
    }
}
