# Rotter Scoops (Android, Kotlin)

A modern client for rotter.net scoops — native Kotlin + Jetpack Compose.
This replaced the Flutter app (last Flutter commit is tagged `flutter-final`).

- Package `com.bennybarak.scoops.rotter_scoops`, signed with the same upload key,
  so it installs as an update over the Flutter build and keeps the user's data
  (settings, read state, saved/followed scoops, drafts, saved sign-in, AI key).
- Signing: put `key.properties` + the `.jks` in the project root (both git-ignored).

```sh
./gradlew :app:testDebugUnitTest     # parser / cache / summary tests (real captured pages)
./gradlew :app:assembleRelease       # signed APK → app/build/outputs/apk/release/
./android_web_install.zsh            # build + serve the APK to a phone over a tunnel
```
