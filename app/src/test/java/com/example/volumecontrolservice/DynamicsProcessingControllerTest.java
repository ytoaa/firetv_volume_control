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
    public void startsAtDefaultLevel20AndReportsActiveLevelState() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();

        assertEquals(20, controller.getLevel());
        assertEquals(0.0f, controller.getGainDb(), 0.0001f);
        assertEquals("Volume 20 / 40 | ACTIVE", controller.overlayText());
    }

    @Test
    public void mapsBoundaryAndCenterLevelsToSpecifiedDbRange() {
        assertEquals(-100.0f, DynamicsProcessingController.levelToDb(0), 0.0001f);
        assertEquals(-20.0f, DynamicsProcessingController.levelToDb(1), 0.0001f);
        assertEquals(0.0f, DynamicsProcessingController.levelToDb(20), 0.0001f);
        assertEquals(20.0f, DynamicsProcessingController.levelToDb(40), 0.0001f);
    }

    @Test
    public void convertsDbToNearestClampedLevel() {
        assertEquals(0, DynamicsProcessingController.dbToLevel(-100.0f));
        assertEquals(1, DynamicsProcessingController.dbToLevel(-20.0f));
        assertEquals(20, DynamicsProcessingController.dbToLevel(0.0f));
        assertEquals(40, DynamicsProcessingController.dbToLevel(20.0f));
        assertEquals(40, DynamicsProcessingController.dbToLevel(100.0f));
    }

    @Test
    public void clampsLevelsAtZeroAndForty() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setLevel(200);
        assertEquals(40, controller.getLevel());
        controller.setLevel(-10);
        assertEquals(0, controller.getLevel());
        assertTrue(controller.isMuted());
        assertEquals(-100.0f, controller.getGainDb(), 0.0001f);
    }

    @Test
    public void fastForwardAndRewindUseConfiguredLevelStep() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setStep(4);

        assertEquals(24, controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD));
        assertEquals(20, controller.stepForKey(KEYCODE_MEDIA_REWIND));
    }

    @Test
    public void handledKeyAppliesConvertedDbRatherThanLogicalLevel() {
        DynamicsProcessingController controller = new DynamicsProcessingController();

        assertEquals(2.0f, VolumeControlService.gainForHandledKey(
                controller, KEYCODE_MEDIA_FAST_FORWARD), 0.0001f);
        assertEquals(-100.0f, VolumeControlService.gainForHandledKey(
                controller, KEYCODE_VOLUME_MUTE), 0.0001f);
    }

    @Test
    public void muteRestoresLastNonMutedLevel() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setLevel(28);

        assertEquals(0, controller.stepForKey(KEYCODE_VOLUME_MUTE));
        assertTrue(controller.isMuted());
        assertEquals(-100.0f, controller.getGainDb(), 0.0001f);
        assertEquals(28, controller.stepForKey(KEYCODE_VOLUME_MUTE));
        assertFalse(controller.isMuted());
    }

    @Test
    public void levelZeroIsMutedEvenWhenReachedByClamping() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setLevel(1);
        controller.setLevel(0);
        controller.markActive();

        assertTrue(controller.isMuted());
        assertEquals("Volume 0 / 40 | MUTED", controller.overlayText());
    }

    @Test
    public void fastForwardFromLevelZeroUnmutesAtConfiguredStep() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setLevel(18);

        for (int press = 0; press < 9; press++) {
            controller.stepForKey(KEYCODE_MEDIA_REWIND);
        }

        assertEquals(0, controller.getLevel());
        assertTrue(controller.isMuted());
        assertEquals(2, controller.stepForKey(KEYCODE_MEDIA_FAST_FORWARD));
        assertFalse(controller.isMuted());
        assertEquals(2, controller.getLevel());
    }

    @Test
    public void overlayUsesLevelAndStateNotRawDb() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.setLevel(22);
        controller.markActive();
        assertEquals("Volume 22 / 40 | ACTIVE", controller.overlayText());

        controller.stepForKey(KEYCODE_VOLUME_MUTE);
        assertEquals("Volume 0 / 40 | MUTED", controller.overlayText());
        controller.markError("unsupported");
        assertEquals("Volume 0 / 40 | ERROR: unsupported", controller.overlayText());
    }

    @Test
    public void muteFallbackIsVisibleWithoutChangingLogicalLevel() {
        DynamicsProcessingController controller = new DynamicsProcessingController();
        controller.markActive();
        controller.setLevel(0);
        controller.markMuteFallback();

        assertEquals(0, controller.getLevel());
        assertEquals(-100.0f, controller.getGainDb(), 0.0001f);
        assertEquals("Volume 0 / 40 | MUTED (effect fallback -80 dB)", controller.overlayText());
    }
}
