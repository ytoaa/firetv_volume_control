package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;

final class MediaKeyPolicy {
    static final float TEST_ATTENUATION_DB = -20.0f;

    enum Feedback {
        NONE,
        VOLUME_UP,
        VOLUME_DOWN
    }

    private MediaKeyPolicy() {
    }

    static boolean shouldConsume(int keyCode) {
        return isVolumeUpKey(keyCode) || isVolumeDownKey(keyCode);
    }

    static boolean isVolumeUpKey(int keyCode) {
        return keyCode == KEYCODE_MEDIA_FAST_FORWARD;
    }

    static boolean isVolumeDownKey(int keyCode) {
        return keyCode == KEYCODE_MEDIA_REWIND;
    }

    static String volumeFeedback(int currentVolume, int maximumVolume) {
        return "Volume " + currentVolume + " / " + maximumVolume;
    }

    static String attenuationFeedback() {
        return "TEST ATTENUATION: -20 dB";
    }

    static boolean canUseAccessibilityOverlay(int sdkInt) {
        return sdkInt >= 22;
    }

    static boolean canUseDynamicsProcessing(int sdkInt) {
        return sdkInt >= 28;
    }

    static Feedback feedbackFor(int keyCode) {
        if (isVolumeUpKey(keyCode)) {
            return Feedback.VOLUME_UP;
        }
        if (isVolumeDownKey(keyCode)) {
            return Feedback.VOLUME_DOWN;
        }
        return Feedback.NONE;
    }
}
