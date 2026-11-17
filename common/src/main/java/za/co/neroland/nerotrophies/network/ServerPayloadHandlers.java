package za.co.neroland.nerotrophies.network;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.engine.ClaimService;
import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.engine.TrophyEngine;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Server handlers for every C2S payload. Each acts on the <b>sending</b> player only, is rate-limited
 * ({@link RateLimiter}, excess dropped silently) and validates its fields before touching anything. None of them
 * accepts or returns a token or a name.
 */
public final class ServerPayloadHandlers {

    static final RateLimiter LIMITER = new RateLimiter(System::nanoTime);

    private ServerPayloadHandlers() {
    }

    static void claim(ClaimPayload payload, ServerPlayer sender) {
        if (!LIMITER.tryAcquire(sender.getUUID()) || TrophyIds.isTrimId(payload.trophy())) {
            return;
        }
        ClaimService.claim(sender, payload.trophy());
    }

    static void setToggle(SetTogglePayload payload, ServerPlayer sender) {
        MinecraftServer server = sender.level().getServer();
        if (server == null || !LIMITER.tryAcquire(sender.getUUID()) || !payload.valid()) {
            return;
        }
        if (payload.toggle() == SetTogglePayload.ANNOUNCE) {
            TrophyEngine.INSTANCE.setAnnounce(server, sender, payload.value());
        } else {
            TrophyEngine.INSTANCE.setShowName(server, sender, payload.value());
        }
    }

    static void bookRefresh(BookRefreshPayload payload, ServerPlayer sender) {
        UUID id = sender.getUUID();
        if (LIMITER.tryAcquire(id) && LIMITER.tryRefresh(id)) {
            EngineHooks.client().sendRecord(sender);
        }
    }

    static void selectCollection(SelectCollectionPayload payload, ServerPlayer sender) {
        MinecraftServer server = sender.level().getServer();
        if (server == null || !LIMITER.tryAcquire(sender.getUUID())) {
            return;
        }
        Optional<Identifier> collection = payload.collection();
        if (collection.isPresent() && !TrophyApi.definitions(server).collections().containsKey(collection.get())) {
            return;
        }
        ItemStack book = bookIn(sender);
        if (book == null) {
            return;
        }
        if (collection.isPresent()) {
            book.set(ModDataComponents.BOOK_SELECTION.get(), collection.get());
        } else {
            book.remove(ModDataComponents.BOOK_SELECTION.get());
        }
    }

    /** The Trophy Book in the main hand, else the off hand, else null. */
    private static ItemStack bookIn(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.is(ModItems.TROPHY_BOOK.get())) {
                return stack;
            }
        }
        return null;
    }

    /** A player left: forget their budget. */
    static void forget(UUID player) {
        LIMITER.forget(player);
    }
}
