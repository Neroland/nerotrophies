package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * S2C {@code nerotrophies:names_update}: what the server resolved for tokens the client can currently see,
 * plus the tier and icon of trophies it can see (a physical trophy reveals both). Never a UUID; a name only with
 * {@link #NAMED}. At most {@value #MAX_ENTRIES} of each list.
 */
public record NamesUpdatePayload(List<Entry> names, List<View> views) implements CustomPacketPayload {

    public static final byte NAMED = 0;
    public static final byte BLANK = 1;
    public static final byte REPLICA = 2;
    public static final int MAX_ENTRIES = 256;

    /** One token: which trophy it was seen on, its status and (only for NAMED) the gamertag. */
    public record Entry(long token, Identifier trophy, byte status, String name) {
        public Entry {
            if (status < NAMED || status > REPLICA) {
                throw new IllegalArgumentException("status");
            }
            if (status != NAMED) {
                name = null;
            } else if (name == null || name.isEmpty() || name.length() > TrophyRecord.NAME_MAX) {
                throw new IllegalArgumentException("name");
            }
        }
    }

    /** A trophy's tier (ordinal, -1 unknown) and icon item id (null unknown). */
    public record View(Identifier trophy, byte tier, Identifier icon) {
    }

    public static final CustomPacketPayload.Type<NamesUpdatePayload> TYPE =
            TrophyNetwork.type(TrophyIds.PAYLOAD_NAMES_UPDATE);

    public static final StreamCodec<RegistryFriendlyByteBuf, NamesUpdatePayload> CODEC =
            StreamCodec.of(NamesUpdatePayload::write, NamesUpdatePayload::read);

    public NamesUpdatePayload {
        names = List.copyOf(names);
        views = List.copyOf(views);
        if (names.size() > MAX_ENTRIES || views.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("too many entries");
        }
    }

    public boolean isEmpty() {
        return names.isEmpty() && views.isEmpty();
    }

    private static void write(RegistryFriendlyByteBuf buf, NamesUpdatePayload p) {
        PayloadIo.writeCount(buf, p.names.size(), MAX_ENTRIES);
        for (Entry e : p.names) {
            buf.writeLong(e.token());
            PayloadIo.writeId(buf, e.trophy());
            buf.writeByte(e.status());
            if (e.status() == NAMED) {
                buf.writeUtf(e.name(), TrophyRecord.NAME_MAX);
            }
        }
        PayloadIo.writeCount(buf, p.views.size(), MAX_ENTRIES);
        for (View v : p.views) {
            PayloadIo.writeId(buf, v.trophy());
            buf.writeByte(v.tier());
            PayloadIo.writeOptionalId(buf, v.icon());
        }
    }

    private static NamesUpdatePayload read(RegistryFriendlyByteBuf buf) {
        int n = PayloadIo.readCount(buf, MAX_ENTRIES);
        List<Entry> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long token = buf.readLong();
            Identifier trophy = PayloadIo.readId(buf);
            byte status = buf.readByte();
            String name = status == NAMED ? buf.readUtf(TrophyRecord.NAME_MAX) : null;
            names.add(new Entry(token, trophy, status, name));
        }
        int m = PayloadIo.readCount(buf, MAX_ENTRIES);
        List<View> views = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            views.add(new View(PayloadIo.readId(buf), buf.readByte(), PayloadIo.readOptionalId(buf)));
        }
        return new NamesUpdatePayload(names, views);
    }

    @Override
    public CustomPacketPayload.Type<NamesUpdatePayload> type() {
        return TYPE;
    }
}
