package gg.amy.singyeong.client.query;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author amy
 * @since 10/23/18.
 */
@SuppressWarnings("unused")
public final class QueryBuilder {
    private final Collection<JsonObject> ops = new ArrayList<>();
    private boolean optional;
    private boolean restricted;
    private String hashKey;
    private String target;
    private JsonObject selector;

    public <T> QueryBuilder eq(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$eq").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder ne(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$ne").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder gt(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$gt").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder gte(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$gte").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder lt(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$lt").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder lte(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$lte").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder in(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$in").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder nin(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$nin").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder contains(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$contains").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public <T> QueryBuilder ncontains(@Nonnull final String key, @Nullable final T value) {
        ops.add(new JsonObject().put("path", key).put("op", "$ncontains").put("to", new JsonObject().put("value", value)));
        return this;
    }

    public QueryBuilder and(@Nonnull final QueryBuilder value) {
        if(value.ops.isEmpty()) {
            throw new IllegalArgumentException("Passed QueryBuilder doesn't have any ops!");
        }
        ops.add(new JsonObject().put("op", "$and").put("with", value.ops));
        return this;
    }

    public QueryBuilder or(@Nonnull final QueryBuilder value) {
        if(value.ops.isEmpty()) {
            throw new IllegalArgumentException("Passed QueryBuilder doesn't have any ops!");
        }
        ops.add(new JsonObject().put("op", "$or").put("with", value.ops));
        return this;
    }

    public QueryBuilder nor(@Nonnull final QueryBuilder value) {
        if(value.ops.isEmpty()) {
            throw new IllegalArgumentException("Passed QueryBuilder doesn't have any ops!");
        }
        ops.add(new JsonObject().put("op", "$nor").put("with", value.ops));
        return this;
    }

    public QueryBuilder selectMin(@Nonnull final String key) {
        selector = new JsonObject().put("$min", key);
        return this;
    }

    public QueryBuilder selectMax(@Nonnull final String key) {
        selector = new JsonObject().put("$max", key);
        return this;
    }

    public QueryBuilder selectAvg(@Nonnull final String key) {
        selector = new JsonObject().put("$avg", key);
        return this;
    }

    /**
     * Directly adds the specified ops to the list of ops. <strong>Input is not
     * validated.</strong>
     *
     * @param ops The ops to add.
     * @return Itself.
     */
    public QueryBuilder withOps(@Nonnull final Collection<JsonObject> ops) {
        this.ops.addAll(ops);
        return this;
    }

    public QueryBuilder optional(final boolean optional) {
        this.optional = optional;
        return this;
    }

    public QueryBuilder restricted(final boolean restricted) {
        this.restricted = restricted;
        return this;
    }

    public QueryBuilder target(@Nullable final String target) {
        this.target = target;
        return this;
    }

    public QueryBuilder hashKey(@Nullable final String hashKey) {
        this.hashKey = hashKey;
        return this;
    }

    public Query build() {
        return new Query(target, new JsonArray(List.copyOf(ops)), optional, restricted, hashKey != null, hashKey, selector);
    }
}
