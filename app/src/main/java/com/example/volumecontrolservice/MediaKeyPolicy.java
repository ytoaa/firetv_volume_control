package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;

final class MediaKeyPolicy {
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
}
