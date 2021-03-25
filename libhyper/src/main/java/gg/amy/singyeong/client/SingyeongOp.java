package gg.amy.singyeong.client;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnegative;

/**
 * @author amy
 * @since 10/23/18.
 */
@Accessors(fluent = true)
public enum SingyeongOp {
    HELLO(0),
    IDENTIFY(1),
    READY(2),
    INVALID(3),
    DISPATCH(4),
    HEARTBEAT(5),
    HEARTBEAT_ACK(6),
    ;
    @Getter
    private final int code;
    
    SingyeongOp(final int code) {
        this.code = code;
    }
    
    public static SingyeongOp fromOp(@Nonnegative final int op) {
        for(final var value : values()) {
            if(value.code == op) {
                return value;
            }
        }
        throw new IllegalArgumentException(op + " is not a valid opcode");
    }
}
