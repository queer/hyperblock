package gg.amy.singyeong.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/**
 * @author amy
 * @since 10/24/18.
 */
public class JsonPojoCodec<T> implements MessageCodec<T, T> {
    private final Class<T> type;
    
    public JsonPojoCodec(final Class<T> type) {
        this.type = type;
    }
    
    @Override
    public void encodeToWire(final Buffer buffer, final T t) {
        buffer.appendString(JsonObject.mapFrom(t).encode());
    }
    
    @Override
    public T decodeFromWire(final int pos, final Buffer buffer) {
        final var out = Buffer.buffer();
        buffer.readFromBuffer(pos, out);
        final var data = new JsonObject(out.getString(0, out.length()));
        return data.mapTo(type);
    }
    
    @Override
    public T transform(final T t) {
        return t;
    }
    
    @Override
    public String name() {
        return "JsonPojoCodec-" + type.getName();
    }
    
    @Override
    public byte systemCodecID() {
        return -1;
    }
}
