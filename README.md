# Universal Cache Clearer

A polished Android storage utility built around Android's official cache-management APIs.

## What it does

- Uses Android's `ACTION_CLEAR_APP_CACHE` request to ask the operating system to clear app caches across the device when the device exposes that capability.
- Shows the app's own temporary-cache size.
- Provides a fallback into Android Storage Settings when the device does not expose the cache-cleanup request.
- Uses the Universal **U** brand icon and redesigned interface.
- Never claims to have unrestricted access to other apps' private data.

## Important Android limitation

Android 11+ prevents ordinary apps from directly opening or deleting other apps' private cache directories. Android does, however, provide the official `ACTION_CLEAR_APP_CACHE` system request for asking the operating system to remove app caches. Device/OEM behavior can vary, and Android warns that clearing all app caches may affect battery life. See the Android developer documentation for the current platform behavior.

## Build

GitHub Actions builds the debug APK with Java 17 and Gradle 8.10 and uploads it as a workflow artifact.
