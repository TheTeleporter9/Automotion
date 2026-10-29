package org.solocode.automotion;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.solocode.automotion.Blocks.CustomBlockStorage;
import org.solocode.automotion.Commands.ItemMenue.GiveItemsCommand;
import org.solocode.automotion.Commands.TestCommand;
import org.solocode.automotion.Items.AutomotionItems;
import org.solocode.automotion.Items.ItemRegistry;
import org.solocode.automotion.Machine.Listeners.PlayerPlaceBlockListener;
import org.solocode.automotion.config.MachineConfig;
import org.solocode.automotion.Commands.ItemMenue.CustomItemMenu;

import java.io.File;


public final class Automotion extends JavaPlugin {

    MachineTest machineTest;
    ItemRegistry itemRegistry;
    CustomItemMenu itemMenu;
    public CustomBlockStorage storage;

    @Override
    public void onEnable() {
        MachineConfig.init(this);

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
        if (machineTest != null) machineTest.shutdown();
    }
}