package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.math.MathUtils;

public abstract class RenderableCardMover {
    public final RenderableCard card;
    public float posSpeed = 4;
    public float sizeSpeed = 4;
    public float rotSpeed = MathUtils.PI2;
    public float displayPosOffsetSpeed = posSpeed;
    public float displayRotOffsetSpeed = MathUtils.PI2;
    public float displayProportionSpeed = 5;
    public float displayProportionalOffsetSpeed = 10;

    protected Target target;

    RenderableCardMover(RenderableCard card) {
        this.card = card;
        target = new Target();
    }

    public abstract void update(float delta);

    public void stop() {
        target.targetX = null;
        target.targetY = null;
        target.targetRotation = null;

        target.targetDisplayXOffset = null;
        target.targetDisplayYOffset = null;
        target.targetDisplayRotationOffset = null;
        target.targetDisplayProportion = null;

        target.targetDisplayProportionalXOffset = null;
        target.targetDisplayProportionalYOffset = null;
    }

    public Float getTargetX() {
        return target.targetX;
    }

    public Float getTargetY() {
        return target.targetY;
    }

    public Float getTargetHeight() {
        return target.targetHeight;
    }

    public Float getTargetRotation() {
        return target.targetRotation;
    }

    public Float getTargetDisplayXOffset() {
        return target.targetDisplayXOffset;
    }

    public Float getTargetDisplayYOffset() {
        return target.targetDisplayYOffset;
    }

    public Float getTargetDisplayRotationOffset() {
        return target.targetDisplayRotationOffset;
    }

    public Float getTargetDisplayProportion() {
        return target.targetDisplayProportion;
    }

    public Float getTargetDisplayProportionalXOffset() {
        return target.targetDisplayProportionalXOffset;
    }

    public Float getTargetDisplayProportionalYOffset() {
        return target.targetDisplayProportionalYOffset;
    }

    public RenderableCardMover setTargetX(float targetX) {
        target.targetX = targetX;
        return this;
    }

    public RenderableCardMover setTargetY(float targetY) {
        target.targetY = targetY;
        return this;
    }

    public RenderableCardMover setTargetPos(float x, float y) {
        setTargetX(x);
        setTargetY(y);
        return this;
    }

    public RenderableCardMover setTargetHeight(float targetHeight) {
        this.target.targetHeight = targetHeight;
        return this;
    }

    public RenderableCardMover setTargetWidth(float targetWidth) {
        this.target.targetHeight = targetWidth * RenderableCardEntity.HEIGHT_TO_WIDTH_RATIO;
        return this;
    }

    public RenderableCardMover setTargetRotationRad(float targetRotationRad) {
        target.targetRotation = targetRotationRad;
        return this;
    }

    public RenderableCardMover setTargetRotationDeg(float targetRotationDeg) {
        return setTargetRotationRad(targetRotationDeg * MathUtils.degreesToRadians);
    }

    public RenderableCardMover setTargetDisplayXOffset(float targetDisplayXOffset) {
        target.targetDisplayXOffset = targetDisplayXOffset;
        return this;
    }

    public RenderableCardMover setTargetDisplayYOffset(float targetDisplayYOffset) {
        target.targetDisplayYOffset = targetDisplayYOffset;
        return this;
    }

    public RenderableCardMover setTargetDisplayXYOffset(float targetDisplayXOffset, float targetDisplayYOffset) {
        setTargetDisplayXOffset(targetDisplayXOffset);
        setTargetDisplayYOffset(targetDisplayYOffset);
        return this;
    }

    public RenderableCardMover setTargetDisplayRotationOffset(float targetDisplayRotationOffset) {
        target.targetDisplayRotationOffset = targetDisplayRotationOffset;
        return this;
    }

    public RenderableCardMover setTargetDisplayProportion(float targetDisplayProportion) {
        target.targetDisplayProportion = targetDisplayProportion;
        return this;
    }

    public RenderableCardMover setTargetDisplayProportionalXOffset(float targetDisplayProportionalXOffset) {
        target.targetDisplayProportionalXOffset = targetDisplayProportionalXOffset;
        return this;
    }

    public RenderableCardMover setTargetDisplayProportionalYOffset(float targetDisplayProportionalYOffset) {
        target.targetDisplayProportionalYOffset = targetDisplayProportionalYOffset;
        return this;
    }

    public static RenderableCardMover scaledDistanceMover(RenderableCard c) {
        return new RenderableCardMover(c) {
            private float getChange(float deltaVar, float delta, float speed) {
                return Math.signum(deltaVar) * Math.min(
                        Math.abs(deltaVar * delta * speed),
                        Math.abs(deltaVar));
            }

            @Override
            public void update(float delta) {
                if(target == null) {
                    return;
                }

                RenderableCardEntity entity = card.entity();
                if(target.targetX != null) {
                    entity.setX(entity.getX()
                            + getChange(target.targetX - entity.getX(), delta, posSpeed));
                }
                if(target.targetY != null) {
                    entity.setY(entity.getY()
                            + getChange(target.targetY - entity.getY(), delta, posSpeed));
                }
                if(target.targetHeight != null) {
                    entity.setHeight(entity.getHeight()
                            + getChange(target.targetHeight - entity.getHeight(), delta, sizeSpeed));
                }
                if(target.targetRotation != null) {
                    entity.setRotationRad(entity.getRotationRad()
                            + getChange(target.targetRotation - entity.getRotationRad(), delta, rotSpeed));
                }
                if(target.targetDisplayXOffset != null) {
                    entity.setDisplayXOffset(entity.getDisplayXOffset()
                            + getChange(target.targetDisplayXOffset - entity.getDisplayXOffset(), delta, displayPosOffsetSpeed));
                }
                if(target.targetDisplayYOffset != null) {
                    entity.setDisplayYOffset(entity.getDisplayYOffset()
                            + getChange(target.targetDisplayYOffset - entity.getDisplayYOffset(), delta, displayPosOffsetSpeed));
                }
                if(target.targetDisplayRotationOffset != null) {
                    entity.setDisplayRotationOffsetRad(entity.getDisplayRotationOffsetRad()
                            + getChange(target.targetDisplayRotationOffset - entity.getDisplayRotationOffsetRad(), delta, displayRotOffsetSpeed));
                }
                if(target.targetDisplayProportion != null) {
                    entity.setDisplayProportion(entity.getDisplayProportion()
                            + getChange(target.targetDisplayProportion - entity.getDisplayProportion(), delta, displayProportionSpeed));
                }
                if(target.targetDisplayProportionalXOffset != null) {
                    entity.setDisplayProportionalXOffset(entity.getDisplayProportionalXOffset()
                            + getChange(target.targetDisplayProportionalXOffset - entity.getDisplayProportionalXOffset(), delta, displayProportionalOffsetSpeed));
                }
                if(target.targetDisplayProportionalYOffset != null) {
                    entity.setDisplayProportionalYOffset(entity.getDisplayProportionalYOffset()
                            + getChange(target.targetDisplayProportionalYOffset - entity.getDisplayProportionalYOffset(), delta, displayProportionalOffsetSpeed));
                }
            }
        };
    }

    public class Target {
        public Float targetX = null;
        public Float targetY = null;
        public Float targetHeight = null;
        public Float targetRotation = null;

        public Float targetDisplayXOffset = null;
        public Float targetDisplayYOffset = null;
        public Float targetDisplayRotationOffset = null;
        public Float targetDisplayProportion = null;

        public Float targetDisplayProportionalXOffset = null;
        public Float targetDisplayProportionalYOffset = null;
    }
}
