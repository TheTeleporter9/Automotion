package org.solocode.automotion.Data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineStateTest {

    private static Recipe recipe(String id) {
        return new Recipe(id, List.of(), List.of());
    }

    @Test
    void freshlyCreatedStateIsDirty() {
        MachineState state = new MachineState();

        assertTrue(state.isDirty());
        assertNull(state.recipe());
        assertEquals(0, state.progressTicks());
    }

    @Test
    void selectingARecipeResetsProgressAndMarksDirty() {
        MachineState state = new MachineState();
        state.setProgress(120);
        state.markClean();

        state.select(recipe("CopperPress"));

        assertEquals("CopperPress", state.recipeId());
        assertEquals(0, state.progressTicks());
        assertTrue(state.isDirty());
    }

    @Test
    void unchangedProgressDoesNotDirtyTheMachine() {
        MachineState state = new MachineState();
        state.setProgress(40);
        state.markClean();

        state.setProgress(40);
        assertFalse(state.isDirty());

        state.setProgress(41);
        assertTrue(state.isDirty());
    }

    @Test
    void restoringPersistedStateStaysClean() {
        MachineState state = new MachineState();
        state.restore(recipe("CopperUnpacking"), 180);

        assertEquals("CopperUnpacking", state.recipeId());
        assertEquals(180, state.progressTicks());
        assertFalse(state.isDirty());
    }

    @Test
    void negativeProgressIsClamped() {
        MachineState state = new MachineState();
        state.setProgress(-20);

        assertEquals(0, state.progressTicks());
    }
}
