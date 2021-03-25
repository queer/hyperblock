package gg.amy.hyperblock.bukkit;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * @author amy
 * @since 3/24/21.
 */
public class SkyblockPopulator extends BlockPopulator {
    @Override
    public void populate(@NotNull final World world, @NotNull final Random random, @NotNull final Chunk chunk) {
        if(chunk.getX() == 0 && chunk.getZ() == 0) {
            System.out.println(">> hyper: worldgen: populating spawn chunk");
            final var loc = new Location(world, 9, 128, 9);
            loc.getBlock().setType(Material.CHEST);
            final var state = (Chest) loc.getBlock().getState();
            final var inv = state.getBlockInventory();
            inv.addItem(
                    new ItemStack(Material.COBWEB, 12),
                    new ItemStack(Material.RED_MUSHROOM, 1),
                    new ItemStack(Material.BROWN_MUSHROOM, 1),
                    new ItemStack(Material.LAVA_BUCKET, 1),
                    new ItemStack(Material.ICE, 2),
                    new ItemStack(Material.BONE_MEAL, 2),
                    new ItemStack(Material.PUMPKIN_SEEDS, 1),
                    new ItemStack(Material.CACTUS, 1),
                    new ItemStack(Material.SUGAR_CANE, 1)
            );
        }
    }
}
