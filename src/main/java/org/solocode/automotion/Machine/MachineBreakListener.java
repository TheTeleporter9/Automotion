package org.solocode.automotion.Machine;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Machine.Persistence.MachinePersistence;

/**
 * Turns a broken machine block into its custom item and drops its contents.
 */
public final class MachineBreakListener implements Listener {

    private final MachineService service;
    private final MachinePersistence persistence;
    private final MachineRemoval removal;

    public MachineBreakListener(MachineService service, MachinePersistence persistence, MachineRemoval removal) {
        this.service = service;
        this.persistence = persistence;
        this.removal = removal;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockPosition position = BlockPosition.of(block);

        PlacedMachine machine = service.getOrNull(position);
        if (machine == null) {
            // Safety net for a machine whose chunk data survived while its runtime
            // instance did not exist: restore it so its contents are not lost.
            machine = persistence.restore(position).orElse(null);
        }
        if (machine == null) {
            return;
        }

        event.setDropItems(false);
        removal.remove(block, machine, event.getPlayer());
    }
}
