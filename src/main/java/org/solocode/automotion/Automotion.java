package org.solocode.automotion;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Commands.ItemMenue.GiveItemsCommand;
import org.solocode.automotion.Commands.TestCommand;
import org.solocode.automotion.Items.AutomotionItems;
import org.solocode.automotion.Items.ItemRegistry;
import org.solocode.automotion.Machine.MachineBehavior;
import org.solocode.automotion.Machine.MachineBehaviorRegistry;
import org.solocode.automotion.Machine.MachineScheduler;
import org.solocode.automotion.Machine.Machines.AssemblerBehavior;
import org.solocode.automotion.config.MachineConfig;
import org.solocode.automotion.Commands.ItemMenue.CustomItemMenu;


public final class Automotion extends JavaPlugin {

    MachineTest machineTest;
    ItemRegistry itemRegistry;
    CustomItemMenu itemMenu;
    public CustomBlockStorage storage;
    private MachineBehaviorRegistry behaviorRegistry;
    private MachineScheduler machineScheduler;

    @Override
    public void onEnable() {
        MachineConfig.init(this);

        behaviorRegistry = new MachineBehaviorRegistry();

        behaviorRegistry.register(
                "Test-assembler",
                new AssemblerBehavior()
        );

        machineScheduler = new MachineScheduler(this, behaviorRegistry);
        machineScheduler.start();

        storage = new CustomBlockStorage(this);

        itemRegistry = new ItemRegistry(this);
        AutomotionItems.register(this, itemRegistry);

        // Initialize the menu
        itemMenu = new CustomItemMenu(this, itemRegistry);

        // Register commands
        BasicCommand testCommand = new TestCommand();
        this.registerCommand("am_test", testCommand);

        BasicCommand giveItemsCommand = new GiveItemsCommand(itemMenu);
        this.registerCommand("am_items", giveItemsCommand);

        // Register menu event listener
        getServer().getPluginManager().registerEvents(itemMenu, this);

        getServer().getPluginManager().registerEvents(
                new PlayerPlaceBlockListener(itemRegistry, storage),
                this
        );
    }

    @Override
    public void onDisable() {
        if (machineScheduler != null) {
            machineScheduler.stop();
        }
    }
}