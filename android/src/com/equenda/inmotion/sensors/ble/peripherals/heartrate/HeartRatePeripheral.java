package com.equenda.inmotion.sensors.ble.peripherals.heartrate;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
import com.equenda.inmotion.sensors.ble.peripherals.BLEPeripheralBase;
import com.equenda.inmotion.sensors.ble.peripherals.BLEServiceBase;

/**
 * Bluetooth Low-Energy Heart Rate sensor peripheral device
 *
 * @author Jason Waring
 */
public class HeartRatePeripheral extends BLEPeripheralBase {

    private static final String TAG = HeartRatePeripheral.class.getSimpleName();

    public HeartRatePeripheral(final Activity activity, final BluetoothDevice device, final String type) {
        super(HeartRateService.class, activity, device, type);
    }

    @Override
    public String getName() {
        return (getDevice().getName() != null) ? getDevice().getName() : "Heart Rate";
    }

    @Override
    public String getServiceType() {
        return "heartRate";
    }

    @Override
    protected BLEServiceBase buildBLEService(IBinder service) {
        return ((HeartRateService.LocalBinder) service).getService();
    }

    @Override
    protected void handleServiceDiscovery(BLEServiceBase bleService) {
        bleService.setCharacteristicNotification(HeartRateConstants.HEART_RATE_BPM_CHAR_UUID,true);
        bleService.readCharacteristic(HeartRateConstants.BODY_SENSOR_LOCATION_CHAR_UUID);
    }
}
