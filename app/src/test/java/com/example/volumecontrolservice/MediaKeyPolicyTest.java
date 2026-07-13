package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void formatsResultingVolumeAndMaximumForVisibleFeedback() {
        assertEquals("Volume 7 / 15", MediaKeyPolicy.volumeFeedback(7, 15));
    }

    @Test
    public void usesAccessibilityOverlayOnSupportedAndroidVersions() {
        assertTrue(MediaKeyPolicy.canUseAccessibilityOverlay(22));
        assertTrue(MediaKeyPolicy.canUseAccessibilityOverlay(32));
        assertFalse(MediaKeyPolicy.canUseAccessibilityOverlay(21));
    }

    @Test
    public void requestsVisibleFeedbackForHandledKeysOnly() {
        assertEquals(MediaKeyPolicy.Feedback.VOLUME_UP,
                MediaKeyPolicy.feedbackFor(KEYCODE_MEDIA_FAST_FORWARD));
        assertEquals(MediaKeyPolicy.Feedback.VOLUME_DOWN,
                MediaKeyPolicy.feedbackFor(KEYCODE_MEDIA_REWIND));
        assertEquals(MediaKeyPolicy.Feedback.NONE,
                MediaKeyPolicy.feedbackFor(KEYCODE_MEDIA_PLAY_PAUSE));
    }
}
