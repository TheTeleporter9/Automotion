package org.solocode.automotion.Data;

import org.bukkit.Chunk;

import java.util.UUID;

public record ChunkPosition(
        UUID worldID,
        int x,
        int z
) {

    public static ChunkPosition of(BlockPosition position) {
        return position.chunk();
    }

    public static ChunkPosition of(Chunk chunk) {
        return new ChunkPosition(
                chunk.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ()
        );
    }
}
