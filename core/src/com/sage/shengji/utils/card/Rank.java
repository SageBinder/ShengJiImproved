package com.sage.shengji.utils.card;

public enum Rank {
    TWO("two", 2),
    THREE("three", 3),
    FOUR("four", 4),
    FIVE("five", 5),
    SIX("six", 6),
    SEVEN("seven", 7),
    EIGHT("eight", 8),
    NINE("nine", 9),
    TEN("ten", 10),
    JACK("jack", 11),
    QUEEN("queen", 12),
    KING("king", 13),
    ACE("ace", 14),
    SMALL_JOKER("small_joker", 15),
    BIG_JOKER("big_joker", 16);

    public final String stringName;
    public final int rankNum;

    Rank(String name, int rankNum) {
        this.stringName = name;
        this.rankNum = rankNum;
    }

    public static Rank fromCardNum(int cardNum) throws InvalidCardException {
        return Card.getRankFromCardNum(cardNum);
    }

    @Override
    public String toString() {
        return (rankNum <= 10) ? Integer.toString(rankNum) : stringName;
    }

    public String toAbbrevString() {
        return (rankNum <= 10) ? Integer.toString(rankNum)
                : (this == SMALL_JOKER) ? "SJ"
                : (this == BIG_JOKER) ? "BJ"
                : Character.toUpperCase(stringName.charAt(0)) + "";
    }
}