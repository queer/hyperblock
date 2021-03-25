package gg.amy.hyperblock.command;

import gg.amy.mc.cardboard.command.Command;
import gg.amy.mc.cardboard.command.Default;
import gg.amy.mc.cardboard.di.Auto;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;

/**
 * @author amy
 * @since 3/24/21.
 */
@Command(name = "iam", permissionNode = "hyperblock.commands.iam")
public class IamCommand {
    @Auto
    private Player player;
    @Auto
    private CommandSender sender;

    @Default
    public void base(final String cmd, final String[] args) {
        if(player == null) {
            sender.sendMessage(ChatColor.RED + "You aren't a player.");
            return;
        }
        final var l = player.getLocation();
        sender.sendMessage(String.format("""
                        %sYou are: %s%s%s (uuid:%s%s%s)
                        %sYou are at (%s%s%s, %s%s%s, %s%s%s) in %s%s%s.
                        """,
                GRAY, GREEN, player.getName(), GRAY, GREEN, player.getUniqueId(), GRAY,
                GRAY, GREEN, l.getBlockX(), GRAY, GREEN, l.getBlockY(), GRAY, GREEN, l.getBlockZ(), GRAY, GREEN, l.getWorld().getName(), GRAY
        ));
    }
}
