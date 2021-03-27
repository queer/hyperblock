package gg.amy.hyperblock.component.database;

import org.immutables.value.Value.Immutable;

import java.util.List;
import java.util.UUID;

/**
 * @author amy
 * @since 3/25/21.
 */
@Immutable
public interface HyperPlayer {
    UUID uuid();

    // TODO: Snapshot object?
    List<Snapshot> snapshots();

    byte[] compressedInventory();
}
