package com.sage.shengji.server.game;

import com.badlogic.gdx.Gdx;
import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.client.network.ClientPacket;
import com.sage.shengji.server.network.MultiplePlayersDisconnectedException;
import com.sage.shengji.server.network.PlayerDisconnectedException;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.server.network.ServerPacket;
import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.shengji.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class TrickRunner {
    static void playTrick(ServerGameState gameState) {
        resetGameStateForNewTrick(gameState);
        gameState.players.sendCodeToAll(ServerCode.TRICK_START);

        do {
            sendTurnPlayer(gameState);
            setTurnPlayerPlay(gameState, getValidPlayFromTurnPlayer(gameState));
            sendTurnEndPacket(gameState, setTeamsAndInvalidateFriendCardsFromLastPlay(gameState));
            gameState.turnPlayer = getNextPlayer(gameState, gameState.turnPlayer);
        } while(gameState.turnPlayer != gameState.startingPlayer);


        if(gameState.leadingPlayer.getTeam() == Team.COLLECTORS) {
            gameState.collectedPointCards.addAll(gameState.pointCardsInTrick);
        } else if(gameState.leadingPlayer.getTeam() == Team.NO_TEAM) {
            gameState.leadingPlayer.pointCards.addAll(gameState.pointCardsInTrick);
        }

        ServerPacket trickEndPacket = new ServerPacket(ServerCode.TRICK_END)
                .put("winner", gameState.leadingPlayer.getPlayerNum())
                .put("pointcards", gameState.leadingPlayer.pointCards.toCardNumList());
        gameState.players.sendPacketToAll(trickEndPacket);
    }

    private static void resetGameStateForNewTrick(ServerGameState gameState) {
        if(gameState.leadingPlayer != null) {
            gameState.turnPlayer = gameState.leadingPlayer;
        } else if(gameState.turnPlayer == null) {
            // gameState.turnPlayer shouldn't be null, but just in case
            gameState.turnPlayer = gameState.caller;
            Gdx.app.debug("playTrick()", "WARNING: gameState.turnPlayer == null. This shouldn't have " +
                    "happened. Continuing with gameState.turnPlayer = gameState.caller.");
        }
        gameState.startingPlayer = gameState.turnPlayer;

        gameState.tricksPlayed++;
        gameState.pointCardsInTrick.clear();
        gameState.leadingPlayer = null;
        gameState.basePlay = null;
        gameState.players.forEach(p -> p.play = null);
    }

    private static Player getNextPlayer(ServerGameState gameState, final Player prev) {
        int nextIdx = gameState.players.indexOf(prev) + 1;
        return (nextIdx == gameState.players.size()) ? gameState.players.get(0) : gameState.players.get(nextIdx);
    }

    private static void sendTurnEndPacket(ServerGameState gameState, CardList<ShengJiCard> invalidatedFriendCards) {
        ServerPacket turnOverPacket = new ServerPacket(ServerCode.TURN_OVER)
                .put("player", gameState.turnPlayer.getPlayerNum())
                .put("play", gameState.turnPlayer.play.toCardNumList())
                .put("leadingplayer", gameState.leadingPlayer.getPlayerNum())
                .put("invalidatedfriendcards", invalidatedFriendCards.toCardNumList())
                .put("teamsmap", gameState.players.stream()
                        .collect(Collectors.toMap(Player::getPlayerNum, Player::getTeam, (a, b) -> b, HashMap::new)))
                .put("pointcardsmap", gameState.players.stream()
                        .collect(Collectors.toMap(Player::getPlayerNum, p -> p.pointCards.toCardNumList(), (a, b) -> b, HashMap::new)))
                .put("collectedpointcards", gameState.collectedPointCards.toCardNumList());

        gameState.players.sendPacketToAll(turnOverPacket);
    }

    private static void sendTurnPlayer(ServerGameState gameState) {
        ServerPacket turnPlayerPacket = new ServerPacket(ServerCode.WAIT_FOR_TURN_PLAYER);
        turnPlayerPacket.data.put("player", gameState.turnPlayer.getPlayerNum());
        gameState.players.sendPacketToAll(turnPlayerPacket);
    }

    private static void setTurnPlayerPlay(ServerGameState gameState, Play play) {
        gameState.turnPlayer.play = play;
        gameState.turnPlayer.hand.removeAllByValue(play);
        gameState.pointCardsInTrick.addAll(play.stream()
                .filter(ShengJiCard::isPointCard)
                .collect(Collectors.toCollection(CardList::new)));
        if(gameState.basePlay == null) {
            gameState.basePlay = play;
            gameState.leadingPlayer = gameState.turnPlayer;
        } else if(play.getPlayHierarchicalValue() > gameState.leadingPlayer.play.getPlayHierarchicalValue()) {
            gameState.leadingPlayer = gameState.turnPlayer;
        }
    }

    private static CardList<ShengJiCard> setTeamsAndInvalidateFriendCardsFromLastPlay(ServerGameState gameState) {
        if(gameState.turnPlayer == gameState.caller) {
            return new CardList<>();
        }

        Play lastPlay = gameState.turnPlayer.play;
        CardList<ShengJiCard> invalidatedFriendCards;
        if(!(invalidatedFriendCards = gameState.friendCards.removeAllByValueAndGet(lastPlay)).isEmpty()) {
            gameState.turnPlayer.setTeam(Team.KEEPERS);
            gameState.turnPlayer.pointCards.clear(); // Once a player becomes a keeper, their points are discarded
            if(gameState.friendCards.isEmpty()) {
                gameState.players.stream()
                        .filter(p -> p.getTeam() == Team.NO_TEAM)
                        .forEach(p -> {
                            p.setTeam(Team.COLLECTORS);
                            gameState.collectedPointCards.addAll(p.pointCards);
                            p.pointCards.clear();
                        });
            }
        }

        return invalidatedFriendCards;
    }

    private static Play getValidPlayFromTurnPlayer(ServerGameState gameState) {
        Play play;
        gameState.turnPlayer.sendPacket(new ServerPacket(ServerCode.MAKE_PLAY));
        while(true) {
            try {
                ClientPacket packet = gameState.turnPlayer.waitForPacket();
                if(packet.networkCode != ClientCode.PLAY) {
                    continue;
                }
                List<Integer> playCardNums = Objects.requireNonNull((List<Integer>)packet.get("play"));
                CardList<ShengJiCard> playCards =
                                CardList.fromCardNumList(playCardNums, cardNum -> new ShengJiCard(cardNum, gameState));
                play = new Play(playCards, gameState);
            } catch(InterruptedException e) {
                RoundRunner.throwExceptionIfPlayerDisconnectDetected(gameState);
                continue;
            } catch(NullPointerException e) {
                gameState.turnPlayer.sendPacket(new ServerPacket(ServerCode.INVALID_PLAY)
                        .put("message", "Server encountered NullPointerException (THIS IS BAD)"));
                e.printStackTrace();
                continue;
            } catch(ClassCastException e) {
                gameState.turnPlayer.sendPacket(new ServerPacket(ServerCode.INVALID_PLAY)
                        .put("message", "Server encountered ClassCastException (THIS IS BAD)"));
                e.printStackTrace();
                continue;
            }

            PlayLegalityResult validityResult = play.getPlayLegalityResult();
            if(validityResult.isValid) {
                var successfulPlayServerPacket = new ServerPacket(ServerCode.SUCCESSFUL_PLAY)
                        .put("message", validityResult.message);
                gameState.turnPlayer.sendPacket(successfulPlayServerPacket);
                return play;
            } else {
                var invalidPlayServerPacket = new ServerPacket(ServerCode.INVALID_PLAY)
                        .put("message", validityResult.message);
                gameState.turnPlayer.sendPacket(invalidPlayServerPacket);
            }
        }
    }
}
