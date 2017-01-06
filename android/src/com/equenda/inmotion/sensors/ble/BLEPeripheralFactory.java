package com.equenda.inmotion.sensors.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import com.equenda.inmotion.sensors.ble.peripherals.heartrate.HeartRateConstants;
import com.equenda.inmotion.sensors.ble.peripherals.heartrate.HeartRatePeripheral;
import com.equenda.inmotion.sensors.ble.peripherals.multispread.MultispreadConstants;
import com.equenda.inmotion.sensors.ble.peripherals.multispread.MultispreadPeripheral;

import java.util.List;

/**
 * Discover a supported Bluetooth Low-Energy peripheral.
 *
 * NOTE: Wow, this is old school using a facotry pattern but I'm not sure how to DI in Android.
 *
 * @author Jason Waring
 */
public class BLEPeripheralFactory {

    private static final String SENSORTAG_NAME= "SensorTag";
    private static final String SENSORTAG_ALT_NAME = "TI BLE Sensor Tag";
    private static final String SENSORTAG_TYPE = "sensor-tag";

    private static final String TASKIT_SERIAL_TYPE = "taskit-serial";
    private static final String TASKIT_SERIAL_SERVICE_UUID = "912FFFF0-3D4B-11E3-A760-0002A5D5C51B";


    /**
     * Create a BLE peripheral.
     *
     * @param activity The activity to bind the service to.
     * @param deviceName The name of the peripheral device
     * @param serviceUuids The service UUIDs associated with the peripheral
     * @param device The device controller.
     * @return A BLEPeripheral or null
     */
    public static final BLEPeripheral createPeripheral(Activity activity, String deviceName, List<ParcelUuid> serviceUuids, BluetoothDevice device) {
        String type = inferType(deviceName, serviceUuids);

        if (MultispreadConstants.TYPE.equals(type)) {
            return new MultispreadPeripheral(activity, device, type);

        } else if (HeartRateConstants.TYPE.equals(type)) {
            return new HeartRatePeripheral(activity, device, type);
        }

        return null;
    }

    /**
     * Create a BLE peripheral.
     *
     * @param activity The activity to bind the service to.
     * @param type The type of the peripheral to bind to.
     * @param device The device controller.
     * @return A BLEPeripheral or null
     */
    public static final BLEPeripheral createPeripheral(Activity activity, String type, BluetoothDevice device) {
        if (MultispreadConstants.TYPE.equals(type)) {
            return new MultispreadPeripheral(activity, device, type);

        } else if (HeartRateConstants.TYPE.equals(type)) {
            return new HeartRatePeripheral(activity, device, type);
        }

        return null;
    }

    // Infer the supported BLE device. Returns a name or null.
    private static String inferType(String deviceName, List<ParcelUuid> services) {

        // First use the services
        if (services != null) {
            for (ParcelUuid service : services) {
                String uuid = service.getUuid().toString();

                if (HeartRateConstants.SERVICE_UUID.equalsIgnoreCase(uuid)) {
                    return HeartRateConstants.TYPE;

                } else if (MultispreadConstants.SERVICE_UUID.equalsIgnoreCase(uuid)) {
                    return MultispreadConstants.TYPE;

                } else if (TASKIT_SERIAL_SERVICE_UUID.equalsIgnoreCase(uuid)) {
                    return TASKIT_SERIAL_TYPE;
                }
            }
        }

        // Now rely on the name.
        if (deviceName != null) {
            if (SENSORTAG_NAME.equalsIgnoreCase(deviceName) || SENSORTAG_ALT_NAME.equalsIgnoreCase(deviceName)) {
                return SENSORTAG_TYPE;
            }
        }

        return "";
    }
}
