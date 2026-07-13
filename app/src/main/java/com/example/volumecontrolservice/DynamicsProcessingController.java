package com.example.volumecontrolservice;

import java.util.Locale;

/** Pure state machine for bounded DynamicsProcessing gain and effect-path mute. */
final class DynamicsProcessingController {
    // +6 dB is a deliberately conservative amplification ceiling: it supports positive gain
    // without the clipping risk and excessive output boost of a larger experimental range.
    static final float MAX_GAIN_DB = 6.0f;
    // -80 dB is near-silence while remaining within the Android DynamicsProcessing gain range
    // used by API 28+ devices; it avoids relying on a separate platform mute route.
    static final float MUTE_GAIN_DB = -80.0f;
    static final float MIN_GAIN_DB = MUTE_GAIN_DB;
    static final float STEP_DB = 2.0f;

    enum Status {
        NOT_INITIALIZED,
        ACTIVE,
        ERROR
    }

    private float gainDb = 0.0f;
    private float lastNonMutedGainDb = 0.0f;
    private boolean muted;
    private Status status = Status.NOT_INITIALIZED;
    private String errorMessage = "not initialized";

    static boolean shouldRetryGainApply(int retryCount) {
        return retryCount == 0;
    }

    float getGainDb() {
        return gainDb;
    }

    boolean isMuted() {
        return muted;
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
        if (MediaKeyPolicy.isMuteKey(keyCode)) {
            return toggleMute();
        }
        if (muted) {
            return gainDb;
        }
        if (MediaKeyPolicy.isVolumeUpKey(keyCode)) {
            setNonMutedGain(gainDb + STEP_DB);
        } else if (MediaKeyPolicy.isVolumeDownKey(keyCode)) {
            setNonMutedGain(gainDb - STEP_DB);
        }
        return gainDb;
    }

    float toggleMute() {
        if (muted) {
            muted = false;
            gainDb = lastNonMutedGainDb;
        } else {
            lastNonMutedGainDb = gainDb;
            muted = true;
            gainDb = MUTE_GAIN_DB;
        }
        return gainDb;
    }

    String overlayText() {
        String gain = String.format(Locale.US, "%.0f", gainDb);
        String mute = muted ? "ON" : "OFF";
        if (status == Status.ACTIVE) {
            return "GAIN: " + gain + " dB | MUTE: " + mute + " | EFFECT: ACTIVE";
        }
        return "GAIN: " + gain + " dB | MUTE: " + mute + " | EFFECT: ERROR: " + errorMessage;
    }

    private void setNonMutedGain(float value) {
        lastNonMutedGainDb = clamp(value);
        gainDb = lastNonMutedGainDb;
    }

    private static float clamp(float value) {
        return Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, value));
    }
}
