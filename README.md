# WordLength 📊✍️

**WordLength** is a modern, feature-rich Android application designed for deep text analysis, word length distribution statistics, and linguistic insights. Built entirely with **Jetpack Compose** and following modern Android architecture guidelines (**MVVM**, Material 3), WordLength provides instant, comprehensive metrics for any text input.

---

## ✨ Key Features

- **Comprehensive Text Metrics**:
  - Word count, character count (with and without spaces), sentence count, and paragraph count.
  - Estimated reading time and speaking time calculations.
  - Lexical diversity and average word length analysis.
- **Advanced Unicode Text Tokenization**:
  - Powered by a robust `UnicodeTokenizer` ensuring accurate word extraction across diverse languages and character sets.
  - Configurable Stop Words filtering (`StopWords`) for cleaner statistical analysis.
- **Interactive Visualizations & Charts**:
  - **Length Histogram Chart**: Visualize word length distribution frequencies instantly.
  - **Distribution Bar Chart**: Detailed breakdown of character lengths per word.
  - **Animated Word Bars**: Smooth, dynamic UI animations showcasing text composition.
  - **Word Detail Bottom Sheet**: Tap into individual word frequencies and metrics.
- **History & Persistence**:
  - Save, review, and manage previous text analysis sessions.
- **Customizable Settings & Preferences**:
  - Tailor analysis rules, themes, and display preferences.
- **Modern UI / UX**:
  - Crafted with **Jetpack Compose** and **Material 3**, supporting dynamic theming and seamless edge-to-edge layouts.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & Flows
- **Navigation**: Jetpack Navigation Compose
- **Lifecycle & State**: ViewModel, StateFlow, Lifecycle KTX
- **Language**: Kotlin 100%
- **Min SDK**: API 26 (Android 8.0+)
- **Compile/Target SDK**: API 37
- **Testing**: JUnit, Kotlin Coroutines Test, Compose UI Testing

---

## 📁 Project Structure

```text
com.example.wordlength/
├── analysis/           # Text tokenization, stop words, length calculator, and analytics engine
├── data/
│   ├── model/          # Data classes for text metrics, word statistics, and history
│   ├── preferences/    # App preferences and settings storage
│   └── repository/     # History repository for saved analyses
└── ui/
    ├── components/     # Custom charts, stat cards, animated bars, and bottom sheets
    ├── navigation/     # App navigation graph and routes
    ├── screens/        # Home, History, Settings, About, and Splash screens
    ├── theme/          # Material 3 colors, typography, and theme definitions
    └── viewmodel/      # Main WordLengthViewModel managing state and business logic
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug or newer recommended)
- **JDK 17**
- **Android SDK** (API 37)

### Installation & Building
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/WordLength.git
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device (API 26+).

---

## 🧪 Testing

Run unit tests and UI tests via Gradle:
```bash
./gradlew test
./gradlew connectedAndroidTest
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
