package com.esr.esense_recorder;

/**
 * Callback interface for check and activation of Bluetooth.
 */
public interface BluetoothCheckCallback {

    /**
     * Called when Bluetooth is ready.
     */
    void onBluetoothReady();

    /**
     * Called when one of the steps for activating Bluetooth has been rejected.
     */
    void onBluetoothActivationRejected();

    /**
     * Called when one of the steps for activating Bluetooth has failed.
     */
    void onBluetoothActivationFailed();

}
