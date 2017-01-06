
package com.equenda.inmotion.sensors.ble.peripherals.heartrate;

import android.bluetooth.*;
import android.content.Intent;
import com.equenda.inmotion.sensors.ble.peripherals.BLEServiceBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Bluetooth heart rate sensor service. This builds on the service base, which handles all of the connectivity
 * and broad-casting.
 * <p>
 * The primary responsibility of this module is to decode the characteristics and return an
 * intent which can be published.
 *
 * @author Jason Waring
 */
public class HeartRateService extends BLEServiceBase {
    private final static String TAG = HeartRateService.class.getSimpleName();

    private HashMap<String, Object> values = new HashMap<String, Object>();

    @Override
    public void updateDevice(Map commands) {
        // NOP
    }

    public Intent buildDataIntent(final String action, final BluetoothGattCharacteristic characteristic) {

        String uuid = characteristic.getUuid().toString();

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (HeartRateConstants.HEART_RATE_BPM_CHAR_UUID.equalsIgnoreCase(uuid)) {
            int flag = characteristic.getProperties();
            int format = ((flag & 0x01) != 0) ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
            final int heartRate = characteristic.getIntValue(format, 1);
            values.put("heartRate", heartRate);

            final Intent intent = new Intent(action);
            intent.putExtra(EXTRA_DATA_TYPE, "sensors");
            intent.putExtra(EXTRA_DATA, values);
            return intent;

        } else if (HeartRateConstants.BODY_SENSOR_LOCATION_CHAR_UUID.equalsIgnoreCase(uuid)) {
            int flag = characteristic.getProperties();
            int format = ((flag & 0x01) != 0) ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
            final int location = characteristic.getIntValue(format, 0);

            switch (location) {
                case 0:
                    values.put("sensorLocation", "other");
                    break;

                case 1:
                    values.put("sensorLocation", "chest");
                    break;

                case 2:
                    values.put("sensorLocation", "wrist");
                    break;

                case 3:
                    values.put("sensorLocation", "finger");
                    break;

                case 4:
                    values.put("sensorLocation", "hand");
                    break;

                case 5:
                    values.put("sensorLocation", "ear lobe");
                    break;

                case 6:
                    values.put("sensorLocation", "foot");
                    break;

                default:
                    values.put("sensorLocation", "unknown");
                    break;
            }

            final Intent intent = new Intent(action);
            intent.putExtra(EXTRA_DATA_TYPE, "sensors");
            intent.putExtra(EXTRA_DATA, values);
            return intent;
        }

        return null;
    }
}
