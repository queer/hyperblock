package gg.amy.singyeong;

import gg.amy.singyeong.client.SingyeongMessage;
import gg.amy.singyeong.client.SingyeongOp;
import gg.amy.singyeong.client.SingyeongSocket;
import gg.amy.singyeong.client.SingyeongType;
import gg.amy.singyeong.client.query.Query;
import gg.amy.singyeong.data.Dispatch;
import gg.amy.singyeong.data.Invalid;
import gg.amy.singyeong.data.ProxiedRequest;
import gg.amy.singyeong.util.JsonPojoCodec;
import gg.amy.vertx.SafeVertxCompletableFuture;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author amy
 * @since 10/23/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Accessors(fluent = true)
public final class SingyeongClient {
    public static final String SINGYEONG_DISPATCH_EVENT_CHANNEL = "singyeong:event:dispatch";
    public static final String SINGYEONG_INVALID_EVENT_CHANNEL = "singyeong:event:invalid";
    @Getter
    private final Vertx vertx;
    @Getter
    private final WebClient client;
    @Getter
    private final String serverUrl;
    @Getter
    private final String gatewayHost;
    @Getter
    private final int gatewayPort;
    @Getter
    private final boolean gatewaySsl;
    @Getter
    private final String appId;
    @Getter
    private final String authentication;
    @Getter
    private final String ip;
    @Getter
    private final Map<String, JsonObject> metadataCache = new ConcurrentHashMap<>();
    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final List<String> tags;
    @Getter
    private SingyeongSocket socket;
    
    private SingyeongClient(@Nonnull final Vertx vertx, @Nonnull final String dsn) {
        this(vertx, dsn, Collections.emptyList());
    }
    
    private SingyeongClient(@Nonnull final Vertx vertx, @Nonnull final String dsn, @Nonnull final List<String> tags) {
        this(vertx, dsn, null, tags);
    }
    
    private SingyeongClient(@Nonnull final Vertx vertx, @Nonnull final String dsn, @Nullable final String ip) {
        this(vertx, dsn, ip, Collections.emptyList());
    }
    
    private SingyeongClient(@Nonnull final Vertx vertx, @Nonnull final String dsn, @Nullable final String ip,
                            @Nonnull final List<String> tags) {
        this.vertx = vertx;
        client = WebClient.create(vertx);
        this.ip = ip;
        try {
            final var uri = new URI(dsn);
            String server = "";
            final var scheme = uri.getScheme();
            if(scheme.equalsIgnoreCase("singyeong")) {
                server += "ws://";
            } else if(scheme.equalsIgnoreCase("ssingyeong")) {
                server += "wss://";
            } else {
                throw new IllegalArgumentException(scheme + " is not a valid singyeong URI scheme (expected 'singyeong' or 'ssingyeong')");
            }
            server += uri.getHost();
            if(uri.getPort() > -1) {
                server += ":" + uri.getPort();
            }
            serverUrl = server.replaceFirst("ws", "http");
            gatewayHost = uri.getHost();
            gatewayPort = uri.getPort() > -1 ? uri.getPort() : 80;
            gatewaySsl = server.startsWith("wss://");
            final String userInfo = uri.getUserInfo();
            if(userInfo == null) {
                throw new IllegalArgumentException("Didn't pass auth to singyeong DSN!");
            }
            final var split = userInfo.split(":", 2);
            appId = split[0];
            authentication = split.length != 2 ? null : split[1];
            this.tags = Collections.unmodifiableList(tags);
            
            vertx.eventBus().registerDefaultCodec(Dispatch.class, new FakeCodec<>());
            vertx.eventBus().registerDefaultCodec(Invalid.class, new FakeCodec<>());
        } catch(final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid singyeong URI!", e);
        }
    }
    
    public static SingyeongClient create(@Nonnull final String dsn) {
        return create(Vertx.vertx(new VertxOptions()
                        .setWorkerPoolSize(1)
                        .setEventLoopPoolSize(1)
                        .setInternalBlockingPoolSize(1)),
                dsn);
    }
    
    @SuppressWarnings("WeakerAccess")
    public static SingyeongClient create(@Nonnull final Vertx vertx, @Nonnull final String dsn) {
        return new SingyeongClient(vertx, dsn);
    }
    
    public static SingyeongClient create(@Nonnull final String dsn, @Nonnull final List<String> tags) {
        return new SingyeongClient(Vertx.vertx(new VertxOptions()
                .setWorkerPoolSize(2)
                .setEventLoopPoolSize(2)
                .setInternalBlockingPoolSize(2)),
                dsn, tags);
    }
    
    public static SingyeongClient create(@Nonnull final Vertx vertx, @Nonnull final String dsn,
                                         @Nonnull final List<String> tags) {
        return new SingyeongClient(vertx, dsn, tags);
    }
    
    public static SingyeongClient create(@Nonnull final Vertx vertx, @Nonnull final String dsn, @Nullable final String ip) {
        return new SingyeongClient(vertx, dsn, ip);
    }
    
    public static SingyeongClient create(@Nonnull final Vertx vertx, @Nonnull final String dsn, @Nullable final String ip,
                                         @Nonnull final List<String> tags) {
        return new SingyeongClient(vertx, dsn, ip, tags);
    }
    
    @Nonnull
    public CompletableFuture<Void> connect() {
        final var promise = Promise.<Void>promise();
        socket = new SingyeongSocket(this);
        socket.connect()
                .thenAccept(__ -> promise.complete(null))
                .exceptionally(throwable -> {
                    promise.fail(throwable);
                    return null;
                });
        
        return SafeVertxCompletableFuture.from(vertx, promise.future());
    }
    
    /**
     * Proxies an HTTP request to the target returned by the routing query.
     *
     * @param request The request to proxy.
     *
     * @return A future that completes with the response body when the request
     * is complete.
     */
    public CompletableFuture<Buffer> proxy(@Nonnull final ProxiedRequest request) {
        final var promise = Promise.<Buffer>promise();
        final var headers = new JsonObject();
        request.headers();
        request.headers().asMap().forEach((k, v) -> headers.put(k, new JsonArray(new ArrayList<>(v))));
        
        final var query = request.query();
        final var payload = new JsonObject()
                .put("method", request.method().name().toUpperCase())
                // Assume that route is / if none specified
                .put("route", request.route() == null ? "/" : request.route())
                .put("headers", headers)
                .put("body", request.body())
                .put("query", new JsonObject()
                        .put("optional", query.optional())
                        .put("restricted", query.restricted())
                        .put("application", query.target())
                        .put("ops", query.ops())
                        .put("selector", query.selector())
                );
        if(query.consistent()) {
            payload.getJsonObject("query").put("key", query.hashKey());
        }
        
        client.postAbs(serverUrl + "/api/v1/proxy").putHeader("Authorization", authentication)
                .sendJson(payload, ar -> {
                    if(ar.succeeded()) {
                        final var result = ar.result();
                        promise.complete(result.body());
                    } else {
                        promise.fail(ar.cause());
                    }
                });
        
        return SafeVertxCompletableFuture.from(vertx, promise.future());
    }
    
    /**
     * Handle events dispatched from the server.
     *
     * @return The consumer, in case you want to unregister it.
     */
    public MessageConsumer<Dispatch> onEvent(@Nonnull final Consumer<Dispatch> consumer) {
        return vertx.eventBus().consumer(SINGYEONG_DISPATCH_EVENT_CHANNEL, m -> consumer.accept(m.body()));
    }
    
    /**
     * Handle messages from the server telling you that you sent a bad message.
     *
     * @return The consumer, in case you want to unregister it.
     */
    public MessageConsumer<Invalid> onInvalid(@Nonnull final Consumer<Invalid> consumer) {
        return vertx.eventBus().consumer(SINGYEONG_INVALID_EVENT_CHANNEL, m -> consumer.accept(m.body()));
    }
    
    public <T> void send(@Nonnull final Query query, @Nullable final T payload) {
        send(query, null, payload);
    }
    
    public <T> void send(@Nonnull final Query query, @Nullable final String nonce, @Nullable final T payload) {
        send(query, nonce, false, payload);
    }
    
    public <T> void send(@Nonnull final Query query, @Nullable final String nonce, final boolean optional, @Nullable final T payload) {
        send(query, nonce, optional, false, payload);
    }
    
    public <T> void send(@Nonnull final Query query, @Nullable final String nonce, final boolean optional,
                         final boolean droppable, @Nullable final T payload) {
        final var msg = createDispatch("SEND", nonce, query, optional, droppable, payload);
        socket.send(msg);
    }
    
    public <T> void broadcast(@Nonnull final Query query, @Nullable final T payload) {
        send(query, null, payload);
    }
    
    public <T> void broadcast(@Nonnull final Query query, @Nullable final String nonce, @Nullable final T payload) {
        send(query, nonce, false, payload);
    }
    
    public <T> void broadcast(@Nonnull final Query query, @Nullable final String nonce, final boolean optional, @Nullable final T payload) {
        send(query, nonce, optional, false, payload);
    }
    
    public <T> void broadcast(@Nonnull final Query query, @Nullable final String nonce, final boolean optional,
                         final boolean droppable, @Nullable final T payload) {
        final var msg = createDispatch("BROADCAST", nonce, query, optional, droppable, payload);
        socket.send(msg);
    }
    
    private <T> SingyeongMessage createDispatch(@Nonnull final String type, @Nullable final String nonce,
                                                @Nonnull final Query query, final boolean optional, final boolean droppable,
                                                @Nullable final T payload) {
        final var data = new JsonObject()
                .put("sender", id.toString())
                .put("target", new JsonObject()
                        .put("optional", query.optional())
                        .put("restricted", query.restricted())
                        .put("application", query.target())
                        .put("ops", query.ops())
                )
                .put("nonce", nonce);
        if(payload instanceof String || payload instanceof JsonObject || payload instanceof JsonArray) {
            data.put("payload", payload);
        } else {
            data.put("payload", JsonObject.mapFrom(payload));
        }
        if(query.consistent()) {
            data.getJsonObject("query").put("key", query.hashKey());
        }
        return new SingyeongMessage(SingyeongOp.DISPATCH, type, System.currentTimeMillis(), data);
    }
    
    /**
     * Update this client's metadata on the server.
     *
     * @param key  The metadata key to set.
     * @param type The type of the metadata. Will be validated by the server.
     * @param data The value to set for the metadata key.
     * @param <T>  The Java type of the metadata.
     */
    public <T> void updateMetadata(@Nonnull final String key, @Nonnull final SingyeongType type, @Nonnull final T data) {
        final var metadataValue = new JsonObject().put("type", type.name().toLowerCase()).put("value", data);
        metadataCache.put(key, metadataValue);
        final var msg = new SingyeongMessage(SingyeongOp.DISPATCH, "UPDATE_METADATA",
                System.currentTimeMillis(),
                new JsonObject().put(key, metadataValue)
        );
        socket.send(msg);
    }
    
    private <T> void codec(@Nonnull final Class<T> cls) {
        vertx.eventBus().registerDefaultCodec(cls, new JsonPojoCodec<>(cls));
    }
    
    private static class FakeCodec<T> implements MessageCodec<T, Object> {
        @Override
        public void encodeToWire(final Buffer buffer, final T dispatch) {
        }
        
        @Override
        public Object decodeFromWire(final int pos, final Buffer buffer) {
            return null;
        }
        
        @Override
        public Object transform(final T dispatch) {
            return dispatch;
        }
        
        @Override
        public String name() {
            return "noop" + new Random().nextInt();
        }
        
        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
}
