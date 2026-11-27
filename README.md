# GoldPulse: Daily Gold Rate

An Android app that shows today's indicative gold rate in India (INR), with a daily notification at around 9 AM.

- 24K, 22K and 18K rates, per 1 gram and per 10 grams
- Change vs yesterday (up or down, in rupees and percent)
- Refresh button and pull-to-refresh; the last rate is saved, so the app works offline and shows when it was last updated
- Daily notification at around 9 AM with today's 22K and 24K rate (WorkManager; asks for notification permission on Android 13+)
- Clear disclaimer: the rate is indicative and excludes GST and making charges
- No ads, no analytics, no login, no data collection

**Stack:** Kotlin, Jetpack Compose, Material 3 · compileSdk 36, targetSdk 36, minSdk 24 · package `com.deepkosh.goldpulse`

---

## How the rate is calculated

```
24K per gram = spot (USD per troy ounce) / 31.1035 x USD/INR x (1 + import duty %)
22K          = 24K x 0.916
18K          = 24K x 0.75
per 10 grams = per gram x 10
```

All the numbers are in one file you can edit:
[`app/src/main/java/com/deepkosh/goldpulse/config/GoldConfig.kt`](app/src/main/java/com/deepkosh/goldpulse/config/GoldConfig.kt)

| Constant | Default | Meaning |
|---|---|---|
| `IMPORT_DUTY_PERCENT` | `6.0` | India's effective import duty on gold (5% Basic Customs Duty + 1% AIDC). Check after every Union Budget. |
| `PURITY_22K` / `PURITY_18K` | `0.916` / `0.75` | Karat purity relative to 24K |
| `GRAMS_PER_TROY_OUNCE` | `31.1035` | Spot gold is quoted per troy ounce |
| `NOTIFICATION_HOUR` / `NOTIFICATION_MINUTE` | `9` / `0` | Daily notification time (device local time) |
| `RATE_TIME_ZONE` | `Asia/Kolkata` | Decides what "today" and "yesterday" mean |
| `STALE_AFTER_MINUTES` | `30` | Auto-refresh when the app is reopened after this long |

The app saves raw spot and exchange-rate values, so changing these constants also updates the saved rates the next time the app runs.

### Data sources (free, no API key)

Each endpoint was tested with `curl` before being used.

| Data | Primary | Fallbacks |
|---|---|---|
| Gold spot XAU/USD | `https://api.gold-api.com/price/XAU/INR` (live) | fawazahmed0/currency-api daily snapshot (jsDelivr, then Cloudflare Pages mirror) |
| USD to INR | Same gold-api.com response (`exchangeRate`) | `https://api.frankfurter.dev/v1/latest?base=USD&symbols=INR` (ECB), then currency-api |
| Yesterday's rate | Rate the app saved yesterday | currency-api dated snapshot, e.g. `.../currency-api@2026-11-26/...` |

Endpoints are in [`data/RateSources.kt`](app/src/main/java/com/deepkosh/goldpulse/data/RateSources.kt). Responses outside realistic ranges are rejected, so the app falls back to the next source rather than show a wrong rate.

---

## Project layout

```
app/src/main/java/com/deepkosh/goldpulse/
├── config/GoldConfig.kt           ← editable constants (duty, purities, notification time)
├── data/                          ← rate sources, offline cache, repository, settings
├── notifications/                 ← WorkManager job, scheduling, notification
├── ui/                            ← Compose screen, ViewModel, formatting, theme
├── GoldPulseApp.kt
└── MainActivity.kt
play-store/                        ← 512 px icon, 1024 x 500 feature graphic and screenshots for the Play listing
privacy-policy.md                  ← host this on GitHub Pages
keystore.properties.example        ← template for release signing
```

---

## Requirements

- **JDK 17 or newer** (21 recommended). With Homebrew: `brew install openjdk@21`, then
  `export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"`
- **Android SDK** with platform 36. Either install Android Studio, or point `local.properties` at an SDK:
  `sdk.dir=/Users/<you>/Library/Android/sdk`

Gradle itself is downloaded by the wrapper (`./gradlew`), with its checksum verified.

## Build and run (debug)

```bash
./gradlew assembleDebug                    # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                     # install on a connected device or emulator
./gradlew testDebugUnitTest                # unit tests
./gradlew lint                             # Android lint
```

---

## Release build for Google Play

### 1. Create an upload keystore (one time)

```bash
keytool -genkeypair -v \
  -keystore goldpulse-upload.jks \
  -alias goldpulse-upload \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

`keytool` asks for a password and your name and organisation. With PKCS12, the key password is the same as the keystore password.

> **Back up `goldpulse-upload.jks` and its password somewhere safe (for example, a password manager).** With Play App Signing this is your *upload* key: Google holds the actual app signing key. If you lose the upload key you can ask Google to reset it from the Play Console, but that takes time.

### 2. Create `keystore.properties`

```bash
cp keystore.properties.example keystore.properties
```

```properties
# storeFile is relative to the project root, or an absolute path
storeFile=goldpulse-upload.jks
storePassword=your-keystore-password
keyAlias=goldpulse-upload
keyPassword=your-keystore-password
```

`keystore.properties`, `*.jks`, `*.keystore`, `local.properties` and all `build/` folders are in `.gitignore`. **Never commit them.**

### 3. Build the signed App Bundle

```bash
./gradlew bundleRelease
```

The bundle is written to:

```
app/build/outputs/bundle/release/app-release.aab
```

Check that it is signed with your key:

```bash
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

If `keystore.properties` is missing, Gradle prints a warning and builds an **unsigned** bundle, which Play will reject.

### 4. For every update

Increase `versionCode` (and usually `versionName`) in [`app/build.gradle.kts`](app/build.gradle.kts) before running `bundleRelease` again.

---

## Hosting the privacy policy on GitHub Pages

1. Push this project to a **public** GitHub repository. Free GitHub Pages needs a public repo; if you want to keep the code private, put only `privacy-policy.md` in a separate public repo.
2. Open the repo's **Settings → Pages**, set **Source** to *Deploy from a branch*, choose `main` and `/ (root)`, then **Save**.
3. After a minute the policy is live at
   `https://<your-github-username>.github.io/<repo-name>/privacy-policy.html`
   (for this repository: `https://aideepkosh.github.io/GoldPulse/privacy-policy.html`)
4. Paste that URL into Play Console → **App content → Privacy policy**.

The policy names the developer as Deepkosh Solutions (the Play Console developer name) with the contact address ai.deepkosh@gmail.com. Keep both the same as in the Play Console developer profile.

## Play Console notes

- **Data safety:** "No data collected" and "No data shared". The app has no SDKs that collect data, and the only network calls download public prices (see the privacy policy).
- **Ads:** No ads.
- **Category:** Finance. **Target audience:** 18+ is the simplest choice for a finance app.
- **Store listing assets:** `play-store/icon-512.png` (app icon), `play-store/feature-graphic-1024x500.jpg`, and three phone screenshots in `play-store/screenshots/` (1080 x 1920 JPEG; Play needs a 9:16 ratio and no transparency). Listing text is ready to paste in `play-store/listing.md`.
- **App name:** "GoldPulse: Daily Gold Rate" in the listing. The launcher label is the shorter "GoldPulse".

## Library versions and compileSdk 36

The app compiles against SDK 36 as specified. Newer AndroidX releases (Compose 1.12+, core 1.19+, lifecycle 2.11+) need compileSdk 37, so [`gradle/libs.versions.toml`](gradle/libs.versions.toml) pins the newest versions that support 36. Lint reports those newer versions as warnings; this is expected. To upgrade, set `compileSdk = 37` and bump the versions in the catalog. `targetSdk` can stay at 36.

## Disclaimer

GoldPulse shows an indicative rate calculated from public market data. It excludes 3% GST, making charges and local premiums, and is not financial advice.
