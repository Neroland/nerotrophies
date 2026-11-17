package za.co.neroland.nerotrophies.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends {@code nerotrophies:main} payloads (one implementation per loader, via {@code META-INF/services}). Never use
 * nearest-player lookups to choose a recipient.
 */
public interface NetworkPlatform {

    /** Sends to one player; false if their client does not speak the channel (a vanilla client). */
    boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Client only: sends an intent to the server. */
    void sendToServer(CustomPacketPayload payload);
}
