package za.co.neroland.nerotrophies.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.Currency;
import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.nerolandcore.economy.CurrencyProvider;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyReward;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyState;

/** Coins only with a real provider, only for live/API awards, once per record even across revoke. */
class CurrencyRewardsTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final CurrencyProvider original = CurrencyApi.provider();

    /** A stand-in for a real economy mod's provider. */
    private static final class RealProvider implements CurrencyProvider {
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
            return false;
        }

        @Override
        public void forgetPlayer(UUID player) {
            balances.remove(player);
        }
    }

    @AfterEach
    void restore() {
        CurrencyApi.setProvider(original);
    }

    private static TrophyDefinition gold() {
        return TestDefs.trophy("gold_one", TrophyTier.GOLD, Optional.empty(), Optional.of(new TrophyReward(40)));
    }

    @Test
    void noRealProviderMeansNothingIsPaid() {
        assertFalse(CurrencyApi.hasRealProvider(), "Core's in-memory default is not a real provider");
        TrophyState s = new TrophyState();
        s.award(A, gold().id(), 1L, 5L);
        long before = CurrencyApi.getBalance(A, CoreCurrencies.CREDITS);
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.LIVE, true, 40L));
        assertEquals(before, CurrencyApi.getBalance(A, CoreCurrencies.CREDITS));
        assertFalse(s.record(A).orElseThrow().paid(gold().id()));
    }

    @Test
    void realProviderPaysOnceForLiveAndApiOnly() {
        RealProvider economy = new RealProvider();
        CurrencyApi.setProvider(economy);
        TrophyState s = new TrophyState();
        s.award(A, gold().id(), 1L, 5L);
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.LIVE, false, 40L), "disabled by config");
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.RETROACTIVE, true, 40L));
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.COMMAND, true, 40L));
        assertEquals(40L, CurrencyRewards.pay(s, A, gold(), AwardSource.LIVE, true, 40L));
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.API, true, 40L), "never twice");
        assertEquals(40L, economy.getBalance(A, CoreCurrencies.CREDITS));
        // Revoke and earn again: still never paid twice.
        assertTrue(s.revoke(A, gold().id()));
        s.award(A, gold().id(), 2L, 6L);
        assertEquals(0L, CurrencyRewards.pay(s, A, gold(), AwardSource.LIVE, true, 40L));
        assertEquals(40L, economy.getBalance(A, CoreCurrencies.CREDITS));
    }
}
