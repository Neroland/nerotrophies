package za.co.neroland.nerotrophies.client.name;

/** What the server said about a stamped token a client can see (decision 009). */
public enum NameState {
    /** Genuine, earner shows their name: draw it. */
    NAMED,
    /** Genuine with the name hidden, erased, forged or unknown: the deliberate blank ornament (008 section 6). */
    BLANK,
    /** Superseded, edited or unstamped: the covered pewter plate. */
    REPLICA
}
