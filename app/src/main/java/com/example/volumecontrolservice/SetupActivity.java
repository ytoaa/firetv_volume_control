package com.example.volumecontrolservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SetupActivity extends Activity {
    private static final int[] STEP_IDS = {R.id.step_1, R.id.step_2, R.id.step_4, R.id.step_8};
    private static final int[] STEP_VALUES = {1, 2, 4, 8};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        VolumeSettings settings = new VolumeSettings(
                VolumeSettings.from(getSharedPreferences(VolumeSettings.class.getName(), MODE_PRIVATE)));
        int selectedStep = settings.getStep();
        RadioGroup stepChoices = findViewById(R.id.step_choices);
        for (int i = 0; i < STEP_VALUES.length; i++) {
            if (STEP_VALUES[i] == selectedStep) stepChoices.check(STEP_IDS[i]);
        }
        stepChoices.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < STEP_IDS.length; i++) {
                if (STEP_IDS[i] == checkedId) {
                    settings.setStep(STEP_VALUES[i]);
                    break;
                }
            }
        });

        findViewById(R.id.open_accessibility_settings).setOnClickListener(
                view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }
}
