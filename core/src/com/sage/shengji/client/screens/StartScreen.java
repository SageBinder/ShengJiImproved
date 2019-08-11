package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.utils.renderable.RenderableCardGroup;
import com.sage.shengji.utils.renderable.RenderableCardList;

public class StartScreen implements Screen, InputProcessor {
    private ShengJiGame game;
    private ClientGameState gameState;

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float viewportScale = 5f;
    private float textProportion = 1f / 7f;

    private Stage stage;
    private Table table;
    private TextButton createGameButton;
    private TextButton joinGameButton;
    private TextButton quitButton;
    private Label gameStateMessageLabel;

    private FreeTypeFontGenerator fontGenerator;
    private FreeTypeFontGenerator.FreeTypeFontParameter buttonFontParameter;
    private FreeTypeFontGenerator.FreeTypeFontParameter labelFontParameter;

    public StartScreen(ShengJiGame game) {
        this.game = game;
        this.gameState = game.getGameState();

        viewportSetup();
        fontSetup();
        uiSetup();
    }

    private void viewportSetup() {
        float viewportHeight = Gdx.graphics.getHeight() * viewportScale;
        float viewportWidth = Gdx.graphics.getWidth() * viewportScale;
        viewport = new ExtendViewport(viewportWidth, viewportHeight);
        viewport.setWorldSize(viewportWidth, viewportHeight);
    }

    private void fontSetup() {
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/OpenSans-Regular.ttf"));

        buttonFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonFontParameter.size = (int)(Math.max(Gdx.graphics.getHeight(), Gdx.graphics.getWidth()) * textProportion);
        buttonFontParameter.incremental = true;

        labelFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        labelFontParameter.size = (int)(Math.max(Gdx.graphics.getHeight(), Gdx.graphics.getWidth()) * textProportion);
        labelFontParameter.incremental = true;
    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        var textButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        textButtonStyle.font = fontGenerator.generateFont(buttonFontParameter);

        var labelStyle = skin.get(Label.LabelStyle.class);
        labelStyle.font = fontGenerator.generateFont(labelFontParameter);

        // Creating UI elements:
        createGameButton = new TextButton("Host game", textButtonStyle);
        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showCreateGameScreen();
            }
        });

        joinGameButton = new TextButton("Join game", textButtonStyle);
        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showJoinGameScreen();
            }
        });

        quitButton = new TextButton("Exit", textButtonStyle);
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        gameStateMessageLabel = new Label("", labelStyle);
        gameStateMessageLabel.setAlignment(Align.center);
        gameStateMessageLabel.setWrap(true);

        // Organizing UI elements in table:
        table = new Table().top();
        table.setFillParent(true);

        table.row().padTop(viewport.getWorldHeight() * 0.1f);
        table.add(createGameButton)
                .prefWidth(viewport.getWorldWidth() * 0.3f)
                .prefHeight(viewport.getWorldHeight()* 0.1f);

        table.row().padTop(viewport.getWorldHeight() * 0.18f);
        table.add(joinGameButton)
                .prefWidth(viewport.getWorldWidth() * 0.3f)
                .prefHeight(viewport.getWorldHeight()* 0.1f);

        table.row().padTop(viewport.getWorldHeight() * 0.18f);
        table.add(quitButton)
                .prefWidth(viewport.getWorldWidth() * 0.3f)
                .prefHeight(viewport.getWorldHeight()* 0.1f);

        table.row().padTop(viewport.getWorldHeight() * 0.1f);
        table.add(gameStateMessageLabel).width(viewport.getWorldWidth() * 0.8f);

        stage = new Stage(viewport, batch);
        stage.addActor(table);
    }

    private void inputProcessorsSetup() {
        var multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {
        gameState.clean();
        RenderableCardGroup.setDebug(false);
        inputProcessorsSetup();
    }

    @Override
    public void render(float delta) {
        batch.begin();
        ShengJiGame.clearScreen(batch, viewport);
        batch.end();

        gameStateMessageLabel.setText(gameState.message);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        table.invalidate();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
        fontGenerator.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.P) {
            game.showPlaygroundScreen();
        } else if(keycode == Input.Keys.U) {
            game.showUiBuilderScreen();
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
