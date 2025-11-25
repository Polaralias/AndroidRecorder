# Android Recorder Architecture

This document outlines the proposed architecture, modules, and data flow for the recorder app.

## High-Level Modules
- **app**: Application entry point, DI setup, navigation graph, theming.
- **core/common**: Shared utilities, coroutine dispatchers, result wrappers, logger.
- **core/designsystem**: Material 3 theme, typography, components (buttons, chips, indicators).
- **data**: Persistence, network, and device integrations.
  - **local**: Room database for recordings, DataStore for preferences (backup, transcription settings).
  - **file**: Audio file management, naming, cleanup, storage location selection.
  - **transcription**: Whisper local engine wrapper and Google Cloud Speech client.
  - **backup**: Google Drive REST API client and sync scheduler (WorkManager).
- **domain**: Entities, repositories interfaces, and use cases for recording, transcription, and backup.
- **feature/record**: Recording screen, recording service coordination, waveform visualization.
- **feature/library**: List/search/sort recordings, detail view with playback and transcription status.
- **feature/settings**: Backup configuration, API key entry, model download management, quick actions.

## Core Data Flow
1. **Start recording**
   - Triggered from UI, Quick Settings tile, notification action, or hardware shortcut handler.
   - ViewModel calls `StartRecordingUseCase` → `RecorderRepository.startRecording()`.
   - Repository requests storage path from FileManager, starts `ForegroundRecorderService` via `RecordingController`, and emits active session state via Flow.
   - Service acquires foreground notification, optionally partial wake lock, and streams audio to file.
2. **Recording lifecycle**
   - Service writes PCM/encoded audio to file, exposes level updates through a shared Flow for UI waveform.
   - Pause/resume handled in service; state mirrored in repository and UI through `RecordingSession` domain model.
3. **Stop recording**
   - `StopRecordingUseCase` stops service, finalizes file, and persists metadata via `RecordingDao`.
   - Use case schedules transcription (local offline or cloud) and optional Drive backup using WorkManager.
4. **Transcription**
   - `TranscriptionManager` selects local Whisper or cloud Speech-to-Text based on settings and connectivity.
   - Local transcription uses downloaded model managed by `ModelManager`. Cloud transcription uses provided API key with minimal backend/direct REST depending on feasibility.
   - Results stored in Room and attached to recording metadata.
5. **Backup**
   - `BackupScheduler` enqueues Drive upload work with constraints (unmetered network, charging if configured).
   - `BackupRepository` handles incremental sync, conflict resolution, and quota errors surfaced to UI.
6. **Playback and library**
   - `RecordingRepository` exposes paging/Flow for recordings with filters. Playback uses ExoPlayer wrapper; editing metadata updates Room.

## Main Screens & Navigation
- **Now Recording** (`record`) – Large timer, waveform/level indicator, pause/resume/stop, transcript progress. Entry for ongoing session from notification.
- **Library** (`library`) – Search, sort (date, duration, name), filter by transcription/backup status, quick actions to share/delete.
- **Recording Detail** (`detail/{id}`) – Playback, transcript view with status, backup state, rename.
- **Settings** (`settings`) – Drive backup toggle, account/auth state, API key entry for Google Cloud Speech, model download management, hardware shortcut setup, privacy.
- **Onboarding** (`onboarding`) – Permissions (record audio, notifications, storage), quick tips, model download prompt.

Navigation uses a single-activity Compose setup with a navigation graph defined in the `app` module. Deep links from notifications/tiles route directly to the recording screen or specific recording detail.
