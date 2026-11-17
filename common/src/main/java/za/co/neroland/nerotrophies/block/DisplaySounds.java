package za.co.neroland.nerotrophies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The block and book sounds from {@code sounds.json}, played by id (direct sound events: no registry entry is needed
 * for a sound the server only plays and the client only resolves by location).
 */
public final class DisplaySounds {

    public static final SoundEvent CABINET_LATCH = event("block.cabinet.latch");
    public static final SoundEvent PEDESTAL_SET = event("block.pedestal.set");
    public static final SoundEvent TROPHY_PLACE = event("block.trophy.place");
    public static final SoundEvent BOOK_OPEN = event("book.open");
    public static final SoundEvent BOOK_PAGE = event("book.page");
    public static final SoundEvent BOOK_RECLAIM = event("book.reclaim");

    private DisplaySounds() {
    }

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(TrophyIds.id(path));
    }

    /** Plays {@code sound} at {@code pos} for everyone nearby (server side). */
    public static void play(Level level, BlockPos pos, SoundEvent sound) {
        if (!level.isClientSide()) {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }
}
