package com.example.volumecontrolservice;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.widget.Toast.LENGTH_SHORT;

import android.app.Dialog;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ProgressBar;
import android.widget.Toast;

public class VolumeControlService extends android.accessibilityservice.AccessibilityService {
    private static final String TAG = "yolo volume_control";
    private AudioManager audioManager;
    private Dialog volumeDialog;
    private final Handler dismissHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "service is connected");
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) { }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return handleKeyEvent(event);
    }

    private boolean handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!MediaKeyPolicy.shouldConsume(keyCode)) {
            return super.onKeyEvent(event);
        }

        if (event.getAction() == ACTION_DOWN) {
            if (MediaKeyPolicy.isVolumeUpKey(keyCode)) {
                increaseVolume();
            } else {
                reduceVolume();
            }
        }

        // Consume both key-down and key-up to prevent the foreground app from
        // receiving only half of a remapped media-key event.
        return true;
    }

    private void showDialog() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            final boolean overlayEnabled = Settings.canDrawOverlays(this);
            if(!overlayEnabled){
                Toast.makeText(this, "Enable draw over apps permission", LENGTH_SHORT).show();
            }
            Log.i(TAG, "overlay: " + overlayEnabled);
            if (!overlayEnabled) return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            // If dialog already exists and is showing, update it instead of creating new
            if (volumeDialog != null && volumeDialog.isShowing()) {
                ProgressBar volumeProgress = volumeDialog.findViewById(R.id.volume_progress);
                volumeProgress.setProgress(audioManager.getStreamVolume(STREAM_MUSIC));
                scheduleAutoDismiss();
                return;   // or update contents if needed
            }

            // create the dialog
            volumeDialog = new Dialog(this);
            volumeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            volumeDialog.setContentView(R.layout.volume_control);
            volumeDialog.getWindow().setBackgroundDrawableResource(android.R.color.background_dark);
            volumeDialog.setCancelable(false);
            Window window = volumeDialog.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();

            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.y = 80;  // adjust distance from bottom if needed

            window.setAttributes(params);

            // create and set volume progress bar
            ProgressBar volumeProgress = volumeDialog.findViewById(R.id.volume_progress);
            volumeProgress.setMax(audioManager.getStreamMaxVolume(STREAM_MUSIC));
            volumeProgress.setProgress(audioManager.getStreamVolume(STREAM_MUSIC));
            volumeProgress.setProgress(audioManager.getStreamVolume(STREAM_MUSIC));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                volumeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }else{
                volumeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }

            volumeDialog.show();

            // ⏱️ Start auto-dismiss timer
            scheduleAutoDismiss();
        });
    }

    private final Runnable dismissRunnable = () -> {
        if (volumeDialog != null && volumeDialog.isShowing()) {
            volumeDialog.dismiss();
        }
    };

    private void scheduleAutoDismiss() {
        // Cancel any previous dismiss timer
        dismissHandler.removeCallbacks(dismissRunnable);

        // Auto-close after 1 seconds
        dismissHandler.postDelayed(dismissRunnable, 1000);
    }

    private void increaseVolume() {
        audioManager.adjustStreamVolume(
                STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
        );
    }

    private void reduceVolume() {
        audioManager.adjustStreamVolume(
                STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
        );
    }
}