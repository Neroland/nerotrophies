package za.co.neroland.nerotrophies.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Codec-test scaffolding for {@code nerotrophies:main} payloads: every payload declared in {@link TrophyPayloads} gets
 * a round-trip test through {@link #roundTrip}, which also fails if the codec leaves bytes unread.
 */
public final class PayloadCodecs {

    private PayloadCodecs() {
    }

    public static <T> T roundTrip(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        codec.encode(buf, value);
        T back = codec.decode(buf);
        if (buf.readableBytes() != 0) {
            throw new AssertionError(buf.readableBytes() + " byte(s) left unread");
        }
        return back;
    }
}
