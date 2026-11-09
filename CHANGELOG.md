# 📋 SavingGoalPlus — Change Log

All notable changes to **SavingGoalPlus** will be documented in this file.
This project follows [Semantic Versioning](https://semver.org/).

---

## [v1.0.2] — 2026-11-10

### 🎨 Added
- **8 iOS-Style Theme Accent Palettes**:
  - 🟢 **Apple Mint (Emerald)** (`#10B981`) — Clean energetic green
  - 🔵 **Cupertino Blue** (`#007AFF`) — Classic Apple iOS system blue
  - 🟣 **Indigo Night** (`#5856D6`) — Deep sophisticated purple-blue
  - 🌸 **Rose Pink** (`#FF2D55`) — Vibrant lively pink
  - 🟠 **Sunset Orange** (`#FF9500`) — Warm radiant orange
  - 🌊 **Pacific Teal** (`#30B0C7`) — Cool ocean teal
  - 👑 **Royal Purple** (`#AF52DE`) — Elegant iOS royal purple
  - 🔘 **Midnight Slate** (`#64748B`) — Sleek minimalist graphite
- **In-App Change Log & Version Viewer**:
  - Accessible directly in **Settings → What's New / Change Log**.
- **1-Click Update Checker**:
  - Fast button in Settings linking directly to the latest GitHub release.

### 🔄 Fixed
- **Seamless In-Place APK Updates (No Uninstallation Required)**:
  - Generated a permanent project release keystore (`app/release.keystore`) with a 10,000-day valid certificate (`DC:3F:19:52:9B:7D:01:CE:6A:BA:4E:55:1A:7A:F2:FC:EF:B3:7B:4B:9A:02:67:7E:60:DD:C2:C6:DF:86:55:3E`).
  - Solved `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: Users can now update the APK directly in 1 tap without losing any data or uninstalling previous versions!

---

## [v1.0.1] — 2026-11-09

### 🍏 Added
- **100% Native iOS App Target (`ios/`)**:
  - Built natively with **Swift 5.9 + SwiftUI (iOS 16.0+)**.
  - Native **WidgetKit** home screen widget.
  - Apple `UNUserNotificationCenter` daily reminders.
  - 100% mathematical parity with Android projection calculations.
- **Sideloadable iOS Distribution Packages**:
  - Unsigned `.ipa` ready for AltStore, SideStore, Sideloadly, or TrollStore.
  - Full `.xcarchive.zip` bundle for Xcode Organizer and developers.
- **Selective Smart CI/CD Workflows**:
  - Path filtering prevents compilation when files outside the app are committed.

---

## [v1.0.0] — 2026-11-09

### 🚀 Initial Modern Release
- **100% Native Kotlin 2.0 Rewrite**:
  - Replaces legacy 2018 Java code with modern Jetpack Compose & Material 3.
  - Zero ads, zero paywalls, zero trackers. 100% free and open-source.
- **Dynamic 260° Arc Progress Gauge**:
  - Smooth animated progress arc with multi-tier dynamic colors (Amber, Cyan, Emerald).
- **Rate Projection Calculation Engine**:
  - Computes required daily, weekly, bi-weekly, and monthly savings rates.
- **Celebratory Confetti**:
  - Full-screen interactive 2D particle physics explosion when goals hit 100%.
- **Disciplined Daily Reminders**:
  - Android `AlarmManager` with boot receiver (`RECEIVE_BOOT_COMPLETED`) auto-reschedule.
- **Private Local Storage & Backup**:
  - SQLite relational database with JSON import/export and CSV export.
- **Vector Branding**:
  - Non-AI custom adaptive vector vault and sprout branding.
