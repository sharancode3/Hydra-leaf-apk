# Hydra Leaf Prototype

A lightweight Jetpack Compose Android game prototype where a single leaf glides across a watercolor river and the player steers only by tilting the device. The entire scene is rendered on a logical 1080x1920 viewport that scales to any phone size, aspect ratio, or pixel density.

## Project layout

```
app/
  src/main/java/com/example/hydraleaf/
    MainActivity.kt        # Entry point + sensor lifecycle glue
    GameViewModel.kt       # MVVM state container, physics, obstacles, collisions
    LeafGameScreen.kt      # Compose UI, Canvas renderer, viewport math, HUD
    SensorUtils.kt         # SensorManager controller + low-pass filtering helpers
    ViewportUtils.kt       # logicalToScreen / screenToLogical helpers and mapping data class
    GameConstants.kt       # Virtual world dimensions and tuning constants
  src/main/AndroidManifest.xml
README.md                  # You are here
```

## Build & dependency notes

- Kotlin DSL Gradle files (`settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`) are configured for AGP **8.6.0**, Kotlin **1.9.24**, and the Compose BOM **2024.10.00**.
- Compose is already enabled via `buildFeatures.compose = true` and `kotlinCompilerExtensionVersion = 1.5.15`, so UI code in `LeafGameScreen` works out of the box.
- `app/build.gradle.kts` wires in Material3, Activity Compose, lifecycle runtime/viewmodel Compose APIs, and `kotlinx-coroutines-android` so the `StateFlow` sensor stream compiles.
- `compileSdk` / `targetSdk` are pinned to **34** with `minSdk 24`. Adjust those fields if you need older device support.
- Release builds reuse the default optimized ProGuard config; extend `app/proguard-rules.pro` when you harden the prototype.

## Building an APK

1. Install **Android Studio Ladybug (2024.3)+** with Android SDK 34 and the latest build tools.
2. Use *File ▸ Open...* and select the repo root. Studio imports the Kotlin DSL project, runs a Gradle sync with its bundled Gradle runtime, and configures the emulator/device list. If you also want CLI builds, trigger the `wrapper` task once from the Gradle tool window (or run `gradle wrapper --gradle-version 8.7` after installing Gradle) so that `gradlew` scripts are generated.
3. Choose *Build ▸ Make Project* to verify, then *Build ▸ Build Bundle(s) / APK(s) ▸ APK*. The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk` and can be installed via *Build ▸ Analyze APK* or `adb install`.

After the wrapper exists you can build outside Studio with:

```bash
./gradlew assembleDebug
```

Use `gradlew.bat assembleDebug` on Windows. Release artifacts live under `app/build/outputs/apk/release/` when you run `assembleRelease`.

## Virtual viewport & scaling

- Logical size lives in `GameConstants.VIRTUAL_WIDTH` / `VIRTUAL_HEIGHT` (default **1080f x 1920f**).
- `LeafGameScreen` recomputes the mapping each recomposition:
  - `scale = min(screenWidth / virtualWidth, screenHeight / virtualHeight)`
  - `offsetX = (screenWidth - virtualWidth * scale) / 2`
  - `offsetY = (screenHeight - virtualHeight * scale) / 2`
- Helper functions `logicalToScreen(PointF)` and `screenToLogical(PointF)` (see `ViewportUtils.kt`) are used for drawing, tap hit‑testing, and collision checks.
- To change the logical world, edit the constants and adjust art sizes / spawn logic if needed. All rendering and physics will pick up the new numbers automatically because they depend on the constants and viewport mapping.

## Controls & sensors

- `SensorController` registers `TYPE_ROTATION_VECTOR` with an accelerometer/magnetometer fallback.
- Raw roll radians are normalized to `[-1, 1]`, low-pass filtered, and exposed as a `StateFlow` consumed by Compose.
- Physics mapping (in `GameViewModel.updateGameState`) steers the leaf toward a `targetX` computed as `virtualWidth/2 + tiltFactor * virtualWidth * 0.4f`. Damping + stiffness provide springy motion and stay frame-rate independent thanks to the `withFrameNanos` driven delta time loop.

## Gameplay systems

- Obstacles spawn at the top with random widths/speeds, flow downstream, and award score when they pass the leaf.
- `detectCollision` converts both `RectF` bounds through the viewport helper to keep hit tests consistent with the pixels on screen. AABB math is kept simple for readability.
- A debug overlay (tap the top-right logical quadrant or hit the HUD button) shows current virtual size, scale, and offsets so you can verify alignment on odd aspect ratios.

## Testing & tuning checklist

1. **Emulators by aspect ratio**
   - 9:16 (Pixel 3) – baseline portrait experience.
   - 18:9 (Pixel 5) – confirms safe areas + tall layouts.
   - 19.5:9 (Pixel 7/8) – stresses notch spacing and the inset-aware HUD.
   - Optional: rotate to landscape to verify that objects still scale and letterboxing behaves despite the portrait lock.
2. **Real device** – any mid-range phone (e.g., Pixel 6a, Galaxy A54). Ensure sensors respond smoothly and tune `LEAF_STIFFNESS`, `LEAF_DAMPING`, or `MAX_TILT_FACTOR` if motion feels sluggish or twitchy.
3. **Debug overlay** – toggle it and confirm that the leaf, obstacles, and HUD remain centered despite different DPIs/resolutions.
4. **Sensor sanity** – run in an emulator with the virtual sensors panel to script tilt curves, then replay on hardware.
5. **Performance** – profile with `adb shell dumpsys gfxinfo <pkg>` or `adb shell am profile` to confirm steady frame pacing. The Canvas renderer reuses `Path` objects and avoids per-frame allocations to keep GC noise low.

## Customization tips

- Swap the programmatic leaf path / obstacle colors with bitmaps by replacing the draw calls in `LeafGameScreen`. Keep positions in logical units and convert them through `logicalToScreen` before drawing.
- Adjust spawn frequency or difficulty curves in `GameConstants` and `GameViewModel.spawnObstacle()`.
- To expose additional HUD buttons, use Compose layouts outside the `Canvas` but still inside the `Box` so they inherit safe area padding.

## Manual test notes

- Sensors: verify smoothing by logging `SensorController.tiltState` and ensuring the value stays within [-1, 1].
- Collisions: turn on the debug overlay, pause the game, and inspect obstacle alignment to ensure logical/spatial calculations match the viewport.
- Resilience: leave the app, resume, and confirm sensors automatically re-register via `setupSensors()` / `tearDownSensors()`.

Happy floating! 🌿
