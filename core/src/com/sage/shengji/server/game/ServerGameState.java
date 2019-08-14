package com.sage.shengji.server.game;

import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.shengji.ShengJiGameState;

import java.util.Collections;

public class ServerGameState extends ShengJiGameState {
    public static final int MAX_PLAYERS = 8;

    private boolean roundRunning = false;

    final PlayerList players = new PlayerList();

    // Fields inherited from ShengJiGameState:
    // Rank trumpRank;
    // Suit trumpSuit;

    int numDecks = 0;
    int numPointsNeeded = 0;
    int numFriendCards = 0;

    final CardList<ShengJiCard> kitty = new CardList<>();
    final CardList<ShengJiCard> friendCards = new CardList<>();
    final CardList<ShengJiCard> collectedPointCards = new CardList<>();

    Player caller = null;
    Player turnPlayer = null;
    Player leadingPlayer = null;
    Player startingPlayer = null;
    final CardList<ShengJiCard> pointCardsInTrick = new CardList<>();
    Play basePlay = null;

    int tricksPlayed = 0;
    int roundsPlayed = 0;

    public synchronized void addPlayer(Player p) throws RoundIsRunningException {
        if(roundRunning) {
            throw new RoundIsRunningException();
        }

        players.add(p);
    }

    public synchronized boolean removePlayer(Player p) throws RoundIsRunningException {
        if(roundRunning) {
            throw new RoundIsRunningException();
        }

        return players.remove(p);
    }

    public synchronized PlayerList getPlayers() {
        return new PlayerList(players);
    }

    public synchronized void shufflePlayers() throws RoundIsRunningException {
        if(roundRunning) {
            throw new RoundIsRunningException();
        }

        Collections.shuffle(players);
    }

    public synchronized boolean removeDisconnectedPlayers() {
        if(roundRunning) {
            return false;
        }

        boolean anyRemoved = players.removeIf(player -> !player.socketIsConnected());
        if(anyRemoved) {
            players.squashPlayerNums();
        }
        return anyRemoved;
    }

    public synchronized void squashPlayerNums() {
        players.squashPlayerNums();
    }

    synchronized void setRoundRunning(boolean roundRunning) {
        this.roundRunning = roundRunning;
    }

    public synchronized boolean isRoundRunning() {
        return roundRunning;
    }

    boolean isLegalCall(Player p, ShengJiCard call, int order) {
        return p.hand.stream().filter(c -> c.getCardNum() == call.getCardNum()).count() <= order
                && call.getRank() == p.getCallRank();
    }

    boolean isLegalKitty(Player p, CardList<ShengJiCard> newKitty) {
        return newKitty.stream()
                .distinct()
                .allMatch(c -> Collections.frequency(p.hand.toCardNumList(), c.getCardNum()) >= Collections.frequency(newKitty.toCardNumList(), c.getCardNum()));
    }

    boolean areLegalFriendCards(CardList<ShengJiCard> friendCards) {
        return friendCards.size() == numFriendCards;
    }
}
