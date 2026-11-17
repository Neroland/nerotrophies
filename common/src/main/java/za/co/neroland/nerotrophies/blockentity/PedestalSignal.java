package za.co.neroland.nerotrophies.blockentity;

import java.util.Optional;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * Display Pedestal comparator output: 4 / 8 / 12 / 15 for a genuine-looking bronze / silver /
 * gold / nero trophy, 1 for a replica or any other item, 0 when empty. Reads the server's own resolution only.
 */
public final class PedestalSignal {

    private PedestalSignal() {
    }

    public static int of(boolean hasItem, Optional<TrophyTier> tier, boolean replica) {
        if (!hasItem) {
            return 0;
        }
        if (replica || tier.isEmpty()) {
            return 1;
        }
        return switch (tier.get()) {
            case BRONZE -> 4;
            case SILVER -> 8;
            case GOLD -> 12;
            case NERO -> 15;
        };
    }
}
