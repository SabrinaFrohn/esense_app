package com.esr.esense_recorder;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseManager;

public class MainActivity extends BluetoothCheckActivity implements BluetoothCheckCallback,
        ESenseListener {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "eSenseRecorder-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // UI components
    private TextView connectionStateLabel;
    private TextView gyroRangeLabel;
    private TextView gyroLPFLabel;
    private TextView accRangeLabel;
    private TextView accLPFLabel;

    // Format for label


    // eSense controller
    ESenseController eSenseController = new ESenseController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // References to UI components
        connectionStateLabel = findViewById(R.id.activity_main_connection_state_label);
        gyroRangeLabel = findViewById(R.id.activity_main_gyro_range_label);
        gyroLPFLabel = findViewById(R.id.activity_main_gyro_lpf_label);
        accRangeLabel = findViewById(R.id.activity_main_acc_range_label);
        accLPFLabel = findViewById(R.id.activity_main_acc_lpf_label);

        // *** UI event handlers ***

        // Connect button
        Button connectButton = findViewById(R.id.activity_main_connect_button);
        if (connectButton != null) {
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eSenseController.getState() == ESenseConnectionState.DISCONNECTED) {
                        // First check Bluetooth
                        activateBluetooth(MainActivity.this);
                    }
                }
            });
        }

        // Disconnect button
        Button disconnectButton = findViewById(R.id.activity_main_disconnect_button);
        if (disconnectButton != null) {
            disconnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eSenseController.getState() != ESenseConnectionState.DISCONNECTED) {
                        eSenseController.disconnect();
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        eSenseController.addListener(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        eSenseController.removeListener(this);
        if (isFinishing()) {
            // Disconnect
            eSenseController.disconnect();
        }
    }

    /**
     * Shows a toast.
     *
     * @param message The message to display in the toast.
     */
    private void showToast(@NonNull final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBluetoothReady() {
        showToast(getString(R.string.toast_message_bt_ready));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Retrieve the list of Paired devices
                ArrayList<BluetoothDevice> devices = new ArrayList<>(
                        BluetoothAdapter.getDefaultAdapter().getBondedDevices());
                if (devices.size() == 0) {
                    showToast(getString(R.string.toast_message_no_paired_device));
                    return;
                }
                // Get name
                final String[] deviceNames = new String[devices.size()];
                for (int i=0; i<devices.size(); i++) {
                    deviceNames[i] = devices.get(i).getName();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialog_device_selection_title);
                builder.setItems(deviceNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Connect when a name is selected
                        try {
                            MainActivity.this.eSenseController.connect(
                                    deviceNames[which], MainActivity.this);
                        } catch (Exception e) {
                            showToast(getString(R.string.toast_message_device_name_error));
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public void onBluetoothActivationRejected() {
        showToast(getString(R.string.toast_message_bt_activation_rejected));
    }

    @Override
    public void onBluetoothActivationFailed() {
        showToast(getString(R.string.toast_message_bt_activation_failed));
    }

    /**
     * Updates all UI components according to the eSense state.
     */
    private void updateUI() {
        updateConnectionPanel();
        updateIMUConfigurationPanel();
    }

    /**
     * Updates the connection panel UI.
     */
    private void updateConnectionPanel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectionStateLabel != null) {
                    connectionStateLabel.setText(eSenseController.getState().toString());
                }
            }
        });
    }

    /**
     * Updates the IMU configuration panel.
     */
    private void updateIMUConfigurationPanel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gyroRangeLabel == null || gyroLPFLabel == null ||
                        accRangeLabel == null || accLPFLabel == null) {
                    return;
                }
                ESenseConfig config = eSenseController.getESenseConfig();
                if (config == null ||
                        eSenseController.getState() != ESenseConnectionState.CONNECTED) {
                    gyroRangeLabel.setText(getString(R.string.imu_no_value_text));
                    gyroLPFLabel.setText(getString(R.string.imu_no_value_text));
                    accRangeLabel.setText(getString(R.string.imu_no_value_text));
                    accLPFLabel.setText(getString(R.string.imu_no_value_text));
                } else {
                    switch (config.getGyroRange()) {
                        case DEG_250:
                            gyroRangeLabel.setText(getString(R.string.gyro_range_250));
                            break;
                        case DEG_500:
                            gyroRangeLabel.setText(getString(R.string.gyro_range_500));
                            break;
                        case DEG_1000:
                            gyroRangeLabel.setText(getString(R.string.gyro_range_1000));
                            break;
                        case DEG_2000:
                            gyroRangeLabel.setText(getString(R.string.gyro_range_2000));
                            break;
                    }
                    switch (config.getGyroLPF()) {
                        case BW_250:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_250));
                            break;
                        case BW_184:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_184));
                            break;
                        case BW_92:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_92));
                            break;
                        case BW_41:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_41));
                            break;
                        case BW_20:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_20));
                            break;
                        case BW_10:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_10));
                            break;
                        case BW_5:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_5));
                            break;
                        case BW_3600:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_3600));
                            break;
                        case DISABLED:
                            gyroLPFLabel.setText(getString(R.string.gyro_lpf_disabled));
                            break;
                    }
                    switch (config.getAccRange()) {
                        case G_2:
                            accRangeLabel.setText(getString(R.string.acc_range_2G));
                            break;
                        case G_4:
                            accRangeLabel.setText(getString(R.string.acc_range_4G));
                            break;
                        case G_8:
                            accRangeLabel.setText(getString(R.string.acc_range_8G));
                            break;
                        case G_16:
                            accRangeLabel.setText(getString(R.string.acc_range_16G));
                            break;
                    }
                    switch (config.getAccLPF()) {
                        case BW_460:
                            accLPFLabel.setText(getString(R.string.acc_lpf_460));
                            break;
                        case BW_184:
                            accLPFLabel.setText(getString(R.string.acc_lpf_184));
                            break;
                        case BW_92:
                            accLPFLabel.setText(getString(R.string.acc_lpf_92));
                            break;
                        case BW_41:
                            accLPFLabel.setText(getString(R.string.acc_lpf_41));
                            break;
                        case BW_20:
                            accLPFLabel.setText(getString(R.string.acc_lpf_20));
                            break;
                        case BW_10:
                            accLPFLabel.setText(getString(R.string.acc_lpf_10));
                            break;
                        case BW_5:
                            accLPFLabel.setText(getString(R.string.acc_lpf_5));
                            break;
                        case DISABLED:
                            accLPFLabel.setText(getString(R.string.acc_lpf_disabled));
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void onDeviceFound(ESenseManager manager) {
        showToast(getString(R.string.toast_message_device_found));
        updateUI();
    }

    @Override
    public void onDeviceNotFound(ESenseManager manager) {
        showToast(getString(R.string.toast_message_device_not_found));
        updateUI();
    }

    @Override
    public void onConnected(ESenseManager manager) {
        // Read IMU config
        eSenseController.readESenseConfig();
        // Show toast and update UI
        showToast(getString(R.string.toast_message_device_connected));
        updateUI();
    }

    @Override
    public void onDisconnected(ESenseManager manager) {
        showToast(getString(R.string.toast_message_device_disconnected));
        updateUI();
    }

    @Override
    public void onBatteryRead(double voltage) {
        // No monitoring of the battery voltage
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        showToast(getString(R.string.toast_message_esense_button_pressed));
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval,
                                                         int maxAdvertisementInterval,
                                                         int minConnectionInterval,
                                                         int maxConnectionInterval) {
        // TODO
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
        // Nothing to do
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        updateIMUConfigurationPanel();
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        // TODO
    }

    @Override
    public void onSensorChanged(ESenseEvent evt) {
        // TODO
    }

    @Override
    public void onConnecting() {
        updateUI();
    }
}

