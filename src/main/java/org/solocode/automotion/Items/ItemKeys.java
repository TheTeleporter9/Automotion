package org.solocode.automotion.Items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemKeys {

    public static NamespacedKey ITEM_ID;

    private ItemKeys() {}

    public static void init(JavaPlugin plugin) {
        ITEM_ID = new NamespacedKey(plugin, "item_id");
    }

}
