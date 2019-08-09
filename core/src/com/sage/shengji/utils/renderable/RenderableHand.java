package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;

import java.util.Collection;

public class RenderableHand<T extends Card & RenderableCard> extends RenderableCardGroup<T> {
    public float bottomPaddingProportion = 0.025f,
            leftPaddingProportion = 0.05f,
            rightPaddingProportion = 0.05f,
            cardHeightProportion = 1f / 7f;

    public RenderableHand() {
        super();
        super.cardHeight = Gdx.graphics.getHeight() * cardHeightProportion;
    }

    public RenderableHand(Collection<? extends T> cards) {
        super(cards);
        super.cardHeight = Gdx.graphics.getHeight() * cardHeightProportion;
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport) {
        this.render(batch, viewport, false);
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport, boolean renderBase) {
        super.rotationRad = 0;
        super.cardHeight = viewport.getWorldHeight() * cardHeightProportion;
        super.regionWidth = viewport.getWorldWidth()
                - (viewport.getWorldWidth() * leftPaddingProportion)
                - (viewport.getWorldWidth() * rightPaddingProportion);
        super.pos.x = viewport.getWorldWidth() * leftPaddingProportion;
        super.pos.y = viewport.getWorldHeight() * bottomPaddingProportion;

        super.render(batch, viewport, renderBase);
    }

    @Override
    public boolean add(T t) {
        boolean ret = super.add(t);
        sort(Card::compareTo);
        return ret;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        sort(Card::compareTo);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean ret = super.addAll(c);
        sort(Card::compareTo);
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean ret = super.addAll(index, c);
        sort(Card::compareTo);
        return ret;
    }
}
