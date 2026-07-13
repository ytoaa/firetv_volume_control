# Fire TV Volume Control

Use a Fire TV remote's media keys to drive an experimental global attenuation effect:

- **Fast-forward** → attenuation +2 dB, clamped at 0 dB
- **Rewind** → attenuation -2 dB, clamped at -40 dB

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
| Fast-forward | Increase attenuation gain by 2 dB toward 0 dB |
| Rewind | Decrease attenuation gain by 2 dB toward -40 dB |

The app consumes both button press and release events for these two keys. This prevents the foreground app from receiving only one half of a remapped key event.

## Important Behavior

- While the service is enabled, **Fast-forward** and **Rewind** no longer perform their original playback actions.
- Disable the accessibility service when normal seek controls are needed.
- Fire OS and individual streaming apps can handle media keys differently. Behavior should be checked on the target Fire TV and the apps you use most.
- On service connection, the app attempts one global DynamicsProcessing effect at audio session 0 with a 0 dB baseline.
- The overlay reports `EFFECT ACTIVE` after successful initialization, or `EFFECT ERROR: ...` when the platform rejects or loses the effect.
- Runtime logs use the `VolumeControlService` tag and include `DYNAMICS_INIT_SUCCESS`, `DYNAMICS_INIT_FAILURE`, `DYNAMICS_STEP`, `DYNAMICS_APPLY_FAILURE`, and `DYNAMICS_RELEASE_*` markers.
- The app does not claim that the effect changes audible HDMI output; validate continuously playing audio on the target device.

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
