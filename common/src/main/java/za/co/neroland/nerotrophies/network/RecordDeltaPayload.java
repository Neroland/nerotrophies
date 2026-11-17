package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * S2C {@code nerotrophies:record_delta}: one entry of the receiver's own record changed (award, claim, re-claim), plus
 * the collections and trims as they now stand (small; keeps completion right without a full record).
 */
public record RecordDeltaPayload(RecordPayload.Entry entry, List<RecordPayload.CollectionView> collections,
        List<String> trims, long serverNow) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RecordDeltaPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_RECORD_DELTA);

    public static final StreamCodec<RegistryFriendlyByteBuf, RecordDeltaPayload> CODEC =
            StreamCodec.of(RecordDeltaPayload::write, RecordDeltaPayload::read);

    public RecordDeltaPayload {
        collections = List.copyOf(collections);
        trims = List.copyOf(trims);
    }

    /** Applies this delta to {@code base} (the client's current record). */
    public RecordPayload applyTo(RecordPayload base) {
        RecordPayload withEntry = base.with(entry, serverNow);
        return new RecordPayload(withEntry.trophies(), collections, trims, base.announce(), base.showName(), serverNow);
    }

    private static void write(RegistryFriendlyByteBuf buf, RecordDeltaPayload p) {
        RecordPayload.Entry.write(buf, p.entry);
        PayloadIo.writeCount(buf, p.collections.size(), RecordPayload.MAX_COLLECTIONS);
        for (RecordPayload.CollectionView c : p.collections) {
            RecordPayload.CollectionView.write(buf, c);
        }
        PayloadIo.writeCount(buf, p.trims.size(), RecordPayload.MAX_TRIMS);
        for (String t : p.trims) {
            buf.writeUtf(t, RecordPayload.MAX_TRIM);
        }
        buf.writeLong(p.serverNow);
    }

    private static RecordDeltaPayload read(RegistryFriendlyByteBuf buf) {
        RecordPayload.Entry entry = RecordPayload.Entry.read(buf);
        int c = PayloadIo.readCount(buf, RecordPayload.MAX_COLLECTIONS);
        List<RecordPayload.CollectionView> collections = new ArrayList<>(c);
        for (int i = 0; i < c; i++) {
            collections.add(RecordPayload.CollectionView.read(buf));
        }
        int t = PayloadIo.readCount(buf, RecordPayload.MAX_TRIMS);
        List<String> trims = new ArrayList<>(t);
        for (int i = 0; i < t; i++) {
            trims.add(buf.readUtf(RecordPayload.MAX_TRIM));
        }
        return new RecordDeltaPayload(entry, collections, trims, buf.readLong());
    }

    @Override
    public CustomPacketPayload.Type<RecordDeltaPayload> type() {
        return TYPE;
    }
}
