package org.solocode.automotion.Machine;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.MachineInventory;

/**
 * Keeps the vanilla container of a machine block empty.
 *
 * Machine items live in the machine inventory, so hopper traffic to or from the
 * vanilla block inventory is cancelled and handled by {@link HopperTransfer}
 * instead. This prevents duplicated or stranded items in either direction.
 */
public final class MachineContainerListener implements Listener {

    private final MachineService service;

    public MachineContainerListener(MachineService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (isMachineBlock(event.getSource()) || isMachineBlock(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isMachineBlock(Inventory inventory) {
        if (inventory.getHolder(false) instanceof MachineInventory.Holder) {
            return false;
        }

        Location location = inventory.getLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }

        return service.isMachine(new BlockPosition(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        ));
    }
}
