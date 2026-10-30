package org.solocode.automotion.Machine.Persistence;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.Machine;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.MachineState;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Data.Recipe;
import org.solocode.automotion.config.MachineConfig;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Stores placed machine state in the chunk persistent data through the existing
 * custom block storage. Persistent identity is independent from runtime instances.
 */
public final class MachinePersistence {

    private final Logger logger;
    private final CustomBlockStorage storage;
    private final MachineConfig config;
    private final MachineStateCodec codec;

    public MachinePersistence(JavaPlugin plugin, CustomBlockStorage storage, MachineConfig config) {
        this.logger = plugin.getLogger();
        this.storage = storage;
        this.config = config;
        this.codec = new MachineStateCodec(logger);
    }

    /**
     * Persists a brand new machine and registers it in its chunk index.
     */
    public void saveNew(PlacedMachine machine) {
        Block block = blockOf(machine.position());
        if (block == null) {
            logger.warning("Could not persist machine at " + machine.position().describe() + ": chunk not loaded");
            return;
        }
        storage.setMachineData(block, codec.encode(machine));
        storage.addMachine(block);
        machine.state().markClean();
    }

    public void save(PlacedMachine machine) {
        Block block = blockOf(machine.position());
        if (block == null) {
            return;
        }
        storage.setMachineData(block, codec.encode(machine));
        machine.state().markClean();
    }

    public void saveIfDirty(PlacedMachine machine) {
        if (machine.state().isDirty()) {
            save(machine);
        }
    }

    public void delete(BlockPosition position) {
        Block block = blockOf(position);
        if (block == null) {
            return;
        }
        storage.removeMachineData(block);
        storage.removeMachine(block);
    }

    public List<BlockPosition> positionsIn(Chunk chunk) {
        return storage.getMachines(chunk);
    }

    public Optional<PlacedMachine> restore(BlockPosition position) {
        Block block = blockOf(position);
        if (block == null) {
            return Optional.empty();
        }

        String payload = storage.getMachineData(block).orElse(null);
        if (payload == null) {
            return Optional.empty();
        }

        MachineStateCodec.Decoded decoded = codec.decode(payload).orElse(null);
        if (decoded == null) {
            logger.warning("Dropping unreadable machine data at " + position.describe());
            delete(position);
            return Optional.empty();
        }

        Optional<Machine> definition = config.getMachine(decoded.machineId());
        if (definition.isEmpty()) {
            logger.warning("Machine definition '" + decoded.machineId() + "' at "
                    + position.describe() + " is no longer configured, keeping its data untouched");
            return Optional.empty();
        }

        Machine machine = definition.get();
        MachineInventory inventory = MachineInventory.create(position, machine.properties().layout());
        inventory.restore(decoded.inputs(), decoded.outputs());

        Recipe recipe = null;
        if (decoded.recipeId() != null) {
            recipe = machine.recipe(decoded.recipeId()).orElse(null);
            if (recipe == null) {
                logger.warning("Recipe '" + decoded.recipeId() + "' of machine '" + machine.id()
                        + "' at " + position.describe() + " no longer exists, clearing the selection");
            }
        }

        MachineState state = new MachineState();
        state.restore(recipe, decoded.progressTicks());

        return Optional.of(new PlacedMachine(position, machine, state, inventory));
    }

    private Block blockOf(BlockPosition position) {
        World world = Bukkit.getWorld(position.worldID());
        if (world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return null;
        }
        return world.getBlockAt(position.x(), position.y(), position.z());
    }
}
