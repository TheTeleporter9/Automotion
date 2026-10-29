package org.solocode.automotion.Commands.ItemMenue;


import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Items.CustomItem;
import org.solocode.automotion.Items.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

public final class CustomItemMenu implements Listener {

    private static final int PAGE_SIZE = 45;

    private final JavaPlugin plugin;
    private final ItemRegistry registry;

    public CustomItemMenu(JavaPlugin plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int page) {
        List<CustomItem> items = new ArrayList<>(registry.getItems());
        int maxPage = Math.max(0, (items.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maxPage));

        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                Component.text("Automotion Items")
        );
        holder.setInventory(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());

        for (int i = start; i < end; i++) {
            CustomItem item = items.get(i);
            inventory.setItem(
                    i - start,
                    registry.createItemStack(item)
            );
        }

        if (page > 0) {
            inventory.setItem(45, navigationItem(
                    Material.ARROW, "Previous Page"
            ));
        }

        inventory.setItem(49, navigationItem(
                Material.BARRIER, "Close"
        ));

        if (page < maxPage) {
            inventory.setItem(53, navigationItem(
                    Material.ARROW, "Next Page"
            ));
        }

        holder.page = page;
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false)
                instanceof MenuHolder holder)) {
            return;
        }

        // Prevent moving items into or out of the GUI.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Ignore clicks in the player's own inventory.
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            open(player, holder.page - 1);
            return;
        }

        if (slot == 53) {
            open(player, holder.page + 1);
            return;
        }

        if (slot < 0 || slot >= PAGE_SIZE) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        registry.getItem(clicked).ifPresent(customItem -> {
            ItemStack reward = registry.createItemStack(customItem);
            var leftovers = player.getInventory().addItem(reward);

            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(
                            player.getLocation(), item
                    )
            );
        });
    }

    private ItemStack navigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        private int page;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}