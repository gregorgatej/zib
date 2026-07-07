# AGENTS.md — ZIB POC Project Constitution

This file governs work by coding agents in this repository.

Codex and other coding agents must read this file before making changes.

---

## 1. Project identity

Project: **zib** — **zvok in beseda**  
Goal: Java 17 POC that reads a `.zib` file, generates speech with eSpeak NG, and starts WAV sound effects at marker positions while speech continues.

The POC is intentionally small. Do not expand it into a GUI, installer, MP3 player, or full audio editor.

---

## 2. Technical stack

Use:

- Java 17.
- Maven.
- JUnit 5.
- eSpeak NG as external CLI executable.
- Java Sound API for WAV playback.
- Linux as the POC target.

Do not add frameworks unless the work order explicitly allows it.

---

## 3. Required user-facing command

The packaged app must run as:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

The demo `.zib` file must contain:

```zib
"Danes je prečudovit dan. Otroci se zunaj igrajo ${otroski_smeh.wav} na igrišču."
```

---

## 4. POC syntax law

The `.zib` parser must enforce:

- exactly one main quoted block;
- no inner double quotes;
- marker form `${filename.wav}`;
- marker must not be empty;
- only `.wav` markers;
- no `.mp3`;
- no paths inside markers;
- no absolute paths;
- no `..` parent traversal;
- referenced WAV must exist next to the `.zib` file.

Invalid input must fail with a clear user-facing error.

---

## 5. Playback law

Sound markers start background playback.

They must not block speech continuation.

The application exits when speech playback ends, even if a background sound effect is still playing.

---

## 6. Temp file law

Generate TTS WAV segments in a temporary directory with a `zib-` prefix.

- On success: delete temp files.
- On failure: keep temp files and report the temp directory path.

---

## 7. Design discipline

Keep pure logic separate from system boundaries.

Pure/testable:

- parser;
- validation logic where possible;
- playback event orchestration;
- temp cleanup decisions.

Boundary adapters:

- eSpeak process execution;
- Java Sound playback;
- filesystem access.

Use interfaces or small abstractions where they make tests meaningful. Do not overengineer.

---

## 8. Testing requirements

Every behavior change must include or update tests.

Minimum command before reporting completion:

```bash
mvn test
```

If packaging changed, also run:

```bash
mvn package
```

If integration tests are configured and `espeak-ng` is installed, run:

```bash
mvn verify -Pintegration-tests
```

A skipped test is not a passing test. A test that was not run is not evidence.

Reports must distinguish:

- passed tests;
- failed tests;
- skipped tests;
- tests not run and why.

---

## 9. Error handling rules

User-facing errors must:

- start with `ERROR:`;
- be understandable without reading Java stack traces;
- identify the failing file or marker when useful;
- exit non-zero.

Do not silently ignore invalid markers or missing WAV files.

---

## 10. Dependency rules

Keep dependencies minimal.

Allowed by default:

- JUnit 5 for tests.
- Maven plugins needed to package an executable JAR.

Ask or justify before adding:

- MP3/audio codec libraries;
- GUI frameworks;
- logging frameworks;
- CLI frameworks;
- external test containers;
- anything that downloads models or binaries.

Do not add network runtime dependencies for the POC.

---

## 11. Security and safety rules

Fail closed.

Do not allow marker paths to read files outside the `.zib` directory.

Do not execute shell commands through `/bin/sh -c` for eSpeak. Use `ProcessBuilder` argument lists.

Do not read secrets.

Do not modify files outside the repository except temporary test/runtime files under the system temp directory.

---

## 12. Git and PR workflow

Use small, reviewable changes.

For agent tasks:

- start from current `main` unless instructed otherwise;
- create a feature branch;
- commit only related files;
- do not merge your own PR;
- do not claim readiness without tests.

---

## 13. Required final report format

Every agent report must include:

```markdown
## Summary
- ...

## Files changed
- ...

## Tests run
- `mvn test`: result
- `mvn package`: result, if run
- other commands: result

## Behavior verified
- ...

## Not done / non-goals preserved
- ...

## Risks or skipped tests
- ...

## Temp/local setup changes
- ...
```

Never write only “done” or “all checks passed”. Provide evidence.

