package org.solocode.automotion.Machine;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.solocode.automotion.Data.BlockPosition;
import org.solocode.automotion.Data.PlacedMachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central timing scheduler: one task for the whole server, machines spread over
 * timing buckets. There is never one task per machine.
 */
public final class MachineScheduler {

    private static final int BUCKET_COUNT = 100;

    @FunctionalInterface
    public interface TickHandler {
        void tick(PlacedMachine machine);
    }

    private final JavaPlugin plugin;
    private final TickHandler handler;
    private final List<List<ScheduledEntry>> buckets = new ArrayList<>(BUCKET_COUNT);
    private final Map<BlockPosition, MachineInstance> machines = new HashMap<>();

    private long currentTick;
    private BukkitTask task;

    public MachineScheduler(JavaPlugin plugin, TickHandler handler) {
        this.plugin = plugin;
        this.handler = handler;

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

    public void add(PlacedMachine machine, int intervalTicks) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("Machine update interval must be positive");
        }

        remove(machine.position());

        MachineInstance instance = new MachineInstance(
                machine, intervalTicks
        );

        machines.put(machine.position(), instance);
        schedule(instance, currentTick + intervalTicks);
    }

    public void remove(BlockPosition position) {
        MachineInstance instance = machines.remove(position);

        if (instance != null) {
            instance.active = false;
        }
    }

    public int size() {
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
                handler.tick(machine.placed);
            } catch (Exception exception) {
                plugin.getLogger().severe(
                        "Failed to update machine "
                                + machine.placed.definition().id()
                                + " at " + machine.key.describe()
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

    private record ScheduledEntry(
            MachineInstance machine,
            long dueTick
    ) {}

    private static final class MachineInstance {
        private final BlockPosition key;
        private final PlacedMachine placed;
        private final int interval;

        private long nextTick;
        private boolean active = true;

        private MachineInstance(PlacedMachine placed, int interval) {
            this.key = placed.position();
            this.placed = placed;
            this.interval = interval;
        }
    }
}
