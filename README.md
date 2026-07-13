# Fire TV Volume Control

Use a Fire TV remote's media keys to drive an experimental global DynamicsProcessing gain effect:

- **Fast-forward** → increase the user-facing volume level by the configured step (default 2), clamped to 40; from level 0/mute, it exits mute at the configured step (default level 2), rather than restoring the previous level
- **Rewind** → decrease the user-facing volume level by the configured step (default 2), clamped to 0
- **Volume mute** → level 0 / -100 dB target; if the effect rejects that target, safely falls back to -80 dB while preserving logical mute; physical Volume Mute again restores the last non-muted level

Levels are integer values from 0 to 40. Level 0 is a separate mute state targeting -100 dB. Levels 1–20 map linearly from -20 dB to 0 dB, and levels 20–40 map linearly from 0 dB to +20 dB; level 20 is exactly 0 dB. The overlay displays levels, not dB, and shows ACTIVE, MUTED, or ERROR status. If mute fallback is used, the overlay reports the -80 dB fallback.
The app runs as an Android accessibility service and displays an accessibility overlay with
the requested attenuation and the DynamicsProcessing status. This is an experimental global
output effect; a successful key event or log entry is not proof that HDMI audio is audibly
affected on a particular Fire TV route.

## Install

### From GitHub Actions

1. Open the repository's **Actions** tab.
2. Open a successful **Android CI** run.
3. Download the `firetv-volume-control-debug-apk` artifact and extract the APK.

### With ADB

1. Enable **Developer Options** and **ADB debugging** on the Fire TV.
2. Connect your computer to the Fire TV:

   ```shell
   adb connect FIRE_TV_IP_ADDRESS
   ```

3. Install the APK:

   ```shell
   adb install -r app-debug.apk
   ```

## Enable the Service

1. Open **VolumeControlService** from the Fire TV app list.
2. Select **Open Accessibility Settings**.
3. Find **Volume control service** and enable it.
4. Confirm Android's accessibility-service warning.

> Do not use `adb shell settings put secure enabled_accessibility_services ...` unless you understand its effect: it can overwrite the list of other enabled accessibility services.

## Use

Once the service is enabled:

| Remote button | Action |
| --- | --- |
| Fast-forward | Increase level by the configured step (default 2); from level 0/mute, exit at the configured step (default level 2) |
| Rewind | Decrease level by the configured step (default 2) |
| Volume mute | Toggle level 0 and restore the last non-muted level on the next physical Volume Mute press |

Open **VolumeControlService** from the Fire TV app list to choose **1, 2, 4, or 8 levels** per Fast-forward/Rewind press. The selected value is saved immediately and survives service restarts.
The app consumes both button press and release events for all three handled keys. This prevents the foreground app from receiving only one half of a remapped key event.

## Important Behavior

- While the service is enabled, **Fast-forward**, **Rewind**, and **Volume mute** no longer perform their original foreground-app actions.
- Disable the accessibility service when normal seek controls are needed.
- Fire OS and individual streaming apps can handle media keys differently. Behavior should be checked on the target Fire TV and the apps you use most.
- On service connection, the app attempts one global DynamicsProcessing effect at audio session 0 with the current gain.
- The experimental gain range is a -100 dB mute target, -20 dB to 0 dB attenuation, and 0 dB to +20 dB amplification. If the effect rejects the -100 dB mute target, the service applies -80 dB, logs `DYNAMICS_MUTE_FALLBACK`, and keeps logical level 0; the overlay makes the fallback visible.
- If applying a non-mute gain fails, the service releases and recreates the effect, then retries once. Release always clears the initialization guard so reconnects can initialize again.
- The overlay reports the logical level and `ACTIVE`, `MUTED`, or `ERROR` state; mute fallback is explicitly shown as `MUTED (effect fallback -80 dB)`. Runtime logs include those states and `DYNAMICS_INIT_SUCCESS`, `DYNAMICS_INIT_FAILURE`, `DYNAMICS_STEP`, `DYNAMICS_APPLY_FAILURE`, `DYNAMICS_APPLY_RETRY_SUCCESS`, `DYNAMICS_MUTE_FALLBACK`, and `DYNAMICS_RELEASE_*` markers.
- HDMI output is experimental and device/route dependent: the global DynamicsProcessing effect may be accepted and report success without changing what is audible over a particular HDMI path. A successful key event, overlay, or log entry is not proof of audible HDMI control; validate with continuously playing audio on the target Fire TV and route.

## Build

The repository includes GitHub Actions CI. Every push, pull request, and manual workflow run executes:

```text
unit tests → Android lint → debug APK build → APK artifact upload
```

To build locally:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release builds

The tag-based release workflow signs and publishes an APK when a `v*` tag is pushed. It requires these GitHub repository secrets:

- `RELEASE_KEYSTORE`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_PASSWORD`
