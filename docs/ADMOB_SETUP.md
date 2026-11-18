# AdMob setup

Default build uses **Google's official test IDs** (safe to click while developing). Real IDs go in `local.properties` / environment / GitHub Secrets – never hard-code them:
`ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`, `ADMOB_NATIVE_ID`.

1. admob.google.com → Apps → Add app (Android; if not yet on Play, choose "No" and link later).
2. Create ad units: Banner, Interstitial, Rewarded, Native advanced. Copy the App ID (`ca-app-pub-…~…`) and each unit ID (`ca-app-pub-…/…`).
3. Set them as described above and rebuild. **Do not test with your own live IDs by tapping ads** (policy violation, can ban the account); use test IDs or registered test devices.
4. Consent: **Privacy & messaging** → create a GDPR message (EEA/UK) and, if needed, US state messages. The app already runs the Google UMP flow and offers "Ad privacy choices" in Settings when required.
5. Link app to Play listing; publish `app-ads.txt` on your developer website if you list one.

Placement rules built in: banners on Home/Trending/Category; native ads inside feeds; interstitial only after leaving a wallpaper detail (every 4th time, ≥2 min apart); rewarded is optional (Settings > Support); **no ads on the set-wallpaper screen**; no ad is required for any feature. Revenue is never guaranteed.
