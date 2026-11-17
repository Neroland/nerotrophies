package za.co.neroland.nerotrophies.network;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.ClientNotifier;
import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.platform.Services;

/**
 * The live {@link ClientNotifier}: {@code record}, {@code record_delta} and {@code award_toast}, each built from and
 * sent to the one player concerned. Never another player's record.
 */
public final class ServerClientNotifier implements ClientNotifier {

    public static final ServerClientNotifier INSTANCE = new ServerClientNotifier();

    private ServerClientNotifier() {
    }

    @Override
    public void sendRecord(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        Services.NETWORK.sendToPlayer(player, build(server, player));
    }

    /** The player's own record payload. */
    public static RecordPayload build(MinecraftServer server, ServerPlayer player) {
        return RecordBuilder.build(TrophyState.get(server), player.getUUID(), TrophyApi.definitions(server),
                d -> EngineHooks.triggers().progress(player, d), System.currentTimeMillis(),
                NeroTrophiesConfig.reclaimCooldownMillis());
    }

    @Override
    public void recordChanged(ServerPlayer player, Identifier trophy) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        TrophyDefinitions.Snapshot defs = TrophyApi.definitions(server);
        TrophyDefinition definition = defs.trophies().get(trophy);
        if (definition == null) {
            return;
        }
        TrophyRecord record = TrophyState.get(server).record(player.getUUID()).orElse(null);
        RecordPayload.Entry entry = RecordBuilder.entry(record, definition,
                d -> EngineHooks.triggers().progress(player, d), NeroTrophiesConfig.reclaimCooldownMillis());
        Services.NETWORK.sendToPlayer(player, new RecordDeltaPayload(entry, RecordBuilder.collections(record, defs),
                RecordBuilder.trims(record), System.currentTimeMillis()));
    }

    @Override
    public void awardToast(ServerPlayer player, Identifier trophy, TrophyTier tier) {
        Services.NETWORK.sendToPlayer(player, AwardToastPayload.trophy(trophy, tier));
    }

    @Override
    public void collectionToast(ServerPlayer player, Identifier collection) {
        Services.NETWORK.sendToPlayer(player, AwardToastPayload.collection(collection));
    }

    @Override
    public void retroSummary(ServerPlayer player, int count) {
        if (count > 0) {
            Services.NETWORK.sendToPlayer(player, AwardToastPayload.summary(count));
        }
    }
}
