package za.co.neroland.nerotrophies.engine;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;

/**
 * Token to status (and, for a genuine trophy whose earner shows their name, the gamertag),
 * including the reserved tokens. This is the <b>only</b> place a token becomes a name; name sync calls it for tokens a
 * client can see, and nothing else ever resolves an arbitrary token.
 */
public final class TokenResolver {

    /** A status, plus the earner's name only for {@link TrophyStatus#GENUINE} with {@code showName} on. */
    public record Resolution(TrophyStatus status, Optional<String> name) {
        static final Resolution REPLICA = new Resolution(TrophyStatus.REPLICA, Optional.empty());
        static final Resolution BLANK = new Resolution(TrophyStatus.BLANK, Optional.empty());
        static final Resolution GALLERY = new Resolution(TrophyStatus.GALLERY, Optional.empty());

        /** Whether a cabinet may count this trophy (genuine only). */
        public boolean counts() {
            return status == TrophyStatus.GENUINE;
        }
    }

    private TokenResolver() {
    }

    public static Resolution resolve(MinecraftServer server, TrophyStamp stamp) {
        return resolve(TrophyState.get(server), stamp.trophy(), stamp.token());
    }

    public static Resolution resolve(TrophyState state, Identifier trophy, long token) {
        if (token == TrophyStamp.UNSTAMPED) {
            return Resolution.REPLICA;
        }
        if (token == TrophyStamp.GALLERY_TOKEN) {
            return Resolution.GALLERY;
        }
        if (TrophyStamp.isReserved(token)) {
            return Resolution.BLANK; // GALLERY_BLANK_TOKEN and the rest of the reserved range
        }
        Optional<TrophyState.TokenOwner> owner = state.owner(token);
        if (owner.isPresent()) {
            if (!owner.get().trophy().equals(trophy)) {
                return Resolution.REPLICA; // a real token stamped onto another trophy id
            }
            Optional<String> name = state.record(owner.get().player())
                    .filter(TrophyRecord::showName)
                    .flatMap(TrophyRecord::name);
            return new Resolution(TrophyStatus.GENUINE, name);
        }
        if (state.retiredTrophy(token).isPresent()) {
            return Resolution.REPLICA;
        }
        return Resolution.BLANK;
    }
}
