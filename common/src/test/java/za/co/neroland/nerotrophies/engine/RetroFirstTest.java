package za.co.neroland.nerotrophies.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.Currency;
import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.nerolandcore.economy.CurrencyProvider;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyEraser;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * Progress that existed before a login, reload or definition change is always recorded as {@code RETROACTIVE}: a live
 * trigger that fires before the player's retroactive pass has finished gets {@code RETROACTIVE} too, so it never pays
 * or announces. An erased online player is parked (nothing awarded) until they log in again, and their re-earned old
 * progress then comes back retroactive, without coins. Genuine new progress after the pass is {@code LIVE} and pays.
 */
class RetroFirstTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final long COINS = 40L;
    private static final long NO_TIME_LIMIT = Long.MAX_VALUE;

    private final CurrencyProvider original = CurrencyApi.provider();

    @AfterEach
    void restore() {
        CurrencyApi.setProvider(original);
    }

    private static final class Economy implements CurrencyProvider {
        final Map<UUID, Long> balances = new HashMap<>();

        @Override
        public long getBalance(UUID player, Currency currency) {
            return balances.getOrDefault(player, 0L);
        }

        @Override
        public boolean deposit(UUID player, Currency currency, long amount) {
            balances.merge(player, amount, Long::sum);
            return true;
        }

        @Override
        public boolean withdraw(UUID player, Currency currency, long amount) {
            balances.merge(player, -amount, Long::sum);
            return true;
        }

        @Override
        public void forgetPlayer(UUID player) {
            balances.remove(player);
        }
    }

    /**
     * The engine's award path without a server: {@link TrophyEngine#awardLive} asks the queue for the source, the
     * post-award listener pays through {@link CurrencyRewards} and announces only {@code LIVE} awards.
     */
    private static final class World implements RetroQueue.Checks {
        final RetroQueue queue = new RetroQueue();
        final TrophyState state = new TrophyState();
        final Set<UUID> online = new HashSet<>();
        final Set<Identifier> met = new HashSet<>();
        final List<AwardSource> sources = new ArrayList<>();
        final List<Identifier> announced = new ArrayList<>();
        final List<TrophyDefinition> defs;
        long tick;
        long token = 100L;

        World(List<TrophyDefinition> defs) {
            this.defs = defs;
        }

        TrophyDefinition def(Identifier id) {
            return defs.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
        }

        boolean record(UUID player, Identifier trophy, AwardSource source) {
            if (has(player, trophy)) {
                return false;
            }
            state.award(player, trophy, tick, ++token);
            sources.add(source);
            CurrencyRewards.pay(state, player, def(trophy), source, true, COINS);
            if (source == AwardSource.LIVE) {
                announced.add(trophy);
            } else if (source == AwardSource.RETROACTIVE) {
                queue.noteAward(player);
            }
            return true;
        }

        /** What {@code TrophyEngine.awardLive} does. */
        boolean live(UUID player, Identifier trophy) {
            Optional<AwardSource> source = queue.liveSource(player);
            return source.isPresent() && record(player, trophy, source.get());
        }

        void login(UUID player) {
            online.add(player);
            queue.enqueue(player, tick + EngineServer.LOGIN_DELAY_TICKS);
        }

        void logout(UUID player) {
            online.remove(player);
            queue.cancel(player);
        }

        List<RetroQueue.Finished> run(int ticks) {
            List<RetroQueue.Finished> done = new ArrayList<>();
            for (int i = 0; i < ticks; i++) {
                tick++;
                done.addAll(queue.tick(tick, defs, 1, this, 2, 512, NO_TIME_LIMIT, System::nanoTime));
            }
            return done;
        }

        @Override
        public boolean online(UUID player) {
            return online.contains(player);
        }

        @Override
        public boolean has(UUID player, Identifier trophy) {
            return state.record(player).map(r -> r.has(trophy)).orElse(false);
        }

        @Override
        public boolean satisfied(UUID player, TrophyDefinition definition) {
            return met.contains(definition.id());
        }

        @Override
        public boolean award(UUID player, Identifier trophy) {
            return record(player, trophy, AwardSource.RETROACTIVE);
        }
    }

    private static List<TrophyDefinition> defs() {
        return List.of(TestDefs.trophy("old_stat", TrophyTier.GOLD), TestDefs.trophy("old_dimension", TrophyTier.GOLD),
                TestDefs.trophy("new_stat", TrophyTier.GOLD));
    }

    @Test
    void aLiveTriggerBeforeTheLoginPassIsRetroactive() {
        World w = new World(defs());
        w.met.add(TestDefs.id("old_stat"));
        w.met.add(TestDefs.id("old_dimension"));
        w.login(A);
        // The dimension check runs on the login tick, the periodic visit lands inside the 40-tick delay.
        w.run(1);
        assertTrue(w.live(A, TestDefs.id("old_dimension")));
        w.run(10);
        assertTrue(w.live(A, TestDefs.id("old_stat")));
        List<RetroQueue.Finished> done = w.run(EngineServer.LOGIN_DELAY_TICKS);
        assertEquals(List.of(AwardSource.RETROACTIVE, AwardSource.RETROACTIVE), w.sources);
        assertTrue(w.announced.isEmpty(), "nothing announced for old progress");
        assertEquals(1, done.size());
        assertEquals(2, done.get(0).awarded(), "the early finds are in the summary toast");
        assertEquals(Optional.of(AwardSource.LIVE), w.queue.liveSource(A), "after the pass, live is live");
    }

    @Test
    void aReloadMakesTheNextEvaluationRetroactiveToo() {
        World w = new World(defs());
        w.login(A);
        w.run(EngineServer.LOGIN_DELAY_TICKS + 2);
        assertEquals(Optional.of(AwardSource.LIVE), w.queue.liveSource(A));
        w.met.add(TestDefs.id("old_stat")); // a definition that is new in the reloaded pack, already met
        w.queue.enqueue(A, w.tick); // what a generation change or /nerotrophies reload does
        assertTrue(w.live(A, TestDefs.id("old_stat")));
        assertEquals(List.of(AwardSource.RETROACTIVE), w.sources);
    }

    @Test
    void eraseThenRejoinNeverPaysAgainAndNewProgressIsLive() {
        Economy economy = new Economy();
        CurrencyApi.setProvider(economy);
        World w = new World(defs());
        w.login(A);
        w.run(EngineServer.LOGIN_DELAY_TICKS + 2);
        // Genuine progress in the session: live, paid once, announced.
        w.met.add(TestDefs.id("old_stat"));
        assertTrue(w.live(A, TestDefs.id("old_stat")));
        assertEquals(COINS, economy.getBalance(A, CoreCurrencies.CREDITS));
        assertEquals(List.of(TestDefs.id("old_stat")), w.announced);
        economy.withdraw(A, CoreCurrencies.CREDITS, COINS); // the coins are moved out

        // /neroland data eraseme while online: the record goes and the player is parked.
        TrophyEraser eraser = new TrophyEraser(new TrophyEraser.Store() {
            @Override
            public TrophyState state(net.minecraft.server.MinecraftServer server) {
                return w.state;
            }

            @Override
            public void backup(net.minecraft.server.MinecraftServer server, TrophyState state) {
            }

            @Override
            public void park(UUID player) {
                w.queue.park(player);
            }
        });
        eraser.erase(null, A);
        assertFalse(w.state.hasRecord(A));
        for (int i = 0; i < 200; i++) {
            w.run(1);
            assertFalse(w.live(A, TestDefs.id("old_stat")), "a parked player is awarded nothing");
        }
        w.queue.enqueue(A, w.tick); // a reload while parked does not unpark
        w.run(5);
        assertFalse(w.state.hasRecord(A), "nothing recorded again while still online");

        // Rejoin: the old progress comes back retroactive, with no coins and no announcement.
        w.logout(A);
        w.login(A);
        w.run(1);
        assertTrue(w.live(A, TestDefs.id("old_stat")), "a live trigger before the pass");
        w.run(EngineServer.LOGIN_DELAY_TICKS + 2);
        assertEquals(0L, economy.getBalance(A, CoreCurrencies.CREDITS), "erase then rejoin never re-pays");
        assertEquals(List.of(TestDefs.id("old_stat")), w.announced, "and never re-announces");
        assertEquals(AwardSource.RETROACTIVE, w.sources.get(w.sources.size() - 1));

        // Genuine new progress after the first pass: live, paid and announced.
        w.met.add(TestDefs.id("new_stat"));
        assertTrue(w.live(A, TestDefs.id("new_stat")));
        assertEquals(AwardSource.LIVE, w.sources.get(w.sources.size() - 1));
        assertEquals(COINS, economy.getBalance(A, CoreCurrencies.CREDITS));
        assertEquals(List.of(TestDefs.id("old_stat"), TestDefs.id("new_stat")), w.announced);
    }

    @Test
    void parkingIsLiftedOnlyByLeaving() {
        RetroQueue q = new RetroQueue();
        q.park(A);
        assertTrue(q.isParked(A));
        assertTrue(q.liveSource(A).isEmpty());
        q.enqueue(A, 0);
        assertTrue(q.isParked(A), "enqueue keeps a parked player parked");
        q.cancel(A);
        assertFalse(q.isQueued(A));
        assertEquals(Optional.of(AwardSource.LIVE), q.liveSource(A));
    }
}
