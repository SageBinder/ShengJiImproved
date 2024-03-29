package com.sage.shengji.client.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.client.game.ClientGameState;
import com.sage.shengji.client.game.RenderablePlayer;
import com.sage.shengji.client.game.RenderableShengJiCard;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.utils.card.Card;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;
import com.sage.shengji.utils.renderable.RenderableCardEntity;
import com.sage.shengji.utils.renderable.RenderableCardGroup;

import java.util.*;

public class UiBuilderScreen extends InputAdapter implements Screen {
    private ClientGameState gameState;
    private ShengJiGame game;

    private ShapeRenderer debugRenderer = new ShapeRenderer();

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

    private float handHeightProportion = 1f / 7f;
    private float expandedHandHeightProportion = 1f / 5f;

    private float playersCenterXProportion = 0.50f;
    private float playersCenterYProportion = 0.70f;

    private float playersWidthRadiusProportion = 0.45f;
    private float playersHeightRadiusProportion = 0.19f;

    private boolean renderChoosingFriendCards = false;
    private boolean renderRoundEndKitty = false;
    private boolean renderCallKitty = false;

    public UiBuilderScreen(ShengJiGame game) {
        this.game = game;
        gameState = new ClientGameState(game);

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
        actionButton = new TextButton("Send play", actionButtonStyle);
        actionButton.setProgrammaticChangeEvents(true);

        noCallButton = new TextButton("No call", actionButtonStyle);
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
        inputProcessorsSetup();
        RenderableCardGroup.debug();
        gameState.thisPlayerHand.prefDivisionProportion = 0.4f;
        gameState.thisPlayerHand.leftPaddingProportion = 0.15f;
        gameState.thisPlayerHand.rightPaddingProportion = 0.15f;
        gameState.thisPlayerHand.bottomPaddingProportion = 0.09f;

        gameState.players.clear();
        gameState.players.add(new RenderablePlayer(0, "This player"));
        gameState.thisPlayer = gameState.players.get(0);
        for(int i = 1; i < 10; i++) {
            gameState.players.add(new RenderablePlayer(i, "Player " + i));
        }

        gameState.players.forEach(p -> {
            p.clearCards();
            p.getPlay().addAll(List.of(
                    new RenderableShengJiCard(Rank.SMALL_JOKER, Suit.JOKER, gameState),
                    new RenderableShengJiCard(Rank.SMALL_JOKER, Suit.JOKER, gameState),
                    new RenderableShengJiCard(Rank.BIG_JOKER, Suit.JOKER, gameState),
                    new RenderableShengJiCard(Rank.BIG_JOKER, Suit.JOKER, gameState)));
        });

        gameState.players.forEach(p -> p.getPointCards().addAll(List.of(
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState))));


        gameState.friendCards.clear();
        gameState.friendCards.addAll(List.of(
                new RenderableShengJiCard(Rank.ACE, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.ACE, Suit.CLUBS, gameState),
                new RenderableShengJiCard(Rank.ACE, Suit.DIAMONDS, gameState)));

        gameState.kitty.clear();
        gameState.kitty.addAll(List.of(
                new RenderableShengJiCard(Rank.TWO, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.THREE, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.FOUR, Suit.HEARTS, gameState),
                new RenderableShengJiCard(Rank.SEVEN, Suit.DIAMONDS, gameState),
                new RenderableShengJiCard(Rank.BIG_JOKER, Suit.JOKER, gameState),
                new RenderableShengJiCard(Rank.NINE, Suit.CLUBS, gameState),
                new RenderableShengJiCard(Rank.ACE, Suit.CLUBS, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.DIAMONDS, gameState)));

        gameState.collectedPointCards.clear();
        gameState.collectedPointCards.addAll((List.of(
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState))));
        gameState.collectedPointCards.centerProportion = 0.0f;

        gameState.numCollectedPoints = 60;
        gameState.numPointsNeeded = 160;

        gameState.setTrump(Rank.FIVE, Suit.HEARTS);

        gameState.thisPlayerHand.clear();
        for(int i = 0; i < 26; i++) {
            gameState.thisPlayerHand.add(new RenderableShengJiCard(gameState));
        }

        gameState.message = "This is a message that the server sent";
        gameState.errorMessage = "[YELLOW]This is an error message oh no!";
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

        renderPlayers(viewport.getWorldWidth() * playersCenterXProportion,
                viewport.getWorldHeight() * playersCenterYProportion,
                viewport.getWorldWidth() * playersWidthRadiusProportion,
                viewport.getWorldHeight() * playersHeightRadiusProportion);

        if(renderCallKitty) {
            renderCallKitty();
        } else if(renderRoundEndKitty) {
            renderRoundEndKitty();
        }

        if(renderChoosingFriendCards) {
            renderFriendCardsForChoosing();
        } else {
            renderFriendCards();
        }

        renderTrumpCard();
        renderCollectedPointCards();
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
                    viewport.getWorldWidth() / 2,
                    gameState.thisPlayerHand.pos.y / 2,
                    Align.center);
        }
        if(!noCallButton.isDisabled()) {
            noCallButton.setWidth(gameState.friendCards.regionWidth * 0.8f);
            noCallButton.setHeight(gameState.thisPlayerHand.pos.y * 0.8f);
            noCallButton.setPosition(
                    gameState.friendCards.pos.x + (gameState.friendCards.regionWidth / 2),
                    gameState.friendCards.pos.y / 2,
                    Align.center);
        }
        uiStage.draw();
    }

    private void update(float delta) {
        handleInputs();
        updateCards(delta);
        uiStage.act(delta);
    }

    private void renderCollectedPointCards() {
        gameState.collectedPointCards.regionWidth = viewport.getWorldWidth() / 8f;
        gameState.collectedPointCards.cardHeight = viewport.getWorldHeight() / 10f;
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
        gameState.friendCards.prefDivisionProportion = 1.1f;
        gameState.friendCards.cardHeight = viewport.getWorldHeight() * 0.17f;
        gameState.friendCards.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.friendCards.cardHeight) * 5;
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

        kittyFont.draw(batch, "From Kitty",
                gameState.kitty.pos.x + (gameState.kitty.regionWidth * 0.5f),
                gameState.kitty.pos.y + (kittyFont.getXHeight() * 4) + gameState.kitty.cardHeight,
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
        gameState.friendCards.update(delta);
        gameState.kitty.update(delta);
        gameState.trumpCardGroup.update(delta);
        gameState.collectedPointCards.update(delta);
    }

    private void renderPlayers(float centerX, float centerY, float widthRadius, float heightRadius) {
        var players = gameState.players;
        float angleIncrement = MathUtils.PI2 / players.size();
        float shift = (players.indexOf(gameState.thisPlayer) * angleIncrement) + (MathUtils.PI * 0.5f);

        float[][] playerPointsXY = equidistantEllipsePoints(widthRadius, heightRadius, -shift, centerX, centerY, players.size());
        List<RenderablePlayer> expandedPlayers = new ArrayList<>();

        for(int i = 0; i < playerPointsXY.length; i++) {
            var toRender = players.get(i);
            if(toRender == null) {
                continue;
            }

            toRender.setPos(playerPointsXY[i][0], playerPointsXY[i][1]);

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

            gameState.players.forEach(p -> debugRenderer.line(centerX, centerY, p.getX(), p.getY()));

            debugRenderer.end();
            batch.begin();
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
            game.showStartScreen();
            break;

        case Input.Keys.UP:
        case Input.Keys.DOWN:
            if(gameState.lastServerCode == ServerCode.SEND_FRIEND_CARDS || gameState.lastServerCode == ServerCode.INVALID_FRIEND_CARDS || renderChoosingFriendCards) {
                if(gameState.friendCards.isEmpty()) {
                    break;
                }

                int valChange = (keycode == Input.Keys.UP) ? 1 : -1;
                var editCard = gameState.friendCards.stream()
                        .filter(c -> c.entity.isHighlighted())
                        .findFirst().orElse(gameState.friendCards.get(0));
                editCard.setCardNum(Math.floorMod(editCard.getCardNum() + valChange, Card.MAX_CARD_NUM));
            }
            break;

        case Input.Keys.LEFT:
        case Input.Keys.RIGHT:
            if(gameState.lastServerCode == ServerCode.SEND_FRIEND_CARDS || gameState.lastServerCode == ServerCode.INVALID_FRIEND_CARDS || renderChoosingFriendCards) {
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

        case Input.Keys.Q:
            renderRoundEndKitty = !renderRoundEndKitty;
            break;

        case Input.Keys.W:
            renderCallKitty = !renderCallKitty;
            break;

        case Input.Keys.E:
            renderChoosingFriendCards = !renderChoosingFriendCards;
            break;

        case Input.Keys.D:
            RenderableCardGroup.setDebug(!RenderableCardGroup.isInDebugMode());
            break;

        case Input.Keys.MINUS:
            if(gameState.players.size() > 1) {
                gameState.players.remove(gameState.players.size() - 1);
            }
            break;

        case Input.Keys.EQUALS:
            if(gameState.players.size() < 10) {
                gameState.players.add(new RenderablePlayer(gameState.players.size(), "Player " + gameState.players.size()));
                gameState.players.get(gameState.players.size() - 1).getPlay().addAll(List.of(
                        new RenderableShengJiCard(Rank.SMALL_JOKER, Suit.JOKER, gameState),
                        new RenderableShengJiCard(Rank.SMALL_JOKER, Suit.JOKER, gameState),
                        new RenderableShengJiCard(Rank.BIG_JOKER, Suit.JOKER, gameState),
                        new RenderableShengJiCard(Rank.BIG_JOKER, Suit.JOKER, gameState)));
                gameState.players.get(gameState.players.size() - 1).getPointCards().addAll(List.of(
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState),
                        new RenderableShengJiCard(Rank.KING, Suit.SPADES, gameState)));
            }
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
            break;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        var clickPos = viewport.unproject(new Vector2(screenX, screenY));
        if(button == Input.Buttons.LEFT) {
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
    public boolean mouseMoved(int screenX, int screenY) {
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
}
