package org.solocode.automotion.Data;

import org.bukkit.Location;
import org.bukkit.block.Block;

public record PlacedMachine(
       Location location,
       Machine definition
) {
}
