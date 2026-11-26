package za.co.neroland.nerotrophies.fabric;

//? if >=1.20.5 {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;

/**
 * Fabric side of the networking seam (the {@code nerotrophies:main} payloads). {@link #registerCommon()} (mod init,
 * both sides) registers the payload types and the serverbound receivers; {@link #registerClient()} (client init)
 * registers the clientbound receivers, keeping {@code ClientPlayNetworking} off the dedicated server. Registered as
 * the {@link NetworkPlatform} implementation via {@code META-INF/services}. Receivers hop to the client / server thread
 * before running a handler.
 */
public final class FabricTrophyNetwork implements NetworkPlatform {

    /** Mod-init (both sides): clientbound payload types, plus serverbound types and receivers. */
    public static void registerCommon() {
        for (TrophyNetwork.Clientbound<?> cb : TrophyNetwork.clientbound()) {
            registerClientboundType(cb);
        }
        for (TrophyNetwork.Serverbound<?> sb : TrophyNetwork.serverbound()) {
            registerServerbound(sb);
        }
    }

    /** Client-init: clientbound receivers (client-only API). */
    public static void registerClient() {
        for (TrophyNetwork.Clientbound<?> cb : TrophyNetwork.clientbound()) {
            registerClientReceiver(cb);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientboundType(TrophyNetwork.Clientbound<T> cb) {
        PayloadTypeRegistry.clientboundPlay().register(cb.type(), cb.codec());
    }

    private static <T extends CustomPacketPayload> void registerClientReceiver(TrophyNetwork.Clientbound<T> cb) {
        ClientPlayNetworking.registerGlobalReceiver(cb.type(), (payload, context) ->
                context.client().execute(() -> cb.handler().accept(payload)));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(TrophyNetwork.Serverbound<T> sb) {
        PayloadTypeRegistry.serverboundPlay().register(sb.type(), sb.codec());
        ServerPlayNetworking.registerGlobalReceiver(sb.type(), (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> sb.handler().accept(payload, player));
        });
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (!ServerPlayNetworking.canSend(player, payload.type())) {
            return false;
        }
        ServerPlayNetworking.send(player, payload);
        return true;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
//?} else {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec;

public final class FabricTrophyNetwork implements NetworkPlatform {
    public static void registerCommon() {
        TrophyNetwork.serverbound().forEach(FabricTrophyNetwork::registerServerbound);
    }

    public static void registerClient() {
        TrophyNetwork.clientbound().forEach(FabricTrophyNetwork::registerClientbound);
    }

    private static <T extends LegacyPayload> void registerServerbound(TrophyNetwork.Serverbound<T> packet) {
        ServerPlayNetworking.registerGlobalReceiver(packet.type().id(), (server, player, handler, buffer, sender) -> {
            T message = packet.codec().decode(buffer);
            server.execute(() -> packet.handler().accept(message, player));
        });
    }

    private static <T extends LegacyPayload> void registerClientbound(TrophyNetwork.Clientbound<T> packet) {
        ClientPlayNetworking.registerGlobalReceiver(packet.type().id(), (client, handler, buffer, sender) -> {
            T message = packet.codec().decode(buffer);
            client.execute(() -> packet.handler().accept(message));
        });
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        var id = payload.type().id();
        if (!ServerPlayNetworking.canSend(player, id)) {
            return false;
        }
        for (var packet : TrophyNetwork.clientbound()) {
            if (packet.type().equals(payload.type())) {
                ServerPlayNetworking.send(player, id, encodeClientbound(packet, payload));
                return true;
            }
        }
        throw new IllegalArgumentException("Unregistered clientbound payload " + id);
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var id = payload.type().id();
        if (!ClientPlayNetworking.canSend(id)) {
            return;
        }
        for (var packet : TrophyNetwork.serverbound()) {
            if (packet.type().equals(payload.type())) {
                ClientPlayNetworking.send(id, encodeServerbound(packet, payload));
                return;
            }
        }
        throw new IllegalArgumentException("Unregistered serverbound payload " + id);
    }

    private static <T extends LegacyPayload> FriendlyByteBuf encodeClientbound(
            TrophyNetwork.Clientbound<T> packet, LegacyPayload payload) {
        return encode(packet.type(), packet.codec(), payload);
    }

    private static <T extends LegacyPayload> FriendlyByteBuf encodeServerbound(
            TrophyNetwork.Serverbound<T> packet, LegacyPayload payload) {
        return encode(packet.type(), packet.codec(), payload);
    }

    private static <T extends LegacyPayload> FriendlyByteBuf encode(LegacyPayload.Type<T> type,
            LegacyStreamCodec<? super FriendlyByteBuf, T> codec, LegacyPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        codec.encode(buffer, type.payloadClass().cast(payload));
        return buffer;
    }
}
*///?}
