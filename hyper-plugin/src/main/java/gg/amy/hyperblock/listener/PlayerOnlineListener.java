package gg.amy.hyperblock.listener;

import com.github.luben.zstd.Zstd;
import gg.amy.hyperblock.bukkit.SkyblockChunkGenerator;
import gg.amy.hyperblock.component.Database;
import gg.amy.hyperblock.component.database.ImmutableHyperPlayer;
import gg.amy.hyperblock.utils.NbtHelpers;
import gg.amy.mc.cardboard.di.Auto;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author amy
 * @since 3/24/21.
 */
public class PlayerOnlineListener implements Listener {
    @Auto
    private Database db;

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final var p = event.getPlayer();
        final var world = Bukkit.createWorld(new WorldCreator(p.getUniqueId().toString()).generator(new SkyblockChunkGenerator()));
        // TODO: Figure out if this is even necessary
        world.setSpawnLocation(new Location(world, 9, 128, 9));
        p.teleport(world.getSpawnLocation());

        final var hp = db.getPlayerDb().get(p.getUniqueId());
        // TODO: This should probably store the size of the array somewhere and
        //       allocate only as much buffer as needed.
        final var buffer = new byte[4 * 1024 * 1024];
        Zstd.decompress(hp.compressedInventory(), buffer);
        try(final var stream = new ByteArrayInputStream(buffer)) {
            NbtHelpers.deserializePlayerInventory(p, NbtHelpers.read(stream));
        } catch(final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        final var serialized = NbtHelpers.serializePlayerInventory(event.getPlayer());
        try(final var baos = new ByteArrayOutputStream()) {
            NbtHelpers.write(serialized, baos);
            final var compressed = Zstd.compress(baos.toByteArray());
            final var hp = ImmutableHyperPlayer.builder()
                    .from(db.getPlayerDb().get(uuid))
                    .compressedInventory(compressed)
                    .build();
            db.getPlayerDb().set(hp);
        } catch(final IOException e) {
            throw new IllegalStateException(e);
        }
        Bukkit.unloadWorld(uuid.toString(), true);
    }
}
