# Google Play checklist (verify manually – policies change; nothing here is an approval)

## Technical
- [ ] Upload the signed **AAB**; enable Play App Signing.
- [ ] targetSdk 36 (set). Re-check Play's current "target API level" requirement before submitting.
- [ ] Release built with your real AdMob IDs (not test IDs).
- [ ] 512×512 icon (export from Android Studio Image Asset using `ic_launcher_foreground`), 1024×500 feature graphic, ≥2 phone screenshots (add 7"/10" tablet screenshots too).
- [ ] Test on Android 8, 10, 13, 15/16, a tablet, dark/light mode, offline mode.
- [ ] New personal developer accounts may need a closed test (e.g. 12 testers for 14 days) before production – confirm in Play Console.

## Store listing
- [ ] Don't claim "8K" unless true for the images you ship; don't claim "approved by Google", "guaranteed earnings", or unrelated keywords.
- [ ] Content must be properly licensed; keep licence proof for every image (Pexels credit is shown in-app).
- [ ] Privacy Policy URL (host `docs/PRIVACY_POLICY_TEMPLATE.txt` after editing) and the same text in-app. Set `PRIVACY_POLICY_URL`.

## Data safety form (answer to match the shipped app; recheck if you change SDKs)
- Data collected/shared: **Device or other IDs (Advertising ID)** – collected & shared with Google AdMob for advertising/analytics; **Approximate location/IP and App interactions/diagnostics** may be collected by the ads SDK; mark purpose *Advertising or marketing* (and *Analytics* if you see it in the SDK disclosures).
- Firebase Firestore reads only: no personal data is written by this app.
- Favorites/downloads/searches stay on device (not collected).
- Data encrypted in transit: **Yes** (HTTPS). Users can request deletion: provide a contact (no accounts exist).
- Also complete **Advertising ID declaration** (app uses AD_ID = Yes, for advertising).
- Use Google's SDK data-disclosure pages for AdMob and Firebase as the source of truth.

## Content rating (IARC questionnaire)
Category: *Utility / Entertainment*. Answer truthfully: no violence, sexual content, gambling, user-generated content, location sharing or purchases. If you add anime/car images, review them so they stay within the answers you gave.

## Ads & policy
- [ ] Ads are labelled, not placed on tappable controls, no interstitial while setting a wallpaper, none at launch/exit.
- [ ] Target audience: 13+/general audience (not "Designed for Families" unless you configure child-directed ads).
- [ ] Permissions requested: INTERNET, ACCESS_NETWORK_STATE, SET_WALLPAPER, AD_ID only.
