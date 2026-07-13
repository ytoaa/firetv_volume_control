package com.example.volumecontrolservice;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DynamicsProcessingController attenuationController =
            new DynamicsProcessingController();
    private android.media.audiofx.DynamicsProcessing dynamicsProcessing;
    private VolumeSettings volumeSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        volumeSettings = new VolumeSettings(
                VolumeSettings.from(getSharedPreferences(VolumeSettings.class.getName(), MODE_PRIVATE)));
        attenuationController.setStep(volumeSettings.getStep());
    }
    private WindowManager windowManager;
    private View feedbackView;
    private boolean dynamicsInitializationAttempted;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Log.i(TAG, "service connected; initializing global DynamicsProcessing");
        initializeDynamicsProcessingOnce();
        showFeedback();
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
            // The settings activity can change this while the service remains connected.
            attenuationController.setStep(volumeSettings.getStep());
            float gainDb = gainForHandledKey(attenuationController, keyCode);
            applyCurrentGain(gainDb, 0);
            Log.i(TAG, "DYNAMICS_STEP key=" + KeyEvent.keyCodeToString(keyCode)
                    + " gainDb=" + formatDb(gainDb)
                    + " muted=" + attenuationController.isMuted()
                    + " status=" + attenuationController.getStatus());
            showFeedback();
        }

        // Consume both key-down and key-up so the foreground app cannot receive
        // only half of a remapped media-key event.
        return true;
    }

    static float gainForHandledKey(DynamicsProcessingController controller, int keyCode) {
        controller.stepForKey(keyCode);
        return controller.getGainDb();
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private void initializeDynamicsProcessingOnce() {
        if (dynamicsInitializationAttempted) {
            return;
        }
        dynamicsInitializationAttempted = true;
        if (!MediaKeyPolicy.canUseDynamicsProcessing(Build.VERSION.SDK_INT)) {
            String message = "requires API 28+ (SDK " + Build.VERSION.SDK_INT + ")";
            attenuationController.markError(message);
            Log.e(TAG, "DYNAMICS_INIT_FAILURE session=0 " + message);
            return;
        }

        try {
            // Let the API/device choose the channel count for global session 0. Fire OS rejects
            // the explicit two-channel engine architecture even though the effect is registered.
            DynamicsProcessing effect = new DynamicsProcessing(
                    DYNAMICS_PRIORITY, GLOBAL_AUDIO_SESSION, null);
            try {
                effect.setInputGainAllChannelsTo(attenuationController.getGainDb());
            } catch (RuntimeException muteException) {
                if (!attenuationController.isMuted()) throw muteException;
                effect.setInputGainAllChannelsTo(
                        DynamicsProcessingController.MUTE_FALLBACK_GAIN_DB);
                attenuationController.markMuteFallback();
                Log.w(TAG, "DYNAMICS_MUTE_FALLBACK requestedDb="
                        + formatDb(DynamicsProcessingController.MUTE_GAIN_DB)
                        + " appliedDb=" + formatDb(DynamicsProcessingController.MUTE_FALLBACK_GAIN_DB),
                        muteException);
            }
            effect.setEnabled(true);
            dynamicsProcessing = effect;
            attenuationController.markActive();
            Log.i(TAG, "DYNAMICS_INIT_SUCCESS session=0 gainDb="
                    + formatDb(attenuationController.getGainDb())
                    + " muted=" + attenuationController.isMuted() + " effectEnabled=true");
        } catch (RuntimeException exception) {
            attenuationController.markError(runtimeMessage(exception));
            dynamicsProcessing = null;
            Log.e(TAG, "DYNAMICS_INIT_FAILURE session=0 baselineDb=0", exception);
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private void applyCurrentGain(float gainDb, int retryCount) {
        if (dynamicsProcessing == null || attenuationController.getStatus()
                != DynamicsProcessingController.Status.ACTIVE) {
            return;
        }
        try {
            dynamicsProcessing.setInputGainAllChannelsTo(gainDb);
            Log.i(TAG, "DYNAMICS_APPLY_SUCCESS gainDb=" + formatDb(gainDb)
                    + " muted=" + attenuationController.isMuted());
        } catch (RuntimeException exception) {
            String message = runtimeMessage(exception);
            Log.e(TAG, "DYNAMICS_APPLY_FAILURE gainDb=" + formatDb(gainDb)
                    + " retryCount=" + retryCount, exception);
            if (attenuationController.isMuted()) {
                try {
                    dynamicsProcessing.setInputGainAllChannelsTo(
                            DynamicsProcessingController.MUTE_FALLBACK_GAIN_DB);
                    attenuationController.markMuteFallback();
                    Log.w(TAG, "DYNAMICS_MUTE_FALLBACK requestedDb="
                            + formatDb(DynamicsProcessingController.MUTE_GAIN_DB)
                            + " appliedDb="
                            + formatDb(DynamicsProcessingController.MUTE_FALLBACK_GAIN_DB), exception);
                    showFeedback();
                    return;
                } catch (RuntimeException fallbackException) {
                    Log.e(TAG, "DYNAMICS_MUTE_FALLBACK_FAILURE appliedDb="
                            + formatDb(DynamicsProcessingController.MUTE_FALLBACK_GAIN_DB),
                            fallbackException);
                }
            }
            if (DynamicsProcessingController.shouldRetryGainApply(retryCount)) {
                // A stale/lost effect can fail on apply. Release and recreate it once, then
                // reapply the requested gain through initialization.
                releaseDynamicsProcessing();
                initializeDynamicsProcessingOnce();
                if (dynamicsProcessing != null && attenuationController.getStatus()
                        == DynamicsProcessingController.Status.ACTIVE) {
                    Log.i(TAG, "DYNAMICS_APPLY_RETRY_SUCCESS gainDb=" + formatDb(gainDb));
                    return;
                }
            }
            attenuationController.markError(message);
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private void releaseDynamicsProcessing() {
        try {
            if (dynamicsProcessing != null) {
                dynamicsProcessing.setEnabled(false);
                dynamicsProcessing.release();
                Log.i(TAG, "DYNAMICS_RELEASE_SUCCESS session=0");
            }
        } catch (RuntimeException exception) {
            Log.w(TAG, "DYNAMICS_RELEASE_FAILURE session=0", exception);
        } finally {
            dynamicsProcessing = null;
            // Release is also the reconnect boundary: always allow a fresh initialization,
            // including when the effect reference was already null.
            dynamicsInitializationAttempted = false;
        }
    }

    private void showFeedback() {
        final String text = attenuationController.overlayText();
        mainHandler.post(() -> {
            if (!MediaKeyPolicy.canUseAccessibilityOverlay(Build.VERSION.SDK_INT)) {
                Toast.makeText(this, text, LENGTH_SHORT).show();
                return;
            }
            showAccessibilityOverlay(text);
        });
    }

    private void showAccessibilityOverlay(String text) {
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

        TextView attenuationText = feedbackView.findViewById(R.id.volume_text);
        ProgressBar attenuationProgress = feedbackView.findViewById(R.id.volume_progress);
        attenuationText.setText(text);
        attenuationProgress.setMax(DynamicsProcessingController.MAX_LEVEL);
        attenuationProgress.setProgress(attenuationController.getLevel());
        mainHandler.removeCallbacks(dismissFeedbackRunnable);
        mainHandler.postDelayed(dismissFeedbackRunnable, FEEDBACK_DURATION_MS);
    }

    private static String formatDb(float gainDb) {
        return String.format(java.util.Locale.US, "%.0f", gainDb);
    }

    private static String runtimeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.length() == 0 ? "" : ": " + message);
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
