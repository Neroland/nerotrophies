package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * NeroTrophies' own payload channel, {@code nerotrophies:main}, protocol {@value #PROTOCOL_VERSION}.
 * Every payload is declared from {@link #init()} (common init step 6), before any loader builds its channel; Core's
 * network is never reused. The payload ids are the {@code PAYLOAD_*} constants in {@link TrophyIds}.
 *
 * <p><b>Registration point:</b> the network work package declares its payloads in
 * {@link TrophyPayloads#declare()} with {@link #clientbound} / {@link #serverbound}. Serverbound handlers receive the
 * sending {@link ServerPlayer} and act on that player only; every one is rate-limited. Each loader's
 * {@code *TrophyNetwork} class turns these lists into its own registrations.
 */
public final class TrophyNetwork {

    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
    }

    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
    }

    public static final String CHANNEL = "main";
    public static final String PROTOCOL_VERSION = "1";

    private static final List<Clientbound<?>> CLIENTBOUND = new ArrayList<>();
    private static final List<Serverbound<?>> SERVERBOUND = new ArrayList<>();
    private static final AtomicBoolean DECLARED = new AtomicBoolean();

    private TrophyNetwork() {
    }

    /** A payload type {@code nerotrophies:<name>}. */
    //? if >=1.20.5 {
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(TrophyIds.id(name));
    }
    //?} else {
    /*public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name, Class<T> payload) {
        return new CustomPacketPayload.Type<>(TrophyIds.id(name), payload);
    }
    *///?}

    public static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        CLIENTBOUND.add(new Clientbound<>(type, codec, handler));
    }

    public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        SERVERBOUND.add(new Serverbound<>(type, codec, handler));
    }

    public static List<Clientbound<?>> clientbound() {
        return Collections.unmodifiableList(CLIENTBOUND);
    }

    public static List<Serverbound<?>> serverbound() {
        return Collections.unmodifiableList(SERVERBOUND);
    }

    public static boolean hasPayloads() {
        return !CLIENTBOUND.isEmpty() || !SERVERBOUND.isEmpty();
    }

    /** Declares every payload once (common init step 6). */
    public static void init() {
        if (DECLARED.compareAndSet(false, true)) {
            TrophyPayloads.declare();
        }
    }
}
