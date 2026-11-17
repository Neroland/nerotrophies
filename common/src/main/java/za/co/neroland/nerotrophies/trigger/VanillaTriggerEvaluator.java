package za.co.neroland.nerotrophies.trigger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.nerolandcore.economy.CurrencyEvents;
import za.co.neroland.nerolandcore.progression.GateEvents;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneEvents;
import za.co.neroland.nerolandcore.reputation.ReputationApi;
import za.co.neroland.nerolandcore.reputation.ReputationEvents;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.engine.EngineServer;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;
import za.co.neroland.nerotrophies.engine.TrophyEngine;

/**
 * The installed {@link TriggerEvaluator}. Live: the advancement mixin queue (drained each tick), Core's gate,
 * material-milestone, reputation and currency buses, a per-tick dimension-change check, and the staggered
 * {@link PeriodicPass} for statistics, items, blocks and dimensions. Retroactive and progress reads go through
 * {@link TriggerRules} over {@link VanillaPlayerFacts}. Awards are {@code LIVE} through
 * {@link TrophyEngine#awardLive}, which is idempotent; earned trophies are skipped first. Server thread only; nothing
 * here logs.
 */
final class VanillaTriggerEvaluator implements TriggerEvaluator {

    static final VanillaTriggerEvaluator INSTANCE = new VanillaTriggerEvaluator();

    private final PeriodicPass pass = new PeriodicPass();
    private final Map<UUID, Identifier> lastDimension = new HashMap<>();
    private volatile TriggerIndex index = TriggerIndex.EMPTY;
    private long ticks;

    private VanillaTriggerEvaluator() {
    }

    TriggerIndex index() {
        return index;
    }

    // ---- TriggerEvaluator ----------------------------------------------------------------------------------------

    @Override
    public boolean retroactive(ServerPlayer player, TrophyDefinition definition) {
        MinecraftServer server = player.level().getServer();
        if (server == null || definition.trigger().isEmpty()) {
            return false;
        }
        return TriggerRules.satisfied(definition.trigger().get(), new VanillaPlayerFacts(server, player));
    }

    @Override
    public Optional<Progress> progress(ServerPlayer player, TrophyDefinition definition) {
        MinecraftServer server = player.level().getServer();
        if (server == null || definition.hidden() || definition.trigger().isEmpty()) {
            return Optional.empty();
        }
        return TriggerRules.progress(definition.trigger().get(), new VanillaPlayerFacts(server, player));
    }

    @Override
    public void tick(MinecraftServer server) {
        ticks++;
        drainAdvancements(server);
        TriggerIndex idx = index;
        boolean dims = idx.hasDimensionTriggers();
        List<TrophyDefinition> polled = idx.polled();
        if (!dims && polled.isEmpty()) {
            return;
        }
        int interval = NeroTrophiesConfig.pollIntervalTicks();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            if (dims) {
                Identifier dim = p.level().dimension().identifier();
                if (!dim.equals(lastDimension.put(id, dim))) {
                    for (TrophyDefinition d : idx.byDimension(dim)) {
                        award(server, id, d.id());
                    }
                }
            }
            if (!polled.isEmpty() && pass.due(id, ticks, interval)) {
                VanillaPlayerFacts facts = new VanillaPlayerFacts(server, p);
                pass.visit(id, polled, new PeriodicPass.Visit() {
                    @Override
                    public boolean earned(TrophyDefinition definition) {
                        return TrophyEngine.INSTANCE.has(server, id, definition.id());
                    }

                    @Override
                    public boolean satisfied(TrophyDefinition definition) {
                        return TriggerRules.satisfied(definition.trigger().get(), facts);
                    }

                    @Override
                    public void award(TrophyDefinition definition) {
                        TrophyEngine.awardLive(server, id, definition.id());
                    }
                });
            }
        }
    }

    @Override
    public void definitionsChanged(MinecraftServer server) {
        TriggerIndex idx = new TriggerIndex(TrophyApi.definitions(server).trophies().values());
        index = idx;
        AdvancementQueue.setWanted(idx.advancementIds());
        VanillaPlayerFacts.clearCache();
        lastDimension.clear();
        pass.clear();
    }

    @Override
    public void playerLeft(MinecraftServer server, UUID player) {
        pass.forget(player);
        lastDimension.remove(player);
    }

    // ---- Core buses ----------------------------------------------------------------------------------------------

    void onGate(GateEvents.GateChange e) {
        MinecraftServer server = EngineServer.current();
        if (server != null) {
            NeroEventRules.onGate(index, e, (p, t) -> award(server, p, t));
        }
    }

    void onMilestone(MaterialMilestoneEvents.Change e) {
        MinecraftServer server = EngineServer.current();
        if (server != null) {
            NeroEventRules.onMilestone(index, e, (p, t) -> award(server, p, t));
        }
    }

    void onReputation(ReputationEvents.ReputationChange e) {
        MinecraftServer server = EngineServer.current();
        if (server != null) {
            NeroEventRules.onReputation(index, e, ReputationApi.hasRealProvider(), (p, t) -> award(server, p, t));
        }
    }

    void onCurrency(CurrencyEvents.BalanceChange e) {
        MinecraftServer server = EngineServer.current();
        if (server != null) {
            NeroEventRules.onCurrency(index, e, CurrencyApi.hasRealProvider(), (p, t) -> award(server, p, t));
        }
    }

    // ---- internals -----------------------------------------------------------------------------------------------

    private void drainAdvancements(MinecraftServer server) {
        for (AdvancementQueue.Completed c : AdvancementQueue.drain()) {
            for (TrophyDefinition d : index.byAdvancement(c.advancement())) {
                award(server, c.player(), d.id());
            }
        }
    }

    private static boolean award(MinecraftServer server, UUID player, Identifier trophy) {
        if (TrophyEngine.INSTANCE.has(server, player, trophy)) {
            return false;
        }
        return TrophyEngine.awardLive(server, player, trophy);
    }
}
