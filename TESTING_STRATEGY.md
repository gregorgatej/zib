# ZIB POC Testing Strategy

## 1. Testing doctrine

Tests are not decoration. They are the evidence that the POC behavior exists and remains stable when the project changes.

Every feature in the POC must have at least one meaningful test unless it is explicitly impossible to test without real audio hardware. For hardware/audio edges, test through interfaces and keep one manual smoke test.

A test that was not run is not evidence. A skipped test must be reported as skipped, not passed.

---

## 2. Test stack

Use:

- JUnit 5 for unit and integration tests.
- Maven Surefire for unit tests.
- Maven Failsafe or a Maven profile for integration tests if needed.

Standard commands:

```bash
mvn test
mvn package
```

Optional integration command:

```bash
mvn verify -Pintegration-tests
```

---

## 3. Unit tests

### 3.1 Parser tests: `ZibParserTest`

Required tests:

1. Parses the required demo file.
2. Parses multiple markers.
3. Parses marker at beginning.
4. Parses marker at end.
5. Rejects missing opening quote.
6. Rejects missing closing quote.
7. Rejects text outside the quoted block.
8. Rejects inner double quotes.
9. Rejects empty marker `${}`.
10. Rejects marker without `.wav`.
11. Rejects `.mp3` marker.
12. Rejects nested marker.
13. Preserves text spacing around markers.

Required demo parse expectation:

Input:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

Expected tokens:

```text
TEXT: "Today is a beautiful day. The children are playing outside "
SOUND: "children_laughing.wav"
TEXT: " on the playground."
```

### 3.2 Validator tests: `ZibValidatorTest`

Required tests:

1. Accepts existing `children_laughing.wav` in same directory as `.zib`.
2. Rejects missing WAV file.
3. Rejects absolute path marker.
4. Rejects parent traversal marker.
5. Rejects nested path like `sounds/x.wav` for POC.
6. Rejects unreadable or non-regular file where practical.

### 3.3 eSpeak tests: `EspeakNgServiceTest`

Do not require real `espeak-ng` for ordinary unit tests.

Required tests using a fake process runner abstraction:

1. Availability check succeeds when fake command returns zero.
2. Availability check fails with clear message when command is missing.
3. TTS generation builds expected argument list.
4. Non-zero eSpeak exit becomes a clear `ZibException`.
5. stderr from eSpeak is included in the error message where useful.

If the implementation does not add a process-runner abstraction, the agent must explain how unit tests avoid invoking real eSpeak.

### 3.4 Playback orchestration tests: `PlaybackOrchestratorTest`

Use a fake `AudioPlayer` that records calls.

Required tests:

1. Speech-only document calls `playBlocking` for each segment in order.
2. Sound markers call `playInBackground`.
3. Sound marker does not block next speech.
4. Orchestrator does not wait for background sounds after final speech.
5. Multiple sounds and speech segments preserve event order.

Expected event order for demo:

```text
playBlocking(segment-001.wav)
playInBackground(children_laughing.wav)
playBlocking(segment-002.wav)
return
```

### 3.5 Temp directory tests: `TempDirectoryManagerTest`

Required tests:

1. Creates a temp directory with `zib-` prefix.
2. Deletes temp directory after success.
3. Leaves temp directory after failure.
4. Recursive delete handles generated files.

---

## 4. Integration tests

Integration tests may require `espeak-ng` installed.

Recommended integration tests:

1. Generate a WAV file from short English text and verify file exists and size > 0.
2. Full parse + validate + generate pipeline on a temporary `.zib` and WAV effect file.

Do not make `mvn test` fail on machines without `espeak-ng` unless the test profile explicitly requires integration tests.

---

## 5. Manual smoke test

Manual smoke test after `mvn package`:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

Expected:

- speech starts;
- when reaching the marker position, `children_laughing.wav` starts in background;
- speech continues without waiting for laughter sound to finish;
- app exits when speech ends.

Because audio hardware differs between Linux systems, this is a manual smoke test, not the main proof of correctness.

---

## 6. Regression principle

When a bug is found, add a test that fails before the fix and passes after the fix.

Do not accept a fix that only changes implementation without capturing the bug in a test, unless the bug is truly untestable in automated form. If untestable, document why.

---

## 7. Coverage priorities

Highest priority:

- parser correctness;
- validation fail-closed behavior;
- missing sound errors;
- eSpeak error handling;
- playback event ordering;
- temp cleanup behavior.

Lower priority for POC:

- audio quality;
- exact speech timing;
- cross-platform audio behavior;
- GUI behavior.

