package org.solocode.automotion.Machine;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.Machine;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.MachineState;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Machine.Persistence.MachinePersistence;
import org.solocode.automotion.config.MachineConfig;

import java.util.Optional;

/**
 * Creates a placed machine from a placed custom item.
 */
public final class MachinePlacement {

    private final JavaPlugin plugin;
    private final MachineConfig config;
    private final MachinePersistence persistence;
    private final MachineService service;

    public MachinePlacement(
            JavaPlugin plugin,
            MachineConfig config,
            MachinePersistence persistence,
            MachineService service
    ) {
        this.plugin = plugin;
        this.config = config;
        this.persistence = persistence;
        this.service = service;
    }

    public Optional<PlacedMachine> place(Block block, String customItemId) {
        BlockPosition position = BlockPosition.of(block);

        Optional<Machine> definition = config.getMachineByItemId(customItemId);
        if (definition.isEmpty()) {
            plugin.getLogger().warning("No machine definition uses the custom item '"
                    + customItemId + "' (placed at " + position.describe() + ")");
            return Optional.empty();
        }

        Machine machine = definition.get();
        PlacedMachine placed = new PlacedMachine(
                position,
                machine,
                new MachineState(),
                MachineInventory.create(position, machine.properties().layout())
        );

        persistence.saveNew(placed);
        service.register(placed);
        return Optional.of(placed);
    }
}
