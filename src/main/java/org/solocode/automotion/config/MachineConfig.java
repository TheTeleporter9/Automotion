package org.solocode.automotion.config;


import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.solocode.automotion.Data.Machine;
import org.solocode.automotion.Data.Recipe;

import javax.crypto.Mac;
import java.io.File;
import java.io.IOException;
import java.net.http.WebSocket;
import java.sql.Driver;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Loads machine definitions from a YAML file.
 *
 * Convention: ALL-CAPS keys are reserved by the plugin, everything else is a user-chosen name.
 *
 * Expected layout:
 *
 * INOUT:
 *   Test-assembler:
 *     BLOCK: minecraft:dropper
 *     RECIPES:
 *       DiamondPressurizer:
 *         IN:
 *           - {item: minecraft:dirt, amount: 10}
 *           - {item: minecraft:coal, amount: 5}
 *         OUT:
 *           - {item: minecraft:diamond, amount: 1, chance: 1.0}
 *
 */

public final class MachineConfig {

    private static final String FILE_NAME = "Machines.yaml";
    private static final String MACHINES_ROOT_KEY = "INOUT";

    private static MachineConfig instance;

    private final YamlFileLoader fileLoader;
    private final MachineParser machineParser;
    private final Logger logger;

    private Map<String, Machine> machinesById = Map.of();
    private Map<String, Recipe> recipesById = Map.of();

    public record RecipeItems(List<ItemStack> in, List<ItemStack> out) {}

    @FunctionalInterface
    public interface ItemSource {
        Optional<ItemStack> find(String itemId);
    }

    public static ItemSource vanillaItems() {
        return itemId -> Optional.ofNullable(Material.matchMaterial(itemId))
                .filter(Material::isItem)
                .map(material -> new ItemStack(material));
    }

    public static MachineConfig init(Plugin plugin, ItemSource... customSources) {
        List<ItemSource> sources = new ArrayList<>(Arrays.asList(customSources));
        sources.add(vanillaItems());
        instance = new MachineConfig(plugin, new ItemCatalog(sources));
        instance.load();
        return instance;
    }

    public static MachineConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MachineConfig.init(plugin) was not called");
        }
        return instance;
    }

    public RecipeItems getItemsFromRecipe(String recipeId) {
        Recipe recipe = recipesById.get(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("Unknown recipe '" + recipeId + "'");
        }
        return new RecipeItems(cloneAll(recipe.recipeItems()), cloneAll(recipe.resultItems()));
    }

    public boolean hasRecipe(String recipeId) {
        return recipesById.containsKey(recipeId);
    }

    public Collection<String> getRecipeIds() {
        return recipesById.keySet();
    }

    public Collection<Recipe> getAllRecipes() {
        return recipesById.values();
    }

    public Map<String, RecipeItems> getAllRecipeItems() {
        Map<String, RecipeItems> all = new LinkedHashMap<>();
        for (String recipeId : recipesById.keySet()) {
            all.put(recipeId, getItemsFromRecipe(recipeId));
        }
        return Collections.unmodifiableMap(all);
    }

    public List<Recipe> getRecipesOfMachine(String machineId) {
        Machine machine = machinesById.get(machineId);
        return machine == null ? List.of() : machine.recipes();
    }

    public Map<String, Machine> getDefinedMachines() {
        return machinesById;
    }

    public void load() {
        ConfigurationSection machinesRoot = fileLoader.load().getConfigurationSection(MACHINES_ROOT_KEY);
        if (machinesRoot == null) {
            logger.warning(FILE_NAME + " has no " + MACHINES_ROOT_KEY + " section");
            clear();
            return;
        }
        replaceWith(ChildSectionLoader.loadAll(machinesRoot, machineParser::parse, logger));
    }

    private MachineConfig(Plugin plugin, ItemCatalog itemCatalog) {
        this.logger = plugin.getLogger();
        this.fileLoader = new YamlFileLoader(plugin, FILE_NAME);
        RecipeParser recipeParser = new RecipeParser(new ItemParser(itemCatalog, logger), logger);
        this.machineParser = new MachineParser(recipeParser, logger);
    }

    private void clear() {
        machinesById = Map.of();
        recipesById = Map.of();
    }

    private void replaceWith(List<Machine> machines) {
        machinesById = indexById(machines, Machine::id, "machine");
        recipesById = indexById(recipesOf(machines), Recipe::id, "recipe");
        logger.info("Loaded " + machinesById.size() + " machine(s), " + recipesById.size() + " recipe(s)");
    }

    private List<Recipe> recipesOf(List<Machine> machines) {
        return machines.stream().flatMap(machine -> machine.recipes().stream()).toList();
    }

    private List<ItemStack> cloneAll(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::clone).toList();
    }

    private <T> Map<String, T> indexById(Collection<T> values, Function<T, String> idOf, String label) {
        Map<String, T> index = new LinkedHashMap<>();
        for (T value : values) {
            warnIfDuplicate(index, idOf.apply(value), label);
            index.putIfAbsent(idOf.apply(value), value);
        }
        return Collections.unmodifiableMap(index);
    }

    private void warnIfDuplicate(Map<String, ?> index, String id, String label) {
        if (index.containsKey(id)) {
            logger.warning("Duplicate " + label + " id '" + id + "', keeping the first");
        }
    }

    @FunctionalInterface
    private interface SectionParser<T> {
        T parse(String id, ConfigurationSection section);
    }

    public static final class ItemCatalog {
        private final List<ItemSource> sources;

        ItemCatalog(List<ItemSource> sources) {
            this.sources = List.copyOf(sources);
        }

        Optional<ItemStack> find(String itemId) {
            return sources.stream()
                    .map(source -> source.find(itemId))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
    }

    private static final class YamlFileLoader {
        private final Plugin plugin;
        private final String fileName;

        YamlFileLoader(Plugin plugin, String fileName) {
            this.plugin = plugin;
            this.fileName = fileName;
        }

        YamlConfiguration load() {
            File file = new File(plugin.getDataFolder(), fileName);
            createIfMissing(file);
            return YamlConfiguration.loadConfiguration(file);
        }

        private void createIfMissing(File file) {
            if (file.exists()) return;
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
                return;
            }
            createEmpty(file);
        }

        private void createEmpty(File file) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName + ": " + e.getMessage());
            }
        }
    }

    private static final class ChildSectionLoader {
        private ChildSectionLoader() {}

        static <T> List<T> loadAll(ConfigurationSection parent, SectionParser<T> parser, Logger logger) {
            List<T> loaded = new ArrayList<>();
            for (String childId : parent.getKeys(false)) {
                loadChild(parent, childId, parser, logger).ifPresent(loaded::add);
            }
            return loaded;
        }

        private static <T> Optional<T> loadChild(ConfigurationSection parent, String childId,
                                                 SectionParser<T> parser, Logger logger) {
            ConfigurationSection child = parent.getConfigurationSection(childId);
            if (child == null) {
                logger.warning(parent.getCurrentPath() + "." + childId + " must be a section, skipping");
                return Optional.empty();
            }
            return parseOrSkip(child, childId, parser, logger);
        }

        private static <T> Optional<T> parseOrSkip(ConfigurationSection section, String id,
                                                   SectionParser<T> parser, Logger logger) {
            try {
                return Optional.of(parser.parse(id, section));
            } catch (IllegalArgumentException e) {
                logger.warning(section.getCurrentPath() + ": " + e.getMessage() + " (skipped)");
                return Optional.empty();
            }
        }
    }

    private static final class UnknownKeyWarner {
        private UnknownKeyWarner() {}

        static void warn(ConfigurationSection section, Set<String> allowedKeys, Logger logger) {
            for (String key : section.getKeys(false)) {
                if (!allowedKeys.contains(key)) {
                    logger.warning("Unknown key '" + key + "' in " + section.getCurrentPath());
                }
            }
        }
    }

    private static final class ItemParser {
        private static final Set<String> ALLOWED_KEYS = Set.of("item", "amount");

        private final ItemCatalog itemCatalog;
        private final Logger logger;

        ItemParser(ItemCatalog itemCatalog, Logger logger) {
            this.itemCatalog = itemCatalog;
            this.logger = logger;
        }

        List<ItemStack> parseAll(ConfigurationSection recipeSection, String key) {
            String location = recipeSection.getCurrentPath() + "." + key;
            List<Map<?, ?>> entries = recipeSection.getMapList(key);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException(location + " must be a list with at least one item");
            }
            return entries.stream().map(entry -> parseEntry(asSection(entry), location)).toList();
        }

        private ConfigurationSection asSection(Map<?, ?> rawEntry) {
            return new MemoryConfiguration().createSection("entry", rawEntry);
        }

        private ItemStack parseEntry(ConfigurationSection entry, String location) {
            UnknownKeyWarner.warn(entry, ALLOWED_KEYS, logger);
            ItemStack stack = findStack(entry.getString("item"), location);
            stack.setAmount(readAmount(entry, stack, location));
            return stack;
        }

        private ItemStack findStack(String itemId, String location) {
            if (itemId == null) {
                throw new IllegalArgumentException(location + ": 'item' is missing");
            }
            return itemCatalog.find(itemId)
                    .orElseThrow(() -> new IllegalArgumentException(location + ": unknown item '" + itemId + "'"));
        }

        private int readAmount(ConfigurationSection entry, ItemStack stack, String location) {
            int amount = entry.getInt("amount", 1);
            int maxAmount = stack.getMaxStackSize();
            if (amount < 1 || amount > maxAmount) {
                throw new IllegalArgumentException(location + ": amount must be 1-" + maxAmount);
            }
            return amount;
        }
    }

    private static final class RecipeParser {
        private static final Set<String> ALLOWED_KEYS = Set.of("IN", "OUT");

        private final ItemParser itemParser;
        private final Logger logger;

        RecipeParser(ItemParser itemParser, Logger logger) {
            this.itemParser = itemParser;
            this.logger = logger;
        }

        Recipe parse(String recipeId, ConfigurationSection section) {
            UnknownKeyWarner.warn(section, ALLOWED_KEYS, logger);
            return new Recipe(
                    recipeId,
                    itemParser.parseAll(section, "IN"),
                    itemParser.parseAll(section, "OUT"));
        }
    }

    private static final class MachineParser {
        private static final Set<String> ALLOWED_KEYS = Set.of("BLOCK", "RECIPES");

        private final RecipeParser recipeParser;
        private final Logger logger;

        MachineParser(RecipeParser recipeParser, Logger logger) {
            this.recipeParser = recipeParser;
            this.logger = logger;
        }

        Machine parse(String machineId, ConfigurationSection section) {
            UnknownKeyWarner.warn(section, ALLOWED_KEYS, logger);
            return new Machine(machineId, readBlockId(section), readRecipes(section));
        }

        private String readBlockId(ConfigurationSection section) {
            String blockId = section.getString("BLOCK");
            Material material = blockId == null ? null : Material.matchMaterial(blockId);
            if (material == null || !material.isBlock()) {
                throw new IllegalArgumentException("BLOCK is missing or not a valid block: '" + blockId + "'");
            }
            return blockId;
        }

        private List<Recipe> readRecipes(ConfigurationSection section) {
            ConfigurationSection recipesRoot = section.getConfigurationSection("RECIPES");
            if (recipesRoot == null) {
                throw new IllegalArgumentException("RECIPES section is missing");
            }
            return ChildSectionLoader.loadAll(recipesRoot, recipeParser::parse, logger);
        }
    }
}