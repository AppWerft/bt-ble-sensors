package com.equenda.inmotion.sensors.ble;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

/**
  *
  */
public interface BLEPeripheral {

    String getName();

    String getType();

    String getAddress();

    BluetoothDevice getDevice();

    boolean isConnected();

    void connect(BLECallback callback);

    void disconnect();

    void update(Map values);

    void requestDeviceInfo();
}
