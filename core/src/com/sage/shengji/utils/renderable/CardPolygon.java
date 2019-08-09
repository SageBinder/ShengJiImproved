package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

class CardPolygon {
    private final Polygon rect = new Polygon();
    private float width;
    private float height;
    private float originXProportion = 0;
    private float originYProportion = 0;

    CardPolygon(float width, float height) throws IllegalArgumentException {
        setSize(width, height);
        setOriginProportion(0, 0);
    }

    public CardPolygon(CardPolygon other) {
        set(other);
    }

    void set(CardPolygon other) {
        rect.setVertices(other.rect.getVertices());
        this.width = other.width;
        this.height = other.height;
        this.originXProportion = other.originXProportion;
        this.originYProportion = other.originYProportion;
    }

    boolean contains(float x, float y) {
        return rect.contains(x, y);
    }

    void setWidth(float width) throws IllegalArgumentException {
        setSize(width, height);
    }

    void setHeight(float height) throws IllegalArgumentException {
        setSize(width, height);
    }

    void setSize(float width, float height) throws IllegalArgumentException {
        this.height = height;
        this.width = width;
        rect.setVertices(new float[]{
                0, 0,
                0, height,
                width, height,
                width, 0
        });
    }

    void setX(float x) {
        rect.setPosition(x, rect.getY());
    }

    void setY(float y) {
        rect.setPosition(rect.getX(), y);
    }

    void setPosition(float x, float y) {
        rect.setPosition(x, y);
    }

    void setPosition(Vector2 position) {
        rect.setPosition(position.x, position.y);
    }

    void setRotationDeg(float deg) {
        rect.setRotation(deg);
    }

    void setOriginXProportion(float originXProportion) {
        this.originXProportion = originXProportion;
        rect.setOrigin(originXProportion * width, rect.getOriginY());
    }

    void setOriginYProportion(float originYProportion) {
        this.originYProportion = originYProportion;
        rect.setOrigin(rect.getOriginX(), originYProportion * height);
    }

    void setOriginProportion(Vector2 proportionalOrigin) {
        setOriginProportion(proportionalOrigin.x, proportionalOrigin.y);
    }

    void setOriginProportion(float originXProportion, float originYProportion) {
        this.originXProportion = originXProportion;
        this.originYProportion = originYProportion;
        rect.setOrigin(originXProportion * width, originYProportion * height);
    }

    void setOriginToCenter() {
        setOriginProportion(0.5f, 0.5f);
    }

    void resetOrigin() {
        setOriginProportion(0, 0);
    }

    float getWidth() {
        return width;
    }

    float getHeight() {
        return height;
    }

    float getX() {
        return rect.getX();
    }

    float getY() {
        return rect.getY();
    }

    float getOriginXProportion() {
        return originXProportion;
    }

    float getOriginYProportion() {
        return originYProportion;
    }

    float getRotation() {
        return rect.getRotation();
    }

    float[] getTransformedVertices() {
        return rect.getTransformedVertices();
    }

    Rectangle getBoundingRectangle() {
        return rect.getBoundingRectangle();
    }
}
