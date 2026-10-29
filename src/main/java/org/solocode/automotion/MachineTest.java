package org.solocode.automotion;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton test harness for display-entity machines.
 *
 * onEnable():   MachineTest.init(this);
 * onDisable():  MachineTest.get().shutdown();   (or MachineTest.shutdownIfActive())
 * anywhere:     MachineTest.get().spawnAtLookTarget(player);
 */
public final class MachineTest implements Listener {

    private static MachineTest instance;

    /** Call once from your plugin's onEnable(). Safe to call again; returns the existing instance. */
    public static synchronized MachineTest init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new MachineTest(plugin);
        }
        return instance;
    }

    /** Returns the instance. Throws if init() hasn't been called yet. */
    public static MachineTest get() {
        if (instance == null) {
            throw new IllegalStateException("MachineTest.init(plugin) has not been called yet");
        }
        return instance;
    }

    /** Null-safe shutdown, handy for onDisable(). */
    public static synchronized void shutdownIfActive() {
        if (instance != null) instance.shutdown();
    }

    // ------------------------------------------------------------------

    private static final Material BASE_MATERIAL = Material.COBBLESTONE;
    private static final float CLOSED = (float) Math.toRadians(15);
    private static final float OPEN = (float) Math.toRadians(60);
    private static final int STEP_TICKS = 10;

    private final JavaPlugin plugin;
    private final Map<Block, Machine> machines = new HashMap<>();
    private final BukkitTask task;

    private static final class Machine {
        ItemDisplay shaft, head, left, right;
        boolean open;
        float spin;
        int steps;

        void removeAll() {
            shaft.remove();
            head.remove();
            left.remove();
            right.remove();
        }
    }

    MachineTest(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::animate, 0L, STEP_TICKS);
    }

    /** Removes all machines (and base blocks), stops the task and listener, and clears the singleton. */
    public void shutdown() {
        task.cancel();
        HandlerList.unregisterAll(this);
        for (Map.Entry<Block, Machine> e : machines.entrySet()) {
            e.getValue().removeAll();
            e.getKey().setType(Material.AIR);
        }
        machines.clear();
        synchronized (MachineTest.class) {
            if (instance == this) instance = null;
        }
    }

    // ---------- spawning ----------

    /** Places a machine on the face of the block the player is looking at. */
    public boolean spawnAtLookTarget(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(6);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            player.sendMessage("Look at a block first.");
            return false;
        }
        Block base = hit.getHitBlock().getRelative(hit.getHitBlockFace());
        if (!base.getType().isAir()) {
            player.sendMessage("Not enough space.");
            return false;
        }
        spawn(base);
        return true;
    }

    /** Places a machine with its base at the given block. */
    public void spawn(Block base) {
        base.setType(BASE_MATERIAL);

        // Origin of all parts = center of the top face of the base block
        Location top = base.getLocation().add(0.5, 1.0, 0.5);

        Machine m = new Machine();
        m.shaft = spawnPart(top, Material.STRIPPED_BIRCH_LOG);
        m.head = spawnPart(top, Material.STRIPPED_OAK_LOG);
        m.left = spawnPart(top, Material.CUT_COPPER);
        m.right = spawnPart(top, Material.CUT_COPPER);

        m.shaft.setTransformation(new Transformation(
                new Vector3f(0f, 0.5f, 0f), new Quaternionf(),
                new Vector3f(0.15f, 1.0f, 0.15f), new Quaternionf()));
        m.head.setTransformation(headTransform(0f));
        m.left.setTransformation(armTransform(-1, CLOSED));
        m.right.setTransformation(armTransform(1, CLOSED));

        machines.put(base, m);
    }

    /** Number of active machines, handy for stress tests. */
    public int count() {
        return machines.size();
    }

    private ItemDisplay spawnPart(Location at, Material material) {
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setPersistent(false);   // test only: not saved
            d.setViewRange(0.6f);
        });
    }

    // ---------- transformations ----------

    private Transformation headTransform(float spin) {
        return new Transformation(
                new Vector3f(0f, 1.0f, 0f),
                new Quaternionf().rotateY(spin),
                new Vector3f(0.8f, 0.12f, 0.2f),
                new Quaternionf());
    }

    private Transformation armTransform(int side, float angle) {
        Quaternionf rot = new Quaternionf().rotateZ(side * angle);
        Vector3f offset = new Vector3f(0f, -0.4f, 0f).rotate(rot);
        Vector3f pos = new Vector3f(0f, 0.9f, 0f).add(offset);
        return new Transformation(pos, rot, new Vector3f(0.5f, 0.8f, 0.08f), new Quaternionf());
    }

    // ---------- animation ----------

    private void animate() {
        for (Machine m : machines.values()) {
            if (!m.head.isValid()) continue;

            m.spin = (m.spin + (float) (Math.PI / 2)) % (float) (Math.PI * 2);
            interpolate(m.head, headTransform(m.spin), STEP_TICKS);

            m.steps++;
            if (m.steps % 2 == 0) {
                m.open = !m.open;
                float angle = m.open ? OPEN : CLOSED;
                interpolate(m.left, armTransform(-1, angle), STEP_TICKS * 2);
                interpolate(m.right, armTransform(1, angle), STEP_TICKS * 2);
            }
        }
    }

    private void interpolate(Display d, Transformation target, int ticks) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(target);
    }

    // ---------- breaking the base removes the machine ----------

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Machine m = machines.remove(e.getBlock());
        if (m != null) {
            m.removeAll();
            e.setDropItems(false);
        }
    }
}