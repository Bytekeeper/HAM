# Bridges to other home automation systems
To reuse existing components.
The minimum requirement for bridges is to make 3rd party components/drivers available in HAM. 
Additionally bridges might integrate HAM drivers in other systems, if possible.

## OpenHAB

Configure OpenHAB to publish all state changes and commands to our broker. See OpenHAB [MQTT-Binding](https://github.com/openhab/openhab/wiki/MQTT-Binding) for details
```
# URL to the MQTT broker, e.g. tcp://localhost:1883 or ssl://localhost:8883
mqtt:mosquitto.url=tcp://localhost:1883

# Optional. Client id (max 23 chars) to use when connecting to the broker.
# If not provided a default one is generated.
mqtt:mosquitto.clientId=openhab

mqtt-eventbus:broker=mosquitto
mqtt-eventbus:statePublishTopic=openhab/out/${item}/state
mqtt-eventbus:commandPublishTopic=openhab/out/${item}/command
mqtt-eventbus:stateSubscribeTopic=openhab/in/${item}/state
mqtt-eventbus:commandSubscribeTopic=openhab/in/${item}/command
```

## home-assistant
