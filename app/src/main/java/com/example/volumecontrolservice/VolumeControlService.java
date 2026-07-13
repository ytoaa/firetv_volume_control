package com.example.volumecontrolservice;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class VolumeControlService extends android.accessibilityservice.AccessibilityService {
    private static final String TAG = "VolumeControlService";
    private static final long FEEDBACK_DURATION_MS = 1500L;
    private static final int DYNAMICS_PRIORITY = 1000;
    private static final int GLOBAL_AUDIO_SESSION = 0;
    private static final int OUTPUT_CHANNEL_COUNT = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AudioManager audioManager;
    private WindowManager windowManager;
    private View feedbackView;
    private DynamicsProcessing dynamicsProcessing;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Log.i(TAG, "service is connected");
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) { }

    @Override
    public void onInterrupt() {
        dismissFeedback();
        releaseDynamicsProcessing();
    }

    @Override
    public void onDestroy() {
        dismissFeedback();
        releaseDynamicsProcessing();
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!MediaKeyPolicy.shouldConsume(keyCode)) {
            return super.onKeyEvent(event);
        }

        if (event.getAction() == ACTION_DOWN) {
            applyTestAttenuation();
            showFeedback();
        }

        // Consume both key-down and key-up so the foreground app cannot receive
        // only half of a remapped media-key event.
        return true;
    }

    private void showFeedback() {
        if (audioManager == null) {
            return;
        }
        final int currentVolume = audioManager.getStreamVolume(STREAM_MUSIC);
        final int maximumVolume = audioManager.getStreamMaxVolume(STREAM_MUSIC);
        final String text = MediaKeyPolicy.attenuationFeedback();

        mainHandler.post(() -> {
            if (!MediaKeyPolicy.canUseAccessibilityOverlay(Build.VERSION.SDK_INT)) {
                Toast.makeText(this, text, LENGTH_SHORT).show();
                return;
            }
            showAccessibilityOverlay(text, currentVolume, maximumVolume);
        });
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private void applyTestAttenuation() {
        if (!MediaKeyPolicy.canUseDynamicsProcessing(Build.VERSION.SDK_INT)) {
            Log.w(TAG, "DynamicsProcessing test attenuation requires API 28+");
            return;
        }
        if (dynamicsProcessing != null) {
            return;
        }
        try {
            DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                    OUTPUT_CHANNEL_COUNT,
                    false, 0,
                    false, 0,
                    false, 0,
                    false)
                    .setInputGainAllChannelsTo(MediaKeyPolicy.TEST_ATTENUATION_DB)
                    .build();
            DynamicsProcessing effect = new DynamicsProcessing(
                    DYNAMICS_PRIORITY, GLOBAL_AUDIO_SESSION, config);
            effect.setInputGainAllChannelsTo(MediaKeyPolicy.TEST_ATTENUATION_DB);
            effect.setEnabled(true);
            dynamicsProcessing = effect;
            Log.i(TAG, "Enabled test global attenuation at -20 dB");
        } catch (RuntimeException exception) {
            Log.e(TAG, "DynamicsProcessing unavailable; continuing without attenuation", exception);
            dynamicsProcessing = null;
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private void releaseDynamicsProcessing() {
        if (dynamicsProcessing == null) {
            return;
        }
        try {
            dynamicsProcessing.setEnabled(false);
            dynamicsProcessing.release();
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to cleanly release DynamicsProcessing", exception);
        } finally {
            dynamicsProcessing = null;
        }
    }

    private void showAccessibilityOverlay(String text, int currentVolume, int maximumVolume) {
        if (windowManager == null) {
            return;
        }
        if (feedbackView == null) {
            feedbackView = LayoutInflater.from(this).inflate(R.layout.volume_control, null);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    android.graphics.PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.y = 96;
            try {
                windowManager.addView(feedbackView, params);
            } catch (WindowManager.BadTokenException | SecurityException exception) {
                Log.e(TAG, "Unable to add accessibility feedback overlay", exception);
                feedbackView = null;
                Toast.makeText(this, text, LENGTH_SHORT).show();
                return;
            }
        }

        TextView volumeText = feedbackView.findViewById(R.id.volume_text);
        ProgressBar volumeProgress = feedbackView.findViewById(R.id.volume_progress);
        volumeText.setText(text);
        volumeProgress.setMax(maximumVolume);
        volumeProgress.setProgress(currentVolume);
        mainHandler.removeCallbacks(dismissFeedbackRunnable);
        mainHandler.postDelayed(dismissFeedbackRunnable, FEEDBACK_DURATION_MS);
    }

    private final Runnable dismissFeedbackRunnable = this::dismissFeedback;

    private void dismissFeedback() {
        mainHandler.removeCallbacks(dismissFeedbackRunnable);
        if (feedbackView != null && windowManager != null) {
            try {
                windowManager.removeView(feedbackView);
            } catch (IllegalArgumentException exception) {
                Log.w(TAG, "Feedback overlay was already removed", exception);
            }
        }
        feedbackView = null;
    }
}
