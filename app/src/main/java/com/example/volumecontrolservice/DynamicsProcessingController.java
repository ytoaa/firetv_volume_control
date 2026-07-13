package com.example.volumecontrolservice;

import java.util.Locale;

/** Pure state machine for the stepped attenuation control. */
final class DynamicsProcessingController {
    static final float MAX_GAIN_DB = 0.0f;
    static final float MIN_GAIN_DB = -40.0f;
    static final float STEP_DB = 2.0f;

    enum Status {
        NOT_INITIALIZED,
        ACTIVE,
        ERROR
    }

    private float gainDb = MAX_GAIN_DB;
    private Status status = Status.NOT_INITIALIZED;
    private String errorMessage = "not initialized";

    float getGainDb() {
        return gainDb;
    }

    Status getStatus() {
        return status;
    }

    void markActive() {
        status = Status.ACTIVE;
        errorMessage = "";
    }

    void markError(String message) {
        status = Status.ERROR;
        errorMessage = message == null || message.length() == 0 ? "unknown failure" : message;
    }

    float stepForKey(int keyCode) {
        if (MediaKeyPolicy.isVolumeUpKey(keyCode)) {
            gainDb = clamp(gainDb + STEP_DB);
        } else if (MediaKeyPolicy.isVolumeDownKey(keyCode)) {
            gainDb = clamp(gainDb - STEP_DB);
        }
        return gainDb;
    }

    String overlayText() {
        String attenuation = String.format(Locale.US, "%.0f", gainDb);
        if (status == Status.ACTIVE) {
            return "TEST ATTENUATION: " + attenuation + " dB | EFFECT ACTIVE";
        }
        return "TEST ATTENUATION: " + attenuation + " dB | EFFECT ERROR: " + errorMessage;
    }

    private static float clamp(float value) {
        return Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, value));
    }
}
