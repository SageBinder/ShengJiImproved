package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;

import java.util.Collection;

public class RenderableCardList<T extends Card & RenderableCard> extends CardList<T> implements Renderable {
    public RenderableCardList() {
        super();
    }

    public RenderableCardList(Collection<? extends T> other) {
        super(other);
    }

    public void update(float delta) {
        forEach(c -> c.update(delta));
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport) {
        render(batch, viewport, false);
    }

    public void render(SpriteBatch batch, Viewport viewport, boolean renderBase) {
        forEach(c -> {
            c.render(batch, viewport);
            if(renderBase && !c.entity().displayRectEqualsBaseRect()) {
                c.renderBase(batch, viewport);
            }
        });
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean removeAll(Collection<?> list) {
        return super.removeAll(list);
    }

    @Override
    public boolean remove(Rank rank, Suit suit) {
        for(T c : this) {
            if(c.getRank() == rank && c.getSuit() == suit) {
                remove(c);
                return true;
            }
        }
        return false;
    }

    public boolean removeDisposed() {
        return super.removeIf(RenderableCard::isDisposed);
    }

    public void disposeAll() {
        forEach(RenderableCard::dispose);
    }
}
