package com.sage.shengji.utils.shengji;

import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.card.InvalidCardException;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;

import java.util.Objects;

public class ShengJiCard extends Card {
    private final ShengJiGameState gameState;
    private int points;
    private int hierarchicalValue;

    public ShengJiCard(Rank rank, Suit suit, ShengJiGameState gameState) throws InvalidCardException {
        super(rank, suit);
        this.gameState = Objects.requireNonNull(gameState);
        cardChangedImpl();
    }

    public ShengJiCard(int cardNum, ShengJiGameState gameState) throws InvalidCardException {
        super(cardNum);
        this.gameState = Objects.requireNonNull(gameState);
        cardChangedImpl();
    }

    public ShengJiCard(ShengJiCard other) throws InvalidCardException {
        super(other);
        this.gameState = other.gameState;
        this.points = other.points;
        this.hierarchicalValue = other.hierarchicalValue;
    }

    public ShengJiCard(ShengJiGameState gameState) {
        super();
        this.gameState = Objects.requireNonNull(gameState);
        cardChangedImpl();
    }

    @Override
    protected void cardChangedImpl() {
        points = determinePointValue();
        hierarchicalValue = determineHierarchicalValue();
    }

    public int getPoints() {
        return points;
    }

    public boolean isPointCard() {
        return getPoints() > 0;
    }

    public int getHierarchicalValue() {
        return hierarchicalValue;
    }

    private int determinePointValue() {
        return (getRank() == Rank.FIVE) ? 5
                : (getRank() == Rank.KING || getRank() == Rank.TEN) ? 10
                : 0;
    }

    private int determineHierarchicalValue() {
        if(getRank() == Rank.BIG_JOKER) {
            return 31;
        } else if(getRank() == Rank.SMALL_JOKER) {
            return 30;
        } else if(isTrumpSuit() && isTrumpRank()) {
            return 29;
        } else if(isTrumpRank()) {
            return 28;
        } else if(isTrumpSuit()) {
            return 13 + getRank().rankNum;
        } else {
            return getRank().rankNum;
        }
    }

    public boolean isTrumpSuit() {
        return getSuit() == Suit.JOKER || (gameState != null && getSuit() == gameState.trumpSuit);
    }

    public boolean isTrumpRank() {
        return getRank() == Rank.SMALL_JOKER || getRank() == Rank.BIG_JOKER || (gameState != null && getRank() == gameState.trumpRank);
    }

    public boolean isTrump() {
        return isTrumpSuit() || isTrumpRank();
    }

    public Suit getEffectiveSuit() {
        return (getSuit() == Suit.JOKER || getRank() == gameState.trumpRank)
                ? gameState.trumpSuit
                : getSuit();
    }

    @Override
    public int compareTo(Card o) {
        if(o instanceof ShengJiCard) {
            ShengJiCard oCasted = (ShengJiCard)o;
            if(this.isTrump() && oCasted.isTrump()) {
                int hierarchyCompare = Integer.compare(this.getHierarchicalValue(), oCasted.getHierarchicalValue());
                if(hierarchyCompare == 0) {
                    return Integer.compare(this.getSuit().suitNum, oCasted.getSuit().suitNum);
                } else {
                    return hierarchyCompare;
                }
            } else if(this.isTrump()) {
                return -1;
            } else if(oCasted.isTrump()) {
                return 1;
            }
        }

        if(getSuit() == o.getSuit()) {
            return Integer.compare(getRank().rankNum, o.getRank().rankNum);
        } else {
            return Integer.compare(getSuit().suitNum, o.getSuit().suitNum);
        }
    }
}
