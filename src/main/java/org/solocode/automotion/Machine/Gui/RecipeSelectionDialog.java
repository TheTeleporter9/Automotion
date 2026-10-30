package org.solocode.automotion.Machine.Gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Data.Recipe;
import org.solocode.automotion.Machine.MachineService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Native Paper dialog used to pick the recipe of one placed machine.
 *
 * The callback never trusts a client supplied id: it re-resolves the machine and
 * the recipe on the server before applying the selection.
 */
public final class RecipeSelectionDialog {

    private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(10))
            .build();

    private final JavaPlugin plugin;
    private final MachineService service;
    private final MachineGui gui;

    public RecipeSelectionDialog(JavaPlugin plugin, MachineService service, MachineGui gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
    }

    public void open(Player player, PlacedMachine machine) {
        List<Recipe> recipes = machine.definition().recipes();
        if (recipes.isEmpty()) {
            player.sendMessage(Component.text("This machine has no recipes configured."));
            return;
        }

        try {
            player.showDialog(build(machine, recipes));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not open the recipe dialog of machine '" + machine.definition().id() + "'", exception);
            player.sendMessage(Component.text("Could not open the recipe selection."));
        }
    }

    private Dialog build(PlacedMachine machine, List<Recipe> recipes) {
        List<ActionButton> actions = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            actions.add(ActionButton.builder(Component.text(recipe.id()))
                    .tooltip(Component.text(describe(recipe)))
                    .width(180)
                    .action(DialogAction.customClick(callback(machine, recipe), CLICK_OPTIONS))
                    .build());
        }

        ActionButton cancel = ActionButton.builder(Component.text("Cancel"))
                .tooltip(Component.text("Keep the current recipe"))
                .width(120)
                .action(DialogAction.customClick(
                        (DialogActionCallback) (response, audience) -> audience.closeDialog(),
                        CLICK_OPTIONS))
                .build();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(machine.definition().properties().layout().title()))
                        .body(List.of(DialogBody.plainMessage(
                                Component.text("Select the recipe this machine should process."))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actions)
                        .columns(1)
                        .exitAction(cancel)
                        .build()));
    }

    private DialogActionCallback callback(PlacedMachine machine, Recipe recipe) {
        return (response, audience) -> select(audience, machine, recipe);
    }

    private void select(Audience audience, PlacedMachine expected, Recipe recipe) {
        if (!(audience instanceof Player player)) {
            return;
        }

        PlacedMachine machine = service.getOrNull(expected.position());
        if (machine == null) {
            player.sendMessage(Component.text("This machine is no longer available."));
            return;
        }

        Optional<Recipe> valid = machine.definition().recipe(recipe.id());
        if (valid.isEmpty()) {
            player.sendMessage(Component.text("That recipe is no longer available for this machine."));
            return;
        }

        machine.state().select(valid.get());
        gui.open(player, machine);
    }

    private String describe(Recipe recipe) {
        return describe(recipe.recipeItems()) + " -> " + describe(recipe.resultItems());
    }

    private String describe(List<ItemStack> stacks) {
        return stacks.stream()
                .map(stack -> stack.getAmount() + "x "
                        + stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '))
                .collect(Collectors.joining(" + "));
    }
}
