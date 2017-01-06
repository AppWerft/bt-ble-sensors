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

package com.equenda.inmotion.sensors.ble.peripherals;

/**
 * Constants associated BLE device information
 */
public interface BLEConstants {

    // Device information
    public static final String DEV_INFO_SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_FIRMWARE_REV = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_HARDWARE_REV = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_SOFTWARE_REV = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String DEV_INFO_11073_CERT_DATA = "00002a2a-0000-1000-8000-00805f9b34fb";

    // Client notification configuration
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
}
