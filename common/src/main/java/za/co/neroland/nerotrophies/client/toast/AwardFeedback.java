package za.co.neroland.nerotrophies.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;

import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;
import za.co.neroland.nerotrophies.client.particle.TrophyParticles;
import za.co.neroland.nerotrophies.network.AwardToastPayload;

/**
 * Award feedback, shared by every loader: subscribes to the {@code award_toast} hook (contract 2C section 4) and, for
 * each payload the server sent this earner, shows the toast, plays the tier or collection sound and spawns the burst.
 * The retroactive summary shows its toast only: no per-trophy sound and no burst (decisions 005, 007). Each loader's
 * feedback glue calls {@link #init} once on the physical client and {@link #tick} at the end of every client tick.
 */
public final class AwardFeedback {

    private static boolean installed;

    private AwardFeedback() {
    }

    /** Physical client, once. */
    public static void init() {
        if (installed) {
            return;
        }
        installed = true;
        ClientTrophyNet.onAwardToast(AwardFeedback::onAward);
    }

    /** End of every client tick. */
    public static void tick() {
        TrophyParticles.tick();
    }

    static void onAward(AwardToastPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        show(new AwardToast(payload));
        switch (payload.kind()) {
            case AwardToastPayload.KIND_TROPHY -> {
                if (payload.tier() != null) {
                    AwardSounds.play(AwardSounds.forTier(payload.tier()));
                    TrophyParticles.awardBurst(payload.tier());
                }
            }
            case AwardToastPayload.KIND_COLLECTION -> {
                AwardSounds.play(AwardSounds.COLLECTION);
                TrophyParticles.collectionBurst();
            }
            default -> {
                // Retroactive summary: the toast alone.
            }
        }
    }

    private static void show(Toast toast) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        mc.gui.toastManager().addToast(toast);
        //?} else if >=1.21.2 {
        /*mc.getToastManager().addToast(toast);
        *///?} else {
        /*mc.getToasts().addToast(toast);
        *///?}
    }
}
