package com.sage.shengji.utils.network;

import com.badlogic.gdx.utils.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class Packet<T extends NetworkCode> implements Serializable {
    public T networkCode;
    public final Map<Serializable, Serializable> data = new HashMap<>();

    public Packet() {
    }

    public Packet(T networkCode) {
        this.networkCode = networkCode;
    }

    public Serializable get(Serializable key) {
        return data.get(key);
    }

    public byte[] toBytes() throws SerializationException {
        return SerializationUtils.serialize(this);
    }

    public static Packet fromBytes(byte[] bytes) throws SerializationException {
        try {
            return SerializationUtils.deserialize(bytes);
        } catch(ClassCastException | IllegalArgumentException e) {
            throw new SerializationException("Could not deserialize, caused by " + e.getClass().toString());
        }
    }
}
