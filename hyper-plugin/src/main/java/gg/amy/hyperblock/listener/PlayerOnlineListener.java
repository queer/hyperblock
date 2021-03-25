package gg.amy.hyperblock.listener;

import gg.amy.hyperblock.bukkit.SkyblockChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author amy
 * @since 3/24/21.
 */
public class PlayerOnlineListener implements Listener {
    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final var p = event.getPlayer();
        final var world = Bukkit.createWorld(new WorldCreator(p.getUniqueId().toString()).generator(new SkyblockChunkGenerator()));
        // TODO: Figure out if this is even necessary
        world.setSpawnLocation(new Location(world, 9, 128, 9));
        p.teleport(world.getSpawnLocation());
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        Bukkit.unloadWorld(event.getPlayer().getUniqueId().toString(), true);
    }
}
