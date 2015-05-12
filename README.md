# HAM

MQTT based Home Automation. The focus of HAM is to provide an abstraction of sensors and actors, and yet remain close to the metal. To reach this goal devices a split into aspect with capabilities, without loosing the information that those aspects might work in tandem.

A night light for example might have a LED and a motion sensor. Both have different capabilities. They also obviously operate together.

HAM devices aren't meant to be necessarily stupid. It's quiet sensible to have even small devices carry out some logic without querying some controller, to provide basic functionality even in case of a communication outage. The motion sensor from above performs its basic function without uplink. But it could also be triggered from a motion sensor in another room.

Shorthand for sensors/actors: entity

## Messages

### Message Format
JSON:
```json
{ "type" : <type>, "aspect" : <aspect name>...}
```
### Message Types

##### Color
Message to driver: Sets the color of a an aspect of an entity.
Message from driver: Color state of the aspect of an entity.
```json
{ "type" : "Color", "aspect" : "<aspect name>", "red" : "0-65535", "green" : "0-65535", "blue" : "0-65535" }
```

For example:
```json
{ "type" : "Color", "aspect" : "light", "red" : "10000", "green" : "0", blue : "0" }
```

The driver is responsible for converting RGB back and forth between the actual hardware (if the acutal hardware uses a yellow LED, the driver performs the mixing of red + green)

##### Trigger
Message to driver: Triggers an aspect on an entity
Message from driver: An aspect was triggered on the hardware (ie. motion sensor)
```json
{ "type" : "Trigger", "aspect" : "<aspect name>" }
```

##### Switch
Message to driver: Switches an aspect of an entity on or off
Message from driver: An aspect was switched on or off (ie. a hardware switch, a light)
```json
{ "type" : "Switch", "aspect" : "<aspect name>", "state": "on/off" }
```

##### Duration
Message to driver: Configures the duration of an aspect (ie. how long the floor light should stay on)
Message from driver: A duration measured by a sensor
```json
{ "type" : "Duration", "aspect" : "<aspect name>", "duration" : "<n>" }
```
where \<n\> is given in seconds (ie. 0.5 for half a second).

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
They should generally use the topic "\<location\>/\<driver_name\>/\<entity_name\>". Some entities can have multiple aspects, for example a colored light with automatic dimmer (like bkbedlight) has a "color", a "fade in duration", a "fade out duration" and a "light duration". These are embedded in the messages themselves.

# Advanced
## Automatic discovery of drivers and MQTT brokers
## Controllers
## UI interfaces
They are basically just controllers, but might incorporate driver aspect (ie. for notifications).
