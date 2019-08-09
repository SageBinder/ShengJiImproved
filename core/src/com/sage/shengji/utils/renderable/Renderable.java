package com.sage.shengji.utils.renderable;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;

public interface Renderable {
    void render(SpriteBatch batch, Viewport viewport);
}
