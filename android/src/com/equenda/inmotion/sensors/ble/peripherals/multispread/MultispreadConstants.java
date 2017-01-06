package com.equenda.inmotion.sensors.ble.peripherals.multispread;

/**
 * Constants for Multispread sensor
 */
public interface MultispreadConstants {

    public static final String TYPE = "mm-controller";
    public static final String SERVICE_UUID = "1810343d-6040-4ac2-baa7-faa02b316363";

    public static final String SPEED_CHAR_UUID = "219b6ba9-d0b1-489c-92dd-d139fefdb5b6";
    public static final String DOOR_OPENING_CHAR_UUID = "4dbe6ddb-a7ba-4979-a623-f2372c102624";
    public static final String LOAD_CELL_CHAR_UUID = "ba5761a5-abfb-42fb-8f9a-a67f663c7684";
    public static final String DOOR_OPENING_TARGET_CHAR_UUID = "9863df31-3379-48d4-981c-4b008ded639d";
    public static final String COMMAND_REQUEST_CHAR_UUID = "e29e238a-c4c1-4160-8dbd-c22437081105";
    public static final String COMMAND_RESPONSE_CHAR_UUID = "aeb1c336-63d8-45e4-89dc-d17172c89036";

    public static final String SENSORS_TAG = "sensors";
    public static final String SPINNER_SPEED_TAG = "spinnerSpeed";
    public static final String BELT_SPEED_TAG = "beltSpeed";
    public static final String DOOR_OPENING_TAG = "doorOpening";
    public static final String LOAD_CELL_TAG = "loadCell";
    public static final String RAW_LOAD_CELLS_TAG = "rawLoadCells";


    public static final String COMMAND_REQUEST_TAG = "commandRequest";
    public static final String COMMAND_RESPONSE_TAG = "commandResponse";

    // Calibrate command
    public static final byte CMD_CONTEXT_CALIBRATE = 0x01;

    public static final byte CMD_CALIBRATE_START = 0x01;
    public static final byte CMD_CALIBRATE_CANCEL = 0x02;

    public static final byte CMD_CAL_RESPONSE_SUCCESS = 0x01;
    public static final byte CMD_CAL_RESPONSE_TIMEOUT = 0x02;
    public static final byte CMD_CAL_RESPONSE_CANCEL = 0x03;

    public static final String COMMAND_RESPONSES_TAG = "commandResponses";

    public static final String DOOR_CALIBRATE_COMMAND_TAG = "doorCalibration";
    public static final String DOOR_CAL_START_ACTION_TAG = "start";
    public static final String DOOR_CAL_CANCEL_ACTION_TAG = "cancel";

    // Control drive wheel
    public static final byte CMD_CONTEXT_DRIVE_WHEEL = 0x02;

    public static final byte CMD_DW_REQUEST_ENGAGE = 0x01;
    public static final byte CMD_DW_REQUEST_DISENGAGE = 0x02;
    public static final byte CMD_DW_REQUEST_STATUS = 0x03;

    public static final byte CMD_DW_RESPONSE_ENGAGED = 0x01;
    public static final byte CMD_DW_RESPONSE_DISENGAGED = 0x02;
    public static final byte CMD_DW_RESPONSE_ENGAGING = 0x03;
    public static final byte CMD_DW_RESPONSE_DISENGAGING = 0x04;
    public static final byte CMD_DW_RESPONSE_TIMEOUT = 0x05;


    public static final String DRIVE_WHEEL_COMMAND_TAG = "driveWheel";
    public static final String DW_STATUS_ACTION_TAG = "status";
    public static final String DW_ENGAGE_ACTION_TAG = "engage";
    public static final String DW_DISENGAGE_ACTION_TAG = "disengage";

    // Door and Drive Wheel Diagnostics
    public static final byte CMD_CONTEXT_DOOR_DIAG = 0x03;
    public static final byte CMD_CONTEXT_DW_DIAG = 0x04;

    public static final byte CMD_DIAG_REQUEST_ENABLE = 0x01;
    public static final byte CMD_DIAG_REQUEST_DISABLE = 0x00;

    public static final byte CMD_DIAG_STATUS_LOW_BATTERY = 0;
    public static final byte CMD_DIAG_STATUS_OFF_TARGET = 1;
    public static final byte CMD_DIAG_STATUS_TIMEOUT = 2;
    public static final byte CMD_DIAG_STATUS_EXTENDING = 3;
    public static final byte CMD_DIAG_STATUS_RETRACTING = 4;
    public static final byte CMD_DIAG_STATUS_EXTENDED = 5;
    public static final byte CMD_DIAG_STATUS_RETRACTED = 6;

    public static final String DIAGNOSTICS_COMMAND_TAG = "diagnostics";
    public static final String ENABLE_DW_DIAG_ACTION_TAG = "enable-drive-wheel";
    public static final String DISABLE_DW_DIAG_ACTION_TAG = "disable-drive-wheel";
    public static final String ENABLE_DOOR_DIAG_ACTION_TAG = "enable-door";
    public static final String DISABLE_DOOR_DIAG_ACTION_TAG = "disable-door";
}
