package za.co.neroland.nerotrophies.engine;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * Mints claim tokens: a random {@code long} from one {@link SecureRandom}, redrawn while it
 * is reserved ({@code -16..0}) or already live or retired. Tokens are unguessable and carry no order or identity.
 */
public final class TrophyTokens {

    private static final RandomGenerator RANDOM = new SecureRandom();

    private TrophyTokens() {
    }

    /** The shared generator (server thread). */
    public static RandomGenerator secureRandom() {
        return RANDOM;
    }

    public static long mint(TrophyState state) {
        return mint(state, RANDOM);
    }

    public static long mint(TrophyState state, RandomGenerator random) {
        long token;
        do {
            token = random.nextLong();
        } while (TrophyStamp.isReserved(token) || state.tokenKnown(token));
        return token;
    }
}
