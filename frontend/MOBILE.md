# Building the Android / iOS apps

The mobile apps are the same React web app wrapped by [Capacitor](https://capacitorjs.com/) - no
second codebase. `android/` and `ios/` are committed native project shells; `npx cap sync` copies
the latest web build into them.

## Before building for a real device (not local dev)

The dev server's `/api` and `/ws` proxy (see `vite.config.ts`) only exists while `vite dev` is
running on your machine. A packaged mobile app has no such proxy - it must be built with the
**absolute** URL of a real, reachable backend:

```bash
VITE_API_BASE_URL=https://api.yourdomain.example VITE_WS_BASE_URL=wss://api.yourdomain.example npm run build
npx cap sync
```

Skipping this and shipping the default (`/api`, relative) means the app will try to call itself
and every request will fail.

## Android

Requires a JDK Gradle actually supports (JDK 25 is too new for the current Gradle release -
"Unsupported class file major version" - use JDK 17 or 21) and the Android SDK command-line
tools + `platform-tools`, `platforms;android-34+`, `build-tools;34.0.0+`.

```bash
npm run build
npx cap sync android
cd android
./gradlew assembleDebug        # unsigned debug APK, installable as-is for testing
# output: android/app/build/outputs/apk/debug/app-debug.apk
```

For a Play Store release build you additionally need a signing keystore and `./gradlew
bundleRelease` (produces an `.aab`, not covered here).

## iOS

The `ios/` Xcode project is fully scaffolded and ready, but **actually building or running it
requires Xcode on macOS** - there is no way to compile or sign an iOS binary from Windows/Linux.
On a Mac:

```bash
npm run build
npx cap sync ios
npx cap open ios     # opens Xcode; build/run from there, or archive for TestFlight/App Store
```
