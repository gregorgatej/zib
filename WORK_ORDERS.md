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

# Work Order 6 — End-to-end documentation and release readiness check

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

