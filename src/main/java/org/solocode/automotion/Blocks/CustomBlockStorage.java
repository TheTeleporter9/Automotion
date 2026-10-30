package org.solocode.automotion.Blocks;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Data.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class CustomBlockStorage {

    private static final String MACHINE_INDEX_KEY = "automotion_machines";
    private static final String MACHINE_DATA_KEY = "automotion_machine_data";

    private final NamespacedKey blockIdKey;
    private final NamespacedKey machineDataKey;
    private final NamespacedKey machineIndexKey;

    public CustomBlockStorage(JavaPlugin plugin) {
        this.blockIdKey = new NamespacedKey(plugin, "automotion_block_id");
        this.machineDataKey = new NamespacedKey(plugin, MACHINE_DATA_KEY);
        this.machineIndexKey = new NamespacedKey(plugin, MACHINE_INDEX_KEY);
    }

    public void set(Block block, String itemId) {
        block.getChunk().getPersistentDataContainer().set(
                getBlockKey(block),
                PersistentDataType.STRING,
                itemId
        );
    }

    public Optional<String> get(Block block) {
        return Optional.ofNullable(
                block.getChunk()
                        .getPersistentDataContainer()
                        .get(getBlockKey(block), PersistentDataType.STRING)
        );
    }

    public void remove(Block block) {
        block.getChunk()
                .getPersistentDataContainer()
                .remove(getBlockKey(block));
    }

    public boolean contains(Block block) {
        return block.getChunk()
                .getPersistentDataContainer()
                .has(getBlockKey(block), PersistentDataType.STRING);
    }

    public void setMachineData(Block block, String payload) {
        block.getChunk().getPersistentDataContainer().set(
                getMachineDataKey(block),
                PersistentDataType.STRING,
                payload
        );
    }

    public Optional<String> getMachineData(Block block) {
        return Optional.ofNullable(
                block.getChunk()
                        .getPersistentDataContainer()
                        .get(getMachineDataKey(block), PersistentDataType.STRING)
        );
    }

    public void removeMachineData(Block block) {
        block.getChunk()
                .getPersistentDataContainer()
                .remove(getMachineDataKey(block));
    }

    /**
     * Adds the block to the chunk machine index so a chunk load can restore every
     * machine without scanning blocks.
     */
    public void addMachine(Block block) {
        BlockPosition position = BlockPosition.of(block);
        List<String> entries = readIndex(block.getChunk());
        String value = position.asString();
        if (entries.contains(value)) {
            return;
        }
        entries.add(value);
        writeIndex(block.getChunk(), entries);
    }

    public void removeMachine(Block block) {
        Chunk chunk = block.getChunk();
        List<String> entries = readIndex(chunk);
        if (entries.remove(BlockPosition.of(block).asString())) {
            writeIndex(chunk, entries);
        }
    }

    public List<BlockPosition> getMachines(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        List<BlockPosition> positions = new ArrayList<>();
        for (String entry : readIndex(chunk)) {
            BlockPosition.parse(worldId, entry).ifPresent(positions::add);
        }
        return positions;
    }

    private List<String> readIndex(Chunk chunk) {
        String raw = chunk.getPersistentDataContainer().get(machineIndexKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> entries = new ArrayList<>();
        for (String entry : raw.split(";")) {
            if (!entry.isBlank()) {
                entries.add(entry.trim());
            }
        }
        return entries;
    }

    private void writeIndex(Chunk chunk, List<String> entries) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (entries.isEmpty()) {
            container.remove(machineIndexKey);
            return;
        }
        container.set(machineIndexKey, PersistentDataType.STRING, String.join(";", entries));
    }

    private NamespacedKey getBlockKey(Block block) {
        return new NamespacedKey(
                blockIdKey.getNamespace(),
                blockIdKey.getKey() + "_" +
                        block.getX() + "_" +
                        block.getY() + "_" +
                        block.getZ()
        );
    }

    private NamespacedKey getMachineDataKey(Block block) {
        return new NamespacedKey(
                machineDataKey.getNamespace(),
                machineDataKey.getKey() + "_" +
                        block.getX() + "_" +
                        block.getY() + "_" +
                        block.getZ()
        );
    }
}
