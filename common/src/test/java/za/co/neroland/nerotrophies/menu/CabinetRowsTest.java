package za.co.neroland.nerotrophies.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;

/** Cabinet button ids resolve against the list captured at open; a reload in between makes a row click stale. */
class CabinetRowsTest {

    private static final Identifier A = TestDefs.id("astronaut");
    private static final Identifier B = TestDefs.id("boss_hunter");
    private static final Identifier C = TestDefs.id("codex");

    @Test
    void rowsAreSortedByIdAndMapToButtons() {
        CabinetRows rows = new CabinetRows(List.of(C, A, B), 7L);
        assertEquals(List.of(A, B, C), rows.rows());
        assertEquals(CabinetRows.UNBIND, rows.buttonFor(Optional.empty()));
        assertEquals(CabinetRows.UNBIND + 2, rows.buttonFor(Optional.of(B)));
        assertEquals(-1, rows.buttonFor(Optional.of(TestDefs.id("gone"))));
        assertEquals(new CabinetRows.Action(CabinetRows.Kind.BIND, Optional.of(B)), rows.resolve(4, 7L));
        assertEquals(CabinetRows.Kind.UNBIND, rows.resolve(2, 7L).kind());
        assertEquals(CabinetRows.Kind.PREVIOUS_TRIM, rows.resolve(0, 7L).kind());
        assertEquals(CabinetRows.Kind.NEXT_TRIM, rows.resolve(1, 99L).kind(), "trims ignore the generation");
        assertEquals(CabinetRows.Kind.INVALID, rows.resolve(6, 7L).kind());
        assertEquals(CabinetRows.Kind.INVALID, rows.resolve(-3, 7L).kind());
    }

    @Test
    void aReloadWhileTheScreenIsOpenMakesRowClicksStale() {
        CabinetRows rows = new CabinetRows(List.of(A, B), 7L);
        // After a reload the server's list could be [A, NEW, B]: row 2 would now mean NEW. It must not bind anything.
        assertEquals(CabinetRows.Kind.STALE, rows.resolve(4, 8L).kind());
        assertEquals(CabinetRows.Kind.STALE, rows.resolve(2, 8L).kind());
        assertEquals(Optional.empty(), rows.resolve(4, 8L).collection());
    }
}
