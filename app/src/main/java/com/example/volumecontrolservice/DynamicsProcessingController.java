package com.example.volumecontrolservice;

import java.util.Arrays;
import java.util.Locale;

/** Pure state machine for bounded level-based DynamicsProcessing gain and mute. */
final class DynamicsProcessingController {
    static final int MIN_LEVEL = 0;
    static final int MAX_LEVEL = 40;
    static final int DEFAULT_LEVEL = 20;
    static final float MAX_GAIN_DB = 20.0f;
    static final float MUTE_GAIN_DB = -100.0f;
    static final float MUTE_FALLBACK_GAIN_DB = -80.0f;
    static final float MIN_NON_MUTE_GAIN_DB = -20.0f;
    static final float MIN_GAIN_DB = MUTE_GAIN_DB;
    static final int DEFAULT_STEP = 2;
    private static final int[] SUPPORTED_STEPS = {1, 2, 4, 8};

    enum Status { NOT_INITIALIZED, ACTIVE, ERROR }

    private int level = DEFAULT_LEVEL;
    private int lastNonMutedLevel = DEFAULT_LEVEL;
    private int step = DEFAULT_STEP;
    private boolean muted;
    private Status status = Status.NOT_INITIALIZED;
    private String errorMessage = "not initialized";
    private boolean muteFallback;

    static boolean shouldRetryGainApply(int retryCount) { return retryCount == 0; }

    static float levelToDb(int requestedLevel) {
        int value = clampLevel(requestedLevel);
        if (value == 0) return MUTE_GAIN_DB;
        if (value <= DEFAULT_LEVEL) {
            return MIN_NON_MUTE_GAIN_DB
                    + (value - 1) * (-MIN_NON_MUTE_GAIN_DB / (DEFAULT_LEVEL - 1));
        }
        return (value - DEFAULT_LEVEL) * (MAX_GAIN_DB / (MAX_LEVEL - DEFAULT_LEVEL));
    }

    static int dbToLevel(float requestedDb) {
        float db = Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, requestedDb));
        if (db < MIN_NON_MUTE_GAIN_DB) return 0;
        if (db <= 0.0f) return Math.round(1 + (db - MIN_NON_MUTE_GAIN_DB)
                * (DEFAULT_LEVEL - 1) / -MIN_NON_MUTE_GAIN_DB);
        return Math.round(DEFAULT_LEVEL + db * (MAX_LEVEL - DEFAULT_LEVEL) / MAX_GAIN_DB);
    }

    static boolean isSupportedStep(int value) {
        for (int supported : SUPPORTED_STEPS) if (supported == value) return true;
        return false;
    }

    static int[] supportedSteps() { return Arrays.copyOf(SUPPORTED_STEPS, SUPPORTED_STEPS.length); }

    int getLevel() { return level; }
    float getGainDb() { return levelToDb(level); }
    boolean isMuted() { return muted; }
    int getStep() { return step; }
    Status getStatus() { return status; }

    void setLevel(int requestedLevel) {
        int value = clampLevel(requestedLevel);
        if (value == MIN_LEVEL) {
            if (!muted) lastNonMutedLevel = level;
            muted = true;
        } else {
            muted = false;
            lastNonMutedLevel = value;
            muteFallback = false;
        }
        level = value;
    }

    void setStep(int requestedStep) {
        if (isSupportedStep(requestedStep)) step = requestedStep;
    }

    void markActive() { status = Status.ACTIVE; errorMessage = ""; }
    void markMuteFallback() { muteFallback = true; }
    void markError(String message) {
        status = Status.ERROR;
        errorMessage = message == null || message.length() == 0 ? "unknown failure" : message;
    }

    int stepForKey(int keyCode) {
        if (MediaKeyPolicy.isMuteKey(keyCode)) return toggleMute();
        if (muted) {
            if (MediaKeyPolicy.isVolumeUpKey(keyCode)) setLevel(step);
            return level;
        }
        if (MediaKeyPolicy.isVolumeUpKey(keyCode)) setLevel(level + step);
        else if (MediaKeyPolicy.isVolumeDownKey(keyCode)) setLevel(level - step);
        return level;
    }

    int toggleMute() {
        if (muted) {
            muted = false;
            level = lastNonMutedLevel;
            muteFallback = false;
        } else {
            lastNonMutedLevel = level;
            muted = true;
            level = MIN_LEVEL;
        }
        return level;
    }

    String overlayText() {
        String state;
        if (status == Status.ERROR) state = "ERROR: " + errorMessage;
        else if (muted) state = muteFallback ? "MUTED (effect fallback -80 dB)" : "MUTED";
        else if (status == Status.ACTIVE) state = "ACTIVE";
        else state = "ERROR: " + errorMessage;
        return String.format(Locale.US, "Volume %d / 40 | %s", level, state);
    }

    private static int clampLevel(int value) { return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, value)); }
}
