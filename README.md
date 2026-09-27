# Night Pause

Night Pause pauses active media on an Android phone when a connected Wear OS watch detects that you have fallen asleep. It does not resume playback when you wake up.

The project has two Android application modules: `app/` for the phone and `wear/` for Wear OS. They share the Play package name `de.joancode.nightpause` and are published as separate phone and Wear OS form-factor bundles under one Play listing.

## Build

Use JDK 17, Android SDK 36, and Gradle 8.13 or newer. Build debug APKs for local installation with:

```sh
gradle :app:assembleDebug :wear:assembleDebug
```

Build signed Play App Bundles with:

```sh
gradle :app:bundleRelease :wear:bundleRelease
```

Release signing reads `~/.config/nightpause/signing.properties` by default. Create a dedicated upload key and keep both it and its password secure. Do not commit signing files. Google Play App Signing should manage the app-signing key used for distribution.

## Setup

On the phone, allow Night Pause to control active media from Android's notification access settings. On the watch, grant Activity Recognition permission and enable sleep detection. Connect the watch to the phone and choose a pause delay.

Sleep-state availability depends on Health Services support on the watch. The phone controls active Android media sessions; apps that do not expose a controllable media session cannot be paused.

## Play Console

The current release preparation, listing copy, and privacy policy draft are in [`play-console/`](play-console/). The privacy policy will be published at [nightpause.joancode.dev](https://nightpause.joancode.dev) after the custom-domain DNS record is configured.
