package org.solocode.automotion;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Items.ItemRegistry;

public class PlayerPlaceBlockListener implements Listener {

    private final ItemRegistry registry;
    private final  CustomBlockStorage storage;

    public PlayerPlaceBlockListener(ItemRegistry registry, CustomBlockStorage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    @EventHandler
    private void playerPlaceBlockEvent(BlockPlaceEvent event) {

        if(event.isCancelled()) return;

        var placedItem = event.getItemInHand();

        registry.getItem(placedItem).ifPresent(customItem -> {
            Block block = event.getBlockPlaced();

            event.getPlayer().sendMessage(
                    "Placed registerd item: " + customItem.id()
            );

            storage.set(block, customItem.id().toString());
        });

    }

}

