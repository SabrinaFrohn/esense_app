package com.esr.esense_recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import io.esense.esenselib.ESenseManager;

public class MainActivity extends BluetoothCheckActivity implements BluetoothCheckCallback {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "eSenseRecorder-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    ESenseManager eSenseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check permissions and activate Bluetooth
        checkBluetooth(this);

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
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBluetoothReady() {
        showToast(getString(R.string.toast_message_bt_ready));
        // TODO
    }

    @Override
    public void onBluetoothActivationRejected() {
        showToast(getString(R.string.toast_message_bt_activation_rejected));
    }

    @Override
    public void onBluetoothActivationFailed() {
        showToast(getString(R.string.toast_message_bt_activation_failed));
    }
}

