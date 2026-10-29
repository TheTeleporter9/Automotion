package org.solocode.automotion.Items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ItemRegistry {

    private final NamespacedKey itemIdKey;
    private final Map<NamespacedKey, CustomItem> items = new HashMap<>();

    public ItemRegistry(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public void register(CustomItem item) {
        if (items.containsKey(item.id())) {
            throw new IllegalArgumentException(
                    "Duplicate custom item ID: " + item.id()
            );
        }
        items.put(item.id(), item);
    }

    public Optional<CustomItem> getItem(String id) {
        NamespacedKey key = NamespacedKey.fromString(id);

        if (key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(items.get(key));
    }

    public Optional<CustomItem> getItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        String id = stack.getPersistentDataContainer().get(
                itemIdKey,
                PersistentDataType.STRING
        );

        if (id == null) {
            return Optional.empty();
        }

        return getItem(id);
    }

    public Collection<CustomItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public ItemStack createItemStack(CustomItem item) {
        if (items.get(item.id()) != item) {
            throw new IllegalArgumentException(
                    "Item is not registered: " + item.id()
            );
        }

        return item.createItemStack(itemIdKey);
    }

}
