package za.co.neroland.nerotrophies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/** C2S {@code nerotrophies:claim}: claim (or re-claim) one of the sender's own trophies. Carries no token. */
public record ClaimPayload(Identifier trophy) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClaimPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_CLAIM);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimPayload> CODEC = StreamCodec.of(
            (buf, p) -> PayloadIo.writeId(buf, p.trophy()), buf -> new ClaimPayload(PayloadIo.readId(buf)));

    @Override
    public CustomPacketPayload.Type<ClaimPayload> type() {
        return TYPE;
    }
}
