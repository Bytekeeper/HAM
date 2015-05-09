# HAM

MQTT based Home Automation.

## Message format
JSON

## Driver basics
Drivers represent, possibly aggregated, devices like an Arduino with multiple sensors.

### Driver registration
Drivers should publish and subscribe to:
<location>/drivers/registry

### Driver topics
All of the topics are with the following "topic space":
<location>/<driver_name>

#### In and Outgoing messages and Failures
Messages to be processed by the driver should end in the topic ".../in", while the driver will push its messages on ".../out". If incoming messages couldn't be processed or something else went wrong, the driver should publish a message on ".../log".

#### Sensors and Actors
Shorthand for sensors/actors: entity
They should generally use the topic "<location>/<driver_name>/<entity_name>". Some entities can have multiple aspects, for example a colored light with automatic dimmer (like bkbedlight) has a "color", a "fade in duration", a "fade out duration" and a "light duration". These are embedded in the messages themselves.
