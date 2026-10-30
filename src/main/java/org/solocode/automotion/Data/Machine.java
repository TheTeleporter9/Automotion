package org.solocode.automotion.Data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A machine definition (a machine type), never a placed machine.
 *
 * Instances are immutable and shared, so they must never hold runtime state.
 */
public record Machine(
        String id,
        String blockId,
        List<Recipe> recipes,
        MachineProperties properties,
        String itemId
) {

    public Machine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blockId, "blockId");
        recipes = List.copyOf(recipes);
        Objects.requireNonNull(properties, "properties");
    }

    /**
     * Compatibility constructor: definitions without properties or an item mapping.
     */
    public Machine(String id, String blockId, List<Recipe> recipes) {
        this(id, blockId, recipes, MachineProperties.defaults(id), null);
    }

    public Optional<Recipe> recipe(String recipeId) {
        if (recipeId == null) {
            return Optional.empty();
        }
        return recipes.stream()
                .filter(recipe -> recipe.id().equals(recipeId))
                .findFirst();
    }

    public Optional<String> item() {
        return Optional.ofNullable(itemId);
    }

    public boolean hasItem() {
        return itemId != null && !itemId.isBlank();
    }
}
