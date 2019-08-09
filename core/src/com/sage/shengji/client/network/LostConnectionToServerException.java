package com.sage.shengji.client.network;

public class LostConnectionToServerException extends RuntimeException {
    public LostConnectionToServerException() {
        super();
    }

    public LostConnectionToServerException(String message) {
        super(message);
    }

    public LostConnectionToServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public LostConnectionToServerException(Throwable cause) {
        super(cause);
    }

    protected LostConnectionToServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
