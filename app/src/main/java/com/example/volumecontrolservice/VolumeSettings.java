package com.example.volumecontrolservice;

import android.content.SharedPreferences;

/** Persists the TV remote step while keeping validation independent of Android UI. */
final class VolumeSettings {
    static final String STEP_KEY = "levels_per_press";
    static final int DEFAULT_STEP = DynamicsProcessingController.DEFAULT_STEP;

    interface Store {
        int getInt(String key, int defaultValue);
        void putInt(String key, int value);
    }

    private final Store store;

    VolumeSettings(Store store) { this.store = store; }

    int getStep() {
        int value = store.getInt(STEP_KEY, DEFAULT_STEP);
        return DynamicsProcessingController.isSupportedStep(value) ? value : DEFAULT_STEP;
    }

    void setStep(int value) {
        if (DynamicsProcessingController.isSupportedStep(value)) store.putInt(STEP_KEY, value);
    }

    static Store from(SharedPreferences preferences) {
        return new Store() {
            @Override public int getInt(String key, int defaultValue) {
                return preferences.getInt(key, defaultValue);
            }
            @Override public void putInt(String key, int value) {
                preferences.edit().putInt(key, value).apply();
            }
        };
    }
}
