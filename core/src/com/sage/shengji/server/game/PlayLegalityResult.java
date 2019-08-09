package com.sage.shengji.server.game;

class PlayLegalityResult {
    final boolean isValid;
    final String message;

    PlayLegalityResult(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }
}
