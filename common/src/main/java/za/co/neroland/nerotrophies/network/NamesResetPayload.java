package za.co.neroland.nerotrophies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/** S2C {@code nerotrophies:names_reset}: forget every name and view (login, dimension change). */
public record NamesResetPayload() implements CustomPacketPayload {

    public static final NamesResetPayload INSTANCE = new NamesResetPayload();

    public static final CustomPacketPayload.Type<NamesResetPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_NAMES_RESET);

    public static final StreamCodec<RegistryFriendlyByteBuf, NamesResetPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<NamesResetPayload> type() {
        return TYPE;
    }
}
