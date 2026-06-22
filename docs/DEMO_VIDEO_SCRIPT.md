# 🎥 Demo Video Script & Storyboard: Kavach-UPI

**Target Duration**: 3 Minutes  
**Objective**: Demonstrate the problem, the technical implementation, and a live threat simulation.

---

## 🎬 Storyboard Timeline

| Time | Scene / Visual | Voiceover (Narration) |
|---|---|---|
| **0:00 - 0:30** | **Slide 1**: Title slide with "Kavach-UPI" logo.<br>**Visual**: Screen-recording showing a phone screen with a UPI transaction and a mock attacker view side-by-side. | *"Imagine checking your bank account, making a payment, and not realizing someone is watching your every move. Today, remote access scam tools like AnyDesk and TeamViewer are weaponized by scammers to intercept OTPs and steal UPI PINs. Current payment apps don't actively detect if you are sharing your screen during transactions. That's why we built Kavach-UPI."* |
| **0:30 - 1:00** | **App Screen**: Show `MainActivity` on the emulator/device.<br>**Visual**: Navigate through the clean onboarding status ring card. Click the language buttons (English/Hindi) and alert modes (Voice/Siren). | *"Kavach-UPI is an on-device security shield. It runs a background accessibility scanner. On launch, users grant overlay and accessibility permissions through this clean, dark-mode dashboard. Users can customize warnings—switching alert types between custom sirens, spoken voices, or both, in English or Hindi."* |
| **1:00 - 2:00** | **Live Simulation (The Sandbox)**:<br>**Visual**: Click "TEST SHIELD OVERLAY". Watch the overlay deploy instantly. Play the voice alarm audibly in the video recording.<br>Then show a split screen showing how the user's payment screen is completely covered by the opaque shield, preventing the attacker from seeing or clicking any fields. | *"Let's see it in action. When we trigger our test sandbox, Kavach immediately deploys an opaque window-level safety overlay. It blocks all touch inputs, ensuring remote control script commands are discarded. Simultaneously, it plays a localized warning warning you to disconnect: (Audio plays: 'Attention! Kavach security shield is active...'). This protects the screen from the screen-recording feed."* |
| **2:00 - 2:40** | **Architecture Walkthrough**:<br>**Visual**: Show the code structure in VS Code/Android Studio. Point to `ThreatStateEvaluator` and the dual-gear polling coroutine in `KavachAccessibilityService.kt`. | *"Under the hood, Kavach-UPI uses a battery-optimized dual-gear coroutine loop. It checks at a passive 3-second interval, but shifts to a rapid 1-second aggressive rate the moment a payment screen is opened. It scans 4 distinct channels: window stacks, background services, virtual displays, and media-projection notifications. If an active screen-share matches a payment screen, the shield fires."* |
| **2:40 - 3:00** | **Outro**:<br>**Visual**: Display the Phase roadmap diagram (ML Touch Velocity, B2B SDK, Telemetry). Final call to action. | *"Entirely local, operating without internet, and packaging into a light 5.6 MB APK, Kavach-UPI sets a new benchmark for on-device payment safety. In the future, we plan to package this as a plug-and-play B2B SDK directly for banking apps. Protect your savings with Kavach-UPI. Thank you."* |

---

## 🎙️ Recording Tips for Person 2
1. **Split-Screen Layout**: If possible, use OBS Studio to record a split-screen video: one half showing the Android device screen, and the other showing a browser window running a remote access viewer (or mirroring app) to visually show the screen going black on the attacker's end while displaying the pulsing shield.
2. **Audio Setup**: Ensure the voice warning plays clearly. Turn up the device volume during the sandbox test so the microphone catches the bilingual Text-To-Speech repeating.
3. **Pacing**: Keep your narration crisp, clear, and steady. Do not rush. Let the visual overlay stay on screen for the full 5 seconds of the sandbox test to highlight the micro-animations of the pulsing rings.
