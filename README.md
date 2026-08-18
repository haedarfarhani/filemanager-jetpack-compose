# File Manager

A modern Android file manager application built with Jetpack Compose and Material Design 3. Browse, manage, and organize your files with a clean, intuitive interface and a custom icon system.

## Features

### Core Features
- **Storage Volume Browsing** - View all storage volumes with real-time usage statistics (used/total space with animated progress indicators)
- **Directory Navigation** - Smooth folder navigation with full back-history support
- **Permission Handling** - Seamless storage permission management for Android 10+ scoped storage (READ/WRITE/MANAGE_EXTERNAL_STORAGE)
- **Splash Screen** - Animated launch screen with app branding

### File Management
- **Context Menu** - Long-press any file to access Copy, Move, Rename, Delete, Share, and Details options
- **File Sharing** - Share files via Android Share Sheet using FileProvider (secure content:// URIs)
- **Rename & Delete** - In-place rename with validation, safe delete with confirmation dialog
- **Copy & Move** - Clipboard-based file operations with progress overlay
- **Text Editor** - Built-in text/code editor with save functionality and unsaved changes detection

### File Details
- **Type-Specific Metadata** - Detailed info based on file type:
  - **Images** - Dimensions (width × height)
  - **Video** - Duration, resolution
  - **Audio** - Duration
  - **APK** - Package name, version, SDK info
  - **Folders** - File/folder count, total size (lazy-loaded)
- **General Info** - Size, location, path, last modified, MIME type, extension

### Display Modes
- **5 Display Modes** - List Small, List Large, Grid Small, Grid Medium, Grid Large
- **Adaptive Grid** - Grid cells adapt to screen size
- **Visual Selection** - Selected files highlighted with primary container color

### Custom Icon System
- **68+ Custom Icons** - Android VectorDrawable icons for every file type
- **Format-Specific Icons** - Separate icons for MP4, MKV, AVI, JPG, PNG, etc.
- **Directory Icons** - Special icons for Download, Documents, Pictures, Music, DCIM, Movies, Android
- **Color-Coded Types** - Each file type has a distinct color (Image=Green, Video=Pink, Audio=Purple, Document=Blue, Archive=Orange)
- **Thumbnail Support** - Images and videos show actual thumbnails via Coil

## Architecture

The project follows **MVVM (Model-View-ViewModel)** architecture pattern with a clean layered structure:

```
app/src/main/java/com/heydar/filemanager/
├── FileManagerApp.kt                    # Application class (Hilt entry point)
├── di/
│   └── AppModule.kt                     # Hilt dependency injection module
├── core/
│   ├── icons/                           # Centralized icon system
│   │   ├── AppIcons.kt                  # Single source of truth for all drawable resources
│   │   ├── FileIconResolver.kt          # Maps FileType/FileFormat → drawable
│   │   └── FolderIconResolver.kt        # Maps DirectoryType → drawable
│   └── util/                            # Core utilities
│       ├── DateFormatter.kt             # Relative date formatting (Just now, Today, Yesterday, etc.)
│       ├── FileMetadataExtractor.kt     # Extracts metadata on IO thread (dimensions, duration, APK info)
│       ├── FileShareManager.kt          # FileProvider-based file sharing
│       ├── FileSizeFormatter.kt         # Human-readable file sizes (B, KB, MB, GB, TB)
│       └── MimeTypeHelper.kt            # Extension → MIME type mapping (70+ types)
├── domain/
│   └── model/                           # Domain models
│       ├── DirectoryType.kt             # 15 directory types (Download, DCIM, WhatsApp, etc.)
│       ├── FileDetails.kt              # FileDetails, CommonFileInfo, SpecificFileInfo sealed interface
│       ├── FileDisplayMode.kt           # 5 display modes enum
│       ├── FileFormat.kt               # 24 file formats with type associations
│       ├── FileItem.kt                  # Core file/folder data class
│       ├── FileType.kt                  # 8 file types (IMAGE, VIDEO, AUDIO, DOCUMENT, TEXT, ARCHIVE, APK, OTHER)
│       └── StorageVolumeInfo.kt         # Storage volume with usage progress
└── ui/
    ├── MainActivity.kt                  # Main activity
    ├── navigation/
    │   ├── Screen.kt                    # Route definitions (Splash, Home, FileList, TextEditor, FileDetails)
    │   └── AppNavGraph.kt              # Navigation graph with all routes
    ├── theme/
    │   ├── Color.kt                    # Material 3 color palette + file type colors
    │   ├── Theme.kt                    # Light/Dark theme with dynamic color support
    │   ├── Type.kt                     # Typography definitions
    │   ├── Spacing.kt                  # Consistent spacing system
    │   └── Shape.kt                    # Rounded corner shapes
    ├── components/
    │   ├── dialog/
    │   │   ├── FileActionsBottomSheet.kt # Context menu bottom sheet
    │   │   ├── RenameDialog.kt          # Rename input dialog
    │   │   └── DeleteConfirmationDialog.kt # Delete confirmation
    │   ├── file/
    │   │   └── FileIcon.kt             # Centralized file icon composable (thumbnails + type icons)
    │   └── icon/
    │       └── AppIcon.kt              # Base icon composable
    └── screens/
        ├── splash/
        │   ├── SplashScreen.kt          # Animated splash screen
        │   ├── SplashViewModel.kt       # Splash logic
        │   └── SplashUiEvent.kt        # Navigation event
        ├── home/
        │   ├── HomeScreen.kt            # Storage volume list with progress bars
        │   ├── HomeScreenViewModel.kt   # Volume loading logic
        │   ├── HomeScreenState.kt       # UI state
        │   └── HomeScreenEvents.kt      # Events
        ├── files/
        │   ├── FileListScreen.kt        # File/folder list with display modes
        │   ├── FileListScreenViewModel.kt # File operations (copy, move, rename, delete)
        │   ├── FileListScreenState.kt   # UI state with display mode
        │   └── FileListScreenEvents.kt  # Events
        ├── details/
        │   ├── FileDetailsScreen.kt     # File/folder details with type-specific info
        │   ├── FileDetailsViewModel.kt  # Metadata extraction logic
        │   ├── FileDetailsUiState.kt    # UI state
        │   └── FileDetailsEvent.kt      # Events
        └── editor/
            ├── TextEditorScreen.kt      # Text/code editor with save
            ├── TextEditorViewModel.kt   # File read/write logic
            ├── TextEditorUiState.kt     # UI state
            └── TextEditorEvent.kt       # Events
```

## Supported File Types

| Category | Formats | Extensions |
|----------|---------|------------|
| **Image** | 13 | jpg, jpeg, png, gif, bmp, webp, heic, heif, tiff, tif, svg, ico, avif |
| **Video** | 11 | mp4, mkv, avi, mov, webm, 3gp, flv, mpeg, mpg, m4v, ts |
| **Audio** | 11 | mp3, wav, flac, aac, ogg, m4a, wma, opus, amr, mid, midi |
| **Document** | 13 | pdf, doc, docx, xls, xlsx, ppt, pptx, rtf, odt, ods, odp, epub, mobi |
| **Text** | 17 | txt, log, md, csv, json, xml, html, css, js, kt, java, py, go, c, cpp, h, cs |
| **Archive** | 11 | zip, rar, 7z, tar, gz, bz2, xz, tar.gz, tar.bz2, tgz, cab |
| **APK** | 3 | apk, apks, xapk |

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Kotlin** | 2.4.10 | Primary language |
| **Jetpack Compose** | BOM 2026.06.01 | Declarative UI toolkit |
| **Material 3** | Latest | Design system and components |
| **Hilt** | 2.60.1 | Dependency injection |
| **KSP** | 2.3.10 | Annotation processing |
| **Coil** | 2.7.0 | Async image loading for thumbnails |
| **Navigation Compose** | 2.9.8 | Type-safe navigation |
| **FileProvider** | AndroidX | Secure file sharing between apps |

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 37 (Target/Compile)
- Min SDK 24 (Android 7.0 Nougat)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/FileManeger.git
   ```
2. Open the project in Android Studio
3. Sync Gradle and let dependencies download
4. Connect a device or start an emulator
5. Click **Run** or use `./gradlew installDebug`

## Permissions

| Permission | Required For | Android Version |
|---|---|---|
| `READ_EXTERNAL_STORAGE` | Reading files | Below Android 13 |
| `WRITE_EXTERNAL_STORAGE` | Writing files | Below Android 10 |
| `MANAGE_EXTERNAL_STORAGE` | Full file access | Android 11+ |

## Navigation

```
Splash → Home → FileList → TextEditor (for text files)
                        → FileDetails (file/folder info)
```

## License

```
Copyright 2026 Heydar Farhani

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
