# Universal Cache Clearer

A lightweight Android utility for safe cache cleanup and storage management.

## What it does

- Shows the current cache size of Universal Cache Clearer itself.
- Clears the app's own cache with one tap.
- Opens Android's storage manager for system-wide app-cache management.
- Avoids hidden/root-only APIs and does not touch another app's private data.
- Targets Android 6.0+ and is suitable for low-resource phones.

## Important Android limitation

Modern Android versions intentionally prevent ordinary third-party apps from silently deleting other applications' private caches. Universal Cache Clearer therefore uses public Android APIs and hands system-wide cleanup to Android's own storage UI instead of pretending it has unrestricted access.

## Build

The GitHub Actions workflow builds a debug APK with Java 17 and Gradle 8.10 and publishes the APK as a workflow artifact.
