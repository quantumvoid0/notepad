# Notepad

A simple, clean notepad app for Android built with Kotlin and Jetpack Compose.

## Features

- Text notes and checklists
- Pin notes
- Search
- Light, dark, and AMOLED themes
- Persistent storage

## Installing

- goto releases, download latest release and install it

## Building yourself for development

Requirements:
- JDK 17
- Android SDK (API 36)

```bash
git clone https://github.com/quantumvoid0/notepad
cd notepad
gradle wrapper --gradle-version 8.14.2
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```
