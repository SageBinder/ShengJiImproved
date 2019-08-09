package com.sage.shengji.utils.card;

public class InvalidCardException extends RuntimeException {
    public InvalidCardException() {
        super();
    }

    public InvalidCardException(String message) {
        super(message);
    }

    public InvalidCardException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCardException(Throwable cause) {
        super(cause);
    }

    protected InvalidCardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
