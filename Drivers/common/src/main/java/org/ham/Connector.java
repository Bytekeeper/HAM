package org.ham;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Created by dante on 18.05.15.
 */
public class Connector {
    private static final Logger LOG = Logger.getLogger(Connector.class.getName());

    public void startFromCommandline(String[] args, String clientId, Driver driver) throws IOException {
        LOG.info("Starting in standalone mode, this is recommended for testing purposes");
        String brokerURI = args.length > 0 ? "tcp://" + args[0] : "tcp://localhost:1883";
        MqttClient mqttClient = null;
        try {
            mqttClient = new MqttClient(brokerURI, clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(1000);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            LOG.info("Connecting to broker: " + brokerURI);
            mqttClient.connect(connOpts);
            LOG.info("Connected");
            LOG.info("Setting up " + clientId);
            driver.setup(mqttClient);
            LOG.info("Starting " + clientId);
            driver.start();
            LOG.info("Started");
            LOG.info("Press enter to exit");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                in.readLine();
            }
            LOG.info("Exiting");
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
            if (mqttClient != null) {
                try {
                    mqttClient.disconnectForcibly(1000);
                    mqttClient.close();
                } catch (MqttException e) {
                    throw new IOException(e);
                }
            }
        }

    }
}
