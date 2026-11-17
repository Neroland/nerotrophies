package za.co.neroland.nerotrophies.client.net;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import za.co.neroland.nerotrophies.network.RecordDeltaPayload;
import za.co.neroland.nerotrophies.network.RecordPayload;

/**
 * The client's copy of its <b>own</b> trophy record (the {@code record} payload), memory only, for the Trophy Book.
 * Cleared on disconnect. Client thread only.
 */
public final class ClientTrophyRecord {

    private static RecordPayload current;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private ClientTrophyRecord() {
    }

    /** The latest record, or null until the first {@code record} payload. */
    public static RecordPayload current() {
        return current;
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    static void set(RecordPayload record) {
        current = record;
        changed();
    }

    static void apply(RecordDeltaPayload delta) {
        RecordPayload base = current;
        if (base == null) {
            return; // the full record follows (login pass end); a delta alone is not a record
        }
        current = delta.applyTo(base);
        changed();
    }

    static void clear() {
        if (current != null) {
            current = null;
            changed();
        }
    }

    private static void changed() {
        for (Runnable r : LISTENERS) {
            r.run();
        }
    }
}
