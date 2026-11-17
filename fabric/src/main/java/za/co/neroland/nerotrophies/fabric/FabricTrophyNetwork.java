package za.co.neroland.nerotrophies.fabric;

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
