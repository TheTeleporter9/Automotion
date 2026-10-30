package org.solocode.automotion.Machine;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.ChunkPosition;
import org.solocode.automotion.Data.MachineContext;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Machine.Gui.MachineGui;
import org.solocode.automotion.Machine.Persistence.MachinePersistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runtime registry of machines whose chunk is loaded. Machines are activated on
 * chunk load, deactivated on chunk unload and never force chunks to stay loaded.
 */
public final class MachineService {

    private static final long FLUSH_INTERVAL_TICKS = 100L;

    private final JavaPlugin plugin;
    private final MachinePersistence persistence;
    private final MachineBehaviorRegistry behaviors;
    private final MachineGui gui;
    private final HopperTransfer hopperTransfer = new HopperTransfer();
    private final MachineScheduler scheduler;

    private final Map<BlockPosition, PlacedMachine> active = new HashMap<>();
    private final Map<ChunkPosition, Set<BlockPosition>> byChunk = new HashMap<>();

    private BukkitTask flushTask;

    public MachineService(
            JavaPlugin plugin,
            MachinePersistence persistence,
            MachineBehaviorRegistry behaviors,
            MachineGui gui
    ) {
        this.plugin = plugin;
        this.persistence = persistence;
        this.behaviors = behaviors;
        this.gui = gui;
        this.scheduler = new MachineScheduler(plugin, this::tick);
    }

    public void start() {
        scheduler.start();
        if (flushTask == null) {
            flushTask = plugin.getServer().getScheduler().runTaskTimer(
                    plugin,
                    this::flushDirty,
                    FLUSH_INTERVAL_TICKS,
                    FLUSH_INTERVAL_TICKS
            );
        }
    }

    public void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }

        scheduler.stop();

        for (PlacedMachine machine : new ArrayList<>(active.values())) {
            gui.closeAll(machine);
            persistence.save(machine);
        }

        active.clear();
        byChunk.clear();
    }

    public void restoreLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                activate(chunk);
            }
        }
    }

    public void activate(Chunk chunk) {
        World world = chunk.getWorld();
        for (BlockPosition position : persistence.positionsIn(chunk)) {
            if (active.containsKey(position)) {
                continue;
            }

            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            persistence.restore(position).ifPresent(machine -> {
                if (!machine.definition().blockId().equals(block.getType().getKey().toString())) {
                    plugin.getLogger().warning("Machine '" + machine.definition().id() + "' at "
                            + position.describe() + " no longer matches the block there ("
                            + block.getType() + "), removing its stale data");
                    persistence.delete(position);
                    return;
                }
                register(machine);
            });
        }
    }

    public void deactivate(Chunk chunk) {
        Set<BlockPosition> positions = byChunk.get(ChunkPosition.of(chunk));
        if (positions == null) {
            return;
        }

        for (BlockPosition position : new ArrayList<>(positions)) {
            PlacedMachine machine = active.get(position);
            if (machine != null) {
                persistence.saveIfDirty(machine);
            }
            unregister(position);
        }
    }

    public void register(PlacedMachine machine) {
        BlockPosition position = machine.position();
        active.put(position, machine);
        byChunk.computeIfAbsent(position.chunk(), key -> new HashSet<>()).add(position);

        MachineBehavior behavior = behaviors.behaviorFor(machine.definition().id());
        scheduler.add(machine, Math.max(1, behavior.interval(machine.definition())));
    }

    public void unregister(BlockPosition position) {
        scheduler.remove(position);
        if (active.remove(position) == null) {
            return;
        }

        Set<BlockPosition> positions = byChunk.get(position.chunk());
        if (positions != null) {
            positions.remove(position);
            if (positions.isEmpty()) {
                byChunk.remove(position.chunk());
            }
        }
    }

    public PlacedMachine getOrNull(BlockPosition position) {
        return active.get(position);
    }

    public boolean isMachine(BlockPosition position) {
        return active.containsKey(position);
    }

    public Collection<PlacedMachine> active() {
        return Collections.unmodifiableCollection(active.values());
    }

    public int activeCount() {
        return active.size();
    }

    public void saveAll() {
        for (PlacedMachine machine : active.values()) {
            persistence.save(machine);
        }
    }

    public void flushDirty() {
        for (PlacedMachine machine : active.values()) {
            persistence.saveIfDirty(machine);
        }
    }

    private void tick(PlacedMachine machine) {
        Block block = resolveBlock(machine.position());
        if (block == null) {
            plugin.getLogger().warning("Machine '" + machine.definition().id() + "' at "
                    + machine.position().describe() + " left a loaded chunk, unloading it");
            persistence.saveIfDirty(machine);
            unregister(machine.position());
            return;
        }

        MachineBehavior behavior = behaviors.behaviorFor(machine.definition().id());
        int interval = Math.max(1, behavior.interval(machine.definition()));
        behavior.update(new MachineContext(machine, block, interval));

        boolean moved = hopperTransfer.pullInputs(machine, block);
        moved |= hopperTransfer.pushOutputs(machine, block);
        if (moved) {
            machine.state().markDirty();
        }

        gui.refresh(machine);
    }

    private Block resolveBlock(BlockPosition position) {
        World world = Bukkit.getWorld(position.worldID());
        if (world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return null;
        }
        return world.getBlockAt(position.x(), position.y(), position.z());
    }
}
