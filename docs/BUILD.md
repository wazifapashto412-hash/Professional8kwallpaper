# Build APK and AAB

Requirements: Android Studio (current stable), JDK 17. Versions used: AGP 8.9.1, Gradle 8.11.1, Kotlin 2.1.20, Compose BOM 2025.04.01. **Check for newer stable versions before release**; Google Play's target-API rule changes every year (this project targets API 36).

## Debug APK (testing)
Android Studio: *Build > Build Bundle(s)/APK(s) > Build APK(s)*, or
```
gradle :app:assembleDebug        # or ./gradlew after generating the wrapper
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

## Gradle wrapper
The wrapper JAR could not be generated offline. Run once: `gradle wrapper --gradle-version 8.11.1`, then commit `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.

## Create your signing key (once, keep it safe & private)
```
keytool -genkeypair -v -keystore infinity-release.jks -alias infinity -keyalg RSA -keysize 2048 -validity 10000
```
**Never commit the keystore or passwords** (`.gitignore` already blocks `*.jks`). Back it up: losing it can block updates (use Play App Signing).

## Signed release APK + AAB locally
```
export KEYSTORE_PATH=/absolute/path/infinity-release.jks
export KEYSTORE_PASSWORD=...  KEY_ALIAS=infinity  KEY_PASSWORD=...
export ADMOB_APP_ID=ca-app-pub-XXXX~YYYY  ADMOB_BANNER_ID=... ADMOB_INTERSTITIAL_ID=... ADMOB_REWARDED_ID=... ADMOB_NATIVE_ID=...
gradle :app:assembleRelease :app:bundleRelease
```
Outputs: `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`.
Without a keystore the release APK is unsigned and cannot be installed; the AAB is what you upload to Google Play (enrol in Play App Signing).

## GitHub Actions
Workflow: `.github/workflows/android.yml`. Add repository secrets (Settings > Secrets and variables > Actions):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 infinity-release.jks` |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | your key details |
| `GOOGLE_SERVICES_JSON_BASE64` | `base64 -w0 google-services.json` (optional, enables Firebase) |
| `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`, `ADMOB_NATIVE_ID` | your real AdMob IDs (otherwise Google test IDs are used) |
| `PEXELS_API_KEY`, `PRIVACY_POLICY_URL`, `SUPPORT_EMAIL` | optional |

Push to `main` → open the **Actions** tab → download artifact **apk-and-aab**.
**Never publish a release that still uses Google test ad IDs** (the Gradle build prints a warning if it does).
