package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.client.network.ClientConnection;
import com.sage.shengji.client.network.ClientPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;

public class LobbyScreen implements Screen, InputProcessor {
    private static int MAX_NAME_CHARS = 24;

    private ShengJiGame game;
    private ClientGameState gameState;
    private ClientConnection client;

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float textProportion = 1 / 7f;
    private float viewportScale = 5f;

    private Stage stage;
    private Table table;
    private Label gameIPLabel;
    private Label messageLabel;
    private Table playersListTable;
    private TextButton startGameButton;
    private Label gameStateMessageLabel;

    private Label.LabelStyle labelStyle;
    private TextButton.TextButtonStyle textButtonStyle;

    private FreeTypeFontGenerator.FreeTypeFontParameter labelFontParameter;
    private FreeTypeFontGenerator.FreeTypeFontParameter textButtonFontParameter;

    private FreeTypeFontGenerator fontGenerator;

    private final Color hostColor = new Color(1f, 1f, 0f, 1f);

    private boolean quitConfirmationFlag = false;
    private Timer quitConfirmationTimer = new Timer();
    private float quitConfirmationDelay = 3;

    public LobbyScreen(ShengJiGame game) {
        this.game = game;
        this.gameState = game.getGameState();
        this.client = game.getClientConnection();

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

        labelFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        labelFontParameter.size = textSize;
        labelFontParameter.incremental = true;

        textButtonFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textButtonFontParameter.size = textSize;
        textButtonFontParameter.incremental = true;
    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        labelStyle = skin.get(Label.LabelStyle.class);
        labelStyle.font = fontGenerator.generateFont(labelFontParameter);
        labelStyle.font.getData().markupEnabled = true;

        textButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        textButtonStyle.font = fontGenerator.generateFont(textButtonFontParameter);

        // Creating UI elements:
        gameIPLabel = new Label("IP Label", labelStyle);
        gameIPLabel.setAlignment(Align.center);
        gameIPLabel.setWrap(true);

        messageLabel = new Label("", labelStyle);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);

        playersListTable = new Table();
        playersListTable.align(Align.center);

        startGameButton = new TextButton("Start Game", textButtonStyle);
        startGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    client.sendPacket(new ClientPacket(ClientCode.START_GAME));
                } catch(IOException e) {
                    messageLabel.setText("[YELLOW]Error connecting to server. Maybe you lost connection?");
                    return;
                }
                messageLabel.setText("");
            }
        });

        gameStateMessageLabel = new Label("", labelStyle);
        gameStateMessageLabel.setAlignment(Align.center);
        gameStateMessageLabel.setWrap(true);

        // Organizing UI elements into main table:
        table = new Table();
        table.setFillParent(true);
        table.padTop(0);

        table.row().padTop(0);
        table.add(gameIPLabel).padBottom(0).width(viewport.getWorldWidth() * 0.9f);

        table.row();
        table.add(messageLabel)
                .padBottom(viewport.getWorldHeight() * 0.1f)
                .width(viewport.getWorldWidth() * 0.9f);

        table.row();
        table.add(playersListTable).align(Align.center).maxWidth(viewport.getWorldWidth() / 2f);

        table.row().padTop(viewport.getWorldHeight() * 0.1f);
        table.add(startGameButton);

        table.row();
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
        client = game.getClientConnection();
        if(client.serverIP.equals("127.0.0.1")) {
            gameIPLabel.setText("You are hosting, determining your IP...");
            new Thread(() -> {
                try {
                    String thisMachineIP =
                            new BufferedReader(
                                    new InputStreamReader(
                                            new URL("https://api.ipify.org").openStream())).readLine();
                    gameIPLabel.setText("Hosting on [CYAN]" + thisMachineIP + "[]:[ORANGE]" + client.port + "[]");
                } catch(IOException e) {
                    gameIPLabel.setText("[YELLOW]Error: could not determine your IP[]");
                }
                if(!game.successfullyOpenedServerPort()) {
                    gameIPLabel.setText(gameIPLabel.getText()
                            + "\nCouldn't open port [ORANGE]" + client.port + "[]. Players may not be able to join your game.");
                }
            }).start();
        }
        messageLabel.setText("");
        quitConfirmationFlag = false;
        quitConfirmationTimer.clear();
        inputProcessorsSetup();
    }

    @Override
    public void render(float delta) {
        if(gameState.update(client)) {
            updateUIFromGameState();
        }
        stage.act(delta);

        batch.begin();
        ShengJiGame.clearScreen(batch, viewport);
        batch.end();

        stage.draw();
    }

    private void updateUIFromGameState() {
        float groupSpacing = viewport.getWorldWidth() / 12f;

        var pNumHeaderLabel = new Label("P#", labelStyle);
        var pNameHeaderLabel = new Label("NAME", labelStyle);
        var pCallRankHeaderLabel = new Label("RANK", labelStyle);
        pNameHeaderLabel.setAlignment(Align.center);

        TextButton shufflePlayersButton = null;
        if(gameState.thisPlayer != null && gameState.thisPlayer.isHost()) {
            shufflePlayersButton = new TextButton("Shuffle players", textButtonStyle);
            shufflePlayersButton.addListener(new ClickListener(Input.Buttons.LEFT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    requestPlayerShuffle();
                }
            });
        }

        playersListTable.clearChildren();
        playersListTable.setFillParent(false);
        playersListTable.setWidth(viewport.getWorldWidth() / 20f);
        playersListTable.defaults();

        playersListTable.row().padBottom(viewport.getWorldHeight() / 20f);
        playersListTable.add(pNumHeaderLabel);
        playersListTable.add(pNameHeaderLabel).padLeft(groupSpacing);
        playersListTable.add(pCallRankHeaderLabel).padLeft(groupSpacing);
        playersListTable.add(shufflePlayersButton).padLeft(groupSpacing).colspan(3);

        gameState.players.stream().filter(Objects::nonNull).forEach(p -> {
            var playerNumLabel = new Label("P" + p.getPlayerNum(), labelStyle);
            var playerNameLabel = new Label(p.getName(), labelStyle);
            var pointsLabel = new Label(p.getCallRank().toAbbrevString() + "", labelStyle);

            var increasePointsButton = new TextButton("+", textButtonStyle);
            var decreasePointsButton = new TextButton("-", textButtonStyle);
            var resetPointsButton = new TextButton("R", textButtonStyle);

            ((ClickListener)increasePointsButton.getListeners().get(0)).setButton(-1);
            increasePointsButton.addListener(new ClickListener(-1) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(event.getButton() == Input.Buttons.LEFT) {
                        requestPlayerRankChange(p.getPlayerNum(), 1);
                    } else if(event.getButton() == Input.Buttons.RIGHT) {
                        requestPlayerRankChange(p.getPlayerNum(), 10);
                    }
                }
            });

            ((ClickListener)decreasePointsButton.getListeners().get(0)).setButton(-1);
            decreasePointsButton.addListener(new ClickListener(-1) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(event.getButton() == Input.Buttons.LEFT) {
                        requestPlayerRankChange(p.getPlayerNum(), -1);
                    } else if(event.getButton() == Input.Buttons.RIGHT) {
                        requestPlayerRankChange(p.getPlayerNum(), -10);
                    }
                }
            });

            resetPointsButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    requestPlayerRankChangeReset(p.getPlayerNum());
                }
            });

            if(playerNameLabel.getText().length() > MAX_NAME_CHARS) {
                playerNameLabel.setText(playerNameLabel.getText().substring(0, MAX_NAME_CHARS) + "...");
            }

            playersListTable.row().padBottom(viewport.getWorldHeight() * 0.01f);
            playersListTable.add(playerNumLabel).padRight(groupSpacing);
            playersListTable.add(playerNameLabel);
            playersListTable.add(pointsLabel).padLeft(groupSpacing);
            if(gameState.thisPlayer != null && gameState.thisPlayer.isHost()) {
                playersListTable.add(decreasePointsButton)
                        .padLeft(viewport.getWorldWidth() * 0.05f)
                        .minWidth(viewport.getWorldWidth() * 0.05f);
                playersListTable.add(increasePointsButton)
                        .padLeft(viewport.getWorldWidth() * 0.025f)
                        .minWidth(viewport.getWorldWidth() * 0.05f);
                playersListTable.add(resetPointsButton)
                        .padLeft(viewport.getWorldWidth() * 0.025f)
                        .minWidth(viewport.getWorldWidth() * 0.05f);
            }

            if(p.isHost()) {
                playerNumLabel.setColor(hostColor);
                playerNameLabel.setColor(hostColor);
                pointsLabel.setColor(hostColor);
            }
            if(gameState.thisPlayer != null && p.getPlayerNum() == gameState.thisPlayer.getPlayerNum()) {
                playerNumLabel.getText().insert(0, "->");
            }
        });

        playersListTable.invalidate();

        if(gameState.thisPlayer != null && gameState.thisPlayer.isHost()) {
            startGameButton.setVisible(true);
            startGameButton.setDisabled(false);
        } else {
            startGameButton.setVisible(false);
            startGameButton.setDisabled(true);
        }

        gameStateMessageLabel.setText(gameState.message);
        if(gameState.thisPlayer != null && !gameState.thisPlayer.isHost()) {
            gameIPLabel.setText("Connected to [YELLOW]"
                    + gameState.hostPlayer.getName()
                    + (gameState.hostPlayer.getName().charAt(gameState.hostPlayer.getName().length() - 1) == 's' ? "'" : "'s")
                    + "[] lobby hosted on [CYAN]" + client.serverIP + "[]:[ORANGE]" + client.port);
        }
    }

    private void requestPlayerRankChange(int playerNum, int pointsChange) {
        ClientPacket newPointsPacket = new ClientPacket(ClientCode.PLAYER_RANK_CHANGE);
        newPointsPacket.data.put("player", playerNum);
        newPointsPacket.data.put("rankchange", pointsChange);
        try {
            client.sendPacket(newPointsPacket);
        } catch(IOException e) {
            messageLabel.setText("[YELLOW]Error connecting to server. Maybe you lost connection?");
        }
    }

    private void requestPlayerRankChangeReset(int playerNum) {
        try {
            ClientPacket resetPointsPacket = new ClientPacket(ClientCode.RESET_PLAYER_RANK);
            resetPointsPacket.data.put("player", playerNum);
            client.sendPacket(resetPointsPacket);
        } catch(IOException e) {
            messageLabel.setText("[YELLOW]Error connecting to server. Maybe you lost connection?");
        }
    }

    private void requestPlayerShuffle() {
        try {
            ClientPacket shufflePlayersPacket = new ClientPacket(ClientCode.SHUFFLE_PLAYERS);
            client.sendPacket(shufflePlayersPacket);
        } catch(IOException e) {
            messageLabel.setText("[YELLOW]Error connecting to server. Maybe you lost connection?");
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        table.invalidate();
        playersListTable.invalidate();
        updateUIFromGameState();
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
        if(keycode == Input.Keys.ESCAPE) {
            if(!quitConfirmationFlag) {
                messageLabel.setText("[YELLOW]Press ESC again to leave the lobby[]");
                quitConfirmationFlag = true;
                quitConfirmationTimer.scheduleTask(new Timer.Task() {
                    @Override
                    public void run() {
                        messageLabel.setText("");
                        quitConfirmationFlag = false;
                    }
                }, quitConfirmationDelay);
            } else {
                client.quit();
                game.closeGameServer();
                gameState.message = "You left the lobby";
                game.showStartScreen();
            }
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
