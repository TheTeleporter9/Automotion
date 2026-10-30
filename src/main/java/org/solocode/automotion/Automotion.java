package org.solocode.automotion;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Commands.ItemMenue.CustomItemMenu;
import org.solocode.automotion.Commands.ItemMenue.GiveItemsCommand;
import org.solocode.automotion.Commands.TestCommand;
import org.solocode.automotion.Items.AutomotionItems;
import org.solocode.automotion.Items.ItemRegistry;
import org.solocode.automotion.Machine.Gui.MachineGui;
import org.solocode.automotion.Machine.Gui.MachineGuiListener;
import org.solocode.automotion.Machine.Gui.RecipeSelectionDialog;
import org.solocode.automotion.Machine.MachineBehaviorRegistry;
import org.solocode.automotion.Machine.MachineBreakListener;
import org.solocode.automotion.Machine.MachineChunkListener;
import org.solocode.automotion.Machine.MachineContainerListener;
import org.solocode.automotion.Machine.MachineInteractListener;
import org.solocode.automotion.Machine.MachinePlacement;
import org.solocode.automotion.Machine.MachineRemoval;
import org.solocode.automotion.Machine.MachineService;
import org.solocode.automotion.Machine.Machines.DefaultProcessingBehavior;
import org.solocode.automotion.Machine.Persistence.MachinePersistence;
import org.solocode.automotion.config.MachineConfig;


public final class Automotion extends JavaPlugin {

    MachineTest machineTest;
    ItemRegistry itemRegistry;
    CustomItemMenu itemMenu;
    public CustomBlockStorage storage;
    private MachineService machineService;
    private MachineGui machineGui;

    @Override
    public void onEnable() {
        // Custom items first: machine definitions reference them.
        itemRegistry = new ItemRegistry(this);
        AutomotionItems.register(this, itemRegistry);

        MachineConfig config = MachineConfig.init(
                this,
                itemId -> itemRegistry.getItem(itemId).map(itemRegistry::createItemStack)
        );

        storage = new CustomBlockStorage(this);

        MachinePersistence persistence = new MachinePersistence(this, storage, config);
        machineGui = new MachineGui(itemRegistry);

        MachineBehaviorRegistry behaviorRegistry = new MachineBehaviorRegistry(new DefaultProcessingBehavior());
        // Special behaviors stay optional: ordinary machines use the default processor.
        // behaviorRegistry.register("CopperPress", new CopperPressBehavior());

        machineService = new MachineService(this, persistence, behaviorRegistry, machineGui);

        MachinePlacement placement = new MachinePlacement(this, config, persistence, machineService);
        MachineRemoval removal = new MachineRemoval(
                this, storage, persistence, machineService, machineGui, itemRegistry
        );
        RecipeSelectionDialog dialog = new RecipeSelectionDialog(this, machineService, machineGui);

        itemMenu = new CustomItemMenu(this, itemRegistry);

        BasicCommand testCommand = new TestCommand();
        this.registerCommand("am_test", testCommand);

        BasicCommand giveItemsCommand = new GiveItemsCommand(itemMenu);
        this.registerCommand("am_items", giveItemsCommand);

        registerEvents(itemMenu);
        registerEvents(new PlayerPlaceBlockListener(itemRegistry, storage, placement));
        registerEvents(new MachineBreakListener(machineService, persistence, removal));
        registerEvents(new MachineInteractListener(machineService, machineGui, dialog));
        registerEvents(new MachineChunkListener(machineService));
        registerEvents(new MachineGuiListener(machineService, machineGui, dialog));
        registerEvents(new MachineContainerListener(machineService));

        machineService.start();
        machineService.restoreLoadedChunks();
    }

    @Override
    public void onDisable() {
        if (machineService != null) {
            machineService.shutdown();
        }
        MachineTest.shutdownIfActive();
    }

    private void registerEvents(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
