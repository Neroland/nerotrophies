package za.co.neroland.nerotrophies.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * S2C {@code nerotrophies:award_toast} (decision 010 section 3), earner only. {@link #KIND_TROPHY}: {@code id} is the trophy,
 * {@code tier} set; {@link #KIND_COLLECTION}: {@code id} is the collection; {@link #KIND_RETRO_SUMMARY}: {@code count}
 * trophies recorded by a retroactive pass.
 */
public record AwardToastPayload(byte kind, Identifier id, TrophyTier tier, int count) implements CustomPacketPayload {

    public static final byte KIND_TROPHY = 0;
    public static final byte KIND_COLLECTION = 1;
    public static final byte KIND_RETRO_SUMMARY = 2;
    /** The id carried by a summary toast (ignored by the client). */
    public static final Identifier NO_ID = TrophyIds.id("summary");

    public static final CustomPacketPayload.Type<AwardToastPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_AWARD_TOAST);

    public static final StreamCodec<RegistryFriendlyByteBuf, AwardToastPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind());
                PayloadIo.writeId(buf, p.id());
                buf.writeByte(PayloadIo.tierByte(p.tier()));
                buf.writeVarInt(p.count());
            },
            buf -> new AwardToastPayload(buf.readByte(), PayloadIo.readId(buf), PayloadIo.tierOf(buf.readByte()),
                    buf.readVarInt()));

    public AwardToastPayload {
        if (kind < KIND_TROPHY || kind > KIND_RETRO_SUMMARY) {
            throw new IllegalArgumentException("kind");
        }
        count = Math.max(0, count);
    }

    public static AwardToastPayload trophy(Identifier trophy, TrophyTier tier) {
        return new AwardToastPayload(KIND_TROPHY, trophy, tier, 1);
    }

    public static AwardToastPayload collection(Identifier collection) {
        return new AwardToastPayload(KIND_COLLECTION, collection, null, 1);
    }

    public static AwardToastPayload summary(int count) {
        return new AwardToastPayload(KIND_RETRO_SUMMARY, NO_ID, null, count);
    }

    @Override
    public CustomPacketPayload.Type<AwardToastPayload> type() {
        return TYPE;
    }
}
