package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.client.game.RenderablePlayer;
import com.sage.shengji.client.network.ClientConnection;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.utils.renderable.RenderableCardEntity;

import java.util.Objects;

public class GameScreen implements Screen, InputProcessor {
    private ShengJiGame game;
    private ClientGameState gameState;
    private ClientConnection client;

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float viewportScale = 5f;
    private float textProportion = 1 / 7f;

    private Stage uiStage;
    private TextButton actionButton;

    private FreeTypeFontGenerator fontGenerator;
    private FreeTypeFontGenerator.FreeTypeFontParameter textButtonFontParameter;

    private BitmapFont quitConfirmationFont;
    private String quitConfirmationText = "";

    private BitmapFont messageFont;
    private BitmapFont errorFont;
    private BitmapFont trumpCardFont;
    private BitmapFont collectedPointCardsFont;
    private BitmapFont friendCardsFont;
    private BitmapFont kittyFont;

    private boolean quitConfirmationFlag = false;
    private Timer quitConfirmationTimer = new Timer();
    private float quitConfirmationDelay = 3f;

    private float updateDelay = 0;
    private float delayCounter = 0;

    private float handHeightProportion = 1f / 7f;
    private float expandedHandHeightProportion = 1f / 5f;

    private float playersCenterXProportion = 0.5f;
    private float playersCenterYProportion = 0.70f;

    private float playersWidthRadiusProportion = 0.35f;
    private float playersHeightRadiusProportion = 0.22f;


    private boolean mouseControl = true;

    public GameScreen(ShengJiGame game) {
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

        textButtonFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textButtonFontParameter.size = textSize;
        textButtonFontParameter.incremental = true;

        var quitConfirmationFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        quitConfirmationFontParameter.size = (int)(textSize * 1.3f);
        quitConfirmationFontParameter.color = Color.RED;
        quitConfirmationFontParameter.incremental = true;
        quitConfirmationFont = fontGenerator.generateFont(quitConfirmationFontParameter);

        var messageFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        messageFontParameter.size = textSize;
        messageFontParameter.incremental = true;
        messageFont = fontGenerator.generateFont(messageFontParameter);
        messageFont.getData().markupEnabled = true;

        var errorFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        errorFontParameter.size = textSize;
        errorFontParameter.incremental = true;
        errorFont = fontGenerator.generateFont(errorFontParameter);
        errorFont.getData().markupEnabled = true;

        var trumpCardFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        trumpCardFontParameter.size = textSize;
        trumpCardFontParameter.incremental = true;
        trumpCardFont = fontGenerator.generateFont(trumpCardFontParameter);

        var collectedPointCardsFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        collectedPointCardsFontParameter.size = textSize;
        collectedPointCardsFontParameter.incremental = true;
        collectedPointCardsFont = fontGenerator.generateFont(collectedPointCardsFontParameter);

        var friendCardsFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        friendCardsFontParameter.size = textSize;
        friendCardsFontParameter.incremental = true;
        friendCardsFont = fontGenerator.generateFont(friendCardsFontParameter);

        var kittyFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        kittyFontParameter.size = textSize;
        kittyFontParameter.incremental = true;
        kittyFont = fontGenerator.generateFont(kittyFontParameter);
    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        var actionButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        actionButtonStyle.font = fontGenerator.generateFont(textButtonFontParameter);

        // Creating UI elements:
        actionButton = new TextButton("", actionButtonStyle);
        actionButton.setProgrammaticChangeEvents(true);

        // Adding UI elements to stage:
        uiStage = new Stage(viewport, batch);
        uiStage.addActor(actionButton);
    }

    private void inputProcessorsSetup() {
        var multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        multiplexer.addProcessor(uiStage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {
        client = game.getClientConnection();
        inputProcessorsSetup();
        updateUiFromGameState();
        gameState.thisPlayerHand.prefDivisionProportion = 0.4f;
    }

    @Override
    public void render(float delta) {
        update(delta);

        // Render:
        viewport.apply(true);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();
        ShengJiGame.clearScreen(batch, viewport);

        errorFont.draw(batch, gameState.errorMessage,
                viewport.getWorldWidth() * 0.5f,
                viewport.getWorldHeight() * 0.45f,
                0, Align.center, false);
        messageFont.draw(batch, gameState.message,
                viewport.getWorldWidth() * 0.5f,
                viewport.getWorldHeight() * 0.55f,
                0, Align.center, false);
        actionButton.setPosition(viewport.getWorldWidth() * 0.9f, viewport.getWorldHeight() * 0.9f, Align.center);
        actionButton.setWidth(viewport.getWorldWidth() - (gameState.thisPlayerHand.pos.x + gameState.thisPlayerHand.regionWidth));

        ServerCode lastServerCode = gameState.lastServerCode;
        switch(lastServerCode) {
        case SEND_FRIEND_CARDS:
        case INVALID_FRIEND_CARDS:
            renderFriendCardsPositionForChoosing();
            break;

        case ROUND_END:
            renderRoundEndKitty();
            break;

        case WAIT_FOR_KITTY_CARD:
        case WAIT_FOR_KITTY_CALL_WINNER:
        case MAKE_KITTY_CALL:
        case INVALID_KITTY_CALL:
        case UNSUCCESSFUL_KITTY_CALL:
        case SUCCESSFUL_KITTY_CALL:
        case NO_KITTY_CALL:
            renderCallKitty();
            break;
        }

        if(lastServerCode != ServerCode.ROUND_END) {
            renderPlayers(viewport.getWorldWidth() * playersCenterXProportion,
                    viewport.getWorldHeight() * playersCenterYProportion,
                    viewport.getWorldWidth() * playersWidthRadiusProportion,
                    viewport.getWorldHeight() * playersHeightRadiusProportion);
        }
        if(gameState.trumpCard != null) {
            renderTrumpCard();
        }
        if(lastServerCode != ServerCode.SEND_FRIEND_CARDS
                && lastServerCode != ServerCode.INVALID_FRIEND_CARDS
                && !gameState.friendCards.isEmpty()) {
            renderFriendCards();
        }

        if(gameState.numPointsNeeded > 0) {
            renderCollectedPointCards();
        }
        gameState.thisPlayerHand.render(batch, viewport);
        quitConfirmationFont.draw(batch, quitConfirmationText,
                viewport.getWorldWidth() / 2,
                viewport.getWorldHeight() - quitConfirmationFont.getCapHeight(),
                0, Align.center, false);
        batch.end();

        uiStage.draw();
    }

    private void update(float delta) {
        delayCounter += delta;
        if(delayCounter >= updateDelay && gameState.update(client)) {
            updateUiFromGameState();
            delayCounter = 0;
        }

        handleInputs();
        updateCards(delta);
        uiStage.act(delta);
    }

    private void renderTrumpCard() {
        gameState.trumpCard.entity.setHeight(gameState.thisPlayerHand.cardHeight);
        gameState.trumpCard.setPosition(
                gameState.thisPlayerHand.pos.x * 0.5f,
                (viewport.getWorldHeight() * 0.6f) - (gameState.trumpCard.getHeight() * 0.5f));
        gameState.trumpCard.render(batch, viewport);

        trumpCardFont.setColor(Color.GOLD);
        trumpCardFont.draw(batch, "Trump\ncard",
                gameState.trumpCard.getPosition().x + (gameState.trumpCard.getWidth() * 0.5f),
                gameState.trumpCard.getPosition().y + (trumpCardFont.getXHeight() * 4) + gameState.trumpCard.getHeight(),
                0, Align.center, false);
        trumpCardFont.setColor(Color.WHITE);
    }

    private void renderCollectedPointCards() {
        gameState.collectedPointCards.regionWidth = gameState.players.get(0).getPlay().regionWidth * 1.5f;
        gameState.collectedPointCards.cardHeight = gameState.players.get(0).getPlay().cardHeight * 1.2f;
        gameState.collectedPointCards.setPos(
                viewport.getWorldWidth() * 0.05f,
                viewport.getWorldHeight() - gameState.collectedPointCards.cardHeight - (viewport.getWorldWidth() * 0.027f));
        gameState.collectedPointCards.render(batch, viewport);

        collectedPointCardsFont.draw(batch,
                gameState.numCollectedPoints + "/" + gameState.numPointsNeeded + " points",
                gameState.collectedPointCards.pos.x + (gameState.collectedPointCards.regionWidth * 0.5f),
                gameState.collectedPointCards.pos.y - (collectedPointCardsFont.getXHeight() * 2),
                0, Align.center, false);
    }

    private void renderFriendCardsPositionForChoosing() {
        gameState.friendCards.cardHeight = viewport.getWorldHeight() * 0.11f;

        gameState.friendCards.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.friendCards.cardHeight) * 4;

        gameState.friendCards.setPos(
                (viewport.getWorldWidth() - gameState.friendCards.regionWidth) * 0.5f,
                gameState.thisPlayerHand.pos.y + (gameState.thisPlayerHand.cardHeight * 2)
        );

        gameState.friendCards.render(batch, viewport);
    }

    private void renderFriendCards() {
        gameState.friendCards.cardHeight = viewport.getWorldHeight() * 0.11f;

        gameState.friendCards.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.friendCards.cardHeight) * 4;

        gameState.friendCards.setPos(
                gameState.friendCards.regionWidth * 0.1f,
                (viewport.getWorldHeight() * 0.4f) - (gameState.friendCards.cardHeight * 0.75f));

        friendCardsFont.draw(batch, "Friend\ncards",
                gameState.friendCards.pos.x + (gameState.friendCards.regionWidth * 0.5f),
                gameState.friendCards.pos.y + (friendCardsFont.getXHeight() * 4) + gameState.friendCards.cardHeight,
                0, Align.center, false);

        gameState.friendCards.render(batch, viewport);
    }

    private void renderRoundEndKitty() {
        gameState.kitty.prefDivisionProportion = 1.5f;
        gameState.kitty.cardHeight = gameState.thisPlayerHand.cardHeight;
        gameState.kitty.regionWidth = gameState.thisPlayerHand.regionWidth;
        gameState.kitty.pos.set(
                gameState.thisPlayerHand.pos.x,
                gameState.thisPlayerHand.pos.y + (gameState.thisPlayerHand.cardHeight * 0.25f));

        kittyFont.draw(batch, "Final kitty:",
                gameState.kitty.pos.x + (gameState.kitty.regionWidth * 0.5f),
                gameState.kitty.pos.y + (kittyFont.getXHeight() * 4) + gameState.kitty.cardHeight,
                0, Align.center, false);

        gameState.kitty.render(batch, viewport);
    }

    private void renderCallKitty() {
        gameState.kitty.prefDivisionProportion = 0.8f;
        gameState.kitty.cardHeight = viewport.getWorldHeight() * 0.11f;

        gameState.kitty.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.kitty.cardHeight) * 4;

        gameState.kitty.setPos(
                (viewport.getWorldWidth() - gameState.kitty.regionWidth) * 0.5f,
                gameState.thisPlayerHand.pos.y + (gameState.thisPlayerHand.cardHeight * 2));

        kittyFont.draw(batch, "Draw from kitty",
                gameState.kitty.pos.x + (gameState.kitty.regionWidth * 0.5f),
                gameState.kitty.pos.y + (friendCardsFont.getXHeight() * 4) + gameState.kitty.cardHeight,
                0, Align.center, false);

        gameState.kitty.render(batch, viewport);
    }

    private void handleInputs() {
        gameState.players.stream().filter(Objects::nonNull).forEach(p -> p.setExpanded(false));

        var hand = gameState.thisPlayerHand;
        hand.cardHeightProportion = handHeightProportion;

        if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)) {
            Vector2 mousePos = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

            if(hand.stream().anyMatch(c ->
                    c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))) {
                hand.cardHeightProportion = expandedHandHeightProportion;
                return;
            }

            gameState.players.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getPlay().stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos))
                            || p.getPointCards().stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos)))
                    .findFirst().ifPresent(p -> p.setExpanded(true));
        }
    }

    private void updateCards(float delta) {
        gameState.thisPlayerHand.forEach(c -> c.entity.mover.posSpeed = 6);
        gameState.thisPlayerHand.update(delta);
        gameState.players.stream().filter(Objects::nonNull).forEach(p -> p.update(delta));
    }

    private void renderPlayers(float centerX, float centerY, float widthRadius, float heightRadius) {
        var players = gameState.players;
        float angleIncrement = MathUtils.PI2 / players.size();
        float shift = (players.indexOf(gameState.thisPlayer) * angleIncrement) + (MathUtils.PI * 0.5f);

        for(int i = 0; i < players.size(); i++) {
            RenderablePlayer toRender = players.get(i);
            if(toRender == null) {
                continue;
            }

            toRender.setX((MathUtils.cos((i * angleIncrement) - shift) * widthRadius) + centerX);
            toRender.setY((MathUtils.sin((i * angleIncrement) - shift) * heightRadius) + centerY);
            toRender.render(batch, viewport);
        }
    }

    private void updateUiFromGameState() {
        setActionButtonFromServerCode(gameState.lastServerCode);
    }

    private void setActionButtonFromServerCode(ServerCode code) {
        switch(code) {
        case SUCCESSFUL_KITTY:
        case SUCCESSFUL_KITTY_CALL:
        case SUCCESSFUL_FRIEND_CARDS:
        case WAIT_FOR_KITTY:
        case WAIT_FOR_FRIEND_CARDS:
        case WAIT_FOR_KITTY_CARD:
        case WAIT_FOR_PLAYERS:
        case WAIT_FOR_KITTY_CALL_WINNER:
        case WAIT_FOR_CALL_WINNER:
        case NO_CALL:
        case NO_ONE_CALLED:
        case KITTY_EXHAUSTED_REDEAL:
        case NO_KITTY_CALL:
        case WAITING_ON_CALLER:
            disableButton(actionButton);
            actionButton.setText("");
            break;

        case SUCCESSFUL_PLAY:
            disableButton(actionButton);
            actionButton.setText("");
            updateDelay = 0.5f;
            break;

        case MAKE_CALL:
        case SUCCESSFUL_CALL:
        case UNSUCCESSFUL_CALL:
        case INVALID_CALL:
            enableButton(actionButton, () -> gameState.actions.sendCall(client));
            actionButton.setText("Send call");
            break;

        case MAKE_KITTY_CALL:
        case INVALID_KITTY_CALL:
        case UNSUCCESSFUL_KITTY_CALL:
            enableButton(actionButton, () -> gameState.actions.sendKittyCall(client));
            actionButton.setText("Send call");
            break;

        case SEND_KITTY:
        case INVALID_KITTY:
            enableButton(actionButton, () -> gameState.actions.sendKitty(client));
            actionButton.setText("Send kitty");
            break;

        case SEND_FRIEND_CARDS:
        case INVALID_FRIEND_CARDS:
            enableButton(actionButton, () -> gameState.actions.sendFriendCards(client));
            actionButton.setText("Send friend cards");
            break;

        case MAKE_PLAY:
        case INVALID_PLAY:
            enableButton(actionButton, () -> gameState.actions.sendPlay(client));
            actionButton.setText("Send play");

            if(gameState.thisPlayerHand.size() == 1) {
                gameState.thisPlayerHand.get(0).setSelected(true);
                actionButton.toggle();
            }
            break;

        case TRICK_END:
            disableButton(actionButton);
            updateDelay = 4;
            break;

        case ROUND_END:
            enableButton(actionButton, () -> game.showLobbyScreen());
            actionButton.setText("Exit to lobby");
            break;

        case TURN_OVER:
            disableButton(actionButton);
            actionButton.setText("");
            updateDelay = 2;
            break;

        case ROUND_START:
            // --- FALL THROUGH ---
        case TRICK_START:
        case WAIT_FOR_TURN_PLAYER:
        case WAIT_FOR_HAND:
        case WAIT_FOR_NEW_PLAY:
            disableButton(actionButton);
            actionButton.setText("");
            // --- FALL THROUGH ---
        default:
            updateDelay = 0;
        }
    }

    private static void enableButton(TextButton button, ButtonAction action) {
        while(button.getListeners().size > 1) {
            button.getListeners().removeIndex(button.getListeners().size - 1);
        }
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.onAction();
            }
        });
        button.setDisabled(false);
        button.setVisible(true);
    }

    private void disableButton(TextButton button) {
        while(button.getListeners().size > 1) {
            button.getListeners().removeIndex(button.getListeners().size - 1);
        }
        button.setDisabled(true);
        button.setVisible(false);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
        case Input.Keys.TAB:
            gameState.thisPlayerHand.forEach(c -> c.entity.setHighlighted(false));
            break;

        case Input.Keys.ESCAPE:
            if(!quitConfirmationFlag) {
                quitConfirmationText = "Press ESC again to leave the game";
                quitConfirmationFlag = true;
                quitConfirmationTimer.scheduleTask(new Timer.Task() {
                    @Override
                    public void run() {
                        quitConfirmationText = "";
                        quitConfirmationFlag = false;
                    }
                }, quitConfirmationDelay);
            } else {
                client.quit();
                game.closeGameServer();
                gameState.message = "You left the game";
                game.showStartScreen();
            }
            break;

        case Input.Keys.LEFT:
        case Input.Keys.RIGHT:
            if(gameState.thisPlayerHand.isEmpty()) {
                break;
            }

            var highlighted = gameState.thisPlayerHand.stream().filter(c -> c.entity.isHighlighted()).findAny();
            if(highlighted.isPresent()) {
                var highlightedCard = highlighted.get();
                int nextHighlightIdx = (gameState.thisPlayerHand.indexOf(highlightedCard)
                        + (keycode == Input.Keys.RIGHT ? 1 : -1)) % gameState.thisPlayerHand.size();
                if(nextHighlightIdx < 0) {
                    nextHighlightIdx = gameState.thisPlayerHand.size() - 1;
                }

                gameState.thisPlayerHand.forEach(c -> c.entity.setHighlighted(false));
                gameState.thisPlayerHand.get(nextHighlightIdx).entity.setHighlightable(true).setHighlighted(true);
            } else {
                gameState.thisPlayerHand.get(keycode == Input.Keys.RIGHT ? 0 : gameState.thisPlayerHand.size() - 1)
                        .entity.setHighlightable(true).setHighlighted(true);
            }
            mouseControl = false;
            break;

        case Input.Keys.SPACE:
            gameState.thisPlayerHand.stream()
                    .filter(c -> c.entity.isHighlighted())
                    .findAny().ifPresent(c -> c.entity.toggleSelected());
            break;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch(keycode) {
        case Input.Keys.ENTER:
            actionButton.toggle();
            // --- FALL THROUGH ---
        case Input.Keys.SPACE:
            updateDelay = 0;
            break;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        var clickPos = viewport.unproject(new Vector2(screenX, screenY));
        if(button == Input.Buttons.LEFT) {
            mouseControl = true;
            gameState.thisPlayerHand.forEach(c -> c.entity.setHighlighted(false));

            for(var i = gameState.thisPlayerHand.reverseListIterator(); i.hasPrevious(); ) {
                var entity = i.previous().entity;
                if(entity.displayRectContainsPoint(clickPos)
                        || (entity.baseRectContainsPoint(clickPos) && !entity.isSelected())) {
                    entity.toggleSelected();
                    break;
                }
            }
        }
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
        if(mouseControl) {
            var newMousePos = viewport.unproject(new Vector2(screenX, screenY));

            gameState.thisPlayerHand.forEach(c -> {
                c.entity.setHighlightable(true);
                c.entity.setHighlighted(false);
            });
            for(var i = gameState.thisPlayerHand.reverseListIterator(); i.hasPrevious(); ) {
                var entity = i.previous().entity;
                if(entity.displayRectContainsPoint(newMousePos) || entity.baseRectContainsPoint(newMousePos)) {
                    if(!entity.isSelected()) {
                        entity.setHighlighted(true);
                        break;
                    } else if(entity.displayRectContainsPoint(newMousePos)) {
                        break;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    private interface ButtonAction {
        void onAction();
    }
}
