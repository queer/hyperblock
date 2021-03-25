package gg.amy.singyeong.data;

import lombok.Value;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 10/23/18.
 */
@Value
@Accessors(fluent = true)
public class Invalid {
    private String reason;
    private String nonce;
}
