package org.solocode.automotion.Data;

import org.bukkit.block.Block;

public record PlacedMachine(
        Machine machine,
        Block block
) {
}
