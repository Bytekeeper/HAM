import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Created by dante on 05.05.15.
 */
public class Driver implements MqttCallback {
    private static final Logger LOG = Logger.getLogger(Driver.class.getName());
    public static final String COLOR = "Color";
    public static final String TRIGGER = "Trigger";
    public static final String DURATION = "Duration";

    String topic = "MQTT Examples";
    String rawInTopic = "mqttRf24Bridge/1/sensor";
    String rawOutTopic = "mqttRf24Bridge/1/actor";
    String driverBaseTopic = "bedroom/bedlight/";
    private MqttClient mqttClient;
    private Settings currentSettings = new Settings();

    public static final byte TYPE_TRIGGER = 1;
    public static final byte TYPE_SWITCH = 2;
    public static final byte TYPE_SETTINGS = (byte) 200;

    public Driver(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        String broker = "tcp://192.168.0.18:1883";
        String clientId = "BedlightDriver";

        try {
            final MqttClient mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(1000);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            LOG.info("Connecting to broker: " + broker);
            mqttClient.connect(connOpts);
            LOG.info("Connected");
            Driver driver = new Driver(mqttClient);
            mqttClient.setCallback(driver);
            driver.start();
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
        }

    }

    public void start() throws MqttException {
        mqttClient.subscribe(rawInTopic);
        mqttClient.subscribe(getLightTopicOut() + "/set");
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        LOG.info("Received message on topic " + s);
        if (s.startsWith(driverBaseTopic) && s.endsWith("/in")) {
            LOG.info("Handling message " + mqttMessage);
            ObjectMapper mapper = new ObjectMapper();

            try {
                JsonNode root = mapper.readTree(mqttMessage.getPayload());
                LOG.info("Message: " + root);
                String type = root.path("type").asText();
                switch (type) {
                    case COLOR:
                        currentSettings.r = (short) (root.path("red").asInt() * 1023 / 65536f);
                        currentSettings.g = (short) (root.path("green").asInt() * 1023 / 65536f);
                        currentSettings.b = (short) (root.path("blue").asInt() * 1023 / 65536f);
                        sendSettings();
                        break;
                    case DURATION:
                        String timer = root.path("timer").asText();
                        double seconds = root.path("seconds").asDouble();
                        short centiSeconds = (short) (seconds * 100);
                        switch (timer) {
                            case "fadeInDuration":
                                currentSettings.fadeInCentiSecs = centiSeconds;
                                break;
                            case "fadeOutDuration":
                                currentSettings.fadeOutCentiSecs = centiSeconds;
                                break;
                            case "activeDuration":
                                currentSettings.lightCentiSecs = centiSeconds;
                                break;
                            default:
                                LOG.severe("Invalid timer: " + timer);
                        }
                        sendSettings();
                        break;
                    case TRIGGER:
                        sendMessageToHW(buf -> {
                            buf.put(TYPE_TRIGGER);
                        });
                        break;
                    default:
                        LOG.severe("Unhandled type: " + type);
                }
            } catch (IOException e) {
                LOG.throwing("Driver", "messageArrived", e);
            } catch (MqttException e) {
                LOG.throwing("Driver", "messageArrived", e);
            }
        } else if (rawInTopic.equals(s)) {
            LOG.info("Handling Arduino message type " + mqttMessage.getPayload()[0]);
            ByteBuffer byteBuffer = ByteBuffer.wrap(mqttMessage.getPayload());
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            dispatchRaw(byteBuffer);
        } else {
            LOG.severe("Unhandled: " + s + " " + mqttMessage);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private void dispatchRaw(ByteBuffer msg) throws JsonProcessingException, UnsupportedEncodingException, MqttException {
        byte sensorId;
        byte type = msg.get();
        HashMap<String, Object> map = new HashMap<>();
        String result;
        String topic;
        ObjectMapper mapper = new ObjectMapper();
        switch (type) {
            case TYPE_TRIGGER:
                sensorId = msg.get();
                map.put("type", "Trigger");
                topic = getMotionSensorTopicOut(sensorId);
                break;
            case TYPE_SWITCH:
                sensorId = msg.get();
                byte state = msg.get();
                map.put("type", "Switch");
                map.put("state", state == 0 ? "off" : "on");
                topic = getLightTopicOut();
                break;
            default:
                LOG.severe("Unhandled raw type:" + type);
                return;
        }
        result = mapper.writeValueAsString(map);
        mqttClient.publish(topic, result.getBytes("UTF8"), 2, false);
    }

    private String getMotionSensorTopicOut(byte sensorId) {
        return driverBaseTopic + "motion" + sensorId + "/out";
    }

    private String getLightTopicOut() {
        return driverBaseTopic + "light/out";
    }

    private void sendSettings() throws MqttException {
        sendMessageToHW(buffer -> {
            buffer.put(TYPE_SETTINGS);
            buffer.putShort(currentSettings.r);
            buffer.putShort(currentSettings.g);
            buffer.putShort(currentSettings.b);
            buffer.putShort(currentSettings.fadeInCentiSecs);
            buffer.putShort(currentSettings.fadeOutCentiSecs);
            buffer.putShort(currentSettings.lightCentiSecs);
        });
    }

    private void sendMessageToHW(Consumer<ByteBuffer> messagePreparer) throws MqttException {
        byte[] array = new byte[20];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        messagePreparer.accept(buffer);
        mqttClient.publish(rawOutTopic, array, 2, false);
        LOG.info("Sent message with type " + buffer.get(0));
    }

    public static class Settings {
        protected short r = 300;
        protected short g;
        protected short b;
        protected short fadeInCentiSecs = 100;
        protected short fadeOutCentiSecs = 500;
        protected short lightCentiSecs = 1500;
    }
}
