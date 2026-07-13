package com.example.volumecontrolservice;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
import android.media.AudioManager;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AudioManager audioManager;
    private WindowManager windowManager;
    private View feedbackView;

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
    }

    @Override
    public void onDestroy() {
        dismissFeedback();
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!MediaKeyPolicy.shouldConsume(keyCode)) {
            return super.onKeyEvent(event);
        }

        if (event.getAction() == ACTION_DOWN) {
            if (MediaKeyPolicy.isVolumeUpKey(keyCode)) {
                adjustVolume(AudioManager.ADJUST_RAISE);
            } else {
                adjustVolume(AudioManager.ADJUST_LOWER);
            }
            showFeedback();
        }

        // Consume both key-down and key-up so the foreground app cannot receive
        // only half of a remapped media-key event.
        return true;
    }

    private void adjustVolume(int direction) {
        if (audioManager == null) {
            Log.w(TAG, "Ignoring volume key before service connection");
            return;
        }
        audioManager.adjustStreamVolume(STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
    }

    private void showFeedback() {
        if (audioManager == null) {
            return;
        }
        final int currentVolume = audioManager.getStreamVolume(STREAM_MUSIC);
        final int maximumVolume = audioManager.getStreamMaxVolume(STREAM_MUSIC);
        final String text = MediaKeyPolicy.volumeFeedback(currentVolume, maximumVolume);

        mainHandler.post(() -> {
            if (!MediaKeyPolicy.canUseAccessibilityOverlay(Build.VERSION.SDK_INT)) {
                Toast.makeText(this, text, LENGTH_SHORT).show();
                return;
            }
            showAccessibilityOverlay(text, currentVolume, maximumVolume);
        });
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
