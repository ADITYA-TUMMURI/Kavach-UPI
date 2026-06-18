# 🛡️ Kavach-UPI

**An on-device security shield that leverages the Android Accessibility API to detect active screen-sharing apps during financial transactions. When a threat is detected, it instantly deploys an opaque full-screen overlay to block remote touch inputs and plays a localized audio warning to stop social engineering scams.**

> **HACKFEST'26 Submission** · Licensed under the **MIT License**

---

## The Problem

Sophisticated social engineering scams trick users into installing remote-access tools — such as AnyDesk, TeamViewer, or RustDesk — under the guise of customer support. Once installed, these tools give attackers **real-time visibility** into the victim's screen, enabling them to view OTPs, intercept UPI PINs, and bypass payment app protections while the user is unaware their screen is being mirrored.

## The Solution

**Kavach-UPI** acts as an active, on-device security shield. It runs a **dual-gear background coroutine service** that continuously intercepts window states, scans for known threat applications across the OS process table, and — the moment a user enters a UPI payment screen while a screen-sharing tool is active — **instantly blocks all unauthorized remote input** using a system-level opaque overlay and an audible safety warning.

No cloud. No internet. Entirely on-device.

---

## Core Features

- **Active Window Tree Parsing** — Performs recursive depth-first traversal of the live `AccessibilityNodeInfo` hierarchy to confirm when a user is on a financial transaction screen (detecting UPI PIN fields, payment buttons, and currency markers).
- **Dual-Gear Background Polling** — Optimized background processing that shifts between **1-second aggressive polling** during active payment contexts and **3-second passive polling** to conserve battery when no financial app is in the foreground.
- **Opaque Security Overlay** — Programmatic, full-screen overlay deployed via the `SYSTEM_ALERT_WINDOW` API that freezes all remote touch inputs, rendering screen-sharing tools unable to interact with the device.
- **Localized Threat Audio** — Instant looping audio warning played over the system alarm stream to alert the user audibly, with automatic fallback to a tone generator if the device's default alarm URI is unavailable.
- **Background Process Scanning** — Targeted `PackageManager` and `ActivityManager` queries to detect threat apps running as invisible background services, even when they hold no visible window.
- **Media-Projection Detection** — Monitors for active virtual displays and intercepts notification metadata to identify live screen-capture or casting sessions initiated by threat applications.

---

## Technical Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (Android Native) |
| **API Target** | Android SDK 34 (Minimum SDK 26 / Android 8.0 Oreo) |
| **Core Frameworks** | Android Accessibility API, kotlinx-coroutines |
| **State Management** | AtomicBoolean flag array with lock-free merge evaluation |
| **Data Caching** | SharedPreferences (persistent state recovery across process death) |
| **UI Orchestration** | Conflated Kotlin Channel command pipeline (background → main thread) |
| **Build System** | Gradle Kotlin DSL with Version Catalog |

---

## Project Structure

```
app/src/main/kotlin/com/kavach/upi/
├── detection/
│   ├── ThreatSignatureStore        # Known threat package registry
│   ├── UpiTargetRegistry           # Financial/UPI app identifier set
│   ├── PaymentScreenVerifier       # Recursive DFS node-tree payment confirmation
│   ├── BackgroundProcessScanner    # OS process table & virtual display inspector
│   └── MediaProjectionDetector     # Notification & UsageStats projection checker
├── service/
│   ├── KavachAccessibilityService  # Core service — lifecycle, polling, event handling
│   ├── ThreatStateEvaluator        # Four-channel atomic merge + state machine
│   └── UICommand                   # Sealed command vocabulary for the UI channel
├── overlay/
│   └── ThreatOverlayManager        # WindowManager overlay deployment & dismissal
├── audio/
│   └── WarningAudioPlayer          # MediaPlayer loop + ToneGenerator fallback
└── MainActivity                    # Permission onboarding interface
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
