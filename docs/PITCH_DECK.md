# 📊 Pitch Deck Structure: Kavach-UPI

Use this slide-by-slide structure to build your PPT, PDF, or Canva presentation for the HACKFEST'26 judges.

---

## 🛝 Slide 1: Title Slide (First Impression)
* **Title**: **Kavach-UPI**
* **Subtitle**: The On-Device Anti-Screen-Sharing Shield for Digital Payments
* **Visuals**: A phone mock-up showing the glowing red warning shield, with the text *"Securing UPI transactions from social engineering and remote control scams."*
* **Footer**: Team Name | HACKFEST'26

---

## 🛝 Slide 2: The Problem (The Trillion-Rupee Threat)
* **Header**: The Vulnerability of Digital Payments
* **Key Stats (Bullet Points)**:
  * **UPI Dominance**: India processes over **10 billion UPI transactions** monthly.
  * **Social Engineering Rise**: Scammers impersonate customer service or bank officials, tricking victims into downloading screen-sharing applications (e.g. AnyDesk, TeamViewer, RustDesk).
  * **The Exploit**: Once connected, the attacker watches the victim type their UPI PIN or intercepts verification SMS messages in real time.
* **Problem Statement**: *Existing UPI apps do not detect whether a screen-sharing tool is mirroring the screen during sensitive PIN entry, exposing billions to financial fraud.*

---

## 🛝 Slide 3: The Solution (Introducing Kavach-UPI)
* **Header**: A Shield for Your Bank Account
* **Key Value Props**:
  * **Zero-Trust Input**: Instantly deploys an opaque system overlay window that intercepts and swallows all remote touch inputs.
  * **Auditory Warning**: Loops a localized bilingual warning (English/Hindi) instructing the user to hang up the call immediately.
  * **Strict Privacy (100% On-Device)**: Operates without internet connectivity, third-party libraries, or server backend storage.
* **Tagline**: *"Providing active, real-time protection at the exact moment of transaction danger."*

---

## 🛝 Slide 4: Technical Architecture
* **Header**: Under the Hood of the Shield
* **Key Pillars**:
  * **Contextual Heuristics**: Continuous recursive depth-first parsing of the `AccessibilityNodeInfo` tree to find payment indicators (currency signs, PIN fields) only when target UPI/banking packages are in focus.
  * **Dual-Gear Loop**: Polling rate dynamically shifts from **3-second passive scan** to **1-second aggressive check** during active payment windows, conserving device battery life.
  * **Multi-Channel Scanner**: Merges state evaluations from 4 distinct vectors: running process services, notification text metadata, virtual displays, and active window stacks.
* **Diagram Idea**: Draw a flow showing the `AccessibilityEvent` entering `KavachAccessibilityService`, checked by `ThreatStateEvaluator`, pushing a command to the `WindowManager` and `WarningAudioPlayer`.

---

## 🛝 Slide 5: Features & Performance
* **Header**: Lightweight, Resilient, and User-Friendly
* **Features Grid**:
  * **Bilingual English/Hindi Voice Alerts**: Driven by the native TTS engine with Ringtone/ToneGenerator fallback.
  * **Interactive Onboarding Settings**: Set languages and alert styles, with quick permission statuses.
  * **Persistent Cache**: Recovers the shield state across process death using `SharedPreferences`.
  * **Low Footprint**: Final compiled package size is only **5.6 MB** with zero bloated dependencies.

---

## 🛝 Slide 6: Post-Hackfest Roadmap
* **Header**: Future Technical Scope
* **Phases Overview**:
  * **Phase 1: ML Behavioral Heuristics**: Lightweight on-device TensorFlow Lite model to analyze touch pressure, velocity, and pattern signatures to block robotic cursor scripting.
  * **Phase 2: Display Buffer Audit**: Inspecting `/dev/graphics/fb0` native framebuffers via NDK hooks to catch hardware-based capture cards.
  * **Phase 3: B2B Banking SDK**: Packaging the core threat detection engine as a standard SDK library for commercial payment apps (GPay, PhonePe, Paytm), eliminating the need for system-level Accessibility permissions.

---

## 🛝 Slide 7: Conclusion
* **Header**: Kavach-UPI
* **Core Takeaways**:
  * Simple, effective, and completely private.
  * Solves a massive real-world threat in India's digital payment ecosystem.
  * Fully functioning prototype with compiled Android APK ready for testing.
* **Footer**: GitHub Link | Devpost Link | Team Contact Info
