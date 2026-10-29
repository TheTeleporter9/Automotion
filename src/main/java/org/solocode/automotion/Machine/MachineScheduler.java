package org.solocode.automotion.Machine;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.solocode.automotion.Data.Machine;
import org.solocode.automotion.Data.MachineContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MachineScheduler {
    private static final int BUCKET_COUNT = 100;

    private final JavaPlugin plugin;
    private final MachineBehaviorRegistry behaviors;
    private final List<List<ScheduledEntry>> buckets =
            new ArrayList<>(BUCKET_COUNT);

    private final Map<BlockKey, MachineInstance> machines = new HashMap<>();

    private long currentTick;
    private BukkitTask task;

    public MachineScheduler(
            JavaPlugin plugin,
            MachineBehaviorRegistry behaviors
    ) {
        this.plugin = plugin;
        this.behaviors = behaviors;

        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    public void start() {
        if (task != null) return;

        task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        machines.clear();
        buckets.forEach(List::clear);
    }

    public void add(Block block, Machine definition) {
        BlockKey key = BlockKey.from(block);

        var behavior = behaviors.get(definition.id()).orElseThrow(
                () -> new IllegalArgumentException(
                        "No behavior registered for " + definition.id()
                )
        );

        int interval = behavior.interval();
        if (interval < 1) {
            throw new IllegalArgumentException(
                    "Machine update interval must be positive"
            );
        }

        remove(block);

        MachineInstance instance = new MachineInstance(
                block, definition, behavior, interval
        );

        machines.put(key, instance);
        schedule(instance, currentTick + interval);
    }

    public void remove(Block block) {
        MachineInstance instance = machines.remove(BlockKey.from(block));

        if (instance != null) {
            instance.active = false;
        }
    }

    public int getLoadedMachineCount() {
        return machines.size();
    }

    private void schedule(MachineInstance machine, long dueTick) {
        machine.nextTick = dueTick;

        buckets.get((int) (dueTick % BUCKET_COUNT))
                .add(new ScheduledEntry(machine, dueTick));
    }

    private void tick() {
        currentTick++;

        List<ScheduledEntry> bucket =
                buckets.get((int) (currentTick % BUCKET_COUNT));

        List<ScheduledEntry> dueEntries = new ArrayList<>(bucket);
        bucket.clear();

        for (ScheduledEntry entry : dueEntries) {
            MachineInstance machine = entry.machine();

            if (!machine.active) continue;
            if (machines.get(machine.key) != machine) continue;
            if (machine.nextTick != entry.dueTick()) continue;

            if (entry.dueTick() > currentTick) {
                bucketFor(entry.dueTick()).add(entry);
                continue;
            }

            try {
                machine.behavior.update(
                        new MachineContext(machine.block, machine.definition)
                );
            } catch (Exception exception) {
                plugin.getLogger().severe(
                        "Failed to update machine "
                                + machine.definition.id()
                                + " at " + machine.block.getLocation()
                                + ": " + exception.getMessage()
                );
                exception.printStackTrace();
            }

            if (machine.active
                    && machines.get(machine.key) == machine) {
                schedule(machine, currentTick + machine.interval);
            }
        }
    }

    private List<ScheduledEntry> bucketFor(long tick) {
        return buckets.get((int) (tick % BUCKET_COUNT));
    }

    private record BlockKey(
            UUID worldId,
            int x,
            int y,
            int z
    ) {
        static BlockKey from(Block block) {
            return new BlockKey(
                    block.getWorld().getUID(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );
        }
    }

    private record ScheduledEntry(
            MachineInstance machine,
            long dueTick
    ) {}

    private static final class MachineInstance {
        private final BlockKey key;
        private final Block block;
        private final Machine definition;
        private final MachineBehavior behavior;
        private final int interval;

        private long nextTick;
        private boolean active = true;

        private MachineInstance(
                Block block,
                Machine definition,
                MachineBehavior behavior,
                int interval
        ) {
            this.key = BlockKey.from(block);
            this.block = block;
            this.definition = definition;
            this.behavior = behavior;
            this.interval = interval;
        }
    }
}