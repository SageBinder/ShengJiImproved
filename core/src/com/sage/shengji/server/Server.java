package com.sage.shengji.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaServerSocketImpl;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.SerializationException;
import com.sage.shengji.client.network.ClientCode;
import com.sage.shengji.server.game.*;
import com.sage.shengji.server.network.MultiplePlayersDisconnectedException;
import com.sage.shengji.server.network.PlayerDisconnectedException;
import com.sage.shengji.server.network.ServerCode;
import com.sage.shengji.server.network.ServerPacket;

import java.util.Timer;
import java.util.TimerTask;

public class Server extends Thread {
    private static final long PRUNE_PERIOD = 1000; // In seconds
    public static final int MAX_PLAYER_NAME_LENGTH = 16;

    public final int port;

    private final ServerGameState gameState = new ServerGameState();

    private volatile boolean startRoundFlag = false; // This flag is set by the player communication thread
    private final Object startRoundObj = new Object();

    private Player host = null;
    private NetJavaServerSocketImpl serverSocket;

    private volatile boolean closed = false;

    public Server(int port) {
        this.port = port;

        ServerSocketHints hints = new ServerSocketHints();
        hints.acceptTimeout = 0;
        serverSocket = new NetJavaServerSocketImpl(Net.Protocol.TCP, port, hints);

        setDaemon(true);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public void run() {
        Thread connectionAcceptorThread = new Thread(connectionAcceptor);
        connectionAcceptorThread.start();

        Timer pruneDisconnectedPlayersTimer = new Timer();
        pruneDisconnectedPlayersTimer.scheduleAtFixedRate(pruneDisconnectedPlayersTask, PRUNE_PERIOD, PRUNE_PERIOD);

        while(!closed) {
            if(startRoundFlag) {
                if(closed) {
                    break;
                } else {
                    playRound();
                }
            }
            try {
                synchronized(startRoundObj) {
                    startRoundObj.wait();
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            serverSocket.dispose();
        } catch(GdxRuntimeException e) {
            Gdx.app.log("Server.run()", "Encountered GdxRuntimeException when trying to dispose of socket");
        }
        pruneDisconnectedPlayersTimer.cancel();
    }

    private void playRound() {
        try {
            RoundRunner.playRound(gameState);
        } catch(PlayerDisconnectedException | MultiplePlayersDisconnectedException e) {
            gameState.removeDisconnectedPlayers();
            try {
                gameState.getPlayers().sendPacketToAll(new ServerPacket(ServerCode.PLAYER_DISCONNECTED));
            } catch(MultiplePlayersDisconnectedException e1) {
                gameState.removeDisconnectedPlayers();
            }
        } catch(RoundStartFailedException e) {
            host.sendPacket(new ServerPacket(ServerCode.COULD_NOT_START_GAME));
        } catch(FatalRoundException e) {
            gameState.getPlayers().sendCodeToAll(ServerCode.FATAL_ROUND_ERROR);
        } finally {
            sendPlayersToAllUntilNoDisconnections();
            startRoundFlag = false;
        }
    }

    private void sendPlayersToAllUntilNoDisconnections() {
        while(true) {
            try {
                gameState.getPlayers().sendPlayersToAll();
            } catch(MultiplePlayersDisconnectedException e1) {
                if(gameState.removeDisconnectedPlayers()) {
                    continue;
                }
            }
            break;
        }
    }

    private void setInitialPacketHandlersForPlayer(Player player) {
        player.setInitialPacketHandlerForCode(ClientCode.NAME, packet -> {
            if(packet.data.get("name") instanceof String && !gameState.isRoundRunning()) {
                String sentName = (String)packet.data.get("name");
                player.setName(sentName.substring(0, Math.min(sentName.length(), MAX_PLAYER_NAME_LENGTH)));
                sendPlayersToAllUntilNoDisconnections();
            } else {
                player.sendPacket(new ServerPacket(ServerCode.UNSUCCESSFUL_NAME_CHANGE));
            }
            return false; // This packet does not need to be put into the player's packetQueue
        });

        player.setInitialPacketHandlerForCode(ClientCode.START_GAME, packet -> {
            if(player == host && !startRoundFlag) {
                startRoundFlag = true; // This simply requests the round runner thread to start; it does not force the round to start
                synchronized(startRoundObj) {
                    startRoundObj.notify();
                }
            } else {
                player.sendPacket(new ServerPacket(ServerCode.COULD_NOT_START_GAME));
            }
            return false; // This packet does not need to be put into the player's packetQueue
        });

        player.setInitialPacketHandlerForCode(ClientCode.PLAYER_RANK_CHANGE, packet -> {
            if(packet.data.get("player") instanceof Integer
                    && packet.data.get("rankchange") instanceof Integer
                    && player == host) {
                gameState.getPlayers().getByPlayerNum((Integer)packet.data.get("player")).ifPresent(p -> {
                    p.incrementCallRankOffset((Integer)packet.data.get("rankchange"));
                    ServerPacket newPlayerRankPacket = new ServerPacket(ServerCode.NEW_PLAYER_RANK)
                            .put("player", p.getPlayerNum())
                            .put("rank", p.getCallRank());
                    sendPacketToAllAndHandleDisconnections(newPlayerRankPacket);
                });
            }
            return false;
        });

        player.setInitialPacketHandlerForCode(ClientCode.RESET_PLAYER_RANK, packet -> {
            if(packet.data.get("player") instanceof Integer && player == host) {
                gameState.getPlayers().getByPlayerNum((Integer)packet.data.get("player")).ifPresent(p -> {
                    p.setCallRankOffset(0);
                    ServerPacket newPlayerRankPacket = new ServerPacket(ServerCode.NEW_PLAYER_RANK)
                            .put("player", p.getPlayerNum())
                            .put("rank", p.getCallRank());
                    sendPacketToAllAndHandleDisconnections(newPlayerRankPacket);
                });
            }
            return false;
        });

        player.setInitialPacketHandlerForCode(ClientCode.SHUFFLE_PLAYERS, packet -> {
            if(player == host) {
                try {
                    gameState.shufflePlayers();
                } catch(RoundIsRunningException e) {
                    return false;
                }
                gameState.squashPlayerNums();
                sendPlayersToAllUntilNoDisconnections();
            }
            return false;
        });

        player.setInitialPacketHandlerForCode(ClientCode.PING, packet -> false);
    }

    private void sendPacketToAllAndHandleDisconnections(ServerPacket packet) {
        try {
            gameState.getPlayers().sendPacketToAll(packet);
        } catch(MultiplePlayersDisconnectedException e) {
            gameState.removeDisconnectedPlayers();
            sendPlayersToAllUntilNoDisconnections();
        }
    }
    
    public void close() {
        try {
            // We drop every player connection which should (?) make RoundRunner.playRound() throw a PlayerDisconnectedException.
            // When the main server loop repeats, it will query the value of closed and will exit.
            gameState.getPlayers().forEach(Player::dropConnection);
            closed = true;

            // We need to notify startRoundObj in case the server is currently waiting for the round to start
            synchronized(startRoundObj) {
                startRoundObj.notify();
            }
        } finally {
            try { // No matter what, serverSocket should be disposed
                serverSocket.dispose();
            } catch(GdxRuntimeException e) {
                Gdx.app.log("Server.close()", "Encountered GdxRuntimeException when trying to dispose of socket");
            }
        }
    }

    Runnable connectionAcceptor = new Runnable() {
        @Override
        public void run() {
            while(!closed) {
                Player newPlayer;
                try {
                    newPlayer = new Player(gameState.getPlayers().size(), serverSocket.accept(null));

                    // If round is started, we can immediately end the connection
                    if(gameState.isRoundRunning() || gameState.getPlayers().size() == ServerGameState.MAX_PLAYERS) {
                        newPlayer.sendPacket(new ServerPacket(ServerCode.CONNECTION_DENIED));
                    }
                } catch(GdxRuntimeException | SerializationException | PlayerDisconnectedException e) {
                    continue;
                }

                if(host == null) {
                    host = newPlayer;
                    host.setHost(true);
                }
                newPlayer.setPlayerNum(gameState.getPlayers().size());
                setInitialPacketHandlersForPlayer(newPlayer);
                try {
                    gameState.addPlayer(newPlayer);
                    newPlayer.sendPacket(new ServerPacket(ServerCode.CONNECTION_ACCEPTED));
                } catch(RoundIsRunningException e) {
                    // If gameState.addPlayer throws a RoundIsRunningException, the new player will not be added
                    newPlayer.sendPacket(new ServerPacket(ServerCode.CONNECTION_DENIED));
                } catch(SerializationException | PlayerDisconnectedException e) {
                    // If a PlayerDisconnectedException is encountered here, the call to
                    // sendPlayersToAllUntilNoDisconnections will remove newPlayer from the player list
                }
                sendPlayersToAllUntilNoDisconnections();
            }
        }
    };

    private TimerTask pruneDisconnectedPlayersTask = new TimerTask() {
        @Override
        public void run() {
            if(!gameState.isRoundRunning()) {
                if(gameState.removeDisconnectedPlayers()) {
                    sendPlayersToAllUntilNoDisconnections();
                }
            }
        }
    };
}
