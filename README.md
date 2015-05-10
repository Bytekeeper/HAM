# HAM

MQTT based Home Automation.

Shorthand for sensors/actors: entity

## Messages

### Message Format
JSON:
```json
{ "type" : \<type\>, "aspect" : \<aspect name\>...}
```
### Message Types

##### Color
As outgoing message: Sets the color of a an aspect of an entity.
As incoming message: Color state of the aspect of an entity.
```json
{ "type" : "Color", "aspect" : "\<aspect name \>", "red" : "0-65535", "green" : "0-65535", "blue" : "0-65535" }
```

For example:
```json
{ "type" : "Color", "aspect" : "light", "red" : "10000", "green" : "0", blue : "0" }
```

The driver is responsible for converting RGB back and forth between the actual hardware (if the acutal hardware uses a yellow LED, the driver performs the mixing of red + green)

##### Trigger

##### Switch

##### Duration

## Driver basics
Drivers represent, possibly aggregated, devices like an Arduino with multiple sensors.

### Driver registration
Drivers should publish and subscribe to:
\<location\>/drivers/registry

### Driver topics
All of the topics are with the following "topic space":
\<location\>/\<driver_name\>

#### In and Outgoing messages and Failures
Messages to be processed by the driver should end in the topic ".../in", while the driver will push its messages on ".../out". If incoming messages couldn't be processed or something else went wrong, the driver should publish a message on ".../log".

#### Sensors and Actors
They should generally use the topic "<location>/<driver_name>/<entity_name>". Some entities can have multiple aspects, for example a colored light with automatic dimmer (like bkbedlight) has a "color", a "fade in duration", a "fade out duration" and a "light duration". These are embedded in the messages themselves.
