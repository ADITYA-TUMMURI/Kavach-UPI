# 🚀 Devpost Submission Copy: Kavach-UPI

Use this template to fill out your project details on the Devpost submission portal for HACKFEST'26.

---

## 💡 Project Title
**Kavach-UPI: On-Device Anti-Screen-Sharing Shield for Payments**

## 🏷️ Short Tagline
An on-device security shield that blocks remote access tools and prevents social engineering UPI payment fraud in real time.

---

## 📖 Detailed Description

### 🌟 Inspiration
Digital payment fraud in India has skyrocketed, with scammers exploiting remote-access tools like AnyDesk, TeamViewer, or RustDesk. Scammers impersonate bank customer support representatives or utility agents, convincing victims to download these remote screen-sharing tools. Once the connection is active, the attacker watches the user's screen in real time, intercepts OTPs, and captures UPI PINs as the victim makes a payment. 

We built **Kavach-UPI** to serve as an on-device "Seatbelt for UPI Payments" — ensuring that the moment screen-sharing is active, the user's sensitive payment fields are immediately shielded and locked.

### 🛡️ What it does
**Kavach-UPI** runs silently in the background as an Android Accessibility Service. It intercepts the device's window state transitions and monitors running tasks.
1. **Context Awareness**: It detects when a user opens a UPI application (such as Google Pay, PhonePe, Paytm, BHIM, CRED, or banking apps) and navigates to a payment or PIN entry screen.
2. **Threat Assessment**: Simultaneously, it audits whether a remote screen-mirroring tool is running or if a virtual display is active.
3. **Instant Mitigation**: If both conditions are met (UPI Payment Screen Open + Screen Sharing Active), Kavach-UPI instantly deploys a system-level opaque overlay blocking all touch inputs, protecting the screen from the remote viewer.
4. **Bilingual Auditory Warning**: It plays a looping Text-To-Speech alert (in English or Hindi) warning the user to disconnect the call immediately to protect their bank account.

### ⚙️ How we built it
* **Accessibility API**: Used for recursive, depth-first tree traversal of `AccessibilityNodeInfo` hierarchies to spot payment indicators (e.g. ₹ currency signs, "Enter UPI PIN" texts, and PIN input fields).
* **Dual-Gear Background Polling**: Optimized battery consumption by dynamically switching polling rates — querying at a rapid **1-second aggressive rate** when a transaction is open, and throttling down to a **3-second passive rate** when the user is doing other tasks.
* **WindowManager Overlay System**: Constructed a programmatic touch-sinking opaque view using the `SYSTEM_ALERT_WINDOW` permission to completely block remote clicks and touch gestures.
* **Bilingual TTS Engine**: Integrated Android's native `TextToSpeech` API to vocalize warning messages in English and Hindi based on preferences stored in `SharedPreferences`.

### 🚨 Challenges we ran into
* **Android Overlay Permissions**: Handling overlay draws across differing API versions (Oreo to Android 14) and ensuring the parent container blocks touch pass-throughs cleanly.
* **Accessibility Node Garbage Collection**: Dealing with memory overhead when scanning rapid tree modifications. We resolved this by implementing strict node recycling structures.
* **Teardown Performance & State Caching**: Eliminating background resource leaks (especially with TextToSpeech listeners and channel emitters) by introducing a strict 7-stage cleanup routine on service destruction.

### 🎉 Accomplishments that we're proud of
* Successfully creating a **fully programmatic, high-end visual UI** entirely in Kotlin without relying on bloated XML layout definitions.
* Achieving zero external library dependencies (besides Kotlin Coroutines), keeping the final APK file footprint at just **5.6 MB**.
* Designing a custom warning vector shield with glowing and pulsing rings that uses value animators to create a premium, reassuring security feel.

### 🧠 What we learned
* The depth of Android's accessibility architecture and the security hazards associated with overlay clickjacking.
* How to safely coordinate cross-thread communication using Kotlin channels to feed threat evaluations directly to main-thread UI operations.

### 🔮 What's next for Kavach-UPI
* **Touch Velocity Profiling**: Implementing an on-device TF-Lite model to distinguish local human touches from robotic, linear touch events issued by remote control APIs.
* **B2B Integration SDK**: Packaging our detection code into a plug-and-play library for commercial banks to integrate directly into payment apps, removing the need for broad Accessibility permissions.
