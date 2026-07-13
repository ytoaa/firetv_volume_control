package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DynamicsProcessingControllerTest {
    @Test
    public void startsAtZeroDbAndReportsNotInitialized() {
        DynamicsProcessingController controller = new DynamicsProcessingController();

        assertEquals(0.0f, controller.getGainDb(), 0.0f);
        assertEquals(DynamicsProcessingController.Status.NOT_INITIALIZED, controller.getStatus());
        assertEquals("GAIN: 0 dB | MUTE: OFF | EFFECT: ERROR: not initialized",
                controller.overlayText());
    }

    @Test
    public void fastForwardRaisesGainAndRewindLowersGainByTwoDb() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        assertEquals(2.0f, controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD), 0.0f);
        assertEquals(0.0f, controller.stepForKey(KEYCODE_MEDIA_REWIND), 0.0f);
        assertEquals(-2.0f, controller.stepForKey(KEYCODE_MEDIA_REWIND), 0.0f);
    }

    @Test
    public void clampsGainAtPositiveCeilingAndSafeFloor() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        for (int i = 0; i < 30; i++) {
            controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD);
        }
        assertEquals(DynamicsProcessingController.MAX_GAIN_DB, controller.getGainDb(), 0.0f);

        for (int i = 0; i < 60; i++) {
            controller.stepForKey(KEYCODE_MEDIA_REWIND);
        }
        assertEquals(DynamicsProcessingController.MIN_GAIN_DB, controller.getGainDb(), 0.0f);
    }

    @Test
    public void reportsGainMuteActiveAndErrorUnambiguously() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();
        assertEquals("GAIN: 0 dB | MUTE: OFF | EFFECT: ACTIVE", controller.overlayText());

        controller.markError("unsupported");
        assertEquals(DynamicsProcessingController.Status.ERROR, controller.getStatus());
        assertEquals("GAIN: 0 dB | MUTE: OFF | EFFECT: ERROR: unsupported",
                controller.overlayText());
    }

    @Test
    public void rejectedKeyDoesNotChangeGain() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        assertEquals(0.0f, controller.stepForKey(85), 0.0f);
        assertEquals(0.0f, controller.getGainDb(), 0.0f);
    }

    @Test
    public void muteStoresAndRestoresLastNonMutedGain() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();
        controller.stepForKey(KEYCODE_MEDIA_REWIND);

        assertEquals(DynamicsProcessingController.MUTE_GAIN_DB,
                controller.toggleMute(), 0.0f);
        assertTrue(controller.isMuted());
        assertEquals(DynamicsProcessingController.MUTE_GAIN_DB, controller.getGainDb(), 0.0f);

        assertEquals(-2.0f, controller.toggleMute(), 0.0f);
        assertFalse(controller.isMuted());
    }

    @Test
    public void muteKeyTogglesEffectGain() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();
        assertEquals(DynamicsProcessingController.MUTE_GAIN_DB,
                controller.stepForKey(KEYCODE_VOLUME_MUTE), 0.0f);
        assertEquals(0.0f, controller.stepForKey(KEYCODE_VOLUME_MUTE), 0.0f);
    }

    @Test
    public void retriesGainApplyOnlyOnce() {
        assertTrue(DynamicsProcessingController.shouldRetryGainApply(0));
        assertFalse(DynamicsProcessingController.shouldRetryGainApply(1));
    }
}
