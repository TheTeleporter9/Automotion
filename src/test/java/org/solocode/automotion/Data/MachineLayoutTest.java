package org.solocode.automotion.Data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineLayoutTest {

    @Test
    void defaultsUseTheDocumentedSlots() {
        MachineLayout layout = MachineLayout.defaults("Press");

        layout.validate();

        assertEquals("Press", layout.title());
        assertEquals(List.of(0, 1, 2, 3), layout.outputSlots());
        assertEquals(List.of(5, 6, 7, 8), layout.inputSlots());
        assertEquals(4, layout.identitySlot());
        assertEquals(List.of(13, 22, 31, 40), layout.progressSlots());
        assertEquals(49, layout.recipeSlot());
        assertEquals(4, layout.inputCount());
        assertEquals(4, layout.outputCount());
    }

    @Test
    void decorativeSlotsNeverOverlapReservedSlots() {
        MachineLayout layout = MachineLayout.defaults("Press");

        assertFalse(layout.decorativeSlots().contains(5));
        assertFalse(layout.decorativeSlots().contains(49));
        assertTrue(layout.decorativeSlots().contains(9));
        assertEquals(MachineLayout.SIZE - 14, layout.decorativeSlots().size());
    }

    @Test
    void overlappingSlotsAreRejected() {
        MachineLayout layout = new MachineLayout(
                "Broken",
                List.of(0, 1),
                List.of(1, 2),
                4,
                List.of(13),
                49
        );

        assertThrows(IllegalArgumentException.class, layout::validate);
    }

    @Test
    void outOfRangeSlotsAreRejected() {
        MachineLayout layout = new MachineLayout(
                "Broken",
                List.of(0),
                List.of(54),
                4,
                List.of(13),
                49
        );

        assertThrows(IllegalArgumentException.class, layout::validate);
    }

    @Test
    void emptyInputsAreRejected() {
        MachineLayout layout = new MachineLayout("Broken", List.of(0), List.of(), 4, List.of(13), 49);

        assertThrows(IllegalArgumentException.class, layout::validate);
    }
}
