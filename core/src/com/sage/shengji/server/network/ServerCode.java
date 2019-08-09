package com.sage.shengji.server.network;

import com.sage.shengji.utils.network.NetworkCode;

public enum ServerCode implements NetworkCode {
    // General codes:
    PING,
    CONNECTION_ACCEPTED,
    CONNECTION_DENIED,
    PLAYER_DISCONNECTED,
    COULD_NOT_START_GAME,
    UNSUCCESSFUL_NAME_CHANGE,
    WAIT_FOR_PLAYERS,
    NEW_PLAYER_RANK,

    // If something goes wrong during a round, the server will send this code to all clients and then abort the round
    FATAL_ROUND_ERROR,

    // Calling codes:
    MAKE_CALL,
    SUCCESSFUL_CALL,
    UNSUCCESSFUL_CALL,
    INVALID_CALL,
    NO_CALL,
    WAIT_FOR_NEW_LEADING_CALL,
    WAIT_FOR_CALL_WINNER,

    // Kitty calling codes:
    NO_ONE_CALLED,
    WAIT_FOR_KITTY_CARD,
    MAKE_KITTY_CALL,
    INVALID_KITTY_CALL,
    UNSUCCESSFUL_KITTY_CALL,
    SUCCESSFUL_KITTY_CALL,
    NO_KITTY_CALL,
    WAIT_FOR_KITTY_CALL_WINNER,
    KITTY_EXHAUSTED_REDEAL,

    WAITING_ON_CALLER,

    // Kitty codes:
    WAIT_FOR_KITTY,
    SEND_KITTY,
    INVALID_KITTY,
    SUCCESSFUL_KITTY,

    // Friend cards codes:
    WAIT_FOR_FRIEND_CARDS,
    SEND_FRIEND_CARDS,
    INVALID_FRIEND_CARDS,
    SUCCESSFUL_FRIEND_CARDS,

    // Trick codes:
    TRICK_START,
    MAKE_PLAY,
    INVALID_PLAY,
    SUCCESSFUL_PLAY,
    WAIT_FOR_TURN_PLAYER,
    WAIT_FOR_NEW_PLAY,
    TURN_OVER,
    TRICK_END,

    // Round codes:
    ROUND_START,
    WAIT_FOR_HAND,
    ROUND_END,
}
