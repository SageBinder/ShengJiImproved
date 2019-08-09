package com.sage.shengji.client.network;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.SerializationException;
import com.sage.shengji.client.ShengJiGame;
import com.sage.shengji.server.network.ServerPacket;

import java.io.*;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection extends Thread {
    public final int port;
    public final String serverIP;
    private final ShengJiGame game;
    private final String playerName;

    private final Socket socket;
    private final DataOutputStream output;
    private final DataInputStream input;

    private final Queue<ServerPacket> packetQueue = new LinkedBlockingQueue<>();

    private volatile boolean quit = false;

    public ClientConnection(String serverIP, int port, String playerName, ShengJiGame game) {
        this.port = port;
        this.serverIP = serverIP;
        this.game = game;
        this.playerName = playerName;

        SocketHints socketHints = new SocketHints();
        socketHints.socketTimeout = 0;
        socket = new NetJavaSocketImpl(Net.Protocol.TCP, serverIP, port, socketHints);
        output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        setDaemon(true);
        Runtime.getRuntime().addShutdownHook(new Thread(socket::dispose));
    }

    @Override
    public void run() {
        while(!quit) {
            ServerPacket packet;
            try {
                int packetSize = input.readInt();
                packet = ServerPacket.fromBytes(input.readNBytes(packetSize));
                packetQueue.add(packet);
            } catch(IOException e) {
                quit();
                packetQueue.add(new LostConnectionToServerQueueItem());
                break;
            } catch(SerializationException | IllegalArgumentException | OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
    }

    public void quit() {
        quit = true;
        socket.dispose();
    }

    public void sendPacket(ClientPacket packet) throws IOException {
        byte[] packetBytes = packet.toBytes();
        output.writeInt(packetBytes.length);
        output.write(packetBytes);
        output.flush();
    }

    public void sendCode(ClientCode code) throws IOException {
        sendPacket(new ClientPacket(code));
    }

    public Optional<ServerPacket> getPacket() throws LostConnectionToServerException {
        synchronized(packetQueue) {
            ServerPacket item = packetQueue.poll();
            if(item instanceof LostConnectionToServerQueueItem) {
                throw new LostConnectionToServerException();
            } else {
                return Optional.ofNullable(item);
            }
        }
    }

    public boolean pingServer() {
        try {
            sendPacket(ClientPacket.pingPacket());
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    private class LostConnectionToServerQueueItem extends ServerPacket {

    }
}
