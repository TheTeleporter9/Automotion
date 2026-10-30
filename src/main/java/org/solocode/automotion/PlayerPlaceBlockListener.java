package org.solocode.automotion;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Items.CustomItem;
import org.solocode.automotion.Items.ItemRegistry;
import org.solocode.automotion.Machine.MachinePlacement;

import java.util.Optional;

public class PlayerPlaceBlockListener implements Listener {

    private final ItemRegistry registry;
    private final CustomBlockStorage storage;
    private final MachinePlacement placement;

    public PlayerPlaceBlockListener(ItemRegistry registry, CustomBlockStorage storage, MachinePlacement placement) {
        this.registry = registry;
        this.storage = storage;
        this.placement = placement;
    }

    @EventHandler(ignoreCancelled = true)
    private void playerPlaceBlockEvent(BlockPlaceEvent event) {
        ItemStack placedItem = event.getItemInHand();
        Optional<CustomItem> customItem = registry.getItem(placedItem);
        if (customItem.isEmpty()) {
            return;
        }

        Block block = event.getBlockPlaced();
        String itemId = customItem.get().id().toString();

        storage.set(block, itemId);
        placement.place(block, itemId);
    }

}
