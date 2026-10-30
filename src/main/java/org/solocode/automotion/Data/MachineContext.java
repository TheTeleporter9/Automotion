package org.solocode.automotion.Data;

import org.bukkit.block.Block;

public record MachineContext(
        PlacedMachine machine,
        Block block,
        int intervalTicks
) {

    public Machine definition() {
        return machine.definition();
    }

    public MachineState state() {
        return machine.state();
    }

    public MachineInventory inventory() {
        return machine.inventory();
    }
}
