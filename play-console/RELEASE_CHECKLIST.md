# Play Console release checklist

## Artifacts

- Phone bundle: `dist/NightPause-phone.aab` (package `de.joancode.nightpause`, version code 5, target API 36)
- Wear OS bundle: `dist/NightPause-watch.aab` (same package, version code 6, target API 35)
- Upload certificate: `dist/NightPause-upload-certificate.pem`
- The two bundles use the same upload key. Enroll the app in Play App Signing and let Google manage the app-signing key.

The private upload key and its password are stored outside this project in `~/.config/nightpause/`. Keep a secure backup of both files before relying on the key for updates. Never upload or share `upload-key.jks` or `signing.properties`.

## Create the app and first test release

1. In Play Console, create an app named **Night Pause**, select the default language, mark it as an app, and choose free or paid. App pricing cannot be changed from paid to free later.
2. Complete identity verification and check the Android developer verification page for registration status of `de.joancode.nightpause`. If manual package registration is requested, follow its APK challenge flow. The package has already been installed outside Play, so Console may require proof tied to an eligible signing key.
3. Complete the app dashboard tasks, then add **Wear OS** under **Test and release → Advanced settings → Form factors**. Google Play handles the phone and Wear OS bundles as separate form-factor releases under the same listing.
4. Upload `NightPause-phone.aab` to an internal testing release. Enroll in Play App Signing when prompted.
5. Create the Wear OS internal testing release and upload `NightPause-watch.aab`. Add at least one real Wear OS screenshot to the Wear OS listing.
6. Finish the store listing, app access instructions, content rating, target audience, ads declaration, Data safety form, and the Health apps declaration. Select **Sleep management** in the Health apps declaration; the app detects sleep state through Wear OS Health Services.
7. Use `NightWatchPlayStore@joancode.dev` as the public support contact, add the publisher/controller name to the privacy policy, publish it at a stable public URL, and enter that URL before submitting the listing or release.
8. Test installation and the complete watch-to-phone flow through Play internal testing before starting closed testing.

This is a personal developer account created after 13 November 2023, so production access requires a closed test with at least 12 testers continuously opted in for 14 days. Internal testing is still available first.

## Store details to prepare

See `STORE_LISTING.md` for copy and outstanding image assets. Replace the contact placeholder in `PRIVACY_POLICY_DRAFT.md`, publish that policy at a stable public URL, and then use the URL in Play Console.

## Build the bundles again

```sh
gradle :app:bundleRelease :wear:bundleRelease
```

The release build reads signing credentials from `~/.config/nightpause/signing.properties` by default. Keep version codes increasing and unique across the phone and Wear OS releases.
