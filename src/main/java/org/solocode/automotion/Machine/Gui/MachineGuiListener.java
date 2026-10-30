package org.solocode.automotion.Machine.Gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.MachineLayout;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Machine.MachineService;

import java.util.Map;

/**
 * Click handling for machine GUIs. Nothing is trusted from the client: every
 * movement is applied to the real machine inventory by this listener.
 */
public final class MachineGuiListener implements Listener {

    private final MachineService service;
    private final MachineGui gui;
    private final RecipeSelectionDialog dialog;

    public MachineGuiListener(MachineService service, MachineGui gui, RecipeSelectionDialog dialog) {
        this.service = service;
        this.gui = gui;
        this.dialog = dialog;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder(false) instanceof MachineInventory.Holder holder) {
            gui.noteOpened(holder.position());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof MachineInventory.Holder holder) {
            gui.noteClosed(holder.position());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MachineInventory.Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof MachineInventory.Holder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PlacedMachine machine = service.getOrNull(holder.position());
        if (machine == null) {
            player.closeInventory();
            return;
        }

        MachineInventory storage = machine.inventory();
        MachineLayout layout = storage.layout();
        int rawSlot = event.getRawSlot();

        if (rawSlot >= top.getSize()) {
            machine.state().markDirty();
            handlePlayerInventoryClick(event, player, storage);
            return;
        }

        if (layout.isInputSlot(rawSlot)) {
            machine.state().markDirty();
            handleInputClick(event, player, storage, rawSlot);
            return;
        }

        if (layout.isOutputSlot(rawSlot)) {
            machine.state().markDirty();
            handleOutputClick(event, player, storage, rawSlot);
            return;
        }

        if (rawSlot == layout.recipeSlot()) {
            dialog.open(player, machine);
        }
    }

    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, MachineInventory storage) {
        if (!event.getClick().isShiftClick()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty()) {
            return;
        }

        int leftover = storage.insertInput(clicked.clone());
        if (leftover >= clicked.getAmount()) {
            return;
        }

        player.getInventory().setItem(event.getSlot(), leftover <= 0 ? null : copyWithAmount(clicked, leftover));
    }

    private void handleInputClick(InventoryClickEvent event, Player player, MachineInventory storage, int slot) {
        ClickType click = event.getClick();

        if (click == ClickType.NUMBER_KEY) {
            swapWithHotbar(event, player, storage, slot);
            return;
        }
        if (click.isShiftClick()) {
            moveSlotToPlayer(player, storage, slot);
            return;
        }
        if (isUnsupportedClick(click)) {
            return;
        }

        ItemStack existing = storage.inventory().getItem(slot);
        ItemStack cursor = event.getCursor();

        if (cursor == null || cursor.isEmpty()) {
            if (existing == null || existing.isEmpty()) {
                return;
            }
            event.setCursor(existing.clone());
            storage.inventory().setItem(slot, null);
            return;
        }

        placeCursor(storage, slot, existing, cursor, event);
    }

    private void handleOutputClick(InventoryClickEvent event, Player player, MachineInventory storage, int slot) {
        ClickType click = event.getClick();

        if (click == ClickType.NUMBER_KEY || isUnsupportedClick(click)) {
            return;
        }
        if (click.isShiftClick()) {
            moveSlotToPlayer(player, storage, slot);
            return;
        }

        ItemStack existing = storage.inventory().getItem(slot);
        if (existing == null || existing.isEmpty()) {
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.isEmpty()) {
            if (!cursor.isSimilar(existing)) {
                return;
            }
            int space = Math.min(existing.getMaxStackSize(), cursor.getMaxStackSize()) - cursor.getAmount();
            if (space <= 0) {
                return;
            }
            int moved = Math.min(space, existing.getAmount());
            event.setCursor(copyWithAmount(cursor, cursor.getAmount() + moved));

            int left = existing.getAmount() - moved;
            storage.inventory().setItem(slot, left <= 0 ? null : copyWithAmount(existing, left));
            return;
        }

        event.setCursor(existing.clone());
        storage.inventory().setItem(slot, null);
    }

    private void placeCursor(MachineInventory storage, int slot, ItemStack existing, ItemStack cursor,
                             InventoryClickEvent event) {
        if (existing == null || existing.isEmpty()) {
            storage.inventory().setItem(slot, cursor.clone());
            event.setCursor(null);
            return;
        }

        if (!existing.isSimilar(cursor)) {
            return;
        }

        int maxStack = Math.min(existing.getMaxStackSize(), cursor.getMaxStackSize());
        int space = maxStack - existing.getAmount();
        if (space <= 0) {
            return;
        }

        int moved = Math.min(space, cursor.getAmount());
        storage.inventory().setItem(slot, copyWithAmount(existing, existing.getAmount() + moved));

        int left = cursor.getAmount() - moved;
        event.setCursor(left <= 0 ? null : copyWithAmount(cursor, left));
    }

    private void moveSlotToPlayer(Player player, MachineInventory storage, int slot) {
        ItemStack existing = storage.inventory().getItem(slot);
        if (existing == null || existing.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(existing.clone());
        storage.inventory().setItem(slot, leftovers.isEmpty() ? null : leftovers.values().iterator().next());
    }

    private void swapWithHotbar(InventoryClickEvent event, Player player, MachineInventory storage, int slot) {
        int hotbar = event.getHotbarButton();
        if (hotbar < 0 || hotbar > 8) {
            return;
        }

        ItemStack hotbarItem = player.getInventory().getItem(hotbar);
        ItemStack slotItem = storage.inventory().getItem(slot);

        boolean hotbarFilled = hotbarItem != null && !hotbarItem.isEmpty();
        boolean slotFilled = slotItem != null && !slotItem.isEmpty();
        if (hotbarFilled && slotFilled && !slotItem.isSimilar(hotbarItem)) {
            return;
        }

        storage.inventory().setItem(slot, hotbarFilled ? hotbarItem.clone() : null);
        player.getInventory().setItem(hotbar, slotFilled ? slotItem.clone() : null);
    }

    private boolean isUnsupportedClick(ClickType click) {
        return switch (click) {
            case SWAP_OFFHAND, DROP, CONTROL_DROP, DOUBLE_CLICK,
                 MIDDLE, CREATIVE, UNKNOWN, WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT -> true;
            default -> false;
        };
    }

    private ItemStack copyWithAmount(ItemStack stack, int amount) {
        ItemStack copy = stack.clone();
        copy.setAmount(amount);
        return copy;
    }
}
