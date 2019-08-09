package com.sage.shengji.server.network;

import com.sage.shengji.server.game.Player;
import com.sage.shengji.server.game.PlayerList;

public class MultiplePlayersDisconnectedException extends RuntimeException {
    private final PlayerList disconnectedPlayers = new PlayerList();

    public MultiplePlayersDisconnectedException() {
        super();
    }

    public MultiplePlayersDisconnectedException(PlayerList players) {
        disconnectedPlayers.addAll(players);
    }

    public MultiplePlayersDisconnectedException(String message) {
        super(message);
    }

    public MultiplePlayersDisconnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultiplePlayersDisconnectedException(Throwable cause) {
        super(cause);
    }

    protected MultiplePlayersDisconnectedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public void addDisconnectedPlayer(Player p) {
        disconnectedPlayers.add(p);
    }

    public void removeDisconnectedPlayer(Player p) {
        disconnectedPlayers.remove(p);
    }

    public void setDisconnectedPlayers(PlayerList pList) {
        disconnectedPlayers.clear();
        disconnectedPlayers.addAll(pList);
    }

    public PlayerList getDisconnectedPlayers() {
        return new PlayerList(disconnectedPlayers);
    }
}
