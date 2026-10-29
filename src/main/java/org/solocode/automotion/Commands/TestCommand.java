package org.solocode.automotion.Commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.solocode.automotion.MachineTest;

import java.lang.management.PlatformLoggingMXBean;

import static java.rmi.server.LogStream.log;

@Slf4j
public class TestCommand implements BasicCommand {
    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) throws CommandSyntaxException {

        Bukkit.getLogger().info("PLEAE WORK");

        if(commandSourceStack.getSender() instanceof Player player)
        {
            player.sendMessage(Component.text("Command ran!").color(NamedTextColor.GREEN));
            MachineTest.get().spawnAtLookTarget(player);
        } else {
            Bukkit.getLogger().warning("NOT A TERMIAL USER YOU BUM!");
        }
    }
}
