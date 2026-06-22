# 🛡️ Kavach-UPI: Active On-Device Threat Interception & Fraud Prevention Shield

An edge-computed, zero-cloud-dependency security service engineered for native Android platforms to defend users against social engineering scams, remote-access exploits, and screen-sharing fraud during financial operations.

---

## 📌 The Core Problem & Vulnerability Context
Unified Payments Interface (UPI) ecosystems in India have suffered massive exploitation via highly coordinated remote social engineering traps. 

* **The Security Blind Spot:** Attackers trick users into installing legal screen-sharing or remote management utilities (e.g., AnyDesk, TeamViewer, RustDesk). Standard banking and payment applications operate inside application-level sandboxes that cannot programmatically detect when underlying OS-level media projection or virtual display frameworks are active.
* **The Exploitation Flow:** The threat actor monitors the user's screen in real-time, intercepts incoming multi-factor authentication tokens or one-time passwords (OTPs), and records or scripts touch layouts to execute unauthorized fund transfers.

Kavach-UPI directly bridges this architectural visibility gap at the system level.

---

## ⚙️ Architectural Solution & Hybrid Detection Engine

Kavach-UPI does not handle transactions; it acts as a low-overhead automated shield running silently in the background. By leveraging the native Android Accessibility framework, it monitors active window structures. When an active payment window is detected concurrently with a screen cast or remote execution signature, the service drops a blocking overlay and triggers defensive alerts.

### 1. Dual-Gear Background Polling Framework
To balance optimal processing responses with aggressive device battery preservation, the core service uses a dynamic, dual-frequency coroutine loop:
* **Passive Gear ($3000\text{ ms}$ interval):** When foreground applications do not match financial package signatures, the background scanner idles in low-power mode, executing sparse system checks.
* **Aggressive Gear ($1000\text{ ms}$ interval):** The exact millisecond a targeted payment layout is verified, the loop scales up frequency processing to analyze system flags continuously, reducing vulnerable time windows.

### 2. High-Priority Recursive Layout Traversal (DFS)
When a foreground window change occurs, the service executes a resource-constrained Depth-First Search (DFS) over the active view tree (`AccessibilityNodeInfo`):
* **Boundaries:** The traversal caps out at a maximum depth of 15 layers and evaluates a maximum of 200 elements per pass to prevent main-thread UI jank.
* **Target Heuristics:** The engine inspects text metadata and layout components for explicit transaction targets (e.g., "Enter UPI PIN", "Pay ₹", `pin_entry`, `amount_field`) alongside sibling-relative checking near currency signs.
* **Memory Management:** To stop background Binder proxy exhaustion and avoid system crashes, explicit node-recycling sequences (`recycle()`) are run on every single child element checked.

### 3. API 29+ Media Projection Subsystem
* On modern devices (Android 10+ / API 29+), the app intercepts `UsageEvents` over a 10-second history window. It filters for active `FOREGROUND_SERVICE_START` indicators and verifies if the service flags match the explicit `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` runtime token.
* **Legacy Fallback:** On devices running below API 29, the engine queries running background signatures dynamically against a localized process store (`ThreatSignatureStore`).

---

## 📊 System State Machine Matrix

The execution states are evaluated locally using a thread-safe atomic merging sequence that updates the system state engine sequentially on every polling cycle:

$$\text{Overlay State} = \begin{cases} \text{Active (Block Input)} & \text{if } C_{\text{UPI}} \land (P_{\text{Media}} \lor D_{\text{Virtual}}) \\ \text{Inactive (Pass Input)} & \text{otherwise} \end{cases}$$

### Anti-Flicker Hysteresis Stabilization
To maintain absolute stability during rapid activity redraws, the system integrates a 2-cycle hysteresis rule. The threat state engine must record exactly two consecutive clean, risk-free polling cycles before dropping an active shield, completely eliminating overlay flickering or race conditions.

---

## 🛠️ Deep Technical Stack & Core Modules
* **Core Language:** Native Kotlin matching Java 17 JVM targets.
* **Concurrency:** `kotlinx-coroutines-android` bound to a thread-safe `SupervisorJob` managing distinct IO/Default dispatch pipelines.
* **UI Layer:** Opaque glassmorphism container built entirely via programmatic window layout parameters (`#0F0F12` to `#1C1E26` canvas with glowing rings driven by a custom `ValueAnimator`).
* **Audio Warning Engine:** Bilingual (English & Hindi) Text-to-Speech warnings injected dynamically into system alert paths with a hardcoded `ToneGenerator` hardware beep fallback.

---

## 🔐 Required System Permissions & Access Tokens

Because Kavach-UPI operates entirely on-device at the OS boundary layer without dependency on external web servers or cloud telemetry, it requires the following platform permission tokens:

1. `android.permission.BIND_ACCESSIBILITY_SERVICE`  
   **Usage:** Mandated to capture layout hierarchies and identify live banking contexts.
2. `android.permission.SYSTEM_ALERT_WINDOW`  
   **Usage:** Allows the injection of the full-screen programmatic layout overlay on the WindowManager to sink and block unauthorized remote touch streams.
3. `android.permission.PACKAGE_USAGE_STATS` (Protected Signature Permission)  
   **Usage:** Essential for parsing system usage timelines to detect API 29+ active media projection services.

* **Onboarding Guard:** If `AppOpsManager` detects that tracking permissions are missing, the `MainActivity` onboarding panel switches the UI to `⚠️ EXTRA SETUP REQUIRED` and blocks local threat testing sandbox modules until the user is directed to the system panel via `Settings.ACTION_USAGE_ACCESS_SETTINGS`.

---

## ⚖️ Open-Source Evaluation License
This prototype is released under the **MIT License**. It offers zero-friction open-source cloning, evaluation, and verification pipelines for all review panels.
