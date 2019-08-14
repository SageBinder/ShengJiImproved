package com.sage.shengji.client.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.InvalidCardException;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.renderable.RenderableCard;
import com.sage.shengji.utils.renderable.RenderableCardEntity;

public class RenderableShengJiCard extends ShengJiCard
        implements RenderableCard<RenderableShengJiCard.RenderableShengJiCardEntity> {
    public final RenderableShengJiCardEntity entity = new RenderableShengJiCardEntity(this);

    public RenderableShengJiCard(Rank rank, Suit suit, ClientGameState gameState) throws InvalidCardException {
        super(rank, suit, gameState);
    }

    public RenderableShengJiCard(int cardNum, ClientGameState gameState) throws InvalidCardException {
        super(cardNum, gameState);
    }

    public RenderableShengJiCard(RenderableShengJiCard other) throws InvalidCardException {
        super(other);
    }

    public RenderableShengJiCard(ClientGameState gameState) {
        super(gameState);
    }

    // Select setters:
    public RenderableShengJiCardEntity setSelectable(boolean selectable) {
        return entity().setSelectable(selectable);
    }

    public RenderableShengJiCardEntity setSelected(boolean selected) {
        return entity().setSelected(selected);
    }

    public RenderableShengJiCardEntity select() {
        return entity().select();
    }

    public RenderableShengJiCardEntity deselect() {
        return entity().deselect();
    }

    // Select getters:
    public boolean isSelectable() {
        return entity().isSelectable();
    }

    public boolean isSelected() {
        return entity().isSelected();
    }

    public RenderableShengJiCardEntity entity() {
        return entity;
    }



    public class RenderableShengJiCardEntity extends RenderableCardEntity<RenderableShengJiCardEntity, RenderableShengJiCard> {
        // On select
        public float defaultProportionalYChangeOnSelect = 0.9f; // Proportional to height
        public float defaultProportionalXChangeOnSelect = 0.05f; // Proportional to width

        public final Color defaultFaceSelectedBackgroundColor = new Color(defaultFaceBackgroundColor).sub(0.5f, 0.5f, 0.5f, 0);
        public final Color defaultBackSelectedBackgroundColor = new Color(defaultBackBackgroundColor);

        private boolean selectable = true;
        private boolean isSelected = false;

        private float proportionalXChangeOnSelect = defaultProportionalXChangeOnSelect;
        private float proportionalYChangeOnSelect = defaultProportionalYChangeOnSelect;

        // On highlight:
        public float defaultProportionalYChangeOnHighlight = 0.45f;
        public float defaultProportionalXChangeOnHighlight = 0.05f;

        public final Color defaultFaceHighlightedBackgroundColor = new Color(1f, 0.98f, 0.7f, 1f);
        public final Color defaultBackHighlightedBackgroundColor = new Color(1.0f, 1.0f, 0.5f, 1f);

        public final Color defaultFaceHighlightedBorderColor = new Color(Color.CYAN);
        public final Color defaultBackHighlightedBorderColor = new Color(Color.CYAN);

        private float proportionalYChangeOnHighlight = defaultProportionalYChangeOnHighlight;
        private float proportionalXChangeOnHighlight = defaultProportionalXChangeOnHighlight;

        private boolean highlightable = false;
        private boolean isHighlighted = false;

        // Other:
        // HOLY MOTHERFUCKING CHRIST, IF THIS COLOR IS 0.75f, 0.75f, 0, 1, IT APPARENTLY CAUSES A HASH COLLISION WITH
        // THE THE TRUMP BORDER COLOR. THAT'S WHY I'VE MADE IT 0.76f AND NOT 0.75f. FUCK.
        public final Color defaultTrumpCardFaceBorderColor = new Color(0.76f, 0.75f, 0f, 1f);
        public final Color defaultTrumpCardBackBorderColor = new Color(0.76f, 0.75f, 0f, 1f);

        public final Color defaultPointCardFaceBackgroundColor = new Color(0.8f, 0.8f, 1f, 1f);
        public final Color defaultPointCardBackBackgroundColor = new Color(0.8f, 0.8f, 1f, 1f);

        private boolean lastKnownIsTrumpValue = false;
        private boolean lastKnownIsPointCardValue = false;

        private RenderableShengJiCardEntity(RenderableShengJiCard other) {
            super(other);
            cardChangedImpl();
        }

        @Override
        protected void cardChangedImpl() {
            resetBothBackgroundColors();
            resetBothBorderColors();
            lastKnownIsTrumpValue = isTrump();
            lastKnownIsPointCardValue = isPointCard();
        }

        @Override
        public void renderAt(SpriteBatch batch, Viewport viewport, float x, float y, float width, float height, float originXProportion, float originYProportion, float rotationDeg) {
            if(isTrump() != lastKnownIsTrumpValue || isPointCard() != lastKnownIsPointCardValue) {
                cardChangedImpl();
            }
            super.renderAt(batch, viewport, x, y, width, height, originXProportion, originYProportion, rotationDeg);
        }

        @Override
        public RenderableShengJiCardEntity resetFaceBorderColor() {
            if(isHighlighted) {
                return super.setFaceBorderColor(defaultFaceHighlightedBorderColor);
            } else if(isTrump()) {
                return super.setFaceBorderColor(defaultTrumpCardFaceBorderColor);
            } else {
                return super.resetFaceBorderColor();
            }
        }

        @Override
        public RenderableShengJiCardEntity resetBackBorderColor() {
            if(isHighlighted) {
                return super.setBackBorderColor(defaultBackHighlightedBorderColor);
            } else if(isTrump()) {
                return super.setBackBorderColor(defaultTrumpCardBackBorderColor);
            } else {
                return super.resetBackBorderColor();
            }
        }

        @Override
        public RenderableShengJiCardEntity resetFaceBackgroundColor() {
            if(isSelected) {
                return super.setFaceBackgroundColor(defaultFaceSelectedBackgroundColor);
            } else if(isHighlighted) {
                return super.setFaceBackgroundColor(defaultFaceHighlightedBackgroundColor);
            } else if(isPointCard()) {
                return super.setFaceBackgroundColor(defaultPointCardFaceBackgroundColor);
            } else {
                return super.resetFaceBackgroundColor();
            }
        }

        @Override
        public RenderableShengJiCardEntity resetBackBackgroundColor() {
            if(isSelected) {
                return super.setBackBackgroundColor(defaultBackSelectedBackgroundColor);
            } else if(isHighlighted) {
                return super.setBackBackgroundColor(defaultBackHighlightedBackgroundColor);
            } else if(isPointCard()) {
                return super.setBackBackgroundColor(defaultPointCardFaceBackgroundColor);
            } else {
                return super.resetBackBackgroundColor();
            }
        }

        private void resetTargetDisplayProportionOffset() {
            if(isSelected) {
                mover.setTargetDisplayProportionalXOffset(proportionalXChangeOnSelect);
                mover.setTargetDisplayProportionalYOffset(proportionalYChangeOnSelect);
            } else if(isHighlighted) {
                mover.setTargetDisplayProportionalXOffset(proportionalXChangeOnHighlight);
                mover.setTargetDisplayProportionalYOffset(proportionalYChangeOnHighlight);
            } else {
                mover.setTargetDisplayProportionalXOffset(0);
                mover.setTargetDisplayProportionalYOffset(0);
            }
        }

        // Position change on select methods:
        public RenderableShengJiCardEntity setProportionalYChangeOnSelect(float proportionalYChangeOnSelect) {
            this.proportionalYChangeOnSelect = proportionalYChangeOnSelect;
            if(isSelected) {
                mover.setTargetDisplayProportionalYOffset(proportionalYChangeOnSelect);
            }
            return this;
        }
        public RenderableShengJiCardEntity setAbsoluteYChangeOnSelect(float absoluteYChangeOnSelect) {
            setProportionalYChangeOnSelect(absoluteYChangeOnSelect / getHeight());
            return this;
        }

        public RenderableShengJiCardEntity setProportionalXChangeOnSelect(float proportionalXChangeOnSelect) {
            this.proportionalXChangeOnSelect = proportionalXChangeOnSelect;
            if(isSelected) {
                mover.setTargetDisplayProportionalXOffset(proportionalXChangeOnSelect);
            }
            return this;
        }

        public RenderableShengJiCardEntity setAbsoluteXChangeOnSelect(float absoluteXChangeOnSelect) {
            setProportionalXChangeOnSelect(absoluteXChangeOnSelect / getWidth());
            return this;
        }

        public float getProportionalXChangeOnSelect() {
            return proportionalXChangeOnSelect;
        }

        public float getProportionalYChangeOnSelect() {
            return proportionalYChangeOnSelect;
        }


        // Selected setters:
        public RenderableShengJiCardEntity select() {
            setSelected(true);
            return this;
        }
        public RenderableShengJiCardEntity deselect() {
            setSelected(false);
            return this;
        }

        public RenderableShengJiCardEntity toggleSelected() {
            return setSelected(!isSelected);
        }

        public RenderableShengJiCardEntity setSelected(boolean selected) {
            if(isSelected == selected || !selectable) {
                return this;
            } else {
                isSelected = selected;
                resetTargetDisplayProportionOffset();
                resetBothBackgroundColors();
                resetBothBorderColors();
                return this;
            }
        }

        public RenderableShengJiCardEntity setSelectable(boolean selectable) {
            this.selectable = selectable;
            return this;
        }

        // Selected getters:
        public boolean isSelected() {
            return isSelected;
        }

        public boolean isSelectable() {
            return selectable;
        }

        // Position change on highlight methods:
        public RenderableShengJiCardEntity setProportionalYChangeOnHighlight(float proportionalYChangeOnHighlight) {
            this.proportionalYChangeOnHighlight = proportionalYChangeOnHighlight;
            if(isHighlighted && !isSelected) {
                mover.setTargetDisplayProportionalYOffset(proportionalYChangeOnHighlight);
            }
            return this;
        }
        public RenderableShengJiCardEntity setAbsoluteYChangeOnHighlight(float absoluteYChangeOnHighlight) {
            setProportionalYChangeOnHighlight(absoluteYChangeOnHighlight / getHeight());
            return this;
        }

        public RenderableShengJiCardEntity setProportionalXChangeOnHighlight(float proportionalXChangeOnHighlight) {
            this.proportionalXChangeOnHighlight = proportionalXChangeOnHighlight;
            if(isHighlighted && !isSelected) {
                mover.setTargetDisplayProportionalXOffset(proportionalXChangeOnHighlight);
            }
            return this;
        }

        public RenderableShengJiCardEntity setAbsoluteXChangeOnHighlight(float absoluteXChangeOnHighlight) {
            setProportionalXChangeOnHighlight(absoluteXChangeOnHighlight / getWidth());
            return this;
        }

        public float getProportionalYChangeOnHighlight() {
            return proportionalYChangeOnHighlight;
        }

        public float getProportionalXChangeOnHighlight() {
            return proportionalXChangeOnHighlight;
        }

        // Highlight setters:
        public RenderableShengJiCardEntity setHighlightable(boolean highlightable) {
            this.highlightable = highlightable;
            return this;
        }

        public RenderableShengJiCardEntity highlight() {
            return setHighlighted(true);
        }

        public RenderableShengJiCardEntity toggleHighlighted() {
            return setHighlighted(!isHighlighted);
        }

        public RenderableShengJiCardEntity setHighlighted(boolean highlighted) {
            if(isHighlighted == highlighted || !highlightable) {
                return this;
            } else {
                isHighlighted = highlighted;
                resetTargetDisplayProportionOffset();
                resetBothBackgroundColors();
                resetBothBorderColors();
                return this;
            }
        }

        // Highlight getters:
        public boolean isHighlightable() {
            return highlightable;
        }

        public boolean isHighlighted() {
            return isHighlighted;
        }
    }
}
