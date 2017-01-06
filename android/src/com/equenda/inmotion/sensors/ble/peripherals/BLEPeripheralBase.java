package com.equenda.inmotion.sensors.ble.peripherals;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.IBinder;
import com.equenda.inmotion.sensors.ble.BLECallback;
import com.equenda.inmotion.sensors.ble.BLEPeripheral;
import org.appcelerator.kroll.common.Log;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_NOT_FOREGROUND;


/**
 * Bluetooth Low-Energy peripheral base class
 *
 * @author Jason Waring
 */
public abstract class BLEPeripheralBase<T> implements BLEPeripheral {

    private static final String TAG = BLEPeripheralBase.class.getSimpleName();

    private Class<T> serviceClass;
    private Activity activity;
    private BluetoothDevice device;
    private String type;
    private BLEServiceBase bleService;
    private BLECallback callback;
    private boolean connected;

    public BLEPeripheralBase(final Class<T> serviceClass,
                             final Activity activity,
                             final BluetoothDevice device,
                             final String type) {
        this.serviceClass = serviceClass;
        this.activity = activity;
        this.device = device;
        this.type = type;
        this.callback = null;
        this.connected = false;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getAddress() {
        return device.getAddress();
    }

    @Override
    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect(BLECallback callback) {
        this.callback = callback;
        if (!connected) {
            Log.i(TAG, "Connecting");
            Intent gattServiceIntent = new Intent(activity, serviceClass);
            activity.startService(gattServiceIntent);
            activity.bindService(gattServiceIntent, serviceConnection, BIND_NOT_FOREGROUND);

        }
    }

    @Override
    public void disconnect() {
        if (connected) {
            Log.i(TAG, "Disconnecting");
            callback = null;

            if (bleService != null) {
                Intent gattServiceIntent = new Intent(activity, serviceClass);
                activity.stopService(gattServiceIntent);
            }
        }
    }

    @Override
    public void update(Map values) {
    }

    @Override
    public void requestDeviceInfo() {
        if (bleService != null) {
            bleService.readCharacteristic(BLEConstants.DEV_INFO_SYSTEM_ID);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_MODEL_NUMBER);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_SERIAL_NUMBER);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_FIRMWARE_REV);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_HARDWARE_REV);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_SOFTWARE_REV);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_MANUFACTURER_NAME);
            bleService.readCharacteristic(BLEConstants.DEV_INFO_11073_CERT_DATA);
        }
    }


    public abstract String getServiceType();

    protected abstract BLEServiceBase buildBLEService(IBinder service);

    protected abstract void handleServiceDiscovery(BLEServiceBase bleService);

    protected BLEServiceBase getBLEService() {
        return bleService;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "Service connected");
            bleService = buildBLEService(service);
            if (bleService.initialize()) {
                // Automatically connects to the device upon successful start-up initialization.
                activity.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
                bleService.connect(device.getAddress());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (bleService != null) {
                Log.i(TAG, "Service disconnected");
                try {
                    activity.unregisterReceiver(gattUpdateReceiver);
                } catch (Throwable th) {
                }
                bleService.close();
                bleService = null;
                connected = false;
            }
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BLEServiceBase.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                if (callback != null) {
                    callback.onConnected(BLEPeripheralBase.this);
                }

            } else if (BLEServiceBase.ACTION_GATT_CONNECTING.equals(action)) {
                connected = false;
                callback.onConnecting(BLEPeripheralBase.this);

            } else if (BLEServiceBase.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                callback.onDisconnected(BLEPeripheralBase.this);

            } else if (BLEServiceBase.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                handleServiceDiscovery(bleService);

                requestDeviceInfo();

            } else if (BLEServiceBase.ACTION_DATA_AVAILABLE.equals(action)) {
                HashMap<String, Object> data = (HashMap<String, Object>) intent.getSerializableExtra(BLEServiceBase.EXTRA_DATA);

                if (intent.hasExtra(BLEServiceBase.EXTRA_DATA_TYPE)) {
                    String dataType = intent.getStringExtra(BLEServiceBase.EXTRA_DATA_TYPE);
                    if ("deviceInfo".equalsIgnoreCase(dataType)) {
                        callback.onDeviceInfo(BLEPeripheralBase.this, data);

                    } else {
                        callback.onData(BLEPeripheralBase.this, getServiceType(), dataType, data);
                    }
                } else {
                    callback.onData(BLEPeripheralBase.this, getServiceType(), "sensors", data);
                }
            }
        }
    };

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEServiceBase.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BLEServiceBase.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEServiceBase.ACTION_GATT_DISCONNECTING);
        intentFilter.addAction(BLEServiceBase.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEServiceBase.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEServiceBase.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
