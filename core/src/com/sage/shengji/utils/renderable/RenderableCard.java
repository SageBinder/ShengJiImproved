package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.Card;

public interface RenderableCard<T extends RenderableCardEntity<? extends T, ? extends Card>> extends Renderable {
    default void render(SpriteBatch batch, Viewport viewport) {
        entity().render(batch, viewport);
    }

    default void renderBase(SpriteBatch batch, Viewport viewport) {
        entity().renderBase(batch, viewport);
    }

    default void renderAt(SpriteBatch batch, Viewport viewport,
                          float x, float y, float width, float height,
                          float originXProportion, float originYProportion, float rotation) {
        entity().renderAt(batch, viewport,
                x, y, width, height,
                originXProportion, originYProportion, rotation);
    }

    default void update(float delta) {
        if(entity().mover != null) {
            entity().mover.update(delta);
        }
    }

    default T setMover(RenderableCardMover mover) {
        entity().mover = mover;
        return entity();
    }

    // Position setters:
    default T setPosition(Vector2 pos) {
        return entity().setPosition(pos);
    }

    default T setPosition(float x, float y) {
        return entity().setPosition(x, y);
    }

    default T setX(float x) {
        return entity().setX(x);
    }

    default T setY(float y) {
        return entity().setY(y);
    }

    // Position/size getters:
    default Vector2 getPosition() {
        return entity().getPosition();
    }

    default float getX() {
        return entity().getX();
    }

    default float getY() {
        return entity().getY();
    }

    default float getWidth() {
        return entity().getWidth();
    }

    default float getHeight() {
        return entity().getHeight();
    }

    default Vector2 getDisplayPosition() {
        return entity().getDisplayPosition();
    }

    default float getDisplayX() {
        return entity().getDisplayX();
    }

    default float getDisplayY() {
        return entity().getDisplayY();
    }

    default float getDisplayWidth() {
        return entity().getDisplayWidth();
    }

    default float getDisplayHeight() {
        return entity().getDisplayHeight();
    }

    default boolean isDisposed() {
        return entity().isDisposed();
    }

    default void dispose() {
        entity().dispose();
    }

    T entity();
}
