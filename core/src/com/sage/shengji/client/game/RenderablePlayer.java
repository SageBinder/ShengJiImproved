package com.sage.shengji.client.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sage.shengji.utils.card.CardList;
import com.sage.shengji.utils.card.Rank;
import com.sage.shengji.utils.shengji.ShengJiCard;
import com.sage.shengji.utils.renderable.Renderable;
import com.sage.shengji.utils.renderable.RenderableCardEntity;
import com.sage.shengji.utils.renderable.RenderableCardGroup;
import com.sage.shengji.utils.shengji.Team;
import net.dermetfan.gdx.math.MathUtils;

import java.util.Arrays;

public class RenderablePlayer implements Renderable {
    private static BitmapFont nameFont;
    private static FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/OpenSans-Regular.ttf"));
    static {
        var fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParameter.size = (int)(Math.max(Gdx.graphics.getHeight(), Gdx.graphics.getWidth()) * (1f / 7f));
        fontParameter.incremental = true;
        nameFont = fontGenerator.generateFont(fontParameter);
        nameFont.getData().markupEnabled = true;
    }

    private String colorString = "";
    private boolean isExpanded = false;

    private final RenderableCardGroup<RenderableShengJiCard> pointCards = new RenderableCardGroup<>();
    private final RenderableCardGroup<RenderableShengJiCard> play = new RenderableCardGroup<>();

    private Vector2 pos = new Vector2();
    private float heightProportion = 0.11f;
    private float expandedHeightProportion = 0.22f;

    private int playerNum;
    private boolean isHost = false;
    private boolean isClientPlayer = false;
    private String name;

    private Rank callRank = Rank.TWO;
    private Team team = Team.NO_TEAM;

    public RenderablePlayer(int playerNum, String name) {
        this.playerNum = playerNum;
        this.name = name;
    }

    @Override
    public void render(SpriteBatch batch, Viewport viewport) {
        nameFont.draw(batch, "P" + playerNum + ": " + colorString + getName(),
                pos.x, pos.y + (nameFont.getXHeight() * 2),
                0, Align.center, false);

        float playTargetHeight = viewport.getWorldHeight() * ((isExpanded) ? expandedHeightProportion : heightProportion);
        float playTargetY = pos.y - playTargetHeight;
        float playTargetWidth = ((play.prefDivisionProportion * 3) * playTargetHeight * RenderableCardEntity.WIDTH_TO_HEIGHT_RATIO);
        float playTargetX = pos.x - (playTargetWidth * 0.5f);

        play.cardHeight = playTargetHeight;
        play.regionWidth = playTargetWidth;
        play.pos.x = MathUtils.clamp(playTargetX, 0, viewport.getWorldWidth() - play.regionWidth);
        play.pos.y = MathUtils.clamp(playTargetY, 0, viewport.getWorldHeight() - play.cardHeight);
        play.prefDivisionProportion = 0.9f;

        pointCards.cardHeight = playTargetHeight * 0.66f;
        pointCards.regionWidth = MathUtils.clamp(playTargetWidth, 0, viewport.getWorldWidth());
        pointCards.pos.x = MathUtils.clamp((playTargetX + (playTargetWidth * 0.5f)) - (pointCards.regionWidth * 0.5f),
                0,
                viewport.getWorldWidth() - pointCards.regionWidth);
        pointCards.pos.y = MathUtils.clamp(playTargetY - (pointCards.cardHeight * 1.05f),
                0,
                viewport.getWorldHeight() - pointCards.cardHeight);
        pointCards.prefDivisionProportion = 1.1f;

        pointCards.forEach(c -> {
            c.entity.mover.sizeSpeed = 8;
            c.entity.mover.posSpeed = 8;
        });
        play.forEach(c -> {
            c.entity.mover.sizeSpeed = 8;
            c.entity.mover.posSpeed = 8;
        });
        play.render(batch, viewport);
        pointCards.render(batch, viewport);
    }

    public void update(float delta) {
        play.update(delta);
        pointCards.update(delta);
    }

    public static void setNameFont(BitmapFont font) {
        nameFont.dispose();
        nameFont = font;
    }

    public void setNameColor(Color nameColor) {
        if(nameColor != null) {
            System.out.println("Received color " + nameColor.toString());
            colorString = "[#"
                    + nameColor.toString().substring(0, 2)
                    + nameColor.toString().substring(2, 4)
                    + nameColor.toString().substring(4, 6)
                    + nameColor.toString().substring(6, 8)
                    + "]";
            System.out.println("colorString = " + colorString);
        } else {
            colorString = "";
        }
    }

    public void clearNameColor() {
        colorString = "";
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public String getName() {
        return name;
    }

    public String getColoredName() {
        return colorString + name + "[]";
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rank getCallRank() {
        return callRank;
    }

    public synchronized void setCallRank(Rank callRank) {
        this.callRank = callRank;
    }

    public synchronized void increaseCallRank(int amount) {
        int currRankIdx = Math.max(Arrays.asList(Arrays.copyOfRange(Rank.values(), 0, 14)).indexOf(callRank), 0);
        setCallRank(Rank.values()[(currRankIdx + amount) % 14]);
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public int getTotalPoints() {
        return pointCards.stream().mapToInt(ShengJiCard::getPoints).sum();
    }

    public RenderableCardGroup<RenderableShengJiCard> getPlay() {
        return play;
    }

    public void setPlay(CardList<RenderableShengJiCard> play) {
        this.play.clear();
        if(play != null) {
            this.play.addAll(play);
        }
    }

    public void clearPlay() {
        play.clear();
    }

    public void clearCards() {
        pointCards.clear();
        play.clear();
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean isHost) {
        this.isHost = isHost;
    }

    public boolean isClientPlayer() {
        return isClientPlayer;
    }

    public void setIsClientPlayer(boolean isClientPlayer) {
        this.isClientPlayer = isClientPlayer;
    }

    public void setX(float x) {
        pos.x = x;
    }

    public void setY(float y) {
        pos.y = y;
    }

    public void setPos(float x, float y) {
        setX(x);
        setY(y);
    }

    public void setPos(Vector2 pos) {
        setX(pos.x);
        setY(pos.y);
    }

    public float getX() {
        return pos.x;
    }

    public float getY() {
        return pos.y;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }

    public RenderableCardGroup<RenderableShengJiCard> getPointCards() {
        return pointCards;
    }
}
