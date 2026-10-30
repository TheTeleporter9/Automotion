package org.solocode.automotion.Machine.Machines;

import org.solocode.automotion.Data.MachineContext;
import org.solocode.automotion.Data.MachineInventory;
import org.solocode.automotion.Data.MachineState;
import org.solocode.automotion.Data.PlacedMachine;
import org.solocode.automotion.Data.Recipe;
import org.solocode.automotion.Machine.MachineBehavior;

/**
 * The shared recipe processor used by every ordinary machine.
 *
 * Ingredients are only consumed when the whole operation completes, so an
 * interrupted craft can never lose or duplicate items.
 */
public final class DefaultProcessingBehavior implements MachineBehavior {

    @Override
    public void update(MachineContext context) {
        PlacedMachine machine = context.machine();
        MachineState state = context.state();
        Recipe recipe = state.recipe();

        if (recipe == null) {
            state.resetProgress();
            return;
        }

        MachineInventory inventory = context.inventory();
        if (!inventory.holdsAll(recipe.recipeItems()) || !inventory.hasRoomFor(recipe.resultItems())) {
            state.resetProgress();
            return;
        }

        int duration = machine.definition().properties().durationTicks();
        int progress = state.progressTicks() + Math.max(1, context.intervalTicks());

        if (progress < duration) {
            state.setProgress(progress);
            return;
        }

        if (!inventory.removeAll(recipe.recipeItems())) {
            state.resetProgress();
            return;
        }

        for (var leftover : inventory.addAll(recipe.resultItems())) {
            context.block().getWorld().dropItemNaturally(
                    context.block().getLocation().add(0.5, 0.5, 0.5),
                    leftover
            );
        }

        state.setProgress(0);
        state.markDirty();
    }
}
