package com.sage.shengji.server.game;

import com.badlogic.gdx.utils.SerializationException;
import com.sage.shengji.server.network.MultiplePlayersDisconnectedException;
import com.sage.shengji.server.network.PlayerDisconnectedException;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.server.network.ServerPacket;
import com.sage.shengji.utils.card.Rank;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PlayerList extends ArrayList<Player> {
    public PlayerList() {
        super();
    }

    public PlayerList(Collection<? extends Player> other) {
        super(other);
    }

    public void sendPacketToAll(ServerPacket packet) throws MultiplePlayersDisconnectedException {
        PlayerList disconnectedPlayers = null;
        for(Player p : this) {
            try {
                p.sendPacket(packet);
            } catch(PlayerDisconnectedException e) {
                if(disconnectedPlayers == null) {
                    disconnectedPlayers = new PlayerList();
                }
                disconnectedPlayers.add(p);
            } catch(SerializationException e) {
                e.printStackTrace();
            }
        }
        if(disconnectedPlayers != null) {
            throw new MultiplePlayersDisconnectedException(disconnectedPlayers);
        }
    }

    public void sendPacketToAllExcluding(ServerPacket packet, Player... excluded) throws MultiplePlayersDisconnectedException {
        PlayerList newPlayerList = new PlayerList(this);
        newPlayerList.removeAll(List.of(excluded));
        newPlayerList.sendPacketToAll(packet);
    }

    public void sendCodeToAll(ServerCode code) throws MultiplePlayersDisconnectedException {
        sendPacketToAll(new ServerPacket(code));
    }

    public void sendCodeToAllExcluding(ServerCode code, Player... excluded) throws MultiplePlayersDisconnectedException {
        sendPacketToAllExcluding(new ServerPacket(code), excluded);
    }

    public void sendPlayersToAll() throws MultiplePlayersDisconnectedException {
        if(isEmpty()) {
            return;
        }

        HashMap<Integer, String> playersMap =
                stream().collect(Collectors.toMap(Player::getPlayerNum, Player::getName, (a, b) -> b, HashMap::new));
        HashMap<Integer, Rank> callRankMap =
                stream().collect(Collectors.toMap(Player::getPlayerNum, Player::getCallRank, (a, b) -> b, HashMap::new));
        Integer hostNum = stream().filter(Player::isHost).findAny().orElse(this.get(0)).getPlayerNum();

        // Pretty much copy/pasted code from sendPacketToAll()
        PlayerList disconnectedPlayers = null;
        for(Player p : this) {
            ServerPacket playersPacket = new ServerPacket(ServerCode.WAIT_FOR_PLAYERS)
                    .put("players", playersMap)
                    .put("rank", callRankMap)
                    .put("host", hostNum)
                    .put("you", p.getPlayerNum());
            try {
                p.sendPacket(playersPacket);
            } catch(PlayerDisconnectedException e) {
                if(disconnectedPlayers == null) {
                    disconnectedPlayers = new PlayerList();
                }
                disconnectedPlayers.add(p);
            } catch(SerializationException e) {
                e.printStackTrace();
            }
        }
        if(disconnectedPlayers != null) {
            throw new MultiplePlayersDisconnectedException(disconnectedPlayers);
        }
    }

    public Optional<Player> getByPlayerNum(int playerNum) {
        for(Player p : this) {
            if(p.getPlayerNum() == playerNum) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public void squashPlayerNums() {
        IntStream.range(0, size()).forEach(i -> get(i).setPlayerNum(i));
    }
}
