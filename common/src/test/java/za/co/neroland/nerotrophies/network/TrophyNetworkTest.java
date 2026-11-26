package za.co.neroland.nerotrophies.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/** The channel scaffolding: payload ids live under {@code nerotrophies:}, and the round-trip helper works. */
class TrophyNetworkTest {

    @Test
    void payloadTypesAreNamespaced() {
        assertEquals("nerotrophies:" + TrophyIds.PAYLOAD_RECORD, RecordPayload.TYPE.id().toString());
        assertEquals("main", TrophyNetwork.CHANNEL);
    }

    @Test
    void roundTripHelperRoundTripsTheStamp() {
        TrophyStamp stamp = new TrophyStamp(TestDefs.id("x/y"), -1234567890123L);
        assertEquals(stamp, PayloadCodecs.roundTrip(TrophyStamp.STREAM_CODEC, stamp));
        assertTrue(TrophyStamp.isReserved(TrophyStamp.GALLERY_BLANK_TOKEN));
    }
}
