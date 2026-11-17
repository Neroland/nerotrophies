package za.co.neroland.nerotrophies.client.net;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.client.name.ClientTrophyNames;
import za.co.neroland.nerotrophies.network.AwardToastPayload;
import za.co.neroland.nerotrophies.network.BookRefreshPayload;
import za.co.neroland.nerotrophies.network.ClaimPayload;
import za.co.neroland.nerotrophies.network.NamesDropPayload;
import za.co.neroland.nerotrophies.network.NamesResetPayload;
import za.co.neroland.nerotrophies.network.NamesUpdatePayload;
import za.co.neroland.nerotrophies.network.RecordDeltaPayload;
import za.co.neroland.nerotrophies.network.RecordPayload;
import za.co.neroland.nerotrophies.network.SelectCollectionPayload;
import za.co.neroland.nerotrophies.network.SetTogglePayload;
import za.co.neroland.nerotrophies.platform.Services;

/**
 * The client end of {@code nerotrophies:main}: S2C handlers (called by each loader on the client thread), the C2S send
 * helpers the Book and cabinet screens use, and the award-toast hook. No helper sends a token or asks for a name.
 * Physical client only.
 */
public final class ClientTrophyNet {

    /** Vanilla menu button ids for the cabinet screen (decision 010 section 1); Stage 3's menu owns their meaning. */
    public static final int BUTTON_TRIM_PREVIOUS = 0;
    public static final int BUTTON_TRIM_NEXT = 1;
    public static final int BUTTON_FIRST_ROW = 2;

    private static final List<Consumer<AwardToastPayload>> TOASTS = new CopyOnWriteArrayList<>();

    private ClientTrophyNet() {
    }

    // ---- C2S helpers ----------------------------------------------------------------------------------------------

    /** Claim, or re-claim, one of the player's own trophies (the server decides which; decision 003). */
    public static void claim(Identifier trophy) {
        send(new ClaimPayload(trophy));
    }

    public static void setAnnounce(boolean value) {
        send(new SetTogglePayload(SetTogglePayload.ANNOUNCE, value));
    }

    public static void setShowName(boolean value) {
        send(new SetTogglePayload(SetTogglePayload.SHOW_NAME, value));
    }

    /** Ask for the player's own record again (answered at most once per 5 s). */
    public static void refreshBook() {
        send(BookRefreshPayload.INSTANCE);
    }

    /** Set (or with null, clear) the collection on the Trophy Book in hand. */
    public static void selectCollection(Identifier collection) {
        send(new SelectCollectionPayload(Optional.ofNullable(collection)));
    }

    /** Step the open cabinet's trim (vanilla menu button 0 / 1). */
    public static void cabinetTrimStep(int containerId, boolean next) {
        menuButton(containerId, next ? BUTTON_TRIM_NEXT : BUTTON_TRIM_PREVIOUS);
    }

    /** Pick collection row {@code row} in the open cabinet (vanilla menu button {@code 2 + row}). */
    public static void cabinetBindRow(int containerId, int row) {
        if (row >= 0) {
            menuButton(containerId, BUTTON_FIRST_ROW + row);
        }
    }

    /** Listen for award toasts (earner only; client thread). */
    public static void onAwardToast(Consumer<AwardToastPayload> listener) {
        TOASTS.add(listener);
    }

    // ---- S2C handlers (loader glue calls these on the client thread) -------------------------------------------------

    public static void handle(NamesUpdatePayload payload) {
        bind();
        ClientTrophyNames.apply(payload);
    }

    public static void handle(NamesDropPayload payload) {
        bind();
        ClientTrophyNames.apply(payload);
    }

    public static void handle(NamesResetPayload payload) {
        bind();
        ClientTrophyNames.clear();
    }

    public static void handle(RecordPayload payload) {
        bind();
        ClientTrophyRecord.set(payload);
    }

    public static void handle(RecordDeltaPayload payload) {
        bind();
        ClientTrophyRecord.apply(payload);
    }

    public static void handle(AwardToastPayload payload) {
        bind();
        for (Consumer<AwardToastPayload> l : TOASTS) {
            l.accept(payload);
        }
    }

    /** The client left a server (loader disconnect hooks): forget every name and the record. */
    public static void disconnected() {
        ClientTrophyNames.clear();
        ClientTrophyRecord.clear();
    }

    // ---- internals -------------------------------------------------------------------------------------------------

    private static Object connection() {
        return Minecraft.getInstance().getConnection();
    }

    /** A packet from a new connection starts from an empty cache and no record. */
    private static void bind() {
        Object current = connection();
        ClientTrophyNames.bindSession(current, ClientTrophyNet::connection);
        if (lastSession.get() != current) {
            ClientTrophyRecord.clear();
            lastSession = new java.lang.ref.WeakReference<>(current);
        }
    }

    private static java.lang.ref.WeakReference<Object> lastSession = new java.lang.ref.WeakReference<>(null);

    private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (connection() != null) {
            Services.NETWORK.sendToServer(payload);
        }
    }

    private static void menuButton(int containerId, int buttonId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(containerId, buttonId);
        }
    }
}
