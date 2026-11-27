package za.co.neroland.nerotrophies.neoforge;

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
//? if >=1.21.6 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?}
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;

/**
 * NeoForge side of the networking seam: registers every {@link TrophyNetwork} payload on NeroTrophies' own registrar
 * during {@code RegisterPayloadHandlersEvent} and implements the send methods. Registered as the
 * {@link NetworkPlatform} implementation via {@code META-INF/services}. The registrar is {@code optional()}, so a
 * client without NeroTrophies can still connect. Handlers run on the game thread.
 */
public final class NeoForgeTrophyNetwork implements NetworkPlatform {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeTrophyNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TrophyNetwork.PROTOCOL_VERSION).optional();
        for (TrophyNetwork.Clientbound<?> cb : TrophyNetwork.clientbound()) {
            registerClientbound(registrar, cb);
        }
        for (TrophyNetwork.Serverbound<?> sb : TrophyNetwork.serverbound()) {
            registerServerbound(registrar, sb);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar,
            TrophyNetwork.Clientbound<T> cb) {
        registrar.playToClient(cb.type(), cb.codec(),
                (payload, context) -> context.enqueueWork(() -> cb.handler().accept(payload)));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar,
            TrophyNetwork.Serverbound<T> sb) {
        registrar.playToServer(sb.type(), sb.codec(),
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        sb.handler().accept(payload, serverPlayer);
                    }
                }));
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (!player.connection.hasChannel(payload.type())) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, payload);
        return true;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        //? if >=1.21.6 {
        ClientPacketDistributor.sendToServer(payload);
        //?} else {
        /*PacketDistributor.sendToServer(payload);
        *///?}
    }
}
//?} else if >=1.20.4 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;

public final class NeoForgeTrophyNetwork implements NetworkPlatform {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeTrophyNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(NeroTrophiesCommon.MOD_ID).versioned("1").optional();
        TrophyNetwork.clientbound().forEach(packet -> registerClientbound(registrar, packet));
        TrophyNetwork.serverbound().forEach(packet -> registerServerbound(registrar, packet));
    }

    private static <T extends LegacyPayload> void registerClientbound(IPayloadRegistrar registrar,
            TrophyNetwork.Clientbound<T> packet) {
        registrar.play(packet.type().id(),
                buffer -> new WirePayload<>(packet.type(), packet.codec(), packet.codec().decode(buffer)),
                builder -> {
                    builder.client((payload, context) ->
                            context.workHandler().execute(() -> packet.handler().accept(payload.message())));
                });
    }

    private static <T extends LegacyPayload> void registerServerbound(IPayloadRegistrar registrar,
            TrophyNetwork.Serverbound<T> packet) {
        registrar.play(packet.type().id(),
                buffer -> new WirePayload<>(packet.type(), packet.codec(), packet.codec().decode(buffer)),
                builder -> {
                    builder.server((payload, context) -> context.workHandler().execute(() ->
                            context.player().ifPresent(player -> {
                                if (player instanceof ServerPlayer serverPlayer) {
                                    packet.handler().accept(payload.message(), serverPlayer);
                                }
                            })));
                });
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        if (!NetworkRegistry.getInstance().isConnected(player.connection, payload.type().id())) {
            return false;
        }
        for (TrophyNetwork.Clientbound<?> packet : TrophyNetwork.clientbound()) {
            if (packet.type().equals(payload.type())) {
                PacketDistributor.PLAYER.with(player).send(wrapClientbound(packet, payload));
                return true;
            }
        }
        throw new IllegalArgumentException("Unregistered clientbound payload " + payload.type().id());
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !NetworkRegistry.getInstance().isConnected(connection, payload.type().id())) {
            return;
        }
        for (TrophyNetwork.Serverbound<?> packet : TrophyNetwork.serverbound()) {
            if (packet.type().equals(payload.type())) {
                PacketDistributor.SERVER.noArg().send(wrapServerbound(packet, payload));
                return;
            }
        }
        throw new IllegalArgumentException("Unregistered serverbound payload " + payload.type().id());
    }

    private static <T extends LegacyPayload> WirePayload<T> wrapClientbound(TrophyNetwork.Clientbound<T> packet,
            LegacyPayload payload) {
        return new WirePayload<>(packet.type(), packet.codec(), packet.type().payloadClass().cast(payload));
    }

    private static <T extends LegacyPayload> WirePayload<T> wrapServerbound(TrophyNetwork.Serverbound<T> packet,
            LegacyPayload payload) {
        return new WirePayload<>(packet.type(), packet.codec(), packet.type().payloadClass().cast(payload));
    }

    private record WirePayload<T extends LegacyPayload>(LegacyPayload.Type<T> type,
            LegacyStreamCodec<? super FriendlyByteBuf, T> codec, T message)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        @Override
        public ResourceLocation id() {
            return type.id();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            codec.encode(buffer, message);
        }
    }
}
*///?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerotrophies.platform.NetworkPlatform;

// NeoForge 20.2 / 20.3: the Forge-style SimpleChannel on <modid>:main (missing on the other side is accepted).
public final class NeoForgeTrophyNetwork implements NetworkPlatform {
    private static SimpleChannel channel;

    public static void register(IEventBus modEventBus) {
        var version = NetworkRegistry.acceptMissingOr("1");
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(NeroTrophiesCommon.MOD_ID, "main"),
                () -> "1", version, version);
        int index = 0;
        for (TrophyNetwork.Clientbound<?> packet : TrophyNetwork.clientbound()) {
            registerClientbound(packet, index++);
        }
        for (TrophyNetwork.Serverbound<?> packet : TrophyNetwork.serverbound()) {
            registerServerbound(packet, index++);
        }
    }

    private static <T extends LegacyPayload> void registerClientbound(TrophyNetwork.Clientbound<T> packet, int index) {
        channel.messageBuilder(packet.type().payloadClass(), index, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> packet.handler().accept(payload))
                .add();
    }

    private static <T extends LegacyPayload> void registerServerbound(TrophyNetwork.Serverbound<T> packet, int index) {
        channel.messageBuilder(packet.type().payloadClass(), index, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.getSender();
                    if (player != null) {
                        packet.handler().accept(payload, player);
                    }
                })
                .add();
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        if (channel == null || !channel.isRemotePresent(player.connection.connection)) {
            return false;
        }
        channel.send(PacketDistributor.PLAYER.with(() -> player), payload);
        return true;
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var client = Minecraft.getInstance().getConnection();
        if (channel != null && client != null && channel.isRemotePresent(client.getConnection())) {
            channel.sendToServer(payload);
        }
    }
}
*///?}
