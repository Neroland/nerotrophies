package za.co.neroland.nerotrophies.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;

/**
 * Forge side of the packet seam, on NeroTrophies' own {@code nerotrophies:main} channel. Registered as the
 * {@link NetworkPlatform} implementation via {@code META-INF/services}. Forge seals a channel at {@code build()}, so
 * {@link #register()} runs after common init, where the payloads are declared. {@code optional()} keeps a client
 * without NeroTrophies connectable. While no payload is declared the channel is not built and sends are refused.
 */
public final class ForgeTrophyNetwork implements NetworkPlatform {

    private static Channel<CustomPacketPayload> channel;

    public static void register() {
        if (!TrophyNetwork.hasPayloads()) {
            NeroTrophiesCommon.LOGGER.debug("[NeroTrophies] no payloads declared; nerotrophies:main channel not built");
            return;
        }
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play =
                ChannelBuilder.named(Identifier.fromNamespaceAndPath(NeroTrophiesCommon.MOD_ID, TrophyNetwork.CHANNEL))
                        .optional()
                        .payloadChannel()
                        .play()
                        .bidirectional();
        for (TrophyNetwork.Clientbound<?> cb : TrophyNetwork.clientbound()) {
            registerClientbound(play, cb);
        }
        for (TrophyNetwork.Serverbound<?> sb : TrophyNetwork.serverbound()) {
            registerServerbound(play, sb);
        }
        channel = play.build();
    }

    private static <T extends CustomPacketPayload> void registerClientbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play, TrophyNetwork.Clientbound<T> cb) {
        // A clientbound payload is only ever accepted from the server (no sender).
        play.addMain(cb.type(), registryCodec(cb.codec()), (payload, context) -> {
            if (context.getSender() == null) {
                cb.handler().accept(payload);
            }
        });
    }

    private static <T extends CustomPacketPayload> void registerServerbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play, TrophyNetwork.Serverbound<T> sb) {
        play.addMain(sb.type(), registryCodec(sb.codec()), (payload, context) -> {
            if (context.getSender() instanceof ServerPlayer serverPlayer) {
                sb.handler().accept(payload, serverPlayer);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> registryCodec(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (channel == null || !channel.isRemotePresent(player.connection.getConnection())) {
            return false;
        }
        channel.send(payload, PacketDistributor.PLAYER.with(player));
        return true;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (channel != null) {
            channel.send(payload, PacketDistributor.SERVER.noArg());
        }
    }
}
