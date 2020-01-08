# Empatica E4 Android App

## About

A simple android application for connecting to an Empatica E4 device via bluetooth and streaming subsequent data to a mysql backend.

## Requirements

- A developer api key is needed for the E4, please check [https://www.empatica.com/connect/developer.php] for all the necessary steps and components.
- A android device is needed [application runs on android version 7.1.1].
- E4 developer documentation can be found at [http://developer.empatica.com].

## Setup

- In "MainActivity" ensure EMPATICA_API_KEY is valid
- In "MainActivity" make sure ACCEL_DEVIATION is the expected value
- In "RetrofitClient" ensure that baseUrl has the correct localhost ip and the port is congruent with the port in empactica_backend
