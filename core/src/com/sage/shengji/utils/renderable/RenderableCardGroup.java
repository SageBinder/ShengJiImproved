package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.Card;

import java.util.Collection;

public class RenderableCardGroup<T extends Card & RenderableCard> extends RenderableCardList<T> {
    public final Vector2 pos = new Vector2();

    public boolean useCardMover = true;
    public float cardHeight = Gdx.graphics.getHeight() / 15f,
            regionWidth = Gdx.graphics.getWidth() / 10f,
            prefDivisionProportion = 0.2f,
            rotationRad = 0.0f;

    private ShapeRenderer debugRenderer = new ShapeRenderer();
    private boolean inDebugMode = false;


    public RenderableCardGroup() {
        super();
    }

    public RenderableCardGroup(Collection<? extends T> other) {
        super(other);
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport) {
        render(batch, viewport, false);
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport, boolean renderBase) {
        float cardPositionRegionWidth = regionWidth - (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * cardHeight);
        float division = Math.min(RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * cardHeight * prefDivisionProportion,
                cardPositionRegionWidth / (size() - 1));
        float offset = MathUtils.clamp((cardPositionRegionWidth * 0.5f) - (0.5f * division * (size() - 1)),
                0,
                cardPositionRegionWidth * 0.5f);

        boolean wrapped;
        if(wrapped = rotationRad > MathUtils.PI2) {
            rotationRad %= MathUtils.PI2;
        }

        for(int i = 0; i < size(); i++) {
            RenderableCardEntity e = get(i).entity();

            float rotCos = MathUtils.cos(rotationRad);
            float rotSin = MathUtils.sin(rotationRad);
            float relativeX = i * division + offset;
            e.setOriginProportion(0, 0);
            if(useCardMover) {
                if(wrapped) {
                    // Gotta subtract PI2 (not mod PI2) so that the rotation direction is correct
                    e.rotateRad(-MathUtils.PI2);
                }
                e.mover.setTargetRotationRad(rotationRad)
                        .setTargetHeight(cardHeight)
                        .setTargetX(pos.x + (relativeX * rotCos))
                        .setTargetY(pos.y + (relativeX * rotSin));
            } else {
                e.setRotationRad(rotationRad)
                        .setHeight(cardHeight)
                        .setX(pos.x + (relativeX * rotCos))
                        .setY(pos.y + (relativeX * rotSin));
            }
        }

        super.render(batch, viewport, renderBase);
        if(inDebugMode) {
            batch.end();
            renderDebugLines(viewport);
            batch.begin();
        }
    }

    public void setPos(float x, float y) {
        pos.x = x;
        pos.y = y;
    }

    public void debug() {
        inDebugMode = true;
    }

    public void setDebug(boolean debug) {
        this.inDebugMode = debug;
    }

    private void renderDebugLines(Viewport viewport) {
        debugRenderer.setProjectionMatrix(viewport.getCamera().combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);

        debugRenderer.setColor(0.0f, 0.0f, 1.0f, 1.0f);
        debugRenderer.rect(pos.x, pos.y, regionWidth, cardHeight);

        debugRenderer.setColor(1.0f, 0.0f, 0.0f, 1.0f);
        debugRenderer.line(pos.x + (regionWidth * 0.5f), pos.y + (cardHeight * 1.5f),
                pos.x + (regionWidth * 0.5f), pos.y - (cardHeight * 0.5f));

        debugRenderer.line(viewport.getWorldWidth() / 2, 0, viewport.getWorldWidth() / 2, viewport.getWorldHeight());
        debugRenderer.line(0, viewport.getWorldHeight() / 2, viewport.getWorldWidth(), viewport.getWorldHeight() / 2);

        for(int i = 0; i < size() - 1; i++) {
            debugRenderer.line(get(i).getDisplayX(), get(i).getDisplayY(), get(i + 1).getDisplayX(), get(i + 1).getDisplayY());
        }

        debugRenderer.end();
    }
}
