package org.solocode.automotion.Machine;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Machine.Gui.MachineGui;
import org.solocode.automotion.Machine.Gui.RecipeSelectionDialog;

/**
 * Right clicking a machine opens its GUI, or the recipe dialog when no recipe is
 * selected yet. Vanilla container behaviour is always suppressed.
 */
public final class MachineInteractListener implements Listener {

    private final MachineService service;
    private final MachineGui gui;
    private final RecipeSelectionDialog dialog;

    public MachineInteractListener(MachineService service, MachineGui gui, RecipeSelectionDialog dialog) {
        this.service = service;
        this.gui = gui;
        this.dialog = dialog;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        PlacedMachine machine = service.getOrNull(BlockPosition.of(block));
        if (machine == null) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);

        if (event.getPlayer().isSneaking()) {
            return;
        }

        event.setCancelled(true);

        if (machine.state().recipe() == null && !machine.definition().recipes().isEmpty()) {
            dialog.open(event.getPlayer(), machine);
            return;
        }

        gui.open(event.getPlayer(), machine);
    }
}
