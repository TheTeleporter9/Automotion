package org.solocode.automotion.Machine;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Items.ItemRegistry;
import org.solocode.automotion.Machine.Gui.MachineGui;
import org.solocode.automotion.Machine.Persistence.MachinePersistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Breaks a placed machine: returns its custom item, drops its contents and
 * removes every trace of its runtime and persistent state.
 */
public final class MachineRemoval {

    private final JavaPlugin plugin;
    private final CustomBlockStorage storage;
    private final MachinePersistence persistence;
    private final MachineService service;
    private final MachineGui gui;
    private final ItemRegistry itemRegistry;

    public MachineRemoval(
            JavaPlugin plugin,
            CustomBlockStorage storage,
            MachinePersistence persistence,
            MachineService service,
            MachineGui gui,
            ItemRegistry itemRegistry
    ) {
        this.plugin = plugin;
        this.storage = storage;
        this.persistence = persistence;
        this.service = service;
        this.gui = gui;
        this.itemRegistry = itemRegistry;
    }

    public void remove(Block block, PlacedMachine machine, Player player) {
        gui.closeAll(machine);

        List<ItemStack> contents = machine.inventory().takeStoredItems();

        service.unregister(machine.position());
        persistence.delete(machine.position());
        storage.remove(block);

        Location dropAt = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack content : contents) {
            block.getWorld().dropItemNaturally(dropAt, content);
        }

        giveMachineItem(block.getWorld(), dropAt, machine, player);
    }

    private void giveMachineItem(World world, Location dropAt, PlacedMachine machine, Player player) {
        String itemId = machine.definition().itemId();
        if (itemId == null) {
            plugin.getLogger().warning("Machine '" + machine.definition().id()
                    + "' at " + machine.position().describe() + " has no ITEM, nothing to return");
            return;
        }

        Optional<ItemStack> item = itemRegistry.createItem(itemId);
        if (item.isEmpty()) {
            plugin.getLogger().warning("Custom item '" + itemId + "' of machine '"
                    + machine.definition().id() + "' is not registered, nothing to return");
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.get());
        for (ItemStack stack : leftover.values()) {
            world.dropItemNaturally(dropAt, stack);
        }
    }
}
