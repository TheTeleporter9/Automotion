package org.solocode.automotion.Machine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Keeps runtime machines in sync with loaded chunks.
 */
public final class MachineChunkListener implements Listener {

    private final MachineService service;

    public MachineChunkListener(MachineService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        service.activate(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.deactivate(event.getChunk());
    }
}
