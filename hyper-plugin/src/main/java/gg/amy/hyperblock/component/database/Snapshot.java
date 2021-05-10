package gg.amy.hyperblock.component.database;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * @author amy
 * @since 3/25/21.
 */
public record Snapshot(@Nonnull UUID uuid, @Nonnull String name, @Nonnull String date) {
}
