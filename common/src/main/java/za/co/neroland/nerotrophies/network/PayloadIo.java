package za.co.neroland.nerotrophies.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/** Bounded read/write helpers shared by the {@code nerotrophies:main} codecs. Every list length is checked on decode. */
final class PayloadIo {

    /** Longest lang key or category string carried. */
    static final int MAX_KEY = 256;

    private PayloadIo() {
    }

    static void writeId(FriendlyByteBuf buf, Identifier id) {
        Identifier.STREAM_CODEC.encode(buf, id);
    }

    static Identifier readId(FriendlyByteBuf buf) {
        return Identifier.STREAM_CODEC.decode(buf);
    }

    static int readCount(FriendlyByteBuf buf, int max) {
        int n = buf.readVarInt();
        if (n < 0 || n > max) {
            throw new DecoderException("nerotrophies payload list too long");
        }
        return n;
    }

    static void writeCount(FriendlyByteBuf buf, int n, int max) {
        if (n < 0 || n > max) {
            throw new IllegalArgumentException("nerotrophies payload list too long");
        }
        buf.writeVarInt(n);
    }

    /** {@code -1} for no tier, else the ordinal. */
    static byte tierByte(TrophyTier tier) {
        return tier == null ? (byte) -1 : (byte) tier.ordinal();
    }

    static TrophyTier tierOf(byte b) {
        TrophyTier[] all = TrophyTier.values();
        return b >= 0 && b < all.length ? all[b] : null;
    }

    static void writeOptionalId(FriendlyByteBuf buf, Identifier id) {
        buf.writeBoolean(id != null);
        if (id != null) {
            writeId(buf, id);
        }
    }

    static Identifier readOptionalId(FriendlyByteBuf buf) {
        return buf.readBoolean() ? readId(buf) : null;
    }

    static void writeOptionalString(FriendlyByteBuf buf, String s, int max) {
        buf.writeBoolean(s != null);
        if (s != null) {
            buf.writeUtf(s, max);
        }
    }

    static String readOptionalString(FriendlyByteBuf buf, int max) {
        return buf.readBoolean() ? buf.readUtf(max) : null;
    }
}
