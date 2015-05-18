package org.ham;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttClient;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by dante on 18.05.15.
 */
public class JsonMessages {
    private static final String TYPE_TRIGGER = "Trigger";
    private static final String TYPE = "type";
    private static final String ASPECT = "aspect";
    private static final String TYPE_SWITCH = "Switch";
    private static final String STATE = "State";
    private static final String ON = "on";
    private static final String OFF = "off";
    private ObjectMapper objectMapper;

    public JsonMessages(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public byte[] trigger(String aspect) {
        HashMap<Object, Object> map = createBaseMessage(TYPE_TRIGGER);
        map.put(ASPECT, aspect);
        try {
            return objectMapper.writeValueAsString(map).getBytes("UTF-8");
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private HashMap<Object, Object> createBaseMessage(String type) {
        HashMap<Object, Object> map = new HashMap<>();
        map.put(TYPE, type);
        return map;
    }

    public byte[] switchMessage(String aspect, boolean on) {
        HashMap<Object, Object> map = createBaseMessage(TYPE_SWITCH);
        map.put(ASPECT, aspect);
        map.put(STATE, on ? ON : OFF);
        try {
            return objectMapper.writeValueAsString(map).getBytes("UTF-8");
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
