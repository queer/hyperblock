package gg.amy.hyperblock.bukkit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * @author amy
 * @since 3/23/21.
 */
public class SkyblockChunkGenerator extends ChunkGenerator {
    private final List<BlockPopulator> populators = List.of(new SkyblockPopulator());

    @Nonnull
    @Override
    public ChunkData generateChunkData(@Nonnull final World world, @Nonnull final Random random, final int x,
                                       final int z, @Nonnull final BiomeGrid biome) {
        final var chunk = createChunkData(world);
        for(int ix = 0; ix < 16; ix++) {
            for(int iy = 0; iy < chunk.getMaxHeight(); iy++) {
                for(int iz = 0; iz < 16; iz++) {
                    chunk.setBlock(ix, iy, iz, Material.AIR);
                    biome.setBiome(ix, iy, iz, Biome.OCEAN);
                }
            }
        }

        if(x == 0 && z == 0) {
            // If this is the spawn chunk, build out a little platform
            for(int ix = 0; ix < 5; ix++) {
                for(int iz = 0; iz < 5; iz++) {
                    chunk.setBlock(6 + ix, 127, 6 + iz, Material.GRASS_BLOCK);
                    chunk.setBlock(6 + ix, 126, 6 + iz, Material.DIRT);
                    chunk.setBlock(6 + ix, 126, 6 + iz, Material.DIRT);
                }
            }

            // Add leaves for our tree
            for(int ix = 0; ix < 5; ix++) {
                for(int iz = 0; iz < 5; iz++) {
                    chunk.setBlock(7 + ix, 130, 7 + iz, Material.OAK_LEAVES);
                    chunk.setBlock(7 + ix, 131, 7 + iz, Material.OAK_LEAVES);
                    chunk.setBlock(7 + ix, 132, 7 + iz, Material.OAK_LEAVES);
                }
            }
            for(int ix = 0; ix < 3; ix++) {
                for(int iz = 0; iz < 3; iz++) {
                    chunk.setBlock(7 + ix, 133, 7 + iz, Material.OAK_LEAVES);
                    chunk.setBlock(7 + ix, 134, 7 + iz, Material.OAK_LEAVES);
                }
            }
            chunk.setBlock(8, 135, 8, Material.OAK_LEAVES);

            // Then add the logs
            chunk.setBlock(8, 128, 8, Material.OAK_LOG);
            chunk.setBlock(8, 129, 8, Material.OAK_LOG);
            chunk.setBlock(8, 130, 8, Material.OAK_LOG);
            chunk.setBlock(8, 131, 8, Material.OAK_LOG);
            chunk.setBlock(8, 132, 8, Material.OAK_LOG);
        }
        return chunk;
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(@NotNull final World world) {
        return populators;
    }

    @Nullable
    @Override
    public Location getFixedSpawnLocation(@NotNull final World world, @NotNull final Random random) {
        return new Location(world, 0.5D, 128, 0.5D);
    }
}
