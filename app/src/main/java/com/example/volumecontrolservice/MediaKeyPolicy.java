package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;

final class MediaKeyPolicy {
    enum Feedback {
        NONE,
        VOLUME_UP,
        VOLUME_DOWN,
        MUTE
    }

    private MediaKeyPolicy() {
    }

    static boolean shouldConsume(int keyCode) {
        return isVolumeUpKey(keyCode) || isVolumeDownKey(keyCode) || isMuteKey(keyCode);
    }

    static boolean isVolumeUpKey(int keyCode) {
        return keyCode == KEYCODE_MEDIA_FAST_FORWARD;
    }

    static boolean isVolumeDownKey(int keyCode) {
        return keyCode == KEYCODE_MEDIA_REWIND;
    }

    static boolean isMuteKey(int keyCode) {
        return keyCode == KEYCODE_VOLUME_MUTE;
    }

    static String volumeFeedback(int currentVolume, int maximumVolume) {
        return "Volume " + currentVolume + " / " + maximumVolume;
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
        if (isMuteKey(keyCode)) {
            return Feedback.MUTE;
        }
        return Feedback.NONE;
    }
}
