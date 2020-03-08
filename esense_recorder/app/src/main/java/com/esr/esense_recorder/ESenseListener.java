package com.esr.esense_recorder;

import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseEventListener;
import io.esense.esenselib.ESenseSensorListener;

/**
 * Listener to the <code>ESenseController</code>. For simplicity reason, this combines
 * all listeners of the <code>ESenseManager</code>.
 */
public interface ESenseListener extends ESenseConnectionListener, ESenseEventListener,
        ESenseSensorListener {

    /**
     * Called when a connected attempt is made.
     */
    void onConnecting();

}
