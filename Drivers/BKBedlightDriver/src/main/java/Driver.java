import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

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
public class Driver {
    private static final Logger LOG = Logger.getLogger(Driver.class.getName());
    public static final String COLOR = "Color";
    public static final String TRIGGER = "Trigger";
    public static final String DURATION = "Duration";

    public static void main(String[] args) throws InterruptedException {
        String topic = "MQTT Examples";
        String rawInTopic = "mqttRf24Bridge/1/sensor";
        String rawOutTopic = "mqttRf24Bridge/1/actor";
        String driverInTopic = "bedroom/bedlight/in";
        final String driverOutTopic = "bedroom/bedlight/out";

        int qos = 2;
        String broker = "tcp://192.168.0.18:1883";
        String clientId = "BedlightDriver";

        try {
            final MqttClient mqttClient = new MqttClient(broker, clientId);
            mqttClient.setTimeToWait(1000);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            mqttClient.connect(connOpts);
            System.out.println("Connected");
            mqttClient.subscribe(rawInTopic);
            mqttClient.subscribe(driverOutTopic);
            mqttClient.setCallback(new MqttCallback() {
                public static final byte TYPE_TRIGGER = 1;
                public static final byte TYPE_SWITCH = 2;
                public static final byte TYPE_SETTINGS = (byte) 200;

                private short r = 300;
                private short g;
                private short b;
                private short fadeInCentiSecs = 100;
                private short fadeOutCentiSecs = 500;
                private short lightCentiSecs = 1500;

                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    LOG.info("Received message on topic " + s);
                    if (driverOutTopic.equals(s)) {
                        LOG.info("Handling message " + mqttMessage);
                        ObjectMapper mapper = new ObjectMapper();

                        try {
                            JsonNode root = mapper.readTree(mqttMessage.getPayload());
                            LOG.info("Message: " + root);
                            String type = root.path("type").asText();
                            switch (type) {
                                case COLOR:
                                    r = (short) (root.path("red").asInt() * 1023 / 65536f);
                                    g = (short) (root.path("green").asInt() * 1023 / 65536f);
                                    b = (short) (root.path("blue").asInt() * 1023 / 65536f);
                                    sendSettings();
                                    break;
                                case DURATION:
                                    String timer = root.path("timer").asText();
                                    double seconds = root.path("seconds").asDouble();
                                    short centiSeconds = (short) (seconds * 100);
                                    switch (timer) {
                                        case "fadeInDuration":
                                            fadeInCentiSecs = centiSeconds;
                                            break;
                                        case "fadeOutDuration":
                                            fadeOutCentiSecs = centiSeconds;
                                            break;
                                        case "activeDuration":
                                            lightCentiSecs = centiSeconds;
                                            break;
                                        default:
                                            LOG.severe("Invalid timer: " + timer);
                                    }
                                    sendSettings();
                                    break;
                                case TRIGGER:
                                    sendMessage(buf -> {
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

                private void dispatchRaw(ByteBuffer msg) throws JsonProcessingException, UnsupportedEncodingException, MqttException {
                    byte sensorId;
                    byte type = msg.get();
                    HashMap<String, Object> map = new HashMap<>();
                    String result;
                    ObjectMapper mapper = new ObjectMapper();
                    switch (type) {
                        case TYPE_TRIGGER:
                            sensorId = msg.get();
                            map.put("type", "Trigger");
                            map.put("subdevice", sensorId);
                            break;
                        case TYPE_SWITCH:
                            sensorId = msg.get();
                            byte state = msg.get();
                            map.put("subdevice", sensorId);
                            map.put("type", "Switch");
                            map.put("state", state == 0 ? "off" : "on");
                            break;
                        default:
                            LOG.severe("Unhandled raw type:" + type);
                            return;
                    }
                    result = mapper.writeValueAsString(map);
                    mqttClient.publish(driverOutTopic, result.getBytes("UTF8"), 2, false);
                }

                private void sendSettings() throws MqttException {
                    sendMessage(buffer -> {
                        buffer.put(TYPE_SETTINGS);
                        buffer.putShort(r);
                        buffer.putShort(g);
                        buffer.putShort(b);
                        buffer.putShort(fadeInCentiSecs);
                        buffer.putShort(fadeOutCentiSecs);
                        buffer.putShort(lightCentiSecs);
                    });
                }

                private void sendMessage(Consumer<ByteBuffer> messagePreparer) throws MqttException {
                    byte[] array = new byte[20];
                    ByteBuffer buffer = ByteBuffer.wrap(array);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    messagePreparer.accept(buffer);
                    mqttClient.publish(rawOutTopic, array, 2, false);
                    LOG.info("Sent message with type " + buffer.get(0));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });

            while (true) {
                Thread.sleep(1000);
            }

//            mqttClient.disconnect();
//            System.out.println("Disconnected");
//            System.exit(0);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }

    }
}
