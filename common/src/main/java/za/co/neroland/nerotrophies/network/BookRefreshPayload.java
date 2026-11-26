package za.co.neroland.nerotrophies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/** C2S {@code nerotrophies:book_refresh}: send me my own record again (at most once per 5 s). */
public record BookRefreshPayload() implements CustomPacketPayload {

    public static final BookRefreshPayload INSTANCE = new BookRefreshPayload();

    public static final CustomPacketPayload.Type<BookRefreshPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_BOOK_REFRESH);

    public static final StreamCodec<RegistryFriendlyByteBuf, BookRefreshPayload> CODEC = StreamCodec.of((buf, ignored) -> { }, buf -> INSTANCE);

    @Override
    public CustomPacketPayload.Type<BookRefreshPayload> type() {
        return TYPE;
    }
}
