package za.co.neroland.nerotrophies.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Only advancements a trophy names are queued; the queue is bounded and drains once. */
class AdvancementQueueTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final Identifier WANTED = Identifier.withDefaultNamespace("story/enter_the_nether");

    @AfterEach
    void reset() {
        AdvancementQueue.clear();
    }

    @Test
    void queuesOnlyWantedAndDrainsOnce() {
        AdvancementQueue.setWanted(Set.of(WANTED));
        assertTrue(AdvancementQueue.wants(WANTED));
        assertFalse(AdvancementQueue.wants(Identifier.withDefaultNamespace("recipes/misc/stick")));
        AdvancementQueue.offer(A, Identifier.withDefaultNamespace("recipes/misc/stick"));
        AdvancementQueue.offer(A, WANTED);
        assertEquals(List.of(new AdvancementQueue.Completed(A, WANTED)), AdvancementQueue.drain());
        assertTrue(AdvancementQueue.drain().isEmpty());
    }

    @Test
    void boundedBetweenTicks() {
        AdvancementQueue.setWanted(Set.of(WANTED));
        for (int i = 0; i < AdvancementQueue.MAX_QUEUED + 50; i++) {
            AdvancementQueue.offer(A, WANTED);
        }
        assertEquals(AdvancementQueue.MAX_QUEUED, AdvancementQueue.drain().size());
    }
}
