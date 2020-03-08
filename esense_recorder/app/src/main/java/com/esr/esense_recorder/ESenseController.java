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
 *     <li>Additional logic.</li>
 * </ul>
 */
public class ESenseController implements ESenseConnectionListener, ESenseEventListener,
        ESenseSensorListener {

    // Connection timeout in seconds
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    // Connection state (including "connecting" state)
    private ESenseConnectionState state = ESenseConnectionState.DISCONNECTED;

    // eSense manager
    private @Nullable ESenseManager eSenseManager;

    // List of listeners
    private ArrayList<ESenseListener> listeners = new ArrayList<>();

    public void addListener(ESenseListener listener) {
        synchronized (this) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(ESenseListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    public ESenseConnectionState getState() {
        return state;
    }

    public void connect(String name, Context context) {
        if (eSenseManager != null) {
            eSenseManager.disconnect();
        }
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

    public void disconnect() {
        if (eSenseManager != null &&
                state != ESenseConnectionState.DISCONNECTED) {
            state = ESenseConnectionState.DISCONNECTED;
            eSenseManager.disconnect();
        }
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
            l.onButtonEventChanged(pressed);
        }
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval,
                                                         int maxAdvertisementInterval,
                                                         int minConnectionInterval,
                                                         int maxConnectionInterval) {
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
            l.onAdvertisementAndConnectionIntervalRead(minAdvertisementInterval,
                    maxAdvertisementInterval, minConnectionInterval, maxConnectionInterval);
        }
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
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
            l.onDeviceNameRead(deviceName);
        }
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
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
