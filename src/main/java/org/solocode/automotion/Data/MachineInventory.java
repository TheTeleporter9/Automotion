package org.solocode.automotion.Data;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The real storage of a placed machine: one Bukkit inventory where the layout
 * decides which slots are inputs, outputs and GUI decoration.
 *
 * All item math happens here so behaviors, hoppers and the GUI never have to
 * know about slot numbers.
 */
public final class MachineInventory {

    private final BlockPosition position;
    private final MachineLayout layout;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final Inventory inventory;

    private MachineInventory(BlockPosition position, MachineLayout layout, Inventory inventory) {
        this.position = position;
        this.layout = layout;
        this.inputSlots = toArray(layout.inputSlots());
        this.outputSlots = toArray(layout.outputSlots());
        this.inventory = inventory;
    }

    public static MachineInventory create(BlockPosition position, MachineLayout layout) {
        Objects.requireNonNull(position, "position");
        layout.validate();

        Holder holder = new Holder(position);
        Inventory inventory = Bukkit.createInventory(holder, MachineLayout.SIZE, Component.text(layout.title()));
        holder.attach(inventory);

        return new MachineInventory(position, layout, inventory);
    }

    public BlockPosition position() {
        return position;
    }

    public MachineLayout layout() {
        return layout;
    }

    public Inventory inventory() {
        return inventory;
    }

    public boolean holdsAll(Collection<ItemStack> required) {
        ItemStack[] contents = snapshot(inputSlots);
        for (ItemStack stack : required) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!removeFrom(contents, stack)) {
                return false;
            }
        }
        return true;
    }

    public boolean removeAll(Collection<ItemStack> required) {
        int[] take = new int[inputSlots.length];

        for (ItemStack stack : required) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int remaining = stack.getAmount();
            for (int index = 0; index < inputSlots.length && remaining > 0; index++) {
                ItemStack present = inventory.getItem(inputSlots[index]);
                if (present == null || present.isEmpty() || !present.isSimilar(stack)) {
                    continue;
                }
                int available = present.getAmount() - take[index];
                if (available <= 0) {
                    continue;
                }
                int used = Math.min(available, remaining);
                take[index] += used;
                remaining -= used;
            }
            if (remaining > 0) {
                return false;
            }
        }

        for (int index = 0; index < inputSlots.length; index++) {
            if (take[index] == 0) {
                continue;
            }
            ItemStack present = inventory.getItem(inputSlots[index]);
            int left = present.getAmount() - take[index];
            if (left <= 0) {
                inventory.setItem(inputSlots[index], null);
            } else {
                ItemStack updated = present.clone();
                updated.setAmount(left);
                inventory.setItem(inputSlots[index], updated);
            }
        }
        return true;
    }

    public boolean hasRoomFor(Collection<ItemStack> results) {
        ItemStack[] contents = snapshot(outputSlots);
        for (ItemStack stack : results) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (addTo(contents, stack) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds products to the output slots.
     *
     * @return the stacks or amounts that did not fit, never lost by the inventory itself.
     */
    public List<ItemStack> addAll(Collection<ItemStack> results) {
        ItemStack[] contents = snapshot(outputSlots);
        List<ItemStack> leftovers = new ArrayList<>();

        for (ItemStack stack : results) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int remaining = addTo(contents, stack);
            if (remaining > 0) {
                ItemStack leftover = stack.clone();
                leftover.setAmount(remaining);
                leftovers.add(leftover);
            }
        }

        writeBack(outputSlots, contents);
        return leftovers;
    }

    public boolean hasInputRoom() {
        for (int slot : inputSlots) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.isEmpty() || current.getAmount() < current.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutputItems() {
        for (int slot : outputSlots) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts items into the input slots only.
     *
     * @return the amount of items that did not fit.
     */
    public int insertInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int remaining = stack.getAmount();
        int maxStack = stack.getMaxStackSize();

        for (int slot : inputSlots) {
            if (remaining <= 0) {
                break;
            }
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.isEmpty()) {
                int moved = Math.min(maxStack, remaining);
                ItemStack placed = stack.clone();
                placed.setAmount(moved);
                inventory.setItem(slot, placed);
                remaining -= moved;
            } else if (current.isSimilar(stack) && current.getAmount() < maxStack) {
                int moved = Math.min(maxStack - current.getAmount(), remaining);
                ItemStack updated = current.clone();
                updated.setAmount(current.getAmount() + moved);
                inventory.setItem(slot, updated);
                remaining -= moved;
            }
        }
        return remaining;
    }

    /**
     * Inserts items into the output slots only.
     *
     * @return the amount of items that did not fit.
     */
    public int addToOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack[] contents = snapshot(outputSlots);
        int leftover = addTo(contents, stack);
        writeBack(outputSlots, contents);
        return leftover;
    }

    /**
     * Removes one output stack, used when pushing products into a hopper.
     */
    public ItemStack extractOutput() {
        for (int slot : outputSlots) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.isEmpty()) {
                continue;
            }
            inventory.setItem(slot, null);
            return current.clone();
        }
        return null;
    }

    public List<ItemStack> inputItems() {
        return copies(inputSlots);
    }

    public List<ItemStack> outputItems() {
        return copies(outputSlots);
    }

    /**
     * Removes and returns everything stored in the input and output slots.
     * GUI decoration slots are left untouched.
     */
    public List<ItemStack> takeStoredItems() {
        List<ItemStack> stored = new ArrayList<>();
        for (int[] slots : new int[][]{inputSlots, outputSlots}) {
            for (int slot : slots) {
                ItemStack current = inventory.getItem(slot);
                if (current != null && !current.isEmpty()) {
                    stored.add(current.clone());
                    inventory.setItem(slot, null);
                }
            }
        }
        return stored;
    }

    public void restore(List<ItemStack> inputs, List<ItemStack> outputs) {
        clearSlots(inputSlots);
        clearSlots(outputSlots);
        placeInto(inputSlots, inputs);
        placeInto(outputSlots, outputs);
    }

    public void clearContents() {
        clearSlots(inputSlots);
        clearSlots(outputSlots);
    }

    private void placeInto(int[] slots, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack[] contents = snapshot(slots);
            addTo(contents, stack);
            writeBack(slots, contents);
        }
    }

    private void clearSlots(int[] slots) {
        for (int slot : slots) {
            inventory.setItem(slot, null);
        }
    }

    private List<ItemStack> copies(int[] slots) {
        List<ItemStack> items = new ArrayList<>(slots.length);
        for (int slot : slots) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.isEmpty()) {
                items.add(current.clone());
            }
        }
        return items;
    }

    private ItemStack[] snapshot(int[] slots) {
        ItemStack[] contents = new ItemStack[slots.length];
        for (int index = 0; index < slots.length; index++) {
            ItemStack current = inventory.getItem(slots[index]);
            contents[index] = current == null || current.isEmpty() ? null : current.clone();
        }
        return contents;
    }

    private void writeBack(int[] slots, ItemStack[] contents) {
        for (int index = 0; index < slots.length; index++) {
            ItemStack current = inventory.getItem(slots[index]);
            ItemStack next = contents[index];
            if (next == null) {
                if (current != null && !current.isEmpty()) {
                    inventory.setItem(slots[index], null);
                }
                continue;
            }
            if (current == null || current.isEmpty() || !current.equals(next)) {
                inventory.setItem(slots[index], next);
            }
        }
    }

    /**
     * Adds a stack to a slot array, merging into matching stacks first.
     *
     * @return the amount of items that did not fit.
     */
    private static int addTo(ItemStack[] contents, ItemStack stack) {
        int remaining = stack.getAmount();
        int maxStack = stack.getMaxStackSize();

        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack current = contents[index];
            if (current == null || !current.isSimilar(stack)) {
                continue;
            }
            int space = maxStack - current.getAmount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining);
            current.setAmount(current.getAmount() + moved);
            remaining -= moved;
        }

        for (int index = 0; index < contents.length && remaining > 0; index++) {
            if (contents[index] != null) {
                continue;
            }
            int moved = Math.min(maxStack, remaining);
            ItemStack placed = stack.clone();
            placed.setAmount(moved);
            contents[index] = placed;
            remaining -= moved;
        }

        return remaining;
    }

    private static boolean removeFrom(ItemStack[] contents, ItemStack stack) {
        int remaining = stack.getAmount();
        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack current = contents[index];
            if (current == null || !current.isSimilar(stack)) {
                continue;
            }
            int used = Math.min(current.getAmount(), remaining);
            current.setAmount(current.getAmount() - used);
            remaining -= used;
            if (current.getAmount() <= 0) {
                contents[index] = null;
            }
        }
        return remaining <= 0;
    }

    private static int[] toArray(List<Integer> slots) {
        int[] array = new int[slots.size()];
        for (int index = 0; index < slots.size(); index++) {
            array[index] = slots.get(index);
        }
        return array;
    }

    /**
     * Marker holder so machine inventories are always identifiable without
     * relying on inventory titles.
     */
    public static final class Holder implements InventoryHolder {

        private final BlockPosition position;
        private Inventory inventory;

        Holder(BlockPosition position) {
            this.position = position;
        }

        void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        public BlockPosition position() {
            return position;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
