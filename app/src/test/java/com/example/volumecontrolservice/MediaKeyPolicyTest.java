package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaKeyPolicyTest {
    @Test
    public void consumesBothRemappedMediaKeys() {
        assertTrue(MediaKeyPolicy.shouldConsume(KEYCODE_MEDIA_FAST_FORWARD));
        assertTrue(MediaKeyPolicy.shouldConsume(KEYCODE_MEDIA_REWIND));
    }

    @Test
    public void leavesOtherMediaKeysUntouched() {
        assertFalse(MediaKeyPolicy.shouldConsume(KEYCODE_MEDIA_PLAY_PAUSE));
    }
}
