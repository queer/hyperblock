package gg.amy.hyperblock.bukkit;

import com.github.luben.zstd.Zstd;
import gg.amy.hyperblock.utils.NbtHelpers;
import gg.amy.hyperblock.utils.ThrowingSupplier;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author amy
 * @since 3/22/21.
 */
@SuppressWarnings({"unused", "OptionalGetWithoutIsPresent"})
public final class WorldStorageBackend {
    private static final JedisPool POOL;
    private static final Collection<Integer> CMP = new LinkedList<>();
    private static final String WRITE_QUEUE = "'hyperblock:queue:worlds:dirty-chunks'";
    private static final S3Client S;
    private static final ExecutorService BACKGROUND_WRITER = Executors.newFixedThreadPool(1);
    private static final Future<?> BACKGROUND_WRITER_FUTURE;
    private static final String BUCKET = "hyperblock-chunk-store";

    static {
        final var config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(3);
        // TODO: Env
        POOL = new JedisPool(config);

        S = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("minioadmin", "minioadmin")))
                // TODO: Env
                .endpointOverride(URI.create("http://localhost:9000"))
                .region(Region.AWS_GLOBAL)
                .build();
        try {
            S.createBucket(CreateBucketRequest.builder()
                    .bucket(BUCKET)
                    .build());
        } catch(final BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            System.out.println(">> backend: storage: bucket already exists");
        }
        S.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                .bucket(BUCKET)
                .build());

        BACKGROUND_WRITER_FUTURE = BACKGROUND_WRITER.submit(() -> {
            while(true) {
                if(!((CraftServer) Bukkit.getServer()).getServer().isRunning()) {
                    return;
                }
                redis(r -> {
                    try {
                        final var dirtyChunk = r.blpop(0, WRITE_QUEUE);
                        final var dirtyKey = dirtyChunk.get(1);
//                        System.out.println(">> backend: storage: dirty chunk: " + dirtyKey);
                        final var compressedDirtyChunk = r.get(dirtyKey.getBytes());
                        s(() -> {
                            // TODO: Versioning
                            S.putObject(
                                    PutObjectRequest.builder()
                                            .key(keyToS3(dirtyKey))
                                            .bucket(BUCKET)
                                            .build(),
                                    RequestBody.fromBytes(compressedDirtyChunk)
                            );
                            return null;
                        });
                    } catch(final Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final var average = CMP.stream().mapToInt(i -> i).average().getAsDouble();
            final var max = CMP.stream().mapToInt(i -> i).max().getAsInt();
            System.out.println(">> backend: storage: avg size bytes = " + Math.round(average) + ", max size bytes = " + max);
        }));
    }

    private WorldStorageBackend() {
    }

    private static void cancelBackgroundWriter() {
        BACKGROUND_WRITER_FUTURE.cancel(true);
    }

    /**
     * Load a chunk. This bypasses the whole "region file" thing that Minecraft
     * does, as I honestly can't be bothered to figure it out. Instead, we can
     * just inspect the directory we're SUPPOSED to read from to find out what
     * world this is, then load it from our remote datastore manually.
     *
     * @param file   The directory it's SUPPOSED to be in.
     * @param coords The coordinates of the chunk.
     * @return Some valid NBT idk
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"}) // bytecode injection
    public static NBTTagCompound read(@Nonnull final File file, @Nonnull final ChunkCoordIntPair coords) {
        final var key = fileToKey(file, coords);
        final var exists = redis(r -> {
            return r.exists(key);
        });
        final byte[] bytes;
        if(!exists) {
            bytes = s(() -> {
                try(final var stream = S.getObject(GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(keyToS3(key))
                        .build())) {
                    return stream.readAllBytes();
                } catch(final NoSuchKeyException ignored) {
                    return null;
                }
            });
            if(bytes != null) {
                // Write to Redis cache
                redis(r -> {
                    r.set(key.getBytes(), bytes);
                });
            }
        } else {
            bytes = redis(r -> {
                return r.get(key.getBytes());
            });
        }
        if(bytes != null) {
            // TODO: Is 4M enough??
            return NbtHelpers.read(new ByteArrayInputStream(Zstd.decompress(bytes, 4 * 1024 * 1024)));
        } else {
            return null;
        }
    }

    /**
     * Writes a chunk. This writes the chunk to Redis then queues it up for
     * persisting.
     *
     * @param file           The directory it's SUPPOSED to be in.
     * @param coords         The coordinates of the chunk.
     * @param nbtTagCompound The NBT to save.
     */
    public static void write(@Nonnull final File file, @Nonnull final ChunkCoordIntPair coords,
                             @Nonnull final NBTTagCompound nbtTagCompound) {
        final var baos = new ByteArrayOutputStream();
        NbtHelpers.write(nbtTagCompound, baos);
        final var bytes = baos.toByteArray();
        final var compressed = Zstd.compress(bytes);
        // We get something like 9-12x compression from this
        // It's WILD
        // System.out.println(">> backend: storage: compressed " + bytes.length + " to " + compressed.length);
        CMP.add(bytes.length);
        tx(tx -> {
            final byte[] key = fileToKey(file, coords).getBytes();
            tx.set(key, compressed);
            tx.rpush(WRITE_QUEUE.getBytes(), key);
        });
    }

    private static String fileToKey(@Nonnull final File file, @Nonnull final ChunkCoordIntPair coords) {
        // TODO: Actual world name?
        final var world = file.getParentFile().getName();
        return "hyperblock:worlds:" + world + ":chunks:" + coords.x + '-' + coords.z;
    }

    private static String keyToS3(@Nonnull final String key) {
        return key.replace(':', '/');
    }

    private static void redis(@Nonnull final Consumer<Jedis> f) {
        try(@Nonnull final var r = POOL.getResource()) {
            f.accept(r);
        }
    }

    private static <T> T redis(@Nonnull final Function<Jedis, T> f) {
        try(@Nonnull final var r = POOL.getResource()) {
            return f.apply(r);
        }
    }

    private static void tx(@Nonnull final Consumer<Transaction> f) {
        redis(r -> {
            final var tx = r.multi();
            f.accept(tx);
            tx.exec();
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    private static <T> T s(final ThrowingSupplier<T> f) {
        try {
            return f.get();
        } catch(final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
