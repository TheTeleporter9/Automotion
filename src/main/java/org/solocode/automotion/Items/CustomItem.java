package org.solocode.automotion.Items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CustomItem {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private final NamespacedKey id;
    private final Material material;
    private final Component name;
    private final List<Component> lore;
    private final Integer customModelData;

    private CustomItem(Builder builder) {
        this.id = builder.id;
        this.material = Objects.requireNonNull(builder.material);
        this.name = builder.name;
        this.lore = List.copyOf(builder.lore);
        customModelData = builder.customModelData;
    }

    public static Builder builder(JavaPlugin plugin, String id) {
        return new Builder(plugin, id);
    }

    public NamespacedKey id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public ItemStack createItemStack(NamespacedKey itemIdKey) {
        ItemStack item = new ItemStack(material);

        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(name);
            }

            if (!lore.isEmpty()) {
                meta.lore(lore);
            }

            if (customModelData != null) {
                var modelData = meta.getCustomModelDataComponent();
                modelData.setFloats(List.of(customModelData.floatValue()));
                meta.setCustomModelDataComponent(modelData);
            }

            meta.getPersistentDataContainer().set(
                    itemIdKey,
                    PersistentDataType.STRING,
                    id.toString()
            );
        });

        return item;
    }

    public static final class Builder {

        private final NamespacedKey id;
        private Material material;
        private Component name;
        private final List<Component> lore = new ArrayList<>();
        private Integer customModelData;

        private Builder(JavaPlugin plugin, String id) {
            this.id = new NamespacedKey(plugin, id);
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder name(String name) {
            this.name = MINI_MESSAGE.deserialize(name);
            return this;
        }

        public Builder lore(String line) {
            this.lore.add(MINI_MESSAGE.deserialize(line));
            return this;
        }

        public Builder customModelData(int value) {
            this.customModelData = value;
            return this;
        }

        public CustomItem build() {
            Objects.requireNonNull(material, "Item material is required");
            return new CustomItem(this);
        }
    }
}