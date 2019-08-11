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
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.card.Suit;
import com.sage.shengji.utils.renderable.RenderableCardEntity;
import com.sage.shengji.utils.renderable.RenderableCardGroup;
import com.sage.shengji.utils.shengji.ShengJiCard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UiBuilderScreen extends InputAdapter implements Screen {
    private ClientGameState gameState;
    private ShengJiGame game;

    private ShapeRenderer debugRenderer = new ShapeRenderer();

    private SpriteBatch batch = new SpriteBatch();
    private Viewport viewport;
    private float viewportScale = 5f;
    private float textProportion = 1 / 7f;
    private float originalTextSize = 0;

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
    private BitmapFont playerNameFont;

    private float handHeightProportion = 1f / 7f;
    private float expandedHandHeightProportion = 1f / 5f;

    private float playersCenterXProportion = 0.55f;
    private float playersCenterYProportion = 0.70f;

    private float playersWidthRadiusProportion = 0.36f;
    private float playersHeightRadiusProportion = 0.19f;

    private boolean renderChoosingFriendCards = false;
    private boolean renderRoundEndKitty = false;
    private boolean renderCallKitty = false;

    private RenderableCardGroup<RenderableShengJiCard> trumpCardGroup = new RenderableCardGroup<>();

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
        originalTextSize = textSize;

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

        var playerNameFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        playerNameFontParameter.size = textSize;
        playerNameFontParameter.incremental = true;
        playerNameFont = fontGenerator.generateFont(playerNameFontParameter);
        RenderablePlayer.setNameFont(playerNameFont);

    }

    private void uiSetup() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        var actionButtonStyle = skin.get(TextButton.TextButtonStyle.class);
        actionButtonStyle.font = fontGenerator.generateFont(textButtonFontParameter);

        // Creating UI elements:
        actionButton = new TextButton("Send play", actionButtonStyle);
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
        inputProcessorsSetup();
        RenderableCardGroup.debug();
        gameState.thisPlayerHand.prefDivisionProportion = 0.4f;

        gameState.players.clear();
        gameState.players.add(new RenderablePlayer(0, "This player"));
        gameState.thisPlayer = gameState.players.get(0);
        for(int i = 1; i < 8; i++) {
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

        gameState.setTrump(Rank.FIVE, Suit.HEARTS);
        trumpCardGroup.clear();
        trumpCardGroup.add(gameState.trumpCard);

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
                viewport.getWorldHeight() * playersCenterYProportion - errorFont.getCapHeight() * 3,
                0, Align.center, false);
        messageFont.draw(batch, gameState.message,
                viewport.getWorldWidth() * playersCenterXProportion,
                viewport.getWorldHeight() * playersCenterYProportion - errorFont.getCapHeight(),
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
        gameState.thisPlayerHand.leftPaddingProportion = 0.15f;
        gameState.thisPlayerHand.rightPaddingProportion = 0.15f;
        gameState.thisPlayerHand.bottomPaddingProportion = 0.09f;
        gameState.thisPlayerHand.render(batch, viewport);
        quitConfirmationFont.draw(batch, quitConfirmationText,
                viewport.getWorldWidth() / 2,
                viewport.getWorldHeight() - quitConfirmationFont.getCapHeight(),
                0, Align.center, false);
        batch.end();

        actionButton.setWidth(viewport.getWorldWidth() * 0.2f);
        actionButton.setHeight(gameState.thisPlayerHand.pos.y * 0.9f);
        actionButton.setPosition(
                viewport.getWorldWidth() / 2,
                gameState.thisPlayerHand.pos.y / 2,
                Align.center);
        uiStage.draw();
    }

    private void update(float delta) {
        handleInputs();
        updateCards(delta);
        uiStage.act(delta);
    }

    private void renderCollectedPointCards() {
        gameState.collectedPointCards.regionWidth = gameState.players.get(0).getPlay().regionWidth * 1.5f;
        gameState.collectedPointCards.cardHeight = gameState.players.get(0).getPlay().cardHeight * 1.2f;
        gameState.collectedPointCards.setPos(
                viewport.getWorldWidth() * 0.05f,
                viewport.getWorldHeight() * 0.8f);
        gameState.collectedPointCards.render(batch, viewport);

        collectedPointCardsFont.draw(batch,
                gameState.numCollectedPoints + "/" + gameState.numPointsNeeded + " points",
                gameState.collectedPointCards.pos.x + (gameState.collectedPointCards.regionWidth * 0.5f),
                gameState.collectedPointCards.pos.y - (collectedPointCardsFont.getXHeight() * 2),
                0, Align.center, false);
    }

    private void renderFriendCardsForChoosing() {
        gameState.friendCards.cardHeight = viewport.getWorldHeight() * 0.17f;
        gameState.friendCards.prefDivisionProportion = 1.1f;

        gameState.friendCards.regionWidth =
                (RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO * gameState.friendCards.cardHeight) * 4;

        gameState.friendCards.setPos(
                (viewport.getWorldWidth() - gameState.friendCards.regionWidth) * 0.5f,
                gameState.thisPlayerHand.pos.y + (gameState.thisPlayerHand.cardHeight * 2)
        );

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
        trumpCardGroup.cardHeight = gameState.thisPlayerHand.cardHeight;
        trumpCardGroup.regionWidth = gameState.thisPlayerHand.pos.x;
        trumpCardGroup.setPos(0, gameState.thisPlayerHand.pos.y);
        trumpCardGroup.render(batch, viewport);

        trumpCardFont.setColor(Color.GOLD);
        trumpCardFont.draw(batch, "Trump",
                trumpCardGroup.pos.x + (trumpCardGroup.regionWidth * 0.5f),
                trumpCardGroup.pos.y + (trumpCardFont.getXHeight() * 2) + trumpCardGroup.cardHeight,
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
        gameState.trumpCard.update(delta);
        gameState.collectedPointCards.update(delta);
    }

    private void renderPlayers(float centerX, float centerY, float widthRadius, float heightRadius) {
        var players = gameState.players;
        float angleIncrement = MathUtils.PI2 / players.size();
        float shift = (players.indexOf(gameState.thisPlayer) * angleIncrement) + (MathUtils.PI * 0.5f);

        Map<Integer, RenderablePlayer> expandedPlayers = new HashMap<>();


        for(int i = 0; i < players.size(); i++) {
            RenderablePlayer toRender = players.get(i);
            if(toRender == null) {
                continue;
            } else if(toRender.isExpanded()) {
                expandedPlayers.put(i, toRender);
                continue;
            }

            toRender.setX((MathUtils.cos((i * angleIncrement) - shift) * widthRadius) + centerX);
            toRender.setY((MathUtils.sin((i * angleIncrement) - shift) * heightRadius) + centerY);
            toRender.render(batch, viewport);
        }

        for(var i : expandedPlayers.keySet()) {
            RenderablePlayer toRender = expandedPlayers.get(i);
            float playerX = (MathUtils.cos((i * angleIncrement) - shift) * widthRadius) + centerX;
            float playerY = (MathUtils.sin((i * angleIncrement) - shift) * heightRadius) + centerY;
            toRender.setX(playerX);
            toRender.setY(playerY);
            toRender.render(batch, viewport);
        }

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
            break;

        case Input.Keys.SPACE:
            gameState.thisPlayerHand.stream()
                    .filter(c -> c.entity.isHighlighted())
                    .findAny().ifPresent(c -> c.entity.toggleSelected());
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
}
