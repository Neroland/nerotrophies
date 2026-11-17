package za.co.neroland.nerotrophies.client;

/**
 * Client-side init, called by each loader's client setup only on the physical client. <b>Owned by the name-sync work
 * package</b> (the in-memory name cache in {@code client/name/}, cleared on disconnect); the blocks and UI packages
 * add their renderer and screen registration through it afterwards.
 */
public final class TrophyClient {

    private TrophyClient() {
    }

    /** Physical client only, after common init. */
    public static void init() {
    }
}
