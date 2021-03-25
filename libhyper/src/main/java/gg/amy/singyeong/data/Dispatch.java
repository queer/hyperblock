package gg.amy.singyeong.data;

import lombok.Value;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 10/23/18.
 */
@Value
@Accessors(fluent = true)
public class Dispatch {
    long timestamp;
    String sender;
    String nonce;
    Object data;
}
