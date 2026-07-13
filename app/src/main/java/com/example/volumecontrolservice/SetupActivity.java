package com.example.volumecontrolservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public class SetupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        findViewById(R.id.open_accessibility_settings).setOnClickListener(
                view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
    }
}
