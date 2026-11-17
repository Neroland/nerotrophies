package za.co.neroland.nerotrophies.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;
import za.co.neroland.nerolandcore.trophy.TrophyProvider;

import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * NeroTrophies' {@link TrophyProvider}, registered with Core's {@link TrophyApi} at init. {@link #award} only records
 * (idempotent, mints the claim token); everything that follows an award (coins, announcement, toasts, collections)
 * runs in the {@link TrophyEvents#onAwarded} listener, so awards made by any mod through {@code TrophyApi.award} are
 * treated the same as NeroTrophies' own. Server thread only; nothing here logs a player.
 */
public final class TrophyEngine implements TrophyProvider {

    public static final TrophyEngine INSTANCE = new TrophyEngine();

    private static boolean installed;

    private TrophyEngine() {
    }

    /** Registers the provider and the post-award listener (common init). Idempotent. */
    public static synchronized void init() {
        if (installed) {
            return;
        }
        installed = true;
        TrophyApi.setProvider(INSTANCE);
        TrophyEvents.onAwarded(INSTANCE::afterAward);
    }

    /**
     * Award from a live trigger ({@code trigger/} calls this). The source is {@code LIVE} only once the player's
     * retroactive pass for this session (login, reload or definition change) has finished; until then it is
     * {@code RETROACTIVE}, so progress made before the pass can never pay coins, announce or push. A player whose record
     * was erased while online is awarded nothing until they log in again.
     */
    public static boolean awardLive(MinecraftServer server, UUID player, Identifier trophy) {
        Optional<AwardSource> source = EngineServer.retro().liveSource(player);
        if (source.isEmpty()) {
            return false;
        }
        return TrophyApi.award(server, player, trophy, source.get());
    }

    // ---- TrophyProvider -------------------------------------------------------------------------------------------

    @Override
    public boolean award(MinecraftServer server, UUID player, Identifier trophy, AwardSource source) {
        EngineServer.remember(server);
        TrophyState state = TrophyState.get(server);
        if (state.record(player).map(r -> r.has(trophy)).orElse(false)) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean added = state.award(player, trophy, now, TrophyTokens.mint(state)).added();
        if (added) {
            ServerPlayer online = server.getPlayerList().getPlayer(player);
            if (online != null) {
                state.refreshName(player, nameOf(online), now);
            }
        }
        return added;
    }

    @Override
    public boolean has(MinecraftServer server, UUID player, Identifier trophy) {
        return TrophyState.get(server).record(player)
                .map(r -> r.has(trophy) || r.completedCollections().contains(trophy))
                .orElse(false);
    }

    @Override
    public Set<Identifier> earned(MinecraftServer server, UUID player) {
        Optional<TrophyRecord> record = TrophyState.get(server).record(player);
        if (record.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> out = new LinkedHashSet<>();
        record.get().earnedEntries().forEach(e -> out.add(e.trophy()));
        out.addAll(record.get().completedCollections());
        return Set.copyOf(out);
    }

    @Override
    public boolean revoke(MinecraftServer server, UUID player, Identifier trophy) {
        TrophyState state = TrophyState.get(server);
        long token = state.record(player).flatMap(r -> r.earned(trophy)).map(TrophyRecord.Earned::token).orElse(0L);
        if (!state.revoke(player, trophy)) {
            return false;
        }
        if (token != 0L) {
            EngineHooks.names().invalidate(server, new long[] {token});
        }
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            EngineHooks.client().sendRecord(online);
        }
        return true;
    }

    // ---- After an award ----------------------------------------------------------------------------------------

    private void afterAward(TrophyEvents.Awarded event) {
        MinecraftServer server = EngineServer.current();
        if (server == null) {
            return;
        }
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(server);
        TrophyDefinition definition = defs.trophies().get(event.trophy());
        if (definition == null) {
            return;
        }
        UUID player = event.player();
        AwardSource source = event.source();
        TrophyState state = TrophyState.get(server);
        ServerPlayer online = server.getPlayerList().getPlayer(player);

        CurrencyRewards.pay(state, player, definition, source);
        if (online != null) {
            Announcements.maybeAnnounce(server, online, definition, source, state.record(player).orElse(null));
        }
        if (source == AwardSource.RETROACTIVE) {
            EngineServer.retro().noteAward(player);
        } else if (online != null) {
            EngineHooks.client().awardToast(online, definition.id(), definition.tier());
            EngineHooks.client().recordChanged(online, definition.id());
        }
        state.record(player).ifPresent(record ->
                completeCollections(server, state, player, CollectionRules.newlyCompletedBy(record, defs,
                        definition.id()), source));
    }

    /** Records every complete but unrecorded collection (end of a retroactive pass). */
    public void sweepCollections(MinecraftServer server, UUID player, AwardSource source) {
        TrophyState state = TrophyState.get(server);
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(server);
        state.record(player).ifPresent(record ->
                completeCollections(server, state, player, CollectionRules.allNewlyCompleted(record, defs), source));
    }

    private void completeCollections(MinecraftServer server, TrophyState state, UUID player,
            List<TrophyCollection> completed, AwardSource source) {
        for (TrophyCollection collection : completed) {
            if (!state.completeCollection(player, collection.id())) {
                continue;
            }
            if (!TrophyIds.STANDARD_TRIM.equals(collection.trim())) {
                state.award(player, TrophyIds.trimId(collection.trim()), System.currentTimeMillis(), 0L);
            }
            TrophyEvents.fireCollectionCompleted(
                    new TrophyEvents.CollectionCompleted(player, collection.id(), collection.trim()));
            ServerPlayer online = server.getPlayerList().getPlayer(player);
            if (online != null && source != AwardSource.RETROACTIVE) {
                EngineHooks.client().collectionToast(online, collection.id());
            }
            collection.trophy().ifPresent(trophy -> TrophyApi.award(server, player, trophy, source));
        }
    }

    // ---- Players ----------------------------------------------------------------------------------------------

    /** Login: refresh the stored gamertag of an existing record and re-push its names if it changed. */
    public void playerJoined(MinecraftServer server, ServerPlayer player) {
        TrophyState state = TrophyState.get(server);
        if (state.refreshName(player.getUUID(), nameOf(player), System.currentTimeMillis())) {
            EngineHooks.names().tokensChanged(server, state.tokensOf(player.getUUID()));
        }
    }

    /** {@code /nerotrophies announce on|off} and the Book checkbox. */
    public void setAnnounce(MinecraftServer server, ServerPlayer player, boolean value) {
        TrophyState.get(server).setAnnounce(player.getUUID(), value);
        EngineHooks.client().sendRecord(player);
    }

    /** {@code /nerotrophies showname on|off} and the Book checkbox: blanks or restores names the same tick. */
    public void setShowName(MinecraftServer server, ServerPlayer player, boolean value) {
        TrophyState state = TrophyState.get(server);
        state.setShowName(player.getUUID(), value);
        long[] tokens = state.tokensOf(player.getUUID());
        if (value) {
            state.refreshName(player.getUUID(), nameOf(player), System.currentTimeMillis());
            EngineHooks.names().tokensChanged(server, tokens);
        } else {
            EngineHooks.names().invalidate(server, tokens);
        }
        EngineHooks.client().sendRecord(player);
    }

    /** The player's gamertag (the account name, not a display name or nickname). */
    public static String nameOf(ServerPlayer player) {
        return player.getGameProfile().name();
    }
}
