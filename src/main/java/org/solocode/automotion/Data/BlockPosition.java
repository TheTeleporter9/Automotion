package org.solocode.automotion.Data;

import org.bukkit.block.Block;

import java.util.Optional;
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

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    public int localX() {
        return x & 15;
    }

    public int localZ() {
        return z & 15;
    }

    public ChunkPosition chunk() {
        return new ChunkPosition(worldID, chunkX(), chunkZ());
    }

    public String asString() {
        return x + "," + y + "," + z;
    }

    public static Optional<BlockPosition> parse(UUID worldID, String value) {
        if (value == null) {
            return Optional.empty();
        }

        String[] parts = value.split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            return Optional.of(new BlockPosition(
                    worldID,
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public String describe() {
        return x + " " + y + " " + z + " (" + worldID + ")";
    }
}
