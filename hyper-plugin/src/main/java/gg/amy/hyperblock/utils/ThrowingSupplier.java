package gg.amy.hyperblock.utils;

/**
 * @author amy
 * @since 3/22/21.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
