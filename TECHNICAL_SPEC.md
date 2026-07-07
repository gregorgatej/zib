# ZIB POC Technical Specification

Project name: **zib** — **zvok in beseda**  
Version target: **0.1.0 POC**  
Language/runtime: **Java 17**  
Build tool: **Maven**  
Target OS for POC: **Linux**  
TTS engine: **eSpeak NG**, invoked as an external command  
Audio effects format: **WAV only**  
Primary user interface for POC: **CLI with `.zib` file input**

Reference links:

- eSpeak NG: https://github.com/espeak-ng/espeak-ng
- Java 17 `ProcessBuilder`: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ProcessBuilder.html
- Java Sound playback guide: https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/chapter4.html
- Java `SourceDataLine`: https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/SourceDataLine.html
- Maven getting started: https://maven.apache.org/guides/getting-started/
- JUnit 5 user guide: https://docs.junit.org/current/user-guide/
- Codex `AGENTS.md` guide: https://developers.openai.com/codex/guides/agents-md

---

## 1. Product goal

ZIB is a local Java application that reads a `.zib` text file, generates speech for the text portions, and starts selected WAV sound effects at marker positions while speech continues.

The POC must prove this user path:

1. User prepares a `.zib` file.
2. User puts referenced WAV sound effects in the same directory as the `.zib` file.
3. User runs:

   ```bash
   java -jar target/zib-0.1.0.jar examples/demo.zib
   ```

4. The app parses the file.
5. The app generates temporary WAV speech segments using `espeak-ng`.
6. The app plays generated speech segments sequentially.
7. When a sound marker is reached, the app starts the referenced WAV file in the background.
8. The app exits when speech playback ends, even if a background sound effect is still playing.
9. On successful completion, temporary TTS WAV files are deleted.
10. On error, temporary files are retained for debugging and the error message explains what failed.

---

## 2. Explicit POC non-goals

Do **not** implement these in the POC:

- GUI or desktop installer.
- Terminal text input mode.
- MP3 support.
- Final mixed audio export.
- Real-time streaming TTS.
- LLM integration.
- Multiple `.zib` documents in one run.
- Nested syntax.
- Escaped quotes inside the main string.
- User-selectable voices beyond a small default configuration.
- Cross-platform support beyond Linux.
- Network access.
- Downloading models or external binaries at runtime.

---

## 3. `.zib` file format for POC

A POC `.zib` file contains exactly one main quoted block.

Valid example:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

The concrete demo file must contain exactly this line:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

### 3.1 Syntax rules

- File must contain exactly one main block between `"` and `"`.
- Inner double quotes are not supported.
- Text outside the one quoted block is invalid, except surrounding whitespace.
- Markers have the exact form `${filename.wav}`.
- Marker content must not be empty.
- Marker content must end with `.wav`.
- Marker content must be a simple filename, not a path.
- Absolute paths are forbidden.
- Parent traversal is forbidden: `../sound.wav` is invalid.
- Nested markers are invalid.
- MP3 is invalid.
- Referenced WAV files must exist in the same directory as the `.zib` file.

### 3.2 Parsed token model

The parser converts the quoted block into an ordered token list:

```text
TEXT("Today is a beautiful day. The children are playing outside ")
SOUND("children_laughing.wav")
TEXT(" on the playground.")
```

Multiple sound markers are supported:

```zib
"Hello ${a.wav} world ${b.wav} again."
```

This should parse as:

```text
TEXT("Hello ")
SOUND("a.wav")
TEXT(" world ")
SOUND("b.wav")
TEXT(" again.")
```

Empty text tokens may be omitted internally, but behavior must still be correct for markers at the beginning or end.

---

## 4. Runtime behavior

### 4.1 Startup validation

At startup, the app must validate:

1. Exactly one CLI argument is provided.
2. The argument points to an existing regular `.zib` file.
3. `espeak-ng` is available on `PATH`.
4. The `.zib` file is syntactically valid.
5. All referenced sound markers point to existing `.wav` files in the same directory.

If any validation fails, the app exits non-zero and prints a clear error message to stderr.

Example missing eSpeak message:

```text
ERROR: espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng
```

Example missing sound message:

```text
ERROR: Referenced sound file not found next to .zib file: children_laughing.wav
```

### 4.2 Temporary directory

The app creates a temporary directory under the system temp location, e.g.:

```text
/tmp/zib-<random>/
```

It writes generated speech WAV segments there.

Rules:

- On successful completion: delete temporary directory recursively.
- On any failure after temp directory creation: keep temp directory and print its path.

Example:

```text
ERROR: Failed to generate TTS segment 2. Temporary files kept at: /tmp/zib-abc123
```

### 4.3 TTS generation

For each text token, generate one WAV file using `espeak-ng`.

Recommended command shape:

```bash
espeak-ng -v en -w /tmp/zib-abc123/segment-001.wav "text to speak"
```

Implementation detail:

- Use Java `ProcessBuilder`.
- Pass arguments as a list, not as a shell-concatenated string.
- Capture stderr.
- Treat non-zero exit status as failure.
- Do not attempt shell escaping manually.

### 4.4 Playback semantics

Speech controls application lifetime.

For token sequence:

```text
TEXT A, SOUND X, TEXT B
```

The app must:

1. Play speech segment A and block until it finishes.
2. Start sound effect X in the background and do not wait for it to finish.
3. Play speech segment B and block until it finishes.
4. Exit as soon as all speech segments have completed.

If X is still playing when speech ends, the process may terminate and cut it off.

### 4.5 WAV playback

For POC, use Java Sound API.

Recommended implementation:

- Use `AudioSystem.getAudioInputStream(...)`.
- Use one `SourceDataLine` for runtime output.
- For speech playback: write speech chunks to the output line and block until the speech segment is complete.
- For background effects: register the WAV data as an active background sound and return immediately.
- Mix active background effects into speech chunks while speech is advancing.

Keep audio implementation behind an interface so parser/orchestrator tests do not require actual speakers.

---

## 5. Error handling rules

All user-facing errors must:

- start with `ERROR:`;
- explain the failed condition;
- include the relevant file path or marker where useful;
- exit with a non-zero process code;
- avoid Java stack traces unless a debug mode is later added.

For POC, no debug flag is required.

---

## 6. Proposed package structure

```text
src/main/java/com/zib/
  ZibApp.java
  cli/CliArguments.java
  parser/ZibParser.java
  parser/ZibDocument.java
  parser/ZibToken.java
  validation/ZibValidator.java
  tts/EspeakNgService.java
  tts/TtsSegmentGenerator.java
  audio/AudioPlayer.java
  audio/JavaSoundAudioPlayer.java
  runtime/PlaybackOrchestrator.java
  runtime/TempDirectoryManager.java
  error/ZibException.java

src/test/java/com/zib/
  parser/ZibParserTest.java
  validation/ZibValidatorTest.java
  tts/EspeakNgServiceTest.java
  runtime/PlaybackOrchestratorTest.java
  runtime/TempDirectoryManagerTest.java
```

---

## 7. Acceptance criteria

The POC is acceptable only if all of the following are true:

- `mvn test` passes.
- `mvn package` produces `target/zib-0.1.0.jar`.
- Running `java -jar target/zib-0.1.0.jar examples/demo.zib` works on Linux when `espeak-ng` is installed and `examples/children_laughing.wav` exists.
- Missing `espeak-ng` produces a clear error.
- Missing marker WAV produces a clear error.
- `.mp3` marker is rejected.
- The parser supports multiple markers.
- Background sound starts without blocking subsequent speech.
- The process exits when speech playback ends.
- Temp files are deleted after success and retained after failure.
- README contains setup and run instructions.
