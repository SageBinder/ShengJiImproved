package com.sage.shengji.utils.card;

import com.sage.shengji.utils.shengji.ShengJiCard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

public class CardList<T extends Card> extends ArrayList<T> {
    public CardList() {
        super();
    }

    public CardList(Collection<? extends T> other) {
        super(other);
    }

    public CardList(CardList<T> other) {
        super(other);
    }

    public boolean remove(Rank rank, Suit suit) {
        for(T c : this) {
            if(c.getRank() == rank && c.getSuit() == suit) {
                remove(c);
                return true;
            }
        }
        return false;
    }

    public boolean removeByValue(ShengJiCard card) {
        return remove(card.getRank(), card.getSuit());
    }

    public boolean removeAllByValue(CardList<T> remove) {
        boolean removedAny = false;
        for(T c : remove) {
            removedAny |= remove(c.getRank(), c.getSuit());
        }
        return removedAny;
    }

    public CardList<T> removeAllByValueAndGet(CardList<T> remove) {
        CardList<T> removed = new CardList<>();
        for(T c : remove) {
            if(remove(c.getRank(), c.getSuit())) {
                removed.add(c);
            }
        }
        return removed;
    }

    public Optional<T> getAndRemove(Rank rank, Suit suit) {
        for(T c : this) {
            if(c.getRank() == rank && c.getSuit() == suit) {
                remove(c);
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public boolean contains(Rank rank, Suit suit) {
        for(T c : this) {
            if(c.getRank() == rank && c.getSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(T card) {
        return contains(card.getRank(), card.getSuit());
    }

    public boolean containsAny(CardList<T> cards) {
        for(T c : cards) {
            if(contains(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnySuit(Suit suit) {
        for(T c : this) {
            if(c.getSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnyRank(Rank rank) {
        for(T c : this) {
            if(c.getRank() == rank) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Integer> toCardNumList() {
        return stream().mapToInt(Card::getCardNum).boxed().collect(Collectors.toCollection(ArrayList::new));
    }

    public static <T extends Card> CardList<T> fromCardNumList(Collection<Integer> cardNumList, CardSupplier<T> supplier) {
        return cardNumList.stream()
                .mapToInt(cardNum -> cardNum)
                .mapToObj(supplier::get)
                .collect(Collectors.toCollection(CardList::new));
    }

    public ListIterator<T> reverseListIterator() {
        return listIterator(size());
    }

    public interface CardSupplier<T> {
        T get(int cardNum);
    }
}
