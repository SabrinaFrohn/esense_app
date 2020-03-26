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
import io.esense.esenselib.SamplingStatus;

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

    // Connected device name
    private String deviceName;

    // Last known sensors configuration
    private ESenseConfig eSenseConfig;

    // Sensor notification state
    private boolean sensorNotificationsActive = false;
    private long lastNotificationNanoTime = -1;
    private long lastNotificationPeriodNano = -1;

    // Sampling rate for sensor notifications
    private int samplingRate = -1;

    // Last known sensor data
    private ESenseEvent lastSensorData = null;

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
     * Returns the state of sensor notifications.
     *
     * @return <code>true</code> if sensor notifications are active, <code>false</code> otherwise.
     */
    public boolean areSensorNotificationsActive() {
        return sensorNotificationsActive;
    }

    /**
     * Returns the requested sampling rate for sensor notifications in milliseconds. Returns a
     * negative value if no sampling rate is defined.
     *
     * @return the requested sampling rate for sensor notifications in Hz.
     */
    public int getSamplingRate() {
        return samplingRate;
    }

    /**
     * Returns the period in seconds between the last two sensor sample. Returns a negative value if
     * there is no samples.
     * @return the period in seconds between the last two sensor sample.
     */
    public double getLastSamplePeriod() {
        if (lastNotificationPeriodNano <= 0) {
            return -1.;
        } else {
            return ((double)lastNotificationPeriodNano)/1.E9;
        }
    }

    /**
     * Returns the last known  raw sensor data. Retrieving sensor data with this method is not
     * recommended for the gyroscope due to the potential lost of data sample.
     *
     * @return the last known raw sensor data.
     */
    public ESenseEvent getLastSensorData() {
        return lastSensorData;
    }

    /**
     * Returns the converted sensor data.
     * @return the converted sensor data.
     */
    public double[][] getConvertedSensorData() {
        if (eSenseConfig == null || lastSensorData == null) {
            return null;
        } else {
            double[] convAcc = lastSensorData.convertAccToG(eSenseConfig);
            double[] convGyro = lastSensorData.convertGyroToDegPerSecond(eSenseConfig);
            double[][] conv = new double[2][3];
            System.arraycopy(convAcc, 0, conv[0], 0, 3);
            System.arraycopy(convGyro, 0, conv[1], 0, 3);
            return conv;
        }
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
     * Starts the sensor notifications.
     *
     * @param samplingRate The sampling rate of the notification in Hz, in range [1-100].
     * @return <code>true</code> if the request has been sent correctly.
     */
    public boolean startSensorNotifications(int samplingRate) {
        if (eSenseManager != null &&
                state == ESenseConnectionState.CONNECTED) {
            SamplingStatus status = eSenseManager.registerSensorListener(this,
                    samplingRate);
            if (status == SamplingStatus.STARTED) {
                sensorNotificationsActive = true;
                this.samplingRate = samplingRate;
                // Notify listeners
                ArrayList<ESenseListener> targets;
                synchronized (this) {
                    targets = new ArrayList<>(listeners);
                }
                for (ESenseListener l: targets) {
                    l.onSensorNotificationsStarted(this.samplingRate);
                }
            }
        }
        return false;
    }

    /**
     * Stops the sensor notifications.
     */
    public void stopSensorNotifications() {
        if (eSenseManager != null &&
                state == ESenseConnectionState.CONNECTED) {
            if (sensorNotificationsActive) {
                eSenseManager.unregisterEventListener();
                // Notify listeners
                ArrayList<ESenseListener> targets;
                synchronized (this) {
                    targets = new ArrayList<>(listeners);
                }
                for (ESenseListener l: targets) {
                    l.onSensorNotificationsStopped();
                }
            }
        }
    }

    /**
     * Clears the sensor data.
     */
    private void clearStoredSensorData() {
        eSenseConfig = null;
        deviceName = null;
        sensorNotificationsActive = false;
        samplingRate = -1;
        lastSensorData = null;
        lastNotificationNanoTime = -1;
        lastNotificationPeriodNano = -1;
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
        eSenseManager.registerEventListener(this);
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
        lastSensorData = evt;
        long nanoTime = System.nanoTime();
        if (lastNotificationNanoTime > 0) {
            lastNotificationPeriodNano = nanoTime-lastNotificationNanoTime;
        } else {
            lastNotificationPeriodNano = -1;
        }
        lastNotificationNanoTime = nanoTime;
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
