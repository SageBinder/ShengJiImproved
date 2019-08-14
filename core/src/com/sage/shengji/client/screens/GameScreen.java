package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import com.sage.shengji.client.game.RenderableShengJiCard;
import com.sage.shengji.client.network.ClientConnection;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.renderable.RenderableCardEntity;
import com.sage.shengji.utils.renderable.RenderableCardGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameScreen implements Screen, InputProcessor {
    private ShengJiGame game;
    private ClientGameState gameState;
    private ClientConnection client;

    private static ShapeRenderer debugRenderer = new ShapeRenderer();

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float viewportScale = 5f;
    private float textProportion = 1 / 7f;

    private Stage uiStage;
    private TextButton actionButton;
    private TextButton noCallButton;

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
    private BitmapFont playerNameFont;

    private boolean quitConfirmationFlag = false;
    private Timer quitConfirmationTimer = new Timer();
    private float quitConfirmationDelay = 3f;

    private float updateDelay = 0;
    private float delayCounter = 0;

    private float handHeightProportion = 1f / 7f;
    private float expandedHandHeightProportion = 1f / 5f;

    private float playersCenterXProportion = 0.50f;
    private float playersCenterYProportion = 0.70f;

    private float playersWidthRadiusProportion = 0.35f;
    private float playersHeightRadiusProportion = 0.19f;

    private boolean renderRoundEndKitty = false;
    private boolean renderCallKitty = false;
    private boolean renderPlayers = false;
    private boolean renderFriendCardsForChoosing = false;

    private boolean collectedPointCardExpanded = false;
    private boolean kittyExpanded = false;
    private boolean friendCardsExpanded = false;

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
        messageFontParameter.size = (int)(textSize * 0.8f);
        messageFontParameter.incremental = true;
        messageFont = fontGenerator.generateFont(messageFontParameter);
        messageFont.getData().markupEnabled = true;

        var errorFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        errorFontParameter.size = (int)(textSize * 0.8f);
        errorFontParameter.incremental = true;
        errorFont = fontGenerator.generateFont(errorFontParameter);
        errorFont.getData().markupEnabled = true;

        var trumpCardFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        trumpCardFontParameter.size = (int)(textSize * 0.85f);
        trumpCardFontParameter.incremental = true;
        trumpCardFont = fontGenerator.generateFont(trumpCardFontParameter);

        var collectedPointCardsFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        collectedPointCardsFontParameter.size = (int)(textSize * 0.6f);
        collectedPointCardsFontParameter.incremental = true;
        collectedPointCardsFont = fontGenerator.generateFont(collectedPointCardsFontParameter);

        var friendCardsFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        friendCardsFontParameter.size = (int)(textSize * 0.85f);
        friendCardsFontParameter.incremental = true;
        friendCardsFont = fontGenerator.generateFont(friendCardsFontParameter);

        var kittyFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        kittyFontParameter.size = textSize;
        kittyFontParameter.incremental = true;
        kittyFont = fontGenerator.generateFont(kittyFontParameter);

        var playerNameFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        playerNameFontParameter.size = (int)(textSize * 0.8f);
        playerNameFontParameter.incremental = true;
        playerNameFont = fontGenerator.generateFont(playerNameFontParameter);
        playerNameFont.getData().markupEnabled = true;
        RenderablePlayer.setNameFont(playerNameFont);
    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        var actionButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        actionButtonStyle.font = fontGenerator.generateFont(textButtonFontParameter);

        // Creating UI elements:
        actionButton = new TextButton("", actionButtonStyle);
        actionButton.setProgrammaticChangeEvents(true);

        noCallButton = new TextButton("", actionButtonStyle);
        noCallButton.setProgrammaticChangeEvents(true);

        // Adding UI elements to stage:
        uiStage = new Stage(viewport, batch);
        uiStage.addActor(actionButton);
        uiStage.addActor(noCallButton);
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
        gameState.thisPlayerHand.leftPaddingProportion = 0.15f;
        gameState.thisPlayerHand.rightPaddingProportion = 0.15f;
        gameState.thisPlayerHand.bottomPaddingProportion = 0.09f;

        gameState.collectedPointCards.centerProportion = 0.0f;

        gameState.trumpCardGroup.prefDivisionProportion = 0.2f;

        // Lazy copy/pasted code so that kitty.snapCards() works correctly the first time
        gameState.kitty.prefDivisionProportion = 0.75f;
        gameState.kitty.cardHeight = gameState.thisPlayerHand.cardHeight;
        gameState.kitty.regionWidth = gameState.thisPlayerHand.regionWidth;
        gameState.kitty.setPos(
                gameState.thisPlayerHand.pos.x,
                viewport.getWorldHeight() * 0.45f);

        quitConfirmationFlag = false;
        quitConfirmationTimer.clear();
        quitConfirmationText = "";

        RenderableCardGroup.setDebug(false);
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
                viewport.getWorldWidth() * playersCenterXProportion,
                gameState.thisPlayerHand.pos.y + (viewport.getWorldHeight() * handHeightProportion) + errorFont.getLineHeight(),
//                viewport.getWorldHeight() * playersCenterYProportion - errorFont.getCapHeight(),
                0, Align.center, false);
        messageFont.draw(batch, gameState.message,
                viewport.getWorldWidth() * playersCenterXProportion,
                viewport.getWorldHeight() - errorFont.getCapHeight(),
                0, Align.center, false);

        if(renderFriendCardsForChoosing) {
            renderFriendCardsForChoosing();
        }
        if(renderRoundEndKitty) {
            renderRoundEndKitty();
        }
        if(renderCallKitty) {
            renderCallKitty();
        }
        if(renderPlayers) {
            renderPlayers();
        }
        if(!gameState.trumpCardGroup.isEmpty()) {
            renderTrumpCard();
        }
        if(!renderFriendCardsForChoosing && !gameState.friendCards.isEmpty()) {
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

        if(!actionButton.isDisabled()) {
            actionButton.setWidth(viewport.getWorldWidth() * 0.4f);
            actionButton.setHeight(gameState.thisPlayerHand.pos.y * 0.8f);
            actionButton.setPosition(
                    viewport.getWorldWidth() * 0.5f,
                    gameState.thisPlayerHand.pos.y * 0.5f,
                    Align.center);
        }
        if(!noCallButton.isDisabled()) {
            noCallButton.setWidth((viewport.getWorldWidth() - gameState.thisPlayerHand.pos.x - gameState.thisPlayerHand.regionWidth) * 0.8f);
            noCallButton.setHeight(actionButton.getHeight() * 0.8f);
            noCallButton.setPosition(
                    viewport.getWorldWidth() - (noCallButton.getWidth() * 0.6f),
                    gameState.thisPlayerHand.pos.y * 0.5f,
                    Align.center);
        }
        uiStage.draw();
    }

    private void renderTrumpCard() {
        gameState.trumpCardGroup.cardHeight = gameState.thisPlayerHand.cardHeight;
        gameState.trumpCardGroup.regionWidth = gameState.thisPlayerHand.pos.x;
        gameState.trumpCardGroup.setPos(0, gameState.thisPlayerHand.pos.y);
        gameState.trumpCardGroup.render(batch, viewport);

        trumpCardFont.setColor(Color.GOLD);
        trumpCardFont.draw(batch, "Trump",
                gameState.trumpCardGroup.pos.x + (gameState.trumpCardGroup.regionWidth * 0.5f),
                gameState.trumpCardGroup.pos.y + (trumpCardFont.getXHeight() * 2) + gameState.trumpCardGroup.cardHeight,
                0, Align.center, false);
        trumpCardFont.setColor(Color.WHITE);
    }

    private void renderCollectedPointCards() {
        gameState.collectedPointCards.regionWidth = viewport.getWorldWidth() / 8f;
        gameState.collectedPointCards.cardHeight = viewport.getWorldHeight() / 10f;

        if(collectedPointCardExpanded) {
            gameState.collectedPointCards.cardHeight *= 1.5f;
            gameState.collectedPointCards.regionWidth *= 1.5f;
        }

        gameState.collectedPointCards.setPos(
                gameState.collectedPointCards.regionWidth * 0.1f,
                viewport.getWorldHeight() - gameState.collectedPointCards.cardHeight - (gameState.collectedPointCards.regionWidth * 0.1f));
        gameState.collectedPointCards.render(batch, viewport);

        collectedPointCardsFont.draw(batch,
                gameState.numCollectedPoints + "/" + gameState.numPointsNeeded + " points",
                gameState.collectedPointCards.pos.x + (gameState.collectedPointCards.regionWidth * 0.5f),
                viewport.getWorldHeight() - collectedPointCardsFont.getXHeight() * 0.25f,
                0, Align.center, false);
    }

    private void renderFriendCardsForChoosing() {
        gameState.friendCards.prefDivisionProportion = 0.9f;
        gameState.friendCards.cardHeight = viewport.getWorldHeight() * 0.17f;
        gameState.friendCards.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.friendCards.cardHeight) * 5;

        if(friendCardsExpanded) {
            gameState.friendCards.cardHeight *= 1.5f;
            gameState.friendCards.regionWidth *= 1.5f;
        }

        gameState.friendCards.setPos(
                (viewport.getWorldWidth() * playersCenterXProportion) - (gameState.friendCards.regionWidth * 0.5f),
                (viewport.getWorldHeight() * playersCenterYProportion) - (gameState.friendCards.cardHeight * 0.7f));

        gameState.friendCards.render(batch, viewport);
    }

    private void renderFriendCards() {
        gameState.friendCards.cardHeight = gameState.thisPlayerHand.cardHeight;
        gameState.friendCards.prefDivisionProportion = 0.2f;
        gameState.friendCards.regionWidth = viewport.getWorldWidth() - gameState.thisPlayerHand.pos.x - gameState.thisPlayerHand.regionWidth;;
        gameState.friendCards.setPos(gameState.thisPlayerHand.pos.x + gameState.thisPlayerHand.regionWidth, gameState.thisPlayerHand.pos.y);

        friendCardsFont.draw(batch, "Friend\nCards",
                gameState.friendCards.pos.x + (gameState.friendCards.regionWidth * 0.5f),
                gameState.friendCards.pos.y + (friendCardsFont.getXHeight() * 4) + gameState.friendCards.cardHeight,
                0, Align.center, false);

        gameState.friendCards.render(batch, viewport);
    }

    private void renderRoundEndKitty() {
        gameState.kitty.prefDivisionProportion = 0.75f;
        gameState.kitty.cardHeight = gameState.thisPlayerHand.cardHeight;
        gameState.kitty.regionWidth = gameState.thisPlayerHand.regionWidth;

        if(kittyExpanded) {
            gameState.kitty.cardHeight *= 1.5f;
            gameState.kitty.regionWidth *= 1.5f;
        }

        gameState.kitty.setPos(
                (viewport.getWorldWidth() - gameState.kitty.regionWidth) * 0.5f,
                viewport.getWorldHeight() * 0.45f);

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

        if(kittyExpanded) {
            gameState.kitty.cardHeight *= 1.5f;
            gameState.kitty.regionWidth *= 1.5f;
        }

        gameState.kitty.setPos(
                (viewport.getWorldWidth() * playersCenterXProportion) - (gameState.kitty.regionWidth * 0.5f),
                (viewport.getWorldHeight() * playersCenterYProportion) - (gameState.kitty.cardHeight * 0.7f));

        kittyFont.draw(batch, "From Kitty",
                gameState.kitty.pos.x + (gameState.kitty.regionWidth * 0.5f),
                gameState.kitty.pos.y + (kittyFont.getXHeight() * 2) + gameState.kitty.cardHeight,
                0, Align.center, false);

        gameState.kitty.render(batch, viewport);
    }

    private void handleInputs() {
        gameState.players.stream().filter(Objects::nonNull).forEach(p -> p.setExpanded(false));
        collectedPointCardExpanded = false;
        kittyExpanded = false;
        friendCardsExpanded = false;

        var hand = gameState.thisPlayerHand;
        hand.cardHeightProportion = handHeightProportion;

        if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)) {
            Vector2 mousePos = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

            if(hand.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))
                    || gameState.trumpCardGroup.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))
                    || gameState.friendCards.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))) {
                if(gameState.friendCards.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))) {
                    friendCardsExpanded = true;
                }
                hand.cardHeightProportion = expandedHandHeightProportion;
                return;
            }

            gameState.players.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getPlay().stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos))
                            || p.getPointCards().stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos)))
                    .findFirst().ifPresent(p -> p.setExpanded(true));

            if(gameState.collectedPointCards.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))) {
                collectedPointCardExpanded = true;
            }

            if(gameState.kitty.stream().anyMatch(c -> c.entity.displayRectContainsPoint(mousePos) || c.entity.baseRectContainsPoint(mousePos))) {
                kittyExpanded = true;
            }
        }
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

    private void updateCards(float delta) {
        gameState.thisPlayerHand.forEach(c -> {
            c.entity.mover.posSpeed = 6;
            c.entity.mover.sizeSpeed = 6;
        });
        gameState.thisPlayerHand.update(delta);
        gameState.players.stream().filter(Objects::nonNull).forEach(p -> p.update(delta));
        gameState.friendCards.update(delta);
        gameState.kitty.update(delta);
        gameState.collectedPointCards.update(delta);
        gameState.trumpCardGroup.update(delta);
    }

    private void renderPlayers() {
        var players = gameState.players;
        List<RenderablePlayer> expandedPlayers = new ArrayList<>();

        for(int i = 0; i < players.size(); i++) {
            var toRender = players.get(i);
            if(toRender == null) {
                continue;
            }

            if(toRender.isExpanded()) {
                expandedPlayers.add(toRender);
            } else {
                toRender.render(batch, viewport);
            }
        }

        expandedPlayers.forEach(p -> p.render(batch, viewport));

        if(RenderableCardGroup.isInDebugMode()) {
            batch.end();
            debugRenderer.begin(ShapeRenderer.ShapeType.Line);

            debugRenderer.setProjectionMatrix(viewport.getCamera().combined);
            debugRenderer.setColor(Color.GREEN);

            gameState.players.forEach(p ->
                    debugRenderer.line(viewport.getWorldWidth() * playersCenterXProportion,
                            viewport.getWorldHeight() * playersCenterYProportion,
                            p.getX(),
                            p.getY()));

            debugRenderer.end();
            batch.begin();
        }
    }

    private void setPlayerPositions(float centerX, float centerY, float widthRadius, float heightRadius) {
        var players = gameState.players;
        float angleIncrement = MathUtils.PI2 / players.size();
        float shift = (players.indexOf(gameState.thisPlayer) * angleIncrement) + (MathUtils.PI * 0.5f);

        float[][] playerPointsXY = equidistantEllipsePoints(widthRadius, heightRadius, -shift, centerX, centerY, players.size());

        for(int i = 0; i < players.size(); i++) {
            var toRender = players.get(i);
            if(toRender == null) {
                continue;
            }
            toRender.setPos(playerPointsXY[i][0], playerPointsXY[i][1]);
        }
    }

    // Code taken from https://stackoverflow.com/a/20510150.
    // AAAAAAHHHHHH THANK YOU DAVE, I LOVE YOU AAAAAAHHHH!!!!!!!!!
    private static float[][] equidistantEllipsePoints(float r1, float r2, float thetaShift, float centerX, float centerY, int numPoints) {
        float[][] pointsXY = new float[numPoints][2];
        double theta = 0.0;
        double deltaTheta = 0.0001;
        double circumference = 0.0;
        double numIntegrals = Math.round(MathUtils.PI2 / deltaTheta);
        double dpt;

        /* integrate over the ellipse to get the circumference */
        for(int i = 0; i < numIntegrals; i++) {
            theta += i * deltaTheta;
            dpt = computeDpt(r1, r2, theta);
            circumference += dpt;
        }

        int pointIdx = 0;
        double run = 0.0;
        theta = thetaShift;

        for(int i = 0; i < numIntegrals; i++) {
            theta += deltaTheta;
            double subIntegral = numPoints * run / circumference;
            if((int)subIntegral >= pointIdx) {
                if(pointIdx >= pointsXY.length) {
                    break;
                }

                double x = (r1 * Math.cos(theta)) + centerX;
                double y = (r2 * Math.sin(theta)) + centerY;

                pointsXY[pointIdx][0] = (float)x;
                pointsXY[pointIdx][1] = (float)y;

                pointIdx++;
            }
            run += computeDpt(r1, r2, theta);
        }

        return pointsXY;
    }

    private static float computeDpt(double r1, double r2, double theta) {
        double dp;

        double dpt_sin = Math.pow(r1 * Math.sin(theta), 2.0);
        double dpt_cos = Math.pow(r2 * Math.cos(theta), 2.0);
        dp = Math.sqrt(dpt_sin + dpt_cos);

        return (float)dp;
    }

    private void updateUiFromGameState() {
        updateUiFromServerCode(gameState.lastServerCode);
    }

    private void updateUiFromServerCode(ServerCode code) {
        boolean disableNoCallButton = false;
        boolean disableActionButton = false;

        switch(code) {
        case ROUND_START:
            updateDelay = 0;
            // --- FALL THROUGH ---
        case KITTY_EXHAUSTED_REDEAL:
            renderPlayers = true;
            renderCallKitty = false;
            renderRoundEndKitty = false;
            renderFriendCardsForChoosing = false;
            disableNoCallButton = true;
            break;

        case WAIT_FOR_KITTY_CARD:
            gameState.kitty.snapCards();
            renderCallKitty = true;
            break;

        case WAIT_FOR_FRIEND_CARDS:
        case SUCCESSFUL_FRIEND_CARDS:
        case SUCCESSFUL_KITTY:
        case WAIT_FOR_KITTY:
        case NO_ONE_CALLED:
        case WAITING_ON_CALLER:
            renderFriendCardsForChoosing = false;
            renderCallKitty = false;
            // --- FALL THROUGH ---
        case NO_CALL:
        case NO_KITTY_CALL:
        case SUCCESSFUL_KITTY_CALL:
            disableActionButton = true;
            disableNoCallButton = true;
            break;

        case WAIT_FOR_KITTY_CALL_WINNER:
        case WAIT_FOR_CALL_WINNER:
            updateDelay = 2;
            disableActionButton = true;
            disableNoCallButton = true;
            break;

        case SUCCESSFUL_PLAY:
            updateDelay = 0.5f;
            disableActionButton = true;
            break;

        case WAIT_FOR_NEW_LEADING_CALL:
        case MAKE_CALL:
        case SUCCESSFUL_CALL:
        case UNSUCCESSFUL_CALL:
        case INVALID_CALL:
            enableButton(actionButton, () -> gameState.actions.sendCall());
            enableButton(noCallButton, () -> gameState.actions.sendNoCall());
            actionButton.setText("Send call");
            noCallButton.setText("No call");
            break;

        case MAKE_KITTY_CALL:
        case INVALID_KITTY_CALL:
        case UNSUCCESSFUL_KITTY_CALL:
            renderCallKitty = true;
            enableButton(actionButton, () -> gameState.actions.sendKittyCall());
            enableButton(noCallButton, () -> gameState.actions.sendNoKittyCall());
            actionButton.setText("Claim call");
            noCallButton.setText("No call");
            break;

        case SEND_KITTY:
        case INVALID_KITTY:
            enableButton(actionButton, () -> gameState.actions.sendKitty());
            actionButton.setText("Send kitty");
            break;

        case SEND_FRIEND_CARDS:
            if(gameState.friendCards.size() > 0) {
                gameState.friendCards.get(0).entity.setHighlighted(true);
                gameState.thisPlayerHand.forEach(c -> c.entity.setHighlighted(false));
            }
        case INVALID_FRIEND_CARDS:
            renderFriendCardsForChoosing = true;
            enableButton(actionButton, () -> gameState.actions.sendFriendCards());
            actionButton.setText("Send friend cards");
            if(gameState.friendCards.stream().noneMatch(c -> c.entity.isHighlighted())) {
                gameState.friendCards.stream().findFirst().ifPresent(c -> c.entity.setHighlighted(true));
            }
            break;

        case MAKE_PLAY:
        case INVALID_PLAY:
            enableButton(actionButton, () -> gameState.actions.sendPlay());
            actionButton.setText("Send play");

            if(gameState.thisPlayerHand.size() == 1) {
                gameState.thisPlayerHand.get(0).setSelected(true);
                actionButton.toggle();
            }
            break;

        case TRICK_END:
            updateDelay = 1;
            break;

        case ROUND_END:
            renderPlayers = false;
            renderRoundEndKitty = true;
            enableButton(actionButton, () -> game.showLobbyScreen());
            actionButton.setText("Exit to lobby");
            break;

        case TURN_OVER:
            disableActionButton = true;
            if(gameState.thisPlayerHand.isEmpty()) {
                updateDelay = 5f;
            }
            break;

        case WAIT_FOR_HAND:
            gameState.thisPlayerHand.snapCards();
        case TRICK_START:
        case WAIT_FOR_TURN_PLAYER:
        case WAIT_FOR_NEW_PLAY:
            disableActionButton = true;
        default:
            updateDelay = 0;
        }

        if(disableNoCallButton) {
            disableButton(noCallButton);
            noCallButton.setText("");
        }
        if(disableActionButton) {
            disableButton(actionButton);
            actionButton.setText("");
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
        setPlayerPositions(viewport.getWorldWidth() * playersCenterXProportion,
                viewport.getWorldHeight() * playersCenterYProportion,
                viewport.getWorldWidth() * playersWidthRadiusProportion,
                viewport.getWorldHeight() * playersHeightRadiusProportion);
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

        case Input.Keys.UP:
        case Input.Keys.DOWN:
            if(gameState.lastServerCode == ServerCode.SEND_FRIEND_CARDS || gameState.lastServerCode == ServerCode.INVALID_FRIEND_CARDS) {
                if(gameState.friendCards.isEmpty()) {
                    break;
                }

                int valChange = (keycode == Input.Keys.UP) ? 1 : -1;
                var editCard = gameState.friendCards.stream()
                        .filter(c -> c.entity.isHighlighted())
                        .findFirst().orElse(gameState.friendCards.get(0));
                editCard.setCardNum(Math.floorMod(editCard.getCardNum() + valChange, Card.MAX_CARD_NUM));
                editCard.entity.setHighlighted(true);
            }
            break;

        case Input.Keys.LEFT:
        case Input.Keys.RIGHT:
            if(gameState.lastServerCode == ServerCode.SEND_FRIEND_CARDS || gameState.lastServerCode == ServerCode.INVALID_FRIEND_CARDS) {
                gameState.friendCards.forEach(c -> {
                    c.entity.setProportionalXChangeOnHighlight(0);
                    c.entity.setProportionalYChangeOnHighlight(0);
                    c.entity.setProportionalXChangeOnSelect(0);
                    c.entity.setProportionalYChangeOnSelect(0);
                });
                highlightNextInCardGroup(gameState.friendCards, keycode);
            } else {
                highlightNextInCardGroup(gameState.thisPlayerHand, keycode);
            }
            break;

        case Input.Keys.SPACE:
            gameState.thisPlayerHand.stream()
                    .filter(c -> c.entity.isHighlighted())
                    .findFirst().ifPresent(c -> c.entity.toggleSelected());
            break;

        case Input.Keys.D:
            RenderableCardGroup.setDebug(!RenderableCardGroup.isInDebugMode());
            break;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch(keycode) {
        case Input.Keys.X:
            if(!noCallButton.isDisabled()) {
                noCallButton.toggle();
            }
            break;

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
                if(entity.displayRectContainsPoint(newMousePos)
                        || (!entity.isSelected() && entity.baseRectContainsPoint(newMousePos))) {
                    entity.setHighlighted(true);
                    break;
                }
            }
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if(amount > 0) {
            for(int i = 0; i < amount; i++) {
                keyDown(Input.Keys.DOWN);
            }
        } else {
            int absAmount = Math.abs(amount);
            for(int i = 0; i < absAmount; i++) {
                keyDown(Input.Keys.UP);
            }
        }

        return false;
    }

    public void highlightNextInCardGroup(RenderableCardGroup<RenderableShengJiCard> group, int keycode) {
        if(group.isEmpty()) {
            return;
        }

        if(keycode != Input.Keys.LEFT && keycode != Input.Keys.RIGHT) {
            return;
        }

        var highlighted = group.stream().filter(c -> c.entity.isHighlighted()).findFirst();
        if(highlighted.isPresent()) {
            var highlightedCard = highlighted.get();
            int nextHighlightIdx = (group.indexOf(highlightedCard)
                    + (keycode == Input.Keys.RIGHT ? 1 : -1)) % group.size();
            if(nextHighlightIdx < 0) {
                nextHighlightIdx = group.size() - 1;
            }

            group.forEach(c -> c.entity.setHighlighted(false));
            group.get(nextHighlightIdx).entity.setHighlightable(true).setHighlighted(true);
        } else {
            group.get(keycode == Input.Keys.RIGHT ? 0 : group.size() - 1)
                    .entity.setHighlightable(true).setHighlighted(true);
        }
    }

    private interface ButtonAction {
        void onAction();
    }
}
