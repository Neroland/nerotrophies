package za.co.neroland.nerotrophies.network;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import za.co.neroland.nerotrophies.blockentity.DisplayIndex;
import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.engine.EngineHooks;

/**
 * Declares every {@code nerotrophies:main} payload (decision 009, 010 sections 2-3) and installs the live
 * {@link ServerNameSync} and {@link ServerClientNotifier} into {@link EngineHooks}.
 *
 * <p>S2C: {@code names_update}, {@code names_drop}, {@code names_reset}, {@code record}, {@code record_delta},
 * {@code award_toast}. C2S: {@code claim}, {@code set_toggle}, {@code book_refresh}, {@code select_collection}. No
 * payload carries a UUID; no C2S payload carries or requests a token or a name. Cabinet trims and collection binding
 * use the vanilla menu button path (010 section 1), not a payload. Client handlers live in
 * {@link ClientTrophyNet} and are only ever invoked on the physical client.
 */
public final class TrophyPayloads {

    private TrophyPayloads() {
    }

    /** Called once from {@link TrophyNetwork#init()}. */
    static void declare() {
        TrophyNetwork.clientbound(NamesUpdatePayload.TYPE, NamesUpdatePayload.CODEC, p -> ClientTrophyNet.handle(p));
        TrophyNetwork.clientbound(NamesDropPayload.TYPE, NamesDropPayload.CODEC, p -> ClientTrophyNet.handle(p));
        TrophyNetwork.clientbound(NamesResetPayload.TYPE, NamesResetPayload.CODEC, p -> ClientTrophyNet.handle(p));
        TrophyNetwork.clientbound(RecordPayload.TYPE, RecordPayload.CODEC, p -> ClientTrophyNet.handle(p));
        TrophyNetwork.clientbound(RecordDeltaPayload.TYPE, RecordDeltaPayload.CODEC, p -> ClientTrophyNet.handle(p));
        TrophyNetwork.clientbound(AwardToastPayload.TYPE, AwardToastPayload.CODEC, p -> ClientTrophyNet.handle(p));

        TrophyNetwork.serverbound(ClaimPayload.TYPE, ClaimPayload.CODEC, ServerPayloadHandlers::claim);
        TrophyNetwork.serverbound(SetTogglePayload.TYPE, SetTogglePayload.CODEC, ServerPayloadHandlers::setToggle);
        TrophyNetwork.serverbound(BookRefreshPayload.TYPE, BookRefreshPayload.CODEC, ServerPayloadHandlers::bookRefresh);
        TrophyNetwork.serverbound(SelectCollectionPayload.TYPE, SelectCollectionPayload.CODEC,
                ServerPayloadHandlers::selectCollection);

        ServerNameSync.setDisplayLookup(new ServerNameSync.DisplayLookup() {
            @Override
            public Map<ChunkPos, Set<BlockPos>> loaded(ServerLevel level) {
                return DisplayIndex.loaded(level);
            }

            @Override
            public List<TrophyStamp> stampsAt(ServerLevel level, BlockPos pos) {
                return DisplayIndex.stampsAt(level, pos);
            }
        });
        DisplayIndex.addListener((level, pos, tokens) -> ServerNameSync.displayChanged(level, pos));
        EngineHooks.setNameSync(ServerNameSync.INSTANCE);
        EngineHooks.setClientNotifier(ServerClientNotifier.INSTANCE);
    }
}
