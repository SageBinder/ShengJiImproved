package com.sage.shengji.server.network;

import com.badlogic.gdx.utils.SerializationException;
import com.sage.shengji.utils.network.Packet;

import java.io.Serializable;

public class ServerPacket extends Packet<ServerCode> {
    public ServerPacket() {
        super();
    }

    public ServerPacket(ServerCode networkCode) {
        super(networkCode);
    }

    public ServerPacket put(Serializable key, Serializable value) {
        data.put(key, value);
        return this;
    }

    public static ServerPacket fromBytes(byte[] bytes) throws SerializationException {
        try {
            return (ServerPacket)Packet.fromBytes(bytes);
        } catch(ClassCastException e) {
            throw new SerializationException();
        }
    }

    public static ServerPacket pingPacket() {
        return new ServerPacket(ServerCode.PING);
    }
}
