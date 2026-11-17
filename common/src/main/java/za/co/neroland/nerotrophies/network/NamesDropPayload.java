package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/** S2C {@code nerotrophies:names_drop}: forget these tokens and trophy views (no longer visible). */
public record NamesDropPayload(long[] tokens, List<Identifier> trophies) implements CustomPacketPayload {

    public static final int MAX = 1024;

    public static final CustomPacketPayload.Type<NamesDropPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_NAMES_DROP);

    public static final StreamCodec<RegistryFriendlyByteBuf, NamesDropPayload> CODEC =
            StreamCodec.of(NamesDropPayload::write, NamesDropPayload::read);

    public NamesDropPayload {
        tokens = tokens.clone();
        trophies = List.copyOf(trophies);
        if (tokens.length > MAX || trophies.size() > MAX) {
            throw new IllegalArgumentException("too many entries");
        }
    }

    public boolean isEmpty() {
        return tokens.length == 0 && trophies.isEmpty();
    }

    private static void write(RegistryFriendlyByteBuf buf, NamesDropPayload p) {
        PayloadIo.writeCount(buf, p.tokens.length, MAX);
        for (long t : p.tokens) {
            buf.writeLong(t);
        }
        PayloadIo.writeCount(buf, p.trophies.size(), MAX);
        for (Identifier id : p.trophies) {
            PayloadIo.writeId(buf, id);
        }
    }

    private static NamesDropPayload read(RegistryFriendlyByteBuf buf) {
        long[] tokens = new long[PayloadIo.readCount(buf, MAX)];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = buf.readLong();
        }
        int n = PayloadIo.readCount(buf, MAX);
        List<Identifier> trophies = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            trophies.add(PayloadIo.readId(buf));
        }
        return new NamesDropPayload(tokens, trophies);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NamesDropPayload other && java.util.Arrays.equals(tokens, other.tokens)
                && trophies.equals(other.trophies);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(tokens) * 31 + trophies.hashCode();
    }

    @Override
    public String toString() {
        return "NamesDropPayload[" + tokens.length + " tokens, " + trophies.size() + " views]";
    }

    @Override
    public CustomPacketPayload.Type<NamesDropPayload> type() {
        return TYPE;
    }
}
