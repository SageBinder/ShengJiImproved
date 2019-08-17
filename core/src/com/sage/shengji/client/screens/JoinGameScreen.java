package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.server.Server;

public class JoinGameScreen implements Screen, InputProcessor {
    private ShengJiGame game;
    private ClientGameState gameState;

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float viewportScale = 5f;
    private float textProportion = 1f / 7f;

    private Stage stage;
    private Table table;
    private Label screenHeaderLabel;
    private TextField nameField;
    private TextField ipField;
    private TextField portField;
    private Label errorLabel;
    private TextButton joinGameButton;

    private FreeTypeFontGenerator fontGenerator;
    private FreeTypeFontGenerator.FreeTypeFontParameter nameFieldFontParameter;
    private FreeTypeFontGenerator.FreeTypeFontParameter errorLabelFontParameter;
    private FreeTypeFontGenerator.FreeTypeFontParameter joinGameButtonFontParameter;

    public JoinGameScreen(ShengJiGame game) {
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
        int textSize = (int)(Math.max(Gdx.graphics.getHeight(), Gdx.graphics.getWidth()) * textProportion);

        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/OpenSans-Regular.ttf"));

        nameFieldFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        nameFieldFontParameter.size = textSize;
        nameFieldFontParameter.incremental = true;

        errorLabelFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        errorLabelFontParameter.size = textSize;
        errorLabelFontParameter.incremental = true;

        joinGameButtonFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        joinGameButtonFontParameter.size = textSize;
        joinGameButtonFontParameter.incremental = true;
    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        var textFieldStyle = skin.get(TextField.TextFieldStyle.class);
        textFieldStyle.font = fontGenerator.generateFont(nameFieldFontParameter);

        var labelStyle = skin.get(Label.LabelStyle.class);
        labelStyle.font = fontGenerator.generateFont(errorLabelFontParameter);
        labelStyle.font.getData().markupEnabled = true;

        var textButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        textButtonStyle.font = fontGenerator.generateFont(joinGameButtonFontParameter);

        // Creating UI elements:
        screenHeaderLabel = new Label("Join game", labelStyle);

        nameField = new TextField("", textFieldStyle);
        nameField.setMessageText("Name");
        nameField.setMaxLength(Server.MAX_PLAYER_NAME_LENGTH);
        nameField.setDisabled(false);

        ipField = new TextField("", textFieldStyle) {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                var style = super.getStyle();
                Color tempColor = style.fontColor;
                style.fontColor = Color.CYAN;
                super.draw(batch, parentAlpha);
                style.fontColor = tempColor;
            }
        };
        ipField.setMessageText("IP");
        ipField.setTextFieldFilter((textField, c) -> ipField.getText().length() < 45 && (Character.isDigit(c) || c == '.'));

        portField = new TextField("", textFieldStyle) {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                var style = super.getStyle();
                Color tempColor = style.fontColor;
                style.fontColor = Color.ORANGE;
                super.draw(batch, parentAlpha);
                style.fontColor = tempColor;
            }
        };
        portField.setMessageText("Port");
        portField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        portField.setDisabled(false);

        errorLabel = new Label("", labelStyle);
        errorLabel.setAlignment(Align.center);

        joinGameButton = new TextButton("Join game", textButtonStyle);
        joinGameButton.setProgrammaticChangeEvents(true);
        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText();
                String ip = ipField.getText();
                int port;

                if(name.length() == 0) {
                    errorLabel.setText("[RED]Please enter a name");
                    return;
                }
                if(!ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
                    errorLabel.setText("[RED]Error: invalid IP");
                    return;
                }

                try {
                    port = Integer.parseInt(portField.getText());
                } catch(NumberFormatException e) {
                    errorLabel.setText("[RED]Error: Port must be an number between 1 and 65535 inclusive");
                    return;
                }


                if(port < 1 || port > 65535) {
                    errorLabel.setText("[RED]Error: Port must be between 1 and 65535 inclusive");
                    return;
                } else if(port == 1023) { // 1023 is a reserved port
                    errorLabel.setText("[RED]Error: Port 1023 is a reserved port");
                }

                try {
                    game.joinGame(ip, port, name, false);
                } catch(Exception e) {
                    errorLabel.setText("[RED]Error: couldn't connect to Hearts game hosted on [][CYAN]" + ip + "[]:[ORANGE]" + port);
                    return;
                }
                errorLabel.setText("");
                gameState.message = "Connecting...";
                game.showLobbyScreen();
            }
        });

        // Organizing UI elements in table:
        table = new Table().top().padTop(viewport.getWorldHeight() * 0.2f);
        table.setFillParent(true);

        table.row().padBottom(viewport.getWorldHeight() / 30f);
        table.add(screenHeaderLabel).align(Align.center);

        table.row().padTop(viewport.getWorldHeight() / 35f).fillX();
        table.add(nameField).maxWidth(viewport.getWorldWidth() * 0.3f).prefWidth(viewport.getWorldWidth() * 0.3f);

        table.row().padTop(viewport.getWorldHeight() / 120f).fillX();
        table.add(ipField).maxWidth(viewport.getWorldWidth() * 0.3f).prefWidth(viewport.getWorldWidth() * 0.3f);

        table.row().padTop(viewport.getWorldHeight() / 120f).fillX();
        table.add(portField).maxWidth(viewport.getWorldWidth() * 0.3f).prefWidth(viewport.getWorldWidth() * 0.3f);

        table.row().padTop(viewport.getWorldHeight() / 120f).fillX();
        table.add(errorLabel).width(viewport.getWorldWidth() * 0.8f);

        table.row().padTop(viewport.getWorldHeight() * 0.05f).fillX();
        table.add(joinGameButton).maxWidth(viewport.getWorldWidth() * 0.3f).prefWidth(viewport.getWorldWidth() * 0.3f);

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
        inputProcessorsSetup();
    }

    @Override
    public void render(float delta) {
        batch.begin();
        ShengJiGame.clearScreen(batch, viewport);
        batch.end();

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
        switch(keycode) {
        case Input.Keys.ESCAPE:
            game.showStartScreen();
            break;
        case Input.Keys.ENTER:
            joinGameButton.toggle();
            break;
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
