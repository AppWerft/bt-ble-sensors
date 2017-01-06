package com.equenda.inmotion.sensors.ble;

import android.bluetooth.BluetoothDevice;
import org.appcelerator.kroll.KrollModule;

import java.util.Date;
import java.util.HashMap;

/**
 * The event API used to communicate over Kroll to the Titanium app through the event protocol.
 *
 * @author Jason Waring
 */
public class API {

    private KrollModule module;

    public API(KrollModule module) {
        this.module = module;
    }

    public void sendStartDiscovery() {
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("action", "discovery-started");
        fireEvent("bluetooth-le:scanning", props);
    }

    public void sendDeviceDetected(String deviceName, String address, String type, int rssi) {
        HashMap<String, Object> props = new HashMap<String, Object>();

        props.put("action", "device-detected");
        props.put("timestamp", new Date().getTime());
        props.put("name", deviceName);
        props.put("address", address);
        props.put("rssi", rssi);
        props.put("type", type);

        fireEvent("bluetooth-le:scanning", props);
    }

    public void sendStopDiscovery() {
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("action", "discovery-stopped");
        fireEvent("bluetooth-le:scanning", props);
    }

    public void sendConnectionStatus(String address, String status) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("timestamp", new Date().getTime());
        data.put("address", address);
        data.put("status", status);

        fireEvent("bluetooth-le:connection", data);
    }

    public void sendStatus(String status, String label) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("timestamp", new Date().getTime());
        data.put("status", status);
        data.put("label", label);

        fireEvent("bluetooth-le:status", data);
    }

    public void sendDeviceInfo(String address, HashMap<String, Object> values) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("timestamp", new Date().getTime());
        data.put("address", address);
        data.put("service", "deviceInfo");
        data.put("type", "deviceInfo");
        data.put("values", values);

        fireEvent("bluetooth-le:data", data);
    }

    public void sendData(String name, String serviceType, String dataType, String address, HashMap<String, Object> values) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("timestamp", new Date().getTime());
        data.put("name", name);
        data.put("address", address);
        data.put("service", serviceType);
        data.put("type", dataType);
        data.put("values", values);

        fireEvent("bluetooth-le:data", data);
    }

    private void fireEvent(String eventName, HashMap<String, Object> props) {
        HashMap<String, Object> dataProps = new HashMap<String, Object>();
        dataProps.put("data", props);

        module.fireEvent(eventName, dataProps);
    }
}
