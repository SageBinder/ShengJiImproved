package com.sage.shengji.client.game;

public class InvalidServerPacketException extends RuntimeException {
    public InvalidServerPacketException() {
        super();
    }

    public InvalidServerPacketException(String message) {
        super(message);
    }

    public InvalidServerPacketException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidServerPacketException(Throwable cause) {
        super(cause);
    }

    protected InvalidServerPacketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
