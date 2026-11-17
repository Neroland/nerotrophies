package za.co.neroland.nerotrophies.network;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * C2S {@code nerotrophies:select_collection}: set or clear the collection on the Trophy Book in the
 * sender's main hand, else off hand.
 */
public record SelectCollectionPayload(Optional<Identifier> collection) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectCollectionPayload> TYPE =
            TrophyNetwork.type(TrophyIds.PAYLOAD_SELECT_COLLECTION);

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectCollectionPayload> CODEC = StreamCodec.of(
            (buf, p) -> PayloadIo.writeOptionalId(buf, p.collection().orElse(null)),
            buf -> new SelectCollectionPayload(Optional.ofNullable(PayloadIo.readOptionalId(buf))));

    @Override
    public CustomPacketPayload.Type<SelectCollectionPayload> type() {
        return TYPE;
    }
}
