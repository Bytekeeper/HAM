# Possible drivers
A collection of drivers which could/should be implemented ;-)

## BKBedlightDriver
This driver controls an RGB stripe and a motion sensor

## TemperatureDriver
This driver receives temperature, humidity, athmospheric pressure and voltage readings from different sensors. The received values are published to the bus.

## OnkyoDriver
Connects to Onkyo receivers to control and query them.

## PioneerDriver
Connetcs to Pioneer receivers to contron and query them.

## FritzboxDriver
Fetches status from a Fritz Box like currently connected devices in the network an WIFI. Connects over UPNP/Soap to Fritz Box.

## DataLoggerDriver
Persists data and can be queried for the persisted data later on.

## DiscoveryServerDriver
Just a technical component which answers to broadcasts of newly started drivers which do not yet know where the MQQT broker resides.

## PS2014Driver
Controls the modded Ikea PS2014 lamp

## AsteriskDriver
Connects to a running Asterisk server and notifies about incoming calls

## TimeTriggerDriver
Sends out events based on configured times, dates, intervals

## CalDavDriver
Queries a CalDav server like Owncloud and triggers events based on calendar entries

## GoogleCalenderDriver
Queries and adds events on the google calendar.

## KodiDriver
Triggers events based on actions performed in Kodi like: Movie paused, switch on light

## EmailDriver
Sends and receives emails.
