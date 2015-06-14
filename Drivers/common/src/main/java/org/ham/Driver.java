package org.ham;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Created by dante on 18.05.15.
 */
public interface Driver {
    void setup(MqttClient mqttClient);

    void start() throws MqttException;

    void stop();
}
