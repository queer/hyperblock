package gg.amy.hyperblock.component.database;

import org.immutables.value.Value.Immutable;

import java.util.UUID;

/**
 * @author amy
 * @since 3/25/21.
 */
@Immutable
public interface Snapshot {
    UUID uuid();

    String name();

    String date();
}
