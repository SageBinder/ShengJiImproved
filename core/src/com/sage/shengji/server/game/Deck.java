package com.sage.shengji.server.game;

import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.shengji.ShengJiGameState;

import java.util.Collections;
import java.util.Random;

public class Deck extends CardList<ShengJiCard> {
    private static Random r = new Random();

    public Deck(int numFullDecks, boolean jokers, ShengJiGameState gameState) {
        for(int i = 0; i < numFullDecks; i++) {
            for(int j = 0; j < 52; j++) {
                add(new ShengJiCard(j, gameState));
            }
            if(jokers) {
                add(new ShengJiCard(52, gameState));
                add(new ShengJiCard(53, gameState));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(this);
    }

    // Does not deal randomly. Call shuffle() before dealing if random dealing is desired
    public void dealToPlayers(PlayerList players) {
        for(int i = 0; i < size(); i++) {
            players.get(i % players.size()).hand.add(this.get(i));
        }
        clear();
    }

    public ShengJiCard pullRandom() {
        ShengJiCard pulled = get(r.nextInt(size()));
        remove(pulled);
        return pulled;
    }
}
