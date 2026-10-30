package org.solocode.automotion.Data;

import java.util.Objects;

/**
 * A single placed machine: identity, its shared definition and its runtime state.
 */
public record PlacedMachine(
        BlockPosition position,
        Machine definition,
        MachineState state,
        MachineInventory inventory
) {

    public PlacedMachine {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(inventory, "inventory");
    }
}
