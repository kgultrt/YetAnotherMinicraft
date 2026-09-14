# Yet Another Minicraft

## What?

Yet another Minicraft mod, based on the original 2011 source code. No big goals — it just makes the game more comfortable to play.

## Why?

Notch built Minicraft in 48 hours for Ludum Dare 22. It is a complete, playable little game, and it does not try to be anything more than that. But the same 48 hours are visible everywhere: hardcoded resolutions, instant menu transitions, no save system, no localization.

The original is still fun to play. But on a modern phone, on a widescreen, in a language that isn't English, the rough edges start to feel less like charm and more like friction.

This project keeps the game exactly as it is, and only fixes the friction.

Just made it comfortable.

## Build & Run

That's simple:
```sh
./gradlew desktop:run
```
For Android:
```sh
./gradlew android:assembleDebug
```
Then install the APK from `android/build/outputs/apk/`.

(Standard libGDX project layout. The game logic lives in the `core` module, shared between desktop and Android.)

## Third party libraries

This project would not be possible without a few tools.

**libGDX** — cross-platform game framework. Replaces the original AWT rendering with something that runs on desktop and Android from a single codebase. The game logic itself is untouched.

**QuanPixel 8px** — a pixel font by diaowinner that fits a full CJK character set into 8×8 pixels. The original Minicraft used a tiny built-in Latin bitmap font; this project extends it to Chinese, Japanese, and Korean without breaking the 8×8 aesthetic. Licensed under the SIL Open Font License.
