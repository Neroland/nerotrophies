package za.co.neroland.nerotrophies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/** C2S {@code nerotrophies:set_toggle}: the sender's own {@code announce} / {@code show_name} toggle. */
public record SetTogglePayload(byte toggle, boolean value) implements CustomPacketPayload {

    public static final byte ANNOUNCE = 0;
    public static final byte SHOW_NAME = 1;

    public static final CustomPacketPayload.Type<SetTogglePayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_SET_TOGGLE);

    public static final StreamCodec<RegistryFriendlyByteBuf, SetTogglePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.toggle());
                buf.writeBoolean(p.value());
            },
            buf -> new SetTogglePayload(buf.readByte(), buf.readBoolean()));

    public boolean valid() {
        return toggle == ANNOUNCE || toggle == SHOW_NAME;
    }

    @Override
    public CustomPacketPayload.Type<SetTogglePayload> type() {
        return TYPE;
    }
}
