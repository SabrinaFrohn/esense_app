package com.esr.esense_recorder;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseEventListener;
import io.esense.esenselib.ESenseManager;
import io.esense.esenselib.ESenseSensorListener;

/**
 * Encapsulation of the <code>ESenseManager</code> to allows for:
 * <ul>
 *     <li>Multiple listeners,</li>
 *     <li>Instantiation without argument (in particular the context and name of the device),</li>
 *     <li>Storing last known values.</li>
 * </ul>
 */
public class ESenseController implements ESenseConnectionListener, ESenseEventListener,
        ESenseSensorListener {

    // Connection timeout in seconds
    private static final int CONNECTION_TIMEOUT_MS = 1500;

    // Connection state (including "connecting" state)
    private ESenseConnectionState state = ESenseConnectionState.DISCONNECTED;

    // eSense manager
    private @Nullable ESenseManager eSenseManager;

    // List of listeners
    private ArrayList<ESenseListener> listeners = new ArrayList<>();

    // Last known configuration
    private ESenseConfig eSenseConfig;

    // Connected device name
    private String deviceName;

    /**
     * Adds a listener to eSense events.
     *
     * @param listener the listener to add.
     */
    public void addListener(ESenseListener listener) {
        synchronized (this) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove.
     */
    public void removeListener(ESenseListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns the connection state of the eSense device.
     *
     * @return the connection state.
     */
    public ESenseConnectionState getState() {
        return state;
    }

    /**
     * Reads asynchronously the eSense sensor configuration.
     *
     * @return <code>true</code> if the request hqs been sent.
     */
    public boolean readESenseConfig() {
        if (eSenseManager != null && state == ESenseConnectionState.CONNECTED) {
            return eSenseManager.getSensorConfig();
        }
        return false;
    }

    /**
     * Returns the last known eSense configuration.
     *
     * @return the last known eSense configuration.
     */
    public ESenseConfig getESenseConfig() {
        return eSenseConfig;
    }

    /**
     * Returns the name of the connected device, or <code>null</code> if not connected.
     *
     * @return the name of the connected device.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Connects a eSense device.
     *
     * @param name the name of the device.
     * @param context the application context to establish the connection.
     */
    public void connect(String name, Context context) {
        clearStoredSensorData();
        if (eSenseManager != null) {
            eSenseManager.disconnect();
        }
        // Keep device name
        deviceName = name;
        // Set state
        state = ESenseConnectionState.CONNECTING;
        // Notify connection attempt
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onConnecting();
        }
        // Connection
        eSenseManager = new ESenseManager(name, context, this);
        eSenseManager.connect(CONNECTION_TIMEOUT_MS);
    }

    /**
     * Disconnects the eSense device.
     */
    public void disconnect() {
        clearStoredSensorData();
        if (eSenseManager != null &&
                state != ESenseConnectionState.DISCONNECTED) {
            state = ESenseConnectionState.DISCONNECTED;
            // Disconnect
            eSenseManager.disconnect();
        }
    }

    /**
     * Clears the sensor data.
     */
    private void clearStoredSensorData() {
        eSenseConfig = null;
        deviceName = null;
        // TODO: To be completed
    }

    // *** Implementation of the ESenseManager listeners ***

    @Override
    public void onDeviceFound(ESenseManager manager) {
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onDeviceFound(manager);
        }
    }

    @Override
    public void onDeviceNotFound(ESenseManager manager) {
        // Set state
        state = ESenseConnectionState.DISCONNECTED;
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onDeviceNotFound(manager);
        }
    }

    @Override
    public void onConnected(ESenseManager manager) {
        // Set state
        state = ESenseConnectionState.CONNECTED;
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onConnected(manager);
        }
    }

    @Override
    public void onDisconnected(ESenseManager manager) {
        clearStoredSensorData();
        // Set state
        state = ESenseConnectionState.DISCONNECTED;
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onDisconnected(manager);
        }
    }

    @Override
    public void onBatteryRead(double voltage) {
        // TODO
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onBatteryRead(voltage);
        }
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onButtonEventChanged(pressed);
        }
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval,
                                                         int maxAdvertisementInterval,
                                                         int minConnectionInterval,
                                                         int maxConnectionInterval) {
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onAdvertisementAndConnectionIntervalRead(minAdvertisementInterval,
                    maxAdvertisementInterval, minConnectionInterval, maxConnectionInterval);
        }
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onDeviceNameRead(deviceName);
        }
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        // Store data
        eSenseConfig = config;
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onSensorConfigRead(config);
        }
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        // TODO
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onAccelerometerOffsetRead(offsetX, offsetY, offsetZ);
        }
    }

    @Override
    public void onSensorChanged(ESenseEvent evt) {
        // TODO
        // Inform listeners
        ArrayList<ESenseListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (ESenseListener l: targets) {
            l.onSensorChanged(evt);
        }
    }
}
