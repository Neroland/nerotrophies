package za.co.neroland.nerotrophies.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/** {@code /nerotrophies gallery} is refused for anything that is not a player, and for a player not in creative. */
class GalleryAccessTest {

    @Test
    void nonPlayerSourcesAreRefused() {
        assertEquals(Optional.of(GalleryAccess.PLAYERS_ONLY), GalleryAccess.refusal(false, true));
        assertEquals(Optional.of(GalleryAccess.PLAYERS_ONLY), GalleryAccess.refusal(false, false));
    }

    @Test
    void nonCreativePlayersAreRefused() {
        assertEquals(Optional.of(GalleryAccess.CREATIVE_ONLY), GalleryAccess.refusal(true, false));
    }

    @Test
    void aCreativePlayerMayUseIt() {
        assertEquals(Optional.empty(), GalleryAccess.refusal(true, true));
    }
}
