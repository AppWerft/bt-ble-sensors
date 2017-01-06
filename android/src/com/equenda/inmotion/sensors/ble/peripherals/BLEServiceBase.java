package com.equenda.inmotion.sensors.ble.peripherals;

import android.app.Service;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.*;

import static android.bluetooth.BluetoothGattDescriptor.*;

/**
 * An abstract base service class for for managing connection and data communication
 * with a BLE GATT server.
 * <p>
 * Based on the Android Open Source Heart Rate source code example.
 *
 * @author Jason Waring
 */
public abstract class BLEServiceBase extends Service {
    public final static String ACTION_GATT_CONNECTING = "com.equenda.inmotion.sensors.ble.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_CONNECTED = "com.equenda.inmotion.sensors.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTING = "com.equenda.inmotion.sensors.ble.ACTION_GATT_DISCONNECTING";
    public final static String ACTION_GATT_DISCONNECTED = "com.equenda.inmotion.sensors.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.equenda.inmotion.sensors.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.equenda.inmotion.sensors.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.equenda.inmotion.sensors.ble.EXTRA_DATA";
    public final static String EXTRA_DATA_TYPE = "com.equenda.inmotion.sensors.ble.EXTRA_DATA_TYPE";

    private final static String TAG = BLEServiceBase.class.getSimpleName();

    protected static final int STATE_DISCONNECTED = 0;
    protected static final int STATE_CONNECTING = 1;
    protected static final int STATE_CONNECTED = 2;

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private String deviceAddress;
    private BluetoothGatt btGatt;
    private int connectionState = STATE_DISCONNECTED;
    private HashMap<String, String> deviceInfo = new HashMap<String, String>();
    private Queue<BluetoothGattDescriptor> descriptorWriteDescQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<BluetoothGattCharacteristic>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BLEServiceBase getService() {
            return BLEServiceBase.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (btManager == null) {
            btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (btManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public BluetoothGatt getGatt() {
        return btGatt;
    }

    public int getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(int cs) {
        connectionState = cs;
        switch (connectionState) {
            case STATE_DISCONNECTED:
                broadcastSimple(ACTION_GATT_DISCONNECTED);
                break;

            case STATE_CONNECTED:
                broadcastSimple(ACTION_GATT_CONNECTED);
                break;

            case STATE_CONNECTING:
                broadcastSimple(ACTION_GATT_CONNECTING);
                break;
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (btAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (deviceAddress != null && address.equalsIgnoreCase(deviceAddress) && btGatt != null) {
            Log.d(TAG, "Trying to use an existing btGatt for connection.");
            if (btGatt.connect()) {
                setConnectionState(STATE_CONNECTING);
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = btAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        btGatt = device.connectGatt(this, false, btGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        deviceAddress = address;
        setConnectionState(STATE_CONNECTING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (btAdapter == null || btGatt == null) {
            return;
        }

        btGatt.disconnect();
        setConnectionState(STATE_DISCONNECTED);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (btGatt == null) {
            return;
        }
        btGatt.close();
        btGatt = null;
    }

    /**
     * Find a characteristic by the UUID string
     *
     * @param strUuid The uuid as a string
     * @return The characteristic OR null if not found
     */
    public BluetoothGattCharacteristic findCharacteristic(String strUuid) {
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }

        UUID uuid = UUID.fromString(strUuid);
        for (BluetoothGattService s : btGatt.getServices()) {
            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                if (uuid.equals(c.getUuid())) {
                    return c;
                }
            }
        }

        return null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param strUuid Characteristic UUID to act on, as a string
     */
    public void readCharacteristic(String strUuid) {
        BluetoothGattCharacteristic c = findCharacteristic(strUuid);
        if (c != null) {
            readGattCharacteristic(c);
        }
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param strUuid Characteristic UUID to act on, as a string
     * @param data    The data to write.
     */
    public void writeCharacteristic(String strUuid, byte[] data) {
        BluetoothGattCharacteristic c = findCharacteristic(strUuid);
        if (c != null) {
            writeGattCharacteristic(c, data);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param strUuid Characteristic UUID to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(String strUuid, boolean enabled) {
        if (btAdapter == null || btGatt == null) {
            return;
        }

        BluetoothGattCharacteristic c = findCharacteristic(strUuid);
        if (c != null) {
            btGatt.setCharacteristicNotification(c, enabled);
            BluetoothGattDescriptor descriptor = c.getDescriptor(UUID.fromString(BLEConstants.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                descriptor.setValue((enabled) ? ENABLE_NOTIFICATION_VALUE : DISABLE_NOTIFICATION_VALUE);
                writeGattDescriptor(descriptor);
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (btGatt == null) return null;
        return btGatt.getServices();
    }

    /**
     * A set of commands to update, for the service,
     *
     * @param values The values
     */
    public abstract void updateDevice(Map values);

    /**
     * An abstract method to build a Data Characteristic intent.
     *
     * @param action         The action
     * @param characteristic The characteristic
     * @return An intent or null.
     */
    public abstract Intent buildDataIntent(final String action,
                                           final BluetoothGattCharacteristic characteristic);


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (btAdapter == null || btGatt == null) {
            return;
        }
        //put the characteristic into the read queue
        characteristicReadQueue.add(characteristic);

        dispatchNextRequest();
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to write to.
     * @param data           The data to write.
     */
    private void writeGattCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (btAdapter == null || btGatt == null) {
            return;
        }

        characteristic.setValue(data);

        //put the characteristic into the write queue
        characteristicWriteQueue.add(characteristic);

        dispatchNextRequest();
    }

    private void writeGattDescriptor(BluetoothGattDescriptor d) {
        //put the descriptor into the write queue
        descriptorWriteDescQueue.add(d);

        dispatchNextRequest();
    }

    private void broadcastSimple(final String action) {
        sendBroadcast(new Intent(action));
    }

    private void broadcastData(final BluetoothGattCharacteristic characteristic) {
        Intent intent = this.buildDataIntent(ACTION_DATA_AVAILABLE, characteristic);
        if (intent != null) {
            sendBroadcast(intent);
        }
    }

    private boolean isDeviceInfoCharacteristic(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();

        return BLEConstants.DEV_INFO_SYSTEM_ID.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_MODEL_NUMBER.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_SERIAL_NUMBER.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_FIRMWARE_REV.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_HARDWARE_REV.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_SOFTWARE_REV.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_MANUFACTURER_NAME.equalsIgnoreCase(uuid) ||
                   BLEConstants.DEV_INFO_11073_CERT_DATA.equalsIgnoreCase(uuid);
    }

    private void broadcastDeviceInfo(final BluetoothGattCharacteristic characteristic) {
        Intent intent = buildDeviceInfoIntent(characteristic);
        if (intent != null) {
            sendBroadcast(intent);
        }
    }

    private Intent buildDeviceInfoIntent(final BluetoothGattCharacteristic characteristic) {

        String uuid = characteristic.getUuid().toString();
        if (BLEConstants.DEV_INFO_SYSTEM_ID.equalsIgnoreCase(uuid)) {
            deviceInfo.put("systemId", charToHex(characteristic));

        } else if (BLEConstants.DEV_INFO_MODEL_NUMBER.equalsIgnoreCase(uuid)) {
            deviceInfo.put("modelNumber", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_SERIAL_NUMBER.equalsIgnoreCase(uuid)) {
            deviceInfo.put("serialNumber", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_FIRMWARE_REV.equalsIgnoreCase(uuid)) {
            deviceInfo.put("firmwareRev", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_HARDWARE_REV.equalsIgnoreCase(uuid)) {
            deviceInfo.put("hardwareRev", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_SOFTWARE_REV.equalsIgnoreCase(uuid)) {
            deviceInfo.put("softwareRev", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_MANUFACTURER_NAME.equalsIgnoreCase(uuid)) {
            deviceInfo.put("manufacturerName", charToString(characteristic));

        } else if (BLEConstants.DEV_INFO_11073_CERT_DATA.equalsIgnoreCase(uuid)) {
            deviceInfo.put("certData", charToString(characteristic));
        }

        final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
        intent.putExtra(EXTRA_DATA_TYPE, "deviceInfo");
        intent.putExtra(EXTRA_DATA, deviceInfo);
        return intent;
    }


    // Process the dispatch queue in order:
    // - Write descriptor
    // - Read characteristic
    // - Write characteristic.
    private void dispatchNextRequest() {

        //if there is more to write, do it!
        if (descriptorWriteDescQueue.size() > 0) {
            btGatt.writeDescriptor(descriptorWriteDescQueue.element());

        } else if (characteristicReadQueue.size() > 0) {
            btGatt.readCharacteristic(characteristicReadQueue.element());

        } else if (characteristicWriteQueue.size() > 0) {
            btGatt.writeCharacteristic(characteristicWriteQueue.element());

        }
    }


    protected String charToString(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        return new String(data);
    }

    protected String charToHex(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data) {
            stringBuilder.append(String.format("%02X", byteChar));
        }

        return stringBuilder.toString();
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");

                setConnectionState(STATE_CONNECTED);
                getGatt().discoverServices();
                broadcastSimple(ACTION_GATT_CONNECTED);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                setConnectionState(STATE_DISCONNECTED);
                broadcastSimple(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastSimple(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.w(TAG, "onServicesDiscovered failed: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            try {
                characteristicReadQueue.remove();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (isDeviceInfoCharacteristic(characteristic)) {
                        broadcastDeviceInfo(characteristic);
                    } else {
                        broadcastData(characteristic);
                    }
                } else {
                    Log.d(TAG, "onCharacteristicRead error: " + status);
                }
            } finally {
                dispatchNextRequest();
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            try {
                characteristicWriteQueue.remove();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastData(characteristic);
                } else {
                    Log.d(TAG, "onCharacteristicWrite error: " + status);
                }
            } finally {
                dispatchNextRequest();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastData(characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                descriptorWriteDescQueue.remove();  //pop the item that we just finishing writing

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // NOP
                } else {
                    Log.d(TAG, "Error writing GATT Descriptor: " + status);
                }
            } finally {
                dispatchNextRequest();
            }
        }
    };

}
