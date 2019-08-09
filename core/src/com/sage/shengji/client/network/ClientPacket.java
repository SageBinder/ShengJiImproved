package com.sage.shengji.client.network;

import com.badlogic.gdx.utils.SerializationException;
import com.sage.shengji.utils.network.Packet;

import java.io.Serializable;

public class ClientPacket extends Packet<ClientCode> {
    public ClientPacket() {
        super();
    }

    public ClientPacket(ClientCode networkCode) {
        super(networkCode);
    }

    public ClientPacket put(Serializable key, Serializable value) {
        data.put(key, value);
        return this;
    }

    public static ClientPacket fromBytes(byte[] bytes) throws SerializationException {
        try {
            return (ClientPacket)Packet.fromBytes(bytes);
        } catch(ClassCastException e) {
            throw new SerializationException();
        }
    }

    public static ClientPacket pingPacket() {
        return new ClientPacket(ClientCode.PING);
    }
}
