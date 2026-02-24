# Re-NetGuard (Compose + M3)

<p align="center">
  <img src="assets/home.png" alt="Re-NetGuard overview" width="980">
</p>

<p align="center">
  <strong>Local-first, no-root firewall control with a modern Material 3 experience.</strong><br>
  Built with Kotlin 2.3, Jetpack Compose, and a first-party Android-only architecture.
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.3.10-blue.svg?logo=kotlin" alt="Kotlin 2.3.10"></a>
  <a href="https://developer.android.com/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-1.10.x-orange.svg?logo=jetpackcompose" alt="Jetpack Compose 1.10.x"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-green.svg" alt="License"></a>
  <a href="#"><img src="https://img.shields.io/badge/Platform-Android-lightgrey.svg" alt="Platforms"></a>
</p>

---

Re-NetGuard is a modernized fork of the original [NetGuard](https://github.com/M66B/NetGuard) project by Marcel Bokhorst. It preserves NetGuard’s core philosophy—no-root, VPN-based filtering, local-first processing, and no account lock-in—while adding a refreshed Material 3 Expressive Android UX with improved workflows for logs, rules, and settings.

## 🚀 Key Features

- 🛡️ **No-Root by Design:** Uses Android `VpnService` with local filtering, no external proxy dependency.
- 🎨 **UI-First Redesign:** Material 3 Expressive visuals, cleaner spacing, and improved interaction behavior.
- 📱 **Modern Navigation:** Adaptive patterns tailored for both phone and tablet form factors.
- 🔒 **Safer Controls:** Clearer toggles, better feedback, and improved update-check visibility.
- 📊 **Stronger Log Clarity:** Upgraded traffic timeline views with status, protocol, and app-level context.
- 🌍 **Localization-Aware:** Translated update states and expanded language coverage.

## 📸 Screenshots

| Home | App Access |
| :---: | :---: |
| ![Home](assets/home.png) | ![App Access](assets/firewall.png) |
| **Details** | **Settings** |
| ![Details](assets/details.png) | ![Settings](assets/settings.png) |

## 🛠️ Tech Stack

This project is a showcase of modern Android development:

- **UI:** [Jetpack Compose](https://developer.android.com/compose) (Material 3)
- **Architecture:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (DI)
- **Navigation:** [Navigation 3](https://developer.android.com/jetpack/compose/navigation) / Adaptive Navigation Suite
- **Data/State:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Concurrency:** [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- **Networking:** [OkHttp](https://square.github.io/okhttp/)
- **Theming:** [Material Kolor](https://github.com/jordond/MaterialKolor) (Dynamic Color)
- **Native Layer:** C++ JNI core

## 🏗️ Project Structure

The project follows a standard Android modular layout:

- `app/`: Android application module (entry point + UI + services).
- `app/src/main/kotlin/eu/faircode/netguard/ui/`: Compose screen structure and app shell.
- `app/src/main/kotlin/eu/faircode/netguard/`: Firewall services and system integration.
- `app/src/main/kotlin/eu/faircode/netguard/ui/screens/`: Main screen implementations.
- `app/src/main/jni/netguard/`: Native networking engine.

## 🏁 Getting Started

### Prerequisites

- **JDK 17** or higher.
- **Android Studio** (current stable) with the Android SDK (API 26+ target).

### Build & Run

#### Android
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

You can also build a release variant configured for production checks and shrinker behavior by running `./gradlew :app:assembleRelease`.

## ⚠️ Behavior Notes

- This app is local-first and does not proxy traffic through a third-party backend.
- Some device manufacturers apply stricter VPN policies; behavior can vary.
- Certain background/network capabilities depend on notification, battery optimization, and device permissions.

## 📜 Credits & License

- **Original Project:** [NetGuard](https://github.com/M66B/NetGuard) by Marcel Bokhorst.
- **License:** GNU GPLv3. See [LICENSE](LICENSE) for details.

---
<p align="center">Made with ❤️ using Jetpack Compose</p>
