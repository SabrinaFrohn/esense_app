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
    private Button connectButton;
    private Button disconnectButton;
    private Button readConfigButton;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Button startSensorButton;
    private Button stopSensorButton;
    private TextView rawAccXLabel;
    private TextView rawAccYLabel;
    private TextView rawAccZLabel;
    private TextView convAccXLabel;
    private TextView convAccYLabel;
    private TextView convAccZLabel;
    private TextView rawGyroXLabel;
    private TextView rawGyroYLabel;
    private TextView rawGyroZLabel;
    private TextView convGyroXLabel;
    private TextView convGyroYLabel;
    private TextView convGyroZLabel;
    private TextView samplingRateLabel;
    private TextView notifPeriodLabel;

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
        rawAccXLabel = findViewById(R.id.activity_main_raw_acc_x_label);
        rawAccYLabel = findViewById(R.id.activity_main_raw_acc_y_label);
        rawAccZLabel = findViewById(R.id.activity_main_raw_acc_z_label);
        convAccXLabel = findViewById(R.id.activity_main_conv_acc_x_label);
        convAccYLabel = findViewById(R.id.activity_main_conv_acc_y_label);
        convAccZLabel = findViewById(R.id.activity_main_conv_acc_z_label);
        rawGyroXLabel = findViewById(R.id.activity_main_raw_gyro_x_label);
        rawGyroYLabel = findViewById(R.id.activity_main_raw_gyro_y_label);
        rawGyroZLabel = findViewById(R.id.activity_main_raw_gyro_z_label);
        convGyroXLabel = findViewById(R.id.activity_main_conv_gyro_x_label);
        convGyroYLabel = findViewById(R.id.activity_main_conv_gyro_y_label);
        convGyroZLabel = findViewById(R.id.activity_main_conv_gyro_z_label);
        samplingRateLabel = findViewById(R.id.activity_main_sampling_rate_label);
        notifPeriodLabel = findViewById(R.id.activity_main_notification_period_label);

        // *** UI event handlers ***

        // Connect button
        connectButton = findViewById(R.id.activity_main_connect_button);
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
        disconnectButton = findViewById(R.id.activity_main_disconnect_button);
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

        // Read IMU config button
        readConfigButton = findViewById(R.id.activity_main_read_imu_config_button);
        if (readConfigButton != null) {
            readConfigButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eSenseController.getState() == ESenseConnectionState.CONNECTED) {
                        eSenseController.readESenseConfig();
                    }
                }
            });
        }

        // Start record button
        startRecordButton = findViewById(R.id.activity_main_start_record_button);
        if (startRecordButton != null) {
            startRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                }
            });
        }

        // Stop record button
        stopRecordButton = findViewById(R.id.activity_main_stop_record_button);
        if (stopRecordButton != null) {
            stopRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                }
            });
        }

        // Start sensors button
        startSensorButton = findViewById(R.id.activity_main_start_sensor_button);
        if (startSensorButton != null) {
            startSensorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                }
            });
        }

        // Stop sensors button
        stopSensorButton = findViewById(R.id.activity_main_stop_sensor_button);
        if (stopSensorButton != null) {
            stopSensorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
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
        updateLoggerPanel();
        updateSensorDataPanel();
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
                if (connectButton != null && disconnectButton != null) {
                    if (eSenseController.getState() == ESenseConnectionState.DISCONNECTED) {
                        connectButton.setEnabled(true);
                        disconnectButton.setEnabled(false);
                    } else {
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                    }
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
                // Set button state
                if (readConfigButton != null) {
                    if (eSenseController.getState() == ESenseConnectionState.CONNECTED) {
                        readConfigButton.setEnabled(true);
                    } else {
                        readConfigButton.setEnabled(false);
                    }
                }
                // Set config labels
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

    /**
     * Updates the logger panel.
     */
    private void updateLoggerPanel() {
        // TODO
    }

    /**
     * Update the sensor data panel.
     */
    private void updateSensorDataPanel() {
        // TODO
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
        if (!eSenseController.readESenseConfig()) {
            showToast(getString(R.string.toast_message_read_config_failed));
        }
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
        // Nothing to do
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
        // Nothing to do
    }

    @Override
    public void onSensorChanged(ESenseEvent evt) {
        updateSensorDataPanel();
    }

    @Override
    public void onConnecting() {
        updateUI();
    }

    @Override
    public void onSensorNotificationsStarted(int samplingRate) {
        updateSensorDataPanel();
    }

    @Override
    public void onSensorNotificationsStopped() {
        updateSensorDataPanel();
    }
}

