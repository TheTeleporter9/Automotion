package org.solocode.automotion.Data;

import org.bukkit.block.Block;

public record MachineContext (
    Block block,
    Machine machine
)
{ }
