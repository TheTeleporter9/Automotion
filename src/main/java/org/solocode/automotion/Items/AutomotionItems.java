package org.solocode.automotion.Items;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class AutomotionItems {

    private AutomotionItems() {}

    public static void register(JavaPlugin plugin, ItemRegistry registry) {
        registry.register(
                CustomItem.builder(plugin, "basic_crafter")
                        .material(Material.CRAFTER)
                        .name("<green>Basic assembler")
                        .lore("<gray> An assembler which allows you to automate recipes")
                        .customModelData(6101001)
                        .build()
        );
        registry.register(
                CustomItem.builder(plugin, "simple_circuit")
                        .material(Material.PAPER)
                        .name("<red><bold>Simple Circuit")
                        .lore("<!i><gray>Used to make basic machines")
                        .customModelData(6101002)
                        .build()
        );
    }

}
