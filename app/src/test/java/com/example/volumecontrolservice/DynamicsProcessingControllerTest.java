package com.example.volumecontrolservice;

import static android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
import static android.view.KeyEvent.KEYCODE_MEDIA_REWIND;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DynamicsProcessingControllerTest {
    @Test
    public void startsAtZeroDbAndReportsNotInitialized() {
        DynamicsProcessingController controller = new DynamicsProcessingController();

        assertEquals(0.0f, controller.getGainDb(), 0.0f);
        assertEquals(DynamicsProcessingController.Status.NOT_INITIALIZED, controller.getStatus());
        assertEquals("TEST ATTENUATION: 0 dB | EFFECT ERROR: not initialized",
                controller.overlayText());
    }

    @Test
    public void fastForwardRaisesGainByTwoDbTowardZero() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        assertEquals(0.0f, controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD), 0.0f);
        assertEquals(-2.0f, controller.stepForKey(KEYCODE_MEDIA_REWIND), 0.0f);
        assertEquals(0.0f, controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD), 0.0f);
    }

    @Test
    public void clampsGainAtZeroAndSafeFloor() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        for (int i = 0; i < 30; i++) {
            controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD);
        }
        assertEquals(0.0f, controller.getGainDb(), 0.0f);

        for (int i = 0; i < 30; i++) {
            controller.stepForKey(KEYCODE_MEDIA_REWIND);
        }
        assertEquals(-40.0f, controller.getGainDb(), 0.0f);
    }

    @Test
    public void reportsActiveAndRuntimeErrorUnambiguously() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();
        assertEquals("TEST ATTENUATION: 0 dB | EFFECT ACTIVE",
                controller.overlayText());

        controller.markError("unsupported");
        assertEquals(DynamicsProcessingController.Status.ERROR, controller.getStatus());
        assertEquals("TEST ATTENUATION: 0 dB | EFFECT ERROR: unsupported",
                controller.overlayText());
    }

    @Test
    public void rejectedKeyDoesNotChangeGain() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        assertEquals(0.0f, controller.stepForKey(85), 0.0f);
        assertEquals(0.0f, controller.getGainDb(), 0.0f);
    }
}
