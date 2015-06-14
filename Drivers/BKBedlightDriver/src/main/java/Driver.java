import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.ham.Connector;
import org.ham.JsonMessages;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Created by dante on 05.05.15.
 */
public class Driver implements MqttCallback, org.ham.Driver {
    private static final Logger LOG = Logger.getLogger(Driver.class.getName());
    public static final String COLOR = "Color";
    public static final String TRIGGER = "Trigger";
    public static final String DURATION = "Duration";

    String rawInTopic = "mqttRf24Bridge/1/sensor";
    String rawOutTopic = "mqttRf24Bridge/1/actor";
    String driverBaseTopic = "bedroom/bedlight/";
    private MqttClient mqttClient;
    private Settings currentSettings = new Settings();
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonMessages jsonMessages = new JsonMessages(objectMapper);

    public static final byte TYPE_TRIGGER = 1;
    public static final byte TYPE_SWITCH = 2;
    public static final byte TYPE_SETTINGS = (byte) 200;


    public static void main(String[] args) throws InterruptedException, IOException {
        Connector connector = new Connector();
        connector.startFromCommandline(args, "BedlightDriver", new Driver());
    }

    @Override
    public void setup(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void start() throws MqttException {
        mqttClient.subscribe(rawInTopic);
        mqttClient.subscribe(getLightTopicOut() + "/set");
    }

    @Override
    public void stop() {
        
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        LOG.info("Received message on topic " + s);
        if (s.startsWith(driverBaseTopic) && s.endsWith("/in")) {
            LOG.info("Handling message " + mqttMessage);

            try {
                JsonNode root = objectMapper.readTree(mqttMessage.getPayload());
                receivedJsonMessage(root);
            } catch (IOException | MqttException e) {
                LOG.throwing("Driver", "messageArrived", e);
            }
        } else if (rawInTopic.equals(s)) {
            LOG.info("Handling Arduino message type " + mqttMessage.getPayload()[0]);
            ByteBuffer byteBuffer = ByteBuffer.wrap(mqttMessage.getPayload());
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            receivedRawMessage(byteBuffer);
        } else {
            LOG.severe("Unhandled: " + s + " " + mqttMessage);
        }

    }

    private void receivedJsonMessage(JsonNode root) throws MqttException {
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
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private void receivedRawMessage(ByteBuffer msg) throws JsonProcessingException, UnsupportedEncodingException, MqttException {
        byte sensorId;
        byte type = msg.get();
        HashMap<String, Object> map = new HashMap<>();
        String topic;
        switch (type) {
            case TYPE_TRIGGER:
                sensorId = msg.get();
                topic = getMotionSensorTopicOut(sensorId);
                mqttClient.publish(topic, jsonMessages.trigger("light"), 2, false);
                break;
            case TYPE_SWITCH:
                sensorId = msg.get();
                byte state = msg.get();
                topic = getLightTopicOut();
                mqttClient.publish(topic, jsonMessages.switchMessage("light", state == 0), 2, false);
                break;
            default:
                LOG.severe("Unhandled raw type:" + type);
                return;
        }
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
