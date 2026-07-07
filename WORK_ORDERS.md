# ZIB POC Work Orders for Codex

Use these as PR-sized tasks. Do not give Codex the whole project as one giant implementation task unless you explicitly want a less controlled run.

Each work order assumes the repository contains `AGENTS.md`, `TECHNICAL_SPEC.md`, `ARCHITECTURE.md`, and `TESTING_STRATEGY.md`.

---

# Work Order 1 — Maven skeleton and executable CLI

## Governing instructions

Read `AGENTS.md` first. Follow the project constitution.

## Goal

Create the initial Java 17 Maven project skeleton for ZIB POC.

## Scope

Implement:

- Maven `pom.xml` for Java 17.
- JUnit 5 test setup.
- executable JAR packaging so this command works after packaging:

  ```bash
  java -jar target/zib-0.1.0.jar examples/demo.zib
  ```

- `com.zib.ZibApp` entry point.
- Basic CLI argument validation:
  - exactly one argument required;
  - file must exist;
  - file must be regular file.
- `examples/demo.zib` with the required demo text.
- README with build/run commands and eSpeak install note.

## Non-goals

- Do not implement parser yet beyond placeholder wiring.
- Do not implement audio playback.
- Do not invoke eSpeak yet.
- Do not add GUI.
- Do not add MP3 support.

## Tests required

- CLI argument validation unit tests.
- `mvn test` must pass.
- `mvn package` must produce the JAR.

## Final report required

Use the report format from `AGENTS.md`.

---

# Work Order 2 — `.zib` parser

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, and `TESTING_STRATEGY.md` first.

## Goal

Implement the pure `.zib` parser and token model.

## Scope

Implement:

- `ZibParser`.
- `ZibDocument`.
- `ZibToken` model.
- Domain exception for parser errors.

Parser must support:

- exactly one quoted block;
- text tokens;
- `${filename.wav}` sound markers;
- multiple markers;
- marker at beginning/end;
- rejection of invalid syntax.

## Non-goals

- No filesystem validation.
- No eSpeak.
- No audio playback.
- No MP3 support.

## Tests required

Implement all parser tests listed in `TESTING_STRATEGY.md` section 3.1.

Run:

```bash
mvn test
```

## Final report required

Use the report format from `AGENTS.md`.

---

# Work Order 3 — Semantic validation and fail-closed file checks

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, and `TESTING_STRATEGY.md` first.

## Goal

Validate parsed sound markers against the `.zib` file directory.

## Scope

Implement:

- `ZibValidator`.
- marker filename safety checks;
- `.wav` only;
- no absolute paths;
- no path traversal;
- no nested paths;
- referenced WAV exists in same directory.

Wire validation into `ZibApp` after parsing.

## Non-goals

- No audio playback.
- No eSpeak TTS generation.
- No MP3 support.
- Do not silently skip missing effects.

## Tests required

Implement validator tests from `TESTING_STRATEGY.md` section 3.2.

Run:

```bash
mvn test
```

## Final report required

Use the report format from `AGENTS.md`.

---

# Work Order 4 — eSpeak NG service and TTS segment generation

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, `ARCHITECTURE.md`, and `TESTING_STRATEGY.md` first.

## Goal

Add eSpeak NG availability check and TTS WAV generation for text tokens.

## Scope

Implement:

- `EspeakNgService`;
- startup availability check for `espeak-ng`;
- generation of one WAV file per text segment;
- `TtsSegmentGenerator`;
- temp directory creation with `zib-` prefix;
- failure behavior that keeps temp directory and reports path.

Use `ProcessBuilder` with argument lists, not shell strings.

## Non-goals

- No audio playback yet.
- No voice configuration UI.
- No MP3 support.
- No automatic installation of eSpeak.

## Tests required

- eSpeak service unit tests using a fake process runner or equivalent seam.
- temp directory tests.
- segment generation tests that do not require real eSpeak.

Run:

```bash
mvn test
```

If integration profile exists and eSpeak is available, also run:

```bash
mvn verify -Pintegration-tests
```

## Final report required

Use the report format from `AGENTS.md` and clearly state whether real eSpeak integration tests were run.

---

# Work Order 5 — Java Sound playback and orchestration

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, `ARCHITECTURE.md`, and `TESTING_STRATEGY.md` first.

## Goal

Implement audio playback orchestration so speech plays sequentially and sound effects start in the background.

## Scope

Implement:

- `AudioPlayer` interface;
- `JavaSoundAudioPlayer`;
- `PlaybackOrchestrator`;
- conversion from parsed tokens + generated speech files to ordered playback events;
- app behavior that exits after final speech segment, without waiting for background effects.

## Non-goals

- No mixing/export.
- No MP3 support.
- No waiting for background sounds at process end.
- No GUI.

## Tests required

- playback orchestration tests using fake `AudioPlayer`;
- event order tests for the demo document;
- test that background effect does not block next speech;
- test that orchestrator returns after final speech.

Run:

```bash
mvn test
mvn package
```

Manual smoke test if audio environment is available:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

## Final report required

Use the report format from `AGENTS.md` and separate automated test evidence from manual smoke test evidence.

---

# Work Order 5.1 — Added-on single-line Java Sound playback fix

## Why this work order exists

This is an added-on corrective work order discovered during manual verification of Work Order 5.

Manual run failed with:

```text
ERROR: Audio line unavailable for WAV file: /tmp/zib-15192541996189026679/segment-002.wav. Temporary files kept at: /tmp/zib-15192541996189026679
```

The generated files and referenced effect were then verified outside ZIB:

```bash
aplay /tmp/zib-15192541996189026679/segment-001.wav
aplay /tmp/zib-15192541996189026679/segment-002.wav
aplay examples/otroski_smeh.wav
```

All three WAV files played normally. This points to Java Sound `Clip` line contention during concurrent background effect playback and the next speech segment, not invalid WAV files.

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, `ARCHITECTURE.md`, and `TESTING_STRATEGY.md` first.

## Goal

Fix Java Sound playback so the demo can continue from a background effect into the next speech segment without opening multiple simultaneous output lines.

## Scope

Implement:

- single shared Java Sound output line for runtime playback;
- in-memory WAV decoding to a common PCM format;
- runtime mixing of active background effects into speech chunks;
- lifecycle cleanup so the output line closes after playback success or failure;
- tests proving background effects do not require a second Java Sound output line.

## Non-goals

- Do not generate a single final mixed WAV file.
- No mixing/export feature.
- No MP3 support.
- No GUI.
- No waiting for background sounds after final speech.

## Tests required

- single-output-line audio tests using fake decoder/output-line seams;
- speech-only playback test;
- background-before-speech test proving no write happens until speech advances;
- demo-order mixing test for `segment-001.wav`, `otroski_smeh.wav`, `segment-002.wav`;
- background-longer-than-final-speech test proving the effect is cut off;
- cleanup-on-failure test.

Run:

```bash
mvn test
mvn package
```

Manual smoke test if audio environment is available:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

Expected manual result:

- no `Audio line unavailable` error;
- speech continues after `${otroski_smeh.wav}`;
- app exits after final speech.

## Final report required

Use the report format from `AGENTS.md` and separate automated test evidence from manual smoke test evidence.

---

# Work Order 6 — English demo content

## Governing instructions

Read `AGENTS.md`, `TECHNICAL_SPEC.md`, `ARCHITECTURE.md`, and `TESTING_STRATEGY.md` first.

## Goal

Switch the canonical demo from Slovene to English because the POC currently uses eSpeak NG with the default English voice.

## Scope

Implement:

- English demo text in `examples/demo.zib`;
- English WAV marker filename `children_laughing.wav`;
- docs/spec/test expectations that match the English demo;
- rename the tracked example WAV from `otroski_smeh.wav` to `children_laughing.wav`.

Canonical demo:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

## Non-goals

- No Slovene support yet.
- No language selection.
- No voice configuration UI.
- No change to the parser syntax.
- No MP3 support.

## Tests required

Run:

```bash
mvn test
mvn package
```

Manual smoke test if audio environment is available:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

Expected manual result:

- English speech is pronounced naturally by the default eSpeak voice;
- `children_laughing.wav` starts at the marker;
- speech continues after the marker;
- app exits after final speech.

## Final report required

Use the report format from `AGENTS.md` and separate automated test evidence from manual smoke test evidence.

---

# Work Order 7 — End-to-end documentation and release readiness check

## Governing instructions

Read all project docs first.

## Goal

Prepare the repository for POC review.

## Scope

- Update README so a new user can install eSpeak, build, test, and run the demo.
- Ensure examples are present.
- Ensure all non-goals are still non-goals.
- Ensure errors are clear.
- Ensure `mvn test` and `mvn package` pass.
- Add a short `docs/POC_REVIEW.md` summarizing implemented behavior and remaining limitations.

## Non-goals

- No new features.
- No GUI.
- No MP3.
- No installer.

## Tests required

Run:

```bash
mvn test
mvn package
```

If possible, run manual smoke test:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

## Final report required

Use the report format from `AGENTS.md`.
