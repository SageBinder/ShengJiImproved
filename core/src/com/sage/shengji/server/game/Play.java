package com.sage.shengji.server.game;

import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.card.Suit;
import com.sage.shengji.utils.shengji.ShengJiCard;

import java.util.*;
import java.util.stream.Collectors;

class Play extends CardList<ShengJiCard> {
    private ServerGameState gameState;

    private final Player player;
    private boolean isBasePlay;
    private PlayLegalityResult playLegalityResult;
    private int playHierarchicalValue;

    private ArrayList<CardList<ShengJiCard>> groupedPlay;
    private int[] playStructure;

    Play(CardList<ShengJiCard> cards, ServerGameState gameState) {
        super(cards);
        this.isBasePlay = gameState.basePlay == null;
        this.player = gameState.turnPlayer;
        this.gameState = gameState;
        init();
    }

    @Override
    public boolean add(ShengJiCard shengJiCard) {
        boolean ret = super.add(shengJiCard);
        init();
        return ret;
    }

    @Override
    public void add(int index, ShengJiCard element) {
        super.add(index, element);
        init();
    }

    @Override
    public boolean addAll(Collection<? extends ShengJiCard> c) {
        boolean ret = super.addAll(c);
        init();
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends ShengJiCard> c) {
        boolean ret = super.addAll(index, c);
        init();
        return ret;
    }

    private void init() {
        this.groupedPlay = groupCardList(this);
        this.playStructure = determinePlayStructure();
        this.playHierarchicalValue = determinePlayHierarchicalValue();
        this.playLegalityResult = determineIsLegal();
    }

    private ArrayList<CardList<ShengJiCard>> groupCardList(CardList<ShengJiCard> list) {
        Map<Integer, CardList<ShengJiCard>> cardGroupsMap = new HashMap<>();
        for(ShengJiCard c : list) {
            int cardNum = c.getCardNum();
            if(cardGroupsMap.get(cardNum) == null) {
                cardGroupsMap.put(cardNum, new CardList<>());
            }
            cardGroupsMap.get(cardNum).add(c);
        }

        ArrayList<CardList<ShengJiCard>> cardGroups = new ArrayList<>(cardGroupsMap.values());
        cardGroups.sort(Comparator.comparingInt(c -> c.get(0).getHierarchicalValue()));
        return cardGroups;
    }
    
    private int[] determinePlayStructure() {
        int[] playStructure = new int[groupedPlay.size()];
        for(int i = 0; i < groupedPlay.size(); i++) {
            playStructure[i] = groupedPlay.get(i).size();
        }
        return playStructure;
    }

    private PlayLegalityResult determineIsLegal() {
        if(size() == 0) {
            return new PlayLegalityResult(false, "Play contained 0 cards");
        } else if(!isBasePlay && size() != gameState.basePlay.size()) {
            return new PlayLegalityResult(false,
                    "Play contained " + size() + " card" + (size() != 1 ? "s" : "") + ", " + "should contain " +
                            gameState.basePlay.size() + " card" + (gameState.basePlay.size() != 1 ? "s" : ""));
        }

        // If the player's hand doesn't contain all the cards in the play, then obviously it's not legal
        if(!player.hand.toCardNumList().containsAll(this.toCardNumList())) {
            return new PlayLegalityResult(false, "Hand did not contain all cards in play (THIS IS BAD)");
        }

        final Suit basePlayEffectiveSuit = (gameState.basePlay != null) ? gameState.basePlay.get(0).getEffectiveSuit() : null;

        // We can assume the play is a trash play if it's not trump and a different suit than the base play
        if(isTrashPlay()) {
            // You cannot start a trick with a trash play
            if(isBasePlay) {
                return new PlayLegalityResult(false, "You cannot start a trick with a trash play");
            }

            int[] basePlayStructure = Arrays.copyOf(gameState.basePlay.playStructure, gameState.basePlay.playStructure.length);
            Arrays.sort(basePlayStructure);
            ArrayList<CardList<ShengJiCard>> groupedHandInBaseSuit =
                    groupCardList(player.hand.stream()
                            .filter(c -> c.getEffectiveSuit() == gameState.basePlay.get(0).getEffectiveSuit())
                            .collect(Collectors.toCollection(CardList::new)));
            ArrayList<CardList<ShengJiCard>> groupedPlayCopy = new ArrayList<>(groupedPlay);

            // Iterate backwards because basePlayStructure is sorted in ascending order but needs to be checked in
            // descending order
            for(int playStructureIdx = basePlayStructure.length - 1; playStructureIdx >= 0 ; playStructureIdx--) {

                // This inner loop is because even if hand can't satisfy basePlayStructure[i], the player still needs
                // to satisfy it as much as possible (i.e if base play is a triple, the player must play a double when
                // the hand can't satisfy a triple)
                for(int tuple = basePlayStructure[playStructureIdx]; tuple >= 1; tuple--) {
                    final int _tuple = tuple; // rrrrrrrrreeeeeeeeeeeeeeeeeeeeeee variables used in lambda must be final

                    // If the hand has cards which adhere to basePlayStructure[i], but this play doesn't, then it can't
                    // be a legal play
                    if(groupedHandInBaseSuit.stream().anyMatch(handGroup -> handGroup.size() == _tuple)) {
                        if(groupedPlayCopy.stream().noneMatch(playGroup -> playGroup.size() == _tuple
                                && playGroup.get(0).getEffectiveSuit() == basePlayEffectiveSuit)) {
                            return new PlayLegalityResult(false, "You did not match the base play");
                        }

                        // Remove the hand group that adheres to basePlayStructure because it can't be used to adhere to
                        // another tuple in basePlayStructure
                        for(var group : groupedHandInBaseSuit) {
                            if(group.size() == tuple) {
                                groupedHandInBaseSuit.remove(group);
                                break;
                            }
                        }

                        // Remove the group that satisfied basePlayStructure[playStructureIdx] so that we don't count it twice
                        for(var group : groupedPlayCopy) {
                            if(group.size() == tuple && group.get(0).getEffectiveSuit() == basePlayEffectiveSuit) {
                                groupedPlayCopy.remove(group);
                                break;
                            }
                        }

                        // If the largest matching group didn't match all of basePlayStructure[playStructureIdx], then
                        // basePlayStructure[playStructureIdx] still needs to be matched by the next largest group
                        if(tuple < basePlayStructure[playStructureIdx]) {
                            basePlayStructure[playStructureIdx] -= tuple;

                            // Prevent tuple from decreasing because if we have just matched a single card, we still
                            // need to confirm that there are no other single cards which could match before we move on.
                            // The Math.min is because there's no need to try to match a tuple greater than basePlayStructure[playStructureIdx].
                            tuple = Math.min(tuple, basePlayStructure[playStructureIdx]) + 1;
                        } else {
                            break;
                        }
                    }
                }
            }
            // The previous loop would have returned false if the hand contained a possible match which the play
            // didn't contain, so we know that the play contains all possible matches to the base play and is legal.
            return new PlayLegalityResult(true, "");
        } else {
            // It it's not a trash play and it's the base play, it's legal
            if(isBasePlay) {
                return new PlayLegalityResult(true, "");
            }

            // If this play is a non-trash trump play, but the player still has the base play suit in their hand,
            // it's not a legal play.
            if(get(0).isTrump() && basePlayEffectiveSuit != gameState.trumpSuit) {
                boolean legal = player.hand.stream().noneMatch(c -> c.getEffectiveSuit() == basePlayEffectiveSuit);
                String message = "";
                if(!legal) {
                    message = "You still have some base-play suits in your hand, so you cannot play trump";
                }
                return new PlayLegalityResult(legal, message);
            } else {
                // If this play was non-trump and base play was trump, it would be considered a trash play
                // Because of that, either both plays are trump, or neither are trump. In both cases, this play is legal
                return new PlayLegalityResult(true, "");
            }
        }
    }

    private int determinePlayHierarchicalValue() {
        if(size() == 0) {
            return -1;
        }

        // "base-compliant" means it is not trash due to the base play structure or suit.

        // Each CardList<ShengJiCard> in groupedPlay contains identical cards,
        // and groupedPlay is sorted by ascending hierarchicalValue

        // These checks make sure this play complies with the base play. If this play IS the base play, then obviously
        // it complies with itself.
        if(gameState.basePlay != null && !isBasePlay) {

            // playStructure of card groups (when groups are both sorted in ascending order) should be equivalent
            if(!Arrays.equals(playStructure, gameState.basePlay.playStructure)) {
                return 0;
            }

            // The number of cards in this play should obviously be the same as the number of cards in the base play
            if(this.size() != gameState.basePlay.size()) {
                return 0;
            }

            // If the first group isn't in a base-compliant suit, it's a trash play.
            // If first group is a trump suit, it still may be a base-compliant play.
            // All other groups will be checked against the first group to ensure that the entire play is the same suit
            if(!groupedPlay.get(0).get(0).isTrump()
                    && groupedPlay.get(0).get(0).getEffectiveSuit() != gameState.basePlay.get(0).getEffectiveSuit()) {
                return 0;
            }

            // At this point all we know is that the FIRST GROUP suit is base-compliant, that the number of cards
            // in this play is equal to the number of cards in the base play, and that the play structures are identical.
            // The hierarchy values may still be trash (meaning, they may not be consecutive), and the group may
            // contain cards of different suits which would make it base-noncompliant and thus a trash play.
        }

        // If there's only one group, it contains identical cards and is a non-trash play
        // (suit of first group is already confirmed to be base-compliant)
        if(groupedPlay.size() == 1) {
            return get(0).getHierarchicalValue();
        }

        // IMPORTANT: At this point we know only the first card group complies with the base play's suit. We need to
        // make sure all the other groups in this play match the first group's suit.

        // Checks if the hierarchical values are consecutive, and if the suit of every group is base-compliant.
        int lastGroupHierarchicalValue = groupedPlay.get(0).get(0).getHierarchicalValue() - 1;
        for(CardList<ShengJiCard> group : groupedPlay) {

            // This check is theoretically only needed if this play is the base play, as the base play shouldn't have any
            // group with size() == 1, and we already know that this play follows the base play's structure.
            // If length of a group is 1, it's a trash play, but ONLY because we know there's more than one group
            if(group.size() == 1) {
                return 0;
            }

            // If the hierarchical value isn't consecutive, it's a trash play.
            if(group.get(0).getHierarchicalValue() != lastGroupHierarchicalValue + 1) {
                return 0;
            }

            // If any group has a different suit than the first group, it's a trash play
            if(group.get(0).getEffectiveSuit() != groupedPlay.get(0).get(0).getEffectiveSuit()) {
                return 0;
            }

            lastGroupHierarchicalValue = group.get(0).getHierarchicalValue();
        }

        // At this point we know the play is a base-compliant play, in either the base or trump suit.
        // This doesn't mean that it is legal, however, as the player may have played trump when
        // they still had the base suit in their hand.
        // The returned hierarchical value is the lowest value in the play
        return groupedPlay.get(0).get(0).getHierarchicalValue();
    }

    PlayLegalityResult getPlayLegalityResult() {
        return playLegalityResult;
    }

    boolean isLegal() {
        return playLegalityResult.isValid;
    }

    boolean isTrashPlay() {
        return playHierarchicalValue == 0;
    }

    int getPlayHierarchicalValue() {
        return playHierarchicalValue;
    }
}
