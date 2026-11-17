package za.co.neroland.nerotrophies.engine;

import java.util.UUID;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyReward;

import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * The optional coin reward. Pays only when every condition holds: rewards enabled,
 * a <b>real</b> currency provider (Core's default in-memory provider accepts deposits, so this check is mandatory),
 * a {@code LIVE} or {@code API} award, a positive amount, and the trophy never paid before in this record.
 */
public final class CurrencyRewards {

    public static final String SOURCE_MOD = "nerotrophies";
    public static final String REASON = "trophy_award";

    private CurrencyRewards() {
    }

    /** Pays per config. Returns the amount paid (0 when nothing was paid). */
    public static long pay(TrophyState state, UUID player, TrophyDefinition definition, AwardSource source) {
        return pay(state, player, definition, source, NeroTrophiesConfig.rewardsEnabled(),
                definition.reward().map(TrophyReward::coins).orElseGet(() -> NeroTrophiesConfig.coinsFor(definition.tier())));
    }

    /** The rule with explicit settings (tests). */
    public static long pay(TrophyState state, UUID player, TrophyDefinition definition, AwardSource source,
            boolean enabled, long amount) {
        if (!enabled || amount <= 0 || !CurrencyApi.hasRealProvider()) {
            return 0L;
        }
        if (source != AwardSource.LIVE && source != AwardSource.API) {
            return 0L;
        }
        if (state.record(player).map(r -> r.paid(definition.id())).orElse(false)) {
            return 0L;
        }
        if (!CurrencyApi.deposit(player, CoreCurrencies.CREDITS, amount, SOURCE_MOD, REASON)) {
            return 0L;
        }
        state.markPaid(player, definition.id());
        return amount;
    }
}
