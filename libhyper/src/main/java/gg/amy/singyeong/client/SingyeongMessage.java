package gg.amy.singyeong.client;

import io.vertx.core.json.JsonObject;
import lombok.Value;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * @author amy
 * @since 10/23/18.
 */
@Value
@Accessors(fluent = true)
public class SingyeongMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingyeongMessage.class);
    private SingyeongOp op;
    private String type;
    private long timestamp;
    JsonObject data;
    
    static SingyeongMessage fromJson(@Nonnull final JsonObject json) {
        final var d = json.getJsonObject("d");
        LOGGER.trace("decoded payload into: {}", d);
        return new SingyeongMessage(SingyeongOp.fromOp(json.getInteger("op")),
                json.getString("t", null), json.getLong("ts"), d);
    }
    
    JsonObject toJson() {
        return new JsonObject()
                .put("op", op.code())
                .put("t", type)
                .put("ts", timestamp)
                .put("d", data)
                ;
    }
}
