package com.equenda.inmotion.sensors.ble;

import java.util.HashMap;

/**
  *
  */
public interface BLECallback {

    void onConnecting(BLEPeripheral peripheral);

    void onConnected(BLEPeripheral peripheral);

    void onDisconnecting(BLEPeripheral peripheral);

    void onDisconnected(BLEPeripheral peripheral);

    void onDeviceInfo(BLEPeripheral peripheral, HashMap<String, Object> deviceInfo);

    void onData(BLEPeripheral peripheral, String name, String dataType, HashMap<String, Object> data);
}
