package za.co.neroland.nerotrophies.config;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies config schema, built on Neroland Core's config framework (file {@code config/nerotrophies.properties},
 * hot-reloadable via {@code /neroland config reload}). Registered once from {@link NeroTrophiesCommon#init()},
 * before anything else; every other subsystem reads it, including telemetry's opt-out flag.
 *
 * <p><b>POPIA/GDPR:</b> {@code telemetryEnabled} is deliberately <b>not</b> server-authoritative: anonymous
 * crash reporting is a per-client opt-out that a server must never force on or off.
 */
public final class NeroTrophiesConfig {

    public static final ConfigSchema SCHEMA =
            ConfigSchema.create(NeroTrophiesCommon.MOD_ID, "NeroTrophies configuration.");

    private static final ConfigValue<Boolean> TELEMETRY = SCHEMA.bool(
            "telemetryEnabled", true, false,
            "send anonymous, NeroTrophies-only crash reports (Sentry, EU servers) - ON by default. Sends the "
                    + "stack trace, mod/MC/loader/OS/Java versions and your other installed mods' ids "
                    + "and versions; never your IP, username, UUID, position, world data or chat; file paths "
                    + "are scrubbed of your account name. Set false to opt out of all of it (takes effect "
                    + "on restart). See PRIVACY.md");

    // ---- Engine. All server-authoritative. --------------------------------------------

    private static final ConfigValue<String> ANNOUNCE_TIERS = SCHEMA.string(
            "announceTiers", "gold,nero", true,
            "comma-separated trophy tiers whose live awards are announced in chat (bronze, silver, gold, nero); "
                    + "empty for none. A player is only named if their own announce and showname toggles are on");
    private static final ConfigValue<Integer> RECLAIM_COOLDOWN = SCHEMA.intRange(
            "reclaimCooldownSeconds", 600, 0, 86_400, true,
            "seconds a player must wait between re-claims of the same trophy from the Trophy Book");
    private static final ConfigValue<Integer> RETRO_PLAYERS = SCHEMA.intRange(
            "retroPlayersPerTick", 2, 1, 20, true,
            "retroactive re-check: new players started per server tick");
    private static final ConfigValue<Integer> RETRO_CHECKS = SCHEMA.intRange(
            "retroChecksPerTick", 512, 32, 8_192, true,
            "retroactive re-check: trophy definitions checked per server tick across all players");
    private static final ConfigValue<Integer> RETRO_MILLIS = SCHEMA.intRange(
            "retroMillisPerTick", 2, 1, 20, true,
            "retroactive re-check: wall-clock milliseconds it may spend per server tick");
    private static final ConfigValue<Integer> POLL_INTERVAL = SCHEMA.intRange(
            "pollIntervalTicks", 100, 20, 1_200, true,
            "ticks between checks of statistic, item, block and dimension trophies for each online player");
    private static final ConfigValue<Boolean> REWARDS = SCHEMA.bool(
            "rewardsEnabled", false, true,
            "pay a small coin reward through Neroland Core's currency for live trophy awards. Needs a real "
                    + "currency provider (such as NeroEconomy); otherwise nothing is paid");
    private static final ConfigValue<Integer> COINS_BRONZE = SCHEMA.intRange(
            "coinsBronze", 10, 0, 10_000, true, "coin reward for a bronze trophy (when rewardsEnabled)");
    private static final ConfigValue<Integer> COINS_SILVER = SCHEMA.intRange(
            "coinsSilver", 25, 0, 10_000, true, "coin reward for a silver trophy (when rewardsEnabled)");
    private static final ConfigValue<Integer> COINS_GOLD = SCHEMA.intRange(
            "coinsGold", 75, 0, 10_000, true, "coin reward for a gold trophy (when rewardsEnabled)");
    private static final ConfigValue<Integer> COINS_NERO = SCHEMA.intRange(
            "coinsNero", 200, 0, 10_000, true, "coin reward for a nero trophy (when rewardsEnabled)");

    private NeroTrophiesConfig() {
    }

    /** Whether a live award of {@code tier} may be announced. */
    public static boolean announces(TrophyTier tier) {
        return parseTiers(ANNOUNCE_TIERS.get()).contains(tier);
    }

    /** The tiers currently listed in {@code announceTiers}. */
    public static Set<TrophyTier> parseTiersConfigured() {
        return parseTiers(ANNOUNCE_TIERS.get());
    }

    /** Parses the {@code announceTiers} list; unknown names are ignored. */
    public static Set<TrophyTier> parseTiers(String raw) {
        Set<TrophyTier> out = EnumSet.noneOf(TrophyTier.class);
        if (raw == null) {
            return out;
        }
        for (String part : raw.split(",")) {
            String name = part.strip().toLowerCase(Locale.ROOT);
            for (TrophyTier tier : TrophyTier.values()) {
                if (tier.getSerializedName().equals(name)) {
                    out.add(tier);
                }
            }
        }
        return out;
    }

    public static long reclaimCooldownMillis() {
        return RECLAIM_COOLDOWN.get() * 1000L;
    }

    public static int retroPlayersPerTick() {
        return RETRO_PLAYERS.get();
    }

    public static int retroChecksPerTick() {
        return RETRO_CHECKS.get();
    }

    public static int retroMillisPerTick() {
        return RETRO_MILLIS.get();
    }

    public static int pollIntervalTicks() {
        return POLL_INTERVAL.get();
    }

    public static boolean rewardsEnabled() {
        return REWARDS.get();
    }

    /** The configured coin reward for {@code tier}. */
    public static int coinsFor(TrophyTier tier) {
        return switch (tier) {
            case BRONZE -> COINS_BRONZE.get();
            case SILVER -> COINS_SILVER.get();
            case GOLD -> COINS_GOLD.get();
            case NERO -> COINS_NERO.get();
        };
    }

    /**
     * Whether anonymous NeroTrophies-only crash reporting is on (default true, opt-out). Read once at bootstrap by
     * {@code NeroTrophiesTelemetry.init()}; changes take effect on restart.
     */
    public static boolean isTelemetryEnabled() {
        return TELEMETRY.get();
    }

    /** Registers the schema with Core's ConfigManager. Called once from common init. */
    public static void init() {
        ConfigManager.register(SCHEMA);
    }
}
