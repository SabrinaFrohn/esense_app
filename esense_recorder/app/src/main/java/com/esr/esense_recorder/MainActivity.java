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

    // eSense controller
    ESenseController eSenseController = new ESenseController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // References to UI components
        connectionStateLabel = findViewById(R.id.activity_main_connection_state_label);

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
        // TODO
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        // TODO
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
        // TODO
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
        // TODO
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        // TODO
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

