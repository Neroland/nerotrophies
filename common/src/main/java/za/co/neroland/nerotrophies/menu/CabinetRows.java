package za.co.neroland.nerotrophies.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;

/**
 * The cabinet screen's button ids: {@code 0} previous trim,
 * {@code 1} next trim, {@code 2} unbind ("None"), {@code 2 + k} (k &ge; 1) bind the k-th collection of the list
 * captured when the menu opened (every loaded collection, sorted by id). The list and the definition generation are
 * captured at open; a click after a datapack reload changed the generation is refused as {@link Kind#STALE}, so a
 * row can never bind a different collection than the one the player saw.
 */
public final class CabinetRows {

    public static final int PREVIOUS_TRIM = 0;
    public static final int NEXT_TRIM = 1;
    public static final int UNBIND = 2;
    /** Hard bound on rows (Core caps collections at 512). */
    public static final int MAX_ROWS = 512;

    public enum Kind { PREVIOUS_TRIM, NEXT_TRIM, UNBIND, BIND, STALE, INVALID }

    public record Action(Kind kind, Optional<Identifier> collection) {
        static Action of(Kind kind) {
            return new Action(kind, Optional.empty());
        }
    }

    private final List<Identifier> rows;
    private final long generation;

    public CabinetRows(Collection<Identifier> collections, long generation) {
        List<Identifier> sorted = new ArrayList<>(collections);
        sorted.sort(Identifier::compareTo);
        this.rows = List.copyOf(sorted.size() > MAX_ROWS ? sorted.subList(0, MAX_ROWS) : sorted);
        this.generation = generation;
    }

    public List<Identifier> rows() {
        return this.rows;
    }

    public long generation() {
        return this.generation;
    }

    /** The button id that binds {@code collection} ({@link #UNBIND} for none), or -1 if it is not a row. */
    public int buttonFor(Optional<Identifier> collection) {
        if (collection.isEmpty()) {
            return UNBIND;
        }
        int index = this.rows.indexOf(collection.get());
        return index < 0 ? -1 : UNBIND + 1 + index;
    }

    /** What button {@code id} means now, given the current definition generation. */
    public Action resolve(int id, long currentGeneration) {
        if (id == PREVIOUS_TRIM) {
            return Action.of(Kind.PREVIOUS_TRIM);
        }
        if (id == NEXT_TRIM) {
            return Action.of(Kind.NEXT_TRIM);
        }
        if (id < UNBIND || id > UNBIND + this.rows.size()) {
            return Action.of(Kind.INVALID);
        }
        if (currentGeneration != this.generation) {
            return Action.of(Kind.STALE);
        }
        if (id == UNBIND) {
            return Action.of(Kind.UNBIND);
        }
        return new Action(Kind.BIND, Optional.of(this.rows.get(id - UNBIND - 1)));
    }
}
