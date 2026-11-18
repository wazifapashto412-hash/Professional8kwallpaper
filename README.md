# Infinity 8K Wallpapers (Android, Kotlin, Jetpack Compose)

Package: `com.infinity.wallpapers` · minSdk 26 · targetSdk/compileSdk 36 · MVVM · Material 3

> **Honest status:** this project was written in a sandbox with **no Android SDK and no network**. It has **not been compiled or run**, so no APK/AAB is included and nothing is claimed as tested. First build may show small compile errors; send me the Gradle log and they can be fixed quickly. Read `docs/BUILD.md`.

## What is included
- 15 screens: Splash, Onboarding, Home, Trending, Categories, Category detail, Wallpaper detail, Full-screen preview (plain / lock / home mock-ups, pinch zoom), Favorites, Downloads, Search, Settings, Privacy Policy, About, Offline/Error.
- 20 categories (Trending, 8K Ultra HD, 4K, AMOLED, Nature, … Latest) plus extra categories added remotely.
- Set wallpaper on Home / Lock / Both (`WallpaperManager`), download (WorkManager, no storage permission), favorites, share, resolution info.
- Search + recent searches + trending suggestions, recently viewed, recommendations, download history.
- Pagination, Coil caching, thumbnails vs preview vs full URL, offline access to downloaded wallpapers, retry on errors.
- AdMob: adaptive banner, native ads in feeds, interstitial (only after leaving a wallpaper screen, rate-limited), optional rewarded ad, Google UMP consent. **No ads on the wallpaper detail / set screen.** Google test IDs by default.
- Settings: theme (system/light/dark), image quality, Wi-Fi-only downloads, cache management, privacy, about, share app, version.
- GitHub Actions workflow building debug APK, release APK and AAB.

## Where wallpapers come from (the "8K" question)
The app picks a content source automatically:
1. **Firebase Firestore** (if `app/google-services.json` exists) – your own catalogue; add/edit/remove without a new APK. See `docs/FIREBASE_SETUP.md`. **Use this for genuine 8K**: host your own properly licensed 7680-px images (Firebase Storage or a CDN) and store the real `width`/`height`.
2. **Pexels API** (if `PEXELS_API_KEY` is set) – free, licensed photos with their real pixel size; the 8K/4K categories show only originals that really measure ≥7680 / ≥3840 px.
3. **Demo mode** – 160 placeholder photos from Picsum so the UI works immediately. These are **not 8K and are never labelled 8K**, and the 8K/4K categories are empty in demo mode.

I could not bundle 8K images in the ZIP (no network here, and licences/size). The 8K/4K badge is computed from real dimensions only.

## Quick start
1. Open the folder in Android Studio (Ladybug or newer, JDK 17). Let it sync. If it asks about the Gradle wrapper, accept; or run `gradle wrapper --gradle-version 8.11.1` once and commit the generated `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`.
2. Copy `local.properties.example` → `local.properties` (optional keys).
3. Run on a device (debug build).

Docs: `docs/BUILD.md` (APK/AAB + signing) · `docs/FIREBASE_SETUP.md` · `docs/ADMOB_SETUP.md` · `docs/PLAY_STORE_CHECKLIST.md` (incl. Data safety & content rating guide) · `docs/GITHUB_UPLOAD.md` · `docs/PRIVACY_POLICY_TEMPLATE.txt` (also bundled as an asset).

## Project layout
`app/src/main/java/com/infinity/wallpapers/`
`domain/` models · `data/` Room + settings · `network/` Pexels & demo sources · `firebase/` Firestore source · `repository/` · `wallpaper/` WallpaperManager + download worker · `ads/` AdMob + UMP · `presentation/` ViewModels · `ui/` theme, components, screens · `navigation/` · `utils/`

## Known limitations (documented, not hidden)
- Firestore search is client-side over the newest 300 wallpapers.
- "Popular downloads" uses the `downloads` field; the app is read-only to Firestore, so keep that number updated yourself (admin tool or a Cloud Function).
- Android 8–9: downloads go to the app's own Pictures folder (to avoid the storage permission); Android 10+: public `Pictures/Infinity Wallpapers`.
- UI strings are English and not yet in `strings.xml` (localisation is a later step).
- A Pexels key inside an APK can be extracted; for a big release, proxy it through your backend.
