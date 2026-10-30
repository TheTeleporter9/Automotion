package org.solocode.automotion.Machine.Gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.MachineLayout;
import org.solocode.automotion.Data.MachineState;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Data.Recipe;
import org.solocode.automotion.Items.ItemRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders the machine GUI. The GUI is the machine inventory itself, so what a
 * player sees is the real storage, not a copy.
 */
public final class MachineGui {

    private static final Material BACKGROUND = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material PROGRESS_ON = Material.LIME_STAINED_GLASS_PANE;
    private static final Material PROGRESS_OFF = Material.GRAY_STAINED_GLASS_PANE;

    private final ItemRegistry itemRegistry;
    private final Map<BlockPosition, Integer> openCounts = new HashMap<>();
    private final Map<BlockPosition, Long> rendered = new HashMap<>();

    public MachineGui(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void open(Player player, PlacedMachine machine) {
        decorate(machine);
        render(machine);
        rendered.put(machine.position(), signature(machine));
        player.openInventory(machine.inventory().inventory());
    }

    public void noteOpened(BlockPosition position) {
        openCounts.merge(position, 1, Integer::sum);
    }

    public void noteClosed(BlockPosition position) {
        openCounts.computeIfPresent(position, (key, count) -> count <= 1 ? null : count - 1);
        if (!openCounts.containsKey(position)) {
            rendered.remove(position);
        }
    }

    /**
     * Updates a machine GUI only while it is visible and only when the displayed
     * state actually changed.
     */
    public void refresh(PlacedMachine machine) {
        if (!openCounts.containsKey(machine.position())) {
            return;
        }

        long signature = signature(machine);
        if (Objects.equals(rendered.get(machine.position()), signature)) {
            return;
        }

        rendered.put(machine.position(), signature);
        render(machine);
    }

    public void closeAll(PlacedMachine machine) {
        Inventory inventory = machine.inventory().inventory();
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            viewer.closeInventory();
        }
        openCounts.remove(machine.position());
        rendered.remove(machine.position());
    }

    private void decorate(PlacedMachine machine) {
        MachineLayout layout = machine.inventory().layout();
        Inventory inventory = machine.inventory().inventory();
        ItemStack background = icon(BACKGROUND, " ");

        for (int slot : layout.decorativeSlots()) {
            inventory.setItem(slot, background.clone());
        }
    }

    private void render(PlacedMachine machine) {
        MachineInventory storage = machine.inventory();
        MachineLayout layout = storage.layout();
        Inventory inventory = storage.inventory();

        inventory.setItem(layout.identitySlot(), identityItem(machine));
        inventory.setItem(layout.recipeSlot(), recipeItem());

        int progressSlots = layout.progressSlots().size();
        int lit = litProgress(machine, progressSlots);
        for (int index = 0; index < progressSlots; index++) {
            inventory.setItem(layout.progressSlots().get(index), progressIcon(index, progressSlots, index < lit));
        }
    }

    private long signature(PlacedMachine machine) {
        return Objects.hash(
                machine.state().recipeId(),
                litProgress(machine, machine.inventory().layout().progressSlots().size()),
                status(machine)
        );
    }

    private int litProgress(PlacedMachine machine, int progressSlots) {
        if (progressSlots <= 0) {
            return 0;
        }

        MachineState state = machine.state();
        int percent = machine.definition().properties().progressPercent(state.progressTicks());
        int lit = percent * progressSlots / 100;

        if (state.progressTicks() > 0 && lit == 0) {
            lit = 1;
        }
        return Math.min(progressSlots, lit);
    }

    private String status(PlacedMachine machine) {
        Recipe recipe = machine.state().recipe();
        if (recipe == null) {
            return "No recipe selected";
        }

        MachineInventory storage = machine.inventory();
        if (!storage.holdsAll(recipe.recipeItems())) {
            return "Waiting for ingredients";
        }
        if (!storage.hasRoomFor(recipe.resultItems())) {
            return "Output full";
        }
        return machine.state().progressTicks() > 0 ? "Working" : "Idle";
    }

    private ItemStack identityItem(PlacedMachine machine) {
        MachineLayout layout = machine.inventory().layout();
        ItemStack icon = machine.definition().itemId() == null
                ? null
                : itemRegistry.createItem(machine.definition().itemId()).orElse(null);

        boolean fallback = icon == null;
        if (fallback) {
            icon = new ItemStack(Material.CRAFTING_TABLE);
        }

        String recipeId = machine.state().recipeId();
        int percent = machine.definition().properties().progressPercent(machine.state().progressTicks());

        ItemStack identity = icon;
        identity.editMeta(meta -> {
            if (fallback) {
                meta.displayName(Component.text(layout.title()));
            }
            meta.lore(List.of(
                    Component.text("Machine: " + machine.definition().id()),
                    Component.text("Recipe: " + (recipeId == null ? "none" : recipeId)),
                    Component.text("Status: " + status(machine)),
                    Component.text("Progress: " + percent + "%")
            ));
        });
        return identity;
    }

    private ItemStack recipeItem() {
        ItemStack item = icon(Material.BOOK, "Change recipe");
        item.editMeta(meta -> meta.lore(List.of(Component.text("Click to pick another recipe"))));
        return item;
    }

    private ItemStack progressIcon(int index, int total, boolean lit) {
        return icon(
                lit ? PROGRESS_ON : PROGRESS_OFF,
                "Progress " + (lit ? index + 1 : 0) + "/" + total
        );
    }

    private ItemStack icon(Material material, String name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }
}
