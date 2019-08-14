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
    public float cardHeight = Gdx.graphics.getHeight() / 15f;
    public float regionWidth = Gdx.graphics.getWidth() / 10f;
    public float prefDivisionProportion = 0.2f;
    public float rotationRad = 0.0f;
    public float centerProportion = 0.5f;

    private ShapeRenderer debugRenderer = new ShapeRenderer();
    private static boolean inDebugMode = false;


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
        float offset = MathUtils.clamp((cardPositionRegionWidth * centerProportion) - (centerProportion * division * (size() - 1)),
                0,
                cardPositionRegionWidth * centerProportion);

        boolean wrapped;
        if(wrapped = rotationRad > MathUtils.PI2) {
            rotationRad %= MathUtils.PI2;
        }

        if(useCardMover) {
            for(int i = 0; i < size(); i++) {
                RenderableCardEntity e = get(i).entity();

                float rotCos = MathUtils.cos(rotationRad);
                float rotSin = MathUtils.sin(rotationRad);
                float relativeX = i * division + offset;

                e.setOriginProportion(0, 0);
                if(wrapped) {
                    // Gotta subtract PI2 (not mod PI2) so that the rotation direction is correct
                    e.rotateRad(-MathUtils.PI2);
                }
                e.mover.setTargetRotationRad(rotationRad)
                        .setTargetHeight(cardHeight)
                        .setTargetX(pos.x + (relativeX * rotCos))
                        .setTargetY(pos.y + (relativeX * rotSin));
            }
        } else {
            snapCards(division, offset, wrapped);
        }

        super.render(batch, viewport, renderBase);
        if(inDebugMode) {
            batch.end();
            renderDebugLines(viewport);
            batch.begin();
        }
    }

    private void snapCards(float division, float offset, boolean wrapped) {
        for(int i = 0; i < size(); i++) {
            RenderableCardEntity e = get(i).entity();

            float rotCos = MathUtils.cos(rotationRad);
            float rotSin = MathUtils.sin(rotationRad);
            float relativeX = i * division + offset;

            e.setOriginProportion(0, 0);
            if(wrapped) {
                // Gotta subtract PI2 (not mod PI2) so that the rotation direction is correct
                e.rotateRad(-MathUtils.PI2);
            }
            e.setRotationRad(rotationRad)
                    .setHeight(cardHeight)
                    .setX(pos.x + (relativeX * rotCos))
                    .setY(pos.y + (relativeX * rotSin));
        }
    }

    public void snapCards() {
        float cardPositionRegionWidth = regionWidth - (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * cardHeight);
        float division = Math.min(RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * cardHeight * prefDivisionProportion,
                cardPositionRegionWidth / (size() - 1));
        float offset = MathUtils.clamp((cardPositionRegionWidth * centerProportion) - (centerProportion * division * (size() - 1)),
                0,
                cardPositionRegionWidth * centerProportion);
        snapCards(division, offset, false);
    }

    public void setPos(float x, float y) {
        pos.x = x;
        pos.y = y;
    }

    public static void debug() {
        inDebugMode = true;
    }

    public static void setDebug(boolean debug) {
        inDebugMode = debug;
    }

    public static boolean isInDebugMode() {
        return inDebugMode;
    }

    private void renderDebugLines(Viewport viewport) {
        debugRenderer.setProjectionMatrix(viewport.getCamera().combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);

        debugRenderer.setColor(0.0f, 0.0f, 1.0f, 1.0f);
        debugRenderer.rect(pos.x, pos.y, regionWidth, cardHeight);

        debugRenderer.setColor(1.0f, 0.0f, 0.0f, 1.0f);
        debugRenderer.line(pos.x + (regionWidth * centerProportion), pos.y + (cardHeight * 1.5f),
                pos.x + (regionWidth * centerProportion), pos.y - (cardHeight * 0.5f));

        debugRenderer.line(viewport.getWorldWidth() / 2, 0, viewport.getWorldWidth() / 2, viewport.getWorldHeight());
        debugRenderer.line(0, viewport.getWorldHeight() / 2, viewport.getWorldWidth(), viewport.getWorldHeight() / 2);

        for(int i = 0; i < size() - 1; i++) {
            debugRenderer.line(get(i).getDisplayX(), get(i).getDisplayY(), get(i + 1).getDisplayX(), get(i + 1).getDisplayY());
        }

        debugRenderer.end();
    }
}
