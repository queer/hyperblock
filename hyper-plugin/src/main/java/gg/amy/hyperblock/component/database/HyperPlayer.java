package gg.amy.hyperblock.component.database;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * @author amy
 * @since 3/25/21.
 */
public record HyperPlayer(@Nonnull UUID uuid, @Nonnull List<Snapshot> snapshots, @Nonnull byte[] compressedInventory) {
}
