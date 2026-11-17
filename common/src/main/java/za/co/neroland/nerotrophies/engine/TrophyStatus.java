package za.co.neroland.nerotrophies.engine;

/**
 * What a stamped trophy is, as resolved by the server. {@code GENUINE} with a name is the only state
 * that ever shows a gamertag; {@code BLANK} and "genuine with the name hidden" look the same on purpose.
 */
public enum TrophyStatus {
    /** The token is its earner's current token for this trophy: counts in a cabinet. */
    GENUINE,
    /** No live owner (erased, forged, or the gallery's nameless copy): nameless, counts for nothing. */
    BLANK,
    /** Superseded by a re-claim or revoke, edited, or unstamped: covered plate, counts for nothing. */
    REPLICA,
    /** A gallery display copy: the client draws the placeholder name. */
    GALLERY
}
