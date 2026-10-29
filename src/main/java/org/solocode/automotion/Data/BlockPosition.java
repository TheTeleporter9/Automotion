package org.solocode.automotion.Data;

import org.bukkit.block.Block;

import java.util.UUID;

public record BlockPosition(
        UUID worldID,
        int x,
        int y,
        int z
) {

    public static BlockPosition of(Block block) {
        return new BlockPosition(
                block.getWorld().getUID(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
    }


}
