package com.sage.shengji.client.network;

import com.sage.shengji.utils.network.NetworkCode;

public enum ClientCode implements NetworkCode {
    PING,
    START_GAME,
    CALL,
    NO_CALL,
    NO_KITTY_CALL,
    KITTY,
    FRIEND_CARDS,
    PLAY,
    NAME,
    PLAYER_RANK_CHANGE,
    RESET_PLAYER_RANK,
    SHUFFLE_PLAYERS,
}
