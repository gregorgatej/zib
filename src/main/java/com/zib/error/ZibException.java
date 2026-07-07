package com.zib.error;

public class ZibException extends RuntimeException {
    public ZibException(String message) {
        super(message);
    }

    public ZibException(String message, Throwable cause) {
        super(message, cause);
    }
}
