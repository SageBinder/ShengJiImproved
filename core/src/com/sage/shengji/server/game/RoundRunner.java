package com.sage.shengji.server.game;

import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.client.network.ClientPacket;
import com.sage.shengji.server.network.MultiplePlayersDisconnectedException;
import com.sage.shengji.server.network.PlayerDisconnectedException;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.server.network.ServerPacket;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.card.InvalidCardException;
import com.sage.shengji.utils.card.Suit;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.shengji.Team;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.sage.shengji.server.network.ServerCode.*;

public class RoundRunner {
    static void throwExceptionIfPlayerDisconnectDetected(ServerGameState gameState) {
        var disconnectedPlayers = gameState.players.stream()
                .filter(p -> !p.socketIsConnected()).collect(Collectors
                        .toCollection(PlayerList::new));
        if(!disconnectedPlayers.isEmpty()) {
            gameState.players.forEach(Player::clearPacketQueue);
            throw new MultiplePlayersDisconnectedException(disconnectedPlayers);
        }
    }

    // This method will ALWAYS call gameState.setRoundRunning(false); before exiting
    public static void playRound(ServerGameState gameState) throws RoundStartFailedException, FatalRoundException {
        try {
            gameState.setRoundRunning(true);

            if(gameState.players.size() < 2
                    || gameState.players.stream().anyMatch(p -> Objects.isNull(p) || !p.socketIsConnected())) {
                throw new RoundStartFailedException();
            }

            gameState.players.forEach(p -> p.setOnDisconnect(() -> {
                gameState.players.forEach(Player::interruptPacketWaiting);
            }));

            gameState.players.sendPlayersToAll();
            gameState.players.sendPacketToAll(new ServerPacket(ServerCode.ROUND_START)
                    .put("playerorder", gameState.players.stream().mapToInt(Player::getPlayerNum).toArray()));

            gameState.roundsPlayed++;
            do {
                resetGameStateForNewRound(gameState);
                sendHands(gameState);
                establishCaller(gameState);
                // establishCaller() should set gameState.caller to some player.
                // If it leaves gameState.caller as null, it means no one wanted to call.
                // In that case, a redeal is necessary.
            } while(gameState.caller == null);

            gameState.caller.setTeam(Team.KEEPERS);
            if(gameState.numFriendCards == 0) {
                gameState.players.stream()
                        .filter(p -> p.getTeam() == Team.NO_TEAM)
                        .forEach(p -> p.setTeam(Team.COLLECTORS));
            }

            gameState.players.sendCodeToAll(WAITING_ON_CALLER);
            sendKittyToCaller(gameState);
            getKittyFromCaller(gameState);
            getFriendCardsFromCaller(gameState);
            sendFriendCardsToPlayers(gameState);

            gameState.turnPlayer = gameState.caller;
            do {
                TrickRunner.playTrick(gameState);
            } while(gameState.players.stream().allMatch(p -> p.hand.size() > 0));

            gameState.players.sendPacketToAll(updateRanksAndGetRoundEndPacket(gameState));
        } finally {
            gameState.setRoundRunning(false);
            gameState.players.forEach(Player::clearPacketQueue);
            gameState.players.forEach(Player::resetOnDisconnect);
        }
    }

    private static void resetGameStateForNewRound(ServerGameState gameState) {
        gameState.numDecks = Math.max(gameState.players.size() / 2, 1);
        gameState.numPointsNeeded = 40 * gameState.numDecks;
        gameState.numFriendCards = Math.max(((gameState.players.size() / 2) - 1), 0);

        gameState.friendCards.clear();
        gameState.collectedPointCards.clear();

        gameState.tricksPlayed = 0;
        gameState.turnPlayer = null;
        gameState.leadingPlayer = null;
        gameState.startingPlayer = null;
        gameState.caller = null;
        gameState.basePlay = null;
        gameState.pointCardsInTrick.clear();
        gameState.players.forEach(Player::resetForNewRound);

        Deck deck = new Deck(gameState.numDecks, true, gameState);
        deck.shuffle();
        gameState.kitty.clear();
        gameState.kitty.addAll(extractKittyFromDeck(deck, gameState));
        deck.dealToPlayers(gameState.players);
    }

    private static CardList<ShengJiCard> extractKittyFromDeck(Deck deck, ServerGameState gameState) {
        int kittySize = deck.size() % gameState.players.size();
        if(kittySize == 0) {
            kittySize = gameState.players.size();
        }

        CardList<ShengJiCard> kitty = new CardList<>();
        while(true) {
            for(int i = 0; i < kittySize; i++) {
                kitty.add(deck.pullRandom());
            }

            // If a kitty consists entirely of jokers, it needs to be redrawn
            if(kitty.stream().allMatch(Card::isJoker)) {
                deck.addAll(kitty);
                kitty.clear();
            } else {
                break;
            }
        }
        return kitty;
    }

    private static void sendHands(ServerGameState gameState) {
        gameState.players.forEach(p ->
                p.sendPacket(new ServerPacket(WAIT_FOR_HAND)
                        .put("hand", p.hand.toCardNumList())
                        .put("numpointsneeded", gameState.numPointsNeeded)));
    }

    private static void sendKittyToCaller(ServerGameState gameState) {
        Player caller = gameState.caller;
        if(caller == null) {
            throw new FatalRoundException("THIS IS BAD: in sendKittyToCaller(), gameState.caller == null");
        }

        caller.sendPacket(new ServerPacket(WAIT_FOR_KITTY).put("kitty", gameState.kitty.toCardNumList()));
        caller.hand.addAll(gameState.kitty);
    }

    private static void getKittyFromCaller(ServerGameState gameState) {
        Player caller = gameState.caller;
        if(caller == null) {
            throw new FatalRoundException("THIS IS BAD: in getKittyFromCaller(), gameState.caller == null");
        }

        // This could probably be refactored
        caller.sendCode(SEND_KITTY);
        while(true) {
            try {
                ClientPacket kittyPacket = caller.waitForPacket();
                if(kittyPacket.networkCode != ClientCode.KITTY) {
                    continue;
                }
                List<Integer> newKittyCardNums = (List<Integer>)Objects.requireNonNull(kittyPacket.get("kitty"));
                CardList<ShengJiCard> newKitty =
                        CardList.fromCardNumList(newKittyCardNums, cardNum -> new ShengJiCard(cardNum, gameState));
                if(gameState.isLegalKitty(caller, newKitty)) {
                    gameState.kitty.clear();
                    gameState.kitty.addAll(newKitty);
                    caller.hand.removeAllByValue(newKitty);
                    caller.sendCode(SUCCESSFUL_KITTY);
                    return;
                } else {
                    caller.sendCode(INVALID_KITTY);
                }
            } catch(InterruptedException e) {
                throwExceptionIfPlayerDisconnectDetected(gameState);
            } catch(NullPointerException | ClassCastException | InvalidCardException e) {
                caller.sendCode(INVALID_KITTY);
            }
        }
    }

    private static void getFriendCardsFromCaller(ServerGameState gameState) {
        Player caller = gameState.caller;
        if(caller == null) {
            throw new FatalRoundException("THIS IS BAD: in getFriendCardsFromCaller(), gameState.caller == null");
        }

        if(gameState.numFriendCards == 0) {
            return;
        }

        caller.sendPacket(new ServerPacket(SEND_FRIEND_CARDS).put("numfriendcards", gameState.numFriendCards));
        while(true) {
            try {
                ClientPacket friendCardsPacket = caller.waitForPacket();
                if(friendCardsPacket.networkCode != ClientCode.FRIEND_CARDS) {
                    continue;
                }
                List<Integer> friendCardNums = (List<Integer>)friendCardsPacket.get("friendcards");
                CardList<ShengJiCard> friendCards =
                        CardList.fromCardNumList(friendCardNums, cardNum -> new ShengJiCard(cardNum, gameState));
                if(gameState.areLegalFriendCards(friendCards)) {
                    gameState.friendCards.clear();
                    gameState.friendCards.addAll(friendCards);
                    caller.sendCode(SUCCESSFUL_FRIEND_CARDS);
                    return;
                } else {
                    caller.sendCode(INVALID_FRIEND_CARDS);
                }
            } catch(InterruptedException e) {
                throwExceptionIfPlayerDisconnectDetected(gameState);
            } catch(ClassCastException | InvalidCardException e) {
                caller.sendCode(INVALID_FRIEND_CARDS);
            }
        }
    }

    private static void sendFriendCardsToPlayers(ServerGameState gameState) {
        gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_FRIEND_CARDS)
                .put("cardnums", gameState.friendCards.toCardNumList()));
    }

    private static ServerPacket updateRanksAndGetRoundEndPacket(ServerGameState gameState) {
        if(gameState.leadingPlayer == null || gameState.leadingPlayer.play == null) {
            throw new FatalRoundException("THIS IS BAD: updateRanksAndGetRoundEndPacket(), gameState.leadingPlayer == null || gameState.leadingPlayer.play == null");
        }

        int pointsInKitty = gameState.kitty.stream().mapToInt(ShengJiCard::getPoints).sum();
        int collectedPointsBeforeKitty = gameState.collectedPointCards.stream().mapToInt(ShengJiCard::getPoints).sum();
        int kittyPointsMultiplier = gameState.leadingPlayer.play.size() + 1;
        int totalCollectedPoints = (pointsInKitty * kittyPointsMultiplier) + collectedPointsBeforeKitty;
        int rankIncrease = (Math.abs(totalCollectedPoints - gameState.numPointsNeeded) / (gameState.numPointsNeeded / 2)) + 1;

        Team winningTeam = (totalCollectedPoints >= gameState.numPointsNeeded) ? Team.COLLECTORS : Team.KEEPERS;
        var winningPlayers = gameState.players.stream()
                .filter(p -> p.getTeam() == winningTeam)
                .collect(Collectors.toCollection(PlayerList::new));
        winningPlayers.forEach(p -> p.increaseCallRank(rankIncrease));

        return new ServerPacket(ROUND_END)
                .put("winningteam", winningTeam)
                .put("winningplayers", winningPlayers.stream()
                        .map(Player::getPlayerNum)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .put("kittycards", gameState.kitty.toCardNumList())
                .put("kittypointsmultiplier", kittyPointsMultiplier)
                .put("collectedpointsbeforekitty", collectedPointsBeforeKitty)
                .put("totalcollectedpoints", totalCollectedPoints)
                .put("rankincrease", rankIncrease);
    }

    // The code for establishCaller and establishKittyCaller have essentially the exact same structure, but with the
    // minor differences, I don't think it's worth it to try to reduce code duplication here
    private static void establishCaller(ServerGameState gameState) {
        // GAAAHHHHHHH THIS IS TERRIBLE REEEEEEEEEEEEEEEEEEEEEEE
        // Basically, what playRound() sets as onDisconnectAction for each player breaks calling code.
        // It's supposed to interrupt the packet waiting of every player if anybody disconnects.
        // In this code, however, that causes packet waiting to be interrupted before playerDisconnected is set,
        // which means the catch block for InterruptedException doesn't think a player is disconnected,
        // and it goes back to waiting.
        // At the end of this method, every player's onDisconnectAction is once again set to interrupt the
        // packet waiting of all players.
        gameState.players.forEach(Player::resetOnDisconnect);

        final Object threadExitNotifierObject = new Object();
        AtomicBoolean playerDisconnected = new AtomicBoolean(false);
        AtomicInteger numFinishedThreads = new AtomicInteger(0);
        int numPlayers = gameState.players.size();

        final Object callLock = new Object();
        AtomicReference<Player> leadingPlayer = new AtomicReference<>(null);
        AtomicReference<ShengJiCard> leadingCallCard = new AtomicReference<>(null);
        AtomicInteger leadingCallOrder = new AtomicInteger(0);

        // Assigning threads:
        Thread[] threads = new Thread[numPlayers];
        for(int i = 0; i < numPlayers; i++) {
            Player p = gameState.players.get(i);
            threads[i] = new Thread(() -> {
                while(true) {
                    try {
                        ClientPacket callPacket = p.waitForPacket();
                        if(callPacket.networkCode == ClientCode.NO_CALL) {
                            if(leadingPlayer.get() != p) {
                                // A player cannot take back their call if their call is currently leading
                                p.sendCode(NO_CALL);
                                synchronized(threadExitNotifierObject) {
                                    gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_NO_CALL_PLAYER)
                                            .put("player", p.getPlayerNum()));
                                    numFinishedThreads.incrementAndGet();
                                    threadExitNotifierObject.notify();
                                }
                                return;
                            } else {
                                continue;
                            }
                        } else if(callPacket.networkCode != ClientCode.CALL) {
                            continue;
                        }

                        ShengJiCard callCard = new ShengJiCard(Objects.requireNonNull((Integer)callPacket.get("card")), gameState);
                        int callOrder = (Integer)callPacket.get("order");
                        synchronized(callLock) {
                            if(!gameState.isLegalCall(p, callCard, callOrder)) {
                                p.sendCode(INVALID_CALL);
                            } else if(callOrder <= leadingCallOrder.get()) {
                                p.sendCode(UNSUCCESSFUL_CALL);
                            } else {
                                leadingCallCard.set(callCard);
                                leadingCallOrder.set(callOrder);
                                leadingPlayer.set(p);
                                p.sendPacket(new ServerPacket(SUCCESSFUL_CALL)
                                        .put("cardnum", callCard.getCardNum())
                                        .put("order", callOrder));

                                gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_NEW_LEADING_CALL)
                                        .put("playernum", p.getPlayerNum())
                                        .put("cardnum", leadingCallCard.get().getCardNum())
                                        .put("order", leadingCallOrder.get()));
                                synchronized(threadExitNotifierObject) {
                                    // We must notify the main thread so that it checks to see if p is the only player
                                    // who hasn't send a NO_CALL packet.
                                    threadExitNotifierObject.notify();
                                }
                            }
                        }
                    } catch(InterruptedException e) {
                        synchronized(threadExitNotifierObject) {
                            if(playerDisconnected.get()
                                    || (numFinishedThreads.get() == numPlayers - 1 && leadingPlayer.get() == p)) {
                                numFinishedThreads.incrementAndGet();
                                threadExitNotifierObject.notify();
                            }
                        }
                        return;
                    } catch(ClassCastException | NullPointerException e) {
                        p.sendCode(INVALID_CALL);
                    } catch(PlayerDisconnectedException e) {
                        synchronized(threadExitNotifierObject) {
                            playerDisconnected.set(true);
                            numFinishedThreads.incrementAndGet();
                            threadExitNotifierObject.notify();
                        }
                        return;
                    }
                }
            });
        }

        // Starting threads:
        gameState.players.sendPacketToAll(new ServerPacket(MAKE_CALL));
        for(Thread t : threads) {
            t.start();
        }

        // Waiting for threads to finish (and interrupting them when appropriate):
        boolean alreadyInterrupted = false;
        while(true) {
            synchronized(threadExitNotifierObject) {
                if(playerDisconnected.get() && !alreadyInterrupted) {
                    gameState.players.forEach(Player::interruptPacketWaiting);
                    alreadyInterrupted = true;
                } else if(numFinishedThreads.get() == numPlayers - 1 && leadingPlayer.get() != null) {
                    leadingPlayer.get().interruptPacketWaiting();
                }

                if(numFinishedThreads.get() == numPlayers) {
                    break;
                }

                try {
                    threadExitNotifierObject.wait();
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Clear any interrupts left in the packet queues
        gameState.players.forEach(Player::clearPacketQueue);

        // Bail to lobby if a player disconnected
        if(playerDisconnected.get()) {
            throw new MultiplePlayersDisconnectedException((PlayerList)gameState.players.stream()
                    .filter(p -> !p.socketIsConnected())
                    .collect(Collectors.toCollection(PlayerList::new)));
        }

        // Send call winner to all players (or, if no one called, draw a card from the kitty and start the calling process again)
        if(leadingPlayer.get() != null) {
            gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_CALL_WINNER)
                    .put("playernum", leadingPlayer.get().getPlayerNum())
                    .put("callcardnum", leadingCallCard.get().getCardNum())
                    .put("callorder", leadingCallOrder.get()));
            gameState.caller = leadingPlayer.get();
            gameState.trumpRank = leadingCallCard.get().getRank();
            gameState.trumpSuit = leadingCallCard.get().getSuit();
        } else {
            gameState.players.sendPacketToAll(new ServerPacket(NO_ONE_CALLED));
            establishKittyCaller(gameState, 0);
        }

        gameState.players.forEach(p -> p.setOnDisconnect(() -> {
            gameState.players.forEach(Player::interruptPacketWaiting);
        }));
    }

    // THIS METHOD IS RECURSIVE, DEAR GOD I'M SO SORRY
    // Each iteration of the method pulls a new card from the kitty and sends it to all players.
    // If no one chooses to call on that card, a new card is pulled.
    // If all cards in the kitty are pulled, it sets gameState.caller to null which will trigger a redeal.
    private static void establishKittyCaller(ServerGameState gameState, final int kittyPullIdx) {
        if(kittyPullIdx >= gameState.kitty.size()) {
            gameState.caller = null;
            gameState.players.sendCodeToAll(KITTY_EXHAUSTED_REDEAL);
            return;
        }

        ShengJiCard kittyCard = gameState.kitty.get(kittyPullIdx);
        gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_KITTY_CARD).put("cardnum", kittyCard.getCardNum()));

        final Object threadExitNotifierObject = new Object();
        AtomicBoolean playerDisconnected = new AtomicBoolean(false);
        AtomicInteger numFinishedThreads = new AtomicInteger(0);
        int numPlayers = gameState.players.size();

        final Object callLock = new Object();
        AtomicReference<Player> winningPlayer = new AtomicReference<>(null);

        // Assigning threads:
        Thread[] threads = new Thread[numPlayers];
        for(int i = 0; i < numPlayers; i++) {
            Player p = gameState.players.get(i);
            threads[i] = new Thread(() -> {
                while(true) {
                    try {
                        ClientPacket callPacket = p.waitForPacket();
                        if(callPacket.networkCode == ClientCode.NO_KITTY_CALL) {
                            p.sendCode(NO_KITTY_CALL);
                            synchronized(threadExitNotifierObject) {
                                gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_NO_CALL_PLAYER)
                                        .put("player", p.getPlayerNum()));
                                numFinishedThreads.incrementAndGet();
                                threadExitNotifierObject.notify();
                            }
                            return;
                        } else if(callPacket.networkCode != ClientCode.KITTY_CALL) {
                            continue;
                        }

                        synchronized(callLock) {
                            if(winningPlayer.get() != null) {
                                p.sendPacket(new ServerPacket(UNSUCCESSFUL_KITTY_CALL)
                                        .put("message", "Someone already called (are you lagging?)"));
                            } else if(gameState.kitty.stream().limit(kittyPullIdx + 1).allMatch(Card::isJoker)) {
                                p.sendPacket(new ServerPacket(UNSUCCESSFUL_KITTY_CALL)
                                        .put("message", "No valid card has been pulled from kitty"));
                            } else {
                                p.sendCode(SUCCESSFUL_KITTY_CALL);
                                winningPlayer.set(p);

                                // The first person to make a call the kitty card wins, so we can return here
                                synchronized(threadExitNotifierObject) {
                                    numFinishedThreads.incrementAndGet();
                                    threadExitNotifierObject.notify();
                                }
                                return;
                            }
                        }
                    } catch(InterruptedException e) {
                        if(playerDisconnected.get() || winningPlayer.get() != null) {
                            synchronized(threadExitNotifierObject) {
                                numFinishedThreads.incrementAndGet();
                                threadExitNotifierObject.notify();
                            }
                            return;
                        }
                    } catch(ClassCastException | NullPointerException e) {
                        p.sendCode(INVALID_KITTY_CALL);
                    } catch(PlayerDisconnectedException e) {
                        synchronized(threadExitNotifierObject) {
                            playerDisconnected.set(true);
                            numFinishedThreads.incrementAndGet();
                            threadExitNotifierObject.notify();
                        }
                        return;
                    }
                }
            });
        }

        // Starting threads:
        gameState.players.sendCodeToAll(MAKE_KITTY_CALL);
        for(Thread t : threads) {
            t.start();
        }

        // Waiting for threads to finish (and interrupting them when appropriate):
        boolean alreadyInterrupted = false;
        while(true) {
            synchronized(threadExitNotifierObject) {
                if((playerDisconnected.get() || winningPlayer.get() != null) && !alreadyInterrupted) {
                    gameState.players.forEach(Player::interruptPacketWaiting);
                    alreadyInterrupted = true;
                }
                if(numFinishedThreads.get() == numPlayers) {
                    break;
                }

                try {
                    threadExitNotifierObject.wait();
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Clear any interrupts left in the packet queues
        gameState.players.forEach(Player::clearPacketQueue);

        // Bail to lobby if a player disconnected
        if(playerDisconnected.get()) {
            throw new MultiplePlayersDisconnectedException((PlayerList)gameState.players.stream()
                    .filter(p -> !p.socketIsConnected())
                    .collect(Collectors.toCollection(PlayerList::new)));
        }

        if(winningPlayer.get() != null) {
            var effectiveKittyCard = gameState.kitty.stream()
                    .limit(kittyPullIdx + 1)
                    .filter(c -> !c.isJoker())
                    .reduce((a, b) -> b);
            gameState.trumpRank = winningPlayer.get().getCallRank();
            gameState.trumpSuit = effectiveKittyCard.isPresent() ? effectiveKittyCard.get().getSuit() : Suit.JOKER;
            gameState.players.sendPacketToAll(new ServerPacket(WAIT_FOR_KITTY_CALL_WINNER)
                    .put("playernum", winningPlayer.get().getPlayerNum())
                    .put("trumprank", gameState.trumpRank)
                    .put("trumpsuit", gameState.trumpSuit));
            gameState.caller = winningPlayer.get();
        } else {
            establishKittyCaller(gameState, kittyPullIdx + 1);
        }
    }
}
