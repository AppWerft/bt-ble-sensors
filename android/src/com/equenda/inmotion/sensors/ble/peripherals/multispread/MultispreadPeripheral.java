package com.equenda.inmotion.sensors.ble.peripherals.multispread;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
import com.equenda.inmotion.sensors.ble.peripherals.BLEPeripheralBase;
import com.equenda.inmotion.sensors.ble.peripherals.BLEServiceBase;

import java.util.Map;


/**
 * Multispread BLE peripheral
 *
 * @author Jason Waring
 */
public class MultispreadPeripheral extends BLEPeripheralBase {

    private static final String TAG = MultispreadPeripheral.class.getSimpleName();

    public MultispreadPeripheral(final Activity activity, final BluetoothDevice device, final String type) {
        super(MultispreadService.class, activity, device, type);
    }

    @Override
    public String getName() {
        return (getDevice().getName() != null) ? getDevice().getName() : "Multispread";
    }

    @Override
    public String getServiceType() {
        return "spreader";
    }

    @Override
    public void update(Map values) {
        if (values.containsKey("service")) {
            String service = values.get("service").toString();
            if ("spreader".equalsIgnoreCase(service)) {
                BLEServiceBase bleService = getBLEService();
                bleService.updateDevice(values);
            }
        }
    }

    @Override
    protected BLEServiceBase buildBLEService(IBinder service) {
        return ((MultispreadService.LocalBinder) service).getService();
    }

    @Override
    protected void handleServiceDiscovery(BLEServiceBase bleService) {
        bleService.setCharacteristicNotification(MultispreadConstants.SPEED_CHAR_UUID,true);
        bleService.setCharacteristicNotification(MultispreadConstants.DOOR_OPENING_CHAR_UUID,true);
        bleService.setCharacteristicNotification(MultispreadConstants.LOAD_CELL_CHAR_UUID,true);
        bleService.setCharacteristicNotification(MultispreadConstants.COMMAND_RESPONSE_CHAR_UUID,true);
    }
}
