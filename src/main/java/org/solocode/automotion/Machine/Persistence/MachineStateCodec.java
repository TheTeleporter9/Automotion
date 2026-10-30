package org.solocode.automotion.Machine.Persistence;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.solocode.automotion.Data.PlacedMachine;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Encodes and validates placed machine state for persistent storage.
 *
 * Uses only supported serialization: YAML for the structure and the Paper item
 * byte format for stacks. Nothing is stored as a raw Java object.
 */
public final class MachineStateCodec {

    private static final String MACHINE_KEY = "machine";
    private static final String RECIPE_KEY = "recipe";
    private static final String PROGRESS_KEY = "progress";
    private static final String INPUT_KEY = "input";
    private static final String OUTPUT_KEY = "output";

    private final Logger logger;

    public MachineStateCodec(Logger logger) {
        this.logger = logger;
    }

    public record Decoded(
            String machineId,
            String recipeId,
            int progressTicks,
            List<ItemStack> inputs,
            List<ItemStack> outputs
    ) {}

    public String encode(PlacedMachine machine) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(MACHINE_KEY, machine.definition().id());

        String recipeId = machine.state().recipeId();
        if (recipeId != null) {
            yaml.set(RECIPE_KEY, recipeId);
        }

        yaml.set(PROGRESS_KEY, machine.state().progressTicks());
        yaml.set(INPUT_KEY, encodeStacks(machine.inventory().inputItems()));
        yaml.set(OUTPUT_KEY, encodeStacks(machine.inventory().outputItems()));
        return yaml.saveToString();
    }

    public Optional<Decoded> decode(String payload) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(payload);
        } catch (InvalidConfigurationException | RuntimeException exception) {
            logger.warning("Could not read machine data: " + exception.getMessage());
            return Optional.empty();
        }

        String machineId = yaml.getString(MACHINE_KEY);
        if (machineId == null || machineId.isBlank()) {
            logger.warning("Machine data has no machine id, skipping");
            return Optional.empty();
        }

        String recipeId = yaml.getString(RECIPE_KEY);
        int progress = Math.max(0, yaml.getInt(PROGRESS_KEY, 0));

        return Optional.of(new Decoded(
                machineId,
                recipeId,
                progress,
                decodeStacks(yaml.getStringList(INPUT_KEY)),
                decodeStacks(yaml.getStringList(OUTPUT_KEY))
        ));
    }

    private List<String> encodeStacks(List<ItemStack> stacks) {
        List<String> encoded = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            encoded.add(Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
        }
        return encoded;
    }

    private List<ItemStack> decodeStacks(List<String> encoded) {
        List<ItemStack> stacks = new ArrayList<>(encoded.size());
        for (String value : encoded) {
            try {
                ItemStack stack = ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
                if (stack != null && !stack.isEmpty()) {
                    stacks.add(stack);
                }
            } catch (RuntimeException exception) {
                logger.warning("Skipping unreadable stored item: " + exception.getMessage());
            }
        }
        return stacks;
    }
}
