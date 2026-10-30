# GT Static Recomp — Android

Android build of [GTStaticRecomp](https://github.com/LorenzoAlfaro/GTStaticRecomp), the static
recompilation of Gran Turismo (PS1, USA v1.1, SCUS-94194). The recompiled game code and the
runtime are the same as on Windows; this project packages them as an APK (arm64-v8a for phones,
x86_64 for the Android emulator and x86 Chromebooks, optionally armeabi-v7a for 32-bit Android
TVs) with SDL2's Android glue, built with the SDK/NDK tools directly (no Gradle).

**No game code or data is included.** The APK contains code recompiled from your own disc, so it
is for personal use only and must not be distributed.

## Status

* **Phones:** a Pixel 7a runs the intro movie, menus, Simulation Mode and races at full speed
  (60 fps) at 2x internal resolution, filling the 20:9 screen, with music and sound.
* **Emulator:** an x86_64 Android emulator runs it correctly but slower, since its software
  GPU takes ~20 ms per frame to present. Good for testing, not for playing.
* **Save states** work as on Windows and survive restarting the app (see GTStaticRecomp's
  README). Without a keyboard they are reached through test options for now.
* **Defaults:** 2x internal resolution and the "fill" aspect (widescreen at the screen's own
  shape).
* **Input:** game controllers over USB or Bluetooth (SDL mappings). Click the left stick to
  cycle 4:3 / 16:9 / fill and the right stick to switch 1x / 2x. No touch controls yet.
* Settings, memory cards, save states and the log (`gt.log`) live next to the disc image in the
  app folder.

## Building

1. Build [GTStaticRecomp](https://github.com/LorenzoAlfaro/GTStaticRecomp) once on Windows with
   `build.ps1 -Disc ...`, in a folder next to this one (`..\GTStaticRecomp`). That extracts the
   game code into `..\GTStaticRecomp\data`.
2. Install Android Studio (for the SDK, platform-tools and its bundled JDK), then the NDK and
   CMake: `sdkmanager "ndk;29.0.14206865" "cmake;3.31.6"`.
3. `pwsh .\build.ps1 -Install` builds `gt-recomp.apk` and installs it over USB or wireless
   debugging. `-Abis x86_64` builds only the emulator version, `-Abis armeabi-v7a` a 32-bit
   one, `-NoStrip` keeps symbols for profiling.

The runtime and the game code are built without the stack protector: save states restore the
game's native stack in a later process, and a per-process canary on it would not match.

## Copying the disc

Start the app once so it creates its folder, then copy the `.cue` and `.bin` into it:

```
adb shell am start -n io.github.lorenzoalfaro.gtrecomp/.GTActivity
adb push "Gran Turismo (v1.1).cue" "Gran Turismo (v1.1).bin" /sdcard/Android/data/io.github.lorenzoalfaro.gtrecomp/files/
```

The folder must be created by the app (not by `adb shell mkdir`), otherwise the app cannot read
it. Android's file manager can also copy files there on most devices.

## Testing from the PC

Command-line options go through the launch intent (the log is `gt.log` in the app folder):

```
adb shell "am start -n io.github.lorenzoalfaro.gtrecomp/.GTActivity -e args '--profile --frames 3000 --shot-every 300 . --keys 1600:start'"
```

* `--profile` logs the per-frame timing every 300 frames.
* `--shot-every N .` writes screenshots into the app folder (`adb pull` them).
* `--save-state-at N slot`, `--load-state slot`: scripted save state tests.

A virtual phone for testing: create an AVD with an x86_64 system image
(`avdmanager create avd -n gt_test -k "system-images;android-36;google_apis_playstore;x86_64"`),
start it with `emulator -avd gt_test -no-window -gpu swiftshader_indirect`, and build with
`-Abis x86_64` (or the default, which includes it).

## License

MIT, like GTStaticRecomp. SDL2 (zlib license) and its Java glue are downloaded at build time.
