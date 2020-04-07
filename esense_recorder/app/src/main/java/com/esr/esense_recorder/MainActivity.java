package com.esr.esense_recorder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
    private TextView recordStateLabel;
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
    private TextView notifRateLabel;

    // eSense controller
    ESenseController eSenseController = new ESenseController();

    // Logger (null is not logging)
    private SimpleLogger logger;
    private long startLogNanoTime;

    // Log parameters
    private @NonNull String logSeparator = "\t";
    private @NonNull String logTerminator = "\n";

    // Handler for regular updates of sensor fields
    Handler uiUpdateHandler = new Handler();
    public static final long UI_UPDATE_DELAY_MILLIS = 500;

    // Decimal formats
    private DecimalFormat convAccFormat = new DecimalFormat("00.00");
    private DecimalFormat convGyroFormat = new DecimalFormat("00.00");

    // Flag for pending log (after config read and start sensor)
    private boolean pendingStartLog = false;

    private String LAST_SAMPLING_RATE_KEY = "LAST_SAMPLING_RATE_KEY";
    private int lastSamplingRate = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // References to UI components
        connectionStateLabel = findViewById(R.id.activity_main_connection_state_label);
        recordStateLabel = findViewById(R.id.activity_main_record_state_label);
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
        notifRateLabel = findViewById(R.id.activity_main_notification_rate_label);

        // Retrieve log parameters
        logSeparator = getString(R.string.log_field_separator);
        logTerminator = getString(R.string.log_line_terminator);

        // Init. formats
        convAccFormat = new DecimalFormat(getString(R.string.conv_acc_data_decimal_format));
        convGyroFormat = new DecimalFormat(getString(R.string.conv_gyro_data_decimal_format));

        // Retrieve defaults
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastSamplingRate = prefs.getInt(LAST_SAMPLING_RATE_KEY, lastSamplingRate);

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
                        if (logger != null && logger.isLogging()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    MainActivity.this);
                            builder.setMessage(R.string.dialog_cancel_log_message);
                            builder.setPositiveButton(
                                    R.string.dialog_cancel_log_yes_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            eSenseController.disconnect();
                                        }
                                    });
                            builder.setNegativeButton(
                                    R.string.dialog_cancel_log_no_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Cancel
                                        }
                                    });
                            builder.create().show();
                        } else {
                            eSenseController.disconnect();
                        }
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
                    if (logger != null && logger.isLogging()) {
                        // Ignore when already logging
                        return;
                    }
                    if (eSenseController.getState() == ESenseConnectionState.CONNECTED ){
                        // Check config
                        if (eSenseController.getESenseConfig() == null) {
                            pendingStartLog = true;
                            eSenseController.readESenseConfig();
                        } else if (!eSenseController.areSensorNotificationsActive()) {
                            pendingStartLog = true;
                            startSensors();
                        } else {
                            startLog();
                        }
                    } else {
                        showToast(getString(R.string.toast_message_no_device_connected));
                    }
                }
            });
        }

        // Stop record button
        stopRecordButton = findViewById(R.id.activity_main_stop_record_button);
        if (stopRecordButton != null) {
            stopRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
                    logger.log(MainActivity.this, logSeparator, logTerminator,
                            String.format(
                                    Locale.getDefault(), "%d", elapsedMillis),
                            getString(R.string.log_stop_message));
                    logger.closeLog(MainActivity.this);
                    logger = null;
                    updateLoggerPanel();
                }
            });
        }

        // Start sensors button
        startSensorButton = findViewById(R.id.activity_main_start_sensor_button);
        if (startSensorButton != null) {
            startSensorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eSenseController.getState() == ESenseConnectionState.CONNECTED ){
                        if (!eSenseController.areSensorNotificationsActive()) {
                            startSensors();
                        }
                    }
                }
            });
        }

        // Stop sensors button
        stopSensorButton = findViewById(R.id.activity_main_stop_sensor_button);
        if (stopSensorButton != null) {
            stopSensorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eSenseController.getState() == ESenseConnectionState.CONNECTED ){
                        if (!eSenseController.areSensorNotificationsActive()) {
                            // Ignore when already stopped
                            return;
                        }
                        if (logger != null && logger.isLogging()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    MainActivity.this);
                            builder.setMessage(R.string.dialog_cancel_log_message);
                            builder.setPositiveButton(
                                    R.string.dialog_cancel_log_yes_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            eSenseController.stopSensorNotifications();
                                        }
                                    });
                            builder.setNegativeButton(
                                    R.string.dialog_cancel_log_no_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Cancel
                                        }
                                    });
                            builder.create().show();
                        } else {
                            eSenseController.stopSensorNotifications();
                        }
                    }
                }
            });
        }
    }

    /**
     * Starts the log of sensor events.
     */
    private void startLog() {
        pendingStartLog = false;
        if (logger != null && logger.isLogging()) {
            logger.closeLog(this);
        }
        // Create logger
        String folderName = getString(R.string.log_folder);
        SimpleDateFormat logFileFormat = new SimpleDateFormat(
                getString(R.string.log_file_date_pattern), Locale.getDefault());
        logger = new SimpleLogger(folderName, logFileFormat.format(new Date()));
        // First log
        startLogNanoTime = System.nanoTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getString(R.string.log_start_date_pattern), Locale.getDefault());
        String date = dateFormat.format(new Date());
        // TODO add configuration details
        ESenseConfig config = eSenseController.getESenseConfig();
        if (config != null) {
            logger.log(this, logSeparator, logTerminator,
                    "0",
                    "acc. range",
                    config.getAccRange().toString()
                    );
            logger.log(this, logSeparator, logTerminator,
                    "0",
                    "gyro. range",
                    config.getGyroRange().toString()
            );
            logger.log(this, logSeparator, logTerminator,
                    "0",
                    "acc. LPF",
                    config.getAccLPF().toString()
            );
            logger.log(this, logSeparator, logTerminator,
                    "0",
                    "gyro. LPF",
                    config.getGyroLPF().toString()
            );
        }
        if (!logger.log(this, logSeparator, logTerminator,
                "0",
                getString(R.string.log_start_message),
                date)) {
            // Log failed
            logger.closeLog(this);
            logger = null;
            showToast(getString(R.string.toast_log_failed));
        }
        updateLoggerPanel();
    }

    /**
     * Asks for the sampling rate and start the sensors (asynchronously)
     */
    private void startSensors() {
        if (eSenseController.areSensorNotificationsActive()) {
            if (pendingStartLog) {
                startLog();
            }
            return;
        }
        // Dialog for sampling rate
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_sampling_rate_title);
        final EditText edittext = new EditText(MainActivity.this);
        edittext.setText(String.format(Locale.getDefault(), "%d", lastSamplingRate));
        edittext.setInputType(2); // Number keyboard
        builder.setView(edittext);
        builder.setPositiveButton(R.string.dialog_sampling_rate_ok_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Retrieve sampling rate
                        String rateString = edittext.getText().toString().trim();
                        try {
                            int rate = Integer.parseInt(rateString);
                            if (rate < 1 || rate > 100) {
                                showToast(getString(R.string.toast_sampling_rate_out_of_bounds));
                                pendingStartLog = false;
                            } else {
                                // Save value
                                SharedPreferences prefs = PreferenceManager
                                        .getDefaultSharedPreferences(MainActivity.this);
                                prefs.edit().putInt(LAST_SAMPLING_RATE_KEY, lastSamplingRate)
                                        .apply();
                                // Start sensors
                                if (!eSenseController.startSensorNotifications(
                                        lastSamplingRate)) {
                                    showToast(
                                            getString(R.string.toast_message_start_sensor_failed));
                                }
                            }
                        } catch (Exception e) {
                            showToast(getString(R.string.toast_sampling_rate_illegal));
                            pendingStartLog = false;
                        }
                    }
                });
        builder.setNegativeButton(
                R.string.dialog_sampling_rate_cancel_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel
                        pendingStartLog = false;
                    }
                });
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        eSenseController.addListener(this);
        updateUI();
        // Start handler for regular sensor fields updates
        uiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (eSenseController != null &&
                            eSenseController.areSensorNotificationsActive()) {
                        updateSensorDataPanel();
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Failed to update UI.", e);
                } finally {
                    uiUpdateHandler.postDelayed(this, UI_UPDATE_DELAY_MILLIS);
                }
            }
        }, UI_UPDATE_DELAY_MILLIS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        eSenseController.removeListener(this);
        // Stop UI update handler
        uiUpdateHandler.removeCallbacksAndMessages(null);
        // Close connection and logger on finishing
        if (isFinishing()) {
            // Stop log
            if (logger != null && logger.isLogging()) {
                long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
                logger.log(MainActivity.this, logSeparator, logTerminator,
                        String.format(
                                Locale.getDefault(), "%d", elapsedMillis),
                        getString(R.string.log_stop_message));
                logger.closeLog(this);
                logger = null;
            }
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
                    gyroRangeLabel.setText(getString(R.string.unknown_value_text));
                    gyroLPFLabel.setText(getString(R.string.unknown_value_text));
                    accRangeLabel.setText(getString(R.string.unknown_value_text));
                    accLPFLabel.setText(getString(R.string.unknown_value_text));
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (eSenseController != null &&
                        startRecordButton != null && stopRecordButton != null) {
                    if (eSenseController.getState() != ESenseConnectionState.CONNECTED) {
                        startRecordButton.setEnabled(false);
                        stopRecordButton.setEnabled(false);
                    } else if (logger == null || !logger.isLogging()) {
                        startRecordButton.setEnabled(true);
                        stopRecordButton.setEnabled(false);
                    } else {
                        startRecordButton.setEnabled(false);
                        stopRecordButton.setEnabled(true);
                    }
                }
                if (recordStateLabel != null) {
                    if (logger == null || !logger.isLogging()) {
                        recordStateLabel.setText(R.string.activity_main_not_recording_text);
                        recordStateLabel.setTextColor(getColor(R.color.colorLabelDisabled));
                    } else {
                        recordStateLabel.setText(R.string.activity_main_recording_text);
                        recordStateLabel.setTextColor(getColor(R.color.colorAccent));
                    }
                }
            }
        });
    }

    /**
     * Update the sensor data panel.
     */
    private void updateSensorDataPanel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (eSenseController == null) {
                    return;
                }
                ESenseEvent rawSensorData = eSenseController.getLastSensorData();
                double[][] convSensorData = eSenseController.getConvertedSensorData();
                // Start stop buttons
                if (startSensorButton != null && stopSensorButton != null) {
                    if (eSenseController.getState() != ESenseConnectionState.CONNECTED) {
                        startSensorButton.setEnabled(false);
                        stopSensorButton.setEnabled(false);
                    } else if (eSenseController.areSensorNotificationsActive()) {
                        startSensorButton.setEnabled(false);
                        stopSensorButton.setEnabled(true);
                    } else {
                        startSensorButton.setEnabled(true);
                        stopSensorButton.setEnabled(false);
                    }
                }
                // Sampling rate
                if (samplingRateLabel != null) {
                    int rate = eSenseController.getSamplingRate();
                    if (rate <= 0) {
                        samplingRateLabel.setText(R.string.unknown_value_text);
                    } else {
                        samplingRateLabel.setText(
                                String.format(getString(R.string.sampling_rate_format), rate));
                    }
                }
                // Notification rate
                if (notifRateLabel != null) {
                    double notifPeriod = eSenseController.getLastSamplePeriod();
                    if (notifPeriod <= 0) {
                        notifRateLabel.setText(R.string.unknown_value_text);
                    } else {
                        double rate = 1./notifPeriod;
                        notifRateLabel.setText(
                                String.format(getString(R.string.notif_rate_format), rate));
                    }
                }
                // Raw data
                if (rawAccXLabel != null && rawAccYLabel != null && rawAccZLabel != null) {
                    if (rawSensorData != null) {
                        rawAccXLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getAccel()[0]));
                        rawAccYLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getAccel()[1]));
                        rawAccZLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getAccel()[2]));
                    } else {
                        rawAccXLabel.setText(R.string.unknown_value_text);
                        rawAccYLabel.setText(R.string.unknown_value_text);
                        rawAccZLabel.setText(R.string.unknown_value_text);
                    }
                }
                if (rawGyroXLabel != null && rawGyroYLabel != null && rawGyroZLabel != null) {
                    if (rawSensorData != null) {
                        rawGyroXLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getGyro()[0]));
                        rawGyroYLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getGyro()[1]));
                        rawGyroZLabel.setText(String.format(Locale.getDefault(),
                                "%d", rawSensorData.getGyro()[2]));
                    } else {
                        rawGyroXLabel.setText(R.string.unknown_value_text);
                        rawGyroYLabel.setText(R.string.unknown_value_text);
                        rawGyroZLabel.setText(R.string.unknown_value_text);
                    }
                }
                // Converted data
                if (convAccXLabel != null && convAccYLabel != null && convAccZLabel != null) {
                    if (convSensorData != null) {
                        convAccXLabel.setText(convAccFormat.format(convSensorData[0][0]));
                        convAccYLabel.setText(convAccFormat.format(convSensorData[0][1]));
                        convAccZLabel.setText(convAccFormat.format(convSensorData[0][2]));
                    } else {
                        convAccXLabel.setText(R.string.unknown_value_text);
                        convAccYLabel.setText(R.string.unknown_value_text);
                        convAccZLabel.setText(R.string.unknown_value_text);
                    }
                }
                if (convGyroXLabel != null && convGyroYLabel != null && convGyroZLabel != null) {
                    if (convSensorData != null) {
                        convGyroXLabel.setText(convGyroFormat.format(convSensorData[1][0]));
                        convGyroYLabel.setText(convGyroFormat.format(convSensorData[1][1]));
                        convGyroZLabel.setText(convGyroFormat.format(convSensorData[1][2]));
                    } else {
                        convGyroXLabel.setText(R.string.unknown_value_text);
                        convGyroYLabel.setText(R.string.unknown_value_text);
                        convGyroZLabel.setText(R.string.unknown_value_text);
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
        if (!eSenseController.readESenseConfig()) {
            showToast(getString(R.string.toast_message_read_config_failed));
        }
        // Show toast and update UI
        showToast(getString(R.string.toast_message_device_connected));
        updateUI();
    }

    @Override
    public void onDisconnected(ESenseManager manager) {
        // Stop log
        if (logger != null && logger.isLogging()) {
            long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
            logger.log(this, logSeparator, logTerminator,
                    String.format(
                            Locale.getDefault(), "%d", elapsedMillis),
                    getString(R.string.log_disconnection_message));
            logger.closeLog(this);
            logger = null;
        }
        // Toast and UI update
        showToast(getString(R.string.toast_message_device_disconnected));
        updateUI();
    }

    @Override
    public void onBatteryRead(double voltage) {
        // No monitoring of the battery voltage
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        // Log event
        if (logger != null && logger.isLogging()) {
            long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
            String elapsed = String.format(Locale.getDefault(), "%d", elapsedMillis);
            if (!logger.log(this, logSeparator, logTerminator,
                    elapsed,
                    getString(R.string.log_button_event_message))) {
                // Log failed
                logger.closeLog(this);
                logger = null;
                showToast(getString(R.string.toast_log_failed));
            }
        }
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
        if (pendingStartLog) {
            startSensors();
        }
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        // Nothing to do
    }

    @Override
    public void onSensorChanged(ESenseEvent evt) {
        // Log sensor data
        if (logger != null) {
            long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
            String elapsed = String.format(Locale.getDefault(), "%d", elapsedMillis);
            ESenseConfig config = eSenseController.getESenseConfig();
            double[] convAcc = null;
            double[] convGyro = null;
            if (config != null) {
                convAcc = evt.convertAccToG(config);
                convGyro = evt.convertGyroToDegPerSecond(config);
            }
            if (!logger.log(this, logSeparator, logTerminator,
                    elapsed,
                    getString(R.string.log_sensor_event_message),
                    evt.getAccel()[0], evt.getAccel()[1], evt.getAccel()[2],
                    evt.getGyro()[0], evt.getGyro()[1], evt.getGyro()[2],
                    (convAcc==null)?("-"):(convAcc[0]),
                    (convAcc==null)?("-"):(convAcc[1]),
                    (convAcc==null)?("-"):(convAcc[2]),
                    (convGyro==null)?("-"):(convGyro[0]),
                    (convGyro==null)?("-"):(convGyro[1]),
                    (convGyro==null)?("-"):(convGyro[2])
                    )) {
                // Log failed
                logger.closeLog(this);
                logger = null;
                showToast(getString(R.string.toast_log_failed));
                updateLoggerPanel();
            }
        }
        // No UI update. This is made in separate handler.
    }

    @Override
    public void onConnecting() {
        updateUI();
    }

    @Override
    public void onSensorNotificationsStarted(int samplingRate) {
        if(pendingStartLog){
            startLog();
        }
        updateSensorDataPanel();
    }

    @Override
    public void onSensorNotificationsStopped() {
        // Stop log
        if (logger != null && logger.isLogging()) {
            long elapsedMillis = (System.nanoTime()-startLogNanoTime)/1000000;
            logger.log(this, logSeparator, logTerminator,
                    String.format(
                            Locale.getDefault(), "%d", elapsedMillis),
                    getString(R.string.log_sensors_stopped_message));
            logger.closeLog(this);
            logger = null;
        }
        updateLoggerPanel();
        updateSensorDataPanel();
    }
}

