# Empatica E4 Android App

## About

A simple android application for connecting to an Empatica E4 device via bluetooth and streaming subsequent data to a mysql backend.

## Requirements

- A developer api key is needed for the E4, please check [https://www.empatica.com/connect/developer.php] for all the necessary steps and components.
- A android device is needed [application runs on android version 7.1.1].
- All E4 developer documentation can be found at [http://developer.empatica.com].

## Setup

- In "MainActivity" ensure EMPATICA_API_KEY is the expected value.
- In "MainActivity" make sure ACCEL_DEVIATION is the expected value.
- In "RetrofitClient" ensure that baseUrl has the correct localhost ip and the port is congruent with the port in empactica_backend.

## Getting started

- Sync and build the project, ensuring no errors occur.
- Run project on an usb connected android device accepting all permissions required (Internet, Bluetooth, location, and etc.).
- For debugging mode run project on debug and in Logcat filter by "CustomDebug" to see all debug outputs.

