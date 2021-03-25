package gg.amy.singyeong.client;

import gg.amy.singyeong.SingyeongClient;
import gg.amy.singyeong.data.Dispatch;
import gg.amy.singyeong.data.Invalid;
import gg.amy.vertx.SafeVertxCompletableFuture;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author amy
 * @since 10/23/18.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class SingyeongSocket {
    private final SingyeongClient singyeong;
    private final AtomicReference<WebSocket> socketRef = new AtomicReference<>(null);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter(AccessLevel.PACKAGE)
    private HttpClient client;
    private long heartbeatTask;
    
    @Nonnull
    public CompletableFuture<Void> connect() {
        final var promise = Promise.<Void>promise();
        
        client = singyeong.vertx().createHttpClient(new HttpClientOptions()
                .setMaxWebSocketFrameSize(Integer.MAX_VALUE)
                .setMaxWebSocketMessageSize(Integer.MAX_VALUE));
        promise.future().onComplete(res -> {
            if(res.failed()) {
                handleClose(null);
            }
        });
        connectLoop(promise);
        
        return SafeVertxCompletableFuture.from(singyeong.vertx(), promise.future());
    }
    
    private void connectLoop(final Promise<Void> promise) {
        logger.info("Starting Singyeong connect...");
        final WebSocketConnectOptions opts = new WebSocketConnectOptions()
                .setHost(singyeong.gatewayHost())
                .setPort(singyeong.gatewayPort())
                .setSsl(singyeong.gatewaySsl())
                .setURI("/gateway/websocket");
        client.webSocket(opts, res -> {
            if(res.succeeded()) {
                handleSocketConnect(res.result());
                promise.complete(null);
            } else {
                final var e = res.cause();
                e.printStackTrace();
                singyeong.vertx().setTimer(1_000L, __ -> connectLoop(promise));
            }
        });
    }
    
    private void handleSocketConnect(@Nonnull final WebSocket socket) {
        socket.frameHandler(this::handleFrame);
        socket.closeHandler(this::handleClose);
        socketRef.set(socket);
        logger.info("Connected to Singyeong!");
    }
    
    @SuppressWarnings("unused")
    private void handleClose(final Void __) {
        logger.warn("Disconnected from Singyeong!");
        socketRef.set(null);
        singyeong.vertx().cancelTimer(heartbeatTask);
        singyeong.vertx().setTimer(1_000L, ___ -> connectLoop(Promise.promise()));
    }
    
    private void handleFrame(@Nonnull final WebSocketFrame frame) {
        if(frame.isText()) {
            final var payload = new JsonObject(frame.textData());
            logger.trace("Received new JSON payload: {}", payload);
            final var msg = SingyeongMessage.fromJson(payload);
            logger.trace("op = {}", msg.op().name());
            switch(msg.op()) {
                case HELLO: {
                    final var heartbeatInterval = msg.data().getInteger("heartbeat_interval");
                    // IDENTIFY to allow doing everything
                    send(identify());
                    startHeartbeat(heartbeatInterval);
                    break;
                }
                case READY: {
                    // Welcome to singyeong!
                    logger.info("Welcome to singyeong!");
                    if(!singyeong.metadataCache().isEmpty()) {
                        logger.info("Refreshing metadata (we probably just reconnected)");
                        final var data = new JsonObject();
                        singyeong.metadataCache().forEach(data::put);
                        final var update = new SingyeongMessage(SingyeongOp.DISPATCH, "UPDATE_METADATA",
                                System.currentTimeMillis(),
                                data
                        );
                        send(update);
                    }
                    break;
                }
                case INVALID: {
                    final var error = msg.data().getString("error");
                    singyeong.vertx().eventBus().publish(SingyeongClient.SINGYEONG_INVALID_EVENT_CHANNEL,
                            new Invalid(error, msg.data()
                                    // lol
                                    .getJsonObject("d", new JsonObject().put("nonce", (String) null))
                                    .getString("nonce")));
                    socketRef.get().close();
                    break;
                }
                case DISPATCH: {
                    logger.trace("sending dispatch");
                    final var d = msg.data();
                    singyeong.vertx().eventBus().publish(SingyeongClient.SINGYEONG_DISPATCH_EVENT_CHANNEL,
                            new Dispatch(msg.timestamp(), d.getString("sender"), d.getString("nonce"),
                                    d.getValue("payload")));
                    break;
                }
                case HEARTBEAT_ACK: {
                    // Avoid disconnection for another day~
                    break;
                }
                default: {
                    logger.warn("Got unknown singyeong opcode " + msg.op());
                    break;
                }
            }
        }
    }
    
    public void send(@Nonnull final SingyeongMessage msg) {
        if(socketRef.get() != null) {
            socketRef.get().writeTextMessage(msg.toJson().encode());
            logger.debug("Sending singyeong payload:\n{}", msg.toJson().encodePrettily());
        }
    }
    
    private void startHeartbeat(@Nonnegative final int heartbeatInterval) {
        // Delay a second before starting just to be safe wrt IDENTIFY
        singyeong.vertx().setTimer(1_000L, __ -> {
            send(heartbeat());
            singyeong.vertx().setPeriodic(heartbeatInterval, id -> {
                heartbeatTask = id;
                if(socketRef.get() != null) {
                    send(heartbeat());
                } else {
                    singyeong.vertx().cancelTimer(heartbeatTask);
                }
            });
        });
    }
    
    private SingyeongMessage identify() {
        final var payload = new JsonObject()
                .put("client_id", singyeong.id().toString())
                .put("application_id", singyeong.appId());
        if(singyeong.authentication() != null) {
            payload.put("auth", singyeong.authentication());
        }
        payload.put("ip", ip());
        return new SingyeongMessage(SingyeongOp.IDENTIFY, null, System.currentTimeMillis(), payload);
    }
    
    @Nonnull
    private String ip() {
        if(singyeong.ip() != null && !singyeong.ip().isEmpty()) {
            return singyeong.ip();
        }
        // Attempt to support kube users who put it as an env var
        final var podIpEnv = System.getenv("POD_IP");
        if(podIpEnv != null) {
            return podIpEnv;
        }
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch(final UnknownHostException e) {
            throw new IllegalStateException("DNS broken? Can't resolve localhost!", e);
        }
    }
    
    private SingyeongMessage heartbeat() {
        return new SingyeongMessage(SingyeongOp.HEARTBEAT, null, System.currentTimeMillis(),
                new JsonObject()
                        .put("client_id", singyeong.id().toString())
        );
    }
}
