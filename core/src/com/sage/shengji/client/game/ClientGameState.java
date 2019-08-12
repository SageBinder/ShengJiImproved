package com.sage.shengji.client.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.client.network.ClientConnection;
import com.sage.shengji.client.network.ClientPacket;
import com.sage.shengji.client.network.LostConnectionToServerException;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.server.network.ServerPacket;
import com.sage.shengji.utils.card.*;
import com.sage.shengji.utils.renderable.RenderableCardGroup;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.renderable.RenderableCardList;
import com.sage.shengji.utils.renderable.RenderableHand;
import com.sage.shengji.utils.shengji.ShengJiGameState;
import com.sage.shengji.utils.shengji.Team;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ClientGameState extends ShengJiGameState {
    // Light Goldenrod
    public static final Color winningPlayColor = new Color(238f / 255f, 221f / 255f, 130f / 255f, 1f);
    public static final Color basePlayColor = Color.LIGHT_GRAY;
    public static final Color turnPlayerNameColor = Color.LIGHT_GRAY;

    public static final Color invalidatedFriendCardBackgroundColor = new Color(Color.LIGHT_GRAY);
    public static final Color invalidatedFriendCardBorderColor = new Color(Color.RED);

    public static final Color keepersNameColor = new Color(Color.GREEN);
    public static final Color collectorsNameColor = new Color(Color.ORANGE);
    public static final Color noTeamNameColor = new Color(Color.WHITE);

    private Updater updater = new Updater();
    private final ShengJiGame game;
    public final Actions actions = new Actions();

    public final ArrayList<RenderablePlayer> players = new ArrayList<>();

    public RenderablePlayer turnPlayer;
    public RenderablePlayer leadingPlayer;
    public RenderablePlayer hostPlayer;

    public RenderablePlayer thisPlayer;
    public final RenderableHand<RenderableShengJiCard> thisPlayerHand = new RenderableHand<>();
    public final RenderableCardList<RenderableShengJiCard> thisPlayerCurrentCall = new RenderableCardList<>();

    public final RenderableCardGroup<RenderableShengJiCard> collectedPointCards = new RenderableCardGroup<>();
    public int numCollectedPoints = 0;
    public int numPointsNeeded = 0;

    public int kittySize = 0;
    public final RenderableCardGroup<RenderableShengJiCard> kitty = new RenderableCardGroup<>();
    public final RenderableCardGroup<RenderableShengJiCard> friendCards = new RenderableCardGroup<>();

    public final RenderableCardList<RenderableShengJiCard> lastAttemptedKitty = new RenderableCardList<>();

    public RenderableShengJiCard trumpCard;

    public ServerCode lastServerCode;
    public String message = "";
    public String errorMessage = "";

    public ClientGameState(ShengJiGame game) {
        this.game = game;
    }

    public void clean() {
        players.clear();
        hostPlayer = null;
        thisPlayer = null;
        cleanNoPlayersReset();
    }

    private void cleanNoPlayersReset() {
        turnPlayer = null;
        leadingPlayer = null;

        thisPlayerHand.clear();
        thisPlayerCurrentCall.clear();

        collectedPointCards.clear();
        numCollectedPoints = 0;
        numPointsNeeded = 0;

        kitty.clear();
        friendCards.clear();

        setTrump(null, null);
    }

    public boolean update(ClientConnection client) {
        if(client == null) {
            return false;
        }

        // It's important to only update at most a single packet. If more than one packet is updated and
        // some packet changes the screen, it won't give the new screen a chance to update with the latest packet.
        // The new screen won't have any idea what state it should be in. Updating from gameState in show()
        // doesn't work because gameState may change after show() is called.
        // POSSIBLE SOLUTION: New screen should update from gameState on its first render.
        try {
            Optional<ServerPacket> p = client.getPacket();
            return (p.isPresent()) && applyUpdate(p.get());
        } catch(LostConnectionToServerException e) {
            updater.lostConnectionToServer();
            return true;
        }
    }

    private boolean applyUpdate(ServerPacket updatePacket) {
        return updater.update(updatePacket.networkCode, updatePacket.data);
    }

    private Optional<RenderablePlayer> getPlayerByNum(Integer playerNum) {
        return (playerNum == null)
                ? Optional.empty()
                : players.stream().filter(player -> player.getPlayerNum() == playerNum).findFirst();
    }

    private class Updater {
        private Map<Serializable, Serializable> data = null;
        private final CardList<RenderableShengJiCard> pointCardsInTrick = new CardList<>();

        private boolean update(ServerCode serverCode, Map<Serializable, Serializable> data) {
            if(serverCode != ServerCode.PING) {
                lastServerCode = serverCode;
            }
            this.data = data;
            try {
                switch(lastServerCode) {
                    // General codes:
                case PING:
                    ping(); break;
                case CONNECTION_ACCEPTED:
                    connectionAccepted(); break;
                case CONNECTION_DENIED:
                    connectionDenied(); break;
                case PLAYER_DISCONNECTED:
                    playerDisconnected(); break;
                case COULD_NOT_START_GAME:
                    couldNotStartGame(); break;
                case UNSUCCESSFUL_NAME_CHANGE:
                    unsuccessfulNameChange(); break;
                case WAIT_FOR_PLAYERS:
                    waitForPlayers(); break;
                case NEW_PLAYER_RANK:
                    newPlayerRank(); break;

                    // Fatal round error:
                case FATAL_ROUND_ERROR:
                    fatalRoundError(); break;

                    // Calling codes:
                case MAKE_CALL:
                    makeCall(); break;
                case SUCCESSFUL_CALL:
                    successfulCall(); break;
                case UNSUCCESSFUL_CALL:
                    unsuccessfulCall(); break;
                case INVALID_CALL:
                    invalidCall(); break;
                case NO_CALL:
                    noCall(); break;
                case WAIT_FOR_NEW_LEADING_CALL:
                    waitForNewLeadingCall(); break;
                case WAIT_FOR_CALL_WINNER:
                    waitForCallWinner(); break;

                    // Kitty calling codes:
                case NO_ONE_CALLED:
                    noOneCalled(); break;
                case WAIT_FOR_KITTY_CARD:
                    waitForKittyCard(); break;
                case MAKE_KITTY_CALL:
                    makeKittyCall(); break;
                case INVALID_KITTY_CALL:
                    invalidKittyCall(); break;
                case UNSUCCESSFUL_KITTY_CALL:
                    unsuccessfulKittyCall(); break;
                case SUCCESSFUL_KITTY_CALL:
                    successfulKittyCall(); break;
                case NO_KITTY_CALL:
                    noKittyCall(); break;
                case WAIT_FOR_KITTY_CALL_WINNER:
                    waitForKittyCallWinner(); break;
                case KITTY_EXHAUSTED_REDEAL:
                    kittyExhaustedRedeal(); break;

                case WAITING_ON_CALLER:
                    waitingOnCaller(); break;

                    // Kitty codes:
                case WAIT_FOR_KITTY:
                    waitForKitty(); break;
                case SEND_KITTY:
                    sendKitty(); break;
                case INVALID_KITTY:
                    invalidKitty(); break;
                case SUCCESSFUL_KITTY:
                    successfulKitty(); break;

                    // Friend cards codes:
                case WAIT_FOR_FRIEND_CARDS:
                    waitForFriendCards(); break;
                case SEND_FRIEND_CARDS:
                    sendFriendCards(); break;
                case INVALID_FRIEND_CARDS:
                    invalidFriendCards(); break;
                case SUCCESSFUL_FRIEND_CARDS:
                    successfulFriendCards(); break;

                    // Trick codes:
                case TRICK_START:
                    trickStart(); break;
                case MAKE_PLAY:
                    makePlay(); break;
                case INVALID_PLAY:
                    invalidPlay(); break;
                case SUCCESSFUL_PLAY:
                    successfulPlay(); break;
                case WAIT_FOR_TURN_PLAYER:
                    waitForTurnPlayer(); break;
                case TURN_OVER:
                    turnOver(); break;
                case TRICK_END:
                    trickEnd(); break;

                    // Round codes:
                case ROUND_START:
                    roundStart(); break;
                case WAIT_FOR_HAND:
                    waitForHand(); break;
                case ROUND_END:
                    roundEnd(); break;
                }
            } catch(ClassCastException | NullPointerException | InvalidServerPacketException | InvalidCardException e) {
                Gdx.app.log("Updater.update()",
                        "Oh shit encountered ClassCastException/InvalidServerPacketException/NullPointerException/InvalidCardException "
                                + "in Updater.update(), this is VERY BAD\n"
                                + e.getMessage());
                e.printStackTrace();
                errorMessage = e.getClass() + ": " + e.getMessage();
                // TODO: When encountering an error here, request the full game state from the server
            }
            return serverCode != ServerCode.PING; // PING does not constitute an update as of now
        }

        // --- GENERAL CODES ---
        // ping() is just here in case anything needs to be done on ping (and to keep the switch pattern)
        private void ping() {
        }

        private void connectionAccepted() {
            message = "Joined successfully!";
        }

        private void connectionDenied() {
            errorMessage = "[YELLOW]Error joining game: connection denied. Maybe the game is full or has already started?";
            game.showStartScreen();
        }

        private void playerDisconnected() {
            errorMessage = "[YELLOW]A player has disconnected!";
            game.showLobbyScreen();
        }

        private void couldNotStartGame() {
            errorMessage = "[YELLOW]Error: cannot start game.";
        }

        private void unsuccessfulNameChange() {
            errorMessage = "[YELLOW]Error: invalid name.";
        }

        private void waitForPlayers() {
            var newPlayersMap = (Map<Integer, String>)data.get("players");
            var callRankMap = (Map<Integer, Rank>)data.get("rank");
            int clientPlayerNum = (Integer)(data.get("you"));
            int hostPlayerNum = (Integer)(data.get("host"));

            players.clear();
            for(int playerNum : newPlayersMap.keySet()) {
                RenderablePlayer newPlayer = new RenderablePlayer(playerNum, newPlayersMap.get(playerNum));
                newPlayer.setHost(playerNum == hostPlayerNum);
                newPlayer.setIsClientPlayer(playerNum == clientPlayerNum);
                newPlayer.setCallRank(callRankMap.getOrDefault(playerNum, Rank.TWO));
                players.add(newPlayer);
            }

            thisPlayer = getPlayerByNum(clientPlayerNum).orElseThrow(() -> new InvalidServerPacketException(
                    "waitForPlayers() - No player found with player num " +
                            clientPlayerNum +
                            " sent by server for client player"
            ));
            hostPlayer = getPlayerByNum(hostPlayerNum).orElseThrow(() -> new InvalidServerPacketException(
                    "waitForPlayers() - No player found with player num " +
                            clientPlayerNum +
                            " sent by server for host player"
            ));
        }

        private void newPlayerRank() {
            RenderablePlayer updatedPlayer = getPlayerByNum((Integer)data.get("player"))
                    .orElseThrow(() -> new InvalidServerPacketException(
                            "allPlayerPoints() - No player found with player num "
                                    + data.get("player")
                                    + " sent by server for updated player"));
            Rank newRank = (Rank)data.get("rank");
            updatedPlayer.setCallRank((newRank != null) ? newRank : updatedPlayer.getCallRank());
        }

        // --- FATAL ROUND ERROR ---
        private void fatalRoundError() {
            errorMessage = "[RED]Server encountered a fatal round error! This is very bad!";
            cleanNoPlayersReset();
            game.showLobbyScreen();
        }

        // --- CALLING CODES ---
        private void makeCall() {
            message = "Make call";
        }

        private void successfulCall() {
            message = "Successful call";

            int callCardNum = (Integer)data.get("cardnum");
            int callOrder = (Integer)data.getOrDefault("order", 0);

            if(callOrder == 0) {
                throw new InvalidServerPacketException("successfulCall() - Server sent 0 for callOrder");
            }

            if(callCardNum == thisPlayer.getPlay().get(0).getCardNum() && callOrder == thisPlayer.getPlay().size()) {
                thisPlayerCurrentCall.clear();
                thisPlayerCurrentCall.addAll(thisPlayer.getPlay());
            } else {
                var callFromHand = thisPlayerHand.stream()
                        .filter(c -> c.getCardNum() == callCardNum)
                        .limit(callOrder)
                        .collect(Collectors.toCollection(CardList::new));
                thisPlayerHand.addAll(thisPlayer.getPlay());
                thisPlayerHand.removeAll(callFromHand);
                thisPlayer.clearPlay();
                thisPlayer.getPlay().addAll(callFromHand);
                thisPlayerCurrentCall.clear();
                thisPlayerCurrentCall.addAll(callFromHand);
            }
        }

        private void unsuccessfulCall() {
            errorMessage = "[YELLOW]Call not strong enough";

            thisPlayerHand.addAll(thisPlayer.getPlay());
            thisPlayer.clearPlay();
            thisPlayer.getPlay().addAll(thisPlayerCurrentCall);
        }

        private void invalidCall() {
            errorMessage = "[YELLOW]Invalid call, try again";

            thisPlayerHand.addAll(thisPlayer.getPlay());
            thisPlayer.clearPlay();
            thisPlayer.getPlay().addAll(thisPlayerCurrentCall);
        }

        private void noCall() {
            message = "You have chosen not to call";

            thisPlayerHand.addAll(thisPlayer.getPlay()); // thisPlayer.getPlay() SHOULD be empty
            thisPlayer.clearPlay();
        }

        private void waitForNewLeadingCall() {
            var callLeader = getPlayerByNum((Integer)data.get("playernum"))
                    .orElseThrow(() -> new InvalidServerPacketException("waitForNewLeadingCall - " +
                            "no player found with player num " + data.get("playernum") +
                            "sent by server for callLeader"
                    ));
            int callCardNum = (Integer)data.get("cardnum");
            int callOrder = (Integer)data.get("order");

            if(callOrder == 0) {
                throw new InvalidServerPacketException("waitForNewLeadingCall() - Server sent 0 for callOrder");
            }

            players.forEach(p -> p.setNameColor(null));
            callLeader.setNameColor(Color.GREEN);

            if(callLeader != thisPlayer) {
                thisPlayerHand.addAll(thisPlayer.getPlay());
                players.forEach(RenderablePlayer::clearPlay);
                thisPlayerCurrentCall.clear();
                for(int i = 0; i < callOrder; i++) {
                    callLeader.getPlay().add(new RenderableShengJiCard(callCardNum, ClientGameState.this));
                }
                message = callLeader.getColoredName() + " is now leading the call";
            }
            // If thisPlayer is already the call leader, it should be safe to ignore this packet
        }

        private void waitForCallWinner() {
            var callWinner = getPlayerByNum((Integer)data.get("playernum"))
                    .orElseThrow(() -> new InvalidServerPacketException("waitForCallWinner - " +
                            "no player found with player num " + data.get("playernum") +
                            "sent by server for callWinner"
                    ));
            Card callCard = new Card((Integer)data.get("callcardnum"));
            int callOrder = (Integer)data.get("callorder");

            if(callOrder == 0) {
                throw new InvalidServerPacketException("waitForCallWinner() - Server sent 0 for callOrder");
            }

            setTrump(callCard.getRank(), callCard.getSuit());
            callWinner.setTeam(Team.KEEPERS);
            callWinner.setNameColor(getTeamNameColor(Team.KEEPERS));

            if(thisPlayer != callWinner) {
                thisPlayerHand.addAll(thisPlayer.getPlay());
            }
            players.stream().filter(p -> p != callWinner).forEach(RenderablePlayer::clearPlay);

            players.forEach(p -> p.setNameColor(null));
            callWinner.setNameColor(Color.GREEN);

            if(callWinner == thisPlayer) {
                message = "You ";
            } else {
                message = callWinner.getColoredName() + " ";
            }
            message += "won the call with " + callCard.toAbbrevString() + " (" + callOrder + "x)";
        }

        // --- KITTY CALLING CODES ---
        private void noOneCalled() {
            message = "No one called. Now drawing cards from the kitty.";
        }

        private void waitForKittyCard() {
            kitty.add(new RenderableShengJiCard((Integer)data.get("cardnum"), ClientGameState.this));
        }

        private void makeKittyCall() {
            // Nothing needs to be done here?
        }

        private void invalidKittyCall() {
            errorMessage = "[YELLOW]Invalid kitty call. Try again.";
            thisPlayerHand.addAll(thisPlayer.getPlay());
            thisPlayer.clearPlay();
        }

        private void unsuccessfulKittyCall() {
            errorMessage = "[YELLOW]Somebody already called. (Is the host/your client lagging?)";
            thisPlayerHand.addAll(thisPlayer.getPlay());
            thisPlayer.clearPlay();
        }

        private void successfulKittyCall() {
            message = "You won the kitty call";
        }

        private void noKittyCall() {
            noCall();
        }

        private void waitForKittyCallWinner() {
            var callWinner = getPlayerByNum((Integer)data.get("playernum"))
                    .orElseThrow(() -> new InvalidServerPacketException("waitForCallWinner - " +
                            "no player found with player num " + data.get("playernum") +
                            "sent by server for callWinner"
                    ));
            Card callCard = new Card((Integer)data.get("callcardnum"));

            setTrump(callWinner.getCallRank(), callCard.getSuit());
            callWinner.setTeam(Team.KEEPERS);
            callWinner.setNameColor(getTeamNameColor(Team.KEEPERS));

            if(thisPlayer != callWinner) {
                thisPlayerHand.addAll(thisPlayer.getPlay());
            }
            players.stream().filter(p -> p != callWinner).forEach(RenderablePlayer::clearPlay);

            players.forEach(p -> p.setNameColor(null));
            callWinner.setNameColor(Color.GREEN);

            if(callWinner == thisPlayer) {
                message = "You ";
            } else {
                message = callWinner.getColoredName() + " ";
            }
            message += "won the call with " + callCard.toAbbrevString();
        }

        private void kittyExhaustedRedeal() {
            message = "All kitty cards have been shown and no one called. Triggering a redeal.";

            thisPlayerHand.clear();
            thisPlayerCurrentCall.clear();
            kitty.clear();
            players.forEach(RenderablePlayer::clearCards);
        }

        private void waitingOnCaller() {
            message = "Waiting for call winner to fill kitty";
            thisPlayerHand.addAll(thisPlayer.getPlay());
            players.forEach(RenderablePlayer::clearCards);
            thisPlayerCurrentCall.clear();
            kitty.clear();
        }

        // --- KITTY CODES ---
        private void waitForKitty() {
            CardList<RenderableShengJiCard> kittyCards = CardList.fromCardNumList(
                    (List<Integer>)data.get("kitty"),
                    cardNum -> new RenderableShengJiCard(cardNum, ClientGameState.this));
            kittySize = kittyCards.size();
            thisPlayerHand.addAll(kittyCards);

            kitty.clear();
        }

        private void sendKitty() {
            message = "Select " + kittySize + " card" + (kittySize == 1 ? "" : "s") + " to put into the kitty";
        }

        private void invalidKitty() {
            errorMessage = "Invalid kitty. " +
                    "Select " + kittySize + " card" + (kittySize == 1 ? "" : "s") + " to put into the kitty";
            thisPlayerHand.addAll(lastAttemptedKitty);
            lastAttemptedKitty.clear();
        }

        private void successfulKitty() {
            message = "Server received kitty successfully";
        }

        // --- FRIEND CARDS CODES ---
        private void waitForFriendCards() {
            friendCards.clear();
            friendCards.addAll(CardList.fromCardNumList(
                    (List<Integer>)data.get("cardnums"),
                    cardNum -> new RenderableShengJiCard(cardNum, ClientGameState.this)));

            if(friendCards.size() == 0) {
                players.stream().filter(p -> p.getTeam() == Team.NO_TEAM).forEach(p -> {
                    p.setTeam(Team.COLLECTORS);
                    p.setNameColor(getTeamNameColor(Team.COLLECTORS));
                });
            }
        }

        private void sendFriendCards() {
            int numFriendCards = (Integer)data.get("numfriendcards");

            message = "Select " + numFriendCards + " friend card" + (numFriendCards == 1 ? "" : "s");

            friendCards.clear();
            for(int i = 0; i < numFriendCards; i++) {
                friendCards.add(new RenderableShengJiCard(Rank.ACE, Suit.fromCardNum(i % 4), ClientGameState.this));
            }
        }

        private void invalidFriendCards() {
            errorMessage = "[YELLOW]Invalid friend cards. This shouldn't have happened unless you're trying to cheat.";
        }

        private void successfulFriendCards() {
            message = "Server received friend cards successfully";
        }

        // --- TRICK CODES ---
        private void trickStart() {
            pointCardsInTrick.clear();
            players.forEach(RenderablePlayer::clearPlay);
        }

        private void makePlay() {
            message = "It's your turn";
            turnPlayer = thisPlayer;
        }

        private void invalidPlay() {
            errorMessage = "[YELLOW]Invalid play - "
                    + Optional.ofNullable(data.get("message")).orElse("(no message received from server)")
                    + "[]";

            thisPlayerHand.addAll(thisPlayer.getPlay());
            thisPlayer.clearPlay();
        }

        private void successfulPlay() {
            if(players.stream().filter(p -> !p.getPlay().isEmpty()).count() == 1) {
                thisPlayer.getPlay().forEach(c -> {
                    c.entity.defaultFaceBackgroundColor.set(basePlayColor);
                    c.entity.resetFaceBackgroundColor();
                });
            }

            message = "Server received play successfully";
        }

        private void waitForTurnPlayer() {
            turnPlayer = getPlayerByNum((Integer)data.get("player"))
                    .orElseThrow(() -> new InvalidServerPacketException(
                            "waitForTurnPlayer() - No player found with player num "
                                    + data.get("player")
                                    + " sent by server for turn player"));
            message = "It's "
                    + "P" + turnPlayer.getPlayerNum() + ": " + turnPlayer.getColoredName()
                    + (turnPlayer.getName().charAt(turnPlayer.getName().length() - 1) == 's' ? "'" : "'s") // Pluralizing
                    + " turn";
        }

        private void turnOver() {
            RenderablePlayer player = getPlayerByNum((Integer)data.get("player"))
                    .orElseThrow(() -> new InvalidServerPacketException(
                            "turnOver() - No player found with player num "
                                    + data.get("player")
                                    + " sent by server for turn player"));
            CardList<RenderableShengJiCard> play = CardList.fromCardNumList(
                    (List<Integer>)data.get("play"),
                    cardNum -> new RenderableShengJiCard(cardNum, ClientGameState.this));
            RenderablePlayer leadingPlayer = getPlayerByNum((Integer)data.get("leadingplayer"))
                    .orElseThrow(() -> new InvalidServerPacketException(
                            "turnOver() - No player found with player num "
                                    + data.get("player")
                                    + " sent by server for leadingPlayer"));
            List<Integer> invalidatedFriendCards = (List<Integer>)data.get("invalidatedfriendcards");
            Map<Integer, Team> teamsMap = (Map<Integer, Team>)data.get("teamsmap");

            for(var playerNum : teamsMap.keySet()) {
                RenderablePlayer p = getPlayerByNum(playerNum)
                        .orElseThrow(() -> new InvalidServerPacketException(
                                "turnOver() - No player found with player num "
                                        + playerNum
                                        + " sent by server for a player in teamsMap"));
                Team team = teamsMap.get(playerNum);
                p.setTeam(team);
                p.setNameColor(getTeamNameColor(team));
                if(team == Team.COLLECTORS) {
                    collectedPointCards.addAll(p.getPointCards());
                    numCollectedPoints = collectedPointCards.stream().mapToInt(ShengJiCard::getPoints).sum();
                    p.getPointCards().clear();
                } else if(team == Team.KEEPERS) {
                    p.getPointCards().clear();
                }
            }

            for(int invalidatedFriendCardNum : invalidatedFriendCards) {
                friendCards.stream()
                        .filter(c -> c.getCardNum() == invalidatedFriendCardNum)
                        .findFirst()
                        .ifPresent(c -> {
                            c.entity.defaultFaceBackgroundColor.set(invalidatedFriendCardBackgroundColor);
                            c.entity.defaultFaceBorderColor.set(invalidatedFriendCardBorderColor);
                            c.entity.resetFaceBackgroundColor();
                            c.entity.resetFaceBackgroundColor();
                        });
            }

            if(player != thisPlayer) {
                pointCardsInTrick.addAll(play.stream()
                        .filter(ShengJiCard::isPointCard)
                        .collect(Collectors.toCollection(CardList::new)));
                player.getPlay().clear();
                player.getPlay().addAll(play);
            } else {
                // If thisPlayer.getPlay() does not match what the server sent, resync the plays
                if(!play.toCardNumList().stream().allMatch(c -> thisPlayer.getPlay().toCardNumList().indexOf(c) > -1)) {
                    thisPlayerHand.addAll(thisPlayer.getPlay());
                    thisPlayer.clearPlay();
                    thisPlayer.getPlay().addAll(play);
                }
                pointCardsInTrick.addAll(thisPlayer.getPlay().stream()
                        .filter(ShengJiCard::isPointCard)
                        .collect(Collectors.toCollection(CardList::new)));
            }

            if(players.stream().filter(p -> !p.getPlay().isEmpty()).count() == 1) {
                player.getPlay().forEach(c -> {
                    c.entity.defaultFaceBackgroundColor.set(basePlayColor);
                    c.entity.resetFaceBackgroundColor();
                });
            }


            players.forEach(p -> p.getPlay().forEach(c -> c.entity.resetFaceBackgroundColor()));
            leadingPlayer.getPlay().forEach(c -> c.entity.setFaceBackgroundColor(winningPlayColor));
        }

        private void trickEnd() {
            RenderablePlayer trickWinner = getPlayerByNum((Integer)data.get("winner"))
                    .orElseThrow(() -> new InvalidServerPacketException(
                            "trickEnd() - No player found with player num "
                                    + data.get("winner")
                                    + " sent by server for trick winner"
                    ));
            leadingPlayer = trickWinner;

            if(!pointCardsInTrick.isEmpty()) {
                pointCardsInTrick.forEach(c -> {
                    c.entity.defaultFaceBackgroundColor.set(Color.WHITE);
                    c.entity.resetFaceBackgroundColor();
                });

                if(trickWinner.getTeam() == Team.NO_TEAM) {
                    trickWinner.getPointCards().addAll(pointCardsInTrick);
                    trickWinner.getPointCards().sort(ShengJiCard::compareTo);
                } else if(trickWinner.getTeam() == Team.COLLECTORS) {
                    collectedPointCards.addAll(pointCardsInTrick);
                    numCollectedPoints = collectedPointCards.stream().mapToInt(ShengJiCard::getPoints).sum();
                    collectedPointCards.sort(ShengJiCard::compareTo);
                }

            }

            message = trickWinner.getName() + " won the trick. Collects "
                    + pointCardsInTrick.stream().mapToInt(ShengJiCard::getPoints).sum()
                    + " points";

            pointCardsInTrick.clear();
        }

        // --- ROUND CODES ---
        private void roundStart() {
            cleanNoPlayersReset();

            int[] playerOrder = (int[])data.get("playerorder");
            ArrayList<RenderablePlayer> tempPlayerList = new ArrayList<>(playerOrder.length);

            for(int i = 0; i < playerOrder.length; i++) {
                int _i = i;
                tempPlayerList.add(getPlayerByNum(playerOrder[i])
                        .orElseThrow(() -> new InvalidServerPacketException(
                                "roundStart() - No player found with player num " + playerOrder[_i] +
                                        " sent by server for a player in playerOrder"
                        )));
            }
            players.clear();
            players.addAll(tempPlayerList);

            game.showGameScreen();
        }

        private void waitForHand() {
            thisPlayerHand.clear();
            thisPlayerHand.addAll(CardList.fromCardNumList(
                    (List<Integer>)data.get("hand"),
                    cardNum -> new RenderableShengJiCard(cardNum, ClientGameState.this)));
            numPointsNeeded = (Integer)data.get("numpointsneeded");
        }

        private void roundEnd() {
            Team winningTeam = (Team)data.get("winningteam");
            List<Integer> winningPlayerNums = (List<Integer>)data.get("winningplayers");
            CardList<RenderableShengJiCard> kittyCards = CardList.fromCardNumList(
                    (List<Integer>)data.get("kittycards"),
                    cardNum -> new RenderableShengJiCard(cardNum, ClientGameState.this));
            int kittyPointsMultiplier = (Integer)data.get("kittypointsmultiplier");
            int collectedPointsBeforeKitty = (Integer)data.get("collectedpointsbeforekitty");
            int totalCollectedPoints = (Integer)data.get("totalcollectedpoints");
            int rankIncrease = (Integer)data.get("rankincrease");

            List<RenderablePlayer> winningPlayers = winningPlayerNums.stream()
                    .map(playerNum -> getPlayerByNum(playerNum).orElseThrow(() -> new InvalidServerPacketException(
                            "roundEnd() - No player found with player num " + playerNum +
                                    " sent by server for a player in winningPlayers"
                    )))
                    .collect(Collectors.toCollection(ArrayList::new));
            int kittyPoints = kittyCards.stream().mapToInt(ShengJiCard::getPoints).sum();

            numCollectedPoints = totalCollectedPoints;

            kitty.clear();
            kitty.addAll(kittyCards);

            winningPlayers.forEach(p -> p.increaseCallRank(rankIncrease));

            message = String.format("Round over! %s win and go up by %d.\n" +
                     "%sCollectors[] got (%d + %d*%d) = [BLUE]%d[] points.\n",
                    getTeamNameColorString(winningTeam) + winningTeam.toString() + "[]",
                    rankIncrease,
                    getTeamNameColorString(Team.COLLECTORS),
                    collectedPointsBeforeKitty,
                    kittyPointsMultiplier,
                    kittyPoints,
                    totalCollectedPoints);
            message += "Winners:\n" + winningPlayers.stream()
                    .map(p -> p.getColoredName() + " (" + p.getCallRank().toAbbrevString() + ")")
                    .collect(Collectors.joining("\n"));
        }

        private void lostConnectionToServer() {
            message = "The connection to the host has been lost.";
            game.showStartScreen();
        }
    }

    private static Color getTeamNameColor(Team team) {
        switch(team) {
        case KEEPERS:
            return keepersNameColor;
        case COLLECTORS:
            return collectorsNameColor;
        case NO_TEAM:
            return noTeamNameColor;
        default:
            return Color.WHITE;
        }
    }

    private static String getTeamNameColorString(Team team) {
        return getColorString(getTeamNameColor(team));
    }

    private static String getColorString(Color color) {
        if(color != null) {
            return "[#"
                    + Integer.toHexString((int)(color.r * 255))
                    + Integer.toHexString((int)(color.g * 255))
                    + Integer.toHexString((int)(color.b * 255))
                    + Integer.toHexString((int)(color.a * 255))
                    + "]";
        } else {
            return "";
        }
    }

    public void setTrump(Rank rank, Suit suit) {
        trumpRank = rank;
        trumpSuit = suit;
        if(trumpRank != null && trumpSuit != null) {
            trumpCard = new RenderableShengJiCard(rank, suit, this);
        } else {
            trumpCard = null;
        }
        thisPlayerHand.sort(ShengJiCard::compareTo);
    }

    public final class Actions {
        private Actions() {

        }

        public void sendCall(ClientConnection client) {
            RenderableCardList<RenderableShengJiCard> selectedCards = thisPlayerHand.stream()
                    .filter(RenderableShengJiCard::isSelected)
                    .collect(Collectors.toCollection(RenderableCardList::new));
            if(selectedCards.size() == 0) {
                try {
                    client.sendCode(ClientCode.NO_CALL);
                } catch(IOException e) {
                    errorMessage = "[Yellow]There was an error while trying to contact the server. Did you lose connection?[]";
                }
                return;
            }

            if(!selectedCards.stream().allMatch(c -> c.isSameAs(selectedCards.get(0)))) {
                errorMessage = "[YELLOW]Your call must consist of identical cards[]";
                return;
            }

            int callOrder = selectedCards.size();
            if(!thisPlayer.getPlay().isEmpty() && selectedCards.get(0).getSuit() == thisPlayer.getPlay().get(0).getSuit()) {
                callOrder += thisPlayer.getPlay().size();
            } else {
                thisPlayer.getPlay().clear();
            }

            try {
                client.sendPacket(new ClientPacket(ClientCode.CALL)
                        .put("card", selectedCards.get(0).getCardNum())
                        .put("order", callOrder));
                thisPlayer.getPlay().addAll(selectedCards);
                thisPlayerHand.removeAll(selectedCards);
                selectedCards.forEach(c -> c.setSelected(false));
            } catch(IOException e) {
                errorMessage = "[Yellow]There was an error while trying to contact the server. Did you lose connection?[]";
            }
        }

        public void sendKittyCall(ClientConnection client) {
            try {
                client.sendCode(ClientCode.KITTY_CALL);
            } catch(IOException e) {
                errorMessage = "[Yellow]There was an error while trying to contact the server. Did you lose connection?[]";
            }
        }

        public void sendKitty(ClientConnection client) {
            if(!lastAttemptedKitty.isEmpty()) {
                errorMessage = "[YELLOW]Your kitty is being validated by the server...[]";
                return;
            }

            RenderableCardList<RenderableShengJiCard> selectedCards = thisPlayerHand.stream()
                    .filter(RenderableShengJiCard::isSelected)
                    .collect(Collectors.toCollection(RenderableCardList::new));
            if(selectedCards.size() != kittySize) {
                errorMessage = "[YELLOW]Incorrect number of cards![]";
                return;
            }

            try {
                client.sendPacket(new ClientPacket(ClientCode.KITTY).put("kitty", selectedCards.toCardNumList()));
                lastAttemptedKitty.addAll(selectedCards);
                thisPlayerHand.removeAll(selectedCards);
            } catch(IOException e) {
                errorMessage = "[YELLOW]There was an error while trying to contact the server. Did you lose connection?";
            }
        }

        public void sendFriendCards(ClientConnection client) {
            try {
                client.sendPacket(new ClientPacket(ClientCode.FRIEND_CARDS).put("friendcards", friendCards.toCardNumList()));
            } catch(IOException e) {
                errorMessage = "[YELLOW]There was an error while trying to contact the server. Did you lose connection?[]";
            }
        }

        public void sendPlay(ClientConnection client) {
            // This else/if prevents the player from making two plays in quick succession,
            // which would overwrite thisPlayer.getPlay() and would send the server both plays.
            // The server might accept the first play, but on the client side the first play would have been overwritten
            // by the second play. The second play would be evaluated by the server on the next turn.
            // This would obviously cause the server and client to go out of sync and everything would break.
            if(thisPlayer != turnPlayer) {
                errorMessage = "[YELLOW]It's not your turn.[]";
            } else if(!thisPlayer.getPlay().isEmpty()) {
                errorMessage = "[YELLOW]Your play is being validated by the server...[]";
                return;
            }

            RenderableCardList<RenderableShengJiCard> selectedCards = thisPlayerHand.stream()
                    .filter(RenderableShengJiCard::isSelected)
                    .collect(Collectors.toCollection(RenderableCardList::new));
            if(selectedCards.size() == 0) {
                return;
            }
            try {
                client.sendPacket(new ClientPacket(ClientCode.PLAY).put("play", selectedCards.toCardNumList()));
                thisPlayer.getPlay().addAll(selectedCards);
                thisPlayerHand.removeAll(selectedCards);
                selectedCards.forEach(c -> c.setSelected(false));
            } catch(IOException e) {
                errorMessage = "[YELLOW]There was an error while trying to contact the server. Did you lose connection?[]";
            }
        }
    }
}
