package com.sage.shengji.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dosse.upnp.UPnP;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.client.network.ClientConnection;
import com.sage.shengji.client.network.ClientPacket;
import com.sage.shengji.client.screens.*;
import com.sage.shengji.server.Server;
import com.sage.shengji.utils.renderable.RenderableCardEntity;

import java.io.IOException;

public class ShengJiGame extends Game {
    private static final Color BACKGROUND_COLOR = new Color(0, 0.2f, 0.11f, 1);
    private static Texture backgroundTexture;

    private ClientGameState gameState;
    private ClientConnection clientConnection;
    private Server server;
    private boolean successfullyOpenedServerPort = false;

    private Screen startScreen,
            createGameScreen,
            joinGameScreen,
            lobbyScreen,
            gameScreen,
            playgroundScreen,
            uiBuilderScreen;

    private Timer titleTimer;

    @Override
    public void create() {
        Gdx.graphics.setTitle("♤♥升級♧♦");

        backgroundTexture = new Texture(Gdx.files.internal("background.jpg"));

        gameState = new ClientGameState(this);
        startScreen = new StartScreen(this);
        createGameScreen = new CreateGameScreen(this);
        joinGameScreen = new JoinGameScreen(this);
        lobbyScreen = new LobbyScreen(this);
        gameScreen = new GameScreen(this);
        playgroundScreen = new PlaygroundScreen(this);
        uiBuilderScreen = new UiBuilderScreen(this);

        setScreen(startScreen);

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeGameServer));
    }

    public void showStartScreen() {
        RenderableCardEntity.disposeAllEntities();

        setScreen(startScreen);
    }

    public void showCreateGameScreen() {
        setScreen(createGameScreen);
    }

    public void showJoinGameScreen() {
        setScreen(joinGameScreen);
    }

    public void showLobbyScreen() {
        setScreen(lobbyScreen);
    }

    public void showGameScreen() {
        setScreen(gameScreen);
    }

    public void showPlaygroundScreen() {
        setScreen(playgroundScreen);
    }

    public void showUiBuilderScreen() {
        setScreen(uiBuilderScreen);
    }

    public Texture getFeltBackgroundTexture() {
        return backgroundTexture;
    }

    public void joinGame(String serverIP, int port, String name) {
        clientConnection = new ClientConnection(serverIP, port, name, this);
        clientConnection.start();
        ClientPacket namePacket = new ClientPacket(ClientCode.NAME);
        namePacket.data.put("name", name);
        try {
            clientConnection.sendPacket(namePacket);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void startGameServer(int port) {
        closeGameServer();

        this.server = new Server(port); // If server couldn't be started it will throw an exception
        server.start();
    }

    public void openServerPort() {
        if(server == null) {
            return;
        }

        if(!UPnP.isMappedTCP(server.port)) {
            successfullyOpenedServerPort = UPnP.openPortTCP(server.port);
        } else {
            successfullyOpenedServerPort = true;
        }
    }

    public boolean successfullyOpenedServerPort() {
        return successfullyOpenedServerPort;
    }

    public void closeGameServer() {
        if(server != null) {
            new Thread(() -> UPnP.closePortTCP(server.port)).start();
            server.close();
            server = null;
            successfullyOpenedServerPort = false;
        }
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }

    public ClientGameState getGameState() {
        return gameState;
    }

    @Override
    public void dispose() {
        startScreen.dispose();
        createGameScreen.dispose();
        joinGameScreen.dispose();
        lobbyScreen.dispose();
        gameScreen.dispose();
        playgroundScreen.dispose();
        closeGameServer();
    }

    public static void clearScreen(SpriteBatch spriteBatch, Viewport viewport) {
        Gdx.gl.glClearColor(ShengJiGame.BACKGROUND_COLOR.r,
                ShengJiGame.BACKGROUND_COLOR.g,
                ShengJiGame.BACKGROUND_COLOR.b,
                ShengJiGame.BACKGROUND_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT
                | GL20.GL_DEPTH_BUFFER_BIT
                | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        spriteBatch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
    }
}
