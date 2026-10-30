package org.solocode.automotion.Machine;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.PlacedMachine;

import java.util.Map;

/**
 * Hopper bridge for machine storages.
 *
 * Inputs are pulled from a hopper above the machine (pointing down), products are
 * pushed into the hopper below. Only input slots are ever filled and only output
 * slots are ever drained, so GUI decoration can never leak into hoppers.
 */
public final class HopperTransfer {

    /**
     * @return true when at least one item moved into the machine.
     */
    public boolean pullInputs(PlacedMachine machine, Block block) {
        MachineInventory storage = machine.inventory();
        if (!storage.hasInputRoom()) {
            return false;
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!(above.getState(false) instanceof Hopper hopper)) {
            return false;
        }
        if (!(above.getBlockData() instanceof org.bukkit.block.data.type.Hopper data)
                || data.getFacing() != BlockFace.DOWN) {
            return false;
        }

        Inventory source = hopper.getInventory();
        for (int slot = 0; slot < source.getSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            int leftover = storage.insertInput(stack.clone());
            if (leftover >= stack.getAmount()) {
                continue;
            }

            if (leftover <= 0) {
                source.setItem(slot, null);
            } else {
                ItemStack remaining = stack.clone();
                remaining.setAmount(leftover);
                source.setItem(slot, remaining);
            }
            return true;
        }
        return false;
    }

    /**
     * @return true when at least one product moved into the hopper below.
     */
    public boolean pushOutputs(PlacedMachine machine, Block block) {
        MachineInventory storage = machine.inventory();
        if (!storage.hasOutputItems()) {
            return false;
        }

        Block below = block.getRelative(BlockFace.DOWN);
        if (!(below.getState(false) instanceof Hopper hopper)) {
            return false;
        }

        ItemStack product = storage.extractOutput();
        if (product == null) {
            return false;
        }

        Map<Integer, ItemStack> leftovers = hopper.getInventory().addItem(product);
        for (ItemStack leftover : leftovers.values()) {
            if (storage.addToOutput(leftover) > 0) {
                block.getWorld().dropItemNaturally(
                        block.getLocation().add(0.5, 0.5, 0.5),
                        leftover
                );
            }
        }
        return true;
    }
}
