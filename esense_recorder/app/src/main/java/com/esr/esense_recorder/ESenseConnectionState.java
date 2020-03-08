package com.esr.esense_recorder;

import androidx.annotation.NonNull;

/**
 * Enumeration of the connection states.
 */
public enum ESenseConnectionState {
    DISCONNECTED(),
    CONNECTING(),
    CONNECTED();

    @NonNull
    @Override
    public String toString() {
        switch (this) {
            case DISCONNECTED:
                return "Disconnected";
            case CONNECTING:
                return "Connecting";
            case CONNECTED:
                return "Connected";
        }
        return "Unknown";
    }
}
