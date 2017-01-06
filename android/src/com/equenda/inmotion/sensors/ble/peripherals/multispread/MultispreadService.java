
package com.equenda.inmotion.sensors.ble.peripherals.multispread;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import com.equenda.inmotion.sensors.ble.peripherals.BLEServiceBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Bluetooth multispread controller service. This builds on the service base, which handles all of the connectivity
 * and broad-casting.
 * <p>
 * The primary responsibility of this module is to decode the characteristics and return an
 * intent which can be published.
 *
 * @author Jason Waring
 */
public class MultispreadService extends BLEServiceBase {
    private final static String TAG = MultispreadService.class.getSimpleName();
    private static final int FLAG_SPINNER = 0x01;
    private static final int FLAG_DOOR = 0x02;
    private static final int FLAG_LC = 0x04;
    private static final int FLAG_ALL = FLAG_SPINNER | FLAG_DOOR | FLAG_LC;
    private static final int FLAG_NONE = 0x00;

    private HashMap<String, Object> values = new HashMap<String, Object>();
    private int flagAccum = FLAG_NONE;

    @Override
    public void updateDevice(Map commands) {

        // Set door opening
        if (commands.containsKey(MultispreadConstants.DOOR_OPENING_TAG)) {
            int doorOpening = asInt(commands.get(MultispreadConstants.DOOR_OPENING_TAG).toString());
            writeCharacteristic(MultispreadConstants.DOOR_OPENING_TARGET_CHAR_UUID, unsignedBytesFromInt(doorOpening));
        }

        // Door calibration
        if (commands.containsKey(MultispreadConstants.DOOR_CALIBRATE_COMMAND_TAG)) {
            String action = commands.get(MultispreadConstants.DOOR_CALIBRATE_COMMAND_TAG).toString();

            if (MultispreadConstants.DOOR_CAL_START_ACTION_TAG.equalsIgnoreCase(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_CALIBRATE, MultispreadConstants.CMD_CALIBRATE_START));

            } else if (MultispreadConstants.DOOR_CAL_CANCEL_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_CALIBRATE, MultispreadConstants.CMD_CALIBRATE_CANCEL));
            }
        }

        // Drive wheel control
        if (commands.containsKey(MultispreadConstants.DRIVE_WHEEL_COMMAND_TAG)) {
            String action = commands.get(MultispreadConstants.DRIVE_WHEEL_COMMAND_TAG).toString();

            if (MultispreadConstants.DW_ENGAGE_ACTION_TAG.equalsIgnoreCase(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DRIVE_WHEEL, MultispreadConstants.CMD_DW_REQUEST_ENGAGE));

            } else if (MultispreadConstants.DW_DISENGAGE_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DRIVE_WHEEL, MultispreadConstants.CMD_DW_REQUEST_DISENGAGE));

            } else if (MultispreadConstants.DW_STATUS_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DRIVE_WHEEL, MultispreadConstants.CMD_DW_REQUEST_STATUS));
            }
        }

        // Diagnostics
        if (commands.containsKey(MultispreadConstants.DIAGNOSTICS_COMMAND_TAG)) {
            String action = commands.get(MultispreadConstants.DIAGNOSTICS_COMMAND_TAG).toString();

            if (MultispreadConstants.ENABLE_DW_DIAG_ACTION_TAG.equalsIgnoreCase(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DW_DIAG, MultispreadConstants.CMD_DIAG_REQUEST_ENABLE));

            } else if (MultispreadConstants.DISABLE_DW_DIAG_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DW_DIAG, MultispreadConstants.CMD_DIAG_REQUEST_DISABLE));

            } else if (MultispreadConstants.ENABLE_DOOR_DIAG_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DOOR_DIAG, MultispreadConstants.CMD_DIAG_REQUEST_ENABLE));

            } else if (MultispreadConstants.DISABLE_DOOR_DIAG_ACTION_TAG.equals(action)) {
                writeCharacteristic(MultispreadConstants.COMMAND_REQUEST_CHAR_UUID, unsignedBytesFromBytePair(MultispreadConstants.CMD_CONTEXT_DOOR_DIAG, MultispreadConstants.CMD_DIAG_REQUEST_DISABLE));
            }
        }
    }

    public Intent buildDataIntent(final String action, final BluetoothGattCharacteristic characteristic) {

        final String uuid = characteristic.getUuid().toString();

        if (MultispreadConstants.COMMAND_RESPONSE_CHAR_UUID.equalsIgnoreCase(uuid)) {
            return buildResponseDataIntent(action, characteristic);
        } else {
            return buildSensorDataIntent(action, characteristic);
        }
    }

    private Intent buildSensorDataIntent(final String action, final BluetoothGattCharacteristic characteristic) {
        Intent intent = null;

        // Map the uuid to a bit flag.
        final int flag = uuidToFlag(characteristic.getUuid().toString());

        // If the flag accumulator has seen this, then build the intent for dispatch.
        // This traps when there is a repitition (e.g. speed, then speed, then speed).
        if ((flagAccum & flag) != 0) {
            intent = new Intent(action);
            intent.putExtra(EXTRA_DATA_TYPE, MultispreadConstants.SENSORS_TAG);
            intent.putExtra(EXTRA_DATA, new HashMap<String, Object>(values));
            flagAccum = FLAG_NONE;
        }

        // Now decode the supplied characteristic.
        byte[] data = characteristic.getValue();
        switch (flag) {
            case FLAG_SPINNER:
                values.put(MultispreadConstants.SPINNER_SPEED_TAG, unsignedBytesToInt(data[1], data[0]));
                if (data.length > 2) {
                    values.put(MultispreadConstants.BELT_SPEED_TAG, unsignedBytesToInt(data[3], data[2]));
                }
                break;

            case FLAG_DOOR:
                values.put(MultispreadConstants.DOOR_OPENING_TAG, unsignedBytesToInt(data[1], data[0]));
                break;

            case FLAG_LC:

                if (data.length < 5) {
                    values.put(MultispreadConstants.LOAD_CELL_TAG, unsignedBytesToInt(data[3], data[2], data[1], data[0]));
                    values.put(MultispreadConstants.RAW_LOAD_CELLS_TAG, new HashMap<String, Object>());

                } else {
                    // Retrieve multi load cell
                    Map<String, Integer> cells = decodeMultiLoadCells(data);

                    // Sum load cells and build dictionary
                    int total = 0;
                    for (Integer lcValue : cells.values()) {

                        // Only add values to the total.
                        if (lcValue != 0x80000000) {
                            total += lcValue;
                        }
                    }

                    values.put(MultispreadConstants.LOAD_CELL_TAG, total);
                    values.put(MultispreadConstants.RAW_LOAD_CELLS_TAG, cells);
                }
                break;
        }

        flagAccum |= flag;

        // If all are available, then dispatch
        if ((flagAccum & FLAG_ALL) != 0) {
            intent = new Intent(action);
            intent.putExtra(EXTRA_DATA_TYPE, MultispreadConstants.SENSORS_TAG);
            intent.putExtra(EXTRA_DATA, new HashMap<String, Object>(values));
            flagAccum = FLAG_NONE;
        }

        return intent;
    }

    private Intent buildResponseDataIntent(final String action, final BluetoothGattCharacteristic characteristic) {
        HashMap<String, Object> responseValues = new HashMap<String, Object>();
        byte[] data = characteristic.getValue();

        switch (unsignedByteToInt(data[0])) {
            case MultispreadConstants.CMD_CONTEXT_CALIBRATE:
                if (data.length == 2) {
                    responseValues.put("context", "door-calibration-status");
                    responseValues.put("status", asCalibrationStatus(unsignedByteToInt(data[1])));
                }
                break;

            case MultispreadConstants.CMD_CONTEXT_DRIVE_WHEEL:
                if (data.length == 2) {
                    responseValues.put("context", "drive-wheel-status");
                    responseValues.put("status", asDriveWheelStatus(unsignedByteToInt(data[1])));
                }
                break;

            case MultispreadConstants.CMD_CONTEXT_DOOR_DIAG:
                /*
                 0 - low battery in last 10 seconds (Linak spec 12v +/- 20% - set our threshold at 10.5V)
                 1- drive off target (drive position not at target)
                 2 - drive movement fail (drive position not at target at end of timeout)
                 3 - drive extending (engaging)
                 4 - drive retracting (disengaging)
                 5 - drive extended (engaged)
                 6 - drive retracted (disengaged)
                 */
                if (data.length == 7) {
                    int status = unsignedBytesToInt(data[6], data[5], data[4], data[3]);
                    HashMap<String, String> statusBits = new HashMap<String, String>();
                    statusBits.put("lowBattery", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_LOW_BATTERY));
                    statusBits.put("offTarget", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_OFF_TARGET));
                    statusBits.put("timeout", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_TIMEOUT));
                    statusBits.put("extending", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_EXTENDING));
                    statusBits.put("retracting", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_RETRACTING));
                    statusBits.put("extended", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_EXTENDED));
                    statusBits.put("retracted", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_RETRACTED));

                    responseValues.put("context", "door-diagnostics");
                    responseValues.put("current", unsignedBytesToInt(data[2], data[1]));
                    responseValues.put("status", statusBits);
                }
                break;

            case MultispreadConstants.CMD_CONTEXT_DW_DIAG:
                /*
                 0 - low battery in last 10 seconds (Linak spec 12v +/- 20% - set our threshold at 10.5V)
                 1 - door off target (door position not at target)
                 2 - door movement fail (door position not at target at end of timeout)
                 3 - door extending (close)
                 4 - door retracting (open)
                 5 - door fully extended (closed)
                 6 - door fully retracted (open)
                 */
                if (data.length == 7) {
                    int status = unsignedBytesToInt(data[6], data[5], data[4], data[3]);
                    HashMap<String, String> statusBits = new HashMap<String, String>();
                    statusBits.put("lowBattery", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_LOW_BATTERY));
                    statusBits.put("offTarget", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_OFF_TARGET));
                    statusBits.put("timeout", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_TIMEOUT));
                    statusBits.put("extending", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_EXTENDING));
                    statusBits.put("retracting", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_RETRACTING));
                    statusBits.put("extended", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_EXTENDED));
                    statusBits.put("retracted", checkDiagStatusBit(status, MultispreadConstants.CMD_DIAG_STATUS_RETRACTED));

                    responseValues.put("context", "drive-wheel-diagnostics");
                    responseValues.put("battery", unsignedBytesToInt(data[2], data[1]));
                    responseValues.put("status", statusBits);
                }
                break;
        }

        if (responseValues.size() > 0) {
            Intent intent = new Intent(action);
            intent.putExtra(EXTRA_DATA_TYPE, MultispreadConstants.COMMAND_RESPONSE_TAG);
            intent.putExtra(EXTRA_DATA, new HashMap<String, Object>(responseValues));
            return intent;
        } else {
            return null;
        }
    }

    private Map<String, Integer> decodeMultiLoadCells(byte[] data) {
        Map<String, Integer> cells = new HashMap<String, Integer>();

        int n = unsignedByteToInt(data[0]);
        for (int i = 0; i < n; ++i) {
            int index = (i << 2) + 1;
            int packed = unsignedBytesToInt(data[index + 3], data[index + 2], data[index + 1], data[index]);
            cells.put(String.format("port%d", packed >> 24), unpack3ByteInt(packed));
        }

        return cells;
    }

    // Unpack a 3 byte integer into a four byte. If we receive a 3 byte
    // -0 (0x0080000), then transform to a four byte -0.
    private int unpack3ByteInt(int packed) {
        int value = packed & 0x00FFFFFF;

        if ((value & 0x00800000) != 0) {
            if (value != 0x00800000) {
                return value | 0xFF800000;
            } else {
                return 0x80000000;
            }
        }

        return value;
    }

    private int uuidToFlag(String uuid) {
        if (MultispreadConstants.SPEED_CHAR_UUID.equals(uuid)) {
            return FLAG_SPINNER;

        } else if (MultispreadConstants.DOOR_OPENING_CHAR_UUID.equals(uuid)) {
            return FLAG_DOOR;

        } else if (MultispreadConstants.LOAD_CELL_CHAR_UUID.equals(uuid)) {
            return FLAG_LC;
        }

        return FLAG_NONE;
    }

    private String asCalibrationStatus(int value) {
        switch (value) {
            case MultispreadConstants.CMD_CAL_RESPONSE_SUCCESS:
                return "completed";

            case MultispreadConstants.CMD_CAL_RESPONSE_TIMEOUT:
                return "timeout";

            case MultispreadConstants.CMD_CAL_RESPONSE_CANCEL:
                return "cancelled";
        }
        return "unknown";
    }


    private String asDriveWheelStatus(int value) {
        switch (value) {
            case MultispreadConstants.CMD_DW_RESPONSE_ENGAGED:
                return "engaged";

            case MultispreadConstants.CMD_DW_RESPONSE_ENGAGING:
                return "engaging";

            case MultispreadConstants.CMD_DW_RESPONSE_DISENGAGED:
                return "disengaged";

            case MultispreadConstants.CMD_DW_RESPONSE_DISENGAGING:
                return "disengaging";

            case MultispreadConstants.CMD_DW_RESPONSE_TIMEOUT:
                return "timeout";
        }

        return "unknown";
    }

    private String checkDiagStatusBit(int value, int bit) {
        return ((value & (1 << bit)) != 0) ? "true" : "false";
    }

    /**
     * Convert an int16 into an unsigned byte array.
     */
    private byte[] unsignedBytesFromInt(int value) {
        byte[] data = new byte[2];

        data[0] = (byte) ((value & 0xff00) >> 8);
        data[1] = (byte) ((value & 0xff));

        return data;
    }

    /**
     * Convert a pair of bytes into an unsigned byte array.
     */
    private byte[] unsignedBytesFromBytePair(byte value1, byte value2) {
        byte[] data = new byte[2];

        data[0] = value1;
        data[1] = value2;

        return data;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    /**
     * Convert signed bytes to a 32-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                   + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    private int asInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ex) {
            return 0;
        }
    }
}
