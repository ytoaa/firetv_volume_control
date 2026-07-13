package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;

final class MediaKeyPolicy {
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
