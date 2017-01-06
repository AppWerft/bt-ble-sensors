/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.equenda.inmotion.sensors.ble.peripherals.heartrate;

/**
 * Constants associated with heart rate sensor.
 */
public interface HeartRateConstants {

    public static final String TYPE = "heart-rate";
    public static final String SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_BPM_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb";
    public static final String BODY_SENSOR_LOCATION_CHAR_UUID = "00002a38-0000-1000-8000-00805f9b34fb";
}
