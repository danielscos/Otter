# Otter 🦦

A minimal Android video downloader powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp), inspired by, well, seal of course!
## Features

- **Wide platform support** -> downloads from TikTok, Instagram, YouTube, and any site yt-dlp supports
- **Saves to gallery** -> downloaded videos go straight to `Movies/otter` on your device
- **Live progress** -> real-time progress bar and status text while downloading
- **Auto-updating engine** -> yt-dlp updates itself to the latest nightly build on every launch
- **Clean UI** -> built with Jetpack Compose and Material 3

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Repository) |
| Downloader engine | [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) (yt-dlp wrapper) |
| Media processing | FFmpeg (via youtubedl-android) |
| Async | Kotlin Coroutines |
| Media saving | Android MediaStore API |

## Requirements

- Android **12** or higher (API 31+)
- Internet connection
- The first launch may take a moment while yt-dlp initializes and updates

## Building

1. Clone the repository:
   ```sh
   git clone https://github.com/yourname/otter.git
   ```

2. Open in **Android Studio** (Meerkat or newer recommended).

3. Create a `keystore.properties` file in the project root for release signing:
   ```
   keyAlias=your-key-alias
   keyPassword=your-key-password
   storeFile=my-release-key.jks
   storePassword=your-store-password
   ```
   For debug builds you can skip this and remove the `signingConfigs` block from `app/build.gradle.kts`.

4. Build and run on a device or emulator.

## How It Works

1. **Initialization** -> on app start, `MyApplication` initializes the yt-dlp and FFmpeg binaries in the background and pulls the latest yt-dlp nightly update.
2. **Input** -> the user pastes or types a video URL. If the app was opened via a share intent, the URL is extracted automatically from the shared text.
3. **Download** -> `DownloadRepository` builds a `YoutubeDLRequest` targeting the best available MP4 quality, runs it, and streams progress back to the UI.
4. **Save** -> once complete, the video is written to the MediaStore under `Movies/otter` and the temporary cache file is deleted.
5. **Feedback** -> the UI transitions from a progress bar to a success checkmark via an animated `AnimatedContent`.

## Project Structure

```
app/src/main/java/com/example/otter/
├── MainActivity.kt          # Entry point, handles share intents
├── MyApplication.kt         # Initializes and updates yt-dlp engine
├── DownloaderScreen.kt      # Compose UI
├── DownloaderViewModel.kt   # UI state management
├── DownloadRepository.kt    # yt-dlp download logic + MediaStore save
└── FileUtils.kt             # MediaStore helper utilities
```

## License

[MIT](LICENSE)
