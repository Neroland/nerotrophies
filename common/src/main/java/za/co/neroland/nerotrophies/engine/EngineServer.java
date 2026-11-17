package za.co.neroland.nerotrophies.engine;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * The engine's server-tick driver, identical on every loader (each loader calls {@link #tick} at the end of the server
 * tick and {@link #stopped} when the server stops). It detects logins and logouts by diffing the online list, watches Core's definition generation, runs the trigger evaluator's tick, the {@link RetroQueue} and the
 * name-sync tick, in that order. A failing hook is contained and logged once, without any player data.
 */
public final class EngineServer {

    /** Ticks between login and the retroactive pass, so other mods' login sync finishes first. */
    public static final int LOGIN_DELAY_TICKS = 40;

    private static final RetroQueue RETRO = new RetroQueue();
    /** Online players by id, with the player object seen, so a relog between two ticks is still a new session. */
    private static final Map<UUID, ServerPlayer> ONLINE = new HashMap<>();
    private static final Set<String> FAILED_ONCE = new HashSet<>();
    private static volatile WeakReference<MinecraftServer> server = new WeakReference<>(null);
    private static long ticks;
    private static long generation = Long.MIN_VALUE;
    private static List<TrophyDefinition> triggered = List.of();

    private EngineServer() {
    }

    /** The running server, if a tick or an award has seen one. */
    public static MinecraftServer current() {
        return server.get();
    }

    static void remember(MinecraftServer s) {
        if (server.get() != s) {
            server = new WeakReference<>(s);
        }
    }

    public static RetroQueue retro() {
        return RETRO;
    }

    /** End of every server tick. */
    public static void tick(MinecraftServer s) {
        remember(s);
        ticks++;
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(s);
        if (defs.generation() != generation) {
            boolean first = generation == Long.MIN_VALUE;
            generation = defs.generation();
            triggered = triggered(defs);
            guard("definitionsChanged", () -> EngineHooks.triggers().definitionsChanged(s));
            if (!first) {
                ONLINE.keySet().forEach(p -> RETRO.enqueue(p, ticks));
            }
        }
        presence(s);
        guard("triggers", () -> EngineHooks.triggers().tick(s));
        retroTick(s);
        guard("names", () -> EngineHooks.names().tick(s));
    }

    /** {@code /nerotrophies reload}: re-read the definitions and re-check every online player. */
    public static void reload(MinecraftServer s) {
        TrophyDefinitions.clear();
        TrophyApi.definitions(s);
        ONLINE.keySet().forEach(p -> RETRO.enqueue(p, ticks));
    }

    /**
     * The player's record was just erased: if they are online, award them nothing more this session. Their next login
     * runs a normal retroactive pass, so progress the game still holds comes back as {@code RETROACTIVE} (no coins, no
     * announcement) and the store does not re-record their gamertag while they are still playing.
     */
    public static void park(UUID player) {
        if (ONLINE.containsKey(player)) {
            RETRO.park(player);
        }
    }

    /** Server stopped: drop every in-memory reference (the store itself is saved by the game). */
    public static void stopped() {
        RETRO.clear();
        ONLINE.clear();
        FAILED_ONCE.clear();
        generation = Long.MIN_VALUE;
        triggered = List.of();
        ticks = 0;
        server = new WeakReference<>(null);
        TrophyDefinitions.clear();
    }

    private static List<TrophyDefinition> triggered(TrophyDefinitions.Snapshot defs) {
        List<TrophyDefinition> out = new ArrayList<>();
        for (TrophyDefinition d : defs.trophies().values()) {
            if (!d.awardOnly()) {
                out.add(d);
            }
        }
        out.sort(Comparator.comparing(d -> d.id().toString()));
        return List.copyOf(out);
    }

    private static void presence(MinecraftServer s) {
        List<ServerPlayer> players = s.getPlayerList().getPlayers();
        Set<UUID> now = new HashSet<>(players.size() * 2);
        for (ServerPlayer p : players) {
            UUID id = p.getUUID();
            now.add(id);
            ServerPlayer seen = ONLINE.get(id);
            if (seen != null && seen != p) {
                left(s, id); // logged out and back in between two ticks: a new session
            }
            if (!ONLINE.containsKey(id)) {
                ONLINE.put(id, p);
                guard("join", () -> TrophyEngine.INSTANCE.playerJoined(s, p));
                RETRO.enqueue(id, ticks + LOGIN_DELAY_TICKS);
                guard("namesJoin", () -> EngineHooks.names().playerJoined(p));
            }
        }
        if (now.size() != ONLINE.size()) {
            for (UUID gone : new ArrayList<>(ONLINE.keySet())) {
                if (!now.contains(gone)) {
                    left(s, gone);
                }
            }
        }
    }

    private static void left(MinecraftServer s, UUID gone) {
        ONLINE.remove(gone);
        RETRO.cancel(gone);
        guard("triggersLeave", () -> EngineHooks.triggers().playerLeft(s, gone));
        guard("namesLeave", () -> EngineHooks.names().playerLeft(s, gone));
    }

    private static void retroTick(MinecraftServer s) {
        if (RETRO.idle()) {
            return;
        }
        TrophyState state = TrophyState.get(s);
        RetroQueue.Checks checks = new RetroQueue.Checks() {
            @Override
            public boolean online(UUID player) {
                return s.getPlayerList().getPlayer(player) != null;
            }

            @Override
            public boolean has(UUID player, Identifier trophy) {
                return state.record(player).map(r -> r.has(trophy)).orElse(false);
            }

            @Override
            public boolean satisfied(UUID player, TrophyDefinition definition) {
                ServerPlayer p = s.getPlayerList().getPlayer(player);
                if (p == null) {
                    return false;
                }
                try {
                    return EngineHooks.triggers().retroactive(p, definition);
                } catch (RuntimeException e) {
                    failedOnce("retroactive", e);
                    return false;
                }
            }

            @Override
            public boolean award(UUID player, Identifier trophy) {
                return TrophyApi.award(s, player, trophy, AwardSource.RETROACTIVE);
            }
        };
        List<RetroQueue.Finished> done = RETRO.tick(ticks, triggered, generation, checks,
                NeroTrophiesConfig.retroPlayersPerTick(), NeroTrophiesConfig.retroChecksPerTick(),
                NeroTrophiesConfig.retroMillisPerTick() * 1_000_000L, System::nanoTime);
        for (RetroQueue.Finished f : done) {
            guard("retroFinish", () -> {
                TrophyEngine.INSTANCE.sweepCollections(s, f.player(), AwardSource.RETROACTIVE);
                ServerPlayer p = s.getPlayerList().getPlayer(f.player());
                if (p != null) {
                    if (f.awarded() > 0) {
                        EngineHooks.client().retroSummary(p, f.awarded());
                    }
                    EngineHooks.client().sendRecord(p);
                }
            });
        }
    }

    private static void guard(String what, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            failedOnce(what, e);
        }
    }

    private static void failedOnce(String what, RuntimeException e) {
        if (FAILED_ONCE.add(what)) {
            // The exception class only at WARN: a message could carry game data, so the full trace stays at DEBUG.
            NeroTrophiesCommon.LOGGER.warn("[NeroTrophies] engine hook '{}' failed; continuing ({})", what,
                    e.getClass().getName());
        }
        NeroTrophiesCommon.LOGGER.debug("[NeroTrophies] engine hook '{}' failure", what, e);
    }
}
