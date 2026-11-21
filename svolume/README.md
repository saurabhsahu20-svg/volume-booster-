# S Volume Booster 1.0 – based on BoostX 1.0.0

**Derivative of the user-provided BoostX project** (original developer: Om/Aum Gupta, see https://github.com/AumGupta/BoostX), licensed **GNU GPL v3.0**. Original BoostX APK/assets are not bundled. Its system-volume slider and output-mix `LoudnessEnhancer(0)` method inspired this modified Android app. Complete source for this fork is available under the `svolume/` directory; accompanying GPL-3.0 license is `svolume/LICENSE`.

## New app branding
- Name: **S Volume Booster**, distinct install package `com.svolume.booster`.
- Native cyan-on-black UI.
- Adjustable normal Android media volume (system-limited).
- Optional, user-initiated **experimental** output-mix LoudnessEnhancer using audio session ID **0**, with up to +6 dB target-gain request.
- Foreground service and persistent notification while the user enables the effect; explicit Stop button and Stop notification action.
- Status indicates only whether Android accepted/initialized the effect, NOT whether volume increased.
- Audio output device listing.

## Limits and safety
**Session 0 audio effects are deprecated in Android and may have no audible effect**, especially on modern phones. This cannot promise 200% speaker output or true universal audio enhancement for YouTube/Instagram/WhatsApp/calls. It cannot bypass OEM or hardware limits. Only apply modest boost if comfortable; reduce immediately if distorted. Do not use on headphones at uncomfortable levels. On some phones the foreground service might be restricted by system policies.

## Build
GitHub Actions workflow in `.github/workflows/build-apk.yml` builds `svolume:assembleDebug`. Android SDK 35, Gradle 8.9, Java 17. APK artifact `SVolumeBooster-v1.0-APK`.

The original BoostX project was supplied as `BoostX-1.0.0.zip` and its GPLv3 notice must be preserved in derivative distributions.
