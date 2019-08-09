package com.sage.shengji.server.game;

public class FatalRoundException extends RuntimeException {
    public FatalRoundException() {
        super();
    }

    public FatalRoundException(String message) {
        super(message);
    }

    public FatalRoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FatalRoundException(Throwable cause) {
        super(cause);
    }

    protected FatalRoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
