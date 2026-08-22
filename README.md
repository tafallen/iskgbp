# ISK to GBP Converter Android App

So, I'm going to Iceland. I'm from the UK. I wanted something to help with the currency conversion. So this is largely generated with Google Antigravity. It's nothing much but it works.

A sleek, lightweight, and modern native Android application built with Kotlin and Jetpack Compose for checking the latest conversion rate between Icelandic Króna (ISK) and British Pound Sterling (GBP).

## Features

* **Today's Exchange Rate:** Displays today's real-time conversion rate dynamically.
* **Two-Way Live Converter:** A side-by-side interactive calculator. Type in ISK to see GBP instantly, or type in GBP to see ISK instantly.
* **Quick Preset Grid:** Shows equivalents for common ISK values: **1,000**, **3,000**, **5,000**, **6,000**, **8,000**, **10,000**, **15,000**, **20,000**, and **25,000** ISK. Tapping any card inserts the amount directly into the calculator.
* **Offline Caching:** Automatically caches the latest successfully loaded rate in `SharedPreferences` so the app starts up instantly and works offline.
* **Material 3 Design:** Beautiful Slate-Teal dark and light theme layouts with responsive sizing.

---

## Technical Architecture

### 1. Dual-API Networking with Fallback
To ensure high uptime, the app implements a redundant API query system running asynchronously via Kotlin Coroutines on a background thread (`Dispatchers.IO`):
1. **Primary API:** `https://open.er-api.com/v6/latest/ISK` (ExchangeRate-API)
2. **Fallback API:** `https://api.frankfurter.app/latest?from=ISK&to=GBP` (Frankfurter API)

If the primary API is unreachable or times out, the app silently calls the Frankfurter API to recover the rate.

### 2. State & Focus-Driven Inputs
The calculator monitors focus states (`onFocusChanged`) to resolve values in one direction at a time. This prevents infinite conversion calculation loops or text cursor jumping when typing.

---

## How to Install and Run

### Option 1: Direct Command-Line Installation (ADB)
If you have the Android SDK installed and your phone connected (wired or via ADB Wireless), you can deploy the debug APK directly from your terminal:

1. Open your terminal in the project directory.
2. Install the compiled APK:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
3. Launch the app on your phone:
   ```bash
   adb shell am start -n com.example.iskgbp/.MainActivity
   ```

### Option 2: Open in Android Studio
1. Launch **Android Studio**.
2. Click **File -> Open** and select this project directory.
3. Android Studio will automatically synchronize the Gradle settings and download the compiler wrapper.
4. Select your device/emulator in the top toolbar and click the green **Run** button (or press `Shift + F10`).

---

## Project Structure

* `app/src/main/java/com/example/iskgbp/MainActivity.kt`: App lifecycle and Jetpack Compose UI layout.
* `app/src/main/java/com/example/iskgbp/ui/theme/`: Custom color palettes, typography styles, and Material 3 theme configurations.
* `app/src/main/res/`: Layout assets, adaptive launcher vector icons, and strings resources.
* `build.gradle.kts` & `settings.gradle.kts`: Gradle build configuration scripts.
