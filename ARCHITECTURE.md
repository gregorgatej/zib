# ZIB POC Architecture

## 1. Architecture summary

ZIB POC is a small Java 17 CLI application with a clean separation between pure logic and system boundaries.

Core design:

```text
.zib file
  -> parser
  -> validator
  -> token list
  -> TTS segment generator using espeak-ng
  -> playback orchestrator
  -> Java Sound playback
```

The parser, validator, and playback orchestration must be testable without real audio output and without invoking eSpeak NG.

---

## 2. Main components

### 2.1 `ZibApp`

Entry point.

Responsibilities:

- parse CLI arguments;
- coordinate startup validation;
- parse `.zib`;
- validate sound references;
- create temp directory;
- generate speech WAVs;
- run playback;
- clean temp directory after success;
- keep temp directory after failure;
- convert exceptions to user-facing stderr messages and non-zero exit code.

### 2.2 `CliArguments`

Responsibilities:

- validate that exactly one argument was provided;
- resolve the `.zib` path;
- validate it exists and is a regular file.

### 2.3 `ZibParser`

Pure parser. No filesystem access. No audio. No process execution.

Input:

```java
String fileContent
```

Output:

```java
ZibDocument(List<ZibToken> tokens)
```

Token types:

```java
sealed interface ZibToken permits TextToken, SoundToken
record TextToken(String text) implements ZibToken {}
record SoundToken(String filename) implements ZibToken {}
```

Alternatively, if avoiding sealed types for simplicity, use a small enum + record/class model.

Parser errors should throw a domain exception with a clear message.

### 2.4 `ZibValidator`

Validates semantic rules that need filesystem context.

Responsibilities:

- marker filename is simple and safe;
- marker has `.wav` extension;
- marker file exists in the same directory as `.zib`;
- marker file is a regular readable file.

### 2.5 `EspeakNgService`

Boundary adapter around the `espeak-ng` executable.

Responsibilities:

- check availability of `espeak-ng`;
- generate one WAV file from one text segment;
- capture stderr and exit code;
- never build commands by shell string concatenation.

### 2.6 `TtsSegmentGenerator`

Converts text tokens into generated speech segment files.

Responsibilities:

- assign deterministic segment filenames: `segment-001.wav`, `segment-002.wav`, ...;
- skip empty/blank text tokens if appropriate;
- preserve the token timeline so sound markers remain in the right place.

Possible internal model:

```java
sealed interface PlaybackEvent permits SpeechEvent, SoundEvent
record SpeechEvent(Path wavFile) implements PlaybackEvent {}
record SoundEvent(Path wavFile) implements PlaybackEvent {}
```

### 2.7 `AudioPlayer`

Interface for audio operations.

```java
public interface AudioPlayer {
    void playBlocking(Path wavFile);
    void playInBackground(Path wavFile);
}
```

Tests should use a fake implementation that records calls.

### 2.8 `JavaSoundAudioPlayer`

Java Sound implementation of `AudioPlayer`.

Responsibilities:

- load WAV file;
- play speech blocking;
- start sound effects in background;
- use one Java Sound output line for runtime playback;
- mix active background effects into speech chunks;
- avoid leaking audio resources as much as possible for POC.

### 2.9 `PlaybackOrchestrator`

Pure-ish coordinator.

Responsibilities:

- walk ordered playback events;
- for speech: call `playBlocking`;
- for sound: call `playInBackground`;
- not wait for background sounds at the end.

### 2.10 `TempDirectoryManager`

Responsibilities:

- create `/tmp/zib-*` directory using Java temp APIs;
- delete recursively on success;
- leave intact on failure;
- expose temp path for error messages.

---

## 3. Design decisions

### 3.1 eSpeak NG instead of LLM

ZIB needs deterministic text-to-speech, not text generation. A local LLM would add model management, CPU cost, configuration, and packaging complexity without solving the core POC need.

### 3.2 WAV instead of MP3

WAV is simpler for POC because eSpeak NG can generate WAV output and Java Sound can play sampled audio via standard APIs. MP3 would likely require extra dependencies or platform-specific support.

### 3.3 External executable instead of Java TTS library

Calling `espeak-ng` via `ProcessBuilder` keeps the Java implementation small and avoids binding to native libraries in the POC.

### 3.4 Generate first, play later

The POC generates all TTS WAV segments before playback. This keeps timing, errors, and tests simpler than streaming TTS.

### 3.5 CLI and `.zib` file only

`.zib` files map naturally to sound effects stored in the same directory. This also makes tests and demo setup reproducible.

---

## 4. Failure posture

ZIB should fail closed:

- invalid syntax is rejected;
- unsupported audio formats are rejected;
- unsafe marker filenames are rejected;
- missing effects stop the run;
- missing `espeak-ng` stops the run;
- non-zero eSpeak exit stops the run.

The app should not silently skip sound markers.

---

## 5. Future architecture notes, not for POC

Possible later changes:

- desktop GUI;
- bundled eSpeak NG executable;
- generated installer;
- project directory format;
- WAV mixing/export;
- waveform preview;
- voice selection;
- language selection;
- MP3 support via an explicit library;
- configuration file.

Do not implement these until the POC behavior is verified and test-backed.
