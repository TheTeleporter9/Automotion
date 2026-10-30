package org.solocode.automotion.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes where the machine inventory stores its data inside the GUI.
 *
 * The machine inventory IS the GUI, so every slot defined here is a real slot of
 * the machine storage: nothing is mirrored or recreated.
 */
public record MachineLayout(
        String title,
        List<Integer> outputSlots,
        List<Integer> inputSlots,
        int identitySlot,
        List<Integer> progressSlots,
        int recipeSlot
) {

    public static final int SIZE = 54;

    public MachineLayout {
        outputSlots = List.copyOf(outputSlots);
        inputSlots = List.copyOf(inputSlots);
        progressSlots = List.copyOf(progressSlots);
    }

    public static MachineLayout defaults(String title) {
        return new MachineLayout(
                title,
                List.of(0, 1, 2, 3),
                List.of(5, 6, 7, 8),
                4,
                List.of(13, 22, 31, 40),
                49
        );
    }

    public int inputCount() {
        return inputSlots.size();
    }

    public int outputCount() {
        return outputSlots.size();
    }

    public boolean isInputSlot(int slot) {
        return inputSlots.contains(slot);
    }

    public boolean isOutputSlot(int slot) {
        return outputSlots.contains(slot);
    }

    public Set<Integer> reservedSlots() {
        Set<Integer> reserved = new LinkedHashSet<>();
        reserved.addAll(outputSlots);
        reserved.addAll(inputSlots);
        reserved.addAll(progressSlots);
        reserved.add(identitySlot);
        reserved.add(recipeSlot);
        return reserved;
    }

    public List<Integer> decorativeSlots() {
        Set<Integer> reserved = reservedSlots();
        List<Integer> decorative = new ArrayList<>(SIZE - reserved.size());
        for (int slot = 0; slot < SIZE; slot++) {
            if (!reserved.contains(slot)) {
                decorative.add(slot);
            }
        }
        return decorative;
    }

    /**
     * @throws IllegalArgumentException when the layout cannot be used for a real inventory.
     */
    public void validate() {
        requireInRange(inputSlots, "INPUT_SLOTS");
        requireInRange(outputSlots, "OUTPUT_SLOTS");
        requireInRange(progressSlots, "PROGRESS_SLOTS");
        requireSlot(identitySlot, "IDENTITY_SLOT");
        requireSlot(recipeSlot, "RECIPE_SLOT");

        if (inputSlots.isEmpty()) {
            throw new IllegalArgumentException("INPUT_SLOTS must define at least one slot");
        }
        if (outputSlots.isEmpty()) {
            throw new IllegalArgumentException("OUTPUT_SLOTS must define at least one slot");
        }

        int declaredSlots = inputSlots.size() + outputSlots.size() + progressSlots.size() + 2;
        if (reservedSlots().size() != declaredSlots) {
            throw new IllegalArgumentException("GUI slots must not be assigned twice");
        }
        if (progressSlots.size() != new LinkedHashSet<>(progressSlots).size()
                || inputSlots.size() != new LinkedHashSet<>(inputSlots).size()
                || outputSlots.size() != new LinkedHashSet<>(outputSlots).size()) {
            throw new IllegalArgumentException("GUI slot lists must not contain duplicates");
        }
    }

    private void requireInRange(List<Integer> slots, String key) {
        for (int slot : slots) {
            requireSlot(slot, key);
        }
    }

    private void requireSlot(int slot, String key) {
        if (slot < 0 || slot >= SIZE) {
            throw new IllegalArgumentException(key + " must be between 0 and " + (SIZE - 1) + ", got " + slot);
        }
    }
}
