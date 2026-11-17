package za.co.neroland.nerotrophies.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The five award sounds. Like {@code block.DisplaySounds} they are direct sound events from
 * {@code sounds.json} (no registry entry is needed to play a client-side sound). Played non-positionally on the
 * earner's own client with the toast, so no other player ever hears them; the volume is in {@code sounds.json}.
 * Client only.
 */
public final class AwardSounds {

    public static final SoundEvent BRONZE = event("award.bronze");
    public static final SoundEvent SILVER = event("award.silver");
    public static final SoundEvent GOLD = event("award.gold");
    public static final SoundEvent NERO = event("award.nero");
    public static final SoundEvent COLLECTION = event("award.collection");

    private AwardSounds() {
    }

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(TrophyIds.id(path));
    }

    /** The tier's award sound; the tier is the number of notes. */
    public static SoundEvent forTier(@Nullable TrophyTier tier) {
        if (tier == null) {
            return BRONZE;
        }
        return switch (tier) {
            case BRONZE -> BRONZE;
            case SILVER -> SILVER;
            case GOLD -> GOLD;
            case NERO -> NERO;
        };
    }

    /** Plays {@code sound} as a UI sound (non-positional) on this client. */
    public static void play(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }
}
