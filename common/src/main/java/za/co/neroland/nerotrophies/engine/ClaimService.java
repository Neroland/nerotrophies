package za.co.neroland.nerotrophies.engine;

import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Claim and re-claim from the Trophy Book. The {@code claim} payload handler (network package) calls
 * {@link #claim(ServerPlayer, Identifier)} for the <em>sender</em> only. A first claim hands out the current token; a
 * re-claim mints a new one and retires the old, so the lost copy becomes a replica. Never pays coins, never fires
 * {@code Awarded}, never announces.
 */
public final class ClaimService {

    public enum Result {
        /** First copy handed out with the current token. */
        CLAIMED,
        /** A new token minted, the old one retired. */
        RECLAIMED,
        /** The sender has not earned it. */
        NOT_EARNED,
        /** Not a loaded definition. */
        UNKNOWN_TROPHY,
        /** Re-claim cooldown still running. */
        COOLDOWN,
        /** Trims and other non-trophy ids are not items. */
        NOT_CLAIMABLE
    }

    /** What happened; {@code token} is the one on the new item, {@code retired} the superseded one (0 if none). */
    public record Outcome(Result result, long token, long retired, long cooldownEndsAt) {
        static Outcome of(Result r) {
            return new Outcome(r, 0L, 0L, 0L);
        }

        public boolean gaveItem() {
            return result == Result.CLAIMED || result == Result.RECLAIMED;
        }
    }

    private ClaimService() {
    }

    /** The rule against a store (tests and the live path). */
    public static Outcome claim(TrophyState state, UUID player, Identifier trophy, boolean loaded, long now,
            long cooldownMillis, RandomGenerator random) {
        if (TrophyIds.isTrimId(trophy)) {
            return Outcome.of(Result.NOT_CLAIMABLE);
        }
        if (!loaded) {
            return Outcome.of(Result.UNKNOWN_TROPHY);
        }
        Optional<TrophyRecord.Earned> entry = state.record(player).flatMap(r -> r.earned(trophy));
        if (entry.isEmpty()) {
            return Outcome.of(Result.NOT_EARNED);
        }
        TrophyRecord.Earned e = entry.get();
        if (!e.claimed() && !TrophyStamp.isReserved(e.token())) {
            state.markClaimed(player, trophy, now);
            return new Outcome(Result.CLAIMED, e.token(), 0L, 0L);
        }
        if (e.claimed() && cooldownMillis > 0 && now - e.lastClaim() < cooldownMillis) {
            return new Outcome(Result.COOLDOWN, 0L, 0L, e.lastClaim() + cooldownMillis);
        }
        long fresh = TrophyTokens.mint(state, random);
        long old = state.replaceToken(player, trophy, fresh, now);
        return new Outcome(Result.RECLAIMED, fresh, TrophyStamp.isReserved(old) ? 0L : old, 0L);
    }

    /** Server entry point: validates, gives the item (dropped at the feet if the inventory is full), syncs, tells. */
    public static Outcome claim(ServerPlayer player, Identifier trophy) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return Outcome.of(Result.UNKNOWN_TROPHY);
        }
        TrophyState state = TrophyState.get(server);
        boolean loaded = TrophyApi.definition(server, trophy).isPresent();
        Outcome outcome = claim(state, player.getUUID(), trophy, loaded, System.currentTimeMillis(),
                NeroTrophiesConfig.reclaimCooldownMillis(), TrophyTokens.secureRandom());
        if (outcome.gaveItem()) {
            give(player, stamped(trophy, outcome.token()));
            if (outcome.retired() != 0L) {
                EngineHooks.names().tokensChanged(server, new long[] {outcome.retired(), outcome.token()});
            } else {
                EngineHooks.names().tokensChanged(server, new long[] {outcome.token()});
            }
            EngineHooks.client().recordChanged(player, trophy);
        }
        player.sendSystemMessage(feedback(outcome), true);
        return outcome;
    }

    /** A trophy item carrying exactly the stamp, nothing else. */
    public static ItemStack stamped(Identifier trophy, long token) {
        ItemStack stack = new ItemStack(ModItems.TROPHY.get());
        stack.set(ModDataComponents.TROPHY.get(), new TrophyStamp(trophy, token));
        return stack;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        drop.setNoPickUpDelay();
        player.level().addFreshEntity(drop);
    }

    private static Component feedback(Outcome outcome) {
        return switch (outcome.result()) {
            case CLAIMED -> Component.translatable("nerotrophies.claim.claimed");
            case RECLAIMED -> Component.translatable("nerotrophies.claim.reclaimed");
            case NOT_EARNED -> Component.translatable("nerotrophies.claim.not_earned");
            case UNKNOWN_TROPHY, NOT_CLAIMABLE -> Component.translatable("nerotrophies.claim.unknown");
            case COOLDOWN -> Component.translatable("nerotrophies.claim.cooldown",
                    Math.max(1L, (outcome.cooldownEndsAt() - System.currentTimeMillis() + 59_999L) / 60_000L));
        };
    }
}
