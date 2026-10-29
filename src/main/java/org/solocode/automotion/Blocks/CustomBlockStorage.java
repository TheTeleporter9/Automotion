package org.solocode.automotion.Blocks;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;


public class CustomBlockStorage {
    private final NamespacedKey blockIdKey;

    public CustomBlockStorage(JavaPlugin plugin) {
        this.blockIdKey = new NamespacedKey(plugin, "automotion_block_id");
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

    private NamespacedKey getBlockKey(Block block) {
        return new NamespacedKey(
                blockIdKey.getNamespace(),
                blockIdKey.getKey() + "_" +
                        block.getX() + "_" +
                        block.getY() + "_" +
                        block.getZ()
        );
    }

}
