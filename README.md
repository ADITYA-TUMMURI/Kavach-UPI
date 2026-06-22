# 🛡️ Kavach-UPI

An on-device, edge-computed security shield designed to protect users against remote-access financial fraud and social engineering scams during live transactions.

## 📌 The Vulnerability & Solution
Standard UPI and banking applications remain completely blind to underlying Android OS-level media casting hooks, allowing malicious threat actors to capture screens and inject overlay touch scripts remotely. 

Kavach-UPI monitors active system window contexts. When a screen-sharing signature or an unverified virtual display process is validated concurrently with a live financial window, the app intercepts the vector:
* **Forcefully drops** a full-screen, opaque glassmorphism layout to block touch inputs.
* **Broadcasts bilingual audio alerts** (English/Hindi) instructing immediate network disconnection.

$$\text{Overlay State} = \begin{cases} \text{Active (Block Input)} & \text{if UPI App Open} \land \text{Screen Share Active} \\ \text{Inactive (Pass Input)} & \text{otherwise} \end{cases}$$

## ⚙️ Architecture & Implementation Gaps Patched
* **Dual-Gear Background Polling Loop:** Alternates frequency profiles automatically ($1000\text{ ms}$ aggressive payment tracking / $3000\text{ ms}$ relaxed idling) to preserve hardware resources.
* **API 29+ Media Projection Subsystem:** Intercepts `UsageEvents` history timelines on Android 10+ devices to catch `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` flags instantly.
* **Thread-Safe Synchronization:** Atomic evaluations (`virtualDisplayThreatDetected`) link the hardware process scanner with the active accessibility node verifier.

## 🔐 System Permissions & Onboarding Requirements
To implement low-latency OS-level protection loops without network dependencies, Kavach-UPI requires explicit local validation for:
1. `BIND_ACCESSIBILITY_SERVICE` — Parses active view-tree layers dynamically to identify vulnerable states.
2. `SYSTEM_ALERT_WINDOW` — Spawns the programmatic input-blocking layer.
3. `PACKAGE_USAGE_STATS` — Inspects the system foreground configuration timeline.

## ⚖️ License
Distributed under the **MIT License**. Completely friction-free open-source evaluation.
