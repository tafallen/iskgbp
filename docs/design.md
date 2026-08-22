# ISK to GBP Converter Android App Design

A lightweight, modern, and robust native Android application built with Kotlin and Jetpack Compose to view and convert ISK (Icelandic Króna) to GBP (British Pound Sterling).

## Features

1. **Today's Conversion Rate:** Fetches and displays the live conversion rate from ISK to GBP (with automatic redundant fallback to ensure reliability).
2. **Preset Values List:** Shows preset amounts of ISK (1,000, 3,000, 5,000, 6,000, 8,000, 10,000, 15,000, 20,000, and 25,000) and their current equivalents in GBP in a neat grid.
3. **Interactive Live Converter:** Two-way converter enabling the user to type an amount in ISK and see it in GBP, or type in GBP and see it in ISK.
4. **Offline Caching:** Saves the last successfully fetched rate in `SharedPreferences` so the app is immediately usable offline.
5. **State Handling:** Handles loading, success, and error states gracefully with modern visual feedback and a retry mechanism.
6. **Premium Dark/Light Material 3 Theme:** A sleek design utilizing glassmorphism-like cards, smooth elevation, custom typography, and dynamic colors.

---

## Technical Architecture

### 1. Network API Integration
To avoid heavy library overhead (like Retrofit or Ktor) and keep compile times extremely fast, the app uses a standard background thread with `HttpURLConnection` and the built-in Android `org.json` package.

- **Primary API:** `https://open.er-api.com/v6/latest/ISK`
- **Fallback API:** `https://api.frankfurter.app/latest?from=ISK&to=GBP`
- **JSON Parsing:** Using native Android `JSONObject` to extract the GBP rate.

### 2. UI Layer (Jetpack Compose)
- **State Management:** A simple UI state data class:
  ```kotlin
  data class CurrencyUiState(
      val isLoading: Boolean = false,
      val rate: Double? = null,
      val lastUpdated: String? = null,
      val errorMessage: String? = null
  )
  ```
- **UI Components:**
  - **Header Card:** Shows the current conversion rate, exchange rate date, and a refresh button.
  - **Quick Conversion Grid:** Responsive card layout showing the requested presets:
    - 1,000 ISK
    - 3,000 ISK
    - 5,000 ISK
    - 6,000 ISK
    - 8,000 ISK
    - 10,000 ISK
    - 15,000 ISK
    - 20,000 ISK
    - 25,000 ISK
  - **Interactive Calculator:** Card containing horizontal input fields for ISK and GBP side-by-side separated by an equal sign, with quick clear action and focus management.

### 3. Permissions
- Requires `<uses-permission android:name="android.permission.INTERNET" />` in the `AndroidManifest.xml` to fetch live exchange rates.
