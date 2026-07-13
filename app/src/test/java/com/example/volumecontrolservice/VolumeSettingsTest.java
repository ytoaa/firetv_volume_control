package com.example.volumecontrolservice;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class VolumeSettingsTest {
    @Test
    public void defaultsToTwoLevelsPerPress() {
        MemoryStore store = new MemoryStore();
        VolumeSettings settings = new VolumeSettings(store);
        assertEquals(2, settings.getStep());
    }

    @Test
    public void persistsOnlySupportedSteps() {
        MemoryStore store = new MemoryStore();
        VolumeSettings settings = new VolumeSettings(store);

        settings.setStep(8);
        assertEquals(8, settings.getStep());
        assertEquals(8, new VolumeSettings(store).getStep());

        settings.setStep(3);
        assertEquals(8, settings.getStep());
    }

    @Test
    public void invalidPersistedStepFallsBackToDefault() {
        MemoryStore store = new MemoryStore();
        store.putInt(VolumeSettings.STEP_KEY, 40);
        assertEquals(2, new VolumeSettings(store).getStep());
    }

    private static final class MemoryStore implements VolumeSettings.Store {
        private final Map<String, Integer> values = new HashMap<>();

        @Override
        public int getInt(String key, int defaultValue) {
            Integer value = values.get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public void putInt(String key, int value) {
            values.put(key, value);
        }
    }
}
