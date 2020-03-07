package com.esr.esense_recorder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import io.esense.esenselib.ESenseManager;

public class MainActivity extends AppCompatActivity {
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
    }
}
