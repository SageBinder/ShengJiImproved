package com.sage.shengji.server.network;

import com.sage.shengji.server.game.Player;

public class PlayerDisconnectedException extends RuntimeException {
    private final Player disconnectedPlayer;

    public PlayerDisconnectedException(Player p) {
        super();
        disconnectedPlayer = p;
    }

    public PlayerDisconnectedException(Player p, String message) {
        super(message);
        disconnectedPlayer = p;
    }

    public PlayerDisconnectedException(Player p, Throwable cause) {
        super(cause);
        disconnectedPlayer = p;
    }

    public PlayerDisconnectedException(Player p, String message, Throwable cause) {
        super(message, cause);
        disconnectedPlayer = p;
    }

    protected PlayerDisconnectedException(Player p, String message, Throwable cause,
                                          boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        disconnectedPlayer = p;
    }

    public Player getDisconnectedPlayer() {
        return disconnectedPlayer;
    }
}
