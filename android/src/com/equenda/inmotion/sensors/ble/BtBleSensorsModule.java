/**
 *
 */
package com.equenda.inmotion.sensors.ble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;


@Kroll.module(name = "BtBleSensors", id = "com.equenda.inmotion.sensors.ble")
public class BtBleSensorsModule extends KrollModule {

    // Standard Debugging variables
    private static final String TAG = BtBleSensorsModule.class.getSimpleName();

    private final static int REQUEST_DISABLE_BT = 0;
    private final static int REQUEST_ENABLE_BT = 1;

    private API api = new API(this);
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private String status;
    private BluetoothLeScanner bluetoothLeScanner;
    private Map<String, BLEPeripheral> discovered = new HashMap<String, BLEPeripheral>();
    private Map<String, BLEPeripheral> active = new HashMap<String, BLEPeripheral>();

    public BtBleSensorsModule() {
        super();
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        Log.i(TAG, "onAppCreate " + app.getCurrentActivity());
    }

    @Override
    public void onStart(Activity activity) {
        super.onStart(activity);
        Log.i(TAG, "onStart");
        prepare();
    }

    @Override
    public void onResume(Activity activity) {
        Log.i(TAG, "onResume");
        super.onResume(activity);
    }

    @Override
    public void onDestroy(Activity activity) {
        Log.i(TAG, "onDestroy");
        this.cancelDiscovery();
        this.disable();
        this.btAdapter = null;
        this.btManager = null;

        super.onDestroy(activity);
    }

    @Kroll.method
    public Boolean isAvailable() {
        prepare();
        return btManager != null;
    }

    @Kroll.method
    public Boolean isEnabled() {
        prepare();
        return btAdapter != null && btAdapter.isEnabled();
    }

    @Kroll.method
    public void enable() {
        prepare();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getActivity().startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Kroll.method
    public void disable() {
        prepare();

        if (btAdapter != null && btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getActivity().startActivityForResult(enableIntent, REQUEST_DISABLE_BT);
        }
    }

    @Kroll.method
    public boolean isConnected(String address) {
        if (isEnabled()) {
            if (active.containsKey(address)) {
                BLEPeripheral peripheral = discovered.get(address);
                return peripheral.isConnected();
            }
        }

        return false;
    }

    @Kroll.method
    public void connect(String address, @Kroll.argument(optional=true) String typeHint) {
        if (isEnabled()) {

            // If the peripheral is not active, then we need to create it.
            if (!active.containsKey(address)) {

                // First check whether a hint was provided. If not, then try to use the previously
                // discovered peripheral.
                String type = typeHint; // typeHint can be null.
                if (type == null) {
                    if (!discovered.containsKey(address)) {
                        Log.e(TAG, "Device not previous discovered and no type hint was provided");
                        return;
                    }
                    type = discovered.get(address).getType();
                }

                // Given the type, we can create the peripheral directly from the address and type.
                BLEPeripheral peripheral = BLEPeripheralFactory.createPeripheral(getActivity(), type, btAdapter.getRemoteDevice(address));
                if (peripheral == null) {
                    Log.e(TAG, String.format("Failed to find a peripheral for address %s and type %s", address, type));
                    return;
                }

                active.put(address, peripheral);
            }

            BLEPeripheral peripheral = active.get(address);
            if (!peripheral.isConnected()) {
                peripheral.connect(leDataCallback);
            }
        }
    }

    @Kroll.method
    public void disconnect(String address) {
        if (isEnabled()) {
            if (active.containsKey(address)) {
                BLEPeripheral peripheral = active.get(address);
                if (peripheral.isConnected()) {
                    peripheral.disconnect();
                }

                // Device removed from active list, in disconnect callback.
            }
        }
    }

    @Kroll.method
    public boolean hasListener(String eventName) {
        return false;
    }

    @Kroll.method
    public void update(String address, HashMap values) {
        if (isEnabled()) {
            if (active.containsKey(address)) {
                BLEPeripheral peripheral = active.get(address);
                if (peripheral.isConnected()) {
                    peripheral.update(values);
                }
            }
        }
    }

    @Kroll.method
    public void requestDeviceInfo(String address) {
        if (isEnabled()) {
            if (active.containsKey(address)) {
                BLEPeripheral peripheral = active.get(address);
                if (peripheral.isConnected()) {
                    peripheral.requestDeviceInfo();
                }
            }
        }
    }

    @Kroll.method
    public void startDiscovery() {
        prepare();
        if (btAdapter != null && btAdapter.isEnabled() && bluetoothLeScanner == null) {
            bluetoothLeScanner = btAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(leScanCallback);
            Log.d(TAG, "sendStartDiscovery");
            api.sendStartDiscovery();
        }
    }

    @Kroll.method
    public void cancelDiscovery() {
        prepare();
        if (btAdapter != null && btAdapter.isEnabled() && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d(TAG, "cancelDiscovery");
            api.sendStopDiscovery();
            bluetoothLeScanner = null;
        }
    }

    private void prepare() {
        boolean hasLe = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (hasLe) {
            if (getActivity() != null && btManager == null) {
                btManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
                if (btManager != null) {
                    btAdapter = btManager.getAdapter();
                    if (btAdapter != null) {
                        setBluetoothStatus("ready");
                    } else {
                        setBluetoothStatus("unknown");
                    }
                } else {
                    setBluetoothStatus("off");
                }
            } else {
                setBluetoothStatus("unknown");
            }
        } else {
            setBluetoothStatus("unsupported");
        }
    }

    private void setBluetoothStatus(String status) {
        if ((this.status == null) || this.status.equals(status)) {
            this.status = status;

            if ("off".equals(status)) {
                api.sendStatus(status, "Bluetooth is powered off");

            } else if ("ready".equals(status)) {
                api.sendStatus(status, "Bluetooth is powered on and ready");

            } else if ("unauthorised".equals(status)) {
                api.sendStatus(status, "Bluetooth is unauthorized");

            } else if ("unsupported".equals(status)) {
                api.sendStatus(status, "Bluetooth in unsupported state");

            } else {
                api.sendStatus(status, "Mysterious status");
            }
        }
    }

    private BLECallback leDataCallback = new BLECallback() {
        @Override
        public void onConnecting(BLEPeripheral peripheral) {
            api.sendConnectionStatus(peripheral.getAddress(), "connecting");
        }

        @Override
        public void onConnected(BLEPeripheral peripheral) {
            api.sendConnectionStatus(peripheral.getAddress(), "connected");
        }

        @Override
        public void onDisconnecting(BLEPeripheral peripheral) {
            api.sendConnectionStatus(peripheral.getAddress(), "disconnecting");
        }

        @Override
        public void onDisconnected(BLEPeripheral peripheral) {
            api.sendConnectionStatus(peripheral.getAddress(), "disconnected");

            // Cleanup.
            if (active.containsKey(peripheral.getAddress())) {
                active.remove(peripheral.getAddress());
            }
        }

        @Override
        public void onDeviceInfo(BLEPeripheral peripheral, HashMap<String, Object> values) {
            api.sendDeviceInfo(peripheral.getAddress(), values);
        }

        @Override
        public void onData(BLEPeripheral peripheral, String serviceType, String dataType, HashMap<String, Object> values) {
            api.sendData(peripheral.getName(), serviceType, dataType, peripheral.getAddress(), values);
        }
    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceName = result.getScanRecord().getDeviceName();
            List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
            BluetoothDevice device = result.getDevice();
            BLEPeripheral peripheral = BLEPeripheralFactory.createPeripheral(getActivity(), deviceName, serviceUuids, device);

            if (peripheral != null) {
                // Add peripheral if not previously discovered.
                if (!discovered.containsKey(peripheral.getAddress())) {
                    discovered.put(peripheral.getAddress(), peripheral);
                }

                api.sendDeviceDetected(peripheral.getName(), peripheral.getAddress(), peripheral.getType(), result.getRssi());
            }

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}
