package org.solocode.automotion.Commands.ItemMenue;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public final class GiveItemsCommand implements BasicCommand {

    private final CustomItemMenu itemMenu;

    public GiveItemsCommand(CustomItemMenu itemMenu) {
        this.itemMenu = itemMenu;
    }

    @Override
    public void execute(
            CommandSourceStack commandSourceStack,
            String[] args
    ) {
        if (!(commandSourceStack.getSender() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    "Only players can open the item menu."
            );
            return;
        }

        itemMenu.open(player);
    }
}