# 🎯 SavingGoalPlus

<p align="center">
  <img src="app/src/main/res/drawable/ic_app_icon_512.png" alt="SavingGoalPlus Logo" width="128" height="128" style="border-radius: 20%;" />
</p>

<p align="center">
  <strong>A modern, privacy-focused, zero-ads savings goal manager for Android.</strong><br>
  Built with 100% Kotlin, Jetpack Compose, and Material 3.
</p>

<p align="center">
  <a href="https://github.com/Minta1234/SavingGoalPlus/actions"><img src="https://img.shields.io/github/actions/workflow/status/Minta1234/SavingGoalPlus/build-and-release.yml?branch=main&label=Build%20%26%20Release&logo=githubactions" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-16%20(API%2036)-3DDC84?logo=android&logoColor=white" alt="Android 16">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.0">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License: MIT"></a>
  <img src="https://img.shields.io/badge/100%25-Free%20%26%20Ad--Free-brightgreen" alt="Free & Ad Free">
</p>

---

## 📖 Overview

**SavingGoalPlus** is a complete, modern rewrite of the popular Android savings goal tracking concept. Built from the ground up to replace legacy architectures, SavingGoalPlus provides an elegant, distraction-free interface to track and manage personal savings goals without subscriptions, locked features, or invasive analytics.

Every feature from the original concept has been faithfully preserved with 100% mathematical and structural parity, and elevated with modern Android 16 capabilities, particle physics animations, customizable daily reminder notifications, and native widgets.

---

## ✨ Features

- **🎯 Target Goal Management**
  - Create and edit financial goals with custom names, target amounts, and deadline dates.
  - Attach personalized photo covers or auto-generated color initials.
  - Real-time percentage badges and progress metrics.

- **📊 Dynamic 260° Arc Gauge & Projections**
  - Visually track progress on a custom vector arc gauge with smooth fluid sweep animations.
  - Multi-tier dynamic color stages:
    - 🟠 **$\le 30\%$**: Amber / Warming up
    - 🔵 **$31\% - 60\%$**: Cyan / Steady growth
    - 🟢 **$> 60\%$**: Emerald Green / Reaching target
  - Precise daily, weekly, bi-weekly, and monthly required savings projections.

- **💰 Transaction Ledger (Deposits & Withdrawals)**
  - Fast logging of deposits (`+`) and withdrawals (`-`).
  - Add descriptive notes and custom transaction timestamps.
  - Real-time balance recalculation and historical payment log.

- **⏰ Smart Daily Reminders (Notification at Custom Time)**
  - Set a scheduled daily reminder time to stay disciplined.
  - Powered by Android `AlarmManager` with boot receiver support (`RECEIVE_BOOT_COMPLETED`), ensuring notifications persist after device reboots.
  - Native notification channel with priority alerts and in-app test trigger.

- **🎉 Celebratory Confetti Physics Animation**
  - When any savings goal hits 100%, an interactive 2D confetti particle explosion celebrates your financial victory!
  - Realistic physics engine simulating gravitational acceleration, horizontal drag, angular velocity, and random colors.

- **📱 Home Screen AppWidget**
  - Native Android AppWidget showing total saved amount, remaining balance, and active goal count directly on your launcher.

- **🗄️ Completed & Archived Goals**
  - Separate archive section for completed milestones.
  - Keep your main dashboard uncluttered while maintaining full historical records.

- **💾 100% Offline, Privacy & Backup**
  - Fully local SQLite storage with zero cloud dependencies or remote tracking.
  - One-click JSON backup export and import (compatible with legacy SQLite database schemas).
  - One-click CSV export for external spreadsheet analysis in Excel or Google Sheets.

- **🎨 Modern Material 3 & Custom Branding**
  - Full support for dynamic color themes (Material You), Dark Mode, and Light Mode.
  - Custom hand-crafted vector app icon (non-AI, geometric finance vault & growth sprout symbol).

---

## 🛠️ What Was Fixed & Upgraded (Comparison with Legacy App)

| Feature | Legacy App | SavingGoalPlus (v2.0) |
| :--- | :--- | :--- |
| **Architecture** | Obsolete Java (2018), XML Layouts | Modern **Kotlin 2.0 + Jetpack Compose** |
| **Android Version Support** | Outdated API levels | **Android 7.0 (API 24) to Android 16 (API 36)** |
| **Monetization & Ads** | Banner ads, interstitial ads, paywalls | **100% Free, Zero Ads, Zero Paywalls** |
| **Total Summary Dashboard** | Locked behind premium in-app purchase | **Permanently Unlocked & Free** |
| **Celebrations** | Static text | **Dynamic Confetti Particle Physics Animation** |
| **Notifications** | Unreliable background alarms | **Precise `AlarmManager` + Reboot Auto-reschedule** |
| **Home Screen Widget** | Basic legacy remote views | **Refined Modern AppWidget** |
| **Storage & Backup** | Hardcoded internal DB | **StateFlow Reactive SQLite + JSON & CSV Export/Import** |
| **App Icon** | Low-res bitmap | **Vector-drawn multi-density Adaptive Icon** |

---

## 🏗️ Architecture & Tech Stack

- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose with Material 3 design tokens
- **Target SDK**: Android 16 (API Level 36)
- **Minimum SDK**: Android 7.0 (API Level 24)
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Database Engine**: Direct SQLite (`SavingsGoalDbHelper`) with relational schema:
  - `MAINLIST` (Goal entities, targets, dates, metadata)
  - `PAYMENTLIST` (Transaction history, foreign-keyed to goal IDs)
- **Serialization**: `kotlinx.serialization.json`
- **Dependency Management**: Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Unit Testing**: JUnit 4 with comprehensive test suite covering mathematical projection edge-cases.

---

## 🚀 Building & Running from Source

### Prerequisites
- **JDK 17** or **JDK 21** (e.g. Eclipse Adoptium Temurin 21)
- **Android SDK** with Platform API 36 and Build-Tools 36.0.0
- **Git**

### Build Debug APK
```bash
git clone https://github.com/Minta1234/SavingGoalPlus.git
cd SavingGoalPlus
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease
```
The signed release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 🔄 Automated CI/CD & Version Release Worker

This repository includes an automated GitHub Actions release workflow in [`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml):
- **Automated Verification**: Runs on every pull request and push to `main` (runs all unit tests and builds test APKs).
- **Dynamic Semantic Versioning (`1.0.0` → `1.0.1` → `1.0.2` ...)**:
  - **Git Tag Releases**: Pushing any tag (e.g., `v1.0.0`, `v1.0.1`, `1.0.1`) automatically derives `versionName` and Android `versionCode`, builds the release APK, and publishes a new GitHub Release.
  - **One-Click Manual Release (`workflow_dispatch`)**: Triggered directly from the GitHub Actions UI. You can either type an explicit version (e.g. `1.0.1`) or let it auto-bump the patch/minor/major version, update `gradle.properties`, create the tag, build the APK, and publish the release automatically.

---

## 👥 Contributors

* **[Minta1234](https://github.com/Minta1234)** — Project Creator, Lead Developer & Maintainer.
* **[Antigravity](https://github.com/google-deepmind)** (Google DeepMind) — Advanced AI Pair Programmer & Systems Architect (system architecture, modern Kotlin & Jetpack Compose rewrite, SQLite data layer, 260° arc gauge, confetti physics engine, notification alarms, and CI/CD automation).

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for full project attribution.

---

## 📄 License

This project is open-source and released under the terms of the **[MIT License](LICENSE)**.

```
Copyright (c) 2026 Minta1234
```

